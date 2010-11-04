package org.argmap.server;

import java.util.logging.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class MemcacheLock {
	private static MemcacheService memcacheService = MemcacheServiceFactory
			.getMemcacheService();

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(MemcacheLock.class
			.getName());

	private static final String LOCK_PREFIX = "___LOCK::";

	public static void lock(String lockName) {
		// milliseconds to wait until next attempt to get the lock
		int delay = 1;
		while (!memcacheService.put(LOCK_PREFIX + lockName, "",
				Expiration.byDeltaSeconds(3000),
				MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT)) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			// continue waiting longer and longer periods until waiting for 3
			// seconds maximum
			delay = delay * 2 > 3000 ? 3000 : delay * 2;
		}

	}

	public static void unlock(String lockName) {
		memcacheService.delete(LOCK_PREFIX + lockName);
	}
}
