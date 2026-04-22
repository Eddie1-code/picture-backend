# 压测执行说明（k6）

## 准备

1. 安装 k6（Windows 可用 `choco install k6`）。
2. 准备 1~3 组有效登录 Token（用于写接口与 WS 握手）。
3. 设置环境变量：

```powershell
$env:BASE_URL="https://picture.xucanwei.xyz"
$env:LOGIN_TOKEN="your-token"
```

## 执行顺序

1. 静态/读接口基线：

```powershell
k6 run .\k6-read.js
```

2. 写接口压力（带登录态）：

```powershell
k6 run .\k6-write.js
```

3. WebSocket 握手与持续连接：

```powershell
k6 run .\k6-ws.js
```

4. 恶意流量模拟（验证限流/反爬）：

```powershell
k6 run .\k6-attack.js
```

## 验收阈值（建议）

- `http_req_failed < 1%`
- `http_req_duration p(95) < 800ms`（读接口）
- `http_req_duration p(95) < 1200ms`（写接口）
- 恶意流量场景下出现可观 `429` / `403`，且正常接口成功率不受显著影响
- WebSocket 握手成功率 > 98%，异常场景握手应被限流拒绝
