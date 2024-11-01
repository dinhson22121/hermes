package net.devnguyen.hermes.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Slf4j
public class LuaScriptLoader {
    private final ResourceLoader resourceLoader;

    public LuaScriptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String loadLuaScript(String scriptPath) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + scriptPath);
        return new String(Files.readAllBytes(Paths.get(resource.getURI())));
    }

    public String worker(){
        try {
            return loadLuaScript("lua-scripts/worker.lua");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
