# custom-redis
custom-redis提供了多种访问redis的方式，具体使用方式参考如下:

依赖jar包:<br/>
lafasoRedis.jar<br/>
commons-attributes-api-2.2.jar<br/>
commons-beanutils-1.8.3.jar<br/>
commons-collections-3.2.1.jar<br/>
commons-configuration-1.5.jar<br/>
commons-io-2.1.jar<br/>
commons-lang-2.4.jar<br/>
commons-logging-1.1.1.jar<br/>
commons-pool-1.3.jar<br/>
jedis-2.1.0.jar<br/>
log4j-1.2.16.jar<br/>

依赖配置文件:
classpath:/ custom-redis.xml

使用代码:
cloudyCart指应用名,具体解释参考custom-redis.xml解释部分
CustomRedis是jedis的封装,通过CustomRedis可以执行redis的所有方法

CustomRedisFactory f = CustomRedisFactory.getInstance();
ICustomRedis redis1 = f.getCustomRedisByAppId("user1","");
redis1.set("key1", "value001");


配置文件custom-redis.xml说明:
custom-redis按照应用为单位管理redis，可以在一个custom-redis.xml文件中配置多个应用，每个应用对应了多种方式实现redis集群的管理。

配置文件分成3个部分

1、<appnode></appnode>以应用为单位配置redis集群，一个配置文件中可以配置多个应用，一个应用可以配置多个集群节点，具体请参考
2、<clusternode></clusternode>配置各个集群，一个集群可以按照集群类型配置多个redis节点
3、<servernode></servernode>配置所有需要的redis节点，每个节点配置。

配置文件示例:

<redisrouting>
	<!-- 是否启动初始化redis false=不启动 默认为启动 如果该配置文件不存在也不启动!-->
	<available>true</available>
	<!-- 节点监控时间间隔 缺省60000 单位毫秒 -->
	<nodemonitorinterval>60000</nodemonitorinterval>
	<appnode>
		<anode><!-- 可配置多个 -->
			<id>user1</id>
			<!-- 多个cluster必须用竖线分割,cluster1对应clusternode中配置的id -->
			<clusternodes>cluster1</clusternodes>
			<dispatch>ROUTING</dispatch>
			<!-- TRAILNUMBER尾号 ONLY只读第一个 HASH一致性哈希算法-->
			<clusterstrategy>ONLY</clusterstrategy>
			<!-- 是否监控 默认为false 只有=true monitorinterval才有效 -->
			<ismonitor>true</ismonitor>
			<!-- 间隔毫秒 -->
			<monitorinterval>60000</monitorinterval>
		</anode>
	</appnode>
	<!-- redis节点配置开始 -->
	<clusternode>
		<cnode><!-- 可配置多个 -->
			<id>cluster1</id>
			<readstrategy>HASHREAD</readstrategy>
			<readservernodes>snode1</readservernodes>
			<writerstrategy>MUTIWRITE</writerstrategy>
			<writeservernodes>snode1</writeservernodes>
			<!-- 该节点对应的尾号,当appnode->anode->clusterstrategy=TRAILNUMBER时才有效 -->
			<trailnumber>00-00</trailnumber>
		</cnode>
	</clusternode>
	<!-- servernode 对应每一台需要用到的redis服务器 -->
	<servernode>
		<node><!-- 可配置多台 -->
			<id>snode1</id>
			<maxActive>200</maxActive><!-- 最大的活动连接 -->
			<maxIdle>100</maxIdle><!-- 最大的空闲连接 -->
			<maxWait>1000</maxWait><!-- 最大的等待时间 -->
			<host>127.0.0.1</host><port>6379</port><!--Ip地址和端口 -->
		</node>
	</servernode>
	<!-- redis节点 配置结束 -->
</redisrouting>
