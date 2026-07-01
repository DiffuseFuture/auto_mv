# 多模态自动mv规划生成系统
以聊天智能体的形式在对话过程中收集用户需求生成 music 和 mv。

## feature
- 手机号验证、微信扫码登录
- 积分充值、订阅系统，支持动态配置
- 可视化音乐、视频生成
- agent自恢复机制
- 任务数据增量处理，减轻大型脚本对模型的压力
- 用户行为追踪

## 环境要求
- ffmpeg 8.0+
- mysql 9.0+
- java 25+
- redis 7.0+
- uv
- node
- newapi（中转平台）

## 三方Api要求

### 文本大模型
> 统一通过newapi进行接入，搭建好平台并配置好模型后，将密钥配置到 model-api.guoyan.ak 中  
> newapi baseUrl 位置 AssistantRegisterComponent#newApi

- deepseek-v4-pro
- deepseek-v4-flash
- gemini-3.1-pro-preview
- gemini-3.5-flash

### 多模态大模型
> 需要去第三方平台申请对应的api密钥，填入 application.yaml 文件 model-api.* 配置项下

##### 模型明细

| 模型名称 | 文档地址 | 对应 model-api.* |
|---------|---------|---------|
| Suno V5 | https://docs.sunoapi.org/cn/suno-api/generate-music | `sunoapi` |
| Mureka（歌曲生成） | https://platform.mureka.cn/docs/api/operations/post-v1-song-generate.html | `mureka` |
| Mureka（纯音乐生成） | https://platform.mureka.cn/docs/api/operations/post-v1-instrumental-generate.html | `mureka` |
| 火山引擎 ASR（语音转字幕） | https://www.volcengine.com/docs/6448/2386124?lang=zh | `volcMediakit` |
| 火山 MediaKit 任务查询 | https://www.volcengine.com/docs/6448/2278532?lang=zh&_vtm_=a106466.b106468.0_0.0_0.0.361_7610611322983646755 | `volcMediakit` |
| doubao-seedream-5-0-lite-260128（图像生成） | https://www.volcengine.com/docs/82379/1541523?lang=zh | `volcFangzhou` |
| gemini-3.1-flash-image-preview（Gemini 图像） | （通过 OmniMaaS 网关调用，无独立 doc） | `omnimaas` |
| pixverse/lipsync（唇形同步） | https://replicate.com/pixverse/lipsync/api | `replicate` |
| viduq2（Vidu Q2 参考生视频，直连官方） | https://platform.vidu.cn/docs/reference-to-video | `vidu` |
| viduq2（Vidu Q2 文生视频，直连官方） | https://platform.vidu.cn/docs/text-to-video | `vidu` |
| viduq2（Vidu Q2 文生视频，OmniMaaS 网关） | https://platform.vidu.cn/docs/text-to-video | `omnimaas` |
| doubao-seedance-2-0-260128（Seedance 2.0） | https://www.volcengine.com/docs/82379/1520757?lang=zh | `volcFangzhou` |
| Seedance 2.0 模型计费 | https://www.volcengine.com/docs/82379/1544106?_vtm_=a106466.b106468.0_0.0_0.0.195_7610611322983646755&lang=zh#83af2aad | `volcFangzhou` |
| doubao-seedance-2-0-fast-260128（Seedance 2.0 Fast） | https://www.volcengine.com/docs/82379/1520757?lang=zh | `volcFangzhou` |
| kling-v3-omni（可灵 V3 Omni） | https://klingai.com/document-api/apiReference/model/OmniVideo | `omnimaas` |

## 使用方法

> 确保环境要求安装完成并启动，api相关属性配置成功

```shell
# clone 项目
git clone https://github.com/DiffuseFuture/auto_mv.git
```

### 启动 backend

1. 执行[sql](./ohyesainext.sql)文件，初始化表结构

2. 编译运行项目
```shell
cd backend
mvn clean package
java -jar target/ohyesai-next-server-0.0.1-SNAPSHOT.jar
```
### 启动 music-segment-api

```shell
cd music-segment-api
uv sync
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 启动 frontend
```shell
cd frontend
npm install
npm run dev
```

## 后续步骤
至此已经全部启动完成，控制台根据 npm 的输出访问页面即可。  

## FAQ

### 定制依赖模型
所有多模态模型的请求逻辑都在 backend/src/main/java/com/ohyesai/next/component/vlm 目录下，由于每个产商的模型api都不同，如果更换产商之需要重写对应的方法即可。

注意：重写要确保方法签名与原始相同，这样可以不影响上层调用