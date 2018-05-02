package com.custom.redis.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.custom.redis.routing.RedisRoutingManager;
import com.custom.redis.routing.RoutingConfigManager;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * HmRedis所有节点监控对象
 * 从配置文件中读取所有节点<servernode>,根据参数初始化redis并执行写入和读取操作用于测试该节点是否正常
 * @author anytron
 */
public class CustomRedisNodeMonitor implements Runnable {
	
	private final static Logger log = Logger.getLogger(CustomRedisNodeMonitor.class); 
	
	private Thread thread = null;
	
	private static boolean INIT_SUCESS = false;
	
	private static long NODE_MONITOR_INTERVAL = 60000;

	//用于node的监控
	private static Map<String,Boolean> nodeMonitor = new HashMap<String,Boolean>();
	
	//所有节点
	private static HashMap<String,JedisPool> serverNodes = RedisRoutingManager.getInstance().getAllServiceNodes();
	
	private static RoutingConfigManager routingConfig = RoutingConfigManager.getInstance();
	
	
	private static class LazyHolder {
		private static final CustomRedisNodeMonitor INSTANCE = new CustomRedisNodeMonitor();
	}
	
	public static CustomRedisNodeMonitor getInstance() {
		return LazyHolder.INSTANCE;
	}
	
	private CustomRedisNodeMonitor(){
		try {
			String temp = routingConfig.getString("nodemonitorinterval");
			NODE_MONITOR_INTERVAL = Long.parseLong(temp);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("HmRedis error nodemonitorinterval cause:"+e.getMessage());
		}
		thread = new Thread(this);
		thread.start();
		//第一次监控完成后才能退出
		while(!INIT_SUCESS){
			try {
				Thread.sleep(2000);//第一次
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error("HmRedis error new HmRedisNodeMonitor cause:"+e.getMessage());
			}
		}
		log.info("HmRedis info HmRedisNodeMonitor start success!");
	}
	
	
	public void run() {
		@SuppressWarnings("unused")
		long i = 0;
		while (true) {
			try {
				executeMonitor(serverNodes);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(!INIT_SUCESS)INIT_SUCESS = true;
			try {
				Thread.sleep(NODE_MONITOR_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 执行监控
	 * AppRedis.executeMoniter()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2013-5-8 </P>
	 * @param serverNodes
	 */
	private void executeMonitor(HashMap<String, JedisPool> serverNodes) {
		boolean flag = true;
		JedisPool pool = null;
		Jedis jedis = null;
		for(Map.Entry<String,JedisPool> entry: serverNodes.entrySet()) {
			try{
				pool = null;
				jedis = null;
				pool = entry.getValue();
				jedis = pool.getResource();
				jedis.set(CustomRedisConstants.MONITOR_CLUSTER_KEY,CustomRedisConstants.MONITOR_CLUSTER_VALUE);
				String value = jedis.get(CustomRedisConstants.MONITOR_CLUSTER_KEY);
				if(value.equals(CustomRedisConstants.MONITOR_CLUSTER_VALUE)){
					nodeMonitor.put(entry.getKey(), true);
					log.info("HmRedis node="+entry.getKey() +" status=running ");
				}else{
					flag = false;
					nodeMonitor.put(entry.getKey(), false);
					log.error("HmRedis node="+entry.getKey() +" status=down ");
				}
			}catch(Exception e){
				e.printStackTrace();
				flag = false;
				nodeMonitor.put(entry.getKey(), false);
				log.error("HmRedis node="+entry.getKey() +" status=exception ");
			}finally{
				if(jedis != null){
					try {
						pool.returnResource(jedis);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if(flag)log.info("HmRedis all nodes("+serverNodes.size()+") test success!");
	}
	
	//获取某个node的当前状态
	public boolean redisNodeIsOk(String nodeId){
		return nodeMonitor.get(nodeId);
	}
	
	
	public static void main(String[] args) {
		CustomRedisNodeMonitor.getInstance();
	}
	
}
