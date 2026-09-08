import { defineConfig } from "vitepress";

// ============================================================
// ⚠️ 部署前唯一需要修改的地方：base 路径
// ------------------------------------------------------------
// 用户/组织站   https://用户名.github.io            → base: '/'
// 项目站       https://用户名.github.io/仓库名      → base: '/仓库名/'
// 自定义域名   https://docs.example.com            → base: '/'
//
// 三条铁律：
//   1. 必须以 / 开头、以 / 结尾（'/my-docs/' 对，'/my-docs' 错）
//   2. 大小写必须和仓库名完全一致（GitHub Pages 的 URL 大小写敏感）
//   3. 禁止用相对路径 './'，会导致 SSR 水合报错
//
// 也支持用环境变量覆盖（CI 里设 BASE_PATH 即可，无需改代码）：
// ============================================================
const BASE_PATH = process.env.BASE_PATH || "/dev-wiki/";

export default defineConfig({
  base: BASE_PATH,

  lang: "zh-CN",
  title: "我的文档站",
  description: "一个用 VitePress + GitHub Pages 搭建的文档站",

  // 显示「最后更新于」，依赖 deploy.yml 里的 fetch-depth: 0
  lastUpdated: true,

  // 去掉 URL 里的 .html 后缀
  cleanUrls: true,

  // 生成 sitemap.xml，把 hostname 换成你自己的域名后取消注释
  // sitemap: {
  //   hostname: 'https://docs.example.com'
  // },

  // head 里以 / 开头的 URL，VitePress 会自动补上 base，可放心写
  head: [
    ["link", { rel: "icon", href: "/favicon.svg" }],
    ["meta", { name: "theme-color", content: "#3c8772" }],
    ["meta", { property: "og:type", content: "website" }],
  ],

  themeConfig: {
    // ----------------------------------------------------------
    // 顶部导航
    // ----------------------------------------------------------
    nav: [
      { text: "首页", link: "/" },
      { text: "指南", link: "/guide/getting-started" },
      {
        text: "生态",
        items: [
          { text: "VitePress 官网", link: "https://vitepress.dev" },
          { text: "GitHub Pages 文档", link: "https://docs.github.com/pages" },
        ],
      },
    ],

    // ----------------------------------------------------------
    // 侧边栏：link 一律用 / 开头的绝对路径，VitePress 会自动补 base
    // ----------------------------------------------------------
    sidebar: [
      {
        text: "开始",
        items: [
          { text: "快速开始", link: "/guide/getting-started" },
          { text: "配置说明", link: "/guide/configuration" },
        ],
      },
    ],

    // ----------------------------------------------------------
    // 本地搜索：开箱即用，无需任何第三方服务
    // ----------------------------------------------------------
    search: {
      provider: "local",
      options: {
        translations: {
          button: {
            buttonText: "搜索文档",
            buttonAriaLabel: "搜索文档",
          },
          modal: {
            noResultsText: "没有找到结果",
            resetButtonTitle: "清除查询条件",
            footer: {
              selectText: "选择",
              navigateText: "切换",
            },
          },
        },
      },
    },

    // 右侧大纲标题层级
    outline: [2, 3],
    outlineTitle: "本页目录",

    docFooter: {
      prev: "上一篇",
      next: "下一篇",
    },

    lastUpdatedText: "最后更新于",

    // 返回顶部文案
    returnToTopLabel: "回到顶部",

    // 右侧「编辑此页」链接，格式 /xxx/edit/main/docs/...
    editLink: {
      pattern: "https://github.com/feiYouLian/dev-wiki/edit/main/docs/:path",
      text: "在 GitHub 上编辑此页",
    },

    socialLinks: [
      { icon: "github", link: "https://github.com/feiYouLian/dev-wiki" },
    ],

    footer: {
      message: "基于 VitePress 构建",
      copyright: "Copyright © 2026",
    },
  },
});
