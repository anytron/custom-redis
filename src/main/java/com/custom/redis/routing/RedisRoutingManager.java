package com.custom.redis.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
/**
 * redis路由管理
 * 读取路由配置文件 初始化配置中的路由节点信息
 * <P>File name : RedisRoutingManager.java </P>
 * <P>Author : anytron </P> 
 * <P>Date : 2016-1-29 </P>
 */
@SuppressWarnings("unchecked")
public class RedisRoutingManager {
	/**
	 * 路由节点集合
	 */
	private Map<String,RedisCluster> redisClusterMap = new HashMap<String, RedisCluster>();
	/**
	 * redis连接池集合
	 */
	private HashMap<String,JedisPool> redisPoolMap = new HashMap<String,JedisPool>();
	
	private static class LazyHolder{
		private static final RedisRoutingManager INSTANCE = new RedisRoutingManager();
	}
	
	//获取所有节点
	public HashMap<String,JedisPool> getAllServiceNodes(){
		return (HashMap<String, JedisPool>) redisPoolMap.clone();
	}
	private RedisRoutingManager(){
		try {
			initDbRouting();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public static RedisRoutingManager getInstance(){
		return LazyHolder.INSTANCE;
	}
	
	/**
	 * 初始化配置文件信息
	 * @throws ConfigurationException 
	 * @throws org.apache.commons.configuration.ConfigurationException 
	 * 
	 */
	private void initDbRouting() throws ConfigurationException{
		XMLConfiguration routingConfig = new XMLConfiguration("custom-redis.xml");
		List<String> serverNodesList = routingConfig.getList("servernode.node.id");
		//存放所有servernode对应的JedisPool key=servernode value=JedisPool
		HashMap<String,JedisPool> redisConnectionMap = new HashMap<String,JedisPool>();
		//初始化所有servernode,存入redisConnectionMap
		for(int clusterIndex=0;clusterIndex<serverNodesList.size();clusterIndex++){
			String nodeId = serverNodesList.get(clusterIndex);
			int maxActive = routingConfig.getInt("servernode.node("+clusterIndex+").maxActive",20);
			int maxIdle = routingConfig.getInt("servernode.node("+clusterIndex+").maxIdle",20);
			int maxWait = routingConfig.getInt("servernode.node("+clusterIndex+").maxWait",20);
			String host = routingConfig.getString("servernode.node("+clusterIndex+").host");
			int port = routingConfig.getInt("servernode.node("+clusterIndex+").port");
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxActive(maxActive);
			config.setMaxIdle(maxIdle);
			config.setMaxWait(maxWait);
			config.setTestOnBorrow(false);
			JedisPool pool = new JedisPool(config, host,port);
			redisConnectionMap.put(nodeId, pool);
		}
		this.redisPoolMap = redisConnectionMap;
		//所有的clusternode
		List<String> clusterList = routingConfig.getList("clusternode.cnode.id");
		Map<String,RedisCluster> clusters = new HashMap<String,RedisCluster>();
		for(int clusterIndex=0;clusterIndex<clusterList.size();clusterIndex++){
			String clusterId = clusterList.get(clusterIndex);
			String writerstrategy = routingConfig.getString("clusternode.cnode("+clusterIndex+").writerstrategy");
			String readstrategy = routingConfig.getString("clusternode.cnode("+clusterIndex+").readstrategy");
			String readservernodes = routingConfig.getString("clusternode.cnode("+clusterIndex+").readservernodes");
			String writeservernodes = routingConfig.getString("clusternode.cnode("+clusterIndex+").writeservernodes");
			RedisCluster cluster = new RedisCluster(clusterId,readstrategy,writerstrategy,redisConnectionMap);
			//readservernodes分别插入cluster
			if(StringUtils.isNotBlank(readservernodes)){
				String[] nodes = readservernodes.split("\\|");
				for(int i = 0 ; i < nodes.length ; i++){
					String node = nodes[i];
					if(redisPoolMap.get(node) == null){
						
					}else{
						cluster.addReadRedisNode(node);	
					}
				}
			}
			//writeservernodes分别插入cluster
			if(StringUtils.isNotBlank(writeservernodes)){
				String[] nodes = writeservernodes.split("\\|");
				for(int i = 0 ; i < nodes.length ; i++){
					String node = nodes[i];
					if(redisPoolMap.get(node) == null){
						
					}else{
						cluster.addWriteRedisNode(node);	
					}
				}
			}
			clusters.put(clusterId,cluster);
		}
		//读取参数完毕
		//开始初始化连接池
//		for(RedisCluster cluster:clusters){
//			cluster.setNodeToLive();
//		}
		this.redisClusterMap = clusters;
	}
	/**
	 * 返回节点对应的连接池
	 * RedisRoutingManager.getJedisPool()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2016-1-31 </P>
	 * @param node
	 * @return
	 */
	protected JedisPool getJedisPool(String node){
		JedisPool pool = redisPoolMap.get(node);
		return pool;
	}
	/**
	 * 返回路由节点
	 * RedisRoutingManager.getRedisCluster()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2016-1-31 </P>
	 * @param clusterId
	 * @return
	 */
	protected RedisCluster getRedisCluster(String clusterId){
		return redisClusterMap.get(clusterId);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RedisRoutingManager manager = RedisRoutingManager.getInstance();
		manager.getJedisPool("11");
	}

}
