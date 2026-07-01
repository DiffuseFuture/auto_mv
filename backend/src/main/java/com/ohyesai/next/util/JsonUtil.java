package com.ohyesai.next.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class JsonUtil {

    public final static ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 忽略未知属性
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.registerModule(new JavaTimeModule());
    }

    /**
     * 解析json
     */
    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("json 解析失败 {}", json, e);
            throw new BusinessException(e);
        }
//        return MissingNode.getInstance();
    }

    public static JsonNode value2Tree(Object obj) {
        return MAPPER.valueToTree(obj);
    }

    public static <T> List<T> toList(String json, Class<T> clazz) {
        try {
            JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, clazz);
            return MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("json 解析失败：{}", json, e);
        }
        return null;
    }

    public static <T> List<T> toList(JsonNode node, Class<T> clazz) {
        try {
            JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, clazz);
            return MAPPER.treeToValue(node, javaType);
        } catch (JsonProcessingException e) {
            log.error("json 解析失败：{}", node, e);
        }
        return null;
    }

    public static JsonNode object2Tree(Object obj) {
        return MAPPER.valueToTree(obj);
    }

    public static <T> T tree2Object(JsonNode node, Class<T> clazz) {
        try {
            return MAPPER.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            log.error("json 解析失败：{}", node, e);
            throw new BusinessException(CodeEnum.Unknow, e);
        }
    }

    public static <T> List<T> tree2ListObject(JsonNode node, Class<T> clazz) {
        try {
            JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, clazz);
            return MAPPER.treeToValue(node, javaType);
        } catch (JsonProcessingException e) {
            log.error("json 解析失败：{}", node, e);
            throw new BusinessException(CodeEnum.Unknow, e);
        }
    }

    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("json 解析失败：{}", json, e);
            throw new BusinessException(CodeEnum.Unknow, e);
        }
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("json 解析失败：{}", object, e);
            throw new BusinessException(CodeEnum.Unknow, e);
        }
    }

    public static ObjectNode object() {
        return MAPPER.createObjectNode();
    }

    public static ArrayNode array() {
        return MAPPER.createArrayNode();
    }


    /**
     * 判断字符串是否为合法json
     */
    public static boolean isJson(String json) {
        try {
            MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
