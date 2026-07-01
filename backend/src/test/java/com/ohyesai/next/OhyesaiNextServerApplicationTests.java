package com.ohyesai.next;

import com.ohyesai.next.biz.vio.agent.VioAssistant;
import com.ohyesai.next.biz.vio.bo.AsrPatchResult;
import com.ohyesai.next.biz.vio.bo.mvscript.Script;
import com.ohyesai.next.common.enums.AspectRatio;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.enums.Resolution;
import com.ohyesai.next.common.exception.BusinessException;
import com.ohyesai.next.component.*;
import com.ohyesai.next.component.vlm.VlmLipSyncComponent;
import com.ohyesai.next.component.vlm.VlmAudioComponent;
import com.ohyesai.next.component.vlm.VlmVideoComponent;
import com.ohyesai.next.util.CvUtil;
import com.ohyesai.next.util.JsonUtil;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
class OhyesaiNextServerApplicationTests {

}
