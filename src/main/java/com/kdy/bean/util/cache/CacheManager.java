package com.kdy.bean.util.cache;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheManager {
	private static final Logger log = LoggerFactory.getLogger(CacheManager.class);

	private ConcurrentHashMap<String, ObjectCache> items = new ConcurrentHashMap<String, ObjectCache>();
	
	private CacheManager() {
		constructDefault();
	}
	
	public static CacheManager getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public ObjectCache removeCache(String key) {

		log.debug("cache manager remove all cache!");
		items.get(key).removeAll();
		return items.remove(key);
	}

	public ObjectCache getCache(String key) {

		ObjectCache cache = items.get(key);
		return cache;
	}	
	
	private void constructDefault() {
		if (getCache("com.kdy.bean.util.data.Output.stringCache") == null) {
			items.put("com.kdy.bean.util.data.Output.stringCache", new ObjectCache());
		}
		if (getCache("com.kdy.bean.util.data.Output.getterCache") == null) {
			items.put("com.kdy.bean.util.data.Output.getterCache", new ObjectCache());
		}
		if (getCache("com.kdy.bean.util.data.Output.fieldCache") == null) {
			items.put("com.kdy.bean.util.data.Output.fieldCache", new ObjectCache());
		}
		if (getCache("com.kdy.bean.util.data.Output.serializeCache") == null) {
			items.put("com.kdy.bean.util.data.Output.serializeCache", new ObjectCache());
		}
	}

	private static final class SingletonHolder {
		private static final CacheManager INSTANCE = new CacheManager();
	}
}
