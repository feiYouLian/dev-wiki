# 快速开始

本文说明如何从零把这套脚手架跑起来并发布到 GitHub Pages。

## 1. 安装依赖

```bash
npm install
```

> 需要 Node.js 18 及以上版本，推荐 20。

## 2. 本地预览

```bash
npm run docs:dev
```

打开终端里提示的地址（默认 <http://localhost:5173>）即可看到站点，修改 Markdown 会自动热更新。

## 3. 改 base 路径（必须）

打开 `docs/.vitepress/config.mts`，找到顶部这一行：

```ts
const BASE_PATH = process.env.BASE_PATH || '/REPO_NAME/'
```

把 `/REPO_NAME/` 换成你的仓库名，例如 `/my-docs/`。

| 站点类型 | 访问地址 | base |
| --- | --- | --- |
| 用户/组织站 | `https://用户名.github.io` | `/` |
| 项目站 | `https://用户名.github.io/仓库名` | `/仓库名/` |
| 自定义域名 | `https://docs.example.com` | `/` |

::: warning 三个高频错误
- 少了结尾斜杠：`/my-docs` ❌ → `/my-docs/` ✅
- 大小写不一致：仓库叫 `FrontendNotes`，就必须写 `/FrontendNotes/`
- 用相对路径 `./`：会直接触发 SSR 水合报错
:::

## 4. 推送到 GitHub

```bash
git init
git add .
git commit -m "init: 初始化文档站"
git branch -M main
git remote add origin https://github.com/用户名/仓库名.git
git push -u origin main
```

::: tip
`package-lock.json`（或 pnpm 的 `pnpm-lock.yaml`）**必须提交**，否则 CI 里 `npm ci` 会失败。
:::

## 5. 开启 Pages 发布源

进入仓库 `Settings → Pages → Build and deployment → Source`，选择 **GitHub Actions**。

::: danger 别选错
不要选 `Deploy from a branch`。那样 GitHub 会把 Markdown 源文件直接当静态文件发布，绕过 VitePress 构建，你配的 base 也会失效。
:::

## 6. 查看结果

推送后到 `Actions` 标签页看工作流运行，出现绿色对勾即部署成功。首次部署约 1–3 分钟。

访问地址可在 `Settings → Pages` 里点 `Visit site` 获得。

之后每次推送到 `main` 分支，站点都会自动重新构建发布。

## 写新文档的约定

- Markdown 放在 `docs/` 下，路径即 URL：`docs/guide/foo.md` → `/guide/foo`
- 新增页面后记得在 `config.mts` 的 `sidebar` 里登记，否则导航里找不到
- 静态资源（图片、字体、`CNAME`、`robots.txt`）放 `docs/public/`，会被原样复制到产物根目录
- 文档内引用 `public` 里的资源用绝对路径：`/logo.svg`
- 文档之间互相链接用 `/` 开头的绝对路径，VitePress 会自动补上 base
