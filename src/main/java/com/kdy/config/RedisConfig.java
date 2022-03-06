package com.kdy.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdy.bean.ehcache.CacheEventLoggerDTO;

@Configuration
@EnableRedisRepositories
public class RedisConfig {
	
	@Value("${spring.redis.port}")
	public int port;

	@Value("${spring.redis.host}")
	public String host;
	
	@Value("${spring.redis.password}")
	public String password;
	
	@Value("${spring.redis.sentinelYn}")
	public String sentinelYn;
	
	@Value("${spring.redis.replica1.port}")
	public int replica1Port;

	@Value("${spring.redis.replica1.host}")
	public String replica1Host;
	
	@Value("${spring.redis.replica1.password}")
	public String replica1Password;
	
	//@Value("${spring.redis.replica2.port}")
	//public int replica2Port;

	//@Value("${spring.redis.replica2.host}")
	//public String replica2Host;
	
	//@Value("${spring.redis.replica2.password}")
	//public String replica2Password;
	
	
	@Autowired
	public ObjectMapper objectMapper;
	
	CacheEventLoggerDTO cdto = CacheEventLoggerDTO.getInstance();

	@Bean(name="redisConnectionFactory")
	public RedisConnectionFactory redisConnectionFactory() {
		
		LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
				.commandTimeout(Duration.ofMillis(3000))
				.shutdownTimeout(Duration.ZERO)
				.build();
		
		if(sentinelYn.equals("Y")) {
			RedisSentinelConfiguration redisSentinelConfiguration = 
					new RedisSentinelConfiguration()
					.master("mymaster")
					.sentinel(host, port);
			redisSentinelConfiguration.setPassword(password);
			return new LettuceConnectionFactory(redisSentinelConfiguration, lettuceClientConfiguration);
		} else {
			if(password != null && !password.isEmpty()) {
				RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host,port);
		        redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
		        return new LettuceConnectionFactory(redisStandaloneConfiguration, lettuceClientConfiguration);
			} else {
				return new LettuceConnectionFactory(host, port);
			}
		}
		
	}
	
	@Bean(name="redisConnectionFactoryReplica1")
	public RedisConnectionFactory redisConnectionFactoryReplica1() {
		
		LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
				.commandTimeout(Duration.ofMillis(3000))
				.shutdownTimeout(Duration.ZERO)
				.build();
	
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(replica1Host,replica1Port);
		if(replica1Password != null && !replica1Password.isEmpty()) {
			redisStandaloneConfiguration.setPassword(RedisPassword.of(replica1Password));
		}
		return new LettuceConnectionFactory(redisStandaloneConfiguration, lettuceClientConfiguration);
		
	}
	
	/*
	@Bean(name="redisConnectionFactoryReplica2")
	public RedisConnectionFactory redisConnectionFactoryReplica2() {
		
		LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
				.commandTimeout(Duration.ofMillis(3000))
				.shutdownTimeout(Duration.ZERO)
				.build();
		
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(replica2Host,replica2Port);
		if(replica2Password != null && !replica2Password.isEmpty()) {
			redisStandaloneConfiguration.setPassword(RedisPassword.of(replica2Password));
		} 
        return new LettuceConnectionFactory(redisStandaloneConfiguration, lettuceClientConfiguration);
	}
	*/
	@Bean
	public RedisMessageListenerContainer redisContainer() {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(this.redisConnectionFactory());

		//Cache Listener Remove 용
		cdto.setRedisContainer(container);
		return container;
	}	
	
	@Bean(name="redisTemplate")
	public RedisTemplate<?, ?> redisTemplate() {
		RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		return redisTemplate;
	}
	

	@Bean(name="redisTemplateReplica1")
	public RedisTemplate<?, ?> redisTemplateReplica1() {
		RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactoryReplica1());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		return redisTemplate;
	}
	/*
	@Bean(name="redisTemplateReplica2")
	public RedisTemplate<?, ?> redisTemplateReplica2() {
		RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactoryReplica2());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		return redisTemplate;
	}
	*/
	@Bean
	public RedisTemplate<?, ?> redisPubsubTemplate() {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
		return redisTemplate;
	}
}
