package com.custom.redis.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.custom.redis.AppRedis;
import com.custom.redis.util.ConsistentHash;

import redis.clients.jedis.JedisPool;

/**
 * redis路由节点
 * 根据不同的策略分别提供读连接 和写连接
 * 后期实现redis节点监控 现阶段还没有实现
 * <P>File name : RedisCluster.java </P>
 * <P>Author : anytron </P> 
 * <P>Date : 2013-1-29 </P>
 */
public class RedisCluster {
	/**
	 * Cluster节点ID，这个是系统集群的唯一标识，
		在配置文件中设置，不允许重复，可以通过这个参数获取相关集群的连接
	*/
	private String clusterId; 
	
	
	/**
	 * redis集群读策略，包括最小连接、随机数、最高性能、一致性Hash策略
	 */
	String readClusterStrategy;
	
	/**
	 * 写策略(多写(复写)、尾号写策略、一致性Hash策略)
	 */
	String writeClusterStrategy;
	
	
	/**
	 * 读连接节点
	 */
	private List<String> readRedisNodeList = new ArrayList<String>();
	
	
	/**
	 * 写连接节点集合
	 */
	private List<String> writeRedisNodeList = new ArrayList<String>();
	
	/**
	 * 初始化读节点一致性Hash算法
	 */
	@SuppressWarnings("unused")
	private ConsistentHash<String> writeHash = null;
	
	/**
	 * 初始化写节点一致性Hash算法
	 */
	private ConsistentHash<String> readHash = null;
	
	/**
	 * 定义节点的监控，这个节点监控可以监控读集群、写集群等
	 */
	//private final RedisClusterNodeMonitor monitor;
	private Map<String,JedisPool> redisPoolMap;
	
	
	public RedisCluster(String clusterId, String readClusterStrategy,
			String writeClusterStrategy,Map<String,JedisPool> redisPoolMap) {
		this.clusterId = clusterId;
		this.redisPoolMap = redisPoolMap;
		if (readClusterStrategy != null) {
			this.readClusterStrategy = readClusterStrategy;
		} else {
			this.readClusterStrategy = RedisRoutingConstants.CLUSTER_STRATEGY_MAXPERFORMANCE; //如果没有设置，默认为最高性能
		}
		
		if (writeClusterStrategy != null) {
			this.writeClusterStrategy = writeClusterStrategy;
		} else {
			this.writeClusterStrategy = RedisRoutingConstants.CLUSTER_STRATEGY_MAXPERFORMANCE; //如果没有设置，默认为最高性能
		}
//		monitor = new RedisClusterNodeMonitor();
//		monitor.start(true);
	}
	
	
	public void setNodeToLive() {
		
		
//		RedisClusterNode clusterNode = this.createClusterNode(nodeId,
//				this);
//		monitor.addClusterNode(clusterNode);
		
	}

	/**
	 * 获取读连接池
	 * 根据不同的策略获取读连接
	 * 先支持的策略为随机获取 hash获取
	 * 
	 * v2.0 edit by  
	 * 获取读连接池，不依赖任何规则，先读取主节点，主节点有问题读取从节点
	 * RedisCluster.getReadRedisPool()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-1-31 </P>
	 * @param token
	 * @return
	 * @version 2.0 
	 */
	public JedisPool getReadRedisPool(String token){

		if(null!=readRedisNodeList && readRedisNodeList.size()>0){
			if (RedisRoutingConstants.CLUSTER_STRATEGY_RANDOM.equals(this.readClusterStrategy)) {
				// 如果读库是随机选择,使用随时数算法
				int nodeSize = readRedisNodeList.size();
				int index = (int) (Math.random() * nodeSize);
				String node =  readRedisNodeList.get(index);
				System.out.println("RANDOM getReadRedisPool:"+node);
				return getReadPool(node);
			} else if(RedisRoutingConstants.CLUSTER_STRATEGY_HASHREAD.equals(this.readClusterStrategy)){// 如果是Hash算法，这里采用的一致性Hash
				if (readHash == null) {// 初始化一致性Hash算法，只初始化一次
					readHash = new ConsistentHash<String>(readRedisNodeList);
				}
				String node =  readHash.get(token);
				System.out.println("HASHREAD getReadRedisPool:"+node);
				return getReadPool(node);
			}else{
				throw new RedisRoutingException("the stategy:" + readClusterStrategy + " can not support,please set the  readClusterStrategy in custom-redis.xml correct");
			}
//			for(String readNode : readRedisNodeList){
//				//判断readNode 是否活着。
//				if(AppRedis.checkNode(readNode)){
//					return getReadPool(readNode);
//				}
//			}
		}else{
			throw new RedisRoutingException("please set the readservernodes in custom-redis.xml");
		}
	}
	
	private JedisPool getReadPool(String nodeId) throws RedisRoutingException {
		if (nodeId == null)
			throw new RedisRoutingException("can not find the node id=" + nodeId);
		if(AppRedis.checkNode(nodeId)){
			return redisPoolMap.get(nodeId);
		}
		JedisPool JedisPool = redisPoolMap.get(nodeId);
		return JedisPool;
	}
	/**
	 * 获取写连接池 (写连接池可能包含多个)
	 * 根据不同的策略获取写连接池
	 * 现支持hash策略  复写策略
	 * RedisCluster.getWriteRedisPool()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-1-31 </P>
	 * @param token
	 * @return
	 */
	public List<JedisPool> getWriteRedisPool(String token){
//		if(RedisRoutingConstants.CLUSTER_STRATEGY_HASHREAD.equals(this.writeClusterStrategy)){
//			if (writeHash == null) {// 初始化一致性Hash算法，只初始化一次
//				writeHash = new ConsistentHash<String>(writeRedisNodeList);
//			}
//			String nodeId = writeHash.get(token);
//			System.out.println("token="+token+" nodeId="+nodeId);
//			List<JedisPool> redisConnectionList = new ArrayList<JedisPool>();
//			JedisPool JedisPool = redisPoolMap.get(nodeId);
//			redisConnectionList.add(JedisPool);
//			return redisConnectionList;
//		}else 
		if(RedisRoutingConstants.CLUSTER_STRATEGY_MUTIWRITE.equals(this.writeClusterStrategy)){
			List<JedisPool> redisConnectionList = new ArrayList<JedisPool>();
			for(String nodeId : writeRedisNodeList){
				JedisPool JedisPool = redisPoolMap.get(nodeId);
				//System.out.println("getWriteRedisPool:"+nodeId);
				redisConnectionList.add(JedisPool);
			}
			return redisConnectionList;
//		}else if(){
//			// 如果是尾号读写
//			if (token.length() < 2)
//				throw new SQLException("illegal token:" + token
//						+ ",maybe occur error");
//			String lastTwoNumber = token.substring(token.length() - 2);
//			String tokenNode = null;
//			for (int i = 0; i < nodes.size(); i++) {
//				Map<String, Integer> trailNumberHash = nodeTrailNumber.get(nodes.get(i));
//				if (trailNumberHash != null && trailNumberHash.size() > 0) {
//					if (trailNumberHash.containsKey(lastTwoNumber)) {
//						tokenNode = nodes.get(i);
//						break;
//					}
//				}
//			}
//		
//			return getConnectionFromPool(tokenNode);
		}else{
			throw new RedisRoutingException(
					"the stategy:"
							+ writeClusterStrategy
							+ " can not support,please set the  writeClusterStrategy in custom-redis.xml correct");
		}
	}
	
	/**
	 * 增加读节点
	 * RedisCluster.addReadRedisNode()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-1-31 </P>
	 * @param node
	 */
	protected void addReadRedisNode(String node){
		readRedisNodeList.add(node);
	}
	/**
	 * 增加写节点
	 * RedisCluster.addWriteRedisNode()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-1-31 </P>
	 * @param node
	 */
	protected void addWriteRedisNode(String node){
		writeRedisNodeList.add(node);
	}
	/**
	 * 移除读节点
	 * RedisCluster.removeReadRedisNode()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-1-31 </P>
	 * @param node
	 */
	protected void removeReadRedisNode(String node){
		boolean isSuc = readRedisNodeList.remove(node);
		if(isSuc){
			this.readHash = null;
		}
	}
	/**
	 * 移除写节点
	 * RedisCluster.removeWriteRedisNode()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-1-31 </P>
	 * @param node
	 */
	protected void removeWriteRedisNode(String node){
		boolean isSuc = writeRedisNodeList.remove(node);
		if(isSuc){
			this.writeHash = null;
		}
	}
	/**
	 * 获取节点ID
	 * RedisCluster.getClusterId()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-1-31 </P>
	 * @return
	 */
	public String getClusterId() {
		return clusterId;
	}

}
