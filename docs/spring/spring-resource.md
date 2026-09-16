

```java

@Service
public class ResourceService {
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    public void readFileWithResourceLoader() throws IOException {
        // 使用 "classpath:" 前缀明确指定从类路径加载
        Resource resource = resourceLoader.getResource("classpath:data/config.json");
        
        try (InputStream inputStream = resource.getInputStream()) {
            // ... 处理输入流
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            System.out.println(content);
        }
    }
}
```


```java


public interface ResourcePatternResolver extends ResourceLoader {
    String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

    Resource[] getResources(String locationPattern) throws IOException;
}


// 使用classpath*:前缀匹配所有JAR包和模块中的文件[6](@ref)
Resource[] resources = resourcePatternResolver.getResources("classpath*:static/changelog.md");
```



1. ResourceLoader​ 的核心任务是返回一个确定的资源。当你传入一个具体路径（如 "classpath:application.properties"），它会返回对应的那个 Resource对象。如果路径不明确（如使用了通配符），它可能无法正确工作
2. ResourcePatternResolver​ 的核心优势在于能一次性返回多个匹配的资源。这在需要批量操作的场景下非常有用，例如加载所有模块的配置文件（classpath*:config/*.xml）或扫描特定注解的类



```java

// ApplicationContext extends ResourcePatternResolver extends ResourceLoader
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory, MessageSource, ApplicationEventPublisher, ResourcePatternResolver {
    @Nullable
    String getId();

    String getApplicationName();

    String getDisplayName();

    long getStartupDate();

    @Nullable
    ApplicationContext getParent();

    AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;
}
```
