package com.ohyesai.next.util;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.biz.vio.bo.SubtitleTranscribed;
import com.ohyesai.next.biz.vio.bo.tool.ToolResp;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.service.tool.*;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 一些杂项 util 工具
 */
public class MiscUtil {

    private static final Logger log = LoggerFactory.getLogger(MiscUtil.class);

    public static final int FONT_SIZE_LANDSCAPE = 15;
    public static final int FONT_SIZE_PORTRAIT = 11;
    private static final int SUBTITLE_CJK_LINE_LIMIT = 14;

    /**
     * 重试
     * 当 callable 执行失败并且异常是 cathException 的子类时，会进行重试，否则会抛出原始异常
     * 如果重试超过3次时，会抛出 throwException
     *
     * @param callable
     * @param cathException
     * @param throwException
     * @param <T>
     * @return
     */
    public static <T> T reTry(Callable<T> callable, Class<? extends Exception> cathException, RuntimeException throwException) {
        for (int i = 0; i < 3; i++) {
            try {
                return callable.call();
            } catch (Exception ex) {
                if (cathException.isInstance(ex)) {
                    log.error("重试第 {} 次, 错误信息: {}", i + 1, ex.getMessage());
                } else {
                    throw new BusinessException(CodeEnum.Unknow, ex);
                }
            }
        }
        throw throwException;
    }

    public static <T> T reTry(Callable<T> callable, int retryCount) {
        return reTry(callable, retryCount, ex -> new BusinessException(CodeEnum.Unknow, ex));
    }

    public static <T> T reTry(Callable<T> callable, int retryCount, Function<Exception, RuntimeException> function) {
        for (int i = 0; ; i++) {
            try {
                return callable.call();
            } catch (Exception ex) {
                log.error("重试第 {} 次 ", i + 1, ex);
                if (i >= retryCount) {
                    throw function.apply(ex);
                }
            }
        }
    }

    /**
     * 构造工具返回值
     */
    public static String toolResp(@NonNull String toolResp, @Nullable String guidance) {
        if (StrUtil.isBlank(guidance)) {
            return toolResp;
        }
        // 如果指导信息不为空则按照固定模板拼接
        return XmlUtil.toXml(new ToolResp(toolResp, guidance));
    }

    /**
     * list转map
     */
    public static <K, V, T> Map<K, V> listToMap(List<T> list, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        Map<K, V> map = new HashMap<>();
        for (T t : list) {
            map.put(keyMapper.apply(t), valueMapper.apply(t));
        }
        return map;
    }

    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
//            log.info("通过 X-Real-IP 获取到ip {}", ip);
            return ip.trim();
        }
        ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
//            log.info("通过 X-Forwarded-For 获取到ip {}", ip);
            // 多次反向代理后会有多个IP值，第一个为真实IP
            String[] ips = ip.split(",");
            return ips[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 将 Suno API 的 JSON 响应转换为适合 Agent 阅读的 LRC 格式
     *
     * @return 格式化后的 LRC 字符串
     */
    public static String convertToAgentLrc(JsonNode alignedWordsNode) {
        StringBuilder lrcBuilder = new StringBuilder();
        // 获取 aligned_lyrics 节点

        if (!alignedWordsNode.isArray()) {
            return null;
        }

        for (JsonNode line : alignedWordsNode) {
            // 1. 获取基础信息
            String word = line.path("word").asText();
            double startSeconds = line.path("startS").asDouble(0.0);

            // 3. 格式化时间戳 [mm:ss.xx]
            String timestamp = formatTimestamp(startSeconds);

            // 4. 拼接最终行
            lrcBuilder.append(timestamp)
                    .append(word.replaceAll("\\n", " "))
                    .append("\n");
        }

        return lrcBuilder.toString();
    }

    /**
     * 将秒数转换为 [mm:ss.xx] 格式
     */
    public static String formatTimestamp(double totalSeconds) {
        int minutes = (int) (totalSeconds / 60);
        double seconds = totalSeconds % 60;
        // %02d: 整数补零, %05.2f: 浮点数保留2位小数并补零 (例如 03.45)
        return String.format("[%02d:%06.3f]", minutes, seconds);
    }

    /**
     * 将llm返回字幕内容转换为srt格式
     */
    public static String toSrtContent(List<SubtitleTranscribed.SubtitleLine> lines, int videoWidth, int videoHeight) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            SubtitleTranscribed.SubtitleLine line = lines.get(i);
            sb.append(i + 1).append('\n');
            sb.append(formatSrtTime(line.getStartTime())).append(" --> ").append(formatSrtTime(line.getEndTime())).append('\n');
            sb.append(wrapSubtitleTextIfNeeded(line.getText(), videoWidth, videoHeight)).append('\n');
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 中文占1个视觉大小，英文占0.5个视觉大小
     */
    private static double charWeight(char c) {
        if (c >= '\u4e00' && c <= '\u9fff') {
            return 1.0;
        }
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == ' ')) {
            return 0.5;
        }
        return 0;
    }

    public static String wrapSubtitleTextIfNeeded(String text, int videoWidth, int videoHeight) {
        if (videoWidth > videoHeight) {
            return text;
        }
        double totalCjk = 0;
        for (int i = 0; i < text.length(); i++) {
            totalCjk += charWeight(text.charAt(i));
        }
        if (totalCjk <= SUBTITLE_CJK_LINE_LIMIT) {
            return text;
        }
        double targetCjk = SUBTITLE_CJK_LINE_LIMIT * 2.0 / 3;
        StringBuilder result = new StringBuilder();
        double lineCjk = 0;
        double remainingCjk = totalCjk;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(c);
            double w = charWeight(c);
            if (w > 0) {
                lineCjk += w;
                remainingCjk -= w;
                if (lineCjk >= targetCjk && (remainingCjk + lineCjk) / SUBTITLE_CJK_LINE_LIMIT >= 1) {
                    result.append('\n');
                    lineCjk = 0;
                }
            }
        }
        return result.toString();
    }

    private static String formatSrtTime(double seconds) {
        int totalMs = (int) Math.round(seconds * 1000);
        int h = totalMs / 3_600_000;
        int m = (totalMs % 3_600_000) / 60_000;
        int s = (totalMs % 60_000) / 1000;
        int ms = totalMs % 1000;
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms);
    }

    /**
     * 将毫秒转为小数秒 double
     */
    public static double millisToSeconds(long millis) {
        return (double) millis / 1000;
    }


    /**
     *
     * @param args
     * @param argsName 必须与统计参数的形参名称一致
     * @param clazz
     */
    public static void toolArgsRequireNonNull(Object args, String argsName, Class<?> clazz) {
        if (args != null) {
            if (!clazz.isInstance(args)) {
                log.error("参数与class类型不匹配 argsName: {}", argsName);
                throw new BusinessException(CodeEnum.Unknow);
            }
            return;
        }
        throw new BusinessException(CodeEnum.ParameterError, toolArgsError(clazz, argsName));
    }

    /**
     * 工具参数异常信息封装
     *
     * @param argsName 必须与统计参数的形参名称一致
     */
    public static String toolArgsError(Class<?> clazz, String argsName) {
        JsonSchemaElement schema = JsonSchemaElementUtils.jsonSchemaElementFrom(clazz);

        Map<String, JsonSchemaElement> props = Map.of(argsName, schema);
        JsonObjectSchema rootSchema = JsonObjectSchema.builder()
                .addProperties(props)
                .required(List.of(argsName))
                .build();

        Map<String, Object> map = JsonSchemaElementUtils.toMap(rootSchema);
        return toolResp("参数错误", "参数格式有误，请按此结构重试: %s".formatted(JsonUtil.toJson(map)));
    }

    public static ToolProvider toolProviderByObject(Object objectWithTools, Predicate<ToolProviderRequest> predicate) {
        // 放到外面提高性能 避免重复反射
        List<AiServiceTool> tools = Arrays.stream(objectWithTools.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(method -> {
                    DefaultToolExecutor toolExecutor = DefaultToolExecutor.builder()
                            .object(objectWithTools)
                            .originalMethod(method)
                            .methodToInvoke(method)
                            .wrapToolArgumentsExceptions(true)
                            .propagateToolExecutionExceptions(true)
                            .build();

                    return AiServiceTool.builder()
                            .toolSpecification(ToolSpecifications.toolSpecificationFrom(method))
                            .toolExecutor(toolExecutor)
                            .returnBehavior(method.getAnnotation(Tool.class).returnBehavior())
                            .build();
                }).toList();

        return request -> {
            if (predicate.negate().test(request)) {
                return null;
            }
            return ToolProviderResult.builder().addAll(tools).build();
        };
    }
}
