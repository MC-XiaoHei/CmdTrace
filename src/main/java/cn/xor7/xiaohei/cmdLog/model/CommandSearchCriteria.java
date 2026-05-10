package cn.xor7.xiaohei.cmdLog.model;

import java.time.LocalDateTime;
import java.util.List;

public final class CommandSearchCriteria {

    private String server;
    private String playerName;
    private String uuid;
    private MatchMode matchMode;
    private String keyword;
    private List<String> startswithKeywords = List.of();
    private LocalDateTime from;
    private LocalDateTime to;
    private int limit;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(MatchMode matchMode) {
        this.matchMode = matchMode;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getStartswithKeywords() {
        return startswithKeywords;
    }

    public void setStartswithKeywords(List<String> startswithKeywords) {
        this.startswithKeywords = startswithKeywords;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean hasServer() {
        return server != null && !server.isBlank();
    }

    public boolean hasPlayerName() {
        return playerName != null && !playerName.isBlank();
    }

    public boolean hasUuid() {
        return uuid != null && !uuid.isBlank();
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    public boolean hasTimeRange() {
        return from != null || to != null;
    }
}
