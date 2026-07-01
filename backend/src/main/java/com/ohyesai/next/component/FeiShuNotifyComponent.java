package com.ohyesai.next.component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.ohyesai.next.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class FeiShuNotifyComponent {

    private final String webHookUrl;

    private final RestClient restClient;

    public FeiShuNotifyComponent(@Value("${feishu-webhook}") String webHookUrl, RestClient restClient) {
        this.webHookUrl = webHookUrl;
        this.restClient = restClient;
    }

    public void sendNotify(String title, String content) {
        String jsonStr = """
                {
                    "msg_type": "post",
                    "content": {
                        "post": {
                            "zh_cn": {
                                "title": %s,
                                "content": [
                                    [
                                      {
                                          "tag": "text",
                                          "text": %s
                                      }
                                    ]
                                ]
                            }
                        }
                    }
                }
                """.formatted(TextNode.valueOf(title), TextNode.valueOf(content));

        send(jsonStr);
    }

    private void send(String jsonStr) {
        try {
            restClient.post()
                    .uri(webHookUrl)
                    .body(JsonUtil.readTree(jsonStr))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("飞书通知发送失败", e);
        }
    }

    /**
     * 发送消息 at all
     */
    public void sendNotifyAtAll(String title, String content) {
        String jsonStr = """
                {
                    "msg_type": "post",
                    "content": {
                        "post": {
                            "zh_cn": {
                                "title": %s,
                                "content": [
                                    [
                                      {
                                          "tag": "text",
                                          "text": %s
                                      },
                                      {
                                              "tag": "at",
                                              "user_id": "all",
                                              "user_name": "所有人"
                                      }
                                    ]
                                ]
                            }
                        }
                    }
                }
                """.formatted(TextNode.valueOf(title), TextNode.valueOf(content));

        send(jsonStr);
    }
}
