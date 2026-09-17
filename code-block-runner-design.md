# 可运行代码块（Code Runner）功能开发文档

> 状态：设计稿 v2（已审核优化）
> 目标仓库：`vitepress-pages-starter`（VitePress 知识库）
> 一句话定位：让文档里的代码块从「静态展示」升级为「可交互验证」。

---

## 1. 背景与目标

文档里的代码示例长期存在「写完就过时」的问题：读者要复制到本地才能跑，写作者也无法保证示例在依赖升级后仍然正确。

引入「可运行代码块」后，形成三方受益：

| 角色 | 收益 |
| --- | --- |
| 人（读者） | 在文档页直接点「运行」看输出，无需复制到本地 |
| AI（IDE / Agent） | 通过 MCP 调用运行工具，自动验证文档示例是否正确 |
| 内容质量 | 示例随文档更新即时验证，避免文档过时 |

---

## 2. 运行能力分层

不同语言的运行成本差异很大，按下表分三档处理。**核心原则：能用前端沙箱解决的，绝不走后端。**

| 档位 | 语言 | 运行方式 | 是否需后端 | 说明 |
| --- | --- | --- | --- | --- |
| **A 档 · 浏览器原生** | JavaScript、TypeScript（转译后）、HTML、CSS、SQL | iframe sandbox / Web Worker / sql.js | 否 | TS 需先转译（见 3.2），并非严格"原生" |
| **B 档 · WASM 运行时** | Python（Pyodide）、Lua（Fengari）、Ruby（ruby.wasm） | 加载 WASM 运行时后在浏览器执行 | 否 | 运行时体积大，需懒加载 + 缓存 |
| **C 档 · 需服务端** | Node.js、Go、Rust、Bash、Java | Docker 沙箱 + API | 是（可选） | 仅在确有需要时启用 |

> ⚠️ **原稿修正点**：原稿把「WASM 版 Go」列入 B 档，这是不准确的。
> Go 没有浏览器内的解释器——它只能把**每个程序单独编译**成 WASM 产物，无法像 Pyodide 那样"接收一段 Go 源码就运行"。
> 因此 Go 归入 **C 档（服务端）**，需要 Docker 或预编译 WASM 产物两种路径，不在 B 档内。

**对个人 / 本地化场景的建议落地顺序**：先做 A 档（零成本、零风险），再补 B 档（按需），C 档按实际需求再上。

---

## 3. 前端实现

### 3.1 代码块标记约定

在 Markdown 代码块元信息（语言后的附加标记）中声明运行能力：

````markdown
```js run
console.log("Hello, AI 知识库")
```

```html run preview
<button onclick="alert('hi')">点我</button>
```

```python run
print("Hello from Pyodide")
```

```bash norun
npm install vitepress
```

```js run expected="42"
console.log(6 * 7)
```
````

标记语义：

| 标记 | 含义 |
| --- | --- |
| `run` | 显示「运行」按钮，允许执行 |
| `preview` | HTML 直接渲染预览，而非输出文本（仅对 html 有意义） |
| `norun` | 显式禁止运行 |
| `expected="..."` | 可选断言，运行后比对输出（见 3.5） |

**安全默认**：未显式标记 `run` 的代码块一律不可运行、不显示按钮。这是整套方案的安全基线。

> ⚠️ **`expected` 的工程注意点**（原稿未展开）：
> - 比对时建议先 `trim()` 再比较，避免尾部换行 / 首尾空白导致的误判。
> - 多行预期用 `expected="line1\nline2"` 转义，或改用 `expectedFile` 指向旁路断言文件。
> - 仅比对 **stdout**，stderr 不计入断言（stderr 另作"是否有错误"判断）。

### 3.2 运行器类型

| 运行器 | 实现 | 适用 | 关键约束 |
| --- | --- | --- | --- |
| iframe sandbox | `<iframe sandbox="allow-scripts">` | HTML/CSS/JS 预览 | 禁止 `allow-same-origin` 与顶层导航 |
| Web Worker | 独立线程执行 JS | 纯 JS 计算，不阻塞 UI | 无法操作 DOM，适合算法类 |
| TS 转译 + 执行 | esbuild-wasm / sucrase 先转译，再交给 Worker/iframe | TypeScript 示例 | A 档里唯一"非原生"的一步 |
| Pyodide | WASM 加载 Python 运行时 | Python 示例 | ~10MB，懒加载 + 缓存 |
| sql.js | WASM 版 SQLite | SQL 示例 | 内存数据库，每次运行重建 |
| 远程 API | 调用后端沙箱 | Node/Go/Bash 等 C 档 | 需后端，见第 4 章 |

### 3.3 输出捕获机制（原稿缺失，关键链路）

不同运行器的"拿到输出"方式不同，这是实现时最容易踩坑的地方：

- **HTML `preview`**：直接把代码作为 iframe `srcdoc` 渲染，看视觉效果即可，无文本输出。
- **JS / TS（iframe 路径）**：在 iframe 内**重写 `console.log/warn/error`**，把它们收集起来，再用 `postMessage` 回传给父页面，父页面渲染到输出面板。
- **JS（Worker 路径）**：在 Worker 内重写 `console`，通过 `worker.postMessage` 回传。
- **Pyodide / sql.js**：运行时本身提供 `pyodide.runPython()` / `db.exec()` 的返回值，直接取字符串。

> 要点：iframe 必须 `sandbox="allow-scripts"` **但不带** `allow-same-origin`，此时 iframe 处于 `null` 源（opaque origin），与父页面完全隔离；父子通信**只能**靠 `postMessage`，因此输出捕获必须围绕它设计。

### 3.4 UI 结构

代码块右上角工具栏：

```
┌────────────────────────────────────┐
│ 代码块                    [复制][运行][重置] │
├────────────────────────────────────┤
│ console.log("hello")               │
├────────────────────────────────────┤
│ ▼ 输出                              │
│ hello                               │
└────────────────────────────────────┘
```

- 输出区域**默认折叠**，运行后展开。
- 正常输出用常规文本色；**错误输出（stderr / 异常）用红色区分**。
- 「重置」清空输出并复原运行环境（尤其 Pyodide/sql.js 需重建状态）。

### 3.5 VitePress 集成方式

1. **自定义 Markdown 插件**：解析 `run` / `preview` / `norun` / `expected` 标记，把对应代码块替换为 `<CodeRunner>` 组件，并把代码内容作为 props 传入。
2. **主题层注册全局组件**：在 `docs/.vitepress/theme/index.ts` 中 `app.component('CodeRunner', CodeRunner)`。
3. **样式跟随主题**：输出面板、工具栏使用 VitePress 的主题 CSS 变量（如 `--vp-c-default`、`--vp-code-bg`），保持视觉一致。
4. **SSR 兼容**：组件须在 `onMounted` 之后再创建 iframe/Worker，避免服务端渲染报错。

> 具体可落地的插件与组件代码见文末「附录 A：VitePress 插件参考实现」。

---

## 4. 后端沙箱（可选）

仅当需要运行 Node / Go / Bash / Rust / Java 等 C 档语言时启用。个人本地使用可以只在本地跑、不暴露公网。

### 4.1 架构

```
前端 → POST /api/run → 沙箱调度器 → Docker 容器（一次性）→ 返回 stdout / stderr / exitCode
```

请求体示例：`{ "language": "go", "code": "...", "timeoutMs": 10000 }`

### 4.2 安全约束（强制）

| 约束 | 配置 |
| --- | --- |
| 一次性容器 | 每次运行新建，结束即销毁（`docker run --rm`） |
| 资源限制 | CPU 1 核、内存 256MB、超时 10 秒 |
| 网络禁用 | `--network none` |
| 文件系统只读 | 仅挂载临时工作目录为可写，其余只读 |
| 镜像白名单 | 仅允许预置语言镜像（如 `python:3.12-slim` 等） |
| 限流 | 按 IP / Token 限速，防止滥用 |
| 输出截断 | 后端和前端都限制返回体大小（如 1MB），防超大输出拖垮页面 |

### 4.3 推荐实现

- **调度**：Node + `dockerode`，或 Go + Docker SDK。
- **部署**：本地 Docker，或自带公网 IP 的 VPS。**注意**：Cloudflare Workers 等边缘运行时不支持 Docker，不能用来跑 C 档沙箱。
- **个人场景**：本地起一个 Docker 守护 + 简单鉴权的 `/api/run` 即可，不必对外网开放。

---

## 5. MCP 集成：让 AI 也能运行代码

新增 MCP 工具，让 AI IDE 能主动验证 / 运行文档中的示例，把「文档验证」从人工变成 AI 可自动完成的任务。

### 5.1 工具定义

| 工具名 | 参数 | 返回 | 说明 |
| --- | --- | --- | --- |
| `run_code_block` | `path`, `blockIndex` | `stdout`, `stderr`, `exitCode` | 运行文档中指定代码块 |
| `run_code` | `language`, `code` | `stdout`, `stderr`, `exitCode` | 运行任意代码片段 |
| `verify_example` | `path`, `blockIndex`, `expected?` | `passed`, `actual` | 断言代码块输出是否符合预期 |
| `read_doc`* | `path` | 文档内容 + 代码块索引 | 定位示例所在块（概念性工具） |
| `write_doc`* | `path`, `blockIndex`, `newCode` | `ok` | 写回修正后的示例（概念性工具） |

> 带 `*` 的工具在原稿工作流中被引用，但原稿工具表里没有，此处补全。`read_doc` / `write_doc` 是「文档读写」类工具，可由文档型 MCP（或本仓库的文档服务）提供，与运行类工具解耦。

### 5.2 典型 AI 工作流

```
用户：「文档里那个 MCP 配置示例还能用吗？」
  → AI 调用 read_doc 找到示例
  → AI 调用 run_code_block 执行
  → AI 对比输出（或用 verify_example 断言）
  → 判断是否需要更新文档
  → 若需更新，调用 write_doc 写回
```

这就把「文档验证」从人工巡检变成了 AI 可自动完成的任务。

---

## 6. 安全设计（贯穿全局）

1. **默认全部不可运行**：只有显式标记 `run` 的代码块才显示运行按钮。
2. **前端沙箱优先**：能用 iframe / Worker / WASM 解决的，就不走后端。
3. **iframe 隔离**：`sandbox="allow-scripts"`，**禁止** `allow-same-origin` 与顶层导航。
4. **CSP 头**：限制 `script-src`、`connect-src`，收敛可加载与可通信的来源。
5. **超时与限流**：前端（iframe/Worker 超时）与后端（容器超时 + 限速）双重防护，防死循环与资源耗尽。
6. **输出长度限制**：前端与后端都截断超大输出。
7. **敏感信息过滤**：运行环境**不注入任何 Token / 环境变量**；C 档沙箱同样不挂载宿主凭据。

---

## 7. 与知识库的联动

- **代码块即知识单元**：每个可运行代码块可被 MCP 单独索引。
- **运行结果可缓存**：把最近一次运行结果写入**旁路文件**（如 `.cache/runs/<hash>.json`）或知识库数据层，方便 AI 快速读取，不必每次重跑。
  > 原稿建议写入 frontmatter，但 frontmatter 位于文件头部、且混入运行态数据会污染源码与 diff，**不推荐**；改用旁路文件更干净。
- **失败即信号**：`verify_example` 失败时，MCP 可返回「文档可能过时」的提示，触发人工或 AI 复核。
- **可运行示例优先**：AI 生成文档时，优先产出带 `run` 标记的代码块，便于后续自动验证。

---

## 8. 实施顺序

1. **A 档前端运行**：JS / HTML / CSS，iframe + Web Worker。
2. **代码块插件**：VitePress Markdown 扩展 + 运行按钮 + 输出面板 + `postMessage` 捕获。
3. **B 档 WASM**：按需接入 Pyodide、sql.js（含懒加载与运行时缓存）。
4. **MCP 运行工具**：`run_code_block`、`verify_example`。
5. **C 档后端沙箱**：需要 Node/Go/Bash 时再上 Docker。
6. **AI 自动验证**：把 `verify_example` 纳入文档 CI 或 AI 工作流。

---

## 9. 最小闭环

```
文档中写 ```js run → 页面出现运行按钮 → 点击看到输出
AI 通过 MCP 调用 verify_example → 自动判断示例是否仍正确
示例失效 → AI 提示或写回更新 → 文档保持「活」的状态
```

---

## 10. 已知限制与边界（新增章节 · 诚实声明）

设计再好，也要讲清楚边界，避免预期错位：

- **B 档运行时体积大**：Pyodide 首次加载约 10MB+，需懒加载并将运行时缓存在 IndexedDB，二次运行才快。
- **WASM 运行时有能力边界**：Pyodide 缺省环境无 `pip` 网络安装能力（离线 whl 可预装），许多依赖 C 扩展的包无法使用；sql.js 是内存库，不持久化。
- **网络默认禁用**：A/B 档在隔离沙箱里无法访问外部网络，依赖联网的示例（如 `fetch` 外部 API）在 B 档内会失败——这类示例要么改成本地 mock，要么归到 C 档。
- **Go/Rust/Java 无浏览器内运行时**：必须走 C 档服务端，或个人预先编译好 WASM 产物再嵌入。
- **TS 转译成本**：A 档里的 TS 需先转译，简单示例用 sucrase 很快，复杂工程（含多文件、装饰器）转译链路复杂，建议这类示例降级为 `norun` 或 C 档。
- **MCP `write_doc` 有写入风险**：自动写回文档属于"对外修改"，应设人工确认或仅在特定 CI 环境下开启，避免 AI 误改正文。

---

## 附录 A：VitePress 插件参考实现

> 仅示意关键结构，便于直接开工，细节按需补全。

**`docs/.vitepress/markdown-run.ts` — 解析 `run` 标记并替换为组件**

```ts
import type { MarkdownEnv } from 'vitepress'
import type MarkdownIt from 'markdown-it'

export function codeRunnerPlugin(md: MarkdownIt) {
  const defaultFence = md.renderer.rules.fence!
  md.renderer.rules.fence = (tokens, idx, options, env: MarkdownEnv, self) => {
    const token = tokens[idx]
    const info = token.info.trim().split(/\s+/)
    const lang = info[0]
    const flags = info.slice(1)
    const runnable = flags.includes('run')
    const preview = flags.includes('preview')
    const expected = token.info.match(/expected="([^"]*)"/)?.[1]

    if (!runnable) return defaultFence(tokens, idx, options, env, self)

    return `<CodeRunner
      lang="${lang}"
      code=${JSON.stringify(token.content)}
      preview="${preview}"
      expected="${expected ?? ''}"
    />`
  }
}
```

**`docs/.vitepress/theme/index.ts` — 注册全局组件**

```ts
import DefaultTheme from 'vitepress/theme'
import CodeRunner from './CodeRunner.vue'
import type { Theme } from 'vitepress'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('CodeRunner', CodeRunner)
  }
} satisfies Theme
```

**`.vitepress/config.ts` — 挂上 markdown 插件**

```ts
import { codeRunnerPlugin } from './markdown-run'

export default {
  markdown: {
    config: (md) => codeRunnerPlugin(md)
  }
}
```

**`CodeRunner.vue` — 运行 + 输出捕获骨架**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = defineProps<{
  lang: string
  code: string
  preview?: boolean
  expected?: string
}>()

const output = ref('')
const isError = ref(false)
const collapsed = ref(true)

function run() {
  collapsed.value = false
  if (props.preview) {
    // HTML 预览：用 iframe srcdoc 渲染，无需文本捕获
    output.value = '' // 预览模式下走另一个 template 分支
    return
  }
  // JS/TS：在 iframe 内重写 console 并用 postMessage 回传
  const iframe = document.createElement('iframe')
  iframe.sandbox.add('allow-scripts')
  iframe.srcdoc = `
    <script>
      const send = (type, text) => parent.postMessage({type, text}, '*')
      console.log = (...a) => send('log', a.join(' '))
      console.error = (...a) => send('error', a.join(' '))
      try { ${props.code} } catch (e) { send('error', e.message) }
    <\/script>`
  window.addEventListener('message', (e) => {
    if (e.data.type === 'error') { isError.value = true; output.value += e.data.text + '\n' }
    else output.value += e.data.text + '\n'
  })
  document.body.appendChild(iframe)
  // 运行后如有 expected，执行断言比对
  if (props.expected) {
    const passed = output.value.trim() === props.expected.trim()
    output.value += passed ? '\n✅ 断言通过' : '\n❌ 断言失败'
  }
}

function reset() {
  output.value = ''
  isError.value = false
  collapsed.value = true
}
</script>

<template>
  <div class="code-runner">
    <div class="toolbar">
      <button @click="run">运行</button>
      <button @click="reset">重置</button>
    </div>
    <pre v-if="!collapsed" :class="{ error: isError }">{{ output }}</pre>
  </div>
</template>
```

---

*文档版本 v2 · 审核优化点：修正运行档位（Go 移出 B 档）、补全 TS 转译与 iframe 输出捕获链路、修正结果缓存方式（旁路文件替代 frontmatter）、统一 MCP 工具表、新增「已知限制与边界」与「插件参考实现」两章。*
