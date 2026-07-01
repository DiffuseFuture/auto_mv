package com.ohyesai.next;

import cn.hutool.http.HttpGlobalConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * todo: 分享、查看创作过程 相关接口目前是无需登陆的，导致只要知道啦sessionid就可以查看其他人的信息
 *   虽然sessionid也相当于密码，用户难以知道其他人的sessionid，但还是不够稳妥，最好增加二次校验 只能查看“首页公开数据、已分享数据”
 */
@SpringBootApplication
public class OhyesaiNextServerApplication {

    public static ApplicationContext applicationContext;

    static void main(String[] args) {
        // hutool http超时时间全局设置为30秒，默认为永不超时防止把系统给拖死
        HttpGlobalConfig.setTimeout(30000);
        applicationContext = SpringApplication.run(OhyesaiNextServerApplication.class, args);

    }

}
