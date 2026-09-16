## AOP

```java
// 自调用时（this.） 手动获取代理
AopContext.currentProxy()
```

```java

/**
* ClassFilters : 多个组合工具
*/
@FunctionalInterface
public interface ClassFilter {

	boolean matches(Class<?> clazz);
	ClassFilter TRUE = TrueClassFilter.INSTANCE;
}

// 常用 ：继承 StaticMethodMatcher
public interface MethodMatcher {

	boolean matches(Method method, Class<?> targetClass);
	boolean isRuntime();
	boolean matches(Method method, Class<?> targetClass, Object... args);
	MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;
}


// 常用： 继承 Pointcut， 实现 getClassFilter，getMethodMatcher 方法
public interface Pointcut {

	ClassFilter getClassFilter();

	MethodMatcher getMethodMatcher();

	Pointcut TRUE = TruePointcut.INSTANCE;
}


// ------------------------------------------------------------------------------------
public interface Advice {

}
public interface Interceptor extends Advice {
}

@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

	Object invoke(MethodInvocation invocation) throws Throwable;

}


// --------------------------------------------------------------------------------------
public interface Advisor {

	Advice getAdvice();
	boolean isPerInstance();

	Advice EMPTY_ADVICE = new Advice() {};
}

// 常用： public abstract class AbstractPointcutAdvisor implements PointcutAdvisor, Ordered, Serializable {}， 带有排序功能
public interface PointcutAdvisor extends Advisor {

	Pointcut getPointcut();
}

public abstract class AbstractPointcutAdvisor implements PointcutAdvisor, Ordered, Serializable {

	@Nullable
	private Integer order;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		if (this.order != null) {
			return this.order;
		}
		Advice advice = getAdvice();
		if (advice instanceof Ordered) {
			return ((Ordered) advice).getOrder();
		}
		return Ordered.LOWEST_PRECEDENCE;
	}
}

```



```java


// ProxyFactoryBean  源码解析
->  ProxyFactoryBean.getObject() throws BeansException {
		// 初始化Advisor链
		initializeAdvisorChain();
		// 获取真正的代理对象
		if (isSingleton()) {
			return getSingletonInstance(); // getProxy(createAopProxy()) : AopProxyFactory -> AopProxy -> Prxoy
		}
		else {
			if (this.targetName == null) {
				logger.warn("Using non-singleton proxies with singleton targets is often undesirable. " +
						"Enable prototype proxies by setting the 'targetName' property.");
			}
			return newPrototypeInstance();
		}
	}
// addAdvisorOnChainCreation来执行添加操作。
// 其中重要的是将根据name从BeanFactory中获得的对象转换成Advisor，
// 因为interceptorNames支持Advisor，Advice，MethodInterceptor多种类型的对象name，
// 通过namedBeanToAdvisor方法统一转成Advisor类型。
->  advice = this.beanFactory.getBean(name);
->  addAdvisorOnChainCreation(advice, name);
->  Advisor advisor = namedBeanToAdvisor(next);
->  this.advisorAdapterRegistry.wrap(next);
->  DefaultAdvisorAdapterRegistry.wrap(Object adviceObject) throws UnknownAdviceTypeException {
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}
		Advice advice = (Advice) adviceObject;
		if (advice instanceof MethodInterceptor) {
			// So well-known it doesn't even need an adapter.
			return new DefaultPointcutAdvisor(advice);
		}
		for (AdvisorAdapter adapter : this.adapters) {
			// Check that it is supported.
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

// ------------------------------------------------------

// ProxyFactory源码解析
// AopProxyFactory -> AopProxy -> Prxoy
-> 	public Object getProxy() { return createAopProxy().getProxy(); }
->	return getAopProxyFactory().createAopProxy(this); // ProxyCreatorSupport() { this.aopProxyFactory = new DefaultAopProxyFactory(); }
->	DefaultAopProxyFactory.createAopProxy(AdvisedSupport config) throws AopConfigException {
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			...
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}
->  JdkDynamicAopProxy.getProxy() { return getProxy(ClassUtils.getDefaultClassLoader()); }
->  return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this); // this : JdkDynamicAopProxy implements InvocationHandler


// 	JdkDynamicAopProxy 回调解析
->  JdkDynamicAopProxy.invoke(Object proxy, Method method, Object[] args) throws Throwable {
		...
		// Get the interception chain for this method.
		// 获取拦截器链
		List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
		// 如果拦截器链为空，则直接反射调用
		if (chain.isEmpty()) {
			// We can skip creating a MethodInvocation: just invoke the target directly
			// Note that the final invoker must be an InvokerInterceptor so we know it does
			// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
			retVal = AopUtils.invokeJoinpointUsingReflection(target, method, args);
		}
		else {
			// We need to create a method invocation...
			// 生成MethodInvocation，进行链式调用
			invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
			// Proceed to the joinpoint through the interceptor chain.
			retVal = invocation.proceed();
		}

		// Massage return value if necessary.
		// 支持返回this的流式调用
		Class<?> returnType = method.getReturnType();
		if (retVal != null && retVal == target && returnType.isInstance(proxy) &&
				!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
			// Special case: it returned "this" and the return type of the method
			// is type-compatible. Note that we can't help if the target sets
			// a reference to itself in another returned object.
			retVal = proxy;
		}
		...
	}
->  this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
->	AdvisedSupport.getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
		...
		cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(this, method, targetClass);
		...
	}
->	DefaultAdvisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {
		...
		for (Advisor advisor : advisors) {
			...
			if (advisor instanceof PointcutAdvisor) {
				...
				MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
				...
			}
			...
		}
		...
		return interceptorList;
	}
-> DefaultAdvisorAdapterRegistry.getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<>(3);
		Advice advice = advisor.getAdvice();
		if (advice instanceof MethodInterceptor) {
			interceptors.add((MethodInterceptor) advice);
		}
		for (AdvisorAdapter adapter : this.adapters) { // MethodBeforeAdviceAdapter AfterReturningAdviceAdapter ThrowsAdviceAdapter
			if (adapter.supportsAdvice(advice)) {
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
		return interceptors.toArray(new MethodInterceptor[0]);
	}
->	MethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
->  retVal = invocation.proceed(); // 链式回调
->	ReflectiveMethodInvocation.proceed() throws Throwable {
		// We start with an index of -1 and increment early.
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			return invokeJoinpoint();
		}

		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// Evaluate dynamic method matcher here: static part will already have
			// been evaluated and found to match.
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
			if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
				return dm.interceptor.invoke(this);
			}
			else {
				// Dynamic matching failed.
				// Skip this interceptor and invoke the next in the chain.
				return proceed();
			}
		}
		else {
			// It's an interceptor, so we just invoke it: The pointcut will have
			// been evaluated statically before this object was constructed.
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this); // this.proceed() chain callback
		}



// 自动代理
// AopConfigUtils 创建 AbstractAdvisorAutoProxyCreator 实现类
// InfrastructureAdvisorAutoProxyCreator 和 AnnotationAwareAspectJAutoProxyCreator
/*
 *  自动代理的实现原理同ProxyFactoryBean中使用FactoryBean扩展不同，
 *	而是通过BeanPostProcessor扩展对Bean对象的创建过程进行控制来实现AOP代理。
 */
// SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor extends BeanPostProcessor

// BeanPostProcessor
// postProcessBeforeInitialization 初始化前扩展(执行init-method前)
// postProcessAfterInitialization 初始化后扩展(执行init-method后) *

// InstantiationAwareBeanPostProcessor
// postProcessBeforeInstantiation 对象实例化前扩展 *
// postProcessAfterInstantiation 对象实例化后扩展
// postProcessPropertyValues 属性依赖注入前扩展

// SmartInstantiationAwareBeanPostProcessor
// predictBeanType 预测bean的类型，在beanFactory的getType时被调用
// determineCandidateConstructors 对象实例化时决定要使用的构造函数时被调用
// getEarlyBeanReference 循环依赖处理时获取Early对象引用时被调用

// 而对于Spring AOP的自动代理，处理的阶段有两个，对象实例化前扩展和初始化后扩展。

-> AopAutoConfiguration
-> @EnableAspectJAutoProxy(proxyTargetClass = true)
-> @Import(AspectJAutoProxyRegistrar.class)
-> AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
-> AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);

// 自动代理基类AbstractAutoProxyCreator
-> postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {// 对象实例化前postProcessBeforeInstantiation
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// Create proxy here if we have a custom TargetSource.
		// Suppresses unnecessary default instantiation of the target bean:
		// The TargetSource will handle target instances in a custom fashion.
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		if (targetSource != null) {
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}
			// 获取拦截器
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			// 创建代理对象
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		return null;
	}
->	postProcessAfterInitialization(@Nullable Object bean, String beanName) {// 初始化后 postProcessAfterInitialization
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}
->	wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
		// 获取拦截器
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			// 创建代理对象
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}
```


