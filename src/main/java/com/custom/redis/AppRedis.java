package com.custom.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

import com.custom.redis.routing.RoutingConfigManager;
import com.custom.redis.routing.RoutingCustomRedis;
import com.custom.redis.util.ConsistentHash;
import com.custom.redis.util.CustomRedisNodeMonitor;

/**
 * 
 * <P>
 * File name : AppRedisCluster.java
 * </P>
 * <P>
 * Author : anytron
 * </P>
 * <P>
 * Date : 2016-1-29
 * </P>
 */
public class AppRedis {

	private String appId;
	private String dispatch;
	private String clusterStrategy;
	private boolean ismonitor;
	private long monitorinterval;
	private List<String> clusterIds;
	private ConsistentHash<String> hash;
	private final static RoutingConfigManager routingConfig = RoutingConfigManager.getInstance();;
	private final static CustomRedisNodeMonitor nodeMoniter = CustomRedisNodeMonitor.getInstance();
	
	private final static Logger log = Logger.getLogger(AppRedis.class);
	
	//根据尾号获取clusternode
	private static Map<Integer,String> cluster = new HashMap<Integer,String>();
	//将每个cluster初始化到map
	private static Map<String,ICustomRedis> clusterRedis = new HashMap<String,ICustomRedis>();
	//用于cluster的监控
	private static Map<String,Boolean> clusterMonitor = new HashMap<String,Boolean>();
	
	/**
	 * 判断node is running 
	 * AppRedis.checkNode()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2016-5-7 </P>
	 * @param node 该node对应配置文件中snode1,如下所示
	 * <servernode><node><id>snode1</id>
	 * @return true 正常
	 */
	public static boolean checkNode(String node){
		return nodeMoniter.redisNodeIsOk(node);
	}
	
	

	public AppRedis(String _appId, String _dispatch, String _clusterStrategy, List<String> clusterIds,String _ismonitor,String _monitorinterval) {
		this.appId = _appId;
		this.dispatch = _dispatch;
		this.clusterIds = clusterIds;
		this.clusterStrategy = _clusterStrategy;
		this.ismonitor = _ismonitor == null ? false : _ismonitor.equals("true") ? true : false;
		this.monitorinterval = 10000;
		try{
			if(ismonitor){
				monitorinterval = Long.parseLong(_monitorinterval);
			}
		}catch (Exception e) {
			e.printStackTrace();
			log.error("Long.parseLong(_monitorinterval) error _monitorinterval="+_monitorinterval+" set default value=10000");
		}
		//初始化
		initAppRedis();
	}
	
	private void initAppRedis() {
		//如果是尾号则需要初始化map
		if("TRAILNUMBER".equals(clusterStrategy) && (cluster==null || cluster.size()==0)){
			pareseTrailnumberRange(clusterIds);
		}
		for(String s:clusterIds){
			ICustomRedis redis = new RoutingCustomRedis(s);
			clusterRedis.put(s, redis);
		}
		//初始化 cluster 与 node的对应关系
		if(null!=clusterNodesMap && clusterNodesMap.size()==0){
			initClusterNodesMapping(clusterIds);
		}
		//是否需要启动监控
		if(ismonitor)monitorCluster();
	}
	
	//用于监控的 cluster与node的对应关系
	private Map<String,String> clusterNodesMap = new HashMap<String,String>();
	/**
	 * 初始化cluster与node的对应关系
	 * AppRedis.initClusterNodesMapping()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2016-5-7 </P>
	 * @param clusterIds
	 */
	private void initClusterNodesMapping(List<String> clusterIds){
		for(String s:clusterIds){
			@SuppressWarnings("unchecked")
			List<String> clusterNodesList = routingConfig.getList("clusternode.cnode.id");
			for(int clusterIndex = 0; clusterIndex < clusterNodesList.size(); clusterIndex++){
				String id = clusterNodesList.get(clusterIndex) == null ? "":clusterNodesList.get(clusterIndex).trim();
				if(s.trim().equals(id)){
					String readServerNodes = routingConfig.getString("clusternode.cnode("+clusterIndex+").readservernodes");
					if(null==readServerNodes || readServerNodes.equals("")){
						log.error("null==readServerNodes || readServerNodes('')");
						break;
					}else{
						clusterNodesMap.put(s, readServerNodes);
					}
				}
			}
		}
	}

	public void pareseTrailnumberRange(List<String> clusterIds) {
		if(clusterIds==null||clusterIds.size()==0){
			log.error("pareseTrailnumberRange error clusterIds="+clusterIds);
			return;
		}
		for(String s:clusterIds){
			@SuppressWarnings("unchecked")
			List<String> clusterNodesList = routingConfig.getList("clusternode.cnode.id");
			for(int clusterIndex = 0; clusterIndex < clusterNodesList.size(); clusterIndex++){
				String id = clusterNodesList.get(clusterIndex) == null ? "":clusterNodesList.get(clusterIndex).trim();
				if(s.trim().equals(id)){
					String trailnumber = routingConfig.getString("clusternode.cnode("+clusterIndex+").trailnumber");
					//如果没有配置尾号，则为配置有误。跳出循环，或者报错
					if(null==trailnumber || trailnumber.equals("")){
						log.error("null==trailnumber || trailnumber.equals('')");
						break;
					}else{
						//尾号组
						String trailnumbers [];
						//判断是配置了几组尾号。  尾号间用"|"隔开
						if(trailnumber.indexOf("|")!=-1){
							trailnumbers = trailnumber.split("\\|");
						}else{
							trailnumbers = new String[1];
							trailnumbers[0] = trailnumber;
						}
						for(int i = 0 ; i < trailnumbers.length ; i++){
							String nums = trailnumbers[i];
							//判断是尾号区间，还是单一尾号。如果单一尾号直接放入map
							if(nums.indexOf("-")!=-1){
								String num[] = nums.split("-");
								int start = Integer.valueOf(num[0]);
								int end = Integer.valueOf(num[1]);
								while(start<=end){
									cluster.put(start, id);
									start++;
								}
							}else{
								cluster.put(Integer.valueOf(nums), id);
							}
						}
					}
				}
			}
		}
	}

	public ICustomRedis getCustomRedis(String token) {
		if ("ROUTING".equals(dispatch)) {
			String clusterId = "";
			if ("HASH".equals(clusterStrategy)) {
				if (hash == null) {// 初始化一致性Hash算法，只初始化一次
					hash = new ConsistentHash<String>(clusterIds);
				}
				clusterId = hash.get(token);
			}
			//
			else if ("ONLY".equals(clusterStrategy)) {
				clusterId = clusterIds.get(0);
			}
			// 尾号
			else if ("TRAILNUMBER".equals(clusterStrategy)) {
				if(token==null || token.length()<2){
					log.error("error clusterStrategy="+clusterStrategy+" token==null || token.length()<2");
					return null;
				}else{
					String tokenKey = token.substring(token.length()-2,token.length());
					String intReg = "^\\d{2}$";
					Pattern p = Pattern.compile(intReg);
					Matcher m = p.matcher(tokenKey);
					if(m.matches()){
						clusterId = cluster.get(Integer.valueOf(tokenKey));
					}
				}
			}
			if(ismonitor){
				if(clusterMonitor.get(clusterId)){
					return clusterRedis.get(clusterId);
				}
			}else{
				return clusterRedis.get(clusterId);
			}
		}
		return null;
	}

	public List<ICustomRedis> getAllRedisCluster() {
		List<ICustomRedis> redisList = new ArrayList<ICustomRedis>();
		if ("ROUTING".equals(dispatch)) {
			for (String clusterId : clusterIds) {
				ICustomRedis redis = new RoutingCustomRedis(clusterId);
				redisList.add(redis);
			}
		}
		return redisList;
	}
	
	/**
	 * 执行监控
	 * AppRedis.executeMoniter()<BR>
	  * <P>Author : anytron </P>  
	 * <P>Date : 2016-5-8 </P>
	 * @param serverNodes
	 */
	private void executeMoniter() {
		//对所有cluster进行操作
		for(String clusterId:clusterIds){
			String msg = "CustomRedis app=" + appId + " clusterId="+clusterId;//打印消息
			String readNodes = clusterNodesMap.get(clusterId);
			try{
				//无节点对应关系，配置文件有问题，不在进行后记判断
				if(null==readNodes || readNodes.equals("")){
					clusterMonitor.put(clusterId,false);
					log.info(msg + " status="+clusterMonitor.get(clusterId)+" monitorinterval="+monitorinterval);
				}else{
					//判断cluster对应几个node ，如果只对应一个，如果该node down则cluster down，如果对应多个，则只要有一个node running 则cluster is ok
					boolean nodeRunning = false;
					if(readNodes.indexOf("|")!=-1){
						String readNode [] = readNodes.split("\\|");
						for(int i = 0 ; i < readNode.length ; i++){
							String node = readNode[i];
							if(nodeMoniter.redisNodeIsOk(node)){
								nodeRunning = true;
								break;
							}
						}
					}else{
						if(nodeMoniter.redisNodeIsOk(readNodes)){
							nodeRunning = true;
						}
					}
					if(nodeRunning){
						clusterMonitor.put(clusterId,true);
						log.info(msg + " status="+clusterMonitor.get(clusterId)+" monitorinterval="+monitorinterval);
					}else{
						clusterMonitor.put(clusterId,false);
						log.info(msg + " status="+clusterMonitor.get(clusterId)+" monitorinterval="+monitorinterval);
					}
				}
			}catch (Exception e) {
				clusterMonitor.put(clusterId,false);
				log.info("CustomRedis appid="+appId+" "+clusterId+" status="+clusterMonitor.get(clusterId)+" monitorinterval="+monitorinterval+" error="+e.getMessage());
			}
		}
	}
	
	/**
	 * 监控该应用下redis的cluster是否正常运行
	 * AppRedis.monitorCluster()<BR>.
	 * <P>Author : wangchen </P>.
	 * <P>Date : 2016-4-16 </P>
	 */
	private void monitorCluster(){
		//初始化clusterMonitor
		for(String s:clusterIds){
			clusterMonitor.put(s,true);
		}
		//启动cluster监控
		new MonitorCluster();
	}
	
	


	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * 定时监控该应用下所有Cluster的状态
	 * <P>File name : AppRedis.java </P>
	 * <P>Author : wangchen </P>.
	 * <P>Date : 2016-4-16 </P>
	 */
	private class MonitorCluster implements Runnable {
		private Thread thread = null;
		MonitorCluster(){
			thread = new Thread(this);
			thread.start();
		}
		
		public void run() {
			while (true) {
				executeMoniter();
				try {
					Thread.sleep(monitorinterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
    }  
}



