package com.redislabs.university.RU102J.dao;

import com.redislabs.university.RU102J.api.MeterReading;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamEntryID;

public class FeedDaoRedisImpl implements FeedDao {

    private final JedisPool jedisPool;
    private static final long globalMaxFeedLength = 10000;
    private static final long siteMaxFeedLength = 2440;

    public FeedDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    // Challenge #6
    @Override
	public void insert( MeterReading meterReading ) {
		// START Challenge #6
		try ( Jedis jedis = jedisPool.getResource() ) {
			Pipeline pipeline = jedis.pipelined();

			pipeline.xadd( RedisSchema.getGlobalFeedKey(), StreamEntryID.NEW_ENTRY, meterReading.toMap(), globalMaxFeedLength, true );
			pipeline.xadd( RedisSchema.getFeedKey( meterReading.getSiteId() ), StreamEntryID.NEW_ENTRY, meterReading.toMap(), siteMaxFeedLength, true );

			pipeline.sync();
		}
		// END Challenge #6
    }

    @Override
    public List<MeterReading> getRecentGlobal(int limit) {
        return getRecent(RedisSchema.getGlobalFeedKey(), limit);
    }

    @Override
    public List<MeterReading> getRecentForSite(long siteId, int limit) {
        return getRecent(RedisSchema.getFeedKey(siteId), limit);
    }

    public List<MeterReading> getRecent(String key, int limit) {
        List<MeterReading> readings = new ArrayList<>(limit);
        try (Jedis jedis = jedisPool.getResource()) {
            List<StreamEntry> entries = jedis.xrevrange(key, null,
                    null, limit);
            for (StreamEntry entry : entries) {
                readings.add(new MeterReading(entry.getFields()));
            }
            return readings;
        }
    }
}
