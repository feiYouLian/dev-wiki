# MVC

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [MVC](#mvc)
  - [调用流程](#调用流程)
  - [HttpEntity](#httpentity)
  - [返回值处理 HandlerMethodReturnValueHandler](#返回值处理-handlermethodreturnvaluehandler)
    - [⚙️ 工作原理与责任链模式](#️-工作原理与责任链模式)
    - [📋 核心内置实现类](#-核心内置实现类)
    - [🛠️ 自定义实现与扩展](#️-自定义实现与扩展)
    - [@ResponseBody](#responsebody)
  - [参数组装 HandlerMethodArgumentResolver](#参数组装-handlermethodargumentresolver)
  - [RequestMappingHandlerMapping](#requestmappinghandlermapping)
  - [http](#http)
    - [RequestMappingHandlerAdapter](#requestmappinghandleradapter)
    - [ConfigurableWebBindingInitializer](#configurablewebbindinginitializer)
  - [细节](#细节)
  - [静态资源](#静态资源)
  - [过滤器配置](#过滤器配置)

<!-- /code_chunk_output -->

## 调用流程

```java
0. Controller.method // GetMapping PostMapping
   ...
1. DispatcherServlet.doService(request, response);
2. FrameworkServlet.doService(request, response);
   // 3. FrameworkServlet.processRequest(request, response)自动注入 HttpServletRequest / HttpServletResponse
   3.5. AutowireUtils$ObjectFactoryDelegatingInvocationHandler
   3.4. WebApplicationContextUtils$RequestObjectFactory
   3.3. RequestContextHolder.currentRequestAttributes()
   3.2. RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
   3.1. FrameworkServlet.initContextHolders(request, localeContext, requestAttributes) // DispatcherServlet extends FrameworkServlet

3. FrameworkServlet.processRequest(request, response)
4. FrameworkServlet.doGet(HttpServletRequest request, HttpServletResponse response) // doPost doPut doDelete doOption...
```

## HttpEntity

> HttpEntityMethodProcessor

```java

@GetMapping("/customHeader")
ResponseEntity<String> customHeader() {
    return ResponseEntity.ok().header("Custom-Header", "foo").body("Custom header set");
}

@GetMapping("/manual")
void manual(HttpServletResponse response) throws IOException {
    response.setHeader("Custom-Header", "foo");
    response.setStatus(200);
    response.getWriter().println("Hello World!");
}

// 下载文件 第一种方法
@RequestMapping(value = "/download", method = GET)
public ResponseEntity<byte[]> download(@RequestParamString filePath) throws Exception {
    File file = fileSupport.getFile(filePath);
    if (file == null) {
        throw new FileNotFoundException(filePath + "不存在");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", file.getName());
    byte[] data = FileUtils.readFileToByteArray(file);

    // return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
    return ResponseEntity.ok().headers(headers).body(data);
}

// 下载文件 第一种方法 优化
@GetMapping("/downloadFile")
public ResponseEntity<FileSystemResource> downloadFile(@RequestParam("filepath") String filepath) throws UnsupportedEncodingException {
    Path path = Paths.get(filepath);
    File file = path.toFile();
    // 中文名乱码解决
    String fileName = URLEncoder.encode(file.getName(), "UTF-8");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setContentDisposition(ContentDisposition.inline().name("attachment").filename(fileName).build());
    return ResponseEntity.ok().headers(headers).body(new FileSystemResource(file));
}


// 下载文件 第二种方法
@RequestMapping(value = "/download", method = GET)
public void download(@RequestParam String filePath, HttpServletResponse response) throws Exception {

    BufferedInputStream bis = null;
    BufferedOutputStream bos = null;

    File file = fileSupport.getFile(filePath);
    if (file == null) {
        throw new FileNotFoundException(filePath + "不存在");
    }

    try {
        long fileLength = file.length();
        response.setContentType("text/plain;");
        response.setHeader("Content-disposition", "attachment; filename=" + file.getName());
        response.setHeader("Content-Length", String.valueOf(fileLength));
        bis = new BufferedInputStream(new FileInputStream(file));
        bos = new BufferedOutputStream(response.getOutputStream());
        byte[] buff = new byte[2048];
        int bytesRead;
        while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
            bos.write(buff, 0, bytesRead);
        }
    } catch (Exception e) {
        throw new FileDownloadException("文件流转换异常");
    } finally {
        if (bis != null)
            bis.close();
        if (bos != null)
            bos.close();
    }
}


```


## 返回值处理 HandlerMethodReturnValueHandler

```java
public interface HandlerMethodReturnValueHandler {
    // 判断当前处理器是否支持该返回值类型
    boolean supportsReturnType(MethodParameter returnType);
    
    // 处理返回值，可向 Model 添加属性、设置视图，或标记响应已直接处理
    void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
                           ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception;
}
```

- supportsReturnType：这是筛选机制。Spring MVC 会遍历所有已注册的处理器，通过此方法找到第一个能够处理当前返回值类型的处理器。

- handleReturnValue：这是执行逻辑。处理器可以修改 mavContainer（例如设置 viewName 或添加模型数据），或者调用 mavContainer.setRequestHandled(true) 来表明响应已经直接写入（如 JSON 序列化），无需再进行视图渲染。

### ⚙️ 工作原理与责任链模式
Spring MVC 并非直接使用单个处理器，而是通过 HandlerMethodReturnValueHandlerComposite 来管理一个有序的处理器列表，这体现了责任链设计模式。

其工作流程如下：

- 方法执行：在 ServletInvocableHandlerMethod.invokeAndHandle 中，Controller 方法被调用并返回结果。

- 委托处理：调用 HandlerMethodReturnValueHandlerComposite.handleReturnValue。

- 选择处理器：Composite 内部调用 selectHandler 方法，按顺序遍历其持有的处理器列表，并检查每个处理器的 supportsReturnType。

- 执行处理：一旦找到支持的处理器，就调用其 handleReturnValue 方法进行实际处理。如果未找到，则抛出 IllegalArgumentException。

- 异步特例：如果返回值是异步类型（如 Callable），selectHandler 会跳过所有非 AsyncHandlerMethodReturnValueHandler 的处理器。

### 📋 核心内置实现类
Spring MVC 提供了丰富的内置实现，以应对各种返回值场景。以下是主要实现类的功能对比：

| 实现类                                  | 支持的返回值类型                                               | 核心作用                                                                                                                                     |
| --------------------------------------- | -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| RequestResponseBodyMethodProcessor      | 标注了 @ResponseBody 的方法返回值                              | 通过 HttpMessageConverter 将返回值序列化为 JSON/XML 等格式，并直接写入响应体。这是处理 RESTful API 的核心处理器。                            |
| ViewNameMethodReturnValueHandler        | void 和 String（且无 @ResponseBody）                           | 将 String 解释为逻辑视图名。如果是 void，则交由 RequestToViewNameTranslator 根据请求路径推导视图名。它也负责识别重定向前缀（如 redirect:）。 |
| ViewMethodReturnValueHandler            | View 及其子类                                                  | 直接使用返回的 View 对象进行渲染，并识别重定向视图。                                                                                         |
| ModelAndViewMethodReturnValueHandler    | ModelAndView                                                   | 从返回的 ModelAndView 对象中提取模型数据和视图信息，并填充到 mavContainer 中。                                                               |
| MapMethodProcessor                      | Map                                                            | 将返回的 Map 作为模型数据添加到 mavContainer 中。注意，它不会设置视图名，因此通常需要配合其他机制来确定视图。                                |
| CallableMethodReturnValueHandler        | Callable                                                       | 处理异步请求。它会启动异步处理，将 Callable 提交给任务执行器，并在其执行完成后继续处理结果。                                                 |
| DeferredResultMethodReturnValueHandler  | DeferredResult, ListenableFuture, CompletionStage              | 处理更灵活的异步返回类型，允许在另一个线程中稍后设置结果。                                                                                   |
| StreamingResponseBodyReturnValueHandler | StreamingResponseBody 或 `ResponseEntity<StreamingResponseBody>` | 用于流式响应，例如大文件下载，允许分块写入响应体。                                                                                           |
| HttpEntityMethodProcessor               | HttpEntity 或 ResponseEntity                                   | 处理包含完整 HTTP 响应信息（状态码、头信息、正文）的返回值。                                                                                 |

### 🛠️ 自定义实现与扩展
当内置处理器无法满足需求时（例如，需要统一包装 API 响应格式或对返回值进行加解密），你可以自定义 HandlerMethodReturnValueHandler。

实现步骤：

1. 创建自定义类：实现 HandlerMethodReturnValueHandler 接口，在 supportsReturnType 中定义你的匹配规则（例如，检查方法或类上是否有特定注解），在 handleReturnValue 中编写你的处理逻辑（如包装数据、加密后写入响应）。

2. 注册处理器：在 Spring MVC 配置中，将你的自定义处理器添加到 RequestMappingHandlerAdapter 的 returnValueHandlers 列表中。

> 关键技巧：控制顺序
> 由于处理器是按顺序匹配的，将你的自定义处理器插入到列表的最前面可以确保它优先被选中，从而覆盖默认行为。

💡 与 ResponseBodyAdvice 的选择
你可能听说过 ResponseBodyAdvice 也能修改响应体，它们的主要区别在于：

ResponseBodyAdvice：作用于 @ResponseBody 注解的处理流程内部，在 HttpMessageConverter 写入响应体之前进行干预。它更适用于对响应体内容进行修改（如包装、加密）。

HandlerMethodReturnValueHandler：作用于更外层，决定返回值整体如何处理。它更适用于改变返回值的处理流程，例如，将原本要渲染视图的返回值改为直接写入响应，或反之。它的灵活性和控制粒度更高。

总的来说，HandlerMethodReturnValueHandler 是 Spring MVC 实现灵活、可扩展的返回值处理机制的基石，通过策略模式和责任链模式，优雅地支持了从传统视图渲染到现代 RESTful API 的各种场景。



### @ResponseBody

> RequestResponseBodyMethodProcessor 


```java
// todo
```

## 参数组装 HandlerMethodArgumentResolver

参数组装

```java
// todo
```

## RequestMappingHandlerMapping

requestPath 和 method 的映射管理

```java
// todo
```

## http

### RequestMappingHandlerAdapter

```java

public interface HandlerAdapter {
    // 用于判断当前HandlerAdapter是否能够处理当前请求
	boolean supports(Object handler);

	// 如果当前HandlerAdapter能够用于适配当前请求，那么就会处理当前请求中
    // 诸如参数和返回值等信息，以便能够直接委托给具体的Handler处理
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response,
        Object handler) throws Exception;

    // 获取当前请求的最后更改时间，主要用于供给浏览器判断当前请求是否修改过，
    // 从而判断是否可以直接使用之前缓存的结果
	long getLastModified(HttpServletRequest request, Object handler);
}
// todo
public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter implements BeanFactoryAware, InitializingBean
```

### ConfigurableWebBindingInitializer

将请求的参数转化为对应的 JavaBean，并且会结合类型、格式转换等 API 一起使用。

```java
// todo
public class ConfigurableWebBindingInitializer implements WebBindingInitializer
```

## 细节

```java
//1.设置文件ContentType类型，这样设置，会自动判断下载文件类型
response.setContentType("multipart/form-data");
 //2.设置文件头：最后一个参数是设置下载文件名(假如我们叫a.pdf)
response.setHeader("Content-Disposition", "attachment;fileName="+"a.pdf");

/**
总结: 一般是 前台 -> 后台， 对某个入参(requestParam) String -> Date
*/
@DateTimeFormat

/**
1. 前台 -> 后台
	Request Method: POST
	Content-Type: application/json
2. 后台 -> 前台

总结: 只要涉及到JSON,就用 @JsonFormat
全量配置 spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
*/
@JsonFormat

```


```java


org.springframework.web.filter.RequestContextFilter extends OncePerRequestFilter {

    private void initContextHolders(HttpServletRequest request, ServletRequestAttributes requestAttributes) {
        LocaleContextHolder.setLocale(request.getLocale(), this.threadContextInheritable);
        RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Bound request context to thread: " + request);
        }

    }
}


org.springframework.web.context.support.WebApplicationContextUtils {


    public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory, @Nullable ServletContext sc) {
        beanFactory.registerScope("request", new RequestScope());
        beanFactory.registerScope("session", new SessionScope());
        if (sc != null) {
            ServletContextScope appScope = new ServletContextScope(sc);
            beanFactory.registerScope("application", appScope);
            sc.setAttribute(ServletContextScope.class.getName(), appScope);
        }

        beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());
        beanFactory.registerResolvableDependency(ServletResponse.class, new ResponseObjectFactory());
        beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectFactory());
        beanFactory.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
        if (jsfPresent) {
            WebApplicationContextUtils.FacesDependencyRegistrar.registerFacesDependencies(beanFactory);
        }

    }
	
    private static ServletRequestAttributes currentRequestAttributes() {
        RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
        if (!(requestAttr instanceof ServletRequestAttributes)) {
            throw new IllegalStateException("Current request is not a servlet request");
        } else {
            return (ServletRequestAttributes)requestAttr;
        }
    }

    // HttpServletRequest  代理对象
	private static class RequestObjectFactory implements ObjectFactory<ServletRequest>, Serializable {
        private RequestObjectFactory() {
        }

        public ServletRequest getObject() {
        	// 获取实际的Object
            return WebApplicationContextUtils.currentRequestAttributes().getRequest();
        }

        public String toString() {
            return "Current HttpServletRequest";
        }
    }

}

// spring boot 启动时设置 ApplicationContext
org.springframework.web.context.support.AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

    @Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
        // 设置 beanFractory
		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
	}

}


// 自定义配置  beanFactory, 实现 BeanFactoryPostProcessor， 
// beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());
@FunctionalInterface
public interface BeanFactoryPostProcessor {
    void postProcessBeanFactory(ConfigurableListableBeanFactory var1) throws BeansException;
}


```


## 静态资源 


静态资源的配置原理在 `WebMvcAutoConfiguration` 这个自动配置类中定义了

比如这两个组件 ：

```java

// REST 请求的过滤器
@Bean
@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
@ConditionalOnProperty(prefix = "spring.mvc.hiddenmethod.filter", name = "enabled")
public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
   return new OrderedHiddenHttpMethodFilter();
}
​
// 表单过滤器
@Bean
@ConditionalOnMissingBean(FormContentFilter.class)
@ConditionalOnProperty(prefix = "spring.mvc.formcontent.filter", name = "enabled", matchIfMissing = true)
public OrderedFormContentFilter formContentFilter() {
   return new OrderedFormContentFilter();
}

```

但我们要找的静态资源配置类在一个名为 `WebMvcAutoConfigurationAdapter` 的静态内部类中

类定义如下


```java
@SuppressWarnings("deprecation") // SpringBoot 版本 2.6.3，显示过时
@Configuration(proxyBeanMethods = false)
@Import(EnableWebMvcConfiguration.class)
@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
@Order(0)
public static class WebMvcAutoConfigurationAdapter implements WebMvcConfigurer, ServletContextAware {
    // 里面只有一个构造方法
    public WebMvcAutoConfigurationAdapter(
                 // 获取和 "spring.web" 绑定的所有的值的对象，此处低版本的参数为 ResourceProperties resourceProperties
                 WebProperties webProperties, 
                 // 获取和 "spring.mvc" 绑定的所有的值的对象
                 WebMvcProperties mvcProperties,
                 // 获取 Spring 容器
                ListableBeanFactory beanFactory, 
                 // 消息转换器
                 ObjectProvider<HttpMessageConverters> messageConvertersProvider,
                 // 资源处理器自定义器
                ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
                 // dispatcherServlet 能处理的路径
                ObjectProvider<DispatcherServletPath> dispatcherServletPath,
                 // 给应用注册原生的 Servlet、Filter 等
                ObjectProvider<ServletRegistrationBean<?>> servletRegistrations) {
            this.resourceProperties = webProperties.getResources(); // 底层写死了四个默认资源路径
            this.mvcProperties = mvcProperties;
            this.beanFactory = beanFactory;
            this.messageConvertersProvider = messageConvertersProvider;
            this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizerProvider.getIfAvailable();
            this.dispatcherServletPath = dispatcherServletPath;
            this.servletRegistrations = servletRegistrations;
            this.mvcProperties.checkConfiguration();
        }
}

```

资源处理的默认规则

> 请求进来，先去找 Controller 看能不能处理，不能处理的所有请求又都交给静态资源处理器，静态资源也找不到则响应404页面

在静态内部类中的这个方法里

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
   if (!this.resourceProperties.isAddMappings()) {
      logger.debug("Default resource handling disabled");
      return;
   }
   // 调用 1（处理 /webjars 的请求）
   addResourceHandler(registry, "/webjars/**", "classpath:/META-INF/resources/webjars/");
   // 调用 2 
   // this.mvcProperties.getStaticPathPattern()：默认是 "/**" 
   addResourceHandler(registry, this.mvcProperties.getStaticPathPattern(), (registration) -> {
      // this.resourceProperties.getStaticLocations()：默认是 "classpath:/META-INF/resources/","classpath:/resources/", "classpath:/static/", "classpath:/public/"
      registration.addResourceLocations(this.resourceProperties.getStaticLocations());
      if (this.servletContext != null) {
         ServletContextResource resource = new ServletContextResource(this.servletContext, SERVLET_LOCATION);
         registration.addResourceLocations(resource);
      }
   });
}


```



## 过滤器配置


| 特性           | @WebFilter + @ServletComponentScan           | FilterRegistrationBean + @Bean           |
| -------------- | -------------------------------------------- | ---------------------------------------- |
| 注册方式       | 依赖 Servlet 容器自动扫描                    | 由 Spring 容器显式管理                   |
| URL 匹配规则   | 通过 urlPatterns 属性定义                    | 通过 addUrlPatterns() 方法动态设置       |
| 执行顺序控制   | 通过 @Order 注解或 filterName                | 隐式控制	通过 setOrder() 方法显式控制    |
| 过滤器实例管理 | 由 Servlet 容器创建和管理                    | 由 Spring 容器管理（可自定义初始化逻辑） |
| 依赖注入支持   | 需要额外配置（如 @Autowired 可能不直接生效） | 天然支持 Spring 依赖注入                 |
| 适用场景       | 简单过滤器，无需复杂配置                     | 需要动态配置或精细控制过滤器的场景       |



代码示例对比
方式 1：@WebFilter 注解
```java
@WebFilter(urlPatterns = "/*")  // 定义过滤路径
public class RepeatableFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // 过滤器逻辑
    }
}
```
启动类需添加 @ServletComponentScan：
```java
@SpringBootApplication
@ServletComponentScan  // 扫描 @WebFilter 等 Servlet 组件
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
```
方式 2：FilterRegistrationBean + @Bean
```java
@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<RepeatableFilter> repeatableFilterBean() {
        FilterRegistrationBean<RepeatableFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RepeatableFilter());  // 可自定义实例化逻辑
        bean.addUrlPatterns("/*");              // 动态设置 URL 匹配规则
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);  // 显式控制执行顺序
        return bean;
    }
}
```
3. 关键差异点

(1) 执行顺序控制
@WebFilter：
依赖 @Order 注解或过滤器类名的字典序，不够直观。
FilterRegistrationBean：
通过 setOrder() 直接指定优先级，更灵活可控。
(2) 依赖注入支持
@WebFilter：
过滤器实例由 Servlet 容器创建，无法直接使用 Spring 的 @Autowired，需通过 ServletContext 间接获取 Bean。
FilterRegistrationBean：
过滤器实例由 Spring 管理，可直接注入其他 Bean：
```java 
@Bean
public FilterRegistrationBean<MyFilter> myFilter(SomeService service) { // 直接注入
    FilterRegistrationBean<MyFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new MyFilter(service));  // 依赖传递给过滤器
    return bean;
}
```
(3) 动态配置能力
```java 
@WebFilter：
URL 匹配规则和参数在编译时固定。
FilterRegistrationBean：
可根据条件动态配置（例如从配置文件读取参数）：
@Value("${filter.url-patterns}")
private String[] urlPatterns;

@Bean
public FilterRegistrationBean<MyFilter> myFilter() {
    FilterRegistrationBean<MyFilter> bean = new FilterRegistrationBean<>();
    bean.addUrlPatterns(urlPatterns);  // 动态注入配置
    return bean;
}
```
1. 如何选择？
简单场景：
若过滤器无需复杂逻辑，优先用 @WebFilter（代码更简洁）。
复杂场景：
若需要动态配置、依赖注入或精细控制执行顺序，选择 FilterRegistrationBean。


示例 多个`@RequestBody` 接收


```java


/**
 * 启动
 */
@ServletComponentScan
@SpringBootApplication
public class ApiServer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DmpApiServer.class);
    }

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(ApiServer.class, args);
    }
}

@WebFilter(urlPatterns = "/*", filterName = "httpServletRequestReplacedFilter")
public class HttpServletRequestReplacedFilter implements Filter {

    @Override
    public void destroy() {
        // 这个过滤器不需要进行任何清理操作，所以这个方法保持为空。
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof APIHttpServletRequestWrapper) && request instanceof HttpServletRequest) {
            request = new APIHttpServletRequestWrapper((HttpServletRequest) request);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 这个过滤器不需要进行任何初始化操作，所以这个方法保持为空。
    }
}


public class APIHttpServletRequestWrapper extends HttpServletRequestWrapper {
    // 报文
    private byte[] body = null;
    static final int BUFFER_SIZE = 4096;
    HttpServletRequest request = null;
    private Map<String, String[]> parameterMap;

    public APIHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.request = request;
        parameterMap = request.getParameterMap();
    }

    @Override
    public String[] getParameterValues(String name) {
        if (parameterMap != null) {
            return parameterMap.get(name);
        }
        return null;
    }

    /**
     * 获取请求体
     *
     * @return 请求体
     */
    public String getBody() throws IOException {
        return new String(getBodyBytes(), request.getCharacterEncoding());
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        byte[] bodyBytes = getBodyBytes();
        final ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);
        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                //  Auto-generated method stub
                return false;
            }

            @Override
            public boolean isReady() {
                //  Auto-generated method stub
                return false;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                //  Auto-generated method stub

            }
        };
    }

    /**
     * 获取请求体字节数组
     *
     * @return 请求体字节数组
     * @throws IOException
     */
    private byte[] getBodyBytes() throws IOException {
        if (body == null) {
            body = inputStreamToByte(request.getInputStream());
        }
        return body;
    }

    // 将InputStream转换成byte数组
    public static byte[] inputStreamToByte(InputStream in) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        byte[] data = new byte[BUFFER_SIZE];

        int count = -1;

        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
            outStream.write(data, 0, count);
        }

        data = null;

        return outStream.toByteArray();

    }
}
```



```java

//  通过实现 InitializingBean的 afterPropertiesSet()方法，在应用启动时从 RequestMappingHandlerMapping获取所有路由。
//  目标：将 ResponseExcelReturnValueHandler和 RequestExcelArgumentResolver置于 Spring MVC 处理器链的最前面。
 public class PermitAllUrlProperties implements InitializingBean {

 	@Override
 	public void afterPropertiesSet() {
 //		ignoreUrls.addAll(Arrays.asList(DEFAULT_IGNORE_URLS));
 		RequestMappingHandlerMapping mapping = SpringContextHolder.getBean("requestMappingHandlerMapping");
 		Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

 		map.keySet().forEach(info -> {
 			HandlerMethod handlerMethod = map.get(info);

 			// 获取方法上边的注解 替代path variable 为 *
 			Inner method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Inner.class);
 			Optional.ofNullable(method).ifPresent(inner -> Objects.requireNonNull(info.getPathPatternsCondition())
 					.getPatternValues().forEach(url -> ignoreUrls.add(ReUtil.replaceAll(url, PATTERN, "*"))));

 			// 获取类上边的注解, 替代path variable 为 *
 			Inner controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Inner.class);
 			Optional.ofNullable(controller).ifPresent(inner -> Objects.requireNonNull(info.getPathPatternsCondition())
 					.getPatternValues().forEach(url -> ignoreUrls.add(ReUtil.replaceAll(url, PATTERN, "*"))));
 		});
 	}

 }



```
