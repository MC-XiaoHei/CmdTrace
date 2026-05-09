# CmdLog

一个运行在 Velocity 上的命令日志插件。

- 记录玩家执行过的命令
  - 前缀/包含/正则查询
- 额外记录指定聊天前缀的消息，比如 `!!`

---

## 使用命令

```log
> /cmdlog help

CmdLog 查询帮助
用法：/cmdlog [--server <服务器名>] [--player <玩家名>] [--uuid <UUID>]
      [--startswith <前缀> | --contains <关键词> | --regex <正则>]
      [--from <时间>] [--to <时间>] [--limit <数量>]
--server <服务器名>  指定服务器；玩家执行时默认是当前服务器
--player <玩家名>    只看这个玩家的命令
--uuid <UUID>        按 UUID 精确查询
--startswith <前缀>  查命令前缀；不写 '/' 时会自动补上
--contains <关键词>  查包含指定内容的命令；不会自动补 '/'
--regex <正则>       用正则查询；不会自动补 '/'
--from <时间>        起始时间，如 2026-01-01 12:00:00
--to <时间>          结束时间，如 2026-01-01 18:00:00
--limit <数量>       限制返回条数
示例：
  /cmdlog (查看当前服务器最近记录)
  /cmdlog --startswith home (实际按 /home 查询)
  /cmdlog --contains "/time " (查包含 /time 的命令)
  /cmdlog --regex "/time .*" (匹配 /time 后面的任意内容)
```

---

## 权限

- `cmdlog.query`

---

## 默认配置

```properties
# SQLite 数据库文件名，最终会放在插件数据目录下
# 例如 plugins/cmd-log/data.db
database.file=data.db

# 日志保留天数
# 启动时会清理一次，之后每天自动清理一次过期记录
retention.days=365

# 普通查询默认返回条数
query.default-limit=100

# 允许查询的最大条数
query.max-limit=500

# 直接执行 /cmdlog 时返回的默认条数
query.default-recent-limit=10

# 是否启用 --regex 查询
query.enable-regex=true

# 额外记录的聊天前缀，多个用英文逗号分隔
# 例如 !!,##
query.custom-cmd-prefixes=!!
```
