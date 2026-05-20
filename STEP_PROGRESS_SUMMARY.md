# Step 进度汇总

本文件用于记录当前项目已经做到哪一步，以及每一步的主要产出。新的 Codex 会话开始后，建议先阅读本文件。

## 当前进度

当前已完成到：`Step 23`

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
