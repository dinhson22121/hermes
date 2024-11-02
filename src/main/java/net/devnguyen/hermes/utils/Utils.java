package net.devnguyen.hermes.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class Utils {
    public static ObjectMapper objectMapper = JsonMapper.builder() // or different mapper for other format
            .addModule(new ParameterNamesModule())
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            // and possibly other configuration, modules, then:
            .build();

    static {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public static String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "";
        }
    }
}
