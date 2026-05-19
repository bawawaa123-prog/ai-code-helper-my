# Codex 项目协作规范

本文件用于给新的 Codex 会话做启动指引。每次开启新的 Codex 会话后，优先阅读本文件，再开始执行任务。

## 1. 项目简介

这是一个 AI 编程学习 / 求职辅助项目，当前采用前后端分离并集成静态资源的结构：

- 后端：Spring Boot 3、Java 21、MyBatis Plus、MySQL、JWT、Redis、LangChain4j
- 前端：Vue 3、Vite
- 核心功能方向：
  - 用户注册 / 登录 / 鉴权
  - 聊天会话管理
  - 聊天消息持久化
  - AI 流式对话
  - Redis 缓存

## 2. 项目结构总览

根目录主要结构：

- `src/main/java/com/yupi/aicodehelper/`
  - `controller/`：后端接口层
  - `service/`：业务服务接口
  - `service/impl/`：业务服务实现
  - `mapper/`：MyBatis Plus Mapper
  - `model/entity/`：数据库实体
  - `model/dto/`：请求参数对象
  - `model/vo/`：返回对象
  - `auth/`：JWT、拦截器、登录用户上下文
  - `common/`：统一返回结构、错误码等
  - `exception/`：业务异常、全局异常处理
  - `ai/`：AI 相关服务与配置
  - `config/`：Spring 配置类
- `src/main/resources/`
  - `application.yml`：应用配置
  - `sql/init.sql`：数据库初始化脚本
  - `static/`：前端构建产物
- `frontend/src/`
  - `api/`：前端接口封装
  - `components/`：Vue 组件
  - `utils/`：通用工具
  - `App.vue`：前端主页面
  - `styles.css`：全局样式

## 3. 阅读代码原则

新会话不要一开始就把所有代码全部展开阅读。

建议流程：

1. 先阅读本文件
2. 再阅读 `STEP_PROGRESS_SUMMARY.md`
3. 只根据当前任务，按需读取相关目录和文件
4. 不要为了“建立全局理解”一次性扫描整个项目源码

按需读取示例：

- 做登录问题：优先看 `auth/`、`UserService`、`UserController`
- 做会话问题：优先看 `ChatSessionService`、`ChatSessionController`
- 做前端聊天问题：优先看 `frontend/src/App.vue`、`frontend/src/api/`
- 做 Redis 问题：优先看 `RedisConfig`、相关 Service、Interceptor

## 4. 修改文件前的要求

如果任务会新增文件、删除文件、重命名文件、改动多个核心模块，先明确告诉用户：

- 计划改哪些文件
- 为什么要改
- 是否会影响现有流程

如果只是一个小修复，且影响范围明确，可以直接做，但也应在动手前简短说明。

特别注意：

- 不要擅自大面积重构
- 不要删除旧兼容逻辑，除非用户明确要求
- 不要顺手改无关模块
- 不要因为“看起来更优雅”就改变当前项目既有结构

## 5. 代码生成与修改规范

生成代码时遵循以下原则：

- 尽量保持现有项目结构与风格一致
- 优先做最小必要修改
- 新增功能优先延续已有命名方式
- DTO / VO / Service / Controller 分层清晰
- 不要引入大型新框架，除非用户明确要求
- Redis、JWT、MyBatis Plus 的接入要和现有实现方式保持一致
- 前端尽量复用现有 `App.vue` 和 `styles.css` 风格，不要突然换整套 UI 风格

后端规范：

- 参数校验和业务异常优先使用现有 `BusinessException`、`ErrorCode`
- 返回结构优先使用 `BaseResponse`
- 鉴权逻辑不要绕过 `AuthInterceptor` 和 `LoginUserHolder`
- 缓存逻辑优先复用 Redis 统一配置，不要自行新建另一套序列化体系

前端规范：

- 接口调用优先复用 `frontend/src/api/` 和 `frontend/src/utils/request.js`
- 不要轻易重写 `App.vue` 主流程
- 保持现有流式 SSE chunk 解析逻辑兼容

## 6. 执行任务后的输出要求

每次完成任务后，必须清晰告诉用户：

1. 修改了哪些文件
2. 新增了哪些功能 / 模块 / 接口 / 组件
3. 关键逻辑怎么实现
4. 做了哪些验证
5. 哪些内容没有做
6. 是否存在已知限制、兼容性问题或后续建议

如果执行了编译 / 构建 / 测试，也要明确写出结果。

## 7. 当前已知注意事项

- 项目中部分历史中文字符串曾出现乱码，修改相关文件时如果碰到乱码，优先顺手修正当前改动范围内的文本
- Redis 当前已切换为统一 JSON 序列化，不要再依赖默认 JDK 序列化思路
- Redis 中可能存在旧格式脏缓存，现有代码已做一定自愈处理；如果出现反序列化问题，可考虑清理相关 key
- 旧聊天兼容逻辑仍然存在，修改时注意不要误删

## 8. 当前工作方式建议

面对新任务时，推荐先做：

1. 阅读本文件
2. 阅读 `STEP_PROGRESS_SUMMARY.md`
3. 用最少量的文件读取确认现状
4. 给出“准备改哪些文件”的简短说明
5. 实施修改
6. 编译 / 构建验证
7. 向用户汇总结果

## 9. 不要做的事情

- 不要默认整个项目都需要重构
- 不要在没有必要时大范围读取代码
- 不要擅自修改前端、后端、数据库三端一起联动，除非任务明确要求
- 不要省略验证步骤
- 不要只给结论不说改了什么

## 10. 配合方式

如果用户说“继续做 stepX”，优先先查看 `STEP_PROGRESS_SUMMARY.md`，确认当前项目已经做到哪一步，再继续实施。

