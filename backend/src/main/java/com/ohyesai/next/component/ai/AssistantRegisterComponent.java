package com.ohyesai.next.component.ai;

import cn.hutool.core.util.StrUtil;
import com.ohyesai.next.biz.vio.agent.VioAssistant;
import com.ohyesai.next.biz.vio.agent.VioAudioTool;
import com.ohyesai.next.biz.vio.agent.VioBaseTool;
import com.ohyesai.next.biz.vio.agent.VioMvTool;
import com.ohyesai.next.biz.vio.dto.VioChatDTO;
import com.ohyesai.next.biz.vio.mapper.ChatSessionMapper;
import com.ohyesai.next.biz.vio.service.VioService;
import com.ohyesai.next.common.properties.ModelApiProperties;
import com.ohyesai.next.component.ai.chat_memory.ChatMemoryStoreImpl;
import com.ohyesai.next.component.ai.chat_memory.mapper.MemoryMessageMapper;
import com.ohyesai.next.util.MiscUtil;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.memory.ChatMemoryService;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * 助手注册
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AssistantRegisterComponent {

    private final String newApi = "https://xxx.com/v1";

    /**
     * Anthropic claude-opus-4-5-20251101
     * <a href="https://platform.claude.com/docs/zh-CN/about-claude/models/overview">claude 不同模型说明 </a>
     *
     * @param modelApiProperties
     * @return
     */
    @Bean
    public StreamingChatModel streamChatModelDeepSeekPro(ModelApiProperties modelApiProperties) {
        return AnthropicStreamingChatModel.builder()
                .apiKey(modelApiProperties.guoyan().ak())
                .baseUrl(newApi)
                .timeout(Duration.ofMinutes(5))
                .modelName("deepseek-v4-pro")
                .maxTokens(384000)
                .returnThinking(true) // 返回思考过程
                .temperature(0.1D)
                .build();
    }

    @Bean
    public ChatModel chatModelDeepSeekFlash(ModelApiProperties modelApiProperties) {
        return AnthropicChatModel.builder()
                .apiKey(modelApiProperties.guoyan().ak())
                .baseUrl(newApi)
                .timeout(Duration.ofMinutes(5))
                .modelName("deepseek-v4-flash")
                .maxTokens(384000)
                .returnThinking(true) // 返回思考过程
                .temperature(0.1D)
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .build();
    }

    @Bean
    public ChatModel chatModelDeepSeekPro(ModelApiProperties modelApiProperties) {
        return AnthropicChatModel.builder()
                .apiKey(modelApiProperties.guoyan().ak())
                .baseUrl(newApi)
                .timeout(Duration.ofMinutes(5))
                .modelName("deepseek-v4-pro")
                .maxTokens(384000)
                .returnThinking(true) // 返回思考过程
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                // https://api-docs.deepseek.com/zh-cn/guides/json_mode
//                .customParameters(Map.of("response_format", Map.of("type", "json_object")))
                .build();
    }

    /**
     * google gemini
     *
     * @return
     */
    @Bean
    public ChatModel chatModelGemini3_1Pro(ModelApiProperties modelApiProperties) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(modelApiProperties.guoyan().ak())
                .timeout(Duration.ofMinutes(5))
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .baseUrl(newApi)
                .modelName("gemini-3.1-pro-preview")
//                .modelName("gemini-3-flash-preview")
                .build();
    }

    @Bean
    public ChatModel chatModelGemini3_5Flash(ModelApiProperties modelApiProperties) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(modelApiProperties.guoyan().ak())
                .timeout(Duration.ofMinutes(5))
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .baseUrl(newApi)
                .modelName("gemini-3.5-flash")
//                .modelName("gemini-3-flash-preview")
                .build();
    }

    @Bean
    public VioAssistant vioAssistantService(@Qualifier("streamChatModelDeepSeekPro") StreamingChatModel streamingChatModel,
                                            ChatSessionMapper chatSessionMapper,
                                            MemoryMessageMapper memoryMessageMapper,
                                            VioAssistant.DeepSeekV4FlashAssistant deepSeekV4FlashAssistant,
                                            VioBaseTool vioBaseTool,
                                            VioMvTool vioMvTool,
                                            VioAudioTool vioAudioTool) {


        ToolProvider vioMvToolProvider = MiscUtil.toolProviderByObject(vioMvTool, toolExecutionRequest -> toolExecutionRequest.invocationParameters().get(VioService.PLAN_ROUTER_TYPE) == VioAssistant.DeepSeekV4FlashAssistant.Type.MV);
        ToolProvider vioAudioToolProvider = MiscUtil.toolProviderByObject(vioAudioTool, toolExecutionRequest -> toolExecutionRequest.invocationParameters().get(VioService.PLAN_ROUTER_TYPE) == VioAssistant.DeepSeekV4FlashAssistant.Type.MUSIC);

        return AiServices.builder(VioAssistant.class)
                .streamingChatModel(streamingChatModel)
//                .executeToolsConcurrently(ProxyExecutors.newVirtualThreadPerTaskExecutor()) // 禁止工具并行 模型经常会返回顺序操作 不能并行执行
                // 幻觉工具策略
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name())
                )
                // 工具参数错处理，返回给模型错误信息
                .toolArgumentsErrorHandler((error, errorContext) ->
                        ToolErrorHandlerResult.text("Something is wrong with tool arguments: " + error.getMessage())
                ).
                toolExecutionErrorHandler((error, errorContext) -> {
                    log.warn("工具执行错误：", error);
                    String errorMessage = StrUtil.blankToDefault(error.getMessage(), error.getClass().getName());
                    // 控制异常信息长度，避免token过长导致模型报错
                    return ToolErrorHandlerResult.text(StrUtil.maxLength(errorMessage, 2000));
                })
                .chatMemoryProvider(memoryId -> {
                            String memoryIdStr = memoryId.toString();
                            if (ChatMemoryService.DEFAULT.equals(memoryIdStr)) {
                                // 不传内存id 的 则不进行存储
                                throw new IllegalArgumentException("必须传入内存id");
                            }

                            ChatMemoryStoreImpl chatMemoryStoreImpl = new ChatMemoryStoreImpl(chatSessionMapper, memoryMessageMapper);

                            return MessageWindowChatMemory.builder()
                                    .id(memoryId)
                                    .maxMessages(300)
                                    .chatMemoryStore(chatMemoryStoreImpl)
                                    // 确保 system prompt 总是存储在消息列表的第一位
                                    // false: 当使用动态system prompt 是,新的system会存储在列表的后面，这不会影响使用但看着别扭
                                    // https://docs.langchain4j.dev/tutorials/chat-memory#special-treatment-of-systemmessage
                                    .alwaysKeepSystemMessageFirst(true)
                                    .build();
                        }
                )
                // 动态 system
                .systemMessageTransformer((systemMessage, context) -> {
                    InvocationParameters invocationParameters = context.invocationParameters();
                    ChatMemoryStoreImpl chatMemoryStoreImpl = new ChatMemoryStoreImpl(chatSessionMapper, memoryMessageMapper);
                    // 获取原始参数
                    VioChatDTO vioChatDTO = invocationParameters.get(VioService.RAW_PARAM);
                    String contentTask = chatMemoryStoreImpl.extractSimpleMessage(context.chatMemoryId(), vioChatDTO.getPrompt(), 20);
                    VioAssistant.DeepSeekV4FlashAssistant.Type type = deepSeekV4FlashAssistant.planRouter(contentTask);

                    invocationParameters.put(VioService.PLAN_ROUTER_TYPE, type);// 存储上下文当前意图

                    String resolveTaskSOP = VioAssistant.DeepSeekV4FlashAssistant.resolveTaskSOP(type);
                    return systemMessage + "\n" + resolveTaskSOP;
                })
                .tools(vioBaseTool)
                .toolProviders(vioMvToolProvider, vioAudioToolProvider)
                .build();
    }


    /**
     * 任务路由助手
     *
     * @param chatModel
     * @return
     */
    @Bean
    public VioAssistant.DeepSeekV4FlashAssistant vioDeepSeekV4FlashAssistantService(@Qualifier("chatModelDeepSeekFlash") ChatModel chatModel) {
        return AiServices.create(VioAssistant.DeepSeekV4FlashAssistant.class, chatModel);
    }

    @Bean
    public VioAssistant.DeepSeekV4ProAssistant vioDeepSeekV4ProAssistantService(@Qualifier("chatModelDeepSeekPro") ChatModel chatModel) {
        return AiServices.create(VioAssistant.DeepSeekV4ProAssistant.class, chatModel);
    }

    /**
     * mv 脚本制作
     */
    @Bean
    public VioAssistant.Gemini3_1ProAssistant vioMvScriptGenerateService(@Qualifier("chatModelGemini3_1Pro") ChatModel chatModel) {
        return AiServices.create(VioAssistant.Gemini3_1ProAssistant.class, chatModel);
    }

    @Bean
    public VioAssistant.Gemini3_5FlashAssistant vioSubtitlesService(@Qualifier("chatModelGemini3_5Flash") ChatModel chatModel) {
        return AiServices.create(VioAssistant.Gemini3_5FlashAssistant.class, chatModel);
    }
}
