package com.custom.redis.routing;

import org.apache.commons.configuration.XMLConfiguration;
import java.util.List;
import java.util.Properties;

/**
 * <p>Title:DbRouting配置文件管理</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2012 3</p>
 *
 * <p>Company: lafaso.com</p>
 *
 * @author anytron
 * @version 1.0
 */
@SuppressWarnings("rawtypes")
public final class RoutingConfigManager {
    XMLConfiguration config = null;
    private static RoutingConfigManager configManager = new RoutingConfigManager();
    
    private RoutingConfigManager() {
    	System.out.println("Redis init custom-redis.xml!");
        try {
           config = new XMLConfiguration("custom-redis.xml");
        } catch (Exception ex) {
        	System.out.println("Redis init custom-redis.xml error!");
            ex.printStackTrace();
        }
    }
    
    public static RoutingConfigManager getInstance(){
        return configManager;
    }
    
    public String getString(String propName){
        return config.getString(propName);
    }
    
    public int getInteger(String propName){
        return config.getInt(propName);
    }
    
	public List getList(String propName){
        return config.getList(propName);
    }
	
    public Object getObject(String propName){
        return config.getProperty(propName);
    }
    
    public Properties getProperties(String propName){
        return config.getProperties(propName);
    }
    
    public Object getProperty(String propName){
        return config.getProperty(propName);
    }
}
