package net.devnguyen.hermes.service;

import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.HermesApplication;
import net.devnguyen.hermes.dto.ResponseDTO;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WorkerAccountLocker {
    private final RedissonClient redissonClient;
    private String baseScript;

    public WorkerAccountLocker(RedissonClient redissonClient, LuaScriptLoader luaScriptLoader) {
        this.redissonClient = redissonClient;
        this.baseScript = luaScriptLoader.worker();
    }

    private String accountRedisKey(String accountId) {
        return String.format("hermes:account:%s", accountId);
    }

    private String workerRedisKey(String workerId) {
        return String.format("hermes:list_account:%s", workerId);
    }

    private String luaScript(String script) {
        return String.join("\n", baseScript, script);
    }


    public boolean pickAccount(String accountId) {
        int ttl = 30;
        var workerId = HermesApplication.ID;
        String script = luaScript("""
                local worker_id = ARGV[1];
                local account_id = ARGV[2];
                local ttl = tonumber(ARGV[3]);
                
                return Worker.pick_account(worker_id, account_id, ttl);
                """);

        List<Object> lockKeys = new ArrayList<>(List.of(workerRedisKey(workerId), accountRedisKey(accountId)));

        var resultScript = redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        workerId, accountId, String.valueOf(ttl));

        List<String> results = (List<String>) resultScript;

        var ok = results.size() == 2 && results.get(0).equalsIgnoreCase("OK");
        if (!ok) {
            log.info("pickAccount: lua script result: {}", resultScript);
        }
        return ok;
    }

    public void unpickAccount(String accountId) {
        String script = luaScript("""
                local worker_id = ARGV[1];
                local account_id = ARGV[2];
                local ttl = tonumber(ARGV[3]);
                
                return Worker.unpick_account(worker_id, account_id);
                """);
        var workerId = HermesApplication.ID;
        List<Object> lockKeys = new ArrayList<>(List.of(workerRedisKey(workerId), accountRedisKey(accountId)));

        var resultScript = redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        workerId, accountId);

        List<String> results = (List<String>) resultScript;

        var ok = results.size() == 2 && results.get(0).equalsIgnoreCase("OK");
        if (!ok) {
            log.info("unpick result: lua script result: {}", resultScript);
        }
    }

    public void shutdown() {
        String script = luaScript("""
                local worker_id = ARGV[1];
                
                return Worker.shutdown(worker_id);
                """);

        List<Object> lockKeys = new ArrayList<>(List.of(workerRedisKey(HermesApplication.ID)));

        var resultScript = redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        HermesApplication.ID);

        log.info("shutdown: lua script result: {}", resultScript);
    }

    public ResponseDTO<Integer> getPartitionRouterForAccount(String accountId, int defaultPartition, boolean isPickAccount) {
        String script = luaScript("""
                local account_id = ARGV[1];
                local default_partition_id = ARGV[2];
                local is_pick_account = ARGV[3];
                return Worker.kafka_router_partition(account_id, default_partition_id, is_pick_account);
                """);

        var resultScript = (List<String>) redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        new ArrayList<>(),
                        accountId, defaultPartition + "", isPickAccount ? "ok" : "false");

        var ok = resultScript.get(0).equalsIgnoreCase("OK");
        if (!ok) {
            return ResponseDTO.fail(String.join(",", resultScript), -1);
        }

        var partitionId = resultScript.get(1);
        return ResponseDTO.success(Integer.parseInt(partitionId));
    }


    public void kafkaAssignedPartition(String partitionId) {
        String script = luaScript("""
                local worker_id = ARGV[1];
                local partition = ARGV[2];
                
                return Worker.kafka_assigned_partition(worker_id, partition);
                """);

        List<Object> lockKeys = new ArrayList<>();

        redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        HermesApplication.ID,
                        partitionId);
    }

    public void kafkaRevokePartition(String partitionId) {
        String script = luaScript("""
                local worker_id = ARGV[1];
                local partition = ARGV[2];
                
                return Worker.kafka_revoke_partition(worker_id, partition);
                """);

        List<Object> lockKeys = new ArrayList<>();

        redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        HermesApplication.ID, partitionId);
    }

    public void kafkaDelPartition() {
        String script = luaScript("""
                local worker_id = ARGV[1];
                
                return Worker.kafka_worker_shutdown(worker_id);
                """);

        List<Object> lockKeys = new ArrayList<>();

        redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        HermesApplication.ID);
    }


}
