package com.kdy.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableCaching
public class EhCacheConfig {
	
	@Bean
	public JCacheManagerFactoryBean jCacheManagerFactoryBean() throws Exception{
		JCacheManagerFactoryBean jCacheManagerFactoryBean = new JCacheManagerFactoryBean();
		jCacheManagerFactoryBean.setCacheManagerUri(new ClassPathResource("ehcache.xml").getURI());
		return jCacheManagerFactoryBean;
	}

	@Bean
	public JCacheCacheManager jCacheCacheManager(JCacheManagerFactoryBean jCacheManagerFactoryBean) {
		JCacheCacheManager jCacheCacheManager = new JCacheCacheManager();
		jCacheCacheManager.setCacheManager(jCacheManagerFactoryBean.getObject());
		return jCacheCacheManager;
	}
}
