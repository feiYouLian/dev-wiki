## Transaction

```java
-> TransactionAutoConfiguration
-> EnableTransactionManagementConfiguration
   @EnableTransactionManagement(proxyTargetClass = false)
-> JdkDynamicAutoProxyConfiguration
-> TransactionManagementConfigurationSelector // extends AdviceModeImportSelector<EnableTransactionManagement> {}
-> selectImports(AdviceMode adviceMode){
	...
	return new String[] {AutoProxyRegistrar.class.getName(), // implements ImportBeanDefinitionRegistrar
						ProxyTransactionManagementConfiguration.class.getName()};
	...
}
-> AutoProxyRegistrar.registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
	...
	AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);// 注册自动代理创建器
	...
}
// InfrastructureAdvisorAutoProxyCreator
// extends AbstractAdvisorAutoProxyCreator
// extends AbstractAutoProxyCreator  *AOP*
// extends ProxyProcessorSupport
// implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware

// SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor extends BeanPostProcessor

-> registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
-> AbstractAutoProxyCreator.postProcessAfterInitialization(@Nullable Object bean, String beanName) { // implements BeanPostProcessor
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				// 如果满足条件对bean进行包裹
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}
-> Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null); // AbstractAdvisorAutoProxyCreator
-> List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName); // 查找符合条件的切面
-> AbstractAdvisorAutoProxyCreator.findEligibleAdvisors(Class<?> beanClass, String beanName) {
    // 获取所有候选的切面，也就是类型为Advisor的切面，此处获取到的候选切面为BeanFactoryTransactionAttributeSourceAdvisor
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    // 从候选的切面中获取可以解析当前bean的切面，最终符合条件的切面为BeanFactoryTransactionAttributeSourceAdvisor
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    extendAdvisors(eligibleAdvisors);
    if (!eligibleAdvisors.isEmpty()) {
    	eligibleAdvisors = sortAdvisors(eligibleAdvisors);
    }
    return eligibleAdvisors;
}
-> AbstractAdvisorAutoProxyCreator.findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
		...
		return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		...
	}
-> AopUtils.findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
		...
		// 当前切面是否可以解析bean
		if (canApply(candidate, clazz, hasIntroductions)) {
			eligibleAdvisors.add(candidate);
		}
		...
	}
-> AopUtils.canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
		...
			PointcutAdvisor pca = (PointcutAdvisor) advisor; // BeanFactoryTransactionAttributeSourceAdvisor implements PointcutAdvisor
			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
		...
	}
-> AopUtils.canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
		...
		if (introductionAwareMethodMatcher != null ?
				introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
				methodMatcher.matches(method, targetClass) // 匹配方法是否符合
				) {
			return true;
		}
		...
	}
-> TransactionAttributeSourcePointcut.matches(Method method, Class<?> targetClass) { // BeanFactoryTransactionAttributeSourceAdvisor.pointcut
		TransactionAttributeSource tas = getTransactionAttributeSource();
		return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
	}
-> AbstractAdvisorAutoProxyCreator.wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		...
		// Create proxy if we have advice.
		// Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));// 省略创建AOP过程和回调过程
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}
		...
	}

-> ProxyTransactionManagementConfiguration // TransactionManagementConfigurationSelector 引入

	// 创建BeanFactoryTransactionAttributeSourceAdvisor
	// @Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	// @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
->  public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor() {
	...
	advisor.setTransactionAttributeSource(transactionAttributeSource());
	// 设置切面对应的通知，后面分析会用到
	advisor.setAdvice(transactionInterceptor());
	...
}


-> TransactionInterceptor.invoke(MethodInvocation invocation)
-> TransactionAspectSupport.invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed){
	...
	// Standard transaction demarcation with getTransaction and commit/rollback calls.
		TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);
		//-> PlatformTransactionManager.getTransaction(txAttr);
		//-> AbstractPlatformTransactionManager.getTransaction
		//-> prepareSynchronization(status, def);
		//-> TransactionSynchronizationManager.initSynchronization();

		Object retVal;
		try {
			// This is an around advice: Invoke the next interceptor in the chain.
			// This will normally result in a target object being invoked.
			retVal = invocation.proceedWithInvocation();
		}
		catch (Throwable ex) {
			// target invocation exception
			completeTransactionAfterThrowing(txInfo, ex);
			throw ex;
		}
		finally {
			cleanupTransactionInfo(txInfo);
		}

		if (vavrPresent && VavrDelegate.isVavrTry(retVal)) {
			// Set rollback-only in case of Vavr failure matching our rollback rules...
			TransactionStatus status = txInfo.getTransactionStatus();
			if (status != null && txAttr != null) {
				retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
			}
		}

		commitTransactionAfterReturning(txInfo);
		return retVal;
}


// 1. 导入AutoProxyRegistrar、ProxyTransactionManagementConfiguration配置类
// 2. AutoProxyRegistrar用来注册InfrastructureAdvisorAutoProxyCreator到IOC中，InfrastructureAdvisorAutoProxyCreator实现了BeanPostProcessor
// 3. 执行BeanPostProcessor的后置处理
// 4. 获取由ProxyTransactionManagementConfiguration配置类创建的切面
// 5. 通过切面解析bean是否需要创建代理，需要就创建代理
// 6. 执行代理的回调，在回调中拿到通知执行通知，
// 7. 通知里面逻辑：开启事务、执行目标方法、提交或回滚事务


```
