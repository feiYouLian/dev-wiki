# VitePress + GitHub Pages 文档站脚手架

开箱即用的文档站模板：VitePress 1.x 构建 + GitHub Actions 自动部署 + GitHub Pages 免费托管，自带 HTTPS。

## 目录结构

```
.
├─ .github/
│  └─ workflows/
│     └─ deploy.yml          # 自动构建部署工作流
├─ docs/
│  ├─ .vitepress/
│  │  ├─ config.mts          # 站点配置（唯一必改文件）
│  │  └─ theme/
│  │     ├─ index.ts         # 自定义主题入口
│  │     └─ style.css        # 品牌色等样式覆盖
│  ├─ guide/
│  │  ├─ getting-started.md  # 示例页：快速开始
│  │  └─ configuration.md    # 示例页：配置说明
│  ├─ public/                # 静态资源，原样复制到产物根目录
│  │  ├─ favicon.svg
│  │  ├─ robots.txt
│  │  └─ CNAME.example       # 需要自定义域名时改名为 CNAME
│  └─ index.md               # 首页
├─ .gitignore
└─ package.json
```

## 三步上线

### 1. 改 base 路径

打开 `docs/.vitepress/config.mts`，修改顶部的 `DEFAULT_BASE` 常量（或部署时用 `BASE_PATH` 环境变量覆盖，无需改代码）：

```ts
// 本地未设 BASE_PATH 时使用的默认值，fork 后改成你的仓库名
const DEFAULT_BASE = "/dev-wiki/";
// 也支持用环境变量覆盖（CI / 本地预览均可）：BASE_PATH=/仓库名/ npm run docs:dev
const BASE_PATH = normalizeBasePath(process.env.BASE_PATH);
```

| 站点类型                                 | 访问地址                          | base 值    |
| ---------------------------------------- | --------------------------------- | ---------- |
| 用户/组织站（仓库名 `用户名.github.io`） | `https://用户名.github.io`        | `/`        |
| 项目站（普通仓库）                       | `https://用户名.github.io/仓库名` | `/仓库名/` |
| 绑定自定义域名                           | `https://docs.example.com`        | `/`        |

**三条铁律**：以 `/` 开头、以 `/` 结尾；大小写与仓库名完全一致（URL 大小写敏感）；禁止用相对路径 `./`。

### 2. 安装并本地预览

```bash
npm install
npm run docs:dev     # 开发预览，默认 http://localhost:5173
npm run docs:build   # 构建
npm run docs:preview # 预览构建产物
```

需要 Node.js 18+，推荐 20。

### 3. 推送并开启 Pages

```bash
git init
git add .
git commit -m "init: 初始化文档站"
git branch -M main
git remote add origin https://github.com/用户名/仓库名.git
git push -u origin main
```

然后进入仓库 `Settings → Pages → Build and deployment → Source`，选择 **GitHub Actions**。

> **千万不要选 `Deploy from a branch`**。那样 GitHub 会把 Markdown 源文件当静态文件直接发布，绕过 VitePress 构建，base 配置也会失效。

推送后到 `Actions` 页看运行状态，绿色对勾即成功，首次约 1–3 分钟。之后每次推 `main` 自动重新发布。

如果 Actions 报 403，检查 `Settings → Actions → General → Workflow permissions` 是否为 **Read and write**。

## 使用 pnpm / yarn

改两处即可：

1. `deploy.yml` 中 `cache: npm` → `cache: pnpm`
2. `deploy.yml` 中 `run: npm ci` → `run: pnpm install --frozen-lockfile`

## 自定义域名

1. `Settings → Pages → Custom domain` 填 `docs.example.com` → Save
2. DNS 服务商加 CNAME 记录：`docs` → `用户名.github.io`（**不带仓库名**）
3. DNS 生效后（最多 24 小时）勾选 `Enforce HTTPS`
4. `config.mts` 里把 `base` 改回 `/`
5. 把 `docs/public/CNAME.example` 改名为 `CNAME`，内容填域名

第 5 步不能漏：Actions 部署不会自动创建 CNAME 文件，漏掉会导致每次部署后域名失效。

## 写文档的约定

- Markdown 放 `docs/` 下，路径即 URL：`docs/guide/foo.md` → `/guide/foo`
- 新页面记得在 `config.mts` 的 `sidebar` 里登记
- 文档间互链用 `/` 开头的绝对路径，VitePress 自动补 base
- 引用 `public/` 里的资源同样用 `/` 开头
- `package-lock.json` 必须提交，否则 CI 的 `npm ci` 失败

## 常见问题

| 症状                   | 原因与解决                                                 |
| ---------------------- | ---------------------------------------------------------- |
| 首页能开，点链接全 404 | base 大小写与仓库名不一致，或少了结尾斜杠                  |
| 页面白屏、CSS/JS 404   | base 没设或设错                                            |
| Actions 报 403         | 缺 `id-token: write`，或仓库 Workflow permissions 未开读写 |
| `npm ci` 报 lock 缺失  | 没提交 `package-lock.json`                                 |
| 「最后更新于」时间不对 | checkout 缺 `fetch-depth: 0`                               |
| hydration mismatch     | 用了相对 base，或托管平台开了 HTML 自动压缩                |
| 部署后自定义域名失效   | 产物中缺少 `CNAME` 文件                                    |

## 成本

GitHub Pages 限额：仓库/站点各 1 GB、月流量 100 GB、单次构建 10 分钟、构建 10 次/小时（**使用自定义 Actions 工作流不受 10 次/小时限制**）。普通文档站远达不到上限，实际成本为零。

## 参考链接

- [VitePress 官方文档](https://vitepress.dev)
- [VitePress 部署指南](https://vitepress.dev/guide/deploy)
- [GitHub Pages 文档](https://docs.github.com/pages)


[pages](https://feiYouLian.github.io/dev-wiki)
