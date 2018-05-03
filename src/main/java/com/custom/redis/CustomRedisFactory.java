package com.custom.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import com.custom.redis.routing.RoutingConfigManager;
import com.custom.redis.util.CustomRedisNodeMonitor;

/**
 * 
 * <P>
 * File name : CustomRedisFactory.java
 * </P>
 * <P>
 * Author : anytron
 * </P>
 * <P>
 * Date : 2016-1-29
 * </P>
 */
public class CustomRedisFactory {
	
	private final static Logger log = Logger.getLogger(CustomRedisFactory.class); 

	private static class LazyHolder {
		private static final CustomRedisFactory INSTANCE = new CustomRedisFactory();
	}

	private static Map<String, AppRedis> appCustomRedis = new HashMap<String, AppRedis>();
	
	private static RoutingConfigManager routingConfig;

	private CustomRedisFactory() {
	}
	
	static {
		System.out.println("CustomRedis init begin!!!");
		//初始化配置文件
		try {
			routingConfig = RoutingConfigManager.getInstance();
		} catch (Exception e) {
			log.error("CustomRedis init custom-redis.xml error cause:"+e.getMessage());
			System.out.println("CustomRedis init custom-redis.xml error cause:"+e.getMessage());
			e.printStackTrace();
		}
		System.out.println("CustomRedis init custom-redis.xml success!!!");
		//初始化cluster
		if(routingConfig!=null){
			String available = routingConfig.getString("available");
			if(available!=null&&available.equals("false")){
				System.out.println("CustomRedis available is "+available + " CustomRedis do not start!!!");
				log.info("CustomRedis available is "+available + " CustomRedis Do not start!!!");
			}else{
				System.out.println("CustomRedis CustomRedisNodeMonitor start !!!");
				//启动节点监控
				CustomRedisNodeMonitor.getInstance();
				System.out.println("CustomRedis CustomRedisNodeMonitor start success!!!");
				@SuppressWarnings("unchecked")
				List<String> appNodesList = routingConfig.getList("appnode.anode.id");
				for (int clusterIndex = 0; clusterIndex < appNodesList.size(); clusterIndex++) {
					String appnodeId 		= appNodesList.get(clusterIndex);
					String dispatch 		= routingConfig.getString("appnode.anode("+clusterIndex+").dispatch");
					String clusterstrategy  = routingConfig.getString("appnode.anode("+clusterIndex+").clusterstrategy");
					String clusternodes 	= routingConfig.getString("appnode.anode("+clusterIndex+").clusternodes");
					String ismonitor 		= routingConfig.getString("appnode.anode("+clusterIndex+").ismonitor");
					String monitorinterval 	= routingConfig.getString("appnode.anode("+clusterIndex+").monitorinterval");
					List<String> clusterIds = getClusterByString(clusternodes);
					AppRedis appRedis = new AppRedis(appnodeId, dispatch, clusterstrategy, clusterIds, ismonitor, monitorinterval);
					appCustomRedis.put(appnodeId,appRedis);
					log.info("CustomRedis init app="+appnodeId+" success!");
				}
				log.info("CustomRedis init all app("+appNodesList.size()+") start success!");
			}
		}else{
			System.out.println("CustomRedis custom-redis.xml can not read CustomRedis Do not start!!!");
			log.error("CustomRedis custom-redis.xml can not read CustomRedis Do not start!!!");
		}
		System.out.println("CustomRedis init Finish!!!");
	}

	private static List<String> getClusterByString(String clusternodes) {
		List<String> clusterIds = new ArrayList<String>();
		if(clusternodes.indexOf("|")==-1){
			clusterIds.add(clusternodes);
		}else{
			String[] clusters = clusternodes.split("\\|");
			for(String cluster:clusters){
				clusterIds.add(cluster);
			}
		}
		return clusterIds;
	}

	public static CustomRedisFactory getInstance() {
		return LazyHolder.INSTANCE;
	}

	public ICustomRedis getCustomRedisByAppId(String appId, String token) {
		AppRedis redisApp = appCustomRedis.get(appId);
		if(redisApp ==null){
			return null;
		}
		return redisApp.getCustomRedis(token);
	}

	public List<ICustomRedis> getAllCustomRedisByAppId(String appId) {
		AppRedis redisApp = appCustomRedis.get(appId);
		if(redisApp ==null){
			return null;
		}
		return Collections.unmodifiableList(redisApp.getAllRedisCluster());
	}
	
	
	/**
	 * @param args
	 * @throws ConfigurationException 
	 */
	public static void main(String[] args) throws ConfigurationException {
		CustomRedisFactory f = CustomRedisFactory.getInstance();
		ICustomRedis redis1 = f.getCustomRedisByAppId("user1","");
		redis1.set("key1", "dfffffffffffffffffff");
		System.out.println(redis1.get("key1"));
		Map<String,String> map1 = new HashMap<String, String>();
		map1.put("001", "ABC");
		Map<String,String> map2 = new HashMap<String, String>();
		map2.put("002", "DEF");
		map2.put("0021", "DEF1");
		map2.put("0022", "DEF2");
		Map<String,String> map3 = new HashMap<String, String>();
		map3.put("003", "GHI");
		//------------------------list集合
		redis1.lpush("listkey1", "123");
		redis1.lpush("listkey1", "345");
		redis1.lpush("listkey1", "456");
		System.out.println(redis1.llen("listkey1"));//获取列表长度
		System.out.println(redis1.lrange("listkey1", 0, redis1.llen("listkey1")));//角标获取,倒着的
		
		//------------------------map集合
		
		redis1.hset("listkey2", "123","abc");
		redis1.hset("listkey2", "345","abc");
		redis1.hset("listkey2", "456","abc");
		System.out.println(redis1.hlen("listkey2"));//获取map长度
		System.out.println(redis1.hgetAll("listkey2"));//获取map
		
		//
		//List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		
//		redis1 = f.getCustomRedisByAppId("degrade", "11");
//		// ICustomRedis redis2 = f.getCustomRedisByAppId("degrade", "12");
//		// ICustomRedis redis3 = f.getCustomRedisByAppId("degrade", "13");
//		// ICustomRedis redis4 = f.getCustomRedisByAppId("degrade", "14");
//		// ICustomRedis redis5 = f.getCustomRedisByAppId("degrade", "15");
//		// ICustomRedis redis6 = f.getCustomRedisByAppId("degrade", "16");
//		// ICustomRedis redis7 = f.getCustomRedisByAppId("degrade", "17");
//		// ICustomRedis redis8 = f.getCustomRedisByAppId("degrade", "18");
//		// ICustomRedis redis9 = f.getCustomRedisByAppId("degrade", "19");
//		// ICustomRedis redis10 = f.getCustomRedisByAppId("degrade", "20");
//		//
		 //System.out.println(redis1.get("ypj_test"));
//		// System.out.println(redis2.get("zou1"));
//		// System.out.println(redis3.get("zou1"));
//		// System.out.println(redis4.get("zou1"));
//		// System.out.println(redis5.get("zou1"));
//		// System.out.println(redis6.get("zou1"));
//		// System.out.println(redis7.get("zou1"));
//		// System.out.println(redis8.get("zou1"));
//		// System.out.println(redis9.get("zou1"));
//		for(int i=0;i<100000;i++){
////			redis1 = f.getCustomRedisByAppId("degrade", "1");
////			if(redis1!=null)System.out.println("degrade redis1="+redis1.get("MONITOR_CLUSTER_KEY"));
//			redis1 = f.getCustomRedisByAppId("other", "1");
//			if(redis1!=null)System.out.println("other redis1="+redis1.get("MONITOR_CLUSTER_KEY"));
//		}
	
//		System.out.println(redis1.get("key啊发发1"));
//		redis1.decrBy("key啊发发1", 1);
//		System.out.println(redis1.get("key啊发发1"));
////		for (int i = 0; i < 1; i++) {
////			redis1 = f.getCustomRedisByAppId("degrade", i + "");
////			System.out.println(redis1.get("key啊发发"));
////			redis1.decrBy("key啊发发", 1);
////			System.out.println(redis1.get("key啊发发"));
////			//System.out.println(redis1.get("key" + i));
////		}

	}

}
