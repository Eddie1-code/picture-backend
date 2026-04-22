# 腾讯云 CDN / COS 安全收尾清单

## 1) 入口与 HTTPS

- 访问入口统一为 CDN 域名（不要直接暴露源站 IP）。
- CDN 开启 HTTPS，证书状态正常。
- 回源协议设置为 HTTPS（优先），若暂时不支持则使用协议跟随并尽快迁移。
- 源站 `80 -> 443` 跳转开启。

## 2) CORS（按最小权限）

仅保留以下 Origin：

- `https://picture.xucanwei.xyz`（生产）
- `http://localhost:5173`（本地开发）

建议：

- Methods：只保留确实需要的方法（GET/POST/PUT/DELETE/HEAD）。
- Allow-Headers：按需列举，不使用 `*`。
- Max-Age：建议 600~1800 秒。

## 3) 防盗链

- 防盗链开启（白名单模式）。
- Referer 白名单至少包含：
  - `xucanwei.xyz`
  - `www.xucanwei.xyz`
  - `picture.xucanwei.xyz`
- 若业务需要直接地址访问，再勾选“允许空 Referer”；否则保持关闭。

## 4) CDN 安全开关

- 开启 CC 防护（挑战/限速模式）。
- 开启 Bot 管理（若套餐支持）。
- 开启异常 UA 拦截规则（sqlmap/nmap/scrapy/curl 扫描器特征）。
- 开启 HTTP 响应头加固：
  - `X-Frame-Options: SAMEORIGIN`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: strict-origin-when-cross-origin`

## 5) 源站保护

- 安全组仅允许 CDN 回源 IP 段访问 80/443。
- Java 后端端口 `8123` 只监听内网/本机，不开放公网。
- 关闭无关管理端口公网访问（Redis / MySQL / SSH 非白名单）。

## 6) 上线后验证

- 使用浏览器开发者工具确认静态资源命中 CDN（`x-cache`）。
- 直接访问源站 IP + Host 伪造应被拦截或不可达。
- CORS 预检与业务请求正常返回。
- 热链来源应返回 403 或指定错误资源。
