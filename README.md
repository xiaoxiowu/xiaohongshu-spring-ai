# xiaohongshu-spring-ai

## 技术路线

目标：先搭建一个 JDK 17 可运行的 Spring AI 后端工程，连接本机小红书 MCP 服务，并预留 MySQL + MyBatis-Plus 的业务数据层。

1. 基础框架：Spring Boot 3.5.x + Maven + JDK 17
   - Spring Boot 3.5.x 支持 JDK 17，并保持 IntelliJ 当前 JUnit5 runner 兼容。
   - Maven 便于后续接入 Spring AI、MyBatis-Plus、MySQL 驱动。

2. AI 能力：Spring AI MCP Client
   - 小红书 EXE 已经作为 MCP Server 在本机运行。
   - 当前项目通过 Streamable HTTP 作为 MCP Client 连接 `http://localhost:18060/mcp`。

3. 模型能力：Spring AI OpenAI 兼容接口 + DeepSeek
   - DeepSeek 提供 OpenAI 兼容 API。
   - 项目通过 `spring-ai-starter-model-openai` 接入 DeepSeek。
   - API Key 支持通过环境变量 `DEEPSEEK_API_KEY` 注入。

4. 数据库：MySQL 8.x
   - 用于保存用户、任务、笔记草稿、采集结果、生成记录等业务数据。
   - 建表 SQL 放在 `src/main/resources/db/schema.sql`。

5. ORM：MyBatis-Plus
   - 少写 CRUD 样板代码。
   - Mapper 放在 `mapper` 包，实体放在 `entity` 包。

6. 初始化验收标准
   - `mvn test` 能通过。
   - `mvn spring-boot:run` 能启动。
   - 小红书 MCP EXE 运行时，应用可按配置连接 MCP Server。

## 项目结构

```text
src/main/java/com/example/xiaohongshuai
├── XiaohongshuAiApplication.java
├── controller
│   ├── ChatController.java
│   ├── HealthController.java
│   ├── NoteDraftController.java
│   ├── PublishTaskController.java
│   └── TopicController.java
├── entity
│   ├── AiGenerationLog.java
│   ├── NoteDraft.java
│   ├── PublishTask.java
│   ├── Topic.java
│   └── User.java
├── mapper
│   ├── AiGenerationLogMapper.java
│   ├── NoteDraftMapper.java
│   ├── PublishTaskMapper.java
│   ├── TopicMapper.java
│   └── UserMapper.java
├── service
└── resources/db/schema.sql
```

## 数据库表

初始化 SQL 在 `src/main/resources/db/schema.sql`，包含：

- `topic`：选题
- `note_draft`：笔记草稿
- `ai_generation_log`：DeepSeek 生成记录
- `publish_task`：MCP 发布任务记录
- `mcp_tool_snapshot`：MCP 工具快照
- `xhs_account`：小红书账号连接状态

## 启动前准备

1. 启动小红书 MCP 服务：

```powershell
cd E:\workspace
.\xiaohongshu-mcp-windows-amd64.exe
```

2. 创建 MySQL 数据库：

```sql
CREATE DATABASE xiaohongshu_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行 `src/main/resources/db/schema.sql` 创建业务表。

3. 设置数据库环境变量，或直接修改 `src/main/resources/application.yml`：

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="xiaohongshu_ai"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的密码"
```

4. 设置 DeepSeek API Key：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
```

## 本地运行

```powershell
cd E:\workspace\xiaohongshu-spring-ai
mvn test
mvn spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/api/health
```

DeepSeek 对话：

```http
POST http://localhost:8080/api/chat
Content-Type: application/json

{
  "message": "你好，介绍一下你自己"
}
```

创建选题：

```http
POST http://localhost:8080/api/topics
Content-Type: application/json

{
  "title": "通勤穿搭",
  "keywords": "通勤,穿搭",
  "audience": "上班族",
  "direction": "轻松实用"
}
```

生成笔记草稿：

```http
POST http://localhost:8080/api/notes/generate
Content-Type: application/json

{
  "topicId": 1
}
```

查询和修改草稿：

```text
GET http://localhost:8080/api/notes
GET http://localhost:8080/api/notes/1
PUT http://localhost:8080/api/notes/1
```

MCP 工具和账号中心：

```text
GET http://localhost:8080/api/mcp/tools
POST http://localhost:8080/api/mcp/tools/sync
GET http://localhost:8080/api/xhs/account/status
GET http://localhost:8080/api/xhs/account/qrcode
POST http://localhost:8080/api/xhs/account/reset
```

发布草稿并查询发布任务：

```http
POST http://localhost:8080/api/notes/1/publish
Content-Type: application/json

{
  "type": "image",
  "images": ["E:/workspace/images/demo.jpg"],
  "scheduleAt": null,
  "visibility": "公开可见",
  "isOriginal": false
}
```

```text
GET http://localhost:8080/api/publish-tasks/1
```

