# Spring 框架笔记

## 目录

### 核心机制

- [启动流程（refresh 方法）](./spring-boot.md) — Spring 容器初始化全流程梳理
- [Bean 生命周期](./spring-bean.md) — Bean 的创建、初始化、销毁全过程
- [BeanFactory 扩展（BeanFactoryPostProcessor）](./spring-beanFactory.md) — Bean 定义加载后的扩展机制
- [自动配置](./spring-autoconfig.md) — Spring Boot 自动装配原理
- [自动执行扩展](./spring-auto-exec.md) — 启动时自动执行的多种方式及顺序

### AOP 与事务

- [Spring AOP 源码笔记](./spring-aop.md) — AOP 核心 API 与源码调用链
- [事务管理](./spring-transaction.md) — 声明式事务源码解析

### Web 与数据访问

- [Spring MVC](./spring-mvc.md) — 调用流程、参数处理、静态资源、过滤器
- [Spring JPA](./spring-jpa.md) — JPA 加载流程
- [Spring MyBatis](./spring-mybatis.md) — MyBatis 整合原理与 SqlSession 线程安全
- [Spring JDBC](./spring-jdbc.md) — Druid 连接池配置参考

### 其他

- [Resource 资源加载](./spring-resource.md) — ResourceLoader 与 ResourcePatternResolver
- [Template 模板类](./spring-template.md) — JdbcTemplate 体系
- [Tools 工具类](./spring-tools.md) — 泛型解析、ObjectProvider 等
- [高级使用技巧](./spring-tips.md) — Spring 中实用的代码技巧
- [高级使用（占位）](./spring-usage.md)

> 相关 Java 基础与 AOP 源码详解见 [Java 学习笔记](/java/)
