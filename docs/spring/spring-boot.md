# Spring Boot 启动流程

> 本文合并整理自 `refresh()` 方法流程梳理与 Spring Boot 启动流程笔记。

## 目录

- [refresh 整体结构](#refresh-整体结构)
- [流程梳理](#流程梳理)
- [启动流程调用链](#启动流程调用链)
- [getBean 处理过程](#getbean-处理过程)
- [循环依赖解决](#循环依赖解决)
- [相关模块索引](#相关模块索引)

---

## refresh 整体结构

下面的代码来自 `AbstractApplicationContext` 类，只保留关键处理流程。Spring 启动时会执行这个方法，主要流程就是 try 代码块中的几个方法，功能包含：

1. IOC 容器初始化
2. 对扫描到的 class 进行封装
3. bean 的实例化、属性填充、初始化、扩展点方法的调用
4. AOP 包装

```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");
        prepareRefresh();
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
        prepareBeanFactory(beanFactory);

        try {
            postProcessBeanFactory(beanFactory);
            StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");
            invokeBeanFactoryPostProcessors(beanFactory);
            registerBeanPostProcessors(beanFactory);
            beanPostProcess.end();
            initMessageSource();
            initApplicationEventMulticaster();
            onRefresh();
            registerListeners();
            finishBeanFactoryInitialization(beanFactory);
            finishRefresh();
        } catch (BeansException ex) {
            destroyBeans();
            cancelRefresh(ex);
            throw ex;
        } finally {
            resetCommonCaches();
            contextRefresh.end();
        }
    }
}
```

---

## 流程梳理

### 1. prepareRefresh

刷新前准备工作。清理 scanner 的缓存，初始化 PropertySources，校验环境中的一些关键属性。

### 2. obtainFreshBeanFactory

获取刷新的 BeanFactory。SpringBoot 启动时，beanFactory 是在 refresh 执行之前创建的，这里直接返回。

### 3. prepareBeanFactory(beanFactory)

进一步完善 BeanFactory，设置一些 IOC 容器加载时必须使用的 BeanPostProcessor 和 register，为它的各项成员变量赋值。

### 4. postProcessBeanFactory(beanFactory)

空实现，留给子类扩展。在 SpringBoot 中会进入 `AnnotationConfigReactiveWebServerApplicationContext` 等，注册 Web 相关的 scope 和依赖。

### 5. invokeBeanFactoryPostProcessors(beanFactory)

调用 beanFactory 后处理器。beanFactory 后处理器充当 beanFactory 的扩展点，可以用来补充或修改 BeanDefinition。

**主要流程：**

1. 调用 `BeanDefinitionRegistryPostProcessor` 的 `postProcessBeanDefinitionRegistry` 方法，扫描 class，封装成 BeanDefinition。分三次调用：
   - 第一次：实现 `PriorityOrdered` 接口的
   - 第二次：实现 `Ordered` 接口的
   - 第三次：其他的所有注册器（例如 mybatis 的注册器）
2. 调用 `BeanFactoryPostProcessor` 的 `postProcessBeanFactory` 方法，在 bean 创建之前修改 bean 的定义属性。同样分三次调用。

**关键类功能：**

- **ConfigurationClassPostProcessor**：Spring 框架中唯一实现 `BeanDefinitionRegistryPostProcessor` 接口的类。从启动类作为入口，解析 `@Configuration` 标记的类。依次解析处理：`@PropertySource`、`@ComponentScan`/`@ComponentScans`、`@Import`、`@ImportResource`、`@Bean` 注解的方法。
- **ClassPathBeanDefinitionScanner**：`doScan` 方法干活，把能读取到的 class 文件中使用了 `@Component` 注解的类都封装成 BeanDefinition。`@Bean` 注解标记的 class，判断 scope 是否需要创建代理对象。
- **ConfigurationClassParser**：`doProcessConfigurationClass` 方法解析当前类，把 `@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`、`@Bean`、接口方法、super 类方法都进行处理。
- **PropertySourcesPlaceholderConfigurer**：替换 BeanDefinition 中的 `${ }`。

**扩展点：**

1. `@Import`、`@ImportResource` 注解。在执行到这两个注解的处理时可以把自定义的对象导入到 spring 容器中。
2. `BeanDefinitionRegistryPostProcessor` 接口。实现类放到 spring 中之后，spring 会调用对应的 `postProcessBeanDefinitionRegistry`，实现自定义对象的注册。

### 6. registerBeanPostProcessors(beanFactory)

实例化 bean 后处理器。通过前面的扫描和注册，spring 能管理的 bean 都已经被读取出来了。这里找出实现了 `BeanPostProcessor` 接口的类，按优先级排序后注册。优先级从高到低依次是：

1. 实现 `PriorityOrdered` 接口的
2. 实现 `Ordered` 接口的
3. 没优先级和 order 顺序的
4. internal 的

### 7. initMessageSource

为 ApplicationContext 添加 messageSource 成员，实现国际化功能。

### 8. initApplicationEventMulticaster

为 ApplicationContext 添加事件广播器成员（applicationEventEventMulticaster），注册到 beanFactory 中。

### 9. onRefresh

空实现，留给子类扩展。Web 应用中这里会使用内置的 tomcat 创建一个 webserver。

### 10. registerListeners

从多种途径找到事件监听器，并添加至 applicationEventMulticaster。

### 11. finishBeanFactoryInitialization(beanFactory)

将 beanFactory 的成员补充完毕，并初始化所有非延迟单例 bean。

**主要流程：**

1. 遍历所有的 beanName，通过 `getBean` 去创建需要加载的 bean。涉及实例化、填充、初始化三个阶段。
2. 再遍历所有的 beanName，找到 `SmartInitializingSingleton` 类型的 bean，调用它的 `afterSingletonsInstantiated` 方法。

### 12. finishRefresh

为 ApplicationContext 添加 lifecycleProcessor 成员，用来控制容器内需要生命周期管理的 bean。

---

## 启动流程调用链

```
-> context = this.createApplicationContext();
   // AnnotationConfigServletWebServerApplicationContext extends ServletWebServerApplicationContext implements AnnotationConfigRegistry

-> AbstractApplicationContext.refresh()

-> AbstractApplicationContext.invokeBeanFactoryPostProcessors(beanFactory)
    -> ServletWebServerApplicationContext.postProcessBeanFactory
    -> WebApplicationContextUtils.registerWebApplicationScopes(this.getBeanFactory());
    -> beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());  // request 自动注入
    -> beanFactory.registerResolvableDependency(ServletResponse.class, new ResponseObjectFactory());

-> PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
-> BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry(registry);
-> ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
-> .processConfigBeanDefinitions(BeanDefinitionRegistry registry)

-> ConfigurationClassParser parser = new ConfigurationClassParser(...);
-> ConfigurationClassParser.parse(Set<BeanDefinitionHolder> configCandidates)
-> .processConfigurationClass(ConfigurationClass configClass) throws IOException
-> .doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
-> .processMemberClasses(configClass, sourceClass);
-> .processPropertySource(propertySource);
-> this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
-> .processImports(configClass, sourceClass, getImports(sourceClass), true)
-> .processInterfaces(configClass, sourceClass);

-> new ClassPathBeanDefinitionScanner(this.registry, ...);
-> ClassPathBeanDefinitionScanner.doScan(String... basePackages)
-> ClassPathScanningCandidateComponentProvider.findCandidateComponents(String basePackage)
-> .scanCandidateComponents(String basePackage)
-> .isCandidateComponent(MetadataReader metadataReader) throws IOException

-> this.reader = new ConfigurationClassBeanDefinitionReader(...);
-> this.reader.loadBeanDefinitions(configClasses);
-> .loadBeanDefinitionsForConfigurationClass(configClass, trackedConditionEvaluator);
-> .loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());

-> registerBeanPostProcessors(beanFactory);
-> finishBeanFactoryInitialization(beanFactory);  // AbstractApplicationContext
-> beanFactory.preInstantiateSingletons();  // DefaultListableBeanFactory
-> getBean(beanName);  // DefaultListableBeanFactory（Bean 加载的生命周期）
```

---

## getBean 处理过程

1. `Object sharedInstance = getSingleton(beanName);` 先获取一次
2. `markBeanAsCreated(beanName);` 放到已经创建的列表，做标记用
3. `getSingleton(String beanName, ObjectFactory<?> singletonFactory);` 再获取一次，没有的话调用 `singletonFactory.getObject()` 创建
4. `createBean(beanName, mbd, args);` 创建的具体实现
5. `doCreateBean(beanName, mbdToUse, args);` 真正的创建方法
6. `populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw);` 属性填充，触发 `InstantiationAwareBeanPostProcessor` 中 `postProcessPropertyValues` 的调用
7. `initializeBean(beanName, exposedObject, mbd);` 初始化：
   1. 触发 `BeanPostProcessor` 的 `postProcessBeforeInitialization`
   2. 触发 `InitializingBean` 的 `afterPropertiesSet`
   3. 触发 `BeanPostProcessor` 的 `postProcessAfterInitialization`

---

## 循环依赖解决

Spring 通过三级缓存解决单例 Bean 的循环依赖问题：

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // Quick check for existing instance without full singleton lock
    // 一级缓存：存放完全初始化好的单例 bean
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 二级缓存：存放早期暴露的半成品 bean（已实例化未完成依赖注入）
        singletonObject = this.earlySingletonObjects.get(beanName);
        if (singletonObject == null && allowEarlyReference) {
            synchronized (this.singletonObjects) {
                // Consistent creation of early reference within full singleton lock
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    singletonObject = this.earlySingletonObjects.get(beanName);
                    if (singletonObject == null) {
                        // 三级缓存：存放 ObjectFactory，用于生成早期引用
                        ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                        if (singletonFactory != null) {
                            singletonObject = singletonFactory.getObject();
                            // 把自己暴露在二级缓存中
                            this.earlySingletonObjects.put(beanName, singletonObject);
                            // 从三级缓存中删除
                            this.singletonFactories.remove(beanName);
                        }
                    }
                }
            }
        }
    }
    return singletonObject;
}
```

**三级缓存说明：**

| 缓存                   | 名称                | 作用                                   |
| ---------------------- | ------------------- | -------------------------------------- |
| singletonObjects       | 一级缓存            | 存放完全初始化好的单例 bean            |
| earlySingletonObjects  | 二级缓存            | 存放早期暴露的半成品 bean（提前暴露的代理对象） |
| singletonFactories     | 三级缓存            | 存放 ObjectFactory，用于生成早期引用   |

> 对象实例化后缓存一份 key 为 beanName 的 ObjectFactory，ObjectFactory 中的 `getObject` 方法返回当前创建对象的引用（可能被 AOP 等后处理器增强为代理对象）。

---

## 相关模块索引

- [Bean 生命周期](./spring-bean.md)
- [自动配置](./spring-autoconfig.md)
- [AOP](./spring-aop.md)
- [MVC](./spring-mvc.md)
- [JPA](./spring-jpa.md)
- [MyBatis](./spring-mybatis.md)
- [事务](./spring-transaction.md)
- [高级使用技巧](./spring-tips.md)

> 参考文章：
> - [Spring refresh()方法流程梳理 - 掘金](https://juejin.cn/post/7068978775425286181)
> - [@ComponentScan 源码分析](https://blog.csdn.net/qq_20597727/article/details/82713306)
