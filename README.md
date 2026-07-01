# 多模态自动 MV 生成系统
以聊天智能体的形式，在对话过程中收集用户需求，并据此生成音乐与 MV。

## 功能特性
- 手机号验证、微信扫码登录
- 积分充值、订阅系统，支持动态配置
- 音乐与视频的可视化生成
- Agent 自恢复机制
- 任务数据增量处理，减轻大规模任务对模型的压力
- 用户行为追踪

## 环境要求
- ffmpeg 8.0+
- mysql 9.0+
- java 25+
- redis 7.0+
- uv
- node
- newapi（中转平台）

## 第三方 API 要求

### 文本大模型
> 统一通过 newapi 进行接入，部署平台并配置好模型后，将密钥写入 model-api.guoyan.ak 配置项  
> newapi baseUrl 位置 AssistantRegisterComponent#newApi

- deepseek-v4-pro
- deepseek-v4-flash
- gemini-3.1-pro-preview
- gemini-3.5-flash

### 多模态大模型
> 需要去第三方平台申请对应的 API 密钥，填入 application.yaml 文件 model-api.* 配置项下

#### 模型明细

| 模型名称 | 文档地址 | 对应 model-api.* |
|---------|---------|---------|
| Suno V5 | https://docs.sunoapi.org/cn/suno-api/generate-music | `sunoapi` |
| Mureka（歌曲生成） | https://platform.mureka.cn/docs/api/operations/post-v1-song-generate.html | `mureka` |
| Mureka（纯音乐生成） | https://platform.mureka.cn/docs/api/operations/post-v1-instrumental-generate.html | `mureka` |
| 火山引擎 ASR（语音转字幕） | https://www.volcengine.com/docs/6448/2386124?lang=zh | `volcMediakit` |
| 火山引擎 MediaKit 任务查询 | https://www.volcengine.com/docs/6448/2278532?lang=zh&_vtm_=a106466.b106468.0_0.0_0.0.361_7610611322983646755 | `volcMediakit` |
| doubao-seedream-5-0-lite-260128（图像生成） | https://www.volcengine.com/docs/82379/1541523?lang=zh | `volcFangzhou` |
| gemini-3.1-flash-image-preview（Gemini 图像） | （通过 OmniMaaS 网关调用，无独立文档） | `omnimaas` |
| pixverse/lipsync（唇形同步） | https://replicate.com/pixverse/lipsync/api | `replicate` |
| viduq2（Vidu Q2 参考图生视频，直连官方） | https://platform.vidu.cn/docs/reference-to-video | `vidu` |
| viduq2（Vidu Q2 文生视频，直连官方） | https://platform.vidu.cn/docs/text-to-video | `vidu` |
| viduq2（Vidu Q2 文生视频，OmniMaaS 网关） | https://platform.vidu.cn/docs/text-to-video | `omnimaas` |
| doubao-seedance-2-0-260128（Seedance 2.0） | https://www.volcengine.com/docs/82379/1520757?lang=zh | `volcFangzhou` |
| Seedance 2.0 模型计费 | https://www.volcengine.com/docs/82379/1544106?_vtm_=a106466.b106468.0_0.0_0.0.195_7610611322983646755&lang=zh#83af2aad | `volcFangzhou` |
| doubao-seedance-2-0-fast-260128（Seedance 2.0 Fast） | https://www.volcengine.com/docs/82379/1520757?lang=zh | `volcFangzhou` |
| kling-v3-omni（可灵 V3 Omni） | https://klingai.com/document-api/apiReference/model/OmniVideo | `omnimaas` |

## 使用方法

> 确保环境要求中的依赖已正确安装并启动，且 API 相关属性已配置成功

```shell
# clone 项目
git clone https://github.com/DiffuseFuture/auto_mv.git
```

### 启动后端

1. 执行 [SQL 脚本](./ohyesainext.sql) 文件，初始化表结构

2. 编译运行项目
```shell
cd backend
mvn clean package
java -jar target/ohyesai-next-server-0.0.1-SNAPSHOT.jar
```
### 启动 `music-segment-api`

```shell
cd music-segment-api
uv sync
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 启动前端
```shell
cd frontend
npm install
npm run dev
```

## 启动完成
至此已全部启动完成。请根据 npm 的输出，在浏览器中访问对应地址即可。  

## FAQ

### 自定义模型依赖
所有多模态模型的请求逻辑都在 backend/src/main/java/com/ohyesai/next/component/vlm 目录下，由于每个厂商的模型 API 都不同，如果更换厂商，只需重写对应的方法即可。

注意：重写时请确保方法签名与原方法保持一致，以免影响上层调用