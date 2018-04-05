package com.github.memcached.common.utils;

public abstract class MemcachedConfig {
    public static final String MEMCACHED_SERVER_HOST = "memcached.server.host";

    public static final String MEMCACHED_SERVER_PORT = "memcached.server.port";

    public static final String MEMCACHED_SERVER_POOL_SIZE = "memcached.server.pool.size";

    public static final String MEMCACHED_SERVER_SCHEDULE_PERIOD = "memcached.server.schedule.period";

    ///////////////// 默认值 //////////////////////
    public static final String DEFAULT_MEMCACHED_SERVER_HOST = "127.0.0.1";

    public static final String DEFAULT_MEMCACHED_SERVER_PORT = "8083";

    public static final String DEFAULT_MEMCACHED_SERVER_POOL_SIZE = "10";

    public static final String DEFAULT_MEMCACHED_SERVER_SCHEDULE_PERIOD = "6000";
}
