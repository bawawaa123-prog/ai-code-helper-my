# Step 进度汇总

本文件用于记录当前项目已经做到哪一步，以及每一步的主要产出。新的 Codex 会话开始后，建议先阅读本文件。

## 当前进度

当前已完成到：`Step 29`

## Step 1

目标：
- 添加后端依赖和基础配置，为 MySQL、MyBatis Plus、JWT、参数校验、密码加密做准备

结果：
- 已完成相关依赖和基础配置准备

## Step 2

目标：
- 创建数据库初始化 SQL 文件

结果：
- 新增 `src/main/resources/sql/init.sql`
- 创建数据库 `ai_code_helper`
- 创建表：
  - `user`
  - `chat_session`
  - `chat_message`

## Step 3

目标：
- 创建 Entity、Mapper，并配置 MyBatis Plus 扫描

结果：
- 新增实体：
  - `User`
  - `ChatSession`
  - `ChatMessage`
- 新增 Mapper：
  - `UserMapper`
  - `ChatSessionMapper`
  - `ChatMessageMapper`
- 启动类已配置 `@MapperScan`

## Step 4

目标：
- 新增统一响应结构和全局异常处理

结果：
- 新增通用类：
  - `BaseResponse`
  - `ResultUtils`
  - `ErrorCode`
- 新增异常处理：
  - `BusinessException`
  - `GlobalExceptionHandler`

## Step 5

目标：
- 实现用户注册功能

结果：
- 新增：
  - `UserRegisterRequest`
  - `LoginUserVO`
  - `UserService`
  - `UserServiceImpl`
  - `UserController`
- 新增接口：
  - `POST /api/user/register`

## Step 6

目标：
- 实现用户登录和 JWT 生成

结果：
- 新增：
  - `UserLoginRequest`
  - `UserLoginVO`
  - `JwtUtils`
- 新增接口：
  - `POST /api/user/login`
- JWT 中包含 `userId`、签发时间、过期时间

## Step 7

目标：
- 实现 JWT 鉴权拦截器

结果：
- 新增：
  - `LoginUserHolder`
  - `AuthInterceptor`
  - `WebMvcConfig`
- 除登录、注册等放行路径外，其余接口默认需要鉴权

## Step 8

目标：
- 实现获取当前登录用户接口 `/user/me`

结果：
- 新增接口：
  - `GET /api/user/me`
- 通过 `LoginUserHolder` 获取当前登录用户

## Step 9

目标：
- 实现聊天会话的新建和列表查询接口

结果：
- 新增：
  - `ChatSessionCreateRequest`
  - `ChatSessionVO`
  - `ChatSessionService`
  - `ChatSessionServiceImpl`
  - `ChatSessionController`
- 新增接口：
  - `POST /api/chat/session`
  - `GET /api/chat/session/list`

## Step 10

目标：
- 实现聊天会话重命名和删除

结果：
- 新增：
  - `ChatSessionUpdateRequest`
- 新增接口：
  - `PUT /api/chat/session/{sessionId}`
  - `DELETE /api/chat/session/{sessionId}`
- 删除为逻辑删除

## Step 11

目标：
- 实现查询某个会话的历史消息列表接口

结果：
- 新增：
  - `ChatMessageVO`
  - `ChatMessageService`
  - `ChatMessageServiceImpl`
- 新增接口：
  - `GET /api/chat/session/{sessionId}/messages`

## Step 12

目标：
- 新增基于 `sessionId` 的 POST 流式聊天接口

结果：
- 新增：
  - `ChatStreamRequest`
- 新增接口：
  - `POST /api/ai/chat/stream`
- 保留旧接口：
  - `GET /api/ai/chat`

## Step 13

目标：
- 在 POST 流式聊天中实现消息持久化

结果：
- `ChatMessageService` 新增保存 user / assistant 消息的方法
- `ChatSessionService` 新增聊天后更新会话摘要的方法
- 流式聊天完成或异常时，会保存 assistant 回复并更新会话 `lastMessage` / `messageCount`

## Step 14

目标：
- 将 AI 记忆从单纯 `sessionId / memoryId` 改为 `userId + sessionId` 隔离

结果：
- `@MemoryId` 已改为 `String`
- 新的 `memoryKey` 规则：
  - `userId:sessionId`
- 保留旧 GET 聊天接口兼容逻辑

## Step 15

目标：
- 新会话首次聊天后自动生成简单会话标题

结果：
- `ChatSessionService` 新增自动标题更新方法
- 当标题为空或为“新会话”时，会根据首条用户消息自动截断生成标题
- 不会覆盖用户手动重命名后的标题

## Step 16

目标：
- 新增前端 API 封装

结果：
- 新增：
  - `frontend/src/utils/request.js`
  - `frontend/src/api/auth.js`
  - `frontend/src/api/session.js`
- 扩展：
  - `frontend/src/api/chat.js`
- 支持普通请求自动带 token
- 支持新的 POST SSE 聊天接口封装

## Step 17

目标：
- 实现前端登录 / 注册界面，并支持刷新页面恢复登录

结果：
- 新增：
  - `frontend/src/components/LoginPanel.vue`
- 改造：
  - `frontend/src/App.vue`
  - `frontend/src/styles.css`
- 支持：
  - 登录 / 注册切换
  - 登录后保存 token 和用户信息
  - 刷新页面恢复登录态
  - 退出登录

## Step 18

目标：
- 实现前端左侧会话列表

结果：
- 新增：
  - `frontend/src/components/SessionSidebar.vue`
- 改造：
  - `frontend/src/App.vue`
  - `frontend/src/styles.css`
- 支持：
  - 会话列表加载
  - 新建会话
  - 切换会话
  - 重命名会话
  - 删除会话
  - 加载历史消息

## Step 19

目标：
- 前端发送消息切换为真实 `currentSessionId` + `POST /api/ai/chat/stream`

结果：
- `App.vue` 发送消息已改为调用新的 POST SSE 接口
- 发送后会刷新左侧会话列表
- 历史消息可通过数据库恢复
- 保留旧 `chat.js` 兼容方法

## Step 20

目标：
- 接入 Redis，缓存登录用户信息

结果：
- 新增 Redis 依赖和配置
- 登录成功后缓存登录用户
- `AuthInterceptor` 优先读 Redis，未命中再查 MySQL
- 缓存 key：
  - `login:user:{userId}`
- TTL：
  - 7 天

## Step 21

目标：
- 接入 Redis 缓存会话列表

结果：
- `listMySessions` 优先读 Redis，未命中查 MySQL 并写缓存
- 会话变更后会删除会话列表缓存
- 缓存 key：
  - `chat:sessions:{userId}`
- TTL：
  - 30 分钟

## Step 22

目标：
- 从 MySQL 恢复历史上下文，让用户重新打开历史会话后，AI 能参考最近聊天内容继续对话

结果：
- `ChatMessageService` 新增最近历史消息查询方法：
  - `listRecentMessages(Long userId, Long sessionId, int limit)`
- 查询规则：
  - 只查询当前 `userId + sessionId`
  - 只查询 `isDelete = 0`
  - 只查询 `status = "success"`
  - 只保留 `role = user / assistant`
  - 按 `createTime` 倒序取最近 10 条，再按正序返回
- `AiCodeHelperServiceFactory` 改造为按 `memoryKey` 复用 `ChatMemory`
- 新增能力：
  - `getOrCreateMemory(String memoryKey)`
  - `reloadMemory(String memoryKey, List<ChatMessage> messages)`
- `POST /api/ai/chat/stream` 调整为：
  - 先根据 `userId:sessionId` 恢复最近历史消息到 Memory
  - 再保存本轮 user 消息
  - 再调用 AI 流式回复
- 保持 `memoryKey` 规则不变：
  - `userId:sessionId`
- 避免了本轮用户消息重复进入 Memory
- 后续运行期修复：
  - 为兼容 DashScope 工具消息协议，当前聊天主链路已移除 `InterviewQuestionTool` 绑定，避免恢复历史上下文后出现 duplicated message 警告

## Step 23

目标：
- 补充项目测试说明和手动验收清单，方便验证用户体系、多会话、消息持久化、Redis 缓存和历史上下文恢复

结果：
- 新增文档：
  - `docs/test-checklist.md`
  - `docs/api-test.md`
- `test-checklist.md` 覆盖模块：
  - 用户模块
  - 会话模块
  - 消息模块
  - AI 聊天模块
  - Redis 模块
  - 前端模块
- `api-test.md` 覆盖接口：
  - `POST /api/user/register`
  - `POST /api/user/login`
  - `GET /api/user/me`
  - `POST /api/chat/session`
  - `GET /api/chat/session/list`
  - `GET /api/chat/session/{sessionId}/messages`
  - `PUT /api/chat/session/{sessionId}`
  - `DELETE /api/chat/session/{sessionId}`
  - `POST /api/ai/chat/stream`
- 补充了 `Authorization: Bearer <token>` 请求头示例
- 补充了 Windows PowerShell 下的 `Invoke-RestMethod` / `curl.exe` 示例
- `README.md` 新增“测试与验收”入口，指向上述两个文档

## Step 24

目标：
- 封装当前 RAG 检索来源结果，为后续“回答下方展示参考来源引用”做准备

结果：
- 新增 `src/main/java/com/yupi/aicodehelper/model/vo/RagSourceVO.java`
- `RagSourceVO` 包含字段：
  - `sourceName`
  - `content`
  - `score`
  - `metadata`
- 新增 `src/main/java/com/yupi/aicodehelper/ai/rag/RagQueryService.java`
- `RagQueryService` 复用当前 `RagConfig` 提供的 `ContentRetriever`，接收用户问题后执行检索，并返回 `List<RagSourceVO>`
- 检索结果映射时会优先从 metadata 中提取来源文件名，并补充相似度分数
- 当 RAG 检索不到内容或检索异常时，返回空列表，不影响现有聊天主流程
- `RagConfig` 仅补充了来源 metadata key 常量复用，未改变现有 RAG 检索参数、聊天接口或 SSE 行为
- 后端已执行 `mvn -DskipTests compile`，结果为 `BUILD SUCCESS`

## Step 25

目标：
- 扩展 RAG 聊天 SSE 事件协议，在不展示前端来源卡片的前提下，让后端可以在 RAG 回答结束后返回 `sources` 事件

结果：
- 修改 `src/main/java/com/yupi/aicodehelper/controller/AiController.java`
- `POST /api/ai/chat/stream` 的普通文本 chunk 显式使用 `event: message`
- 当 `finalUseRag = true` 且本轮流式回复正常完成后，后端会追加返回：
  - `event: sources`
  - `data: List<RagSourceVO>` 的 JSON
- 在流结束时追加返回：
  - `event: done`
  - `data: done`
- 客户端主动停止生成时，不强行发送 `sources` 或 `done`
- `AiController` 接入 `RagQueryService`，仅在 RAG 模式下用本轮用户 `message` 查询来源
- 检索失败或为空时，`sources` 返回空数组，不影响 assistant 消息持久化和会话摘要更新逻辑
- 修改 `frontend/src/api/chat.js`
- 前端底层 SSE 解析已兼容 `message`、`sources`、`done` 事件：
  - `message` 或无 `event` 时继续走原有 `onChunk(data)`
  - `sources` 时仅在调用方传入 `onSources` 后回调，不会被拼接到回答文本
  - `done` 时仅在调用方传入 `onDone` 后回调
- 当前 `App.vue` 未修改，现有聊天页面行为保持不变
- 已更新 `STEP_PROGRESS_SUMMARY.md` 到 Step 25

## Step 26

目标：
- 前端展示 RAG 来源引用卡片，让 RAG 模式回答结束后在 AI 消息下方显示参考来源

结果：
- 修改 `frontend/src/App.vue`
- 前端消息结构新增 `sources` 字段：
  - `createMessage` 默认带 `sources: []`
  - 默认欢迎消息的 `sources` 为空
  - 历史消息转换后的 `sources` 也为空
- 在 `sendMessage` 调用 `streamChatBySession` 时接入 `onSources`
- `onSources(sources)` 只会把来源结果写入当前这轮的 `assistantMessage.sources`
- `sources` 不会拼接进回答正文，也不会持久化到历史消息中
- 仅当 assistant 消息存在非空 `sources` 数组时，在回答气泡下方展示“参考来源”卡片
- 来源卡片展示内容包括：
  - `sourceName`
  - `content`
  - `score`
- `sourceName` 为空时显示“未知来源”
- `content` 为空时不展示空内容
- 修改 `frontend/src/styles.css`
- 已补充来源卡片样式，整体风格与现有 assistant 气泡保持一致
- 非 RAG 模式或 `sources` 为空数组时，不展示来源卡片
- 已更新 `STEP_PROGRESS_SUMMARY.md` 到 Step 26

## Step 27

目标：
- 优化 RAG 来源引用卡片交互，将参考来源内容默认折叠，用户点击后再展开查看具体片段内容

结果：
- 修改 `frontend/src/App.vue`
- 前端消息结构新增 `sourcesExpanded` 字段：
  - `createMessage` 默认带 `sourcesExpanded: false`
  - 默认欢迎消息的 `sourcesExpanded` 为 `false`
  - 历史消息转换后的 `sourcesExpanded` 也为 `false`
- 参考来源入口默认折叠，仅显示“参考来源（数量）”
- 用户点击后可以展开来源列表，再次点击可以收起
- 每条 assistant 消息的来源卡片独立控制展开 / 收起，互不影响
- 折叠状态仅保存在前端临时内存中，不做持久化，不写入后端或 localStorage
- 展开后继续沿用 Step 26 的展示逻辑：
  - `sourceName`
  - `content`
  - `score`
- `sourceName` 为空时显示“未知来源”
- `content` 为空时不展示空内容
- 修改 `frontend/src/styles.css`
- 已优化折叠态样式和可点击状态，不影响普通 AI 消息和用户消息样式
- 本 Step 仅做 RAG 来源卡片折叠交互优化，未修改 SSE 协议、`chat.js` 或知识库管理逻辑
- 已更新 `STEP_PROGRESS_SUMMARY.md` 到 Step 27

## Step 28

目标：
- 新增知识库、知识库文档、知识库切片的数据表结构与后端基础模型，为后续文档上传、解析切片和向量化检索做准备

结果：
- 修改 `src/main/resources/sql/init.sql`
- 新增知识库相关三张表：
  - `knowledge_base`
  - `knowledge_document`
  - `knowledge_segment`
- 三张表统一保留：
  - `user_id`
  - `status`
  - `create_time`
  - `update_time`
  - `is_delete`
- `knowledge_document` 通过 `knowledge_base_id` 关联知识库
- `knowledge_segment` 通过 `document_id` 关联知识文档
- `knowledge_segment.content` 使用 `MEDIUMTEXT`
- `knowledge_segment.metadata` 使用 `TEXT`
- `knowledge_segment.vector_id` 使用 `VARCHAR(128)`
- 已补充索引：
  - `knowledge_base.idx_user_id_update_time`
  - `knowledge_document.idx_base_id_update_time`
  - `knowledge_document.idx_user_id_update_time`
  - `knowledge_segment.idx_document_id_segment_index`
  - `knowledge_segment.idx_base_id_document_id`
- 新增 Entity：
  - `KnowledgeBase`
  - `KnowledgeDocument`
  - `KnowledgeSegment`
- 新增 Mapper：
  - `KnowledgeBaseMapper`
  - `KnowledgeDocumentMapper`
  - `KnowledgeSegmentMapper`
- 本 Step 仅完成知识库相关表结构、Entity、Mapper，未新增接口，未实现文档上传、切片解析、embedding 或向量化检索逻辑

## Step 29

目标：
- 实现知识库基础管理接口，支持当前登录用户创建、查询、重命名和删除自己的知识库

结果：
- 已检查并确认 Step 28 内容完整：
  - `init.sql` 中已存在 `knowledge_base`、`knowledge_document`、`knowledge_segment`
  - `KnowledgeBase`、`KnowledgeDocument`、`KnowledgeSegment` Entity 已存在
  - `KnowledgeBaseMapper`、`KnowledgeDocumentMapper`、`KnowledgeSegmentMapper` 已存在
  - `STEP_PROGRESS_SUMMARY.md` 已记录 Step 28
- 新增 DTO：
  - `KnowledgeBaseCreateRequest`
  - `KnowledgeBaseUpdateRequest`
- 新增 VO：
  - `KnowledgeBaseVO`
- 新增 Service：
  - `KnowledgeBaseService`
  - `KnowledgeBaseServiceImpl`
- 新增 Controller：
  - `KnowledgeBaseController`
- 新增接口：
  - `POST /api/knowledge/base`
  - `GET /api/knowledge/base/list`
  - `PUT /api/knowledge/base/{knowledgeBaseId}`
  - `DELETE /api/knowledge/base/{knowledgeBaseId}`
- 所有知识库操作均按 `userId` 隔离：
  - 只允许当前登录用户操作自己的知识库
  - 查询列表仅返回当前用户且未删除的数据
  - 删除采用逻辑删除
  - 列表按 `updateTime` 倒序返回
- 本 Step 仅实现知识库基础管理接口，未实现文档上传、文档解析、切片处理、embedding、向量化检索和聊天接入
