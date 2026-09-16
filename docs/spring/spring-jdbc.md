

``` yml
      initial-size: 5 #指定连接池初始化时预创建的物理连接数量。
      min-idle: 5 #确保连接池中始终保留的最小空闲连接数，防止低负载时连接被过度回收
      max-active: 50 #限制连接池同时存在的最大活跃连接数（包括使用中和空闲连接）
      max-wait: 600000 #10分钟当连接池耗尽时，新请求等待可用连接的最长时间
      connect-timeout: 30000 # 30秒连接超时
      socket-timeout: 7200000 # 2小时秒SQL执行超时
      validation-query: select 1
      keep-alive: true       # 保活
      phy-timeout-millis: 1800000 # 30分钟连接存活时间
      test-on-borrow: false  # 关闭获取实时验证
      test-while-idle: true  #  空闲验证
      test-on-return: false  # 关闭 放回实时验证
      keep-alive-between-time-millis: 60000    # 60秒一次健康检查  SELECT 1
      time-between-eviction-runs-millis: 100000 # 100秒执行一次驱逐检测
      min-evictable-idle-time-millis: 280000    # 空闲4分40秒后驱逐
```