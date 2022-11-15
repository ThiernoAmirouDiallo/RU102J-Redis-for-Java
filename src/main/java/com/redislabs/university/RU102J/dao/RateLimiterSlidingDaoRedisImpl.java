package com.redislabs.university.RU102J.dao;

import com.redislabs.university.RU102J.core.KeyHelper;

import java.time.ZonedDateTime;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

public class RateLimiterSlidingDaoRedisImpl implements RateLimiter {

	private final JedisPool jedisPool;
	private final long windowSizeMS;
	private final long maxHits;

	public RateLimiterSlidingDaoRedisImpl( JedisPool pool, long windowSizeMS, long maxHits ) {
		this.jedisPool = pool;
		this.windowSizeMS = windowSizeMS;
		this.maxHits = maxHits;
	}

	// Challenge #7
	@Override
	public void hit( String name ) throws RateLimitExceededException {
		// START CHALLENGE #7
		try ( Jedis jedis = jedisPool.getResource(); Transaction t = jedis.multi() ) {
			long currentMs = ZonedDateTime.now().toInstant().toEpochMilli();

			// make the member unique in order not to replace the score of another one
			String member = String.format( "%s:%s", currentMs, UUID.randomUUID() );
			String key = KeyHelper.getKey( String.format( "limiter:%s:%s:maxHits", windowSizeMS, name ) );
			t.zadd( key, currentMs, member );
			t.zremrangeByScore( key, 0, currentMs - windowSizeMS ); // remove hits before the current sliding window
			Response<Long> hitCount = t.zcard( key );
			t.exec();

			if ( hitCount.get() > maxHits ) {
				throw new RateLimitExceededException();
			}
		}
		// END CHALLENGE #7
	}
}
