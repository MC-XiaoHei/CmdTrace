package cn.xor7.xiaohei.cmdLog.database;

import cn.xor7.xiaohei.cmdLog.model.CommandLogEntry;
import cn.xor7.xiaohei.cmdLog.model.CommandLogRecord;
import cn.xor7.xiaohei.cmdLog.model.CommandSearchCriteria;
import cn.xor7.xiaohei.cmdLog.model.MatchMode;
import cn.xor7.xiaohei.cmdLog.util.SqlLikeEscaper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.sqlite.Function;

public final class CommandLogRepository {

    private final Connection connection;

    public CommandLogRepository(Connection connection) {
        this.connection = connection;
    }

    public void initialize() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS command_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    username TEXT NOT NULL,
                    uuid TEXT,
                    raw_command TEXT NOT NULL,
                    server TEXT NOT NULL
                )
                """
            );
            statement.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_server_cmd_time ON command_log(server, raw_command, timestamp DESC)"
            );
            statement.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_server_time ON command_log(server, timestamp DESC)"
            );
            statement.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_timestamp ON command_log(timestamp)"
            );
        }
        registerRegexpFunction();
    }

    public void insert(CommandLogRecord record) throws SQLException {
        String sql = """
            INSERT INTO command_log (timestamp, username, uuid, raw_command, server)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(record.timestamp()));
            statement.setString(2, record.username());
            statement.setString(3, record.uuid());
            statement.setString(4, record.rawCommand());
            statement.setString(5, record.server());
            statement.executeUpdate();
        }
    }

    public List<CommandLogEntry> search(CommandSearchCriteria criteria)
        throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT id, timestamp, username, uuid, raw_command, server FROM command_log WHERE 1=1"
        );
        List<Object> parameters = new ArrayList<>();

        appendServerClause(criteria, sql, parameters);
        appendMatchClause(criteria, sql, parameters);
        appendPlayerClause(criteria, sql, parameters);
        appendUuidClause(criteria, sql, parameters);
        appendTimeClause(criteria, sql, parameters);

        sql.append(" ORDER BY timestamp DESC LIMIT ?");
        parameters.add(criteria.getLimit());

        try (
            PreparedStatement statement = connection.prepareStatement(
                sql.toString()
            )
        ) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapEntries(resultSet);
            }
        }
    }

    public int deleteOlderThan(LocalDateTime time) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM command_log WHERE timestamp < ?"
            )
        ) {
            statement.setTimestamp(1, Timestamp.valueOf(time));
            return statement.executeUpdate();
        }
    }

    private void appendServerClause(
        CommandSearchCriteria criteria,
        StringBuilder sql,
        List<Object> parameters
    ) {
        if (!criteria.hasServer()) {
            return;
        }

        sql.append(" AND server = ?");
        parameters.add(criteria.getServer());
    }

    private void appendMatchClause(
        CommandSearchCriteria criteria,
        StringBuilder sql,
        List<Object> parameters
    ) {
        if (criteria.getMatchMode() == null || !criteria.hasKeyword()) {
            return;
        }

        if (criteria.getMatchMode() == MatchMode.STARTSWITH) {
            List<String> keywords = criteria.getStartswithKeywords().isEmpty()
                ? List.of(criteria.getKeyword())
                : criteria.getStartswithKeywords();
            sql.append(" AND (");
            for (int index = 0; index < keywords.size(); index++) {
                if (index > 0) {
                    sql.append(" OR ");
                }
                sql.append("raw_command LIKE ? ESCAPE '\\'");
                parameters.add(
                    SqlLikeEscaper.escape(keywords.get(index)) + "%"
                );
            }
            sql.append(")");
            return;
        }

        if (criteria.getMatchMode() == MatchMode.CONTAINS) {
            sql.append(" AND raw_command LIKE ? ESCAPE '\\'");
            parameters.add(
                "%" + SqlLikeEscaper.escape(criteria.getKeyword()) + "%"
            );
            return;
        }

        sql.append(" AND raw_command REGEXP ?");
        parameters.add(criteria.getKeyword());
    }

    private void appendPlayerClause(
        CommandSearchCriteria criteria,
        StringBuilder sql,
        List<Object> parameters
    ) {
        if (!criteria.hasPlayerName()) {
            return;
        }

        sql.append(" AND LOWER(username) = LOWER(?)");
        parameters.add(criteria.getPlayerName());
    }

    private void appendUuidClause(
        CommandSearchCriteria criteria,
        StringBuilder sql,
        List<Object> parameters
    ) {
        if (!criteria.hasUuid()) {
            return;
        }

        sql.append(" AND uuid = ?");
        parameters.add(criteria.getUuid());
    }

    private void appendTimeClause(
        CommandSearchCriteria criteria,
        StringBuilder sql,
        List<Object> parameters
    ) {
        if (criteria.getFrom() != null) {
            sql.append(" AND timestamp >= ?");
            parameters.add(Timestamp.valueOf(criteria.getFrom()));
        }

        if (criteria.getTo() != null) {
            sql.append(" AND timestamp <= ?");
            parameters.add(Timestamp.valueOf(criteria.getTo()));
        }
    }

    private void bindParameters(
        PreparedStatement statement,
        List<Object> parameters
    ) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private List<CommandLogEntry> mapEntries(ResultSet resultSet)
        throws SQLException {
        List<CommandLogEntry> entries = new ArrayList<>();
        while (resultSet.next()) {
            entries.add(
                new CommandLogEntry(
                    resultSet.getLong("id"),
                    resultSet.getTimestamp("timestamp").toLocalDateTime(),
                    resultSet.getString("username"),
                    resultSet.getString("uuid"),
                    resultSet.getString("raw_command"),
                    resultSet.getString("server")
                )
            );
        }
        return entries;
    }

    private void registerRegexpFunction() throws SQLException {
        Function.create(
            connection,
            "REGEXP",
            new Function() {
                @Override
                protected void xFunc() throws SQLException {
                    String regex = value_text(0);
                    String value = value_text(1);
                    if (regex == null || value == null) {
                        result(0);
                        return;
                    }
                    try {
                        boolean matches = Pattern.compile(
                            regex,
                            Pattern.CASE_INSENSITIVE
                        )
                            .matcher(value)
                            .find();
                        result(matches ? 1 : 0);
                    } catch (PatternSyntaxException exception) {
                        result(0);
                    }
                }
            }
        );
    }
}
