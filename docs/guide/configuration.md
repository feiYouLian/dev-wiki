# 配置说明

## base 路径

站点部署的子路径，是唯一必须按实际情况修改的配置。

```ts
// config.mts 顶部
const DEFAULT_BASE = "/dev-wiki/";          // 改成你的仓库名，例如 "/my-docs/"
const BASE_PATH = normalizeBasePath(process.env.BASE_PATH);

export default defineConfig({
  base: BASE_PATH
})
```

支持用环境变量覆盖，方便同一份代码部署到不同环境（优先级高于 `DEFAULT_BASE`）：

```ts
// 本地预览
BASE_PATH=/my-docs/ npm run docs:dev
// 或 CI 中 env: { BASE_PATH: '/my-docs/' }
```

在 CI 中：

```yaml
- name: Build with VitePress
  run: npm run docs:build
  env:
    BASE_PATH: /仓库名/
```

## 常用站点配置

| 配置项 | 作用 |
| --- | --- |
| `title` | 站点标题，显示在导航栏和浏览器标签 |
| `description` | 站点描述，用于 SEO |
| `lang` | 语言，中文用 `zh-CN` |
| `lastUpdated` | 显示「最后更新于」，需配合 `fetch-depth: 0` |
| `cleanUrls` | 去掉 URL 中的 `.html` 后缀 |
| `sitemap` | 生成 `sitemap.xml`，需填 `hostname` |
| `head` | 注入额外的 `<head>` 标签 |

## 导航与侧边栏

`nav` 是顶部导航，`sidebar` 是左侧目录，支持多级分组：

```ts
sidebar: [
  {
    text: '开始',
    items: [
      { text: '快速开始', link: '/guide/getting-started' }
    ]
  },
  {
    text: '进阶',
    collapsed: false,
    items: [
      { text: '自定义主题', link: '/advanced/theme' }
    ]
  }
]
```

分组还可以按页面动态切换 —— 给 `sidebar` 传对象，键是路径前缀：

```ts
sidebar: {
  '/guide/': [ /* 指南页显示这组 */ ],
  '/advanced/': [ /* 进阶页显示这组 */ ]
}
```

## 本地搜索

内置搜索零配置可用：

```ts
themeConfig: {
  search: { provider: 'local' }
}
```

文档量很大（数百页以上）时，可以申请 [Algolia DocSearch](https://docsearch.algolia.com/)（开源项目免费），换成 `provider: 'algolia'` 并填入 `appId` / `apiKey` / `indexName`。

## 自定义样式

改写品牌色只需覆盖 CSS 变量，见 `docs/.vitepress/theme/style.css`：

```css
:root {
  --vp-c-brand-1: #3c8772;
}
```

想彻底换主题，在 `docs/.vitepress/theme/index.ts` 里引入自定义 Layout。

## 自定义域名

1. 仓库 `Settings → Pages → Custom domain` 填 `docs.example.com` 并保存
2. DNS 服务商添加 CNAME：`docs` → `用户名.github.io`（**不要带仓库名**）
3. DNS 生效后（最多 24 小时）勾选 `Enforce HTTPS`
4. **把 `base` 改回 `/`**
5. **在 `docs/public/` 放一个无后缀的 `CNAME` 文件**，内容就是域名

第 5 步至关重要：GitHub Actions 部署不会自动创建 CNAME 文件，漏掉的话每次部署后自定义域名都会失效。

## GitHub Pages 限额

| 项目 | 限额 |
| --- | --- |
| 仓库 / 站点体积 | 各 1 GB |
| 月流量 | 100 GB（软限制） |
| 单次构建 | 10 分钟超时 |
| 构建频率 | 10 次/小时（用自定义 Actions 工作流**不受此限**） |

普通文档站远远用不到这些额度。
