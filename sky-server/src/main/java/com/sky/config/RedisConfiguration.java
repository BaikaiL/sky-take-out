package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis配置类
 */
@Configuration
public class RedisConfiguration {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		// 设置连接工厂
		redisTemplate.setConnectionFactory(redisConnectionFactory);

		// 1. 设置 Key 使用 String 序列化 (这样 Key 在 Redis 里是可读的)
		redisTemplate.setKeySerializer(new StringRedisSerializer());

		// 2. 设置 Value 使用 JSON 序列化 (这样 Value 自动转为 JSON)
		// 使用 GenericJackson2JsonRedisSerializer 替换默认的 JDK 序列化
		redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

		return redisTemplate;
	}
}
