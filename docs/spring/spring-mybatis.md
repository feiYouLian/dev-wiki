## Mybatis

```java
-> @MapperScan
-> MapperScannerRegistrar.class (AutoConfiguredMapperScannerRegistrar.class)
-> BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);// MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor
-> registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
-> MapperScannerConfigurer.postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) // PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors -> invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
-> ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry); // ClassPathMapperScanner extends ClassPathBeanDefinitionScanner
-> scanner.scan(
        StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
-> ClassPathBeanDefinitionScanner.scan()
-> ClassPathMapperScanner.doScan()
-> ClassPathBeanDefinitionScanner.doScan()
-> ClassPathMapperScanner.processBeanDefinitions(beanDefinitions);
-> definition.setBeanClass(this.mapperFactoryBeanClass);
-> definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);// bean初始化时, BeanWrapper 设置

-> finishBeanFactoryInitialization(beanFactory); //AbstractApplicationContext
-> beanFactory.preInstantiateSingletons(); // DefaultListableBeanFactory
-> if (isFactoryBean(beanName)) {} // DefaultListableBeanFactory  省略了Bean 加载的生命周期

// MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {}
// SqlSessionDaoSupport extends DaoSupport {}
// DaoSupport implements InitializingBean {}
-> DaoSupport.afterPropertiesSet() // FactoryBean 创建完, 后置属性初始化
-> MapperFactoryBean.checkDaoConfig()
-> Configuration configuration = getSqlSession() // SqlSessionTemplate implements SqlSession, DisposableBean{}
	.getConfiguration(); // this.sqlSessionFactory.getConfiguration() form=> MybatisAutoConfiguration.SqlSessionFactoryBean.setConfiguration
-> Configuration.addMapper(this.mapperInterface); //
-> MapperRegistry.addMapper(type);
-> knownMappers.put(type, new MapperProxyFactory<>(type)); // getMapper: mapperProxyFactory.newInstance(sqlSession);
-> MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
-> parser.parse(); // MapperAnnotationBuilder 解析SQL

// mapperProxy 创建
-> MapperFactoryBean.getObject();
-> getSqlSession().getMapper(this.mapperInterface);
-> getConfiguration().getMapper(type, this);
-> mapperRegistry.getMapper(type, sqlSession);
-> final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
-> mapperProxyFactory.newInstance(sqlSession);
-> final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache); // Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<>();
-> Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);

// execute mapperInterface.method
-> MapperProxy.invoke(Object proxy, Method method, Object[] args)
-> final MapperMethod mapperMethod = cachedMapperMethod(method);
-> methodCache.computeIfAbsent(method,
        k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration())); // 绑定 method 和 sql
-> mapperMethod.execute(sqlSession, args); // sqlSession: SqlSessionTemplate
-> sqlSession.xxx(command.getName(), param)// xxx: insert,update,delete,select...
-> sqlSession.sqlSessionProxy.selectList(statement, parameter, rowBounds); // sqlSessionProxy: newProxyInstance(SqlSessionFactory.class.getClassLoader(), new Class[] { SqlSession.class }, new SqlSessionInterceptor());
-> SqlSessionInterceptor.invoke(Object proxy, Method method, Object[] args)
-> SqlSession sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory,
          SqlSessionTemplate.this.executorType, SqlSessionTemplate.this.exceptionTranslator);
-> SqlSession session = sessionHolder(executorType, holder); // 从事务资源中获取 session
-> if (session != null) { return session; }
-> session = sessionFactory.openSession(executorType); // session: final Executor executor = configuration.newExecutor(tx, execType);  new DefaultSqlSession(configuration, executor, autoCommit);
-> registerSessionHolder(sessionFactory, executorType, exceptionTranslator, session); // 将session保存到事务资源中
-> Object result = method.invoke(sqlSession, args); // SqlSessionInterceptor
-> DefaultSqlSession.xxx() // xxx: insert,update,delete,select...
-> MappedStatement ms = configuration.getMappedStatement(statement);
-> return executor.update(ms, wrapCollection(parameter)); // 查询: executor.query(ms, wrapCollection(parameter), rowBounds, handler);
-> ResultSetHandler

// Executor 是总的执行者，他就像一个大总管，用于协调管理其他执行者。
// StatementHandler 是用于生成Statement或者PreparedStatement的执行者，
// 同时他会调用ParameterHandler进行对sql语句中的参数设值，设值完了之后会通过StatementHandler 去调用sql在数据库中执行，最后返回一个结果集，
// 通过ResultSetHandler将结果集和对应的实体进行映射填充数据，之后会把结果实体返回给StatementHandler


// AutoConfiguration
-> 	@Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		...
		 factory.setConfiguration(configuration); // Configuration
		...
        return factory.getObject();
	}

->  @Bean
    @ConditionalOnMissingBean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {}

->  @Configuration
    @Import(AutoConfiguredMapperScannerRegistrar.class)
    @ConditionalOnMissingBean({MapperFactoryBean.class, MapperScannerConfigurer.class})
    public static class MapperScannerRegistrarNotFoundConfiguration implements InitializingBean {}




// mybatis plus 重写
// 1. MybatisConfiguration extends Configuration
// 2. MybatisMapperRegistry extends MapperRegistry
// 3. MybatisMapperProxyFactory copy from MapperProxyFactory
// 4. MybatisMapperProxy copy from MapperProxy
// 5. MybatisMapperMethod  copy from MapperMethod
// 6. MybatisMapperAnnotationBuilder extends MapperAnnotationBuilder
```

[Mybatis3 详解系列](https://www.cnblogs.com/tanghaorong/tag/Mybatis3%E8%AF%A6%E8%A7%A3%E7%B3%BB%E5%88%97/)


## sqlsession、sqlsessionManager以及sqlsessionTemplate的理解


### sqlSession

是mybatis的核心操作类，其中对数据库的crud都封装在这个中，是一个顶级接口，其中默认实现类是DefaultSqlSession这个类，

```java
public interface SqlSession extends Closeable  {}
public class DefaultSqlSession implements SqlSession {}

```

```java
SqlSession session = null;
String resource = "configuration.xml";
// 使用io流读取配置
InputStream inputStream;
inputStream = Resources.getResourceAsStream(resource);
//这里是解析配置文件
sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
// 得到了一个会话，有了这个会话，你就可以对数据进行增，删，改，查的操作
session = sqlSessionFactory.openSession();
```

```java
//这个是org.apache.ibatis.session.defaults.DefaultSqlSessionFactory
@Override
  public SqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }
//之后走到该类的这个方法里来(openSessionFromDataSource)
  private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      final Environment environment = configuration.getEnvironment();
      //开始创建事物
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      //将事物传递给执行器Executor，这个是session执行数据库操作的核心(有三种执行器类型)
      final Executor executor = configuration.newExecutor(tx, execType);
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
```

```java

public class DefaultSqlSession implements SqlSession {
  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER); // SimpleExecutor
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
}

public class SimpleExecutor extends BaseExecutor implements Executor  {
    @Override
    public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Statement stmt = null;
        try {
        Configuration configuration = ms.getConfiguration();
        StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
        // 这里创建statement对象,这个方法中就用到了Connection连接对象，此时我们主要看这个方法中Connection的创建时怎么样的
        stmt = prepareStatement(handler, ms.getStatementLog());
        return handler.query(stmt, resultHandler); // SimpleStatementHandler
        } finally {
        closeStatement(stmt);
        }
    }

    private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
        Statement stmt;
        // 看下面的方法，此时只需要看这个方法
        Connection connection = getConnection(statementLog);
        stmt = handler.prepare(connection, transaction.getTimeout());
        handler.parameterize(stmt);
        return stmt;
    }

    protected Connection getConnection(Log statementLog) throws SQLException {
        Connection connection = transaction.getConnection();
        if (statementLog.isDebugEnabled()) {
        return ConnectionLogger.newInstance(connection, statementLog, queryStack);
        } else {
        return connection;
        }
    }
}

public class JdbcTransaction implements Transaction {
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null) {
        openConnection();
        }
        return connection;
    }
    // JdbcTransaction
    protected void openConnection() throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug("Opening JDBC Connection");
        }
        connection = dataSource.getConnection();
        if (level != null) {
            connection.setTransactionIsolation(level.getLevel());
        }
        setDesiredAutoCommit(autoCommit);
    }
    //  SpringManagedTransaction
    private void openConnection() throws SQLException {
        // DataSourceUtils 很重要 , 多数据源 注入 AbstractRoutingDataSource,  普通 注入 DruidDataSource 或 HikariDataSource
        this.connection = DataSourceUtils.getConnection(this.dataSource); 
        this.autoCommit = this.connection.getAutoCommit();
        this.isConnectionTransactional = DataSourceUtils.isConnectionTransactional(this.connection, this.dataSource);

        LOGGER.debug(() -> "JDBC Connection [" + this.connection + "] will"
            + (this.isConnectionTransactional ? " " : " not ") + "be managed by Spring");
    }
}

public class PreparedStatementHandler extends BaseStatementHandler implements StatementHandler  {
    @Override
    public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
        PreparedStatement ps = (PreparedStatement) statement; // Druid 或 Hikari 的实现
        ps.execute();
        return resultSetHandler.handleResultSets(ps);
    }
}


```

```java

// jdbc Statement 的实现类 DruidPooledStatement
public class DruidPooledStatement extends PoolableWrapper implements Statement{

    @Override
    public final boolean execute(String sql) throws SQLException {
        checkOpen();

        incrementExecuteCount();
        transactionRecord(sql);

        try {
            return stmt.execute(sql);
        } catch (Throwable t) {
            errorCheck(t);

            throw checkException(t, sql);
        }
    }
}
```
最终可以看出一次SqlSession的执行最终只会产生一个connection，所以我们设想一下，在两个线程通过同一个sqlsession来执行crud，那么就有可能，我先跑完的线程，把唯一的这一个连接给关闭掉，从而造成另一条线程的逻辑不被成功执行，所以通过DefaultSqlSession来执行数据库操作是线程不安全的。


### sqlsessionTemplate

DefaultSqlSession来执行数据库操作是线程不安全的。
为什么说sqlsessionTemplate是线程安全的？

```java


  public class SqlSessionTemplate implements SqlSession, DisposableBean {

  private final SqlSessionFactory sqlSessionFactory;

  private final ExecutorType executorType;

  private final SqlSession sqlSessionProxy;

  private final PersistenceExceptionTranslator exceptionTranslator;

  private final PersistenceExceptionTranslator exceptionTranslator;
  
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
  }
  ..........
      
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
  PersistenceExceptionTranslator exceptionTranslator) {
  notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
  notNull(executorType, "Property 'executorType' is required");

  this.sqlSessionFactory = sqlSessionFactory;
  this.executorType = executorType;
  this.exceptionTranslator = exceptionTranslator;
  this.sqlSessionProxy = (SqlSession) newProxyInstance(SqlSessionFactory.class.getClassLoader(),
      new Class[] { SqlSession.class }, new SqlSessionInterceptor());
    }


    private class SqlSessionInterceptor implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 获取新的 sqlSession 
        // SqlSessionUtils.getSqlSession()---->  DefaultSqlSessionFactory.openSession() ----->new DefaultSqlSession
        SqlSession sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory,
            SqlSessionTemplate.this.executorType, SqlSessionTemplate.this.exceptionTranslator);
        try {
            Object result = method.invoke(sqlSession, args);
            if (!isSqlSessionTransactional(sqlSession, SqlSessionTemplate.this.sqlSessionFactory)) {
            // force commit even on non-dirty sessions because some databases require
            // a commit/rollback before calling close()
            sqlSession.commit(true);
            }
            return result;
        } catch (Throwable t) {
            Throwable unwrapped = unwrapThrowable(t);
            if (SqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
            // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
            closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
            sqlSession = null;
            Throwable translated = SqlSessionTemplate.this.exceptionTranslator
                .translateExceptionIfPossible((PersistenceException) unwrapped);
            if (translated != null) {
                unwrapped = translated;
            }
            }
            throw unwrapped;
        } finally {
            if (sqlSession != null) {
            closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
            }
        }
        }
    }
  }



public final class SqlSessionUtils {
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType,
      PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    notNull(executorType, NO_EXECUTOR_TYPE_SPECIFIED);

    SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

    SqlSession session = sessionHolder(executorType, holder);
    if (session != null) {
      return session;
    }

    LOGGER.debug(() -> "Creating a new SqlSession");
    // DefaultSqlSessionFactory 或  SqlSessionManager
    session = sessionFactory.openSession(executorType);

    registerSessionHolder(sessionFactory, executorType, exceptionTranslator, session);

    return session;
  }
}


public class DefaultSqlSessionFactory implements SqlSessionFactory {

  @Override
  public SqlSession openSession(ExecutorType execType) {
    return openSessionFromDataSource(execType, null, false);
  }
  
  private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      final Environment environment = configuration.getEnvironment();
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      final Executor executor = configuration.newExecutor(tx, execType);
      //  DefaultSqlSession
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

}


```
从这个构造方法可以看出，sqlsessionTemplate传参是必须需要一个sqlsessionfactory的，sqlsessionTemplate在执行crud操作时，都不是通过唯一的一个sqlsession来执行的，他都是通过动态代理来执行具体的操作的，所以多个线程持有同一个sqlsessionTemplate是不会产生线程安全问题的。


### sqlSessionManager  



可以看出他的一个必要的参数也是sqlsessionFactory，SqlSessionManager既实现了SqlSessionFactory，也实现了SqlSession，具备生产SqlSession的能力，也具备SqlSession的能力，SqlSession的作用是执行具体的Sql语句。

```java
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

  private final SqlSessionFactory sqlSessionFactory;
  private final SqlSession sqlSessionProxy;

  private final ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<>();
  private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSession.class},
        new SqlSessionInterceptor());
  }
  .....
  
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get(); // threadLocal线程副本,解决线程安全问题
      if (sqlSession != null) {
        try {
          return method.invoke(sqlSession, args);
        } catch (Throwable t) {
          throw ExceptionUtil.unwrapThrowable(t);
        }
      } else {
        try (SqlSession autoSqlSession = openSession()) {// 解决自动关闭Session问题
          try {
            final Object result = method.invoke(autoSqlSession, args);
            autoSqlSession.commit();
            return result;
          } catch (Throwable t) {
            autoSqlSession.rollback();
            throw ExceptionUtil.unwrapThrowable(t);
          }
        }
      }
    }
  }

}
```

### 总结
DefaultSqlSession 
0. DefaultSqlSession 是SqlSession的默认实现。来自 Mybatis
1. 我们需要自己手动关闭sqlsesion，我们知道，人总是不可靠的。忘关sqlsession 是有很大概率发生的
2. 线程安全问题：DefaultSqlSession是线程不安全的Sqlsession 。也就是说DefaultSqlSession不能是单例，

SqlSessionManager
0. Mybatis为我们提供了升级版的DefaultSqlSession 
1. 解决自动关闭Session问题
2. 解决线程安全问题
3. 具备 sqlsessionFactory 的能力，可以生成 SqlSession

SqlSessionTemplate
1. SqlSessionTemplate 是Mybatis与Spring 整合时的  线程安全sqlsession 和 自动关闭session

