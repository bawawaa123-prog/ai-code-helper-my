# CODEX_LEARNINGS.md

本文件用于记录本项目中长期可复用的协作经验、项目偏好、踩坑记录和已验证结论。

新的 Codex 会话开始后，应先阅读：

1. `AGENTS.md`
2. `STEP_PROGRESS_SUMMARY.md`
3. `docs/CODEX_LEARNINGS.md`

其中：

- `AGENTS.md` 记录最高优先级、长期稳定的协作规则；
- `STEP_PROGRESS_SUMMARY.md` 记录每个 Step 的实际完成情况；
- `docs/CODEX_LEARNINGS.md` 记录项目协作过程中沉淀出的经验、偏好、踩坑和已验证结论。

本文件不是任务计划表，不提前写未来功能；也不是进度汇总，不重复维护每个 Step 的详细产出。

---

## 1. 用户长期偏好

- 用户希望项目按 **Step by Step 小步迭代** 的方式推进。
- 每次任务应先分析当前状态，再给出明确的小步修改建议或 Codex 提示词。
- 不要一次性大改，不要提前实现下一步，不要修改与当前任务无关的文件。
- 如果用户明确说“项目代码更新了”，回答项目前需要先到 GitHub 查看用户问题涉及到的最新代码文件。
- 用户希望这个项目最终可以作为可运行、可演示、可写进简历的 AI 编程教练平台。
- 用户希望 Codex 能随着多轮对话逐步进步，因此需要把稳定经验沉淀到本文件或在必要时升级到 `AGENTS.md`。

---

## 2. 项目当前稳定认知

- 项目名称：`ai-code-helper-my`。
- 项目定位：AI 编程小助手 / AI 编程教练平台。
- 后端技术栈：Spring Boot 3、Java 21、MyBatis Plus、MySQL、JWT、Redis、LangChain4j。
- 前端技术栈：Vue 3、Vite、Axios。
- 前端开发端口：Vite 使用 `5173`，`/api` 代理到后端 `http://127.0.0.1:8081`。
- 前端构建产物输出到：`src/main/resources/static`。
- 项目已有文档：
  - `README.md`
  - `STEP_PROGRESS_SUMMARY.md`
  - `docs/test-checklist.md`
  - `docs/api-test.md`
- 当前项目进度以 `STEP_PROGRESS_SUMMARY.md` 为准。生成本文件时，GitHub 上的进度记录显示已完成到 `Step 33`。

---

## 3. 项目结构约定

后端主要结构：

- `controller/`：接口层。
- `service/`：业务接口。
- `service/impl/`：业务实现。
- `mapper/`：MyBatis Plus Mapper。
- `model/entity/`：数据库实体。
- `model/dto/`：请求参数对象。
- `model/vo/`：返回视图对象。
- `auth/`：登录鉴权、JWT、用户上下文。
- `common/`：通用返回、错误码等。
- `exception/`：业务异常、全局异常处理。
- `ai/`：AI 服务、RAG、记忆、工具和相关配置。
- `config/`：Spring 配置类。

前端主要结构：

- `frontend/src/api/`：前端接口封装。
- `frontend/src/components/`：Vue 组件。
- `frontend/src/utils/`：通用请求工具等。
- `frontend/src/App.vue`：当前主页面核心逻辑。
- `frontend/src/styles.css`：当前全局样式。

文档结构：

- `STEP_PROGRESS_SUMMARY.md`：Step 完成情况。
- `docs/api-test.md`：接口测试说明。
- `docs/test-checklist.md`：手动验收清单。
- `docs/CODEX_LEARNINGS.md`：长期项目经验和协作记忆。

---

## 4. 已验证项目能力摘要

以下内容来自项目当前进度记录和仓库结构，用于帮助后续 Codex 快速理解上下文。详细状态仍以 `STEP_PROGRESS_SUMMARY.md` 为准。

### 用户与鉴权

- 已实现用户注册、登录、JWT 生成。
- 已实现 JWT 鉴权拦截器。
- 已实现当前登录用户接口 `/api/user/me`。
- Redis 已用于缓存登录用户信息。
- 登录用户缓存 key：`login:user:{userId}`。
- 登录用户缓存 TTL：7 天。

### 聊天会话与消息

- 已实现聊天会话新建、列表查询、重命名、逻辑删除。
- 已实现会话历史消息查询。
- 已实现基于 `sessionId` 的 POST SSE 流式聊天接口。
- 已实现 user / assistant 消息持久化。
- 已实现聊天后更新会话摘要。
- AI 记忆隔离 key 使用：`userId:sessionId`。
- 历史上下文恢复时，从 MySQL 查询最近消息并恢复到 Memory。
- 恢复历史上下文时需避免本轮用户消息重复进入 Memory。

### Redis 缓存

- 登录用户缓存：`login:user:{userId}`，TTL 7 天。
- 会话列表缓存：`chat:sessions:{userId}`，TTL 30 分钟。
- 会话新增、重命名、删除、发送消息后，需要注意会话列表缓存失效。

### RAG 与来源引用

- 已有 RAG 来源结果封装：`RagSourceVO`。
- `RagSourceVO` 字段包括：`sourceName`、`content`、`score`、`metadata`。
- SSE 协议已经扩展：
  - 普通文本：`event: message`
  - 来源引用：`event: sources`
  - 结束标记：`event: done`
- 前端 `chat.js` 已兼容 `message`、`sources`、`done` 事件。
- 前端 `App.vue` 已支持在 assistant 消息下方展示来源卡片。
- 来源卡片支持折叠 / 展开。
- 来源折叠状态只保存在前端临时内存，不做持久化。
- 来源内容不拼接进回答正文，也不持久化到历史消息中。

### 知识库模块

- 已新增知识库相关三张表：
  - `knowledge_base`
  - `knowledge_document`
  - `knowledge_segment`
- 已新增对应 Entity 和 Mapper。
- 已实现知识库基础管理接口：创建、列表、重命名、删除。
- 已实现 Markdown / TXT 文档上传和文档列表查询。
- 上传文件只允许 `.md` 和 `.txt`。
- 单文件大小限制为 5MB。
- 原始文件保存目录：`data/knowledge/{userId}/{knowledgeBaseId}/`。
- 实际保存文件名使用 UUID，避免同名覆盖和路径穿越。
- 已实现 Markdown / TXT 文档解析、文本切片、切片入库和切片列表查询。
- 切片规则：优先按段落切分，过长段落再按长度拆分。
- `segmentIndex` 从 0 开始稳定递增。
- `tokenCount` 当前使用 `content.length()` 近似。
- 解析前会将当前文档旧切片逻辑删除，再保存新切片。
- 已实现文档切片向量化。
- 向量化复用项目现有 `EmbeddingModel qwenEmbeddingModel` 和 `EmbeddingStore<TextSegment> embeddingStore`。
- 向量化 metadata 包含：
  - `userId`
  - `knowledgeBaseId`
  - `documentId`
  - `segmentId`
  - `fileName`
  - `fileType`
  - `segmentIndex`
- 向量写入成功后，会回写 `knowledge_segment.vectorId`，并更新切片和文档状态。
- 当前重新向量化会覆盖数据库中的最新 `vectorId`，但不会删除旧的向量库记录。
- 已实现用户知识库相似度检索接口：`POST /api/knowledge/base/{knowledgeBaseId}/search`。
- 知识库检索请求 DTO 字段包括：`query`、`maxResults`、`minScore`。
- 检索结果统一映射为 `List<RagSourceVO>`。
- 检索时需要同时依赖 metadata 过滤和 Java 代码二次归属校验，确保只返回当前用户、当前知识库的数据。

---

## 5. 踩坑记录

### 5.1 DashScope 工具消息协议与历史上下文恢复

问题：

- 恢复历史上下文后，如果聊天主链路绑定工具消息，可能出现 duplicated message 相关警告或协议兼容问题。

已知处理：

- 当前聊天主链路已经移除 `InterviewQuestionTool` 绑定，以兼容 DashScope 工具消息协议。

后续避免方式：

- 修改 AI 主链路或重新接入工具调用前，必须先检查历史上下文恢复逻辑和 DashScope 消息协议兼容性。
- 不要随意把工具调用重新绑定到主聊天链路。

### 5.2 历史上下文恢复中的重复消息

问题：

- 如果先保存本轮用户消息，再从数据库恢复最近历史，可能导致本轮用户消息重复进入 Memory。

已知处理：

- 当前流程应保持：先根据 `userId:sessionId` 恢复最近历史消息到 Memory，再保存本轮 user 消息，再调用 AI 流式回复。

后续避免方式：

- 修改 `POST /api/ai/chat/stream` 时，要重点检查 Memory 恢复顺序。
- 不要让本轮用户消息重复进入 Memory。

### 5.3 SSE 事件协议兼容

问题：

- 前端聊天流式解析依赖后端 SSE 事件协议。
- 如果后端随意改变事件名称或 data 格式，可能导致前端把来源 JSON 拼进回答文本，或无法识别流结束。

已知处理：

- 普通文本使用 `event: message`。
- 来源引用使用 `event: sources`。
- 结束标记使用 `event: done`。

后续避免方式：

- 修改 SSE 相关逻辑时，必须同时检查 `AiController` 和 `frontend/src/api/chat.js`。
- 不要破坏旧的无 `event` chunk 兼容逻辑。

### 5.4 知识库向量重新生成不会删除旧向量

问题：

- 当前重新向量化时会覆盖数据库中的最新 `vectorId`，但不会删除旧的向量库记录。

影响：

- 如果向量库是长期持久化的，旧向量可能造成重复检索或脏数据。

后续避免方式：

- 在切换向量库持久化方案或实现删除文档 / 重新向量化增强逻辑时，需要补充旧向量清理策略。
- 不要只更新数据库状态而忽略向量库中的旧数据。

### 5.5 文件上传路径与类型限制

问题：

- 文档上传涉及本地文件系统，容易出现同名覆盖、路径穿越、超大文件和不支持格式等问题。

已知处理：

- 当前只允许 `.md` 和 `.txt`。
- 单文件大小限制为 5MB。
- 原始文件保存到 `data/knowledge/{userId}/{knowledgeBaseId}/`。
- 实际保存文件名使用 UUID。

后续避免方式：

- 新增 PDF、DOCX 或其他格式前，应先确认解析方案、文件大小限制和安全校验。
- 不要直接使用用户上传文件名作为本地真实保存文件名。

---

## 6. 后续任务常用检查清单

### 修改后端接口前

- 是否需要登录鉴权？
- 是否需要从 `LoginUserHolder` 获取当前用户？
- 是否校验了资源归属 `userId`？
- 是否沿用了 `BaseResponse` / `ResultUtils` / `ErrorCode`？
- 是否需要新增 DTO / VO？
- 是否会影响前端请求封装？
- 是否需要更新 `docs/api-test.md`？

### 修改数据库前

- 是否需要兼容旧数据？
- 是否需要新增索引？
- 是否需要更新 Entity 字段？
- 是否需要更新 Mapper 或 Service 查询条件？
- 是否影响已有 Step 的功能？
- 是否需要在 `STEP_PROGRESS_SUMMARY.md` 中记录？

### 修改前端前

- 是否优先复用 `frontend/src/api/` 中已有封装？
- 是否需要新增组件到 `frontend/src/components/`？
- 是否需要同步修改 `styles.css`？
- 是否会影响登录态、会话切换、历史消息恢复？
- 是否会破坏 SSE 流式聊天展示？
- 是否需要执行 `npm run build`？

### 修改 RAG / 知识库前

- 是否校验当前用户和知识库归属？
- 是否需要过滤 `isDelete = 0`？
- 是否需要检查文档 / 切片状态？
- metadata 是否包含后续检索需要的字段？
- 向量库检索后是否做了 Java 代码二次归属校验？
- 是否会影响来源引用卡片展示？
- 是否需要更新测试文档？

### 修改缓存前

- 缓存 key 是否和已有规则一致？
- TTL 是否合理？
- 数据变更后是否删除或刷新缓存？
- Redis 未命中时是否能回退 MySQL？
- 是否会出现不同用户之间缓存串数据？

---

## 7. 不建议采用的方案

- 不建议一次性重构大量目录或跨模块大改。
- 不建议为了某个单点功能引入大型新框架。
- 不建议把来源引用内容拼接进 assistant 正文。
- 不建议把 RAG 来源卡片状态持久化到后端，除非用户明确要求。
- 不建议在未确认持久化方案前大改向量库实现。
- 不建议绕过 `userId` 归属校验直接按 id 查询知识库、文档、切片或会话。
- 不建议在配置文件中提交真实 API Key、数据库密码、JWT 密钥。
- 不建议直接使用本地绝对路径写死运行命令或文件保存路径。

---

## 8. 经验升级规则

当本文件中的某条经验满足以下条件之一，可以建议升级到 `AGENTS.md`：

1. 已经在 2 次以上任务中反复出现；
2. 用户明确说“以后都这样做”；
3. 它是全项目长期适用的协作规则；
4. 它影响多个模块或后续大部分任务。

不应升级到 `AGENTS.md` 的内容：

- 单次 bug；
- 临时调试结论；
- 未验证方案；
- 只适用于某个很小任务的实现细节；
- 未来可能很快变化的计划。

升级前，Codex 应先说明：

- 准备升级哪条经验；
- 为什么它已经足够稳定；
- 升级后会影响哪些后续任务；
- 是否需要用户确认。

---

## 9. 后续可继续补充的内容

后续每次完成明确 Step 后，可以根据情况追加以下内容：

- 新增的稳定用户偏好；
- 新增的项目约定；
- 新增的已验证技术结论；
- 新踩坑和修复方式；
- 被证明不适合本项目的方案；
- 需要升级到 `AGENTS.md` 的长期规则候选。
