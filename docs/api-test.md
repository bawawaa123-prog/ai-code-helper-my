# 接口测试说明

本文档提供当前项目常用接口的手动测试示例，默认后端地址为 `http://localhost:8081/api`。

## 通用请求头

`Authorization` 请求头示例：

```http
Authorization: Bearer your_jwt_token
```

## PowerShell 说明

Windows PowerShell 中的 `curl` 可能会被映射为 `Invoke-WebRequest` 的别名，所以本文档统一使用以下两种方式：

- `curl.exe`：最接近原生 curl，用法和 Linux/macOS 一致，适合 SSE / 流式接口。
- `Invoke-RestMethod`：PowerShell 原生命令，适合普通 JSON 接口。

> 重要：下面的命令请**只复制命令本身**，不要把 `PS E:\...>` 提示符、`>>` 续行符或其他输出一起复制进去。

## curl.exe 示例

### 1. 注册接口

```powershell
curl.exe -X POST "http://localhost:8081/api/user/register" -H "Content-Type: application/json" -d "{\"userAccount\":\"test001\",\"userPassword\":\"12345678\",\"checkPassword\":\"12345678\"}"
```

### 2. 登录接口

```powershell
curl.exe -X POST "http://localhost:8081/api/user/login" -H "Content-Type: application/json" -d "{\"userAccount\":\"test001\",\"userPassword\":\"12345678\"}"
```

### 3. 获取当前登录用户 `/user/me`

```powershell
curl.exe -X GET "http://localhost:8081/api/user/me" -H "Authorization: Bearer your_jwt_token"
```

### 4. 新建会话

```powershell
curl.exe -X POST "http://localhost:8081/api/chat/session" -H "Content-Type: application/json" -H "Authorization: Bearer your_jwt_token" -d "{\"title\":\"新会话\"}"
```

### 5. 查询会话列表

```powershell
curl.exe -X GET "http://localhost:8081/api/chat/session/list" -H "Authorization: Bearer your_jwt_token"
```

### 6. 查询历史消息

```powershell
curl.exe -X GET "http://localhost:8081/api/chat/session/1/messages" -H "Authorization: Bearer your_jwt_token"
```

### 7. 重命名会话

```powershell
curl.exe -X PUT "http://localhost:8081/api/chat/session/1" -H "Content-Type: application/json" -H "Authorization: Bearer your_jwt_token" -d "{\"title\":\"计算机网络复习\"}"
```

### 8. 删除会话

```powershell
curl.exe -X DELETE "http://localhost:8081/api/chat/session/1" -H "Authorization: Bearer your_jwt_token"
```

### 9. POST SSE 聊天接口

```powershell
curl.exe -N -X POST "http://localhost:8081/api/ai/chat/stream" -H "Content-Type: application/json" -H "Authorization: Bearer your_jwt_token" -d "{\"sessionId\":1,\"message\":\"请介绍一下计算机网络分层模型\",\"useRag\":false}"
```

说明：

- `-N` 用于关闭输出缓冲，便于观察 SSE 流式返回。
- 返回内容为 `text/event-stream`，前端会按 chunk 逐步消费。
- 如果你从文档复制命令，建议一条一条整行复制，不要手动换行。

## PowerShell 示例

下面这些命令可以直接在 PowerShell 里运行。

### 1. 注册接口

```powershell
$body = @{ userAccount = "test001"; userPassword = "12345678"; checkPassword = "12345678" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/user/register" -ContentType "application/json" -Body $body
```

### 2. 登录接口

```powershell
$loginBody = @{ userAccount = "test001"; userPassword = "12345678" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/user/login" -ContentType "application/json" -Body $loginBody
```

### 3. 获取当前登录用户 `/user/me`

```powershell
$headers = @{ Authorization = "Bearer your_jwt_token" }
Invoke-RestMethod -Method Get -Uri "http://localhost:8081/api/user/me" -Headers $headers
```

### 4. 新建会话

```powershell
$headers = @{ Authorization = "Bearer your_jwt_token" }
$sessionBody = @{ title = "新会话" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/chat/session" -Headers $headers -ContentType "application/json" -Body $sessionBody
```

### 5. 查询会话列表

```powershell
$headers = @{ Authorization = "Bearer your_jwt_token" }
Invoke-RestMethod -Method Get -Uri "http://localhost:8081/api/chat/session/list" -Headers $headers
```

### 6. 查询历史消息

```powershell
$headers = @{ Authorization = "Bearer your_jwt_token" }
Invoke-RestMethod -Method Get -Uri "http://localhost:8081/api/chat/session/1/messages" -Headers $headers
```

### 7. 重命名会话

```powershell
$headers = @{ Authorization = "Bearer your_jwt_token" }
$renameBody = @{ title = "计算机网络复习" } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "http://localhost:8081/api/chat/session/1" -Headers $headers -ContentType "application/json" -Body $renameBody
```

### 8. 删除会话

```powershell
$headers = @{ Authorization = "Bearer your_jwt_token" }
Invoke-RestMethod -Method Delete -Uri "http://localhost:8081/api/chat/session/1" -Headers $headers
```

### 9. POST SSE 聊天接口

```powershell
curl.exe -N -X POST "http://localhost:8081/api/ai/chat/stream" -H "Content-Type: application/json" -H "Authorization: Bearer your_jwt_token" -d "{\"sessionId\":1,\"message\":\"请介绍一下计算机网络分层模型\",\"useRag\":false}"
```

## 常见问题

### 1. 为什么 `curl` 不能用？

在 PowerShell 中，`curl` 可能不是原生 curl，而是别名命令。请改用 `curl.exe`。

### 2. 为什么 `>>` 会出现？

`>>` 表示 PowerShell 进入了“命令未结束”的续行状态，通常是因为你复制了换行续写命令但中间的引号、反引号或括号没有按 PowerShell 的规则正确闭合。本文档已改成单行命令，能避免这个问题。

### 3. 为什么 JSON 直接写字符串容易报错？

PowerShell 对引号和转义更敏感。推荐使用 `ConvertTo-Json` 先生成 JSON，再传给 `Invoke-RestMethod`。

### 4. 为什么 SSE 接口建议用 `curl.exe`？

因为 SSE 是流式输出，`curl.exe -N` 更稳定，PowerShell 的 `Invoke-RestMethod` 不太适合直接观察流。
