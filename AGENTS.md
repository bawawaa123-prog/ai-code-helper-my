# AGENTS.md

本文件用于指导 Codex 在本项目中的协作方式。新的 Codex 会话开始后，应优先阅读本文件，再阅读项目根目录下的 `STEP_PROGRESS_SUMMARY.md`，然后根据当前任务按需查看代码。

如果项目中存在 `docs/CODEX_LEARNINGS.md`，新的 Codex 会话还应在阅读 `STEP_PROGRESS_SUMMARY.md` 后继续阅读该文件，用于了解历史协作经验、用户偏好、已验证的技术结论和曾经踩过的坑。

本项目希望通过“进度记录 + 协作经验沉淀”的方式，让 Codex 在多轮协作中逐步变得更了解本项目。需要注意：Codex 不会自动记住所有历史对话，只有被写入项目文件的稳定规则、项目经验和进度记录，才可以在后续会话中继续发挥作用。

---

## 1. 项目定位

本项目目标是逐步完善为一个 **AI 编程教练平台**，面向编程学习、项目准备和求职面试场景。

最终版本可以逐步包含以下方向：

- AI 流式对话与多会话管理
- RAG 知识库问答与来源引用
- AI 模拟面试、追问、评分和复盘
- 代码解释、代码审查、Bug 分析和测试用例生成
- 个性化学习路径生成
- 项目简历包装与面试讲解辅助
- 后端工程化、接口文档、部署和运行说明

注意：本文件只描述协作规则和目标方向，不记录当前已经完成的功能。当前进度统一记录在 `STEP_PROGRESS_SUMMARY.md`。

---

## 2. 当前技术栈

当前项目主要技术栈：

- 后端：Spring Boot 3、Java 21、MyBatis Plus、MySQL、JWT、Redis、LangChain4j
- 前端：Vue 3、Vite、Axios
- 数据与配置：MySQL、Redis、`application.yml`
- AI 能力：基于 LangChain4j 接入大模型能力，并已使用 Embedding / RAG 相关能力
- 构建与运行：Maven Wrapper、Vite、Node.js、Docker Redis 开发脚本

如果后续实现功能时需要引入新的技术、框架、组件库、中间件或明显改变现有技术选型，必须先询问用户，并说明：

1. 为什么需要引入；
2. 会影响哪些文件或模块；
3. 有没有更轻量的替代方案；
4. 使用后需要同步更新本 `AGENTS.md` 的技术栈说明。

---

## 3. 项目结构说明

项目根目录主要结构如下：

```text
.
├── .mvn/wrapper/          # Maven Wrapper 相关文件
├── docs/                  # 项目测试说明、接口测试说明、长期协作经验
│   ├── api-test.md
│   ├── test-checklist.md
│   └── CODEX_LEARNINGS.md # 记录长期可复用协作经验和项目记忆
├── frontend/              # Vue 3 前端项目
│   ├── src/
│   │   ├── api/           # 前端接口封装
│   │   ├── components/    # Vue 组件
│   │   ├── utils/         # 通用工具
│   │   ├── App.vue        # 前端主入口组件
│   │   ├── main.js        # 前端启动入口
│   │   └── styles.css     # 全局样式
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── src/main/java/com/yupi/aicodehelper/
│   ├── ai/                # AI 服务、RAG、记忆、工具与配置相关代码
│   ├── auth/              # 登录鉴权、JWT、用户上下文
│   ├── common/            # 通用返回、错误码等
│   ├── config/            # Spring 配置类
│   ├── controller/        # 后端接口层
│   ├── exception/         # 业务异常、全局异常处理
│   ├── mapper/            # MyBatis Plus Mapper
│   ├── model/             # Entity、DTO、VO 等模型
│   ├── service/           # 业务接口
│   └── AiCodeHelperMyApplication.java
├── src/main/resources/
│   ├── datas/             # RAG / 示例数据等资源
│   ├── docs/              # 应用内部文档资源
│   ├── sql/               # 数据库初始化或迁移 SQL
│   ├── static/            # 前端构建产物
│   └── system-prompt.txt  # 系统提示词资源
├── src/test/java/com/yupi/aicodehelper/
├── README.md
├── STEP_PROGRESS_SUMMARY.md
├── AGENTS.md
├── pom.xml
├── mvnw
├── mvnw.cmd
└── start-redis-dev.ps1
```

说明：

- `STEP_PROGRESS_SUMMARY.md` 用于记录每个 Step 的实际完成情况。
- `docs/CODEX_LEARNINGS.md` 用于记录长期可复用经验、用户偏好、已验证结论和踩坑记录。
- `AGENTS.md` 用于记录稳定、长期、全项目适用的协作规则，不记录临时想法和单次问题。

---

## 4. 代码阅读规则

不要一开始就一次性读取整个项目源码。

每次开始任务时，建议按以下顺序：

1. 阅读 `AGENTS.md`；
2. 阅读 `STEP_PROGRESS_SUMMARY.md`，确认当前完成到哪一步；
3. 如果存在 `docs/CODEX_LEARNINGS.md`，阅读该文件，确认历史协作经验、项目偏好和已验证结论；
4. 根据当前任务只查看必要文件；
5. 在修改前简要说明准备查看或修改哪些位置。

按需查看示例：

- 登录、注册、鉴权相关：优先查看 `auth/`、`UserController`、`UserService`、`UserServiceImpl`。
- 会话管理相关：优先查看 `ChatSessionController`、`ChatSessionService`、`ChatSessionServiceImpl`。
- 聊天消息相关：优先查看 `ChatMessageService`、`ChatMessageServiceImpl`、相关 DTO / VO。
- AI 流式对话相关：优先查看 `AiController`、AI 相关 service、`frontend/src/api/chat.js` 和 `frontend/src/App.vue`。
- RAG 检索和来源引用相关：优先查看 `ai/rag/`、`RagSourceVO`、`AiController`、`frontend/src/api/chat.js`、`frontend/src/App.vue` 和 `frontend/src/styles.css`。
- 知识库基础管理相关：优先查看 `KnowledgeBaseController`、`KnowledgeBaseService`、`KnowledgeBaseServiceImpl`、知识库 DTO / VO / Entity / Mapper。
- 知识库文档上传、解析、切片、向量化、检索相关：优先查看 `KnowledgeDocumentController`、`KnowledgeDocumentService`、`KnowledgeSegmentService`、`KnowledgeVectorService`、`KnowledgeSearchService` 及其实现类。
- Redis 缓存相关：优先查看 Redis 配置类、相关 service、鉴权拦截器。
- 前端页面相关：优先查看 `frontend/src/App.vue`、`frontend/src/components/`、`frontend/src/api/`、`frontend/src/utils/`、`frontend/src/styles.css`。
- 数据库结构相关：优先查看 `src/main/resources/sql/` 和对应 entity。
- 测试说明相关：优先查看 `docs/test-checklist.md` 和 `docs/api-test.md`。

---

## 5. 编码规范

编码时遵循以下原则：

1. 尽量保持现有项目结构、命名方式和代码风格一致。
2. 代码应清晰、直接、可维护，不为了炫技写复杂实现。
3. 优先做最小必要修改，不随意重构无关模块。
4. 不要因为个人偏好替换项目既有方案。
5. 不要删除旧兼容逻辑，除非用户明确要求。
6. 不要修改与当前任务无关的格式、命名或结构。
7. 后端继续保持 Controller、Service、ServiceImpl、Mapper、DTO、VO、Entity 分层清晰。
8. 前端优先复用现有 `api/`、`components/`、`utils/` 和样式结构。
9. 参数校验、异常处理、统一返回结构应尽量沿用项目已有写法。
10. 不要把真实 API Key、密码、Token、数据库密码等敏感信息写入代码或提交到配置文件中。
11. 涉及用户数据、聊天数据、知识库数据时，必须优先检查 `userId` 隔离逻辑，避免越权访问。
12. 涉及 SSE 流式响应时，必须注意前后端事件协议兼容性，不要破坏已有 `message`、`sources`、`done` 事件处理逻辑。
13. 涉及 RAG / 向量检索时，必须注意 metadata 的完整性和过滤逻辑，避免不同用户或不同知识库的数据混用。

---

## 6. 新增、删除和技术选型规则

当任务涉及以下操作时，Codex 必须先向用户确认：

1. 引入新的依赖、框架、中间件或组件库；
2. 大范围重构目录结构；
3. 修改接口路径、请求格式或返回格式，并可能影响前端兼容性；
4. 更换或新增项目主要技术栈；
5. 修改数据库表结构、字段含义或索引设计；
6. 修改缓存 key 规则、TTL 或缓存失效策略；
7. 修改 AI 调用链路、RAG 检索链路、向量库持久化方案或 Embedding 模型配置。

确认方式要求：

- 优先使用 Codex 当前环境提供的内置审批 / 权限确认机制，也就是类似 Yes / No 的确认弹窗。
- 不要用“请直接回复同意，我就继续”这种机械话术替代审批。
- 如果当前环境无法弹出内置审批窗口，则在当前会话中用自然语言说明计划，并等待用户明确确认后再继续。
- 说明计划时要简短列出：
  - 准备新增 / 删除 / 修改哪些文件；
  - 为什么需要这样做；
  - 是否会影响现有功能；
  - 是否有更小改动的替代方案。

例子：

“这一步需要新增 3 个 Entity 和 3 个 Mapper，并修改 init.sql。原因是要先建立知识库模块的数据模型。不会改 controller、service 和前端逻辑。请确认是否继续。”

特别说明：

- 如果用户在当前任务中已经明确授权某类操作，例如“可以新增文件”“可以改表结构”“可以引入依赖”，则本轮不需要重复确认。
- 如果只是修改已有文件中的小问题，且不涉及新增 / 删除 / 改表 / 新依赖 / 接口破坏性变更，可以直接进行，但需要在修改前简要说明修改范围。
- `STEP_PROGRESS_SUMMARY.md` 是每个 Step 完成后的固定进度记录文件。如果本次任务已经明确要求完成一个 Step，则完成后更新该文件不需要额外确认。
- `docs/CODEX_LEARNINGS.md` 是项目长期经验记录文件。如果用户已经明确要求创建或更新该文件，可以直接修改；否则应先说明原因并征求确认。

---

## 7. 进度记录规则

项目进度只记录在根目录下的 `STEP_PROGRESS_SUMMARY.md`。

`AGENTS.md` 不记录当前已经完成了哪些功能，也不维护 step 明细。

如果是 step 的步骤，那么每完成一个新的 step 后，更新 `STEP_PROGRESS_SUMMARY.md`，追加本次 step 的内容。推荐格式：

```md
## Step X

目标：
- 本 step 要解决的问题或实现的功能

修改文件：
- `文件路径`：新增 / 修改 / 删除内容说明

结果：
- 完成了什么能力
- 新增了什么接口、组件、配置或逻辑

验证：
- 是否编译通过
- 是否运行测试
- 是否完成接口测试或前端联调

遗留问题：
- 未完成内容
- 已知限制
- 后续建议
```

更新规则：

1. 每完成一个 step，必须把当前完成到的 step 数写清楚。
2. 每个 step 只记录实际完成内容，不提前写未来计划。
3. 如果某个 step 只完成了一部分，要明确写“部分完成”。
4. 如果修改过程中发现 bug 并修复，也要记录到对应 step。
5. 如果引入了新技术栈，除了更新 `STEP_PROGRESS_SUMMARY.md`，还要同步更新本 `AGENTS.md` 的技术栈说明。
6. 不要把长期协作经验、用户偏好或通用踩坑记录混入 `STEP_PROGRESS_SUMMARY.md`，这类内容应放入 `docs/CODEX_LEARNINGS.md`。

---

## 8. 协作进化与项目记忆规则

本项目希望 Codex 不只是完成当前任务，还能在多轮协作中逐步沉淀经验。

因此，每次完成一个明确任务或 Step 后，Codex 应判断本次协作是否产生了长期可复用经验。

长期可复用经验包括但不限于：

1. 用户反复强调的开发偏好；
2. 项目中已经验证过的技术选择；
3. 曾经踩过的坑和对应解决方案；
4. 某些文件、模块、接口之间的固定关系；
5. 后续任务必须避免的错误做法；
6. 项目中稳定存在的命名习惯、分层习惯和接口约定；
7. 已经确认不适合本项目的实现方式；
8. 用户明确说“以后都这样做”“后续都按这个规则来”的协作规则。

这些内容不应混入 `STEP_PROGRESS_SUMMARY.md` 的 step 进度里。

文件职责划分：

- `STEP_PROGRESS_SUMMARY.md`：只记录每个 Step 的完成情况、修改文件、验证结果和遗留问题。
- `docs/CODEX_LEARNINGS.md`：记录长期可复用经验、项目记忆、踩坑记录和用户偏好。
- `AGENTS.md`：只记录稳定、长期、全项目适用的最高优先级协作规则。

如果 `docs/CODEX_LEARNINGS.md` 已经存在，每次完成任务后，Codex 应在回复中说明：

1. 本次是否发现新的可复用经验；
2. 是否建议更新 `docs/CODEX_LEARNINGS.md`；
3. 是否建议把某条经验升级为 `AGENTS.md` 中的长期规则。

升级规则：

1. 临时想法、单次 bug、未验证方案，不应写入 `AGENTS.md`。
2. 单次任务中的经验，如果未来可能复用，可以先写入 `docs/CODEX_LEARNINGS.md`。
3. 当某条经验已经在 2 次以上任务中反复出现，或者用户明确说“以后都这样做”，Codex 才可以建议把它从 `docs/CODEX_LEARNINGS.md` 升级到 `AGENTS.md`。
4. 修改 `AGENTS.md` 前必须说明原因和影响范围。
5. `AGENTS.md` 不应频繁变动，避免变得臃肿、重复或包含过时信息。

---

## 9. 修改前沟通规则

开始修改前，应先用简短文字告诉用户：

1. 准备做哪一个 step 或哪一项任务；
2. 预计会查看哪些文件；
3. 预计会修改哪些已有文件；
4. 是否需要新增、删除文件或引入新技术；
5. 是否会更新 `STEP_PROGRESS_SUMMARY.md` 或 `docs/CODEX_LEARNINGS.md`。

如果任务中需要新增或删除文件，必须等待用户确认后再继续。

如果用户已经明确说“生成文件给我下载”“直接创建这个文件”“可以新增文件”，则可以按用户要求创建对应文件，但仍需在最终回复中说明新增原因和使用方式。

---

## 10. 完成后的回复格式

每完成一个 step 或一次明确修改后，按照以下格式回复用户：

```md
## 本次完成内容

简要说明本次完成的功能或修复。

## 文件变更

### 新增文件
- `文件路径`
  - 说明新增原因和主要内容

### 修改文件
- `文件路径`
  - 说明修改了哪些部分
  - 说明为什么要这样改

### 删除文件
- `文件路径`
  - 说明删除原因

## 关键逻辑

说明核心实现思路，尽量简洁清楚。

## Bug 排查

- 已检查的问题：
- 修复的问题：
- 仍可能存在的风险：

## 验证情况

- 编译 / 构建：是否执行，结果如何
- 后端接口测试：是否执行，结果如何
- 前端联调：是否执行，结果如何

## 进度文件更新

说明是否已更新 `STEP_PROGRESS_SUMMARY.md`，当前完成到 Step 几。

## 协作经验沉淀

- 本次是否产生新的长期经验：
- 是否建议更新 `docs/CODEX_LEARNINGS.md`：
- 是否建议更新 `AGENTS.md`：

## 是否需要测试

如果本次新增了功能、后端接口、数据库逻辑或前端交互，需要询问用户是否要继续进行测试或联调。
```

如果没有新增文件、删除文件或某类变更，可以写“无”。

---

## 11. 测试与验证规则

如果新增了功能、后端接口、数据库逻辑或前端交互，完成代码修改后需要询问用户是否要继续测试。

可根据情况建议以下测试方式：

- 后端编译：`mvn test`、`mvn package` 或 `mvn -DskipTests compile`
- 前端构建：在 `frontend/` 下执行 `npm run build`
- 接口测试：使用 curl、Postman、PowerShell `Invoke-RestMethod` 或前端页面联调
- 数据库验证：检查 SQL、表结构和数据写入情况
- Redis 验证：检查 key、TTL、缓存命中和缓存失效逻辑
- SSE 验证：检查 `message`、`sources`、`done` 事件是否按预期返回
- RAG / 知识库验证：检查上传、解析、切片、向量化、搜索、来源引用展示是否符合预期

如果用户要求直接测试，再执行对应测试。

已有测试文档：

- 手动验收清单：`docs/test-checklist.md`
- 接口测试示例：`docs/api-test.md`

---

## 12. 常见安全规则

1. 不要提交真实密钥、Token、密码、Cookie。
2. 不要把本地绝对路径写死到代码中。
3. 不要在生产配置中默认放开所有 CORS。
4. 不要在日志中输出敏感信息。
5. 涉及数据库变更时，应说明是否兼容旧数据。
6. 涉及缓存时，应说明缓存 key、TTL 和失效策略。
7. 涉及 AI 调用时，应注意输入长度、异常兜底和成本控制。
8. 修改配置文件时，应说明是否需要用户本地补充环境变量。
9. 涉及文件上传时，应限制文件类型、文件大小，并避免路径穿越和同名覆盖。
10. 涉及知识库、向量库、RAG 检索时，应确保 `userId`、`knowledgeBaseId`、`documentId` 等归属校验正确。
11. 涉及聊天上下文恢复时，应避免重复注入本轮用户消息，并确保不同用户、不同会话的上下文隔离。
12. 涉及前端展示 AI 内容或文档片段时，应避免直接插入不可信 HTML。

---

## 13. 当前项目协作重点

当前项目已经从基础聊天功能逐步推进到知识库和 RAG 能力。后续任务通常应优先围绕以下方向小步推进：

- 将用户知识库检索能力接入聊天链路；
- 前端增加知识库管理、文档上传、解析、向量化、检索或选择器；
- 完善 RAG 来源引用展示；
- 补充 PDF 等更多文档类型支持；
- 评估向量库持久化方案；
- 强化接口测试、前端联调和项目演示说明；
- 将项目能力整理为简历可展示亮点。

实际下一步以用户当前指令和 `STEP_PROGRESS_SUMMARY.md` 的最新记录为准，不要提前实现未来 Step。

---

## 14. 协作目标

Codex 的目标不是一次性把项目重写，而是按照 step 逐步把项目做成可运行、可演示、可写进简历的完整项目。

每次任务都应尽量做到：

- 改动范围清楚；
- 文件变化清楚；
- 进度记录清楚；
- 验证结果清楚；
- 协作经验清楚；
- 后续风险清楚。
