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
// 默认仓库名，fork 后不改 BASE_PATH 也能直接跑（本地预览用 / 更方便，见下）
const DEFAULT_BASE = "/dev-wiki/";
const SITE_TITLE = "我的文档站";
const SITE_DESCRIPTION = "一个用 VitePress + GitHub Pages 搭建的文档站";

/**
 * 把 BASE_PATH 规范化成「以 / 开头、以 / 结尾」的合法 base，
 * 避免上面三条铁律被写错时静默产出 404 站点。
 */
function normalizeBasePath(rawBasePath: string | undefined): string {
  if (!rawBasePath) return DEFAULT_BASE;

  // 去掉空白与误加的引号（Windows 下 set BASE_PATH="xxx" 很常见），统一分隔符
  let basePath = rawBasePath.trim().replace(/^["']+|["']+$/g, "").replace(/\\/g, "/");

  // 去掉「协议 + 域名」和 // 前缀，防止 base 被指向外域导致资源从第三方站点加载
  basePath = basePath.replace(/^[a-z][a-z0-9+.-]*:\/\/[^/]+/i, "").replace(/^\/+/, "/");

  // 相对路径（./、.）会破坏 SSR 水合，一律归到根路径
  basePath = basePath.replace(/^\.\//, "");
  if (basePath === "." || basePath === "") basePath = "/";

  if (!basePath.startsWith("/")) basePath = `/${basePath}`;
  if (!basePath.endsWith("/")) basePath = `${basePath}/`;

  return basePath.replace(/\/{2,}/g, "/");
}

const BASE_PATH = normalizeBasePath(process.env.BASE_PATH);

export default defineConfig({
  base: BASE_PATH,

  lang: "zh-CN",
  title: SITE_TITLE,
  description: SITE_DESCRIPTION,

  // 显示「最后更新于」，依赖 deploy.yml 里的 fetch-depth: 0
  lastUpdated: true,

  // 去掉 URL 里的 .html 后缀
  cleanUrls: true,

  // 代码源码放在 docs/public/code/ 下，以 /code/ 开头的链接是静态资源，放行死链检查
  ignoreDeadLinks: [/^\/code\//],

  // 生成 sitemap.xml，把 hostname 换成你自己的域名后取消注释
  // sitemap: {
  //   hostname: 'https://docs.example.com'
  // },

  // head 里以 / 开头的 URL，VitePress 会自动补上 base，可放心写
  head: [
    ["link", { rel: "icon", href: "/favicon.svg" }],
    ["meta", { name: "theme-color", content: "#3c8772" }],
    ["meta", { property: "og:type", content: "website" }],
    ["meta", { property: "og:title", content: SITE_TITLE }],
    ["meta", { property: "og:description", content: SITE_DESCRIPTION }],
  ],

  themeConfig: {
    // ----------------------------------------------------------
    // 顶部导航
    // ----------------------------------------------------------
    nav: [
      { text: "首页", link: "/" },
      { text: "指南", link: "/guide/getting-started" },
      { text: "笔记", link: "/notes" },
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
      {
        text: "学习笔记",
        items: [
          { text: "笔记总览", link: "/notes" },
          {
            text: "Git",
            items: [
              { text: "命令大全", link: "/git/git命令大全" },
              { text: "常用命令图解", link: "/git/常用command图解" },
              { text: "学习资源", link: "/git/resources" },
            ],
          },
          {
            text: "Docker",
            items: [
              { text: "Dockerfile 详解", link: "/docker/" },
              { text: "Hadoop 部署", link: "/docker/hadoop" },
              { text: "安装部署", link: "/docker/install" },
            ],
          },
          {
            text: "大数据",
            items: [
              { text: "概览", link: "/bigdata/bigData" },
              { text: "Hive SQL", link: "/bigdata/hive_sql" },
            ],
          },
          {
            text: "EFK",
            items: [
              { text: "EFK 日志栈", link: "/efk/" },
              { text: "Elasticsearch", link: "/efk/elastic-search" },
            ],
          },
          {
            text: "Linux",
            items: [
              { text: "Linux 总览", link: "/linux/" },
              { text: "日志排查命令", link: "/linux/日志排查命令" },
              { text: "SpringBoot jar 管理脚本", link: "/linux/SpringBoot-jar启动停止重启脚本-掘金" },
            ],
          },
          {
            text: "算法",
            items: [
              { text: "Algorithm", link: "/algorithm/" },
            ],
          },
          {
            text: "Go",
            items: [
              { text: "Go 笔记", link: "/go/" },
              { text: "Go 代码示例", link: "/go/code" },
            ],
          },
          {
            text: "Java",
            items: [
              { text: "Java 学习笔记", link: "/java/" },
              { text: "HashMap", link: "/java/HashMap/HashMap" },
              { text: "Java 代码示例", link: "/java/code" },
              { text: "异步线程与线程池", link: "/java/async-thread-pool" },
            ],
          },
          {
            text: "Spring",
            items: [
              { text: "Spring 总览", link: "/spring/" },
              { text: "IoC / Bean", link: "/spring/spring-bean" },
              { text: "BeanFactory", link: "/spring/spring-beanFactory" },
              { text: "AOP", link: "/spring/spring-aop" },
              { text: "自动配置", link: "/spring/spring-autoconfig" },
              { text: "自动执行", link: "/spring/spring-auto-exec" },
              { text: "Boot", link: "/spring/spring-boot" },
              { text: "MVC", link: "/spring/spring-mvc" },
              { text: "JDBC", link: "/spring/spring-jdbc" },
              { text: "MyBatis", link: "/spring/spring-mybatis" },
              { text: "JPA", link: "/spring/spring-jpa" },
              { text: "事务", link: "/spring/spring-transaction" },
              { text: "资源", link: "/spring/spring-resource" },
              { text: "模板", link: "/spring/spring-template" },
              { text: "技巧", link: "/spring/spring-tips" },
              { text: "工具", link: "/spring/spring-tools" },
              { text: "用法", link: "/spring/spring-usage" },
              { text: "内置功能", link: "/spring/spring内置功能" },
              { text: "Excel 导入导出", link: "/spring/Excel导入导出与字典翻译说明" },
              { text: "refresh() 流程", link: "/spring/Spring -- refresh()方法流程梳理 - 掘金" },
            ],
          },
          {
            text: "MySQL",
            items: [
              { text: "MySQL 总览", link: "/mysql/" },
              { text: "Explain 执行计划分析", link: "/mysql/MySQL-Explain执行计划分析-掘金" },
            ],
          },
          {
            text: "Node.js",
            items: [
              { text: "Node.js 总览", link: "/node/" },
              { text: "脚本编写：自动导出组件库", link: "/node/Node.js-脚本编写自动导出组件-掘金" },
            ],
          },
          {
            text: "TypeScript",
            items: [
              { text: "TypeScript 总览", link: "/typescript/" },
              { text: "20 个必知技巧", link: "/typescript/TypeScript-20个技巧-掘金" },
            ],
          },
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
