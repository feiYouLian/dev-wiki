---
title: Java 异步线程与线程池详解
---

# Java 异步线程与线程池详解

> 本文综合三篇掘金文章的要点（见文末「参考资料」），系统梳理 **Java 异步线程** 与 **线程池（ThreadPoolExecutor）** 的核心知识，并补充了 `CompletableFuture` 等现代异步写法，便于在项目与面试中直接使用。

## 一、三篇文章内容总结

| 文章 | 作者 | 核心视角 |
| --- | --- | --- |
| [面试必备：Java线程池解析](https://juejin.cn/post/6844903889678893063) | 捡田螺的小男孩 | 以经典面试题切入，讲清线程池参数、执行流程、拒绝策略、异常处理、工作队列、常用线程池、线程池状态 |
| [如何优雅的使用和理解线程池](https://juejin.cn/post/6844903648405766158) | crossoverJie | 从池化思想讲起，覆盖 `execute()` 流程、线程数配置、优雅关闭、SpringBoot 集成、监控、线程池隔离（Hystrix） |
| [Java—线程池 ThreadPoolExecutor 详解](https://juejin.cn/post/6844904146856837128) | Andya | 围绕 `ThreadPoolExecutor` 的 7 个参数、工作流程、5 种状态、4 种标准线程池源码、拒绝策略与阿里规范展开 |

**三篇文章的共识：**
- 线程是稀缺资源，**不能频繁创建/销毁**，必须用线程池复用（阿里 Java 手册强制要求）。
- 线程池的本质是「池化技术」：核心线程≈正式员工、非核心线程≈外包、阻塞队列≈需求池、拒绝策略≈拒单。
- 直接用 `Executors` 工厂方法存在隐患（`newFixedThreadPool`/`newSingleThreadExecutor` 用无界队列易 OOM，`newCachedThreadPool` 可能创建过多线程），**生产环境建议手动 `new ThreadPoolExecutor(...)`**。

---

## 二、Java 异步与线程基础

### 2.1 进程 vs 线程

- **进程**：操作系统分配资源的最小单位，彼此隔离。
- **线程**：CPU 调度的最小单位，同进程内线程共享堆内存，通信成本低但需处理并发安全。

### 2.2 为什么需要异步 / 多线程

- **提升吞吐**：将一个耗时的 IO / 计算任务从主线程剥离，避免阻塞主流程（如接口响应）。
- **并行计算**：多核 CPU 下，多个线程可真正并行执行 CPU 密集任务。
- **解耦**：任务的「提交」与「执行」分离，便于统一调度、监控与限流。

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

- **同步**：调用方必须等被调用方返回结果后才继续（阻塞）。
- **异步**：调用方提交任务后立即返回，结果通过回调 / `Future` / 后续阶段获取（非阻塞）。

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
        new Thread()
             │ start()
             ▼
           NEW ─────────▶ RUNNABLE ◀──────────┐
                                                │ 获取到锁 / 被唤醒 / 超时
             │ 竞争 synchronized 锁失败          │
             ▼                                  │
          BLOCKED ─────────▶ RUNNABLE           │
                                                │
             │ wait() / join() / park()         │
             ▼                                  │
        WAITING / TIMED_WAITING ───────────────┘
                                                │
             │ run() 结束 / 异常退出             │
             ▼                                  │
          TERMINATED ◀──────────────────────────┘
```

注意点：
- `RUNNABLE` 在 JVM 层面把 OS 的「就绪」和「运行中」合并为一，不细分。
- `BLOCKED` **只**由 `synchronized` 竞争锁引起；`ReentrantLock` 的等待走的是 `WAITING` / `TIMED_WAITING`（底层用 `LockSupport.park`）。排查死锁 / 卡顿时要分清二者。
- 线程一旦进入 `TERMINATED` 就**不能再 `start()`**，否则抛 `IllegalThreadStateException`。

排查小技巧：`jstack <pid>` 导出的线程栈里，状态字段就是上表中的 `BLOCKED` / `WAITING` / `TIMED_WAITING` 等，可据此定位「哪个线程卡在哪把锁上」。

### 2.6 核心概念辨析：Task / Runnable / Callable / Future / Callback

这些词常被混用，厘清关系有助于理解整个异步模型：

- **Task（任务）**：业务语义上的「要被执行的一段工作」，是统称。在 Java 并发里，任务通常由 `Runnable` 或 `Callable` 来表达。
- **Runnable（可运行任务）**：最基础的「任务单元」。`void run()` **无返回值、不能抛受检异常**。可直接传给 `Thread` 构造器或线程池的 `execute()`。
- **`Callable<V>`（可回调任务）**：`V call() throws Exception` **有返回值、可抛受检异常**。因为 `Thread` 只认 `Runnable`，`Callable` 必须配合 `ExecutorService.submit()` 使用。
- **Future（异步结果句柄 / 提货单）**：提交任务后拿到的「凭证」。通过 `get()` 阻塞取结果、`cancel()` 取消、`isDone()` 查询是否完成。它把「提交任务」和「取结果」两个动作在时间上解耦。
- **FutureTask**：`Runnable` + `Future` 的桥接实现。它既是一个能被线程池执行的任务（`implements Runnable`），又是一个能拿结果的句柄（`implements Future`），内部把 `Callable` 包装起来。`submit(Callable)` 时线程池底层就是包了一层 `FutureTask`。
- **Callback（回调）**：「结果就绪后由框架主动调用我」的编程模式，避免 `Future.get()` 的阻塞等待。典型实现：`CompletableFuture.thenAccept(...)`、`thenApply(...)`，以及 Guava 的 `ListenableFuture.addListener(...)`。

```
提交 Callable / Runnable ──▶ 线程池
                                │
                                ▼
                      内部包装为 FutureTask
                                │
              ┌─────────────────┴─────────────────┐
              ▼                                    ▼
   返回 Future（句柄）给调用方            任务完成时触发 Callback（被动通知）
   调用方用 get() 主动取结果              （CompletableFuture.thenXxx / ListenableFuture）
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

一句话区分：**Runnable / Callable 描述「干什么」，Future 是「干完后的凭证」，Callback 是「干完后通知我」的钩子，而 Task 是它们的总称。**

---

## 三、线程池（ThreadPoolExecutor）详解

### 3.1 为什么用线程池

1. **降低开销**：复用已创建线程，省去频繁创建（类加载）与销毁（GC）的成本。
2. **提升响应速度**：任务到达时直接取空闲线程执行，比临时建线程快得多。
3. **便于管理**：统一命名、监控、限流、优雅关闭，并可做线程池隔离。

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

- **corePoolSize**：常驻核心线程数，默认一直存活（除非 `allowCoreThreadTimeOut(true)`）。
- **maximumPoolSize**：线程总数上限。
- **keepAliveTime / unit**：非核心线程空闲超过该时间即回收。
- **workQueue**：来不及执行的任务在此排队。
- **threadFactory**：创建线程的工厂，建议用 `ThreadFactoryBuilder`（Guava）或自定义，给线程加可读前缀，方便排查问题。
- **handler**：队列与线程都满时的兜底策略（见 3.5）。

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

`submit()` 与 `execute()` 的区别：`submit()` 返回 `Future`，可通过 `Future.get()` 拿到返回值或捕获异常；`execute()` 只提交 `Runnable`，无返回。

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

| 策略 | 行为 |
| --- | --- |
| `AbortPolicy`（默认） | 抛出 `RejectedExecutionException` |
| `DiscardPolicy` | 静默丢弃新任务，不抛异常 |
| `DiscardOldestPolicy` | 丢弃队列中最旧的任务，再尝试提交当前任务 |
| `CallerRunsPolicy` | 由**调用者线程**自己执行该任务，起到「反压 / 平滑降级」作用 |

```java
RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
```

### 3.6 五种线程池状态

```
RUNNING ──shutdown()──▶ SHUTDOWN ──队列空且任务空──▶ TIDYING ──terminated()──▶ TERMINATED
   │                                                          ▲
   └──────────────shutdownNow()──────────────────────────────┘
                         │
                         ▼
                       STOP ──任务全空──▶ TIDYING
```

- **RUNNING**：接收新任务，并处理队列中的任务。
- **SHUTDOWN**：不接收新任务，但会把队列中已有的任务执行完（`shutdown()` 后）。
- **STOP**：不接收新任务，也不处理队列任务，并中断正在执行的任务（`shutdownNow()` 后）。
- **TIDYING**：所有任务已终止，线程数为 0，准备进入终止。
- **TERMINATED**：`terminated()` 执行完毕，线程池彻底停止。

### 3.7 四种常用线程池（及隐患）

```java
// 1) FixedThreadPool：固定线程数，无界队列
ExecutorService fixed = new ThreadPoolExecutor(n, n, 0L, MILLISECONDS,
        new LinkedBlockingQueue<>());
// 特点：core=max=n；适合 CPU 密集型、长期任务；风险：无界队列可能 OOM

// 2) CachedThreadPool：可缓存，来一个任务建一个线程
ExecutorService cached = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
        60L, SECONDS, new SynchronousQueue<>());
// 特点：core=0，max 极大；适合大量短期小任务；风险：并发高时线程数爆炸

// 3) SingleThreadExecutor：单线程串行
ExecutorService single = new ThreadPoolExecutor(1, 1, 0L, MILLISECONDS,
        new LinkedBlockingQueue<>());
// 特点：顺序执行，适合串行场景；同样有 OOM 风险

// 4) ScheduledThreadPool：定时 / 周期任务
ScheduledExecutorService scheduled = new ScheduledThreadPoolExecutor(core, new DelayedWorkQueue());
// 支持 scheduleAtFixedRate / scheduleWithFixedDelay
```

**结论**：阿里规范不推荐直接用 `Executors` 快捷方法，应手动创建并明确队列容量、线程上下限和拒绝策略。

### 3.8 线程池异常处理

任务里抛 `RuntimeException` 时，线程池可能「吞掉」异常，导致无感知。四种处理方案：

1. **任务内 try/catch**：最直接，在 `run()` / `call()` 里捕获。
2. **`Future.get()` 捕获**：`submit()` 提交的任务，通过 `future.get()` 拿到 `ExecutionException`。
3. **`UncaughtExceptionHandler`**：给线程设置未捕获异常处理器。
4. **重写 `afterExecute`**：继承 `ThreadPoolExecutor`，在 `afterExecute(Runnable r, Throwable t)` 中统一处理（JDK 文档示例）。

```java
ExecutorService pool = Executors.newFixedThreadPool(1, r -> {
    Thread t = new Thread(r);
    t.setUncaughtExceptionHandler((t1, e) -> System.out.println(t1.getName() + " 异常: " + e));
    return t;
});
```

### 3.9 线程数配置建议

线程池并非越大越好，按任务性质区分：

- **CPU 密集型**（大量计算）：线程数 ≈ CPU 核心数（`Runtime.getRuntime().availableProcessors()`），避免过多上下文切换。
- **IO 密集型**（网络 / 数据库 / 磁盘）：线程数可放大，常见经验值 `CPU 核心数 * 2`，甚至更多，因为线程常在等待。
- 最佳值需结合压测逐步收敛。

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

- `shutdown()`：平滑，等队列清空。
- `shutdownNow()`：激进，立即中断并丢弃队列。

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

### 4.2 核心概念：Future vs CompletionStage

- **Future（结果句柄）**：`CompletableFuture` 实现了 `Future`，所以它**依然能用 `get()` 取结果、`cancel()` 取消**。这是向下兼容。
- **CompletionStage（完成阶段）**：这是 `CompletableFuture` 的核心能力来源。一个 `CompletionStage` 代表「异步计算的某一个阶段」，它允许你**注册当本阶段完成时该做什么**（转换、消费、组合、异常处理）。所有 `thenXxx` / `xxxAsync` 方法都来自这个接口。
- **CompletableFuture（可手动完成的 Future）**：`Completable` 意味着「可由外部主动使其完成」——你既能让任务自己跑完，也能通过 `complete()` / `completeExceptionally()` 从外部塞入结果或异常（常用于测试桩、超时兜底）。

一句话：**`Future` 给你「拿结果」的能力，`CompletionStage` 给你「编排下一步」的能力，二者在 `CompletableFuture` 里合体。**

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

`CompletableFuture` 里**几乎所有 `thenXxx` 都有两个版本**：

- **`thenApply(...)`（无 Async）**：默认在「完成上一阶段的那个线程」上继续执行（如果上一阶段已同步完成，就在调用线程执行）。
- **`thenApplyAsync(...)`（带 Async）**：**总是把任务提交到你指定的线程池（或 `commonPool`）去执行**，与上一阶段的线程解耦。

```java
CompletableFuture.supplyAsync(() -> fetchData(), pool)
        .thenApplyAsync(data -> heavyTransform(data), pool)  // 重的转换丢到 pool 跑
        .thenAccept(result -> System.out.println(result));
```

经验法则：**CPU 重的后续步骤用 `xxxAsync(pool)` 指定池执行，避免占用回调线程（可能是 IO 线程）**；轻量的纯内存转换用无 Async 版本即可。

### 4.6 任务组合（CompletableFuture 的真正强项）

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

| 方法 | 语义 | 返回 |
| --- | --- | --- |
| `allOf(cf1, cf2, ...)` | 等待**全部**完成（一失败即失败） | `CompletableFuture<Void>` |
| `anyOf(cf1, cf2, ...)` | 等待**任意一个**先完成 | `CompletableFuture<Object>` |

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

除了阻塞的 `get()`，还有更安全的做法：

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

1. **忘了传自定义线程池** → 落入 `ForkJoinPool.commonPool()` 共享池，互相拖累。
2. **吞异常**：`thenApply` 链路里抛异常若没有 `exceptionally` / `handle` 兜底，异常会被「冻结」在 Future 里，直到有人 `get()` / `join()` 才暴露；`@Async` 返回 `CompletableFuture` 时异常由调用方处理，要配套兜底。
3. **`allOf` 后直接 `get()` 单个子 Future 却没先 `join` 全部** → 用 `allOf().thenApply(v -> ...join...)` 才是安全顺序。
4. **`thenRun` / `thenAccept` 不返回结果** → 想在末尾拿到最终值要用 `thenApply` 或 `join()`。
5. **线程池不复用 / 不关闭** → `CompletableFuture` 只是编排层，真正跑任务的是你传进去的 `ExecutorService`，记得按 3.10 优雅关闭。
6. **`get()` / `join()` 在 HTTP 请求线程里裸用** → 又变回阻塞，违背异步初衷；要么链式到底，要么在 Controller 层统一 `join`（配合超时）。

> 小结：`CompletableFuture` = `Future`（取结果）+ `CompletionStage`（编排）。核心范式是 **`supplyAsync(pool)` 起飞 → 用 `thenApply/thenCompose/thenCombine/allOf` 编排 → 用 `exceptionally/handle` 兜底 → 最后 `join`（带超时）收口**。用它替代手写线程 + 共享变量的异步拼装，代码可读性和健壮性都会上一个台阶。

---

## 五、Executors 与 Spring Boot 线程池对比

这一节把「JDK 原生线程池」和「Spring Boot 线程池」两条线彻底讲清，并说明各种 `Executor` 概念的关系。

### 5.1 Executor 家族概念辨析

Java 并发包里有一串带「Executor」名字的接口/类，经常搞混。从抽象层级自顶向下看：

- **Executor（JDK 顶层接口）**：只定义一个方法 `void execute(Runnable command)`。它是所有线程池的「根」，职责是把**任务的提交**与**任务的执行**解耦。
- **ExecutorService（接口，extends Executor）**：在 `Executor` 之上扩展出任务管理：`submit()`（返回 `Future`）、`invokeAll` / `invokeAny`、`shutdown()` / `shutdownNow()` / `awaitTermination()` 等生命周期方法。
- **AbstractExecutorService（抽象类）**：把 `submit` / `invokeAll` 等用 `FutureTask` 统一实现好，子类只需关心 `execute()`。
- **ThreadPoolExecutor（具体类）**：JDK 标准线程池实现，就是全文重点讲的那个，7 个参数都在它身上。
- **ScheduledExecutorService（接口）/ ScheduledThreadPoolExecutor（类）**：带「定时 / 周期」能力的线程池，提供 `schedule()` / `scheduleAtFixedRate()` 等。

```
Executor（仅 execute）
   ▲ extends
ExecutorService（submit / shutdown）
   ▲ extends
AbstractExecutorService ──▶ ThreadPoolExecutor
                                  ▲ extends
                       ScheduledThreadPoolExecutor
                       （implements ScheduledExecutorService）
```

一句话：**Executor 是「会跑任务的东西」，ExecutorService 是「会跑任务且能管任务的池子」，ThreadPoolExecutor 是它最常用、最可配的具体实现。**

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

核心接口（Spring 侧，位于 `org.springframework.core.task` 与 `org.springframework.scheduling`）：

- **TaskExecutor（Spring 接口）**：Spring 对 JDK `Executor` 的镜像，只有一个 `execute(Runnable)`。Spring 内部所有「要异步执行」的地方都面向它编程，便于替换实现。
- **AsyncTaskExecutor**：扩展 `TaskExecutor`，增加 `submit()`（返回 `Future`）。
- **AsyncListenableTaskExecutor**：再扩展，支持 `ListenableFuture`——即 Spring 版「回调」（任务完成时触发 `ListenableFutureCallback`），比 JDK `Future.get()` 阻塞更优雅。
- **ThreadPoolTaskExecutor（最常用具体类）**：Spring 对 JDK `ThreadPoolExecutor` 的**包装**。它内部持有并委托一个 `ThreadPoolExecutor`，但配置更「Spring 风格」（setter 注入、JavaConfig 友好）。

调度侧：

- **TaskScheduler（接口）/ ThreadPoolTaskScheduler（实现）**：Spring 对 JDK `ScheduledThreadPoolExecutor` 的包装，配合 `@Scheduled` 注解做定时任务。

```
JDK 侧                               Spring 侧（抽象 / 包装）
──────────────────────────────────   ────────────────────────────────────────
Executor                         ◀──  TaskExecutor
ExecutorService                  ◀──  AsyncTaskExecutor ──▶ AsyncListenableTaskExecutor
ThreadPoolExecutor               ◀──  ThreadPoolTaskExecutor（内部委托一个 ThreadPoolExecutor）
ScheduledThreadPoolExecutor      ◀──  ThreadPoolTaskScheduler
```

> 关键认知：**`ThreadPoolTaskExecutor` 本质就是 `ThreadPoolExecutor`，只是换了个 Spring 友好的壳**。所以「Executors 的坑」在 Spring 里同样存在——`ThreadPoolTaskExecutor` 的 `queueCapacity` 默认是 `Integer.MAX_VALUE`（无界），一样会 OOM。

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

要点：
- `@EnableAsync` 开启；`@Async` 标注在 **public** 方法上。
- **自调用失效**：同一个类里 A 方法调 `this.b()`，`@Async` 不生效——Spring 靠 AOP 代理，必须**跨 bean 调用**才进代理。
- 返回值：`void` / `Future` / `CompletableFuture` / Spring 的 `ListenableFuture` 都支持；返回普通对象会被忽略（取到 `null`）。
- 方法抛异常：`void` 型异步方法的异常默认被 `SimpleAsyncUncaughtExceptionHandler` 吃掉（只打日志），如需处理可实现 `AsyncConfigurer.getAsyncUncaughtExceptionHandler()`。

⚠️ **默认执行器陷阱**：若你没有定义任何 `Executor` Bean，Spring 会 fallback 到 `SimpleAsyncTaskExecutor`——它**每次都 new 一个线程，根本不池化**！务必显式配置 `ThreadPoolTaskExecutor`。

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

## 六、生产实践 checklist

- [ ] 不复用 `Executors` 快捷方法，手动 `new ThreadPoolExecutor` 并明确 7 个参数。
- [ ] 使用**有界队列**（Spring 里改 `queueCapacity`），配合 `CallerRunsPolicy` 等合理拒绝策略防 OOM。
- [ ] 用 `ThreadFactory` / `setThreadNamePrefix` 给线程起名，便于日志排查。
- [ ] 区分 CPU / IO 密集，合理设置 `corePoolSize` / `maximumPoolSize`，并压测收敛。
- [ ] 任务内做好异常捕获（try/catch 或 `Future.get()` / `afterExecute`）；`@Async` 用 `AsyncConfigurer` 统一处理未捕获异常。
- [ ] 应用关闭时调用 `shutdown()` + `awaitTermination` 优雅退出。
- [ ] 跨业务使用线程池隔离；异步编排优先用 `CompletableFuture` 并指定自定义池。
- [ ] Spring Boot 中显式配置 `ThreadPoolTaskExecutor`（避免 fallback 到不池化的 `SimpleAsyncTaskExecutor`），并设**有界** `queueCapacity`。

---

## 参考资料

- [面试必备：Java线程池解析](https://juejin.cn/post/6844903889678893063) — 捡田螺的小男孩
- [如何优雅的使用和理解线程池](https://juejin.cn/post/6844903648405766158) — crossoverJie
- [Java—线程池 ThreadPoolExecutor 详解](https://juejin.cn/post/6844904146856837128) — Andya
