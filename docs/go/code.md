---
title: Go 代码示例
---

# Go 代码示例

以下 Go 源码已从 `gitTest/go` 迁移到本站的静态资源目录（`docs/public/code/go/`），点击即可查看/下载完整文件。运行时通过 `/code/go/...` 路径访问。

## goweb（Web 框架示例）
- [main.go](/code/go/goweb/main.go) —— 程序入口
- [config/config.go](/code/go/goweb/config/config.go)
- [db/mysql.go](/code/go/goweb/db/mysql.go)、[db/domain.go](/code/go/goweb/db/domain.go)
- [api/code.go](/code/go/goweb/api/code.go)、[api/code_test.go](/code/go/goweb/api/code_test.go)
- [config.yaml](/code/go/goweb/config.yaml)

## market-task（定时任务）
- [main.go](/code/go/market-task/main.go)
- [runner/order.go](/code/go/market-task/runner/order.go)、[runner/task.go](/code/go/market-task/runner/task.go)
- [notice/notice.go](/code/go/market-task/notice/notice.go)
- [db/db.go](/code/go/market-task/db/db.go)
- [Dockerfile](/code/go/market-task/Dockerfile)

## sync（并发示例）
- [main.go](/code/go/sync/main.go)
- [exmples/controlnum.go](/code/go/sync/exmples/controlnum.go)、[exmples/helloworld.go](/code/go/sync/exmples/helloworld.go)
- [exmples/pc.go](/code/go/sync/exmples/pc.go)、[exmples/pubsub.go](/code/go/sync/exmples/pubsub.go)、[exmples/selectopter.go](/code/go/sync/exmples/selectopter.go)

## test
- [main.go](/code/go/test/main.go)、[jsonTime.go](/code/go/test/jsonTime.go)

## websocket
- [main.go](/code/go/websocket/main.go)
- [ws/client.go](/code/go/websocket/ws/client.go)、[ws/hub.go](/code/go/websocket/ws/hub.go)
- [home.html](/code/go/websocket/home.html)

> 完整源码目录：`docs/public/code/go/`
