# Linux 服务器 Shell 脚本管理 SpringBoot jar 包（总结）

> 原文：[Linux服务器shell脚本启动、停止、重启SpringBoot jar包](https://juejin.cn/post/7314139049370927131)
> 作者：sum墨 · 发布于 2023 年前后（一线后端开发）
> 本文为该文章的要点提炼，保留核心脚本与两个关键踩坑提醒。

## 一、为什么要用脚本

SpringBoot 项目打包成 `xxx.jar` 后上生产环境运行，很多人直接 `java -jar xxx.jar &`。这样有两个毛病：

- 启动要带的参数（如 `-Dxxx=xxx`）会让命令又长又难记、容易写错；
- 后台运行、查状态、重启都要手动敲一串命令，不便维护。

结论：**用 shell 脚本把配置和命令统一维护起来**，对外只暴露 `start | stop | restart | status` 四个动作。

## 二、start.sh 脚本（核心）

```bash
#!/bin/bash
# 这里可替换为你自己的执行程序，其他代码无需更改
APP_NAME=xxx.jar

# 使用说明，用来提示输入参数
usage() {
    echo "Usage: sh 脚本名.sh [start|stop|restart|status]"
    exit 1
}

# 检查程序是否在运行
is_exist() {
  pid=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')
  # 不存在返回 1，存在返回 0
  if [ -z "${pid}" ]; then
    return 1
  else
    return 0
  fi
}

# 启动方法
start() {
  is_exist
  if [ $? -eq "0" ]; then
    echo "${APP_NAME} is already running. pid=${pid} ."
  else
    nohup java -jar /home/admin/$APP_NAME > /dev/null 2>&1 &
    echo "${APP_NAME} start success"
  fi
}

# 停止方法
stop() {
  is_exist
  if [ $? -eq "0" ]; then
    kill -9 $pid
  else
    echo "${APP_NAME} is not running"
  fi
}

# 输出运行状态
status() {
  is_exist
  if [ $? -eq "0" ]; then
    echo "${APP_NAME} is running. Pid is ${pid}"
  else
    echo "${APP_NAME} is NOT running."
  fi
}

# 重启
restart() {
  stop
  start
}

# 根据输入参数选择执行对应方法，不输入则执行使用说明
case "$1" in
  "start")   start ;;
  "stop")    stop ;;
  "status")  status ;;
  "restart") restart ;;
  *)         usage ;;
esac
```

要点：

- `is_exist()` 用 `ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}'` 取进程 pid，再据此判断启停；
- `start()` 通过 `nohup java -jar ... > /dev/null 2>&1 &` 后台拉起，**关键在 `> /dev/null`**（见下方注意事项）；
- `restart = stop + start`，`case` 分发参数。

## 三、使用方式

```bash
# 1. 给脚本授权
chmod 744 start.sh

# 2. 启动 / 停止 / 重启 / 查状态
./start.sh start
./start.sh stop
./start.sh restart
./start.sh status
```

## 四、注意事项（重点踩坑）

核心启动语句里第一个箭头后面是 `/dev/null`：

```bash
nohup java -jar /home/admin/$APP_NAME > /dev/null 2>&1 &
```

- **坑**：很多网上的脚本把 `/dev/null` 换成一个「指定的日志文件路径」，让 nohup 把所有输出写进这个文件。短期看着没问题，几个月后服务器就可能**存储被这个几十 GB 的日志文件撑满**，轻则命令失效、重则连不上服务器。
- **原因**：SpringBoot 的日常日志本来就用 logback / log4j 配好了，根本不需要 nohup 再输出一份。所以 nohup 的输出**直接丢给 `/dev/null` 即可**（`/dev/null` 用于丢弃不需要的输出流）。
- 这是作者「血与泪的教训」，务必注意。

## 五、开启远程 debug

把启动语句改成在 `java` 后加 JDWP 代理：

```bash
nohup java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar /home/admin/$APP_NAME > /dev/null 2>&1 &
```

- 即加上 `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005` 开启远程 debug；
- IDEA 里在 host / port 填远程服务 IP 和 `5005`，运行即可连接；
- **⚠️ 强烈提醒**：远程 debug 相当于一个「后门」，别人也能连上来。**调试完一定要记得删掉该参数**，不要长期开着。

## 六、小结

- 用 `start.sh` 统一管理 SpringBoot jar 的启停/重启/状态，比裸 `java -jar &` 更规范、好维护；
- 启动日志**必须丢 `/dev/null`**，不要重定向到文件，否则日志会悄悄吃满磁盘；
- 远程 debug 临时用，用完即删，避免留下安全隐患。
