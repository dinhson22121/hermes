local Worker = {}

function Worker.pick_account(worker_id, account_id, ttl)
    local account_key = Worker.account_key(account_id)
    local current_worker_id = redis.call("GET", account_key)

    if Worker.is_empty(current_worker_id) or current_worker_id == worker_id then
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

    if current_worker_id == worker_id then
        redis.call("DEL", account_key)
        return true
    end

    return false
end

function Worker.add_account(worker_id, account_id)
    redis.call("SADD", Worker.list_account_key(worker_id), account_id)
end

function Worker.remove_account(worker_id, account_id)
    redis.call("SREM", Worker.list_account_key(worker_id), account_id)
end

function Worker.shutdown(worker_id)
    local account_ids = redis.call("SMEMBERS", Worker.list_account_key(worker_id))
    if Worker.is_empty(account_ids) then
        return true
    end

    for _, account_id in ipairs(account_ids) do
        Worker.unpick_account(worker_id, account_id)
    end

    return true
end

function Worker.list_account_key(worker_id)
    return string.format("hermes:list_account:%s", worker_id)
end

function Worker.account_key(account_id)
    return string.format("hermes:account:%s", account_id)
end

function Worker.is_empty(data)
    return data == nil or (type(data) == "boolean" and not data)
end
