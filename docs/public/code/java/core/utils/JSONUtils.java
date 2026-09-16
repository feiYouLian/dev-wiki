package com.sjbb.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static String writeToString(Object value) throws IOException {
        return MAPPER.writeValueAsString(value);
    }

    public static JsonNode readTree(String content) throws IOException {
        return MAPPER.readTree(content);
    }

    public static <T> T readValue(String content, Class<T> clazz) throws IOException {
        return MAPPER.readValue(content, clazz);
    }

    public static Map<String, Object> readValue(String content) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map = MAPPER.readValue(content, map.getClass());
        return map;
    }

    public static <T> List<T> readValueAsList(String content, Class<T> clazz) throws IOException {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return MAPPER.readValue(content, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    public static <T> T parseResultForData(String result, Class<T> clazz) throws IOException {
        JsonNode jsonNode = MAPPER.readTree(result);
        return MAPPER.readValue(jsonNode.get("data").asText(), clazz);
    }

}
