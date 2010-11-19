package org.argmap.server;

import java.util.logging.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class Lock {
	private static MemcacheService memcacheService = MemcacheServiceFactory
			.getMemcacheService();

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Lock.class.getName());

	private static final String LOCK_PREFIX = "___LOCK::";

	private final String lockName;
	private boolean locked;

	private Lock(String lockName) {
		this.lockName = lockName;
		this.locked = false;
	}

	public static Lock getLock(String lockName) {
		return new Lock(lockName);
	}

	public void lock() {
		if (!locked) {
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
				// continue waiting longer and longer periods until waiting for
				// 3
				// seconds maximum
				delay = delay * 2 > 3000 ? 3000 : delay * 2;
			}
			locked = true;
		}
	}

	public void unlock() {
		if (locked) {
			memcacheService.delete(LOCK_PREFIX + lockName);
			locked = false;
		}
	}

	public static Lock getNodeLock(Long nodeID) {
		return Lock.getLock("NODE_ID:" + nodeID);
	}
}
