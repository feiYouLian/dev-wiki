> 原文：[什么，你连一个Node.js脚本都不会写！！！](https://juejin.cn/post/7361687968519700514) · 作者：前端大骆

# 用 Node.js 脚本做前端工程自动化

## 核心观点

不要把「Node.js 脚本」想得太神秘——它只是在 **Node.js 运行时里执行的 JavaScript 脚本**而已。前端工程里最常见的用途是文件操作（读取 / 写入 / 删除 / 新建），跟平时写的 JS 没有本质区别，只是运行环境不同。

## 怎么运行

1. 装好 Node.js，随便建个 `index.js`，写入 `console.log('我是一个Node.js脚本')`；
2. 命令行进入目录执行 `node ./index.js` 即可看到输出。

想把它变成 npm 脚本，在 `package.json` 的 `scripts` 里加：

```json
"scripts": {
  "my-script": "node ./scripts/index.js"
}
```

之后用 `npm run my-script` 运行。

## 引入第三方包

- Node < 12：只能用 `require()`；
- Node ≥ 12：可用 ES Module 的 `import`，但要在 `package.json` 设 `"type": "module"`，或干脆把脚本后缀改成 `.mjs`。

示例（作者 Node 16.14，用 `autoExport.mjs`）：

```js
import fs from 'fs-extra';
```

选 `fs-extra` 而不是原生 `fs`，是为了少踩文件操作的边界坑、保证跨平台兼容。

## 实战：自动导出组件库

需求：把 `components/` 下每个组件，按 `export { default as Xxx } from './components/Xxx'` 的格式，自动写进根目录的 `index.ts`。

最简版（理想情况）：

```js
import fs from 'fs-extra';

fs.readdir('./src/components')
  .then(res => {
    if (Array.isArray(res)) {
      let exportStr = '';
      res.forEach(item => {
        exportStr = `${exportStr}\nexport { default as ${item} } from './components/${item}';`;
      });
      fs.writeFile('./src/index.export.ts', exportStr);
    }
  })
  .catch(err => console.error(err));
```

进阶版（加校验，只导出符合规范的组件）：要求**文件夹名是大驼峰**且**包含 `index.tsx`**，用 `lstatSync().isDirectory()`、`existsSync` 配合正则 `/^[A-Z][a-zA-Z]*$/` 过滤。

```js
import fs from 'fs-extra';

fs.readdir('./src/components')
  .then(files => {
    if (Array.isArray(files)) {
      let exportStr = '';
      files.forEach(item => {
        if (
          fs.lstatSync(`./src/components/${item}`).isDirectory() &&
          /^[A-Z][a-zA-Z]*$/.test(item) &&
          fs.existsSync(`./src/components/${item}/index.tsx`)
        ) {
          exportStr = `${exportStr}\nexport { default as ${item} } from './components/${item}';`;
        }
      });
      fs.writeFile('./src/index.export.ts', exportStr);
    }
  })
  .catch(err => console.error(err));
```

## 怎么调试

不推荐用侵入式的 `console.log`。直接用 **VS Code 自带调试**：

1. 点左侧调试图标 → 创建 `launch.json`；
2. 选 Node.js 调试器类型；
3. 选择要调试脚本的执行命令（对应 `package.json` 里 `scripts` 定义的命令名）；
4. 点开始调试，体验和浏览器 DevTools 调试 JS 一样。

## 常用第三方 CLI 包

| 包 | 用途 |
| --- | --- |
| `yargs` | 解析命令行参数 |
| `chalk` | 命令行输出上色 |
| `cli-table` | 命令行画表格 |
| `ora` | 加载动画 |
| `inquirer` | 交互式命令行输入 |
| `boxen` | 带边框的提示框 |
| `progress` | 进度条 |
| `figlet` | 艺术字 |
| `execa` | 更强大的子进程管理（替代 `child_process`） |
| `shelljs` | 用类 Shell 方式执行命令 |

## 小结

Node.js 脚本就是「换了个运行环境的 JS」。掌握 `fs-extra` 文件操作 + npm script + VS Code 调试，就能写出各种前端工程自动化脚本。
