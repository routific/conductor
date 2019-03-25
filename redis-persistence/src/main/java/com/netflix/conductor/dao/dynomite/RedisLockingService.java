package com.netflix.conductor.dao.dynomite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.utils.LockingService;
import com.netflix.conductor.dyno.DynoProxy;
import javax.inject.Inject;

public class RedisLockingService extends BaseDynoDAO implements LockingService {

    private final static String DECIDER_LOCKING_SET = "DECIDER_LOCKING_SET";

    @Inject
    public RedisLockingService(DynoProxy dynoClient, ObjectMapper objectMapper, Configuration config) {
        super(dynoClient, objectMapper, config);
    }

    @Override
    public boolean acquire(String key) {
        String workflowToTaskKey = nsKey(DECIDER_LOCKING_SET);
        Long result = dynoClient.sadd(workflowToTaskKey, key);
        logger.debug("");
        // Jedis returns numeric result 1 if key is not already a member of set and added successfully.
        return result == 1L;
    }

    @Override
    public boolean release(String key) {
        // TODO remove
        // Simulating release failures.
        // Result: Workflow would be stuck forever.
        boolean debug = false;
        if(debug) return true;

        Long result = dynoClient.srem(nsKey(DECIDER_LOCKING_SET), key);
        // Jedis returns numeric result 1 if key is a member of set and removed successfully.
        logger.debug("");
        return result == 1L;
    }
}
