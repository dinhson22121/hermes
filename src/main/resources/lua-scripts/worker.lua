local Worker = {}

function Worker.pick_operation(worker_id, account_id, operation_id, ttl)
    local account_key = Worker.account_key(account_id)
    local operation_key = Worker.operation_key(account_id, operation_id);
    local account_current_worker_id = redis.call("GET", account_key)
    local operation_current_worker_id = redis.call("GET", operation_key)

    return { "ok", "" }
end

function Worker.pick_account(worker_id, account_id, ttl)
    local account_key = Worker.account_key(account_id)
    local current_worker_id = redis.call("GET", account_key)

    if Worker.is_empty(current_worker_id) or current_worker_id == worker_id then
        local current_ttl = redis.call("TTL", account_key)

        if current_ttl < 0 or current_ttl < ttl or Worker.is_empty(current_ttl) then
            current_ttl = ttl
        end
        redis.call("SET", account_key, worker_id, "EX", ttl)
        Worker.add_account(worker_id, account_id)
        return { "ok", "" }
    else
        Worker.remove_account(worker_id, account_id)
        return { "fail", current_worker_id }
    end
end

function Worker.unpick_account(worker_id, account_id)
    local account_key = Worker.account_key(account_id)
    local current_worker_id = redis.call("GET", account_key)

    --Worker.remove_account(worker_id, account_id)
    if current_worker_id == worker_id then
        redis.call("DEL", account_key)
        return { "ok", "" }
    elseif Worker.is_empty(current_worker_id) then
        return { "ok", "" }
    else
        return { "fail", "current worker is not you" }
    end
end

function Worker.add_account(worker_id, account_id)
    redis.call("SADD", Worker.list_account_key(worker_id), account_id)
    --redis.call("EXPIRE", Worker.list_account_key(worker_id), 3600) -- set ttl 1h
end

function Worker.remove_account(worker_id, account_id)
    redis.call("SREM", Worker.list_account_key(worker_id), account_id)
    --redis.call("EXPIRE", Worker.list_account_key(worker_id), 3600)
end

function Worker.shutdown(worker_id)
    local account_ids = redis.call("SMEMBERS", Worker.list_account_key(worker_id))
    if Worker.is_empty(account_ids) then
        return { "ok", "" }
    end

    for _, account_id in ipairs(account_ids) do
        Worker.unpick_account(worker_id, account_id)
    end

    redis.call("DEL", Worker.list_account_key(worker_id))
    return { "ok", "" }
end

function Worker.list_account_key(worker_id)
    return string.format("hermes:list_account:%s", worker_id)
end

function Worker.account_key(account_id)
    return string.format("hermes:account:%s", account_id)
end

function Worker.operation_key(account_id, operation_id)
    return string.format("hermes:account:operation:%s:%s", account_id, operation_id)
end

-- kafka manager
function Worker.kafka_router_partition(account_id, default_partition_id, is_pick_account)
    local account_key = Worker.account_key(account_id)
    local worker_id = redis.call("GET", account_key)
    if Worker.is_empty(worker_id) then
        if is_pick_account == "ok" then
            -- hold
            Worker.pick_account(worker_id, account_id, 10)
        end

        return { "ok", default_partition_id, "default default_partition_id" }
    end

    local partition_worker_key = Worker.kafka_assigned_partition_key(worker_id)

    local partition_ids = redis.call("SMEMBERS", partition_worker_key)

    -- Check if partition_ids is empty
    if #partition_ids == 0 then
        return { "error", "No partitions assigned to worker", "-1" }
    end

    if is_pick_account == "ok" then
        -- hold
        Worker.pick_account(worker_id, account_id, 10)
    end

    -- Return the first partition if there are partitions available
    return { "ok", partition_ids[1] }
end

function Worker.kafka_assigned_partition(worker_id, partition)
    local key = Worker.kafka_assigned_partition_key(worker_id)
    redis.call("SADD", key, partition)
    redis.call("SET", Worker.kafka_partition_key(partition), worker_id)
    return { "ok", "" }
end

function Worker.kafka_revoke_partition(worker_id, partition)
    local key = Worker.kafka_assigned_partition_key(worker_id)
    redis.call("SREM", key, partition)
    redis.call("DEL", Worker.kafka_partition_key(partition), worker_id)
    return { "ok", "" }
end

function Worker.kafka_worker_shutdown(worker_id)
    local key = Worker.kafka_assigned_partition_key(worker_id)
    redis.call("DEL", key)
    return { "ok", "" }
end

function Worker.kafka_assigned_partition_key(worker_id)
    return string.format("hermes:kafka:assigned_partition:%s", worker_id)
end

function Worker.kafka_partition_key(partition)
    return string.format("hermes:kafka:partition:%s", partition)
end

function Worker.is_empty(data)
    return data == nil or (type(data) == "boolean" and not data)
end
