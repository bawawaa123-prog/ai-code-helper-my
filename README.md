# AI 编程小助手

基于 Spring Boot、Vue3、LangChain4j 的 AI 编程学习与面试辅助项目。

## 开发准备说明

- 后端运行依赖 Java 21
- 前端运行依赖 Node.js
- `node_modules`、`target`、IDE 配置文件不应提交到仓库
- API Key 当前仍在开发配置中，后续会由开发者自行调整为环境变量
- 数据库连接支持通过 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 环境变量覆盖
- JWT 基础配置支持通过 `JWT_SECRET` 环境变量覆盖

## 后续计划

- 用户体系
- 会话历史
- 消息持久化
- JWT 鉴权

## 运行

### 启动Redis：

打开docker desktop

powershell -ExecutionPolicy Bypass -File .\start-redis-dev.ps1

### 前端：

cd E:\Bawa_Data\Xiangmu\ai-code-helper-my\frontend
npm.cmd run dev

### 后端：

cd E:\Bawa_Data\Xiangmu\ai-code-helper-my
.\mvnw.cmd spring-boot:run

## 测试与验收

- 手动验收清单：`docs/test-checklist.md`
- 接口测试示例：`docs/api-test.md`
