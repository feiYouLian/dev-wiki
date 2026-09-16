# BeanFactory 扩展（BeanFactoryPostProcessor）

> 来源：Spring 的 BeanFactoryPostProcessor 接口详解。本文由原 HTML 文件整理转换而来。

## 目录

- [BeanFactoryPostProcessor 作用](#beanfactorypostprocessor-作用)
- [重要实现](#重要实现)
- [执行时机](#执行时机)
- [扩展举例](#扩展举例)
- [使用建议及注意事项](#使用建议及注意事项)

---

## BeanFactoryPostProcessor 作用

BeanFactoryPostProcessor 是 Spring 框架中的重要接口，用于在 BeanFactory 加载 Bean 定义之后、实例化 Bean 之前对 BeanFactory 进行自定义修改和扩展。它允许开发人员在 Spring 容器加载配置文件并创建 Bean 实例之前对 Bean 定义进行操作。

接口定义：

```java
void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
```

参数 `ConfigurableListableBeanFactory` 是 Spring 容器的核心接口，也是 BeanFactory 的子接口。它可以通过继承自 BeanFactory 的方法获取容器中所有的 Bean，并支持像 BeanPostProcessor 这样的接口。其唯一实现类是 `DefaultListableBeanFactory`。

通过实现 BeanFactoryPostProcessor 接口，可以实现以下功能：

- **修改 Bean 定义**：动态修改 Bean 的属性值、更改 Bean 的作用域等。
- **注册新的 Bean 定义**：动态向 Spring 容器中添加新的 Bean 定义，实现动态扩展。
- **添加自定义元数据**：向 Bean 定义中添加自定义的元数据，供后续处理器使用。

> 注意：BeanFactoryPostProcessor 的实现类必须在 Spring 容器启动之前被注册到容器中。

---

## 重要实现

- **BeanDefinitionRegistryPostProcessor 接口**：BeanFactoryPostProcessor 的一个非常重要的子接口。
- **PropertySourcesPlaceholderConfigurer**：用 properties 文件的配置值替换 xml 文件中的占位符。
- **CustomEditorConfigurer**：实现类型转换。

---

## 执行时机

在 `AbstractApplicationContext#refresh` 的 **步骤5：invokeBeanFactoryPostProcessors** 中执行，由 `PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors` 完成详细流程。

### 处理 BeanDefinitionRegistryPostProcessor 接口

执行顺序：PriorityOrdered >> Ordered >> 其他

> 重要：**ConfigurationClassPostProcessor 是 Spring 框架中唯一实现 BeanDefinitionRegistryPostProcessor 接口的实现类**，它会扫描相关的配置类，将对应的 Bean Definition 注册到 BeanFactory 中。

```java
List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
// 收集所有BeanDefinitionRegistryPostProcessor的Bean名称
String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
for (String ppName : postProcessorNames) {
    if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
        currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
        processedBeans.add(ppName);
    }
}
// 排序
sortPostProcessors(currentRegistryProcessors, beanFactory);
registryProcessors.addAll(currentRegistryProcessors);
// 触发调用每个 BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry 方法
invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
currentRegistryProcessors.clear();
```

### 处理 BeanFactoryPostProcessor 接口

执行顺序：同样按 PriorityOrdered >> Ordered >> 其他 分三次执行。

---

## 扩展举例

### 注册新的 Bean 定义

```java
public class AdditionalBean {
    private String message;
    public void setMessage(String message) { this.message = message; }
    public String getMessage() { return message; }
    public void displayMessage() { System.out.println("Additional Bean"); }
}

@Component
public class MyBeanFactoryPostProcessorForNewDefinitionReg implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(AdditionalBean.class).getBeanDefinition();
        beanDefinitionRegistry.registerBeanDefinition("additionalBean", beanDefinition);
    }
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        // 这里留空
    }
}
```

### 修改 Bean 定义

```java
@Component
public class MyBeanFactoryPostProcessorForUpdateBean implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition("myTestMsgBean");
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        propertyValues.add("message", "Hello, World!");
    }
}
```

### 添加自定义元数据

```java
@Component
public class MyBeanFactoryPostProcessorForDefineMata implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition("additionalBean");
        beanDefinition.setAttribute("customData", "This is custom data");
        // 获取的时候使用 BeanDefinition 对应的 getAttribute() 方法获取
    }
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        // 这里留空
    }
}
```

---

## 使用建议及注意事项

1. BeanFactoryPostProcessor 在 BeanFactory 加载 Bean 定义后执行，但在 Bean 实例化之前执行。可以修改 Bean 定义的属性，但无法修改 Bean 实例的状态。如需在 Bean 实例化后修改，考虑使用 BeanPostProcessor 接口。
2. 避免在 BeanFactoryPostProcessor 中创建循环依赖关系。
3. 尽量避免在 BeanFactoryPostProcessor 中引入复杂的业务逻辑。
4. 注意 BeanFactoryPostProcessor 的执行顺序。可以使用 `@Order` 注解或实现 `Ordered` 接口来指定执行顺序。
5. 谨慎使用 BeanFactoryPostProcessor 注册新的 Bean 定义。
6. 建议遵循单一职责原则。每个 BeanFactoryPostProcessor 类应该专注于一个特定的任务或功能。
7. 注意 BeanFactoryPostProcessor 的生命周期。确保执行时间较短，以避免影响应用程序的启动性能。
