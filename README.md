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

### 查看当前 Windows 上的 Java 版本：

```powershell
java -version
```

如果你想看当前正在使用的 `java.exe` 路径：

```powershell
where java
```

如果要检查某个具体的 Java 可执行文件，PowerShell 里要用调用运算符 `&`：

```powershell
& "C:\Users\12942\.jdks\corretto-21.0.11\bin\java.exe" -version
```

### 永久配置 Java 21：

1. 先安装 JDK 21，例如放在：`C:\Program Files\Java\jdk-21`
2. 打开“系统属性” → “高级” → “环境变量”
3. 新建或修改系统变量 `JAVA_HOME`，值设置为你的 JDK 21 安装目录
4. 编辑系统变量 `Path`，把 `%JAVA_HOME%\bin` 放到前面
5. 重新打开 PowerShell，再执行：

```powershell
java -version
```

看到 `21.x` 就说明配置成功了。

### 启动Redis：

打开docker desktop

```powershell
powershell -ExecutionPolicy Bypass -File .\start-redis-dev.ps1
```

### 前端：

```powershell
cd E:\Bawa_Data\Xiangmu\ai-code-helper-my\frontend
npm.cmd run dev
```

### 后端：

先确认当前终端已经是 Java 21：

```powershell
java -version
```

然后启动后端：

```powershell
cd E:\Bawa_Data\Xiangmu\ai-code-helper-my
.\mvnw.cmd spring-boot:run
```

## 测试与验收

- 手动验收清单：`docs/test-checklist.md`
- 接口测试示例：`docs/api-test.md`
