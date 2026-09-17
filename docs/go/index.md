<!-- TOC -->

- [1. golang](#1-golang)
    - [1.1. doc](#11-doc)
    - [1.2. vscoe env](#12-vscoe-env)
        - [1.2.1. golang.org plugin](#121-golangorg-plugin)
        - [1.2.2. github.com plugin](#122-githubcom-plugin)
        - [1.2.3. install plugin](#123-install-plugin)
    - [1.3. go mod](#13-go-mod)
        - [1.3.1. tools gopls](#131-tools-gopls)
        - [1.3.2. set GOPROXY](#132-set-goproxy)
        - [1.3.3. set GO111MODULE](#133-set-go111module)
        - [1.3.4. command](#134-command)
        - [1.3.5. go.mod](#135-gomod)
        - [1.3.6. Practice demo](#136-practice-demo)
    - [1.4. glide](#14-glide)
        - [1.4.1. install](#141-install)
        - [1.4.2. bug fix](#142-bug-fix)
        - [1.4.3. base-command](#143-base-command)
        - [1.4.4. mirror](#144-mirror)

<!-- /TOC -->

# 1. golang 

## 1.1. doc
[go.doc](https://pkg.go.dev/)

[golang mirrors](https://github.com/golang)

[GOPROXY.IO(代理nb)](https://goproxy.io/zh/)

## 1.2. vscoe env 

### 1.2.1. golang.org plugin

**必要操作，因为https://golang.org 被墙了**

```sh
cd $GOPATH/src
mkdir -p golang.org/x
cd golang.org/x
git clone https://github.com/golang/tools.git
git clone https://github.com/golang/lint.git
git clone https://github.com/golang/sync.git
```
<!-- > gopls install dependence package: github.com/golang/sync -->

### 1.2.2. github.com plugin

**非必要操作，目的是提高安装速度**

```sh
cd $GOPATH/src
mkdir github.com
cd $GOPATH/src/github.com
mkdir acroca cweill derekparker go-delve josharian karrick mdempsky pkg ramya-rao-a rogpeppe sqs uudashr
cd $GOPATH/src/github.com/acroca
git clone https://github.com/acroca/go-symbols.git
cd $GOPATH/src/github.com/cweill
git clone https://github.com/cweill/gotests.git
cd $GOPATH/src/github.com/derekparker
git clone https://github.com/derekparker/delve.git
cd $GOPATH/src/github.com/go-delve
git clone https://github.com/go-delve/delve.git
cd $GOPATH/src/github.com/josharian
git clone https://github.com/josharian/impl.git
cd $GOPATH/src/github.com/karrick
git clone https://github.com/karrick/godirwalk.git
cd $GOPATH/src/github.com/mdempsky
git clone https://github.com/mdempsky/gocode.git
cd $GOPATH/src/github.com/pkg
git clone https://github.com/pkg/errors.git
cd $GOPATH/src/github.com/ramya-rao-a
git clone https://github.com/ramya-rao-a/go-outline.git
cd $GOPATH/src/github.com/rogpeppe
git clone https://github.com/rogpeppe/godef.git
cd $GOPATH/src/github.com/sqs
git clone https://github.com/sqs/goreturns.git
cd $GOPATH/src/github.com/uudashr
git clone https://github.com/uudashr/gopkgs.git
```

### 1.2.3. install plugin

```sh
cd $GOPATH/src
go install github.com/mdempsky/gocode
go install github.com/uudashr/gopkgs/cmd/gopkgs
go install github.com/ramya-rao-a/go-outline
go install github.com/acroca/go-symbols
go install github.com/rogpeppe/godef
go install github.com/sqs/goreturns
go install github.com/derekparker/delve/cmd/dlv
go install github.com/cweill/gotests
go install github.com/josharian/impl
go install golang.org/x/tools/cmd/guru
go install golang.org/x/tools/cmd/gorename
go install golang.org/x/lint/golint
```

## 1.3. go mod 

### 1.3.1. tools gopls

> 之前用的vscode的自动代码提示，发现太慢了，隔3，4秒才会出提示，所以换为Google推荐的`gopls`来代替。   

> gopls install dependence package 
1. 方案一
    1. 打开 VS Code 的setting, 搜索 `go.useLanguageServe`, 并勾选上.
    2. 默认情况下, 会提示叫你`reload`，重新打开之后，右下角会自动弹出下载的框框，点击`install`即可。

2. 方案二
```sh
cd $GOPATH/src/src/golang.org/x
git clone https://github.com/golang/sync.git
```

```sh
cd $GOPATH/src
go install github.com/golang/tools/cmd/gopls
```

### 1.3.2. set GOPROXY

- `GOPROXY=https://goproxy.io`: 只需设置该环境变量即可正常下载被墙的源码包了   

```shell
go env -w GOPROXY=https://goproxy.io,direct
```

### 1.3.3. set GO111MODULE

- `GO111MODULE=off`:go命令行将不会支持`module`功能，寻找依赖包的方式将会沿用旧版本那种通过`vendor`目录或者`GOPATH`模式来查找。   
- `GO111MODULE=on`:go命令行会使用`modules`，而一点也不会去`GOPATH`目录下查找。   
- `GO111MODULE=auto`:go命令行将会根据`当前目录`来决定是否启用`module`功能     

> 当modules 功能启用时，依赖包的存放位置变更为$GOPATH/pkg，允许同一个package多个版本并存，且多个项目可以共享缓存的 module。     

### 1.3.4. command

> go mod command

| command  | desc                                       | 说明                          |
|----------|--------------------------------------------|-----------------------------|
| download | download modules to local cache            | 下载依赖包                    |
| edit     | edit go.mod from tools or scripts          | 编辑go.mod                    |
| graph    | print module requirement graph             | 打印模块依赖图                |
| init     | initialize new module in current directory | 在当前目录初始化mod           |
| tidy     | add missing and remove unused modules      | 拉取缺少的模块，移除不用的模块 |
| vendor   | make vendored copy of dependencies         | 将依赖复制到vendor下          |
| verify   | verify dependencies have expected content  | 验证依赖是否正确              |
| why      | explain why packages or modules are needed | 解释为什么需要依赖            |

### 1.3.5. go.mod

> go.mod文件一旦创建后，它的内容将会被go toolchain全面掌控。go toolchain会在各类命令执行时，比如go get、go build、go mod等修改和维护go.mod文件。   

`go.mod` 提供了`module`, `require`、`replace`和`exclude` 四个命令    

- `module` 语句指定包的名字（路径）
- `require` 语句指定的依赖项模块
- `replace` 语句可以替换依赖项模块
- `exclude` 语句可以忽略依赖项模块

### 1.3.6. Practice demo

1. 在`GOPATH`目录之外新建一个目录，并使用`go mod init`初始化生成`go.mod`文件
```sh
mkdir hello
cd hello
hello go mod init hello
go: creating new go.mod: module hello
hello ls
go.mod
hello cat go.mod
module hello

go 1.12

```
> go.mod文件一旦创建后，它的内容将会被go toolchain全面掌控。go toolchain会在各类命令执行时，比如go get、go build、go mod等修改和维护go.mod文件。    

2. 添加依赖  
 
新建一个 server.go 文件，写入以下代码
```go
package main

import (
	"net/http"
	
	"github.com/labstack/echo"
)

func main() {
	e := echo.New()
	e.GET("/", func(c echo.Context) error {
		return c.String(http.StatusOK, "Hello, World!")
	})
	e.Logger.Fatal(e.Start(":1323"))
}
```

执行 go run server.go 运行代码会发现 go mod 会自动查找依赖自动下载：
```sh
$ go run server.go
go: finding github.com/labstack/echo v3.3.10+incompatible
go: downloading github.com/labstack/echo v3.3.10+incompatible
go: extracting github.com/labstack/echo v3.3.10+incompatible
go: finding github.com/labstack/gommon/color latest
go: finding github.com/labstack/gommon/log latest
go: finding github.com/labstack/gommon v0.2.8
# 此处省略很多行
...

   ____    __
  / __/___/ /  ___
 / _// __/ _ \/ _ \
/___/\__/_//_/\___/ v3.3.10-dev
High performance, minimalist Go web framework
https://echo.labstack.com
____________________________________O/_______
                                    O\
⇨ http server started on [::]:1323

```

现在查看go.mod 内容：
```go
$ cat go.mod


module hello

go 1.12

replace (
	golang.org/x/crypto => github.com/golang/crypto v0.0.0-20190313024323-a1f597ede03a
	golang.org/x/sys => github.com/golang/sys v0.0.0-20190602015325-4c4f7f33c9ed
)

require (
	github.com/labstack/echo v3.3.10+incompatible // indirect
	github.com/labstack/gommon v0.2.9 // indirect
	golang.org/x/crypto v0.0.0-20190313024323-a1f597ede03a // indirect
)

```
go 会自动生成一个 `go.sum` 文件来记录 dependency tree：
```sh
$ cat go.sum
github.com/davecgh/go-spew v1.1.0/go.mod h1:J7Y8YcW2NihsgmVo/mv3lAwl/skON4iLHjSsI+c5H38=
github.com/davecgh/go-spew v1.1.1/go.mod h1:J7Y8YcW2NihsgmVo/mv3lAwl/skON4iLHjSsI+c5H38=
github.com/golang/crypto v0.0.0-20190313024323-a1f597ede03a h1:fGZX4Nte1cGOVDzn8FwUXtYtefF0bklp8ulcm416hMQ=
github.com/golang/crypto v0.0.0-20190313024323-a1f597ede03a/go.mod h1:djNgcEr1/C05ACkg1iLfiJU5Ep61QUkGW8qpdssI0+w=
... 省略很多行
```

3. 再次执行脚本 `go run server.go` 发现跳过了检查并安装依赖的步骤。

4. 可以使用命令`go list -m -u all`来检查可以升级的package，使用`go get -u need-upgrade-package` 升级后会将新的依赖版本更新到go.mod * 也可以使用 `go get -u` 升级所有依赖



## 1.4. glide

### 1.4.1. install

> 外国人写的原版
``` go
go get github.com/Masterminds/glide
```

> 国内大神fix版，优先这个，fix window bug and can use --base options...
[more infomation](https://github.com/xkeyideal/glide)
``` go
go get github.com/xkeyideal/glide  
```

### 1.4.2. bug fix
> If you choose **xkeyideal/glide**,you can skip step 1. Just perform step 2 
1. update file: $GOPATH/src/github.com/Masterminds/glide/path/winbug.go
```go
// cmd := exec.Command("cmd.exe", "/c", "move", o, n) //将这一行(line:75)代码注释掉
cmd := exec.Command("cmd.exe", "/c", "xcopy /s/y", o, n+"\\") //新增这一行代码	
```

2. rebuild glide.go
```sh
cd $GOPATH/src/github.com/Masterminds/glide | cd $GOPATH/src/github.com/xkeyideal/glide 
go build glide
copy $GOPATH/src/github.com/Masterminds/glide/glide.exe $GOPATH/bin/glide.exe
```
### 1.4.3. base-command

```glide
glide create|init 初始化项目并创建glide.yaml文件.
glide install  创建glide.lock文件,安装依赖包到vendor目录下
glide get 获取单个包
　　--all-dependencies 会下载所有关联的依赖包
　　-s 删除所有版本控制，如.git
　　-v 删除嵌套的vendor
glide install 安装包
glide update|up 更新包
```

eq:
```go
glide get github.com/mattn/go-adodb
glide get --all-dependencies -s -v github.com/mattn/go-adodb
// 下载指定版本
glide get github.com/go-sql-driver/mysql#v1.2
```

### 1.4.4. mirror

```glide
// set
glide mirror set https://golang.org/x/sys/unix https://github.com/golang/sys --base golang.org/x/sys
// remove
glide mirror remove https://golang.org/x/sys/unix
```

```yaml
# ..\usr\.glide\mirrors.yaml
repos:
- original: https://golang.org/x/crypto
  repo: https://github.com/golang/crypto
- original: https://golang.org/x/crypto/acme/autocert
  repo: https://github.com/golang/crypto
  base: golang.org/x/crypto
- original: https://golang.org/x/sys/unix
  repo: https://github.com/golang/sys
  base: golang.org/x/sys
- original: https://golang.org/x/net
  repo: https://github.com/golang/net
- original: https://golang.org/x/sync
  repo: https://github.com/golang/sync
- original: https://golang.org/x/tools
  repo: https://github.com/golang/tools
- original: https://golang.org/x/grpc
  repo: https://github.com/golang/grpc
- original: https://golang.org/x/time
  repo: https://github.com/golang/time
```
