# Style Guide

本文档定义 `CmdLog` 项目的代码风格、注释风格与 Git 提交规范。

---

## 1. 总体原则

- 优先写**容易阅读的代码**。
- 优先通过**清晰的命名**表达意图，而不是依赖注释解释代码。
- 保持实现直接、简洁，避免过度抽象。
- 在满足可读性的前提下，尽量减少无意义样板代码。

---

## 2. 命名风格

### 2.1 通用要求

- 名称应准确表达职责、用途和边界。
- 避免使用含义模糊的名称，如 `data`、`obj`、`temp`、`util`、`manager2`。
- 变量、方法、类名应尽量做到“看到名字就知道用途”。

### 2.2 推荐做法

- 优先使用完整、有语义的英文命名。
- 布尔值使用可读性强的命名，如：
  - `isConsoleSource`
  - `hasServerArgument`
  - `regexEnabled`
- 方法名应体现动作和结果，如：
  - `parseSearchCriteria`
  - `resolveDefaultServer`
  - `validateTimeRange`
  - `buildHelpMessage`
- 如果一段逻辑需要靠注释才能看懂，优先考虑：
  - 提取函数
  - 重命名变量
  - 拆分复杂条件

---

## 3. 注释风格

### 3.1 基本规则

- **注释使用中文。**
- **注释短小、简短。**
- **只有在确实必要时才写注释。**
- 能通过命名表达清楚的内容，不写注释。

### 3.2 注释适用场景

以下情况可以写注释：

- 解释“为什么这样做”，而不是“代码正在做什么”。
- 标记特殊约束、边界条件或平台行为。
- 说明某个实现是为了兼容 Velocity / SQLite / JDBC 的限制。
- 提醒后续维护者注意某个容易误用的逻辑点。

### 3.3 不推荐的注释

避免以下类型的注释：

- 逐行翻译代码含义。
- 与代码内容重复的注释。
- 明显可以通过更好命名替代的注释。
- 大段、啰嗦、解释性过强的注释。

不推荐示例：

```/dev/null/bad-comment.java#L1-4
// 判断是否有 server 参数
boolean hasServer = arguments.contains("--server");

// 返回帮助消息
return helpMessage;
```

更推荐通过命名表达：

```/dev/null/good-naming.java#L1-4
boolean hasServerArgument = arguments.contains("--server");

return buildHelpMessage();
```

### 3.4 推荐写法

推荐将注释放在“必要但不显然”的逻辑上，并保持一句话可读完。

示例：

```/dev/null/good-comment.java#L1-5
// 控制台没有当前服务器上下文，必须显式传入 --server
if (isConsoleSource && !hasServerArgument) {
    return commandMessages.consoleMustSpecifyServer();
}
```

---

## 4. 函数与结构风格

### 4.1 函数设计

- 优先使用**命名良好的小函数**代替解释性注释。
- 一个函数应尽量只做一件事。
- 函数长度保持适中，避免把解析、校验、执行、格式化全部堆在一起。
- 如果一个函数中出现多个“逻辑阶段”，优先拆成多个私有函数。

### 4.2 推荐拆分方向

以命令处理为例，优先拆分为：

- `parseArguments`
- `parseSearchCriteria`
- `validateCriteria`
- `executeSearch`
- `formatSearchResult`
- `buildHelpMessage`

### 4.3 条件表达式

- 复杂判断优先提取为布尔变量或独立函数。
- 避免写过长的嵌套 `if/else`。
- 优先使用提前返回减少缩进层级。

推荐示例：

```/dev/null/early-return.java#L1-8
if (!hasPermission(source)) {
    return commandMessages.noPermission();
}

if (isConsoleSource(source) && !criteria.hasServer()) {
    return commandMessages.consoleMustSpecifyServer();
}

return executeSearch(criteria);
```

---

## 5. 错误处理风格

- 错误提示面向用户时，使用中文。
- 错误信息应具体、可操作。
- 不要只返回笼统的“参数错误”。
- 内部异常日志可保留技术细节；面向用户的提示应保持简洁。

推荐示例：

- `参数错误：--limit 必须是 1 到 500 之间的整数`
- `控制台执行时必须指定 --server <服务器名> 或 --server all`
- `正则表达式无效：缺少右方括号`

---

## 6. 消息文本风格

- 面向命令使用者的消息统一使用中文。
- 句子尽量短。
- 优先使用直接表达，不堆砌修饰语。
- 同类消息保持语气一致。

推荐风格：

- `未找到符合条件的命令记录`
- `你没有权限使用此命令`
- `输入 /cmdlog help 查看完整帮助`

不推荐风格：

- `系统当前未能检索到与您输入条件相符合的数据记录`
- `抱歉，您暂时没有权限执行该命令`

---

## 7. Git Commit 规范

提交信息格式固定为：

```/dev/null/commit-format.txt#L1-1
type: summary
```

要求：

- `type` 使用英文小写。
- `summary` 使用中文。
- `summary` 简短、明确，直接说明本次改动。
- 不要写多行长说明作为提交标题。

允许的 `type`：

- `feat`
- `fix`
- `chore`
- `refactor`

示例：

```/dev/null/commit-examples.txt#L1-4
feat: 添加命令日志查询参数解析
fix: 修复控制台未指定服务器时报错缺失问题
refactor: 拆分命令查询结果格式化逻辑
chore: 补充项目设计与风格文档
```
