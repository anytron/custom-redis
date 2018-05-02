# custom-redis

`custom-redis`提供了多种访问`redis`的方式，具体使用方式参考如下:

依赖jar包:<br>
`custom-redis.jar`<br>
`commons-attributes-api-2.2.jar`<br>
`commons-beanutils-1.8.3.jar`<br>
`commons-collections-3.2.1.jar`<br>
`commons-configuration-1.5.jar`<br>
`commons-io-2.1.jar`<br>
`commons-lang-2.4.jar`<br>
`commons-logging-1.1.1.jar`<br>
`commons-pool-1.3.jar`<br>
`jedis-2.1.0.jar`<br>
`log4j-1.2.16.jar`<br>
或更新的版本jar包,但值得注意的是比如`jedis`最新jar包有方法变动,需要更改代码<br>
依赖配置文件:

	classpath:/custom-redis.xml
使用代码:

	cloudyCart指应用名,具体解释参考custom-redis.xml解释部分
	ICustomRedis是jedis的封装,通过ICustomRedis可以执行redis的所有方法
```Java
	CustomRedisFactory f = CustomRedisFactory.getInstance();
	ICustomRedis redis1 = f.getCustomRedisByAppId("cloudyCart","");
	redis1.set("key1", "value001");
```
配置文件`custom-redis.xml`说明:

	custom-redis按照应用为单位管理redis
	可以在一个custom-redis.xml文件中配置多个应用
	每个应用对应了多种方式实现redis集群的管理
配置文件分成3个部分

	1、<appnode></appnode>以应用为单位配置redis集群，一个配置文件中可以配置多个应用，一个应用可以配置多个集群节点，具体请参考
	2、<clusternode></clusternode>配置各个集群，一个集群可以按照集群类型配置多个redis节点
	3、<servernode></servernode>配置所有需要的redis节点，每个节点配置。
配置文件示例:

	<?xml version="1.0" encoding="UTF-8"?>
		<redisrouting>	
			<appnode>
				<!—- <appnode>下配置多个应用 -->
				<anode>
					<!—- id=应用名对应示例java代码中的cloudyCart -->
					<id>cloudyCart</id>
				    <!-- clusternodes 配置多个cluster,多个用竖线分割 -->
					<clusternodes>cluster1|cluster2|cluster3</clusternodes>
					<!—- dispatch 暂时无意义-->
					<dispatch>ROUTING</dispatch>
					<!-- TRAILNUMBER尾号 ONLY只读第一个 HASH一致性哈希算法-->
					<clusterstrategy>TRAILNUMBER</clusterstrategy>
					<!-- 是否监控 默认为false 只有=true monitorinterval才有效 -->
					<ismonitor>true</ismonitor>
					<!-- 监控间隔毫秒 -->
					<monitorinterval>2000</monitorinterval>
				</anode>
				<!—- 第二个应用配置-->
				<anode>
					<id>degrade</id>
					<!-- 多个cluster必须用竖线分割,cluster1对应clusternode中配置的id -->
					<clusternodes>cluster1</clusternodes>
					<dispatch>ROUTING</dispatch>
					<clusterstrategy>ONLY</clusterstrategy>
					<!-- 是否监控 默认为false 只有=true monitorinterval才有效 -->
					<ismonitor>true</ismonitor>
					<!-- 间隔毫秒 -->
					<monitorinterval>10000</monitorinterval>
				</anode>
				<!—- 第三个应用配置-->
				<anode>
					<id>other</id>
					<clusternodes>cluster4</clusternodes>
					<dispatch>ROUTING</dispatch>
					<clusterstrategy>ONLY</clusterstrategy>
					<ismonitor>true</ismonitor>
					<monitorinterval>10000</monitorinterval>
				</anode>
			</appnode>
			<!—- redis cluster配置 -->
			<clusternode>
				<cnode>
					<id>cluster1</id>
					<readstrategy>HASHREAD</readstrategy>
					<readservernodes>snode1</readservernodes>
					<writerstrategy>MUTIWRITE</writerstrategy>
					<writeservernodes>snode1</writeservernodes>
					<!-- 该节点对应的尾号,当appnode->anode->clusterstrategy=TRAILNUMBER时才有效 -->
					<trailnumber>0-33</trailnumber>
				</cnode>
				<cnode>
					<id>cluster2</id>
					<!-- 读策略 目前支持 HASHREAD RANDOM-->
					<readstrategy>HASHREAD</readstrategy>
					<readservernodes>snode2</readservernodes>
					<!-- 写策略,写必须是多写 -->
					<writerstrategy>MUTIWRITE</writerstrategy>
					<writeservernodes>snode2</writeservernodes>
					<trailnumber>34-66</trailnumber>
				</cnode>
				<cnode>
					<id>cluster3</id>
					<!-- 读策略 目前支持 HASHREAD RANDOM-->
					<readstrategy>HASHREAD</readstrategy>
					<readservernodes>snode3</readservernodes>
					<!-- 写策略,写必须是多写 -->
					<writerstrategy>MUTIWRITE</writerstrategy>
					<writeservernodes>snode3</writeservernodes>
					<trailnumber>67-99</trailnumber>
				</cnode>
				<cnode>
					<id>cluster4</id>
					<!-- 读策略 目前支持 HASHREAD RANDOM-->
					<readstrategy>HASHREAD</readstrategy>
					<readservernodes>snode4</readservernodes>
					<!-- 写策略,写必须是多写 -->
					<writerstrategy>MUTIWRITE</writerstrategy>
					<writeservernodes>snode4</writeservernodes>
				</cnode>
			</clusternode>
			<!-- servernode 对应每一台需要用到的redis服务器 -->
			<servernode>
				<node>
					<id>snode1</id>
					<maxActive>200</maxActive><!-- 最大的活动连接 -->
					<maxIdle>100</maxIdle><!-- 最大的空闲连接 -->
					<maxWait>1000</maxWait><!-- 最大的等待时间 -->
					<host>10.1.200.77</host><port>6379</port><!--Ip地址和端口 -->
				</node>
				<node>
					<id>snode2</id>
					<maxActive>200</maxActive>
					<maxIdle>100</maxIdle>
					<maxWait>1000</maxWait>
					<host>10.1.200.78</host><port>6379</port>
				</node>
				<node>
					<id>snode3</id>
					<maxActive>200</maxActive>
					<maxIdle>100</maxIdle>
					<maxWait>1000</maxWait>
					<host>10.1.200.84</host><port>6379</port>
				</node>
				<node>
					<id>snode4</id>
					<maxActive>200</maxActive>
					<maxIdle>100</maxIdle>
					<maxWait>1000</maxWait>
					<host>10.1.200.188</host><port>6379</port>
				</node>
			</servernode>
			<!-- redis节点 配置结束 -->
		</redisrouting>
		
		
