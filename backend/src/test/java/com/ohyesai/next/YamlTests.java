package com.ohyesai.next;

import cn.hutool.core.io.IoUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ohyesai.next.biz.billing.enums.PointsOperation;
import com.ohyesai.next.biz.vio.agent.VioAssistant;
import com.ohyesai.next.biz.vio.bo.ChatSessionTaskPayload;
import com.ohyesai.next.biz.vio.bo.MergeAudioVideoArgs;
import com.ohyesai.next.common.enums.GenderEnum;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.JsonUtil;
import com.ohyesai.next.util.YamlUtil;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.service.output.JsonSchemas;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

//@SpringBootTest
public class YamlTests {

//    public record TaskSOP(String currentIntentName, String dynamicSopContent, String dynamicExamples) {
//    }
//
//    @Data
//    public static class TaskSopC{
//        public String currentIntentName;
//        public String dynamicSopContent;
//        public String dynamicExamples;
//
//    }

    @Test
    void test1() throws JsonProcessingException {
        GenderEnum genderEnum = GenderEnum.MALE;
        String a = switch (genderEnum) {
            case MALE -> "m";
            case FEMALE -> "f";
            case null -> null;
        };
        System.out.println(a);

    }
}
