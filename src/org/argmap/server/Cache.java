package org.argmap.server;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class Cache {
	private static MemcacheService memcacheService = MemcacheServiceFactory
			.getMemcacheService();

	public static void put(String key, Object value) {
		memcacheService.put(key, value);
	}

	public static void putOnlyIfNotPresent(String key, Object value,
			int expireInSeconds) {
		memcacheService.put(key, value, Expiration
				.byDeltaSeconds(expireInSeconds),
				MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
	}

	public static Object get(String key) {
		return memcacheService.get(key);
	}

	public static boolean contains(String key) {
		return memcacheService.contains(key);
	}
}
