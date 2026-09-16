# Spring 高级使用技巧

> 来源：spring 中那些让你爱不释手的代码技巧（掘金文章，苏三说技术）。本文由原 HTML 文件整理转换而来。

## 目录

1. [如何获取 Spring 容器对象](#一如何获取-spring-容器对象)
2. [如何初始化 Bean](#二如何初始化-bean)
3. [自定义 Scope](#三自定义-scope)
4. [FactoryBean 的使用](#四factorybean-的使用)
5. [自定义类型转换](#五自定义类型转换)
6. [Spring MVC 拦截器](#六spring-mvc-拦截器)
7. [Enable 开关](#七enable-开关)
8. [RestTemplate 拦截器](#八resttemplate-拦截器)
9. [统一异常处理](#九统一异常处理)
10. [优雅的异步](#十优雅的异步)
11. [缓存的使用](#十一缓存的使用)

---

## 一、如何获取 Spring 容器对象

### 1. 实现 BeanFactoryAware 接口

```java
@Service
public class PersonService implements BeanFactoryAware {
    private BeanFactory beanFactory;
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    public void add() {
        Person person = (Person) beanFactory.getBean("person");
    }
}
```

### 2. 实现 ApplicationContextAware 接口

```java
@Service
public class PersonService2 implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    public void add() {
        Person person = (Person) applicationContext.getBean("person");
    }
}
```

### 3. 实现 ApplicationListener 接口

```java
@Service
public class PersonService3 implements ApplicationListener<ContextRefreshedEvent> {
    private ApplicationContext applicationContext;
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        applicationContext = event.getApplicationContext();
    }
    public void add() {
        Person person = (Person) applicationContext.getBean("person");
    }
}
```

> **Aware 接口**是一个空接口，表示已感知的意思，通过这类接口可以获取指定对象：`BeanFactoryAware` 获取 BeanFactory、`ApplicationContextAware` 获取 ApplicationContext、`BeanNameAware` 获取 BeanName 等。

---

## 二、如何初始化 Bean

Spring 支持 3 种初始化 bean 的方法：XML 中指定 init-method、使用 `@PostConstruct` 注解、实现 `InitializingBean` 接口。

### 1. 使用 @PostConstruct 注解

```java
@Service
public class AService {
    @PostConstruct
    public void init() {
        System.out.println("===初始化===");
    }
}
```

### 2. 实现 InitializingBean 接口

```java
@Service
public class BService implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("===初始化===");
    }
}
```

**执行顺序**：决定调用顺序的关键代码在 `AbstractAutowireCapableBeanFactory` 类的 `initializeBean` 方法中。先调用 BeanPostProcessor 的 `postProcessBeforeInitialization`，而 `@PostConstruct` 是通过 `InitDestroyAnnotationBeanPostProcessor` 实现的（它就是一个 BeanPostProcessor），所以 `@PostConstruct` 先执行。`invokeInitMethods` 方法决定先调用 InitializingBean，再调用 init-method。

> 执行顺序为：**@PostConstruct → InitializingBean → init-method**

---

## 三、自定义 Scope

Spring 默认支持的 Scope：`singleton`（单例）、`prototype`（多例）。Spring web 扩展了 `RequestScope`、`SessionScope`。

**自定义 Scope 示例（ThreadLocalScope）：**

第一步，实现 Scope 接口：

```java
public class ThreadLocalScope implements Scope {
    private static final ThreadLocal THREAD_LOCAL_SCOPE = new ThreadLocal();
    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Object value = THREAD_LOCAL_SCOPE.get();
        if (value != null) {
            return value;
        }
        Object object = objectFactory.getObject();
        THREAD_LOCAL_SCOPE.set(object);
        return object;
    }
    @Override
    public Object remove(String name) {
        THREAD_LOCAL_SCOPE.remove();
        return null;
    }
    @Override
    public void registerDestructionCallback(String name, Runnable callback) {}
    @Override
    public Object resolveContextualObject(String key) { return null; }
    @Override
    public String getConversationId() { return null; }
}
```

第二步，将新定义的 Scope 注入到 Spring 容器中：

```java
@Component
public class ThreadLocalBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope("threadLocalScope", new ThreadLocalScope());
    }
}
```

第三步，使用新定义的 Scope：

```java
@Scope("threadLocalScope")
@Service
public class CService {
    public void add() {}
}
```

---

## 四、FactoryBean 的使用

- **BeanFactory**：Spring 容器的顶级接口，管理 bean 的工厂。
- **FactoryBean**：隐藏了实例化一些复杂 Bean 的细节，给上层应用带来便利。

> mybatis 的 `SqlSessionFactory` 对象就是通过 `SqlSessionFactoryBean` 类创建的。

```java
@Component
public class MyFactoryBean implements FactoryBean {
    @Override
    public Object getObject() throws Exception {
        String data1 = buildData1();
        String data2 = buildData2();
        return buildData3(data1, data2);
    }
    private String buildData1() { return "data1"; }
    private String buildData2() { return "data2"; }
    private String buildData3(String data1, String data2) { return data1 + data2; }
    @Override
    public Class<?> getObjectType() { return null; }
}
```

> 获取 FactoryBean 实例对象：
> - `getBean("myFactoryBean")` 获取的是 `getObject()` 方法返回的对象
> - `getBean("&myFactoryBean")` 获取的才是 FactoryBean 对象本身

---

## 五、自定义类型转换

Spring 支持 3 种类型转换器：

- **Converter<S,T>**：将 S 类型对象转为 T 类型对象
- **ConverterFactory<S, R>**：将 S 类型对象转为 R 类型及子类对象
- **GenericConverter**：支持多个 source 和目标类型的转化，还提供 source 和目标类型的上下文

以 `Converter<S,T>` 为例，将字符串日期转换为 Date：

第一步，定义实体：

```java
@Data
public class User {
    private Long id;
    private String name;
    private Date registerDate;
}
```

第二步，实现 Converter 接口：

```java
public class DateConverter implements Converter<String, Date> {
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Override
    public Date convert(String source) {
        if (source != null && !"".equals(source)) {
            try {
                simpleDateFormat.parse(source);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
```

第三步，将类型转换器注入到 Spring 容器中：

```java
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new DateConverter());
    }
}
```

第四步，调用接口：

```java
@RequestMapping("/user")
@RestController
public class UserController {
    @RequestMapping("/save")
    public String save(@RequestBody User user) {
        return "success";
    }
}
```

请求接口时 User 对象中 registerDate 字段会被自动转换成 Date 类型。

---

## 六、Spring MVC 拦截器

Spring MVC 拦截器能够获取 HttpServletRequest 和 HttpServletResponse 等 web 对象实例。顶层接口是 `HandlerInterceptor`，包含三个方法：

- `preHandle`：目标方法执行前执行
- `postHandle`：目标方法执行后执行
- `afterCompletion`：请求完成时执行

一般用 `HandlerInterceptorAdapter` 实现类。

第一步，定义拦截器：

```java
public class AuthInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUrl = request.getRequestURI();
        if (checkAuth(requestUrl)) {
            return true;
        }
        return false;
    }
    private boolean checkAuth(String requestUrl) {
        System.out.println("===权限校验===");
        return true;
    }
}
```

第二步，注册拦截器：

```java
@Configuration
public class WebAuthConfig extends WebMvcConfigurerAdapter {
    @Bean
    public AuthInterceptor getAuthInterceptor() {
        return new AuthInterceptor();
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getAuthInterceptor());
    }
}
```

> 在 `DispatcherServlet` 类的 `doDispatch` 方法中可以看到调用过程。

---

## 七、Enable 开关

如 `@EnableAsync`、`@EnableCaching`、`@EnableAspectJAutoProxy` 等，像开关一样，在 `@Configuration` 定义的配置类上加上这类注解就能开启相关功能。

实现自定义开关：

第一步，定义 LogFilter：

```java
public class LogFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("记录请求日志");
        chain.doFilter(request, response);
        System.out.println("记录响应日志");
    }
}
```

第二步，注册 LogFilter：

```java
@ConditionalOnWebApplication
public class LogFilterWebConfig {
    @Bean
    public LogFilter timeFilter() {
        return new LogFilter();
    }
}
```

第三步，定义开关 `@EnableLog` 注解：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LogFilterWebConfig.class)
public @interface EnableLog {
}
```

第四步，在 SpringBoot 启动类加上 `@EnableLog` 注解即可。

---

## 八、RestTemplate 拦截器

使用 RestTemplate 调用远程接口时，在 header 中传递 traceId、source 等信息：

```java
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set("traceId", MdcUtil.get());
        return execution.execute(request, body);
    }
}

@Configuration
public class RestTemplateConfiguration {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(restTemplateInterceptor()));
        return restTemplate;
    }
    @Bean
    public RestTemplateInterceptor restTemplateInterceptor() {
        return new RestTemplateInterceptor();
    }
}
```

MdcUtil 利用 MDC 工具在 ThreadLocal 中存储和获取 traceId：

```java
public class MdcUtil {
    private static final String TRACE_ID = "TRACE_ID";
    public static String get() {
        return MDC.get(TRACE_ID);
    }
    public static void add(String value) {
        MDC.put(TRACE_ID, value);
    }
}
```

---

## 九、统一异常处理

使用 `@RestControllerAdvice` 进行全局异常处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        if (e instanceof ArithmeticException) {
            return "数据异常";
        }
        if (e instanceof Exception) {
            return "服务器内部异常";
        }
        return null;
    }
}
```

只需在 `handleException` 方法中处理异常情况，业务接口中不再需要捕获异常。

---

## 十、优雅的异步

**传统方式**：继承 Thread 类、实现 Runnable 接口、使用线程池。

**Spring 异步方式**：

第一步，启动类加 `@EnableAsync` 注解：

```java
@EnableAsync
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```

第二步，在方法上加 `@Async` 注解：

```java
@Service
public class PersonService {
    @Async
    public String get() {
        System.out.println("===add==");
        return "data";
    }
}
```

**自定义线程池**：

```java
@Configuration
public class ThreadPoolConfig {
    @Value("${thread.pool.corePoolSize:5}")
    private int corePoolSize;
    @Value("${thread.pool.maxPoolSize:10}")
    private int maxPoolSize;
    @Value("${thread.pool.queueCapacity:200}")
    private int queueCapacity;
    @Value("${thread.pool.keepAliveSeconds:30}")
    private int keepAliveSeconds;
    @Value("${thread.pool.threadNamePrefix:ASYNC_}")
    private String threadNamePrefix;

    @Bean
    public Executor MessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 十一、缓存的使用

以 caffeine 为例：

第一步，引入 jar 包：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>2.6.0</version>
</dependency>
```

第二步，配置 CacheManager，开启 EnableCaching：

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)  // 最后一次写入后经过固定时间过期
            .maximumSize(1000);  // 缓存的最大条数
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
```

第三步，使用 `@Cacheable` 注解获取数据：

```java
@Service
public class CategoryService {
    // category是缓存名称, #type是具体的key，可支持el表达式
    @Cacheable(value = "category", key = "#type")
    public CategoryModel getCategory(Integer type) {
        return getCategoryByType(type);
    }
    private CategoryModel getCategoryByType(Integer type) {
        System.out.println("根据不同的type:" + type + "获取不同的分类数据");
        CategoryModel categoryModel = new CategoryModel();
        categoryModel.setId(1L);
        categoryModel.setParentId(0L);
        categoryModel.setName("电器");
        categoryModel.setLevel(3);
        return categoryModel;
    }
}
```

> 调用方法时，先从 caffeine 缓存中获取数据，如果能获取到则直接返回，不会进入方法体。如果不能获取到数据，则执行方法体获取数据后放到缓存中。
