# Spring Tools 工具类

> 泛型解析与 ObjectProvider 等 Spring 核心工具类的使用笔记。

## 泛型类型解析

Spring 提供了在运行时解析泛型类型信息的工具，详见 [ResolvableTypeProvider 与泛型解析](./spring-resolvable-type.md)：

- `org.springframework.core.GenericTypeResolver#resolveTypeArguments` —— 从类继承结构中解析泛型接口的实际类型参数。
- `ResolvableTypeProvider` —— 让对象主动提供完整的 `ResolvableType`。

## ObjectProvider

```java
ObjectProvider<xxx>
```

`ObjectProvider` 是 `ObjectFactory` 的子接口，用于延迟、可选地获取 Bean，常配合泛型注入使用。