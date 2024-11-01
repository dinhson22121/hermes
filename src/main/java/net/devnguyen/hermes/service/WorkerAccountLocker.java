package net.devnguyen.hermes.service;

import lombok.extern.slf4j.Slf4j;
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

    public String accountRedisKey(String accountId) {
        return String.format("hermes:account:%s", accountId);
    }

    public String workerRedisKey(String workerId) {
        return String.format("hermes:list_account:%s", workerId);
    }

    public String luaScript(String script) {
        return String.join("\n", baseScript, script);
    }


    public boolean pickAccount(String workerId, String accountId) {
        int ttl = 30;
        String script = luaScript("""
                local account_key = KEYS[1];
                local worker_key = KEYS[2];
                local account_id = ARGV[1];
                local worker_id = ARGV[2];
                local ttl = tonumber(ARGV[3]);
                
                return Worker.pick_account(worker_id, account_id, ttl);
                """);

        List<Object> lockKeys = new ArrayList<>(List.of(workerRedisKey(workerId), accountRedisKey(accountId)));

        var resultScript = redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        accountId, workerId, String.valueOf(ttl));

        List<String> results = (List<String>) resultScript;

        var ok = results.size() == 2 && results.get(0).equalsIgnoreCase("OK");
        if (!ok) {
            log.info("lua script result: {}", resultScript);
        }
        return ok;
    }

    public void shutdown(String workerId){
        String script = luaScript("""
                local worker_id = ARGV[1];
                
                return Worker.shutdown(worker_id);
                """);

        List<Object> lockKeys = new ArrayList<>(List.of(workerRedisKey(workerId)));

        var resultScript = redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE,
                        script,
                        RScript.ReturnType.MULTI,
                        lockKeys,
                        workerId);

        log.info("lua script result: {}", resultScript);
    }

}
