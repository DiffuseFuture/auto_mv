package com.ohyesai.next.component;

import cn.dev33.satoken.apikey.loader.SaApiKeyDataLoader;
import cn.dev33.satoken.apikey.model.ApiKeyModel;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.ohyesai.next.biz.user.entity.User;
import com.ohyesai.next.biz.user.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

/**
 * sa token 的api key模式
 * <a href="https://sa-token.cc/doc.html#/plugin/api-key">doc</a>
 */
@Slf4j
@Component
@AllArgsConstructor
public class SaApiKeyDataLoaderComponent implements SaApiKeyDataLoader {

    private final UserMapper userMapper;

    // 指定框架不再维护 API Key 索引信息，而是由我们手动从数据库维护
    @Override
    public Boolean getIsRecordIndex() {
        return false;
    }

    // 根据 apiKey 从数据库获取 ApiKeyModel 信息 （实现此方法无需为数据做缓存处理，框架内部已包含缓存逻辑）
    @Override
    public ApiKeyModel getApiKeyModelFromDatabase(String namespace, String apiKey) {
        User user = ChainWrappers.lambdaQueryChain(userMapper)
                .eq(User::getApiKey, apiKey)
                .one();
        if (user == null) {
            log.warn("用户不存在，apiKey: {}", apiKey);
            return null;
        }

        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey(apiKey);
        apiKeyModel.setLoginId(user.getId());
        apiKeyModel.setTitle(user.getNickName());
        apiKeyModel.setCreateTime(user.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        // 过期时间
        apiKeyModel.setExpiresTime(Instant.now().plusSeconds(5 * 60).toEpochMilli());
        return apiKeyModel;

    }
}
