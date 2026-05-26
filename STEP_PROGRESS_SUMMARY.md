# Step 进度汇总

本文件用于记录当前项目已经做到哪一步，以及每一步的主要产出。新的 Codex 会话开始后，建议先阅读本文件。

## 当前进度

当前已完成到：`Step 39`

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

## Step 30

目标：
- 实现 Markdown / TXT 文档上传接口，将文档原始文件保存到本地，并把文档元信息写入 `knowledge_document` 表

结果：
- 新增 VO：
  - `KnowledgeDocumentVO`
- 新增 Service：
  - `KnowledgeDocumentService`
  - `KnowledgeDocumentServiceImpl`
- 新增 Controller：
  - `KnowledgeDocumentController`
- 新增接口：
  - `POST /api/knowledge/base/{knowledgeBaseId}/document/upload`
  - `GET /api/knowledge/base/{knowledgeBaseId}/document/list`
- 上传文件仅允许：
  - `.md`
  - `.txt`
- 单文件大小限制为 5MB
- 原始文件会保存到本地目录：
  - `data/knowledge/{userId}/{knowledgeBaseId}/`
- 实际保存文件名使用 UUID 生成，避免同名覆盖和路径穿越
- `knowledge_document` 入库字段包括：
  - `userId`
  - `knowledgeBaseId`
  - `fileName`
  - `fileType`
  - `filePath`
  - `fileSize`
  - `segmentCount = 0`
  - `status = 1`
- 查询文档列表只返回当前用户、当前知识库、未删除的文档，并按 `updateTime` 倒序
- 本 Step 仅实现 Markdown / TXT 文档上传、原始文件保存和文档列表查询
- 未实现文档解析、切片、embedding、向量化、聊天接入和前端页面改动

## Step 31

目标：
- 实现 Markdown / TXT 文档解析与文本切片入库，将已上传文档解析成多个 `knowledge_segment` 记录，为后续 embedding 和向量化检索做准备

结果：
- 已检查 Step 30 进度记录，确认已完整记录：
  - 已实现 Markdown / TXT 文档上传
  - 已实现文档本地保存
  - 已实现 `knowledge_document` 元信息入库
  - 已实现文档列表查询
  - 未实现解析、切片、embedding、向量化和聊天接入
- 新增 VO：
  - `KnowledgeSegmentVO`
- 新增 Service：
  - `KnowledgeSegmentService`
  - `KnowledgeSegmentServiceImpl`
- 扩展 `KnowledgeDocumentController`
- 新增接口：
  - `POST /api/knowledge/base/{knowledgeBaseId}/document/{documentId}/parse`
  - `GET /api/knowledge/base/{knowledgeBaseId}/document/{documentId}/segment/list`
- 解析规则：
  - 使用 UTF-8 读取 Markdown / TXT 原始文件
  - 统一换行、去掉首尾空白、过滤空段落
  - 优先按段落切分
  - 过长段落再按长度继续拆分
  - `segmentIndex` 从 0 开始稳定递增
  - `tokenCount` 使用 `content.length()` 近似
- 切片入库规则：
  - 解析前先将当前文档旧切片逻辑删除
  - 再批量保存新的 `knowledge_segment`
  - `vectorId = null`
  - `metadata` 保存 `fileName`、`fileType`、`segmentIndex`
- 解析完成后会更新：
  - `knowledge_document.segmentCount`
  - `knowledge_document.status = 2`
- 查询切片列表只返回当前用户、当前知识库、当前文档、未删除的切片，并按 `segmentIndex` 升序返回
- 本 Step 仅实现 Markdown / TXT 文档解析、文本切片、切片入库和切片列表查询
- 未实现 embedding、向量库持久化、聊天接入、PDF 解析和前端页面改动

## Step 32

目标：
- 实现知识库文档切片向量化，将已解析的 `knowledge_segment` 写入当前项目已有 `EmbeddingStore`
- 为每个切片回写 `vectorId`，并更新文档与切片状态，为后续相似度检索和聊天接入做准备

修改文件：
- `src/main/java/com/yupi/aicodehelper/service/KnowledgeVectorService.java`：新增知识库文档向量化 Service 接口
- `src/main/java/com/yupi/aicodehelper/service/impl/KnowledgeVectorServiceImpl.java`：新增向量化实现，完成权限校验、切片查询、metadata 构建、embedding 生成、EmbeddingStore 写入、`vectorId` 回写和状态更新
- `src/main/java/com/yupi/aicodehelper/controller/KnowledgeDocumentController.java`：新增文档向量化接口
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 32 完成记录

结果：
- 新增接口：
  - `POST /api/knowledge/base/{knowledgeBaseId}/document/{documentId}/vectorize`
- 复用项目现有 Spring Bean：
  - `EmbeddingModel qwenEmbeddingModel`
  - `EmbeddingStore<TextSegment> embeddingStore`
- 向量化时为每个 `TextSegment` 写入 metadata：
  - `userId`
  - `knowledgeBaseId`
  - `documentId`
  - `segmentId`
  - `fileName`
  - `fileType`
  - `segmentIndex`
- 向量写入成功后：
  - 回写 `knowledge_segment.vectorId`
  - 更新 `knowledge_segment.status = 2`
  - 更新 `knowledge_document.status = 3`
- 保持 `knowledge_document.segmentCount` 不变
- 当前重新向量化时会覆盖数据库中的最新 `vectorId`，但不会删除旧的向量库记录

验证：
- 已按要求执行后端编译：
  - `mvn -DskipTests compile`
- 未执行前端构建
- 未执行上传 / parse / vectorize 的手动接口联调

遗留问题：
- 本 Step 未实现相似度检索
- 本 Step 未实现聊天接入
- 本 Step 未实现 PDF 解析
- 本 Step 未实现前端页面改动
- 本 Step 未实现向量库持久化方案切换

## Step 33

目标：
- 实现用户知识库相似度检索接口，根据用户问题从已向量化的 `knowledge_segment` 中检索相关片段
- 返回可用于后续聊天接入的参考来源结果，但本 Step 不接入聊天链路

修改文件：
- `src/main/java/com/yupi/aicodehelper/model/dto/knowledge/KnowledgeSearchRequest.java`：新增知识库检索请求 DTO
- `src/main/java/com/yupi/aicodehelper/service/KnowledgeSearchService.java`：新增知识库检索 Service 接口
- `src/main/java/com/yupi/aicodehelper/service/impl/KnowledgeSearchServiceImpl.java`：新增知识库检索实现，完成参数校验、embedding 生成、EmbeddingStore 检索、metadata 过滤和结果映射
- `src/main/java/com/yupi/aicodehelper/controller/KnowledgeSearchController.java`：新增知识库检索接口
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 33 完成记录

结果：
- 新增请求 DTO：
  - `KnowledgeSearchRequest`
  - 字段包括：`query`、`maxResults`、`minScore`
- 新增接口：
  - `POST /api/knowledge/base/{knowledgeBaseId}/search`
- 复用项目现有 Spring Bean：
  - `EmbeddingModel qwenEmbeddingModel`
  - `EmbeddingStore<TextSegment> embeddingStore`
- 检索流程：
  - 先校验当前用户和知识库归属
  - 再对用户问题生成 query embedding
  - 再调用 `EmbeddingStore.search(...)` 做相似度检索
  - 优先使用 metadata 中的 `userId`、`knowledgeBaseId` 作为过滤条件
  - 同时在 Java 代码中再次校验检索结果归属，确保最终只返回当前用户、当前知识库的数据
- 检索结果统一映射为 `List<RagSourceVO>`
  - `sourceName`：优先取 `metadata.fileName`，为空时返回“未知来源”
  - `content`：取 `TextSegment.text()`
  - `score`：取向量检索匹配分数
  - `metadata`：返回 `TextSegment.metadata().toMap()`
- 若无匹配结果，返回空列表，不抛异常

验证：
- 已执行后端编译：
  - `mvn -DskipTests compile`
- 未执行前端构建
- 未执行上传 / parse / vectorize / search 的手动接口联调

遗留问题：
- 本 Step 未接入聊天接口
- 本 Step 未实现前端页面
- 本 Step 未实现 PDF 解析
- 本 Step 未实现知识库选择器
- 本 Step 未实现向量库持久化方案切换

## Step 34

目标：
- 将用户知识库检索接入后端聊天接口，让 `POST /api/ai/chat/stream` 支持可选 `knowledgeBaseId`
- 在 RAG 模式下，基于当前用户自己的知识库片段增强回答
- 保持不传 `knowledgeBaseId` 时原有静态 docs RAG 逻辑不变

修改文件：
- `src/main/java/com/yupi/aicodehelper/model/dto/chat/ChatStreamRequest.java`：新增 `knowledgeBaseId`
- `src/main/java/com/yupi/aicodehelper/controller/AiController.java`：接入 `KnowledgeSearchService`，支持用户知识库 RAG、增强提示词拼接和 sources 事件切换
- `frontend/src/api/chat.js`：扩展 `streamChatBySession`，支持可选 `knowledgeBaseId`
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 34 完成记录

结果：
- `ChatStreamRequest` 新增字段：
  - `knowledgeBaseId`
- `POST /api/ai/chat/stream` 行为扩展为：
  - `useRag = true` 且 `knowledgeBaseId` 不为空时：
    - 调用 `KnowledgeSearchService.searchKnowledgeBase(...)`
    - 使用用户知识库检索结果拼接增强提示词
    - 调用 `aiCodeHelperServiceFactory.chatStream(..., false)`，避免混用静态 docs RAG
    - SSE `sources` 事件返回本轮用户知识库来源
  - `useRag = true` 且 `knowledgeBaseId` 为空时：
    - 保持原有静态 docs RAG 逻辑不变
    - SSE `sources` 事件继续返回 `RagQueryService` 的静态来源结果
  - `useRag = false` 时：
    - 忽略 `knowledgeBaseId`
    - 仍只返回 `done`
- 数据持久化规则保持：
  - 数据库保存的 user 消息仍为用户原始 `message`
  - 会话标题仍基于用户原始 `message`
  - assistant 回复仍按原逻辑持久化
  - 不保存增强提示词到数据库
- 前端 API 层已兼容：
  - `streamChatBySession` 支持可选 `knowledgeBaseId`
  - 当前页面不传 `knowledgeBaseId` 时现有聊天行为不变

验证：
- 已执行后端编译：
  - `mvn -DskipTests compile`
- 已执行前端构建：
  - `cd frontend && npm run build`
- 未执行手动 `curl` / SSE 联调

遗留问题：
- 本 Step 未实现前端知识库选择器
- 本 Step 未实现 PDF 解析
- 本 Step 未实现文档删除
- 本 Step 未实现知识库管理页面
- 本 Step 未实现向量库持久化方案切换

## Step 35

目标：
- 实现前端知识库列表加载和知识库选择入口
- 让聊天页面可以选择“静态内置知识库”或“我的知识库”
- 发送消息时根据当前选择携带可选 `knowledgeBaseId`

修改文件：
- `frontend/src/api/knowledge.js`：新增知识库列表 API 封装
- `frontend/src/App.vue`：新增知识库状态、知识库列表加载、知识库选择器和发送参数传递
- `frontend/src/styles.css`：补充知识库选择器样式
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 35 完成记录

结果：
- 新增前端 API：
  - `getKnowledgeBaseList()`
  - 调用 `GET /api/knowledge/base/list`
- `App.vue` 新增状态：
  - `knowledgeBases`
  - `selectedKnowledgeBaseId`
  - `knowledgeLoading`
  - `knowledgeError`
- 加载规则：
  - 用户登录成功后加载知识库列表
  - 刷新页面恢复登录态后也会加载知识库列表
  - 退出登录时会清空知识库列表和当前选择
  - 加载失败不会阻断正常聊天，只显示简单提示
- 选择器规则：
  - 仅登录后显示
  - RAG 关闭时选择器禁用
  - 默认选项是“静态内置知识库”
  - 其余选项来自“我的知识库”列表
  - 没有知识库时保留默认选项并显示提示
- 发送消息规则：
  - `useRag = true` 且选中了某个我的知识库时，发送 `knowledgeBaseId`
  - `useRag = true` 且当前选中“静态内置知识库”时，不传 `knowledgeBaseId`
  - `useRag = false` 时，不传 `knowledgeBaseId`
- 保持不变：
  - `streamChatBySession` 的 SSE 解析逻辑未改
  - `onChunk`、`onSources`、`onDone` 现有行为不变
  - 停止生成逻辑不变
  - 来源卡片展示逻辑不变

验证：
- 已执行前端构建：
  - `cd frontend && npm run build`
- 未执行手动页面联调
- 后端未修改，因此未重新编译后端

遗留问题：
- 本 Step 未实现知识库管理页面
- 本 Step 未实现文档上传页面
- 本 Step 未实现 PDF 解析
- 本 Step 未实现文档删除

## Step 36

目标：
- 实现前端知识库基础管理入口
- 支持在聊天页面创建、重命名和删除自己的知识库
- 操作完成后自动刷新知识库选择列表，并保持 Step 35 的聊天传参逻辑不变

修改文件：
- `frontend/src/api/knowledge.js`：扩展知识库前端 API，新增创建、更新、删除方法
- `frontend/src/App.vue`：新增知识库管理状态、创建表单、重命名编辑态、删除逻辑和列表刷新逻辑
- `frontend/src/styles.css`：补充知识库管理区、表单、列表和按钮样式
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 36 完成记录

结果：
- `knowledge.js` 新增前端 API：
  - `createKnowledgeBase(data)`
  - `updateKnowledgeBase(knowledgeBaseId, data)`
  - `deleteKnowledgeBase(knowledgeBaseId)`
- `App.vue` 新增知识库管理状态：
  - `newKnowledgeBaseName`
  - `newKnowledgeBaseDescription`
  - `editingKnowledgeBaseId`
  - `editingKnowledgeBaseName`
  - `editingKnowledgeBaseDescription`
  - `knowledgeActionLoading`
  - `knowledgeActionError`
- 聊天页右侧知识库区域新增基础管理入口：
  - 可直接输入名称和描述创建知识库
  - 可对已有知识库执行重命名和删除
  - 创建、更新、删除成功后会自动刷新知识库列表
- 删除当前选中的知识库后会自动切回“静态内置知识库”：
  - `selectedKnowledgeBaseId` 置空
- 保持 Step 35 的聊天传参规则不变：
  - `useRag = true` 且选中“我的知识库”时才传 `knowledgeBaseId`
  - 选择“静态内置知识库”或关闭 RAG 时不传 `knowledgeBaseId`
- 退出登录时会清空知识库列表、当前选择和本 Step 新增的管理状态

验证：
- 已执行前端构建：
  - `cd frontend && npm run build`
- 构建结果：
  - 通过
- 未执行手动页面联调
- 后端未修改，因此未重新编译后端

遗留问题：
- 本 Step 未实现文档上传页面
- 本 Step 未实现文档解析按钮
- 本 Step 未实现向量化按钮
- 本 Step 未实现 PDF 解析
- 本 Step 未实现文档删除
- 本 Step 未实现 Step 37

## Step 37

目标：
- 完成前端知识库文档管理入口
- 支持在选中的“我的知识库”下上传 Markdown / TXT 文档、查看文档列表，并手动触发解析和向量化
- 保持 Step 35 / Step 36 已有的聊天链路和知识库传参逻辑不变

修改文件：
- `frontend/src/api/knowledge.js`：新增知识库文档相关前端 API，补充上传、列表、解析、向量化请求封装
- `frontend/src/App.vue`：新增文档管理状态、上传入口、文档列表展示和解析 / 向量化操作
- `frontend/src/styles.css`：补充文档管理区域、文档列表和按钮状态样式
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 37 完成记录

结果：
- `knowledge.js` 新增前端 API：
  - `getKnowledgeDocumentList(knowledgeBaseId)`
  - `uploadKnowledgeDocument(knowledgeBaseId, file)`
  - `parseKnowledgeDocument(knowledgeBaseId, documentId)`
  - `vectorizeKnowledgeDocument(knowledgeBaseId, documentId)`
- `App.vue` 新增文档管理状态：
  - `knowledgeDocuments`
  - `documentLoading`
  - `documentActionLoading`
  - `documentError`
  - `selectedUploadFile`
- 仅当选中“我的知识库”时，右侧面板显示“文档管理”区域
- 切换到某个我的知识库后会自动加载当前知识库的文档列表
- 切回“静态内置知识库”或退出登录时，会清空文档列表、错误状态和已选上传文件
- 支持前端选择并上传：
  - `.md`
  - `.txt`
- 上传参数名固定为：
  - `file`
- 未选择文件或文件类型不符合要求时，前端会直接提示，不发起上传请求
- 文档列表展示字段包括：
  - `fileName`
  - `fileType`
  - `fileSize`
  - `segmentCount`
  - `status`
  - `updateTime / createTime`
- `status` 前端映射为：
  - `1` -> `已上传`
  - `2` -> `已解析`
  - `3` -> `已向量化`
  - 其他 -> `未知状态`
- 每条文档支持手动操作：
  - `解析`
  - `向量化`
- 解析或向量化成功后都会自动刷新文档列表
- 保持不变：
  - 未修改 `frontend/src/api/chat.js`
  - 未修改 `streamChatBySession` 调用规则
  - 未修改 `knowledgeBaseId` 发送规则
  - 未修改 `onChunk`、`onSources`、`onDone`
  - 未修改停止生成逻辑和来源卡片展示逻辑

验证：
- 已执行前端构建：
  - `cd frontend && npm run build`
- 后端未修改，因此未重新编译后端
- 未执行手动页面联调和真实上传 / 解析 / 向量化点击验证

遗留问题：
- 本 Step 未实现文档删除
- 本 Step 未实现 PDF 上传或 PDF 解析
- 本 Step 未实现切片详情页面
- 本 Step 未实现文档搜索页面
- 本 Step 未实现 Step 38

## Step 38

目标：
- 实现知识库文档删除功能
- 支持用户删除自己知识库下的文档
- 在前端文档列表中提供删除入口，并保持 Step 37 已有上传、解析、向量化能力不变

修改文件：
- `src/main/java/com/yupi/aicodehelper/service/KnowledgeDocumentService.java`：新增文档删除 Service 方法
- `src/main/java/com/yupi/aicodehelper/service/impl/KnowledgeDocumentServiceImpl.java`：补充文档归属校验、文档逻辑删除和关联切片逻辑删除
- `src/main/java/com/yupi/aicodehelper/controller/KnowledgeDocumentController.java`：新增删除文档接口
- `frontend/src/api/knowledge.js`：新增前端删除文档 API
- `frontend/src/App.vue`：新增文档删除按钮、二次确认、删除请求和列表刷新逻辑
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 38 完成记录

结果：
- `KnowledgeDocumentService` 新增方法：
  - `void deleteDocument(Long userId, Long knowledgeBaseId, Long documentId)`
- 后端新增接口：
  - `DELETE /api/knowledge/base/{knowledgeBaseId}/document/{documentId}`
- 删除流程包含校验：
  - `userId` 必须有效
  - `knowledgeBaseId` 必须有效
  - `documentId` 必须有效
  - 知识库必须存在、未删除且属于当前用户
  - 文档必须存在、未删除、属于当前用户且属于当前知识库
- 删除文档时会执行逻辑删除：
  - `knowledge_document.is_delete = 1`
  - 当前 `documentId` 下未删除的 `knowledge_segment.is_delete = 1`
- 保持不变：
  - 不物理删除本地文件
  - 不删除 EmbeddingStore 中旧向量
- 前端 `knowledge.js` 新增：
  - `deleteKnowledgeDocument(knowledgeBaseId, documentId)`
- 前端文档列表每条记录新增“删除”按钮：
  - 删除前使用 `window.confirm` 二次确认
  - 删除成功后刷新当前知识库文档列表
  - 删除失败时显示 `documentError`
  - 删除中会复用 `documentActionLoading` 禁用按钮，避免重复点击
  - 删除成功后会清空已选上传文件
- 保持不变：
  - 上传、解析、向量化按钮逻辑不变
  - 聊天发送 `knowledgeBaseId` 逻辑不变
  - 来源卡片展示逻辑不变

验证：
- 已执行后端编译：
  - `mvn -DskipTests compile`
- 已执行前端构建：
  - `cd frontend && npm run build`
- 未执行真实 `curl` 删除联调
- 未执行真实页面点击删除联调

遗留问题：
- 本 Step 未实现物理文件删除
- 本 Step 未实现向量库旧向量删除
- 本 Step 未实现 PDF 上传或 PDF 解析
- 本 Step 未实现切片详情页面
- 本 Step 未实现文档搜索页面
- 本 Step 未实现 Step 39

## Step 39

目标：
- 完成前端文档切片详情查看入口
- 支持在文档列表中查看某个文档解析后的切片内容
- 方便验证解析效果和后续 RAG 检索质量

修改文件：
- `frontend/src/api/knowledge.js`：新增切片列表查询 API
- `frontend/src/App.vue`：新增切片详情状态、“查看切片”按钮、切片加载和展开收起逻辑
- `frontend/src/styles.css`：新增切片详情区域、切片列表、长文本展开和 metadata 展示样式
- `STEP_PROGRESS_SUMMARY.md`：追加 Step 39 完成记录

结果：
- `knowledge.js` 新增前端 API：
  - `getKnowledgeSegmentList(knowledgeBaseId, documentId)`
- `App.vue` 新增切片详情状态：
  - `selectedSegmentDocumentId`
  - `knowledgeSegments`
  - `segmentLoading`
  - `segmentError`
  - `expandedSegmentIds`
- 文档列表每条记录新增“查看切片”按钮：
  - 点击后调用 `getKnowledgeSegmentList(...)`
  - 再次点击同一个文档时会收起切片详情
  - 点击其他文档时会切换显示对应文档切片
  - 未解析或 `segmentCount = 0` 的文档不会误展示切片
- 切片详情展示字段包括：
  - `segmentIndex`
  - `content`
  - `tokenCount`
  - `status`
  - `metadata`
- `content` 默认限制最大高度，可单个切片展开 / 收起
- `metadata` 通过折叠区以字符串形式展示，不做复杂格式化
- 切换知识库时会清空切片详情状态
- 删除当前正在查看切片的文档时会清空切片详情状态
- 退出登录时会清空切片详情状态
- 保持不变：
  - 未修改后端接口
  - 未修改 `frontend/src/api/chat.js`
  - 未修改 `streamChatBySession` 调用规则
  - 未修改 `knowledgeBaseId` 发送规则
  - 未修改 `onChunk`、`onSources`、`onDone`
  - 未修改上传、解析、向量化、删除接口逻辑
  - 未修改停止生成逻辑和来源卡片展示逻辑

验证：
- 已执行前端构建：
  - `cd frontend && npm run build`
- 后端未修改，因此未重新编译后端
- 未执行真实页面联调和切片点击验证

遗留问题：
- 本 Step 未实现切片编辑
- 本 Step 未实现切片删除
- 本 Step 未实现文档搜索页面
- 本 Step 未实现 PDF 上传或 PDF 解析
- 本 Step 未实现物理文件删除
- 本 Step 未实现向量库旧向量删除
- 本 Step 未实现 Step 40
