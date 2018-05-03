package com.custom.redis.routing;

/**
 * redis路由异常类
 * <P>File name : RedisRoutingException.java </P>
 * <P>Author : anytron </P> 
 * <P>Date : 2016-1-29 </P>
 */
public class RedisRoutingException extends RuntimeException {
	/**
	 * 字段或域定义：<code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -5034463949539478898L;
	
	public RedisRoutingException(){
		super();
	}
	
	public RedisRoutingException(String msg){
		super(msg);
	}
}
