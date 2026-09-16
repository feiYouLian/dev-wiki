# MVC

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [MVC](#mvc)
  - [调用流程](#调用流程)
  - [HttpEntity](#httpentity)
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

## @ResponseBody

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
