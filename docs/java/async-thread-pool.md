---
title: Java 异步线程与线程池详解
---

# Java 异步线程与线程池详解

> 本文综合四篇掘金文章的要点（见文末「参考资料」），系统梳理 **Java 异步线程** 与 **线程池（ThreadPoolExecutor）** 的核心知识，并补充了 `CompletableFuture`、**结构化并发（Structured Concurrency）** 等现代异步写法。
>
> **建议先看下面的「概念全景速查」**——三张类型关系图和「按问题找答案」索引都在那里，可以直接跳到你要的章节。

## 概念全景速查（读前先看）

> 这一节是全篇的「地图」：先认清类型关系，再按问题索引直达章节，避免来回翻找。

### 三张关系图

**① 任务执行侧：谁继承谁（JDK）**

```
                  Executor «interface»
                  只定义 execute(Runnable)
                        △ extends
                  ExecutorService «interface»
                  + submit / invokeAll / invokeAny / shutdown / awaitTermination
                        △ implements（抽象类）
                  AbstractExecutorService
                  用 FutureTask 把 submit / invokeAll 统一实现好
                        △ extends
                  ThreadPoolExecutor «class»     ← 全文主角，7 个参数都在它身上
                        △ extends
                  ScheduledThreadPoolExecutor «class»
                  + implements ScheduledExecutorService（定时 / 周期）

   接口分支：Executor ──extends──▶ ScheduledExecutorService
   能力递进：「会跑任务」────▶「会跑任务 + 会管任务」────▶「最常用、最可配的具体实现」

   Executors（工具类，不在继承链上）──静态工厂 new──▶ 各种预置参数的 ThreadPoolExecutor
```

**② 任务与结果侧：谁实现谁、怎么串起来**

```
   【任务侧】描述「干什么」
     Runnable «interface»       void run()                     无返回值、不能抛受检异常
     Callable<V> «interface»    V call() throws Exception     有返回值、可抛受检异常
            │
            └─ submit() 时，线程池内部先包一层 FutureTask
                       ▽
   【桥接层】FutureTask «class»   implements Runnable + Future
             既能被线程执行，又能取出结果
                       │
            ┌──────────┴───────────┐
            ▽                      ▽
   【主动取结果】                  【完成时通知我】
     Future «interface»             Callback（编程模式，非具体类型）
     get / cancel / isDone          CompletableFuture.thenXxx
                                    ListenableFuture.addListener

   【合体】Future + CompletionStage = CompletableFuture
     Future «interface»                     CompletionStage «interface»
       get / cancel / isDone                  thenApply / thenCompose / thenCombine
                                              thenAccept / exceptionally / handle …
              ╲                                       ╱
               ╲────────── implements 二者 ──────────╱
                                ▽
                    CompletableFuture «class»
                    （额外：可由外部主动 complete / completeExceptionally）
```

**③ Spring 集成侧：Spring 的壳 + JDK 的芯**

```
   Spring 抽象（继承链）                        JDK 对应
   ────────────────────                        ─────────
   TaskExecutor «interface»        ≡ 镜像 ──▶   Executor
        △ extends
   AsyncTaskExecutor               + submit() 返回 Future
        △ extends
        ├──────────────────────────────┐
        ▽                              ▽
   AsyncListenableTaskExecutor    SchedulingTaskExecutor «interface»
   + ListenableFuture 回调         （标记：适合短任务调度）
        └──────────────┬───────────────┘
                       ▽  implements 上面两个接口
   ThreadPoolTaskExecutor «class»  ──⇢ 内部委托持有 ──▶  ThreadPoolExecutor
        └─ extends ExecutorConfigurationSupport
             └─ extends CustomizableThreadFactory   implements ThreadFactory
                （接入 Spring 生命周期：InitializingBean → initialize() 建池；
                  DisposableBean → 容器关闭时自动 shutdown 池）

   TaskScheduler «interface» ──△ implements── ThreadPoolTaskScheduler ⇢ ScheduledThreadPoolExecutor
```

> **三句话记住这三张图**：
> 1. **谁来跑** —— `Executor` → `ExecutorService` → `ThreadPoolExecutor`；`Executors` 只是帮你 `new` 的工厂，不在继承链上。
> 2. **结果怎么拿** —— `Future`（主动 `get`）+ `CompletionStage`（注册下一步）合体成 `CompletableFuture`；`Callback` 是「完成时通知我」的第三条路。
> 3. **Spring 只是壳** —— `TaskExecutor` 系列镜像 JDK 的 `Executor` 系列，`ThreadPoolTaskExecutor` 内部就是一个 `ThreadPoolExecutor`。

### 概念速查表

| 层 | 概念 | 一句话定位 | 章节 |
| --- | --- | --- | --- |
| 任务 | `Runnable` | 无返回值、不能抛受检异常的任务单元 | §2.6 |
| 任务 | `Callable<V>` | 有返回值、可抛受检异常的任务单元；必须走 `submit()` | §2.6 |
| 任务 | `FutureTask` | `Runnable` + `Future` 的桥接器（`submit` 时线程池内部就是它） | §2.6 |
| 结果 | `Future` | 异步结果「提货单」：`get` / `cancel` / `isDone` | §2.6 · §4.1 |
| 结果 | `CompletionStage` | 「某个阶段完成后做什么」的编排接口 | §4.2 |
| 结果 | `CompletableFuture` | `Future` + `CompletionStage` 合体，现代异步首选 | §4 |
| 通知 | `Callback` | 「结果就绪后通知我」，替代阻塞式 `get()` | §2.6 · §5.3 |
| 线程 | `Thread.State` | 线程 6 态：`NEW` / `RUNNABLE` / `BLOCKED` / `WAITING` / `TIMED_WAITING` / `TERMINATED` | §2.5 |
| 池 | `Executor` | 最顶层接口，只有 `execute(Runnable)` | §5.1 |
| 池 | `ExecutorService` | 会跑任务 + 会管任务（`submit` / `shutdown`） | §5.1 |
| 池 | `ThreadPoolExecutor` | 最可配的具体实现，7 个参数 | §3.2 |
| 池 | `Executors` | 静态工厂，快捷但有坑（生产禁用） | §5.2 |
| 池 | 5 种池状态 | `RUNNING` → `SHUTDOWN` / `STOP` → `TIDYING` → `TERMINATED` | §3.6 |
| 池 | 4 种拒绝策略 | 推荐 `CallerRunsPolicy` 做反压 | §3.5 |
| Spring | `TaskExecutor` | Spring 版 `Executor`，`@Async` 面向它编程 | §5.3 |
| Spring | `ThreadPoolTaskExecutor` | Spring 最常用实现，内部委托 `ThreadPoolExecutor` | §5.3 |
| Spring | `@Async` | 声明式异步；注意自调用失效与默认执行器陷阱 | §5.4 |
| 进阶 | `StructuredTaskScope` | JDK 官方结构化并发（截至 JDK 27 仍未转正） | §6.6 |
| 进阶 | `ThreadForge` | 第三方结构化并发库，JDK 8+ 可用 | §6 |

### 按问题找答案

| 我想知道…… | 去哪看 |
| --- | --- |
| 创建线程有哪几种方式 | §2.3 |
| 线程有哪几个状态、怎么转换 | §2.5 |
| `Task` / `Runnable` / `Callable` / `Future` / `Callback` 到底什么关系 | §2.6 |
| 线程池的 7 个参数分别是什么 | §3.2 |
| 任务提交后线程池内部怎么走 | §3.3 |
| `submit()` 和 `execute()` 有什么区别 | §3.3 |
| 队列满了会怎样 | §3.5 |
| 线程池有哪几种状态、`shutdown` 和 `shutdownNow` 差在哪 | §3.6 · §3.10 |
| 任务里的异常为什么「消失了」 | §3.8 · §4.7 |
| 线程数到底设多少 | §3.9 |
| 为什么不能直接用 `Executors` | §5.2 |
| `CompletableFuture` 的 `then` 系列怎么选 | §4.4 · §4.6 |
| 带 `Async` 后缀和不带有什么区别 | §4.5 |
| 「并发调 N 个接口再聚合」怎么写最省心 | §4.9 · §6.4 |
| Spring Boot 里怎么配异步线程池 | §5.4 · §5.6 |
| 有没有比 `CompletableFuture` 更新的做法 | §6 |

---

## 一、参考文章内容总结

| 文章 | 作者 | 核心视角 |
| --- | --- | --- |
| [面试必备：Java线程池解析](https://juejin.cn/post/6844903889678893063) | 捡田螺的小男孩 | 以经典面试题切入，讲清线程池参数、执行流程、拒绝策略、异常处理、工作队列、常用线程池、线程池状态 |
| [如何优雅的使用和理解线程池](https://juejin.cn/post/6844903648405766158) | crossoverJie | 从池化思想讲起，覆盖 `execute()` 流程、线程数配置、优雅关闭、SpringBoot 集成、监控、线程池隔离（Hystrix） |
| [Java—线程池 ThreadPoolExecutor 详解](https://juejin.cn/post/6844904146856837128) | Andya | 围绕 `ThreadPoolExecutor` 的 7 个参数、工作流程、5 种状态、4 种标准线程池源码、拒绝策略与阿里规范展开 |
| [Java多线程神器——ThreadForge，让多线程从此简单](https://juejin.cn/post/7604779604126138368) | 一只叫煤球的猫 | **扩展**：从「并发调三个接口写了 50 行」切入，介绍**结构化并发**框架 ThreadForge（作用域边界、失败策略、内置限流、统一观测、跨 JDK 一致体验） |

**几篇文章的共识：**
- 线程是稀缺资源，**不能频繁创建/销毁**，必须用线程池复用（阿里 Java 手册强制要求）。
- 线程池的本质是「池化技术」：核心线程≈正式员工、非核心线程≈外包、阻塞队列≈需求池、拒绝策略≈拒单。
- 直接用 `Executors` 工厂方法存在隐患（`newFixedThreadPool`/`newSingleThreadExecutor` 用无界队列易 OOM，`newCachedThreadPool` 可能创建过多线程），**生产环境建议手动 `new ThreadPoolExecutor(...)`**。

---

## 二、Java 异步与线程基础

### 2.1 进程 vs 线程

| 维度 | 进程 | 线程 |
| --- | --- | --- |
| 定义 | 操作系统**分配资源**的最小单位 | CPU **调度**的最小单位 |
| 内存 | 各自独立的地址空间 | 共享所属进程的堆 / 方法区，各自独立栈与程序计数器 |
| 隔离性 | 强：一个进程崩溃通常不影响其他进程 | 弱：一个线程 OOM / 崩溃可能拖垮整个进程 |
| 通信成本 | 高（管道、消息队列、共享内存、Socket） | 低（直接读写共享变量，但需处理并发安全） |
| 创建 / 切换开销 | 大 | 小（但仍远大于一次普通方法调用） |
| 一句话 | 资源边界 | 执行单元 |

### 2.2 为什么需要异步 / 多线程

| 目标 | 说明 | 典型场景 |
| --- | --- | --- |
| 提升吞吐 | 把耗时的 IO / 计算任务从主线程剥离，避免阻塞主流程 | 接口里发通知、写日志、调三方 |
| 并行计算 | 多核 CPU 下真正并行执行 CPU 密集任务 | 大批量计算、图片 / 编解码处理 |
| 缩短响应 | 多个互不依赖的远程调用并行发起 | 详情页聚合 N 个下游接口（§4.9） |
| 解耦 | 任务的「提交」与「执行」分离，便于统一调度、监控、限流 | 统一线程池 + 统一观测埋点 |

### 2.3 创建线程的几种方式

| 方式 | 特点 | 能否返回结果 / 抛异常 |
| --- | --- | --- |
| 继承 `Thread` 重写 `run()` | 简单，但耦合、单继承受限 | 否 |
| 实现 `Runnable` | 解耦，可丢进线程 / 线程池 | 否（`run` 无返回值） |
| 实现 `Callable` + `Future` | 可返回结果、可抛受检异常 | 是（`Future.get()` 获取） |
| 线程池 `ExecutorService` | 复用线程、统一管控 | 取决于 `execute` / `submit` |
| `CompletableFuture`（Java 8+） | 链式异步编排，现代首选 | 是，且支持回调 |

```java
// 最朴素的异步：直接 new Thread
new Thread(() -> System.out.println("hello async")).start();

// 更规范：实现 Runnable
Runnable task = () -> System.out.println("task running");
new Thread(task).start();
```

### 2.4 同步 vs 异步

| 维度 | 同步（阻塞） | 异步（非阻塞） |
| --- | --- | --- |
| 调用方行为 | 必须等被调用方返回才继续 | 提交后立即返回，继续干别的 |
| 结果获取 | 直接拿返回值 | `Future.get()` / 回调 / `thenXxx` 后续阶段 |
| 异常处理 | `try-catch` 就地处理 | 需专门机制（`ExecutionException`、`exceptionally` / `handle`） |
| 调用链可读性 | 直观，但慢 | 快，但链路变成「声明式」，调试更麻烦 |
| 典型 API | 普通方法调用 | `ExecutorService.submit`、`CompletableFuture`、`@Async` |

```java
// 同步：会阻塞当前线程
String result = blockingCall();

// 异步：提交即返回 Future，稍后取结果
ExecutorService pool = Executors.newFixedThreadPool(2);
Future<String> future = pool.submit(() -> longRunningTask());
// ... 干别的事 ...
String result = future.get(); // 真正需要结果时再阻塞
```

### 2.5 线程的生命周期（6 种状态）

Java 用 `Thread.State` 枚举定义了线程从「出生」到「消亡」的 6 种状态：

| 状态 | 含义 | 典型进入方式 |
| --- | --- | --- |
| `NEW` | 新建，尚未调用 `start()` | `new Thread()` |
| `RUNNABLE` | 可运行（可能正在 CPU 上跑，也可能在就绪队列等调度） | `start()`；从 `BLOCKED` / `WAITING` 恢复 |
| `BLOCKED` | 阻塞，等待进入 `synchronized` 监视器锁 | 竞争 `synchronized` 锁失败 |
| `WAITING` | 无限期等待，需其他线程显式唤醒 | `Object.wait()` / `thread.join()` / `LockSupport.park()` |
| `TIMED_WAITING` | 限期等待，超时后自动恢复 | `Thread.sleep(n)` / `wait(n)` / `join(n)` / `parkNanos(n)` |
| `TERMINATED` | 终止，`run()` 正常结束或抛异常退出 | — |

```
   new Thread()          start()                    run() 结束 / 抛异常
   ──────────▶ NEW ──────────────▶ RUNNABLE ─────────────────────▶ TERMINATED
                                     │  ▲                          （不可再 start）
                                     │  │ 拿到锁 / 被唤醒 / 超时到
                    ┌────────────────┘  │
                    ├─ 竞争 synchronized 锁失败 ─────▶ BLOCKED ────────┘
                    ├─ wait() / join() / park()  ────▶ WAITING ────────┘
                    └─ sleep(n) / wait(n) / join(n) ▶ TIMED_WAITING ──┘
```

| 纠偏点 | 说明 |
| --- | --- |
| `RUNNABLE` 不细分「就绪」和「运行中」 | JVM 把 OS 的就绪与运行合并为一个状态 |
| `BLOCKED` **只**由 `synchronized` 引起 | `ReentrantLock` 的等待走的是 `WAITING` / `TIMED_WAITING`（底层 `LockSupport.park`）——排查死锁 / 卡顿必须分清 |
| `TERMINATED` 不可复用 | 再次 `start()` 会抛 `IllegalThreadStateException` |
| 怎么定位「卡在哪把锁」 | `jstack <pid>` 导出的线程栈里，状态字段就是上表中的值，可据此定位 |

### 2.6 核心概念辨析：Task / Runnable / Callable / Future / FutureTask / Callback

这几个词天天见，但**层级完全不同**，先用一张表钉死：

| 概念 | 类型 / 签名 | 有返回值 | 可抛受检异常 | 结果怎么拿 | 一句话定位 |
| --- | --- | --- | --- | --- | --- |
| **Task** | 业务语义（非具体类型） | — | — | — | 「要被执行的一段工作」的**统称**，Java 里由 `Runnable` / `Callable` 表达 |
| **`Runnable`** | 接口 `void run()` | ❌ | ❌ | — | 最基础的**任务单元**，可直接给 `Thread` 或 `execute()` |
| **`Callable<V>`** | 接口 `V call() throws Exception` | ✅ | ✅ | `Future.get()` | **有返回值的任务单元**；`Thread` 只认 `Runnable`，所以必须走 `submit()` |
| **`Future`** | 接口 | — | — | `get()` / `get(timeout)` | 异步结果的**凭证 / 提货单**：`get`、`cancel`、`isDone` |
| **`FutureTask`** | 类，`implements Runnable, Future` | — | — | 同上 | **桥接器**：把 `Callable` 包成「既能被线程执行、又能取结果」的对象 |
| **`Callback`** | 编程模式（非具体类型） | — | — | 框架主动回调 | 「结果就绪后**通知我**」，避免阻塞式 `get()` |

**它们是怎么串起来的**（这是理解整个异步模型的关键）：

```
   你要执行的东西                线程池内部                    你能拿到什么
   ──────────────               ──────────                   ────────────
   Runnable   ──submit──┐
                        ├──▶ 包装成 FutureTask ──▶ 交给线程 ──┬──▶ Future（句柄）
   Callable<V> ─────────┘      （Runnable + Future）          │     你主动 get() 取结果
                                                              │
                                                              └──▶ Callback（通知）
                                                                   结果就绪时框架主动回调你
                                                                   （CompletableFuture.thenXxx
                                                                     / ListenableFuture.addListener）
```

```java
// Runnable：无返回值的任务单元
Runnable r = () -> System.out.println("done");

// Callable：带返回值、可抛异常的任务单元
Callable<String> c = () -> "result";

ExecutorService pool = Executors.newFixedThreadPool(2);

// Runnable 也能 submit，返回 Future<?>（结果为 null）
Future<?> f1 = pool.submit(r);

// Callable submit 后得到带结果的 Future（底层是 FutureTask）
Future<String> f2 = pool.submit(c);
String res = f2.get(); // 主动阻塞取结果

// Callback：用 CompletableFuture 实现「结果就绪后自动回调」
CompletableFuture.supplyAsync(() -> "result", pool)
        .thenAccept(res2 -> System.out.println("回调收到: " + res2));
```

> **一句话收口**：`Runnable` / `Callable` 描述「**干什么**」，`Future` 是「干完后的**凭证**」，`Callback` 是「干完后**通知我**的钩子」，而 `Task` 是它们的**总称**。

---

## 三、线程池（ThreadPoolExecutor）详解

### 3.1 为什么用线程池

| 收益 | 说明 | 不用池的代价 |
| --- | --- | --- |
| 降低开销 | 复用已创建的线程 | 频繁创建 / 销毁线程，类加载 + GC 成本高 |
| 提升响应速度 | 任务到达直接取空闲线程执行 | 每次都要等线程创建完成 |
| 便于管控 | 统一命名、监控、限流、优雅关闭、线程池隔离 | 线程散落各处，无法统一治理 |
| 防止资源耗尽 | 线程数有上限，天然限流 | 无限起线程 → 疯狂上下文切换直至宕机 |

### 3.2 七个核心参数

```java
public ThreadPoolExecutor(
    int corePoolSize,                // 核心线程数
    int maximumPoolSize,             // 最大线程数（核心 + 非核心）
    long keepAliveTime,              // 非核心线程空闲存活时间
    TimeUnit unit,                   // 存活时间单位
    BlockingQueue<Runnable> workQueue, // 任务阻塞队列
    ThreadFactory threadFactory,     // 线程工厂（建议给线程起名）
    RejectedExecutionHandler handler  // 拒绝（饱和）策略
)
```

| # | 参数 | 含义 | 建议 / 坑 |
| --- | --- | --- | --- |
| 1 | `corePoolSize` | 常驻核心线程数 | 默认一直存活，除非 `allowCoreThreadTimeOut(true)` |
| 2 | `maximumPoolSize` | 线程总数上限（核心 + 非核心） | 与 `corePoolSize` 一起决定「何时扩容」与「何时拒绝」 |
| 3 | `keepAliveTime` | 非核心线程空闲存活时间 | 配合 `allowCoreThreadTimeOut(true)` 也可回收核心线程 |
| 4 | `unit` | `keepAliveTime` 的时间单位 | `TimeUnit.SECONDS` 等 |
| 5 | `workQueue` | 任务阻塞队列 | **必须用有界队列**，无界队列会 OOM（§3.4） |
| 6 | `threadFactory` | 线程工厂 | 一定要给线程起名（如 `biz-pool-%d`），否则出问题只能看 `pool-1-thread-3` |
| 7 | `handler` | 队列与线程都满时的拒绝策略 | 推荐 `CallerRunsPolicy` 做反压（§3.5） |

### 3.3 任务提交流程（execute）

```
提交任务
  │
  ├─ 当前线程数 < corePoolSize？
  │     是 → 新建【核心线程】执行
  │     否 ↓
  ├─ 任务成功入队 workQueue？
  │     是 → 排队等待空闲线程取走
  │     否 ↓
  ├─ 当前线程数 < maximumPoolSize？
  │     是 → 新建【非核心线程】执行
  │     否 ↓
  └─ 触发【拒绝策略】
```

> 生活类比：正式员工（核心线程）先接需求；忙不过来就放进需求池（队列）；池子也满了就请外包（非核心线程）；全员满负荷就启动拒单流程（拒绝策略）。

| 对比项 | `execute(Runnable)` | `submit(...)` |
| --- | --- | --- |
| 返回值 | 无（`void`） | `Future<T>`（`Runnable` → `Future<?>`，`Callable` → `Future<T>`） |
| 能提交 `Callable` 吗 | ❌ 只能 `Runnable` | ✅ 两者都行 |
| 异常怎么暴露 | 直接抛给线程的 `UncaughtExceptionHandler`（**可能被静默吞掉**） | 被「冻结」在 `Future` 里，必须 `get()` 才抛 `ExecutionException` |
| 是否方便取结果 | ❌ | ✅ |
| 适用 | 只关心「跑掉」，不关心结果 | 需要结果 / 需要感知异常（**推荐**） |

### 3.4 五种阻塞队列

| 队列 | 特点 | 典型使用 |
| --- | --- | --- |
| `ArrayBlockingQueue` | 数组实现、有界 FIFO | 手动指定容量，最可控 |
| `LinkedBlockingQueue` | 链表 FIFO，**默认无界**（上限 `Integer.MAX_VALUE`） | `newFixedThreadPool` / `newSingleThreadExecutor` |
| `SynchronousQueue` | 不存元素，直接移交，每个入队需等待出队 | `newCachedThreadPool` |
| `DelayQueue` | 按延迟时间排序的延迟队列 | `newScheduledThreadPool` |
| `PriorityBlockingQueue` | 支持优先级的无界队列 | 需按优先级调度的场景 |

> ⚠️ **无界队列的坑**：`newFixedThreadPool` 使用无界 `LinkedBlockingQueue`，若任务执行慢、提交快，队列会无限堆积，最终导致 **OOM**。生产环境推荐用有界队列 + 合理拒绝策略。

### 3.5 四种拒绝策略

| 策略 | 行为 | 丢任务？ | 抛异常？ | 有反压？ | 适用 |
| --- | --- | --- | --- | --- | --- |
| `AbortPolicy`（**默认**） | 抛出 `RejectedExecutionException` | 否 | ✅ | ❌ | 需要明确感知「已被打满」的核心链路 |
| `DiscardPolicy` | 静默丢弃新任务 | ✅ | ❌ | ❌ | 允许丢的非关键任务（不推荐：问题会被隐藏） |
| `DiscardOldestPolicy` | 丢弃队列中最旧的任务，再重试提交当前任务 | ✅（丢旧的） | ❌ | ❌ | 只关心「最新」的任务，如实时行情刷新 |
| `CallerRunsPolicy` | 由**调用者线程**自己执行该任务 | 否 | ❌ | ✅ | **生产推荐**：拖慢上游提交速度，形成天然背压 |

```java
RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
```

### 3.6 五种线程池状态

```
                     ┌── shutdown() ───▶ SHUTDOWN ──┐  队列空 & 线程数为 0
   RUNNING ──────────┤                             ├──▶ TIDYING ──terminated()──▶ TERMINATED
   （收新任务 +        └── shutdownNow() ─▶ STOP ────┘  任务全空
     跑队列任务）                          （不收新任务、不跑队列、中断在跑的）
```

| 状态 | 接收新任务 | 处理队列中已有任务 | 中断正在执行的任务 | 如何进入 |
| --- | --- | --- | --- | --- |
| `RUNNING` | ✅ | ✅ | ❌ | 线程池创建后即为此状态 |
| `SHUTDOWN` | ❌ | ✅（执行完为止） | ❌ | 调用 `shutdown()` |
| `STOP` | ❌ | ❌（直接丢弃） | ✅ | 调用 `shutdownNow()` |
| `TIDYING` | ❌ | ❌ | ❌ | 队列空且线程数为 0，准备终止 |
| `TERMINATED` | ❌ | ❌ | ❌ | `terminated()` 钩子执行完毕 |

### 3.7 四种常用线程池（及隐患）

`Executors` 的 4 个常用工厂方法，本质只是**不同参数的 `ThreadPoolExecutor`**。看懂这张表就够用了：

| 线程池 | `core` | `max` | 队列 | 特点 | 风险 |
| --- | --- | --- | --- | --- | --- |
| `FixedThreadPool` | `n` | `n` | `LinkedBlockingQueue`（**无界**） | 线程数固定；适合 CPU 密集、长期任务 | 队列无上限 → 任务堆积 → **OOM** |
| `CachedThreadPool` | `0` | `Integer.MAX_VALUE` | `SynchronousQueue` | 来一个任务建一个线程，空闲 60s 回收 | 并发高时**线程数爆炸**，耗尽资源 |
| `SingleThreadExecutor` | `1` | `1` | `LinkedBlockingQueue`（**无界**） | 单线程串行，保证顺序 | 同样有 **OOM** 风险 |
| `ScheduledThreadPool` | `n` | `Integer.MAX_VALUE` | `DelayedWorkQueue` | 定时 / 周期任务（`scheduleAtFixedRate` 等） | `max` 极大，任务堆积会猛涨线程 |

对应源码（看清「参数到底传了什么」）：

```java
// 1) FixedThreadPool
new ThreadPoolExecutor(n, n, 0L, MILLISECONDS, new LinkedBlockingQueue<>());
//                   ^core ^max  ^keepAlive      ^无界队列！上限 Integer.MAX_VALUE

// 2) CachedThreadPool
new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, SECONDS, new SynchronousQueue<>());
//                   ^core=0  ^max 极大                     ^不存元素，直接移交

// 3) SingleThreadExecutor
new ThreadPoolExecutor(1, 1, 0L, MILLISECONDS, new LinkedBlockingQueue<>());

// 4) ScheduledThreadPool
new ScheduledThreadPoolExecutor(core, new DelayedWorkQueue());
```

**结论**：阿里规范不推荐直接用 `Executors` 快捷方法，应手动创建并明确队列容量、线程上下限和拒绝策略（详见 §5.2）。

### 3.8 线程池异常处理

任务里抛 `RuntimeException` 时，**用 `execute()` 提交，异常会直接打到线程的未捕获异常处理器（很容易被静默吞掉）；用 `submit()` 提交，异常被「冻结」在 `Future` 里，直到有人 `get()` 才暴露**。五种兜底方案：

| 方案 | 生效范围 | 能拿到返回值 | 说明 / 代价 |
| --- | --- | --- | --- |
| 任务内 `try/catch` | 单个任务 | ✅ | 最直接，但每个任务都要写，容易漏 |
| `Future.get()` 捕获 | 单个任务 | ✅ | `submit()` 提交的任务，`get()` 时抛 `ExecutionException`；缺点是要阻塞取值 |
| `UncaughtExceptionHandler` | 该线程工厂创建的所有线程 | ❌ | 给 `Thread` 设置；只对「线程级未捕获异常」生效 |
| 重写 `afterExecute` | **该池的所有任务** | ❌ | 继承 `ThreadPoolExecutor` 重写 `afterExecute(Runnable, Throwable)`，**统一收口**（JDK 文档推荐） |
| `exceptionally` / `handle` | 单条异步链 | ✅ | 现代异步写法，见 §4.7 |

```java
ExecutorService pool = Executors.newFixedThreadPool(1, r -> {
    Thread t = new Thread(r);
    t.setUncaughtExceptionHandler((t1, e) -> System.out.println(t1.getName() + " 异常: " + e));
    return t;
});
```

### 3.9 线程数配置建议

线程池并非越大越好。按任务性质区分：

| 任务类型 | 建议线程数 | 理由 |
| --- | --- | --- |
| **CPU 密集型**（大量计算 / 编解码） | ≈ CPU 核心数（`Runtime.getRuntime().availableProcessors()`） | 线程再多也只能排队等 CPU，反而增加上下文切换开销 |
| **IO 密集型**（网络 / 数据库 / 磁盘） | `CPU 核心数 × 2` 起步，甚至更多 | 线程大部分时间在等待，可以多开 |
| 通用折中 | 先按上面给初值，**再用压测收敛** | 没有万能公式，最佳值取决于依赖的 RT 分布 |

> 更精细的估算（IO 密集）：`线程数 ≈ CPU 核心数 × (1 + 平均等待时间 / 平均计算时间)`。

### 3.10 优雅关闭

```java
pool.shutdown(); // 不再接收新任务，已有任务继续执行完毕
// 或 pool.shutdownNow(); 直接中断所有任务并清空队列

// 阻塞等待终止，超时则强制退出
try {
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow();
    }
} catch (InterruptedException e) {
    pool.shutdownNow();
    Thread.currentThread().interrupt();
}
```

| 方法 | 接收新任务 | 队列中已排队的任务 | 正在执行的任务 | 使用场景 |
| --- | --- | --- | --- | --- |
| `shutdown()` | ❌ | ✅ 继续执行完 | 等它跑完 | **推荐**：平滑下线 |
| `shutdownNow()` | ❌ | ❌ 丢弃（返回未执行的任务列表） | 发送中断（`interrupt()`） | 超时后强制退出 / 兜底 |
| `awaitTermination(t, unit)` | — | — | 阻塞等待终止，返回是否已终止 | 配合 `shutdown()` 使用，超时后再 `shutdownNow()` |

> 注意：`shutdownNow()` 只是**发中断信号**。如果任务里是死循环、或阻塞在不可中断的 IO 上，它依然停不下来。

### 3.11 SpringBoot 中使用线程池

```java
@Configuration
public class ThreadPoolConfig {

    @Bean("bizThreadPool")
    public ExecutorService bizThreadPool() {
        ThreadFactory namedFactory = new ThreadFactoryBuilder()
                .setNameFormat("biz-pool-%d").build();
        return new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100), namedFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
```

使用时直接注入：

```java
@Resource(name = "bizThreadPool")
private ExecutorService bizThreadPool;
```

还可通过 Actuator 暴露监控端点，或继承 `ThreadPoolExecutor` 重写 `beforeExecute` / `afterExecute` / `terminated` 做自定义埋点。

### 3.12 线程池隔离

多个业务共用一个线程池时，某业务把线程耗尽会导致其他业务全部瘫痪（如 Tomcat 接收线程池被慢请求占满）。应对：

- **按业务拆分线程池**：下单用一个池、查询用另一个池，互不影响。
- **Hystrix 线程隔离**：用 `HystrixCommand` 为不同 Command 指定独立线程池（`HystrixThreadPoolKey`），实现依赖隔离与熔断降级。

```java
public class CommandOrder extends HystrixCommand<String> {
    public CommandOrder(String name) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("OrderGroup"))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("OrderPool"))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(10).withMaxQueueSize(10)));
    }
    @Override public String run() { /* 下单逻辑 */ return "ok"; }
}
```

---

## 四、CompletableFuture 详解（概念 / 使用 / 原理）

`CompletableFuture`（Java 8 引入）是目前 Java 异步编程的「集大成者」。它**同时实现了 `Future` 和 `CompletionStage` 两个接口**，把「线程池执行」与「异步结果编排」合二为一，是编写非阻塞、可组合业务代码的首选。

### 4.1 为什么需要 CompletableFuture：先看清 `Future` 的痛

JDK 5 的 `Future` 解决了「提交任务后拿凭证」的问题，但有几个硬伤：

| `Future` 的局限 | 说明 | 后果 |
| --- | --- | --- |
| **只能阻塞取结果** | `get()` 要么一直等，要么带超时等，调用线程被卡住 | 无法真正「非阻塞」 |
| **无法链式组合** | 想「A 算完 → 拿结果做 B → 再拿结果做 C」只能嵌套 `get()` | 代码退化成回调地狱 / 同步等待 |
| **没有回调机制** | 结果就绪后无法主动通知，只能轮询 `isDone()` | 浪费 CPU，且不及时 |
| **异常难处理** | 异常被包成 `ExecutionException`，要 `get()` 时才暴露 | 容易吞异常 |

`CompletableFuture` 的设计目标就是补齐这些短板：**声明式、可组合、带回调、支持编排**。

```java
// 旧 Future：阻塞、不可组合
Future<String> f = pool.submit(() -> remoteCall());
String r = f.get();          // 必须阻塞，无法在这一行做"结果转换后继续异步"

// CompletableFuture：非阻塞、可链式
CompletableFuture.supplyAsync(() -> remoteCall(), pool)
        .thenApply(String::toUpperCase)         // 结果转换，仍在异步
        .thenAccept(System.out::println);        // 消费结果，不阻塞主线程
```

### 4.2 核心概念：Future vs CompletionStage vs CompletableFuture

| 接口 / 类 | 是什么 | 给你什么能力 | 关键方法 |
| --- | --- | --- | --- |
| `Future` | 「异步结果句柄」接口 | **拿结果**（被动） | `get()` / `get(timeout)` / `cancel()` / `isDone()` |
| `CompletionStage` | 「异步计算的某个阶段」接口 | **编排下一步** | `thenApply` / `thenAccept` / `thenCompose` / `thenCombine` / `allOf` / `exceptionally` / `handle` |
| `CompletableFuture` | 同时 `implements Future, CompletionStage` 的类 | 上述两者**合体**，且可**由外部主动完成** | 上面全部 + `complete()` / `completeExceptionally()` / `orTimeout()` |

实现关系：

```
   Future «interface»                      CompletionStage «interface»
     get / cancel / isDone                   thenApply / thenCompose / thenCombine
                                             thenAccept / exceptionally / handle …
              ╲                                        ╱
               ╲────────── implements 二者 ──────────╱
                                ▽
                    CompletableFuture «class»
                    （额外：可由外部主动 complete / completeExceptionally）
```

**一句话**：`Future` 给你「**拿结果**」的能力，`CompletionStage` 给你「**编排下一步**」的能力，二者在 `CompletableFuture` 里合体；多出来的 `Completable` 意思是——允许**从外部主动把它置为完成**（`complete()` / `completeExceptionally()`），常用于测试桩与超时兜底。

### 4.3 创建异步任务（3 种入口）

| 方法 | 入参 | 返回值 | 用途 |
| --- | --- | --- | --- |
| `supplyAsync(Supplier)` | 有返回值的任务 + 可选 `Executor` | `CompletableFuture<U>` | 需要计算结果 |
| `runAsync(Runnable)` | 无返回值的任务 + 可选 `Executor` | `CompletableFuture<Void>` | 只执行、不关心结果 |
| `completedFuture(value)` | 一个已就绪的值 | 已完成的 `CompletableFuture` | 快速构造、作组合起点 |

```java
ExecutorService pool = new ThreadPoolExecutor(4, 8, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100));

// 有返回值的异步任务
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> remoteCall(), pool);

// 无返回值的异步任务
CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> log("done"), pool);

// 直接构造一个"已完成"的结果（不真正异步，常用于组合起点/默认值）
CompletableFuture<String> f3 = CompletableFuture.completedFuture("default");
```

> ⚠️ **务必显式传入线程池**：不带 `Executor` 的重载会使用 `ForkJoinPool.commonPool()`——它是**所有不指定池的异步任务共享的全局池**，一旦某个任务阻塞/变慢，会拖垮整个进程里的所有 `CompletableFuture`。生产环境一定传自定义池。

### 4.4 链式编排：thenApply / thenAccept / thenRun

这三者都是「上一阶段完成后做什么」，区别在于「要不要拿结果、要不要返回新结果」：

| 方法 | 入参 | 拿到上一阶段结果？ | 返回新结果？ | 典型用途 |
| --- | --- | --- | --- | --- |
| `thenApply(fn)` | `Function<T, R>` | ✅ | ✅ | 结果转换（map） |
| `thenAccept(fn)` | `Consumer<T>` | ✅ | ❌ | 消费结果（如写日志、入库） |
| `thenRun(fn)` | `Runnable` | ❌ | ❌ | 仅做动作（如发通知），不关心结果 |

```java
CompletableFuture.supplyAsync(() -> "hello", pool)
        .thenApply(s -> s + " world")        // 转换: "hello world"
        .thenApply(String::toUpperCase)      // 再转换: "HELLO WORLD"
        .thenAccept(System.out::println)     // 消费: 打印
        .thenRun(() -> System.out.println("所有阶段完成")); // 收尾动作
```

### 4.5 同步 vs 异步后缀（最关键的使用细节）

`CompletableFuture` 里**几乎所有 `thenXxx` 都有两个版本**，这是最容易踩坑的细节：

| 写法 | 在哪个线程执行 | 何时用 |
| --- | --- | --- |
| `thenApply(fn)`（无 `Async`） | **完成上一阶段的那个线程**（若上一阶段已同步完成，则在调用线程） | 轻量的纯内存转换，省一次线程调度 |
| `thenApplyAsync(fn)` | 提交到 **`ForkJoinPool.commonPool()`**（共享池） | 不关心线程归属时；但要警惕共享池被拖垮 |
| `thenApplyAsync(fn, pool)` | 提交到**你指定的线程池** | **生产推荐**：CPU 重的后续步骤、需要隔离的任务 |

```java
CompletableFuture.supplyAsync(() -> fetchData(), pool)
        .thenApplyAsync(data -> heavyTransform(data), pool)  // 重的转换丢到 pool 跑
        .thenAccept(result -> System.out.println(result));
```

经验法则：**CPU 重的后续步骤用 `xxxAsync(pool)` 指定池执行，避免占用回调线程（可能是 IO 线程）**；轻量的纯内存转换用无 Async 版本即可。

### 4.6 任务组合（CompletableFuture 的真正强项）

组合类方法一次看全，再按需看下面的细节：

| 方法 | 语义 | 依赖关系 | 返回 |
| --- | --- | --- | --- |
| `thenCompose(fn)` | 把「结果 → 另一个 Future」**扁平化**（类似 `flatMap`） | **串行**：后一个依赖前一个的结果 | `CompletableFuture<U>` |
| `thenCombine(other, fn)` | 两个**独立**任务都完成后合并两个结果 | **并行**：互不依赖 | `CompletableFuture<V>` |
| `thenAcceptBoth(other, fn)` | 同上，但只消费、不返回 | 并行 | `CompletableFuture<Void>` |
| `runAfterBoth(a, b, runnable)` | 两个都完成后跑一个 `Runnable` | 并行 | `CompletableFuture<Void>` |
| `allOf(cf...)` | 等**全部**完成（任一失败即整体失败） | 并行聚合 | `CompletableFuture<Void>` |
| `anyOf(cf...)` | 等**任意一个**先完成（竞速） | 并行竞速 | `CompletableFuture<Object>` |

> 选型口诀：**「结果要去发下一个异步请求」用 `thenCompose`；「两个结果要合并」用 `thenCombine`；「凑齐一批」用 `allOf`；「谁快用谁」用 `anyOf`。**

#### 4.6.1 thenCompose —— 扁平化（类似 flatMap）

当一个异步结果要用来**再发起另一个异步调用**时，用 `thenCompose` 避免 `CompletableFuture<CompletableFuture<T>>` 的嵌套：

```java
CompletableFuture<User> getUser = CompletableFuture.supplyAsync(() -> findUser(id), pool);

// 错误写法：得到 CompletableFuture<CompletableFuture<Order>>
getUser.thenApply(u -> CompletableFuture.supplyAsync(() -> findOrder(u)));

// 正确写法：thenCompose 把内层 Future 展平
CompletableFuture<Order> orderCf = getUser.thenCompose(u -> CompletableFuture.supplyAsync(() -> findOrder(u), pool));
```

#### 4.6.2 thenCombine —— 合并两个独立结果

两个任务**互不依赖**、可并行跑，都完成后合并：

```java
CompletableFuture<String> userCf  = CompletableFuture.supplyAsync(() -> "user", pool);
CompletableFuture<String> orderCf = CompletableFuture.supplyAsync(() -> "order", pool);

// 两个都完成后，用 BiFunction 合并
CompletableFuture<String> combined = userCf.thenCombine(orderCf,
        (user, order) -> user + " / " + order);
```

相近方法：`thenAcceptBoth`（只消费、不返回）、`runAfterBoth`（都不关心结果，都完成后跑 `Runnable`）。

#### 4.6.3 allOf / anyOf —— 等待多个任务


```java
CompletableFuture<String> a = CompletableFuture.supplyAsync(() -> svcA(), pool);
CompletableFuture<String> b = CompletableFuture.supplyAsync(() -> svcB(), pool);
CompletableFuture<String> c = CompletableFuture.supplyAsync(() -> svcC(), pool);

// 全部完成后统一汇总（join() 在 allOf 之后拿各自结果）
CompletableFuture<Void> all = CompletableFuture.allOf(a, b, c);
CompletableFuture<List<String>> result = all.thenApply(v ->
        Stream.of(a, b, c).map(CompletableFuture::join).collect(Collectors.toList()));

// 谁快用谁
CompletableFuture<Object> fastest = CompletableFuture.anyOf(a, b, c);
```

> `allOf` 返回 `Void`，要拿每个子结果需用 `join()`（在 `allOf` 已完成的前提下调用不会阻塞）。`anyOf` 返回 `Object`，需自行强转。

### 4.7 异常处理

`CompletableFuture` 提供三种异常钩子，覆盖「兜底 / 包装 / 收尾观察」：

| 方法 | 行为 | 与正常返回值 |
| --- | --- | --- |
| `exceptionally(fn)` | 仅异常时触发，返回兜底值；正常时原样跳过 | 返回**同类型**兜底值，恢复链路 |
| `handle(fn)` | 正常/异常**都会**触发，`BiFunction` 同时拿到结果和异常 | 可同时处理成功与失败，返回新值 |
| `whenComplete(fn)` | 正常/异常都会触发，但**不改变**结果（纯观察） | 返回**原结果**，不能改值 |

```java
CompletableFuture.supplyAsync(() -> riskyCall(), pool)
        .exceptionally(ex -> "fallback")                 // 出错了给默认值，链路继续
        .thenAccept(System.out::println);

CompletableFuture.supplyAsync(() -> riskyCall(), pool)
        .handle((res, ex) -> ex != null ? "err:" + ex.getMessage() : res) // 成功/失败都进
        .thenAccept(System.out::println);

CompletableFuture.supplyAsync(() -> riskyCall(), pool)
        .whenComplete((res, ex) -> log("done, res=%s, ex=%s", res, ex));   // 只观察，不改结果
```

### 4.8 获取结果：阻塞、超时与主动完成

「取结果」这一族方法很容易搞混（**是否阻塞**、**超时后什么行为**、**抛什么异常**），先看表：

| 方法 | 阻塞？ | 超时 / 未完成时行为 | 抛什么异常 | 用途 |
| --- | --- | --- | --- | --- |
| `get()` | ✅ 一直等 | — | `InterruptedException` / `ExecutionException`（**受检**） | 简单场景；不推荐裸用在请求线程 |
| `get(t, unit)` | ✅ 限时等 | 抛 `TimeoutException` | 同上 + `TimeoutException` | 带超时的兜底 |
| `join()` | ✅ 一直等 | — | `CompletionException`（**未受检**） | 流式链式代码里更顺手（不必捕获受检异常） |
| `getNow(v)` | ❌ 不等待 | 立刻返回默认值 | — | 「有就用，没有就算」 |
| `orTimeout(t, unit)`（Java 9+） | ❌ 返回新的 CF | 让 CF 以 `TimeoutException` 完成 | — | **超时失败**语义 |
| `completeOnTimeout(v, t, unit)`（Java 9+） | ❌ 返回新的 CF | 塞入默认值完成 | — | **超时降级**语义（更常用） |
| `complete(v)` / `completeExceptionally(e)` | ❌ 立即返回 | 从外部**主动置为成功 / 失败** | — | 测试桩、把回调式 API 适配成 CF |

```java
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> slowCall(), pool);

cf.get();                       // 阻塞，可抛受检异常，不推荐直接裸用
cf.get(2, TimeUnit.SECONDS);   // 带超时阻塞，超时抛 TimeoutException

cf.join();                      // 与 get() 类似但抛【未受检】异常（CompletionException），流式里更顺手
cf.getNow("default");           // 若已完成立刻返回值，否则返回"默认值"（不阻塞、不等待）

// Java 9+：超时自动降级 / 抛异常
cf.orTimeout(2, TimeUnit.SECONDS);                  // 超时就以 TimeoutException 完成
cf.completeOnTimeout("default", 2, TimeUnit.SECONDS); // 超时则塞入默认值完成

// 从外部主动"完成"这个 Future（测试桩 / 主动设值 / 手动取消）
cf.complete("manual value");                 // 置为成功
cf.completeExceptionally(new RuntimeException("boom")); // 置为失败
```

### 4.9 实战：并行调用多个接口 + 汇总 + 超时降级

典型场景：一个订单详情页需要并发拉取「用户信息、商品信息、优惠券」，全部拿到后组装返回，并带超时兜底。

```java
ExecutorService bizPool = new ThreadPoolExecutor(8, 16, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(200),
        new ThreadFactoryBuilder().setNameFormat("biz-async-%d").build(),
        new ThreadPoolExecutor.CallerRunsPolicy());

public CompletableFuture<OrderDetail> buildOrderDetail(Long orderId) {
    CompletableFuture<User>    userCf  = CompletableFuture
            .supplyAsync(() -> userService.get(orderId), bizPool)
            .orTimeout(500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> User.EMPTY);

    CompletableFuture<Product> prodCf  = CompletableFuture
            .supplyAsync(() -> productService.get(orderId), bizPool)
            .orTimeout(500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> Product.EMPTY);

    CompletableFuture<Coupon>  couponCf = CompletableFuture
            .supplyAsync(() -> couponService.get(orderId), bizPool)
            .orTimeout(500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> Coupon.EMPTY);

    // 三者并行，全部完成后组装
    return CompletableFuture.allOf(userCf, prodCf, couponCf)
            .thenApply(v -> new OrderDetail(
                    userCf.join(), prodCf.join(), couponCf.join()));
}
```

要点：每个子任务**独立设超时 + 降级值**，避免一个慢接口拖垮整个详情页；`allOf().thenApply(join)` 是「等齐再汇总」的标准范式。

### 4.10 常见坑

| # | 坑 | 后果 | 正确做法 |
| --- | --- | --- | --- |
| 1 | 忘了传自定义线程池 | 落入 `ForkJoinPool.commonPool()` 共享池，互相拖累 | 所有 `supplyAsync` / `runAsync` / `xxxAsync` 都**显式传池** |
| 2 | 吞异常 | 异常被「冻结」在 Future 里，直到有人 `get()` / `join()` 才暴露 | 链路末尾一定接 `exceptionally` / `handle`；`@Async` 返回 `CompletableFuture` 时由调用方兜底 |
| 3 | `allOf` 后直接 `get()` 单个子 Future | 可能拿到还没完成的结果 | 用 `allOf(...).thenApply(v -> ...join()...)`，**先等齐再 join** |
| 4 | 用 `thenRun` / `thenAccept` 收尾 | 拿不到最终值（它们不返回结果） | 末尾要值就用 `thenApply`，或直接 `join()` |
| 5 | 忘了线程池要关闭 | 应用无法优雅退出 / 线程泄漏 | `CompletableFuture` 只是编排层，真正跑任务的是你传进去的池 → 按 §3.10 关闭 |
| 6 | 在 HTTP 请求线程里裸用 `get()` / `join()` | 又变回阻塞，异步白做了 | 要么链式到底，要么在 Controller 层统一 `join`（**务必带超时**） |

> 小结：`CompletableFuture` = `Future`（取结果）+ `CompletionStage`（编排）。核心范式是 **`supplyAsync(pool)` 起飞 → 用 `thenApply/thenCompose/thenCombine/allOf` 编排 → 用 `exceptionally/handle` 兜底 → 最后 `join`（带超时）收口**。用它替代手写线程 + 共享变量的异步拼装，代码可读性和健壮性都会上一个台阶。

---

## 五、Executors 与 Spring Boot 线程池对比

这一节把「JDK 原生线程池」和「Spring Boot 线程池」两条线彻底讲清，并说明各种 `Executor` 概念的关系。

### 5.1 Executor 家族概念辨析

Java 并发包里有一串带「Executor」名字的接口 / 类，经常搞混。先看**继承 / 实现关系图**：

```
   继承链（自顶向下读，连接线指向父级）：

   «interface» Executor            仅 void execute(Runnable)
        △ extends                  —— 把「任务提交」与「任务执行」解耦
   «interface» ExecutorService     + submit / invokeAll / invokeAny
        △ implements（抽象类）       + shutdown / shutdownNow / awaitTermination
   AbstractExecutorService         用 FutureTask 统一实现 submit / invokeAll
        △ extends
   ThreadPoolExecutor «class»      ← 全文主角，7 个参数都在它身上
        △ extends                    （可自定义子类：重写 beforeExecute /
   ScheduledThreadPoolExecutor       afterExecute / terminated 做埋点）
        + implements ScheduledExecutorService（定时 / 周期）

   旁支 1（接口分支）  ExecutorService ──extends──▶ ScheduledExecutorService
   旁支 2（独立实现）  ForkJoinPool «class»  implements ExecutorService
                      （Executors.newWorkStealingPool 的底座）
   旁支 3（工厂，不在继承链上）
                      Executors ──静态工厂 new──▶ 各种预置参数的 ThreadPoolExecutor
```

| 类型 | 层级 | 相对上一层新增的能力 |
| --- | --- | --- |
| `Executor` | 顶层接口 | 仅 `void execute(Runnable command)`：把**任务提交**与**任务执行**解耦 |
| `ExecutorService` | 接口，`extends Executor` | 任务管理：`submit()` 返回 `Future`、`invokeAll` / `invokeAny`、`shutdown()` / `shutdownNow()` / `awaitTermination()` |
| `AbstractExecutorService` | 抽象类，`implements ExecutorService` | 把 `submit` / `invokeAll` 用 `FutureTask` 实现好，子类只需实现 `execute()` |
| `ThreadPoolExecutor` | 具体类，`extends AbstractExecutorService` | 真正可用的线程池：7 个参数 + `beforeExecute` / `afterExecute` / `terminated` 钩子 |
| `ScheduledExecutorService` | 接口，`extends ExecutorService` | 定时 / 周期能力：`schedule` / `scheduleAtFixedRate` / `scheduleWithFixedDelay` |
| `ScheduledThreadPoolExecutor` | 具体类，`extends ThreadPoolExecutor` + `implements ScheduledExecutorService` | 同时具备线程池与定时能力 |
| `ForkJoinPool` | 具体类，`extends AbstractExecutorService` | 工作窃取（work-stealing），适合可分解的并行计算 |
| `Executors` | 工具类（**不在继承链上**） | 静态工厂：一行 `new` 出预置配置的线程池（生产禁用，见 §5.2） |

> **一句话**：`Executor` 是「会跑任务的东西」，`ExecutorService` 是「会跑任务且能管任务的池子」，`ThreadPoolExecutor` 是它最常用、最可配的具体实现；`Executors` 只是帮你 `new` 的工厂，本身不在继承链上。

### 5.2 Executors 工厂方法全景

`java.util.concurrent.Executors` 是工具类，用一行代码帮我们 `new` 出各种 `ThreadPoolExecutor`。完整清单：

| 工厂方法 | 等效核心参数 | 队列 | 典型场景 | 风险 |
| --- | --- | --- | --- | --- |
| `newFixedThreadPool(n)` | core=max=n，keepAlive=0 | `LinkedBlockingQueue`（无界） | 长期、CPU 密集任务 | 无界队列 → OOM |
| `newSingleThreadExecutor()` | core=max=1 | `LinkedBlockingQueue`（无界） | 串行、顺序执行 | 无界队列 → OOM |
| `newCachedThreadPool()` | core=0，max=`Integer.MAX_VALUE`，keepAlive=60s | `SynchronousQueue` | 大量短期小任务 | 线程数爆炸，耗尽资源 |
| `newScheduledThreadPool(n)` | core=n，max=`Integer.MAX_VALUE` | `DelayedWorkQueue` | 定时 / 周期任务 | max 极大 |
| `newWorkStealingPool()`（Java 8+） | 基于 `ForkJoinPool`，并行度=CPU 核数 | 工作窃取队列 | 可分解的并行计算（分治） | 不适合 IO 密集长任务 |

源码回顾（以 Fixed 为例）：

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>()); // 无界！上限 Integer.MAX_VALUE
}
```

> ⚠️ **阿里 Java 开发手册明确禁止直接用 `Executors` 创建线程池**，原因正是上表的「无界队列」与「max 极大」两个坑——任务堆积或并发突增时，极易 OOM 或把机器拖死。正确做法是**手动 `new ThreadPoolExecutor(...)`，显式给定有界队列 + 拒绝策略**。

### 5.3 Spring Boot 的线程池体系

Spring 没有「另造一个线程池实现」，而是在 JDK `Executor` 之上做了一层**面向框架的抽象**，以便和 `@Async`、事件、调度等机制整合。

核心接口与实现（Spring 侧，位于 `org.springframework.core.task` 与 `org.springframework.scheduling`）：

| Spring 类型 | 对应的 JDK 概念 | 新增的能力 / 说明 |
| --- | --- | --- |
| `TaskExecutor` | `Executor` | Spring 版镜像，只有 `execute(Runnable)`。Spring 内部所有「要异步执行」的地方都面向它编程，便于替换实现 |
| `AsyncTaskExecutor` | `ExecutorService`（部分） | 增加 `submit()`（返回 `Future`） |
| `AsyncListenableTaskExecutor` | 无直接对应 | 支持 `ListenableFuture`——Spring 版「回调」（完成时触发 `ListenableFutureCallback`），比 `get()` 阻塞更优雅 |
| `SchedulingTaskExecutor` | 无直接对应 | 标记接口，表示该执行器适合短任务调度 |
| **`ThreadPoolTaskExecutor`** | **`ThreadPoolExecutor`** | **最常用具体类**：内部**委托**一个 `ThreadPoolExecutor`，但配置是 Spring 风格（setter / JavaConfig），并接入 Spring 生命周期 |
| `TaskScheduler` / `ThreadPoolTaskScheduler` | `ScheduledThreadPoolExecutor` | Spring 版调度器，配合 `@Scheduled` 使用 |
| `SimpleAsyncTaskExecutor` | 无 | **不池化**！每次 new 一个线程，切勿用于生产（见 §5.4 陷阱） |
| `ConcurrentTaskExecutor` | 适配器 | 把任意原生 `Executor` 包装成 Spring 的 `TaskExecutor` |

Spring 侧的继承 / 实现关系：

```
   Spring 抽象（继承链）                      JDK 对应
   ────────────────────                      ─────────
   TaskExecutor «interface»      ≡ 镜像 ──▶   Executor
        △ extends
   AsyncTaskExecutor             + submit() 返回 Future
        △ extends
        ├──────────────────────────────┐
        ▽                              ▽
   AsyncListenableTaskExecutor    SchedulingTaskExecutor «interface»
   + ListenableFuture 回调         （标记：适合短任务调度）
        └──────────────┬───────────────┘
                       ▽  implements 上面两个接口
   ThreadPoolTaskExecutor «class»  ──⇢ 内部委托持有 ──▶  ThreadPoolExecutor
        └─ extends ExecutorConfigurationSupport
             └─ extends CustomizableThreadFactory   implements ThreadFactory
                （接入 Spring 生命周期：InitializingBean → initialize() 建池；
                  DisposableBean → 容器关闭时自动 shutdown 池）

   TaskScheduler «interface» ──△ implements── ThreadPoolTaskScheduler ⇢ ScheduledThreadPoolExecutor
```

> 关键认知：**`ThreadPoolTaskExecutor` 本质就是 `ThreadPoolExecutor`，只是换了个 Spring 友好的壳**。所以「`Executors` 的坑」在 Spring 里同样存在——`queueCapacity` 默认是 `Integer.MAX_VALUE`（无界），一样会 OOM。

### 5.4 @Async 异步注解全解

Spring Boot 做异步最常用的是 `@Async`：

```java
@Configuration
@EnableAsync                                 // 开启异步代理
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {        // 方法名/返回类型决定默认异步执行器
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);     // 有界队列，防 OOM
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();               // 必须调用，否则报未初始化
        return executor;
    }
}

@Service
public class OrderService {
    @Async                                   // 异步执行，默认用上面的 taskExecutor
    public CompletableFuture<String> createOrder(Long id) {
        // 耗时逻辑……
        return CompletableFuture.completedFuture("ok");
    }
}
```

| 事项 | 说明 | 正确做法 |
| --- | --- | --- |
| 开启方式 | 需要 `@EnableAsync`；`@Async` 只能标在 **public** 方法上 | 配置类加 `@EnableAsync`，被标注的方法必须是 public |
| **自调用失效** | 同一个类里 A 方法调 `this.b()`，不走代理，`@Async` 不生效 | 必须**跨 bean 调用**（把异步方法拆到另一个 Service），或注入自身代理 |
| 返回值 | 支持 `void` / `Future` / `CompletableFuture` / `ListenableFuture` | 返回普通对象会被**忽略**（拿到 `null`）！要结果就用 `CompletableFuture<T>` |
| 异常处理 | `void` 型异步方法抛的异常默认被 `SimpleAsyncUncaughtExceptionHandler` **吃掉**（只打日志） | 实现 `AsyncConfigurer.getAsyncUncaughtExceptionHandler()`（见 §5.6）；或直接返回 `CompletableFuture` 让调用方兜底 |
| **默认执行器陷阱** | 没定义任何 `Executor` Bean 时，Spring 会 fallback 到 `SimpleAsyncTaskExecutor`——**每次都 new 线程、根本不池化** | 显式定义 `ThreadPoolTaskExecutor`（或用 `AsyncConfigurer.getAsyncExecutor()`） |
| 多个执行器 | 容器里有多个 `Executor` 时，按「方法名 → 类型 → 名字 `taskExecutor`」的顺序找 | 用 `@Async("bizExecutor")` 显式指定，避免找错池 |

### 5.5 Executors vs Spring Boot 线程池 对比

| 维度 | JDK `Executors` / `ThreadPoolExecutor` | Spring `ThreadPoolTaskExecutor` / `@Async` |
| --- | --- | --- |
| 来源 | `java.util.concurrent` | `org.springframework.scheduling.concurrent` |
| 配置风格 | 构造器 7 参数（一次性传齐） | setter（Spring / 注解友好） |
| 关键参数 | corePoolSize / maximumPoolSize / workQueue | corePoolSize / maxPoolSize / **queueCapacity**（等价 workQueue 容量） |
| 队列默认 | `Executors` 多为无界 | `queueCapacity` 默认 `Integer.MAX_VALUE`（**同样无界，需手动设**） |
| 回调能力 | `Future.get()` 阻塞；`CompletableFuture` 回调 | 额外支持 `ListenableFuture` 回调 |
| 与框架整合 | 无 | 原生支持 `@Async`、`@Scheduled`、`ApplicationEvent` 异步事件 |
| 拒绝策略 | 4 种 `RejectedExecutionHandler` | 同样可设（`setRejectedExecutionHandler`） |
| 适用场景 | 纯 JDK 代码、精确控制池行为 | Spring Boot 业务代码、声明式异步 |

**结论**：二者底层都是 `ThreadPoolExecutor`，区别在「封装形态」与「生态整合」。在 Spring Boot 项目里优先用 `ThreadPoolTaskExecutor` + `@Async`（声明式、可监控、可统一异常），但要**主动把 `queueCapacity` 设成有界值**，否则和 `newFixedThreadPool` 一样踩 OOM。

### 5.6 Spring Boot 生产级配置示例（含监控与异常处理）

```java
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("biz-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true); // 低峰期释放核心线程
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                System.err.println("异步方法 " + method.getName() + " 执行失败: " + ex);
    }
}
```

监控：通过 `ThreadPoolTaskExecutor` 暴露指标（活跃线程数、队列深度等），接 Spring Boot Actuator / Micrometer，再上 Grafana 看板，实时观察池是否打满、是否触发拒绝。

---

## 六、结构化并发扩展：ThreadForge 与 StructuredTaskScope

> **扩展阅读**。来源：[Java多线程神器——ThreadForge，让多线程从此简单](https://juejin.cn/post/7604779604126138368)（一只叫煤球的猫）。以下先还原原文要点，再由本文补充「与 JDK 官方 `StructuredTaskScope` 的对照」和「生产落地评估」。

### 6.1 起点：一个「并发调三个接口」为什么写了 50 行

场景：用户详情页串行调三个接口（用户信息 / 订单列表 / 积分余额），每个 200ms，合计 600ms。改成并发后，代码却越写越长：

| 你最初以为要写的 | 实际还得处理的 |
| --- | --- |
| 建线程池 + `submit` 三个任务 | 线程池参数怎么配？谁来 `shutdown()`？ |
| `future.get()` 取结果 | 超时怎么办？`get(500, MILLISECONDS)` 之后任务还在跑吗？ |
| — | 一个失败，另外两个要不要取消？ |
| — | 异常怎么传播？吞掉还是手动包装？ |
| — | 每个任务跑了多久？埋点写在哪儿？ |

传统的 `ExecutorService` / `Future` / `CompletableFuture` 确实强大，但也确实啰嗦：

- 线程池要手动创建和关闭；
- 超时逻辑每个任务都要写一遍；
- 失败了要不要取消其他任务，得自己判断；
- 异常要么吞掉，要么手动包装；
- 想知道任务跑多久，自己打日志。

**根因**：`ExecutorService` 属于**非结构化并发（unstructured concurrency）**——**任务一旦提交，它的生命周期就不再受提交它的方法约束**。池子里的任务可以活得比提交它的方法更久，「谁负责、何时结束、失败怎么办」于是全部外溢成调用方的负担，下次遇到类似场景还得把这些边界条件再想一遍。

### 6.2 什么是「结构化并发」

一句话：**并发任务的生命周期被限定在一个词法作用域（scope）内，作用域退出时，其中所有任务必然已经结束（成功、失败或被取消）。**

| 维度 | 非结构化（`ExecutorService`） | 结构化（`ThreadScope` / `StructuredTaskScope`） |
| --- | --- | --- |
| 任务生命周期 | 由线程池持有，可超出提交方 | 绑定在 `try-with-resources` 作用域内 |
| 代码结构 vs 并发结构 | 不一致（取决于线程池配置） | **一致**，读代码即知并发关系 |
| 失败传播 | 自己判断 | 策略化、统一 |
| 超时 | 每个任务各写一遍 | 作用域级 deadline，统一生效 |
| 取消 | 手动 `cancel` | 作用域关闭时自动取消未完成任务 |
| 观测 | 业务各自埋点 | Hook 统一收口 |

原文用一句话概括 ThreadForge 的设计哲学：**先降低认知成本，再追求性能**——可以把它理解为「对 Java 内置并发工具的二次包装」，目标是让 Java 并发更简单、更清晰。

最小示例（注意所有任务都绑定在 `scope` 上，且默认就带超时、失败传播、自动取消）：

```java
try (ThreadScope scope = ThreadScope.open()) {
    Task<String> user = scope.submit("load-user", () -> fetchUser());
    Task<Integer> orders = scope.submit("load-orders", () -> fetchOrders());

    scope.await(user, orders);   // 到这里，两个任务必定都已结束（成功 / 失败 / 超时）

    String result = user.await() + ":" + orders.await();
}
// scope 关闭时：所有任务自动取消、资源自动清理
```

对照传统写法要额外做的事：建线程池并配参数 → `submit` + 手动管 `Future` → `try-finally` 保证 `shutdown` → 手动处理超时与异常传播。

### 6.3 五个「省脑力」的设计

#### 1）默认行为就是正确的（`FAIL_FAST` + 默认超时 + 自动取消）

```java
// 默认：FAIL_FAST + 30 秒超时 + 自动取消其他任务
try (ThreadScope scope = ThreadScope.open()) {
    Task<Integer> a = scope.submit(() -> riskyRpc());
    Task<Integer> b = scope.submit(() -> anotherRpc());
    scope.await(a, b);
} catch (ScopeTimeoutException timeout) {
    fallback();                  // 超时了，所有任务已被自动取消
} catch (FailurePropagationException failed) {
    handleError(failed);         // 某个任务失败，其他任务已被自动取消
}
```

不需要额外配置，开箱即用。

#### 2）失败策略明确且统一（5 种）

| 策略 | 语义 | 典型场景 |
| --- | --- | --- |
| `FAIL_FAST` | 快速失败，立即取消其他任务（**默认**） | 强依赖，任一失败则整体无意义 |
| `COLLECT_ALL` | 等所有任务结束，汇总所有失败 | 需要完整失败清单 |
| `SUPERVISOR` | 不自动取消，失败信息收集到 `Outcome` | 批量导入：部分失败也要知道哪些成功 |
| `CANCEL_OTHERS` | 失败后取消其余任务，但不抛异常 | 尽力而为 |
| `IGNORE_ALL` | 忽略失败，只返回成功的结果 | 容错聚合 |

```java
// 场景：批量导入，即使部分失败也要知道哪些成功了
try (ThreadScope scope = ThreadScope.open()
        .withFailurePolicy(FailurePolicy.SUPERVISOR)) {

    List<Task<Void>> tasks = ids.stream()
            .map(id -> scope.submit(() -> importData(id)))
            .collect(toList());

    Outcome outcome = scope.await(tasks);
    log.info("成功: {}, 失败: {}", outcome.successCount(), outcome.failureCount());
}
```

#### 3）并发度控制不再需要手写信号量

```java
// 场景：调用外部 API，最多同时 50 个请求
try (ThreadScope scope = ThreadScope.open().withConcurrencyLimit(50)) {
    List<Task<Result>> tasks = hugeIdList.stream()
            .map(id -> scope.submit(() -> externalApi.call(id)))
            .collect(toList());

    List<Result> results = scope.awaitAll(tasks);
}
// 自动限流，不会把外部服务打爆
```

不用自己写 `Semaphore`、不用手动分批。

#### 4）生命周期观测统一收口

```java
ThreadScope scope = ThreadScope.open()
        .withHook(new ThreadHook() {
            @Override public void onStart(TaskInfo info) {
                metrics.taskStarted(info.name());
            }

            @Override public void onSuccess(TaskInfo info, Duration duration) {
                metrics.taskSuccess(info.name(), duration.toMillis());
            }

            @Override public void onFailure(TaskInfo info, Throwable error, Duration duration) {
                log.error("Task {} failed after {}", info.name(), duration, error);
                metrics.taskFailed(info.name());
            }
        });
```

**一处埋点，全局生效**——不必在每个任务里重复写日志和监控代码。

#### 5）跨 JDK 版本的一致体验

```java
// 同一套 API
try (ThreadScope scope = ThreadScope.open()) {
    // JDK 21+：自动使用虚拟线程；JDK 8 ~ 20：自动降级到线程池
    Task<String> task = scope.submit(() -> longRunningTask());
    return task.await();
}
```

不用分叉代码、不用写 `if-else`，框架自动适配。这一点对**仍在 Java 8 / 11 上、又想提前用上结构化并发心智模型**的团队比较有吸引力。

### 6.4 三个典型适用场景

**① 并发 RPC 聚合**

```java
try (ThreadScope scope = ThreadScope.open()) {
    Task<User> user = scope.submit(() -> userService.get(uid));
    Task<List<Order>> orders = scope.submit(() -> orderService.list(uid));
    Task<Profile> profile = scope.submit(() -> profileService.get(uid));

    scope.await(user, orders, profile);

    return buildResponse(user.await(), orders.await(), profile.await());
}
```

**② 批量数据处理（限流 + 全局 deadline）**

```java
try (ThreadScope scope = ThreadScope.open()
        .withConcurrencyLimit(100)
        .withDeadline(Duration.ofMinutes(5))) {

    List<Task<Void>> tasks = records.stream()
            .map(r -> scope.submit(() -> process(r)))
            .collect(toList());

    scope.awaitAll(tasks);
}
```

**③ 生产者-消费者（内置有界通道）**

```java
try (ThreadScope scope = ThreadScope.open()) {
    Channel<Data> channel = Channel.bounded(1000);

    scope.submit(() -> {
        for (Data d : datasource) {
            channel.send(d);
        }
        channel.close();
        return null;
    });

    List<Task<Void>> consumers = IntStream.range(0, 4)
            .mapToObj(i -> scope.submit(() -> {
                for (Data d : channel) {
                    process(d);
                }
                return null;
            }))
            .collect(toList());

    scope.awaitAll(consumers);
}
```

### 6.5 快速开始与能力一览

坐标 `pub.lighting:threadforge-core`（MIT 协议，已发布至 Maven Central）。原文示例用的是 `1.0.1`，当前最新为 **`1.2.0`**：

```xml
<dependency>
    <groupId>pub.lighting</groupId>
    <artifactId>threadforge-core</artifactId>
    <version>1.2.0</version>
</dependency>
```

```groovy
// Gradle
implementation("pub.lighting:threadforge-core:1.2.0")
```

```java
// 最小示例
try (ThreadScope scope = ThreadScope.open()) {
    Task<String> task = scope.submit(() -> "Hello, ThreadForge");
    System.out.println(task.await());
}
```

`1.2.0` 的能力矩阵：

| 能力 | 说明 |
| --- | --- |
| 结构化作用域 / 任务句柄 | `ThreadScope` + `Task` |
| 失败与重试 | `FailurePolicy`、`RetryPolicy` |
| 取消与优先级 | `CancellationToken`（协作式取消）、`TaskPriority` |
| 上下文传播 | `Context` 在提交 / 调度时自动捕获并传播 |
| 流式通信 | 有界 `Channel`；`Scheduler` 调度策略 |
| 定时能力 | `DelayScheduler` + `ScheduledTask` |
| 组合式编排 | `Task.thenApply` / `thenCompose` / `exceptionally` |
| 高阶编排 | `JoinStrategy` + `ScopeJoiner`（`firstSuccess` / `quorum` / `hedged`） |
| 可观测 | `ThreadHook` + `TaskInfo`、`ScopeMetricsSnapshot`、OpenTelemetry 集成 |

> 注：`firstSuccess`（最快成功即返回）、`quorum`（凑够 N 个成功即返回）、`hedged`（对冲请求，慢的就再发一份）这类模式，正是 `CompletableFuture` 写起来最别扭的部分，值得关注。

### 6.6 补充对照：与 JDK 官方 `StructuredTaskScope`

ThreadForge 的思想并不孤立——**JDK 官方正在做同一件事**，叫 Structured Concurrency（结构化并发），API 是 `java.util.concurrent.StructuredTaskScope`。

| 维度 | ThreadForge | JDK `StructuredTaskScope` |
| --- | --- | --- |
| 出身 | 第三方开源库（MIT，社区维护） | JDK 官方（JEP 流程） |
| 可用版本 | **JDK 8+**（旧版本自动降级到线程池） | JDK 19 孵化 → JDK 21 起 Preview，**至今仍未转正** |
| 开启作用域 | `ThreadScope.open()` | `StructuredTaskScope.open()`（JDK 25 起用静态工厂替代构造器） |
| 提交任务 | `scope.submit(...)` → `Task<T>` | `scope.fork(...)` → `Subtask<T>` |
| 等待与失败 | `scope.await(...)` / `awaitAll(...)` + `FailurePolicy` | `scope.join()` + `throwIfFailed()`，或自定义 `Joiner` |
| 并发度限制 | `withConcurrencyLimit(n)` 内置 | 无直接对应，需自行控制 |
| 有界通道 | `Channel.bounded(n)` 内置 | **明确不在范围内**（JEP 非目标声明不做 channel） |
| 观测 | `ThreadHook` + 指标快照 + OpenTelemetry | 靠结构化带来的栈信息，埋点自建 |
| 虚拟线程 | JDK 21+ 自动启用 | 原生基于虚拟线程 |
| 生态整合 | 截至 `1.2.0` **尚无 Spring Boot Starter / Actuator endpoint** | 无（属于 JDK 层能力） |

时间线（结构化并发 JEP 演进）：

```
JDK 19/20  JEP 428 / 437  孵化（Incubator）
JDK 21     JEP 453        首个 Preview，fork() 改为返回 Subtask
JDK 22-24  JEP 462 / 480 / 499   持续 re-preview
JDK 25     JEP 505        re-preview，构造器改为静态工厂 open()
JDK 26/27  JEP 525 / 533  继续 re-preview
JDK 28     JEP 543        计划正式转正（Finalize）
```

官方写法对照（JDK 25 预览 API）：

```java
// 注意：截至 JDK 27 仍为预览特性，需 --enable-preview 才能编译运行
try (var scope = StructuredTaskScope.open()) {
    var user   = scope.fork(() -> findUser(uid));
    var orders = scope.fork(() -> fetchOrders(uid));

    scope.join();                       // 等全部结束（任一失败则取消其余）
    return new Response(user.get(), orders.get());
}
```

**结论**：

- 在 **JDK 21+**、且能接受 preview API 变动 → 直接用官方 `StructuredTaskScope`，这是长期方向；
- 在 **JDK 8 / 11 / 17**，或希望 API 稳定、功能更「开箱」（限流、`Channel`、Hook、重试、高阶编排）→ ThreadForge 是当前可用的折中方案；
- 两者不冲突：可以先用 ThreadForge 建立结构化并发的心智模型，将来平滑迁移到官方 API。

### 6.7 落地评估：什么时候用，什么时候先别用

| 判断 | 信号 | 建议 |
| --- | --- | --- |
| ✅ 值得试 | 大量「并发调 N 个接口再聚合」的代码，超时 / 取消 / 异常处理每次重写 | JDK 21+ 先上官方 `StructuredTaskScope`；老 JDK 考虑 ThreadForge |
| ✅ 值得试 | 团队被 `CompletableFuture` 的长链和异常传递折磨 | 用结构化作用域把「并发关系」显式化 |
| ✅ 值得试 | 需要**统一**的并发观测（一处 hook 全局生效） | ThreadForge 的 `ThreadHook` 直接可用 |
| ⚠️ 先观望 | **核心交易链路** | 库较年轻（1.x）、社区维护 → 上线前务必压测 + 故障演练 |
| ⚠️ 先观望 | 需要 Spring Boot 自动配置 / Actuator 端点 | 截至 `1.2.0` 尚未提供，得自己包一层 |
| ⚠️ 先观望 | 团队缺乏并发排障能力 | 框架降低的是「书写成本」，**不能替代对线程池、虚拟线程、取消语义的理解**（§三～§五 仍是基本功） |

**迁移成本提醒**：引入新库意味着多一个依赖与学习成本。如果只是零星几处并发聚合，`CompletableFuture` + 一个配置好的线程池可能已经够用。**并发工具的选择标准不是「新」，而是「团队能推理、能观测、能排障」。**

> 原文结尾：*ThreadForge 的目标不是取代所有并发工具，而是让 80% 的常见场景变得简单、安全、可维护。让并发回归简单，让代码重新可读。*

---

## 七、生产实践 checklist

- [ ] 不复用 `Executors` 快捷方法，手动 `new ThreadPoolExecutor` 并明确 7 个参数。
- [ ] 使用**有界队列**（Spring 里改 `queueCapacity`），配合 `CallerRunsPolicy` 等合理拒绝策略防 OOM。
- [ ] 用 `ThreadFactory` / `setThreadNamePrefix` 给线程起名，便于日志排查。
- [ ] 区分 CPU / IO 密集，合理设置 `corePoolSize` / `maximumPoolSize`，并压测收敛。
- [ ] 任务内做好异常捕获（try/catch 或 `Future.get()` / `afterExecute`）；`@Async` 用 `AsyncConfigurer` 统一处理未捕获异常。
- [ ] 应用关闭时调用 `shutdown()` + `awaitTermination` 优雅退出。
- [ ] 跨业务使用线程池隔离；异步编排优先用 `CompletableFuture` 并指定自定义池。
- [ ] Spring Boot 中显式配置 `ThreadPoolTaskExecutor`（避免 fallback 到不池化的 `SimpleAsyncTaskExecutor`），并设**有界** `queueCapacity`。
- [ ] 并发聚合场景（N 个接口汇总）考虑**结构化并发**：JDK 21+ 优先用官方 `StructuredTaskScope`；JDK 较老或需要内置限流 / `Channel` / 统一观测时再评估 ThreadForge 等第三方库（引入前评估维护活跃度与故障演练）。

---

## 参考资料

- [面试必备：Java线程池解析](https://juejin.cn/post/6844903889678893063) — 捡田螺的小男孩
- [如何优雅的使用和理解线程池](https://juejin.cn/post/6844903648405766158) — crossoverJie
- [Java—线程池 ThreadPoolExecutor 详解](https://juejin.cn/post/6844904146856837128) — Andya
- [Java多线程神器——ThreadForge，让多线程从此简单](https://juejin.cn/post/7604779604126138368) — 一只叫煤球的猫（扩展阅读：结构化并发）
