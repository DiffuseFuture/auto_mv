# OhYesAI Next Web

OhYesAI 前端项目（Vue 3 + Vite），用于 AI 音乐与 MV 创作、作品管理、订阅支付与分享展示。
项目支持中英文路由前缀（`/zh`、`/en`），并根据终端能力自动加载桌面端或移动端页面。当前实际上线的是中文版，英文版只有路由架构占位。

> Claude Code 在本仓库工作时以根目录的 `CLAUDE.md` 为唯一规则与上下文来源。本 README 面向人类开发者，介绍项目形态与开发约定，不重复编码规则细节。

---

## 1. 功能概览

- **AI 创作工作台**：SSE 流式对话生成 MV / 音乐内容，支持继续对话、澄清、反馈、版本切换、直接编辑（SUBJECT / SCENE / SCENE_SCRIPT 三类资产）。
- **资源中心**：MV / 音乐作品的列表查询、重命名、删除。
- **分享中心**：生成分享链接 + 分享详情页（PC 复刻原首页 MV 预览弹窗样式）。
- **订阅与积分**：订阅套餐、积分附加包、微信扫码支付、订单状态轮询、积分流水。
- **账号体系**：手机号验证码登录、微信扫码登录、用户信息更新、头像上传、API Key 管理、删除账户。
- **多端适配**：同一路由根据 UA 自动选择 `index.vue`（桌面）或 `mobile.vue`（移动）。

---

## 2. 技术栈

- Vue 3（组合式 API，`<script setup>`）
- Vite 7、Vue Router 4（HTML5 history）、Pinia 3
- Element Plus 2（`main.js` 全局注册）
- Tailwind CSS + SCSS + PostCSS + Autoprefixer
- Axios（统一封装在 `src/utils/request.js`）
- `@microsoft/fetch-event-source`（SSE）
- `@ffmpeg/ffmpeg`（浏览器侧多媒体处理）

纯 JavaScript（无 TypeScript），`jsconfig.json` 提供 `@/*` 路径别名。

---

## 3. 环境要求

- Node.js 18+（建议 LTS）
- npm 9+

---

## 4. 快速开始

```bash
npm install
npm run dev
```

默认端口 `5173`。开发代理在 `vite.config.js` 中：

- 代理前缀：`/ohyesai-next`
- 目标地址：`http://127.0.0.1:5173`

---

## 5. 可用脚本

| 命令 | 说明 |
|---|---|
| `npm run dev` | 开发服务器（先复制 FFmpeg 核心文件） |
| `npm run build` | 生产构建（同样会先复制 FFmpeg） |
| `npm run preview` | 预览构建产物 |
| `npm run copy-ffmpeg` | 单独执行 FFmpeg 核心文件复制脚本 |

无 lint / test / typecheck 脚本。

---

## 6. 环境变量

项目已包含 `.env.development` 与 `.env.production`。关键变量：

- `VITE_API_BASE_URL`：API 基础地址（当前请求走 `/ohyesai-next` 代理前缀）
- `VITE_APP_TITLE` / `VITE_APP_VERSION`：应用标识
- `VITE_ICONFONT_URL`：阿里图标脚本地址

---

## 7. 目录结构

```text
src/
  api/                 接口层（auth / creation / resource / share / tracking）
  components/          通用组件（含 SvgIcon、InviteDialog、mobile/MobileTabBar）
  composables/         通用组合式函数（useLogin / useSeo / useAudioTrimUpload）
  i18n/                多语言（zh-CN、en-US）
  layout/              桌面 / 移动端布局壳 + login-dialog / account-dialog
  router/              路由配置与守卫
  store/               Pinia 状态（user / model / creationEditContext / creationSubjectEdit）
  utils/               工具与请求封装（index / request / audio / baiduTrack）
  views/
    home/              首页（index.vue + mobile.vue + composables/）
    creation/          PC 创作页（index.vue + components/ + composables/）
    creation-mobile/   移动端创作页
    resource/          资源中心（index.vue + mobile.vue + 共享 SCSS）
    subscription/      订阅页（含 PaymentDialog、FAQ；路由名为 subscribe / subscribeFaq）
    share/             分享页（index.vue + mobile.vue + useSharePage 共享逻辑）
    legal/             用户协议 / 隐私政策
```

> 注：`views/subscription/` 是目录名，对外暴露的路由是 `/subscribe` / `/subscribe-faq`（早期重命名，未改文件夹名）。

---

## 8. 路由

### 8.1 主路由（带语言前缀，套在 Layout Shell 内）

| 路径 | 路由名 | 说明 |
|---|---|---|
| `/:lang(en\|zh)/mv` | `home` | 首页 |
| `/:lang(en\|zh)/creation` | `creation` | 创作页 |
| `/:lang(en\|zh)/resource` | `resource` | 资源中心 |
| `/:lang(en\|zh)/policy/:type(terms\|privacy)` | `policy` | 用户协议 / 隐私政策 |

### 8.2 独立路由（不含 Layout）

| 路径 | 路由名 | 说明 |
|---|---|---|
| `/:lang(en\|zh)/subscribe` | `subscribe` | 订阅页 |
| `/:lang(en\|zh)/subscribe-faq` | `subscribeFaq` | 订阅 FAQ |
| `/:lang(en\|zh)/share` | `share` | 分享详情页 |

### 8.3 路由行为

- `/` 自动跳转到当前语言首页（保留 query 参数）。
- 无语言前缀的旧路径（`/mv` / `/creation` / `/resource` / `/subscribe` / `/share`）会重定向到对应语言路径，保留 query。
- 未知路径走通配兜底回首页。
- 全局前置守卫同步 URL 语言到 i18n。
- 全局后置守卫按 `meta.seo` 更新页面 SEO（分享页例外，由 `useShareSeo` 单独写入）。

### 8.4 登录守卫（菜单层）

PC 端 Layout 侧边栏菜单点击 `creation` / `resource` 时，未登录会被拦截弹出登录框（不发生路由跳转）。`home` 不拦截。

订阅页本身允许未登录浏览；但点「订阅」/「购买附加积分」按钮时：
- PC：当前页弹本地 LoginDialog
- 移动端：跳 `/:lang/mv?login=1`，移动端首页消费 `?login=1` query 并自动打开内联登录界面

---

## 9. 账户区与登录入口

### 9.1 PC 端右上角账户区（home / resource / creation）

| 状态 | 元素 |
|---|---|
| 未登录 | 「定价」描边胶囊（次要 CTA，常态 `#C2FF00` 高亮，hover 显 `#C2FF00` 外辉光）+「登录 / 注册」实色 CTA（主） |
| 登录态 | 积分胶囊（hover 弹「订阅/购买附加积分」「积分使用详情」菜单；创作页特殊：弹本会话积分流水）+ 头像（hover 弹用户信息卡：套餐 / V 点 / 升级 / 管理账号 / 退出登录） |

注：home 页这块用 `absolute top-5 right-6`（相对外层滚动容器），随页面向下滚动会一起滚走，避免遮挡瀑布流。

### 9.2 移动端

- 底部 TabBar：首页 / 项目 / 订阅。未登录也能点订阅 Tab 进订阅页。
- 移动端登录是首页的全屏内联界面（`v-if="loginVisible"`），由 `useLogin` composable 驱动。
- 移动端订阅页购买按钮未登录时跳 `?login=1`，由移动端首页 `consumeLoginQuery` 自动打开登录界面。

---

## 10. API 模块

所有请求统一通过 `src/utils/request.js`：

- `code === 0` 视为成功，返回 `data`
- 业务错误 reject 整个 `res`（含 `code/message/data`）
- 网络 / HTTP 错误把人话文案塞到 `error.message`
- 请求层**不弹 toast**，调用方自行决定提示

| 文件 | 职责 |
|---|---|
| `src/api/auth.js` | 登录（手机号验证码 / 微信扫码）、用户信息、头像、API Key |
| `src/api/creation.js` | 创作核心：SSE 对话、澄清、历史消息、文件上传、视频元信息、分镜 / 主体直连编辑与查询、版本切换、脚本行更新、反馈、积分查询、套餐与下单 / 支付状态 |
| `src/api/resource.js` | MV / 音乐资源的查找、重命名、删除 |
| `src/api/share.js` | 分享链接生成、分享详情、邀请码 |
| `src/api/tracking.js` | 行为埋点 `saveUserTracking`、百度 CAPI 转化 `uploadCovertData` |

接口的方法签名、参数与返回值以源码 JSDoc 为准。新增接口请放进对应文件并补 JSDoc。

---

## 11. 请求与鉴权

- **Token 存储**：`localStorage.token`
- **请求拦截器**：自动把 token 写入请求头 `token` 字段
- **响应拦截器**：业务错误 reject `res`，网络 / HTTP 错误把人话文案塞到 `error.message`
- **Token 过期**：通过 `window.dispatchEvent(new Event('token-expired'))` 通知 store，store 清状态并 reload

---

## 12. 多语言与 SEO

### 12.1 i18n

- 路由前缀 `/:lang(en|zh)` 与 i18n locale 双向同步（`router.beforeEach` 完成）。
- 语言包按需懒加载（`src/i18n/messages/{zh-CN,en-US}.js`），第一次切换时 `await ensureLocaleLoaded`。
- UI 文案统一通过 `useI18nText()` / `t(...)`，禁止硬编码中英文字面量。

### 12.2 SEO

**当前实际只上线中文版**，因此 SEO 信号统一向中文版收敛：

- `useSeo` 内部把所有 canonical / og:url 强制归一化到 `/zh/...`（即使用户从 `/en/...` 落地也归因到中文版，避免被搜索引擎视为稀薄重复页）。
- `og:locale` 永远输出 `zh_CN`，不动态切换。
- **不输出 `hreflang` alternate**，避免向爬虫宣告并不存在的英文版本。
- `useShareSeo` 单独处理分享页 SEO（含 JSON-LD VideoObject / MusicRecording）。
- `public/sitemap.xml` 与 `public/robots.txt` 仅列 `/zh/` 路径。

英文版上线后再恢复 hreflang、`og:locale` 动态、sitemap 双语条目。

---

## 13. 行为埋点

### 13.1 内部行为埋点（`saveUserTracking`）

调用 `POST /ohyesai-next/user-tracking/save`，统一带 `platform: 'WEB' | 'H5'`（按 `isMobileClient()` 区分）+ `referer`（来源页，存 `sessionStorage`）。

当前在用的 `target` 枚举（按业务场景）：

| Target | 触发时机 |
|---|---|
| `LOGIN` | 登录成功（手机号 / 微信扫码） |
| `HOME_PAGE` | 首页 PV |
| `ENTER_CREATE_CHAT` | 进入创作页 |
| `HOME_CLICK_UPGRADE` | PC 升级按钮 / 移动端积分详情升级 / 移动端 TabBar 订阅 Tab 点击 |
| `HOME_VIEW_INVITE_RULES` | 打开邀请规则弹窗 |
| `HOME_VIEW_QR_CODE` | 打开客服 / 社群微信扫码 |
| `CREATE_NEW_PROJECT` | 创作页侧栏「+ 新项目」/ 首页「立即创作」（PC + 移动端） |
| `SUBSCRIPTION_CLICK_SUBSCRIBE` | 订阅页点订阅按钮（不论登录态都打，登录态再走支付） |
| `HOME_SHARE_VIEW_PROCESS` | 分享页点「创作过程」 |
| `HOME_SHARE_SHARE` | 分享页点「分享」按钮（复制链接） |
| `CREATE_MV_DOWNLOAD_VIDEO` | 创作页下载 MV 视频 |
| `CREATE_MUSIC_DOWNLOAD_AUDIO` | 创作页下载音乐 |

### 13.2 百度推广 CAPI 转化（`reportBaiduConvert`）

`src/utils/baiduTrack.js` 配合后端 `POST /ohyesai-next/cover-data/upload-covert-data`：

- `FORM_SUCCESS`：登录成功时（手机号 / 微信扫码），由 `userStore.login` / `loginByWechat` 触发
- `SERVICE_SUCCESS`：扫码支付状态轮询成功时，由 `PaymentDialog` 触发

落地 URL（`logidUrl`）抓取仅在 `main.js` 模块顶层 `captureBdVid()` 进行一次，确保拿到的是浏览器真实落地 URL（在 router 改写之前），避免 SPA 路由重写导致 `logidUrl` 与百度系统记录不一致而被 CAPI 拒收。

---

## 14. 状态管理（Pinia）

| store | 职责 |
|---|---|
| `user.js` | token / userInfo / userPlan / pointsBalance / tierName；login / loginByWechat / logout；登录成功触发百度 CAPI `FORM_SUCCESS` |
| `model.js` | 创作模型选择 |
| `creationEditContext.js` | 创作页直连编辑上下文（SUBJECT / SCENE 草稿） |
| `creationSubjectEdit.js` | 主体编辑临时状态 |

Pinia stores 都在 `src/store/`（**不是** `stores`）。

---

## 15. 共享组合式函数

| composable | 职责 |
|---|---|
| `useLogin.js` | 登录业务（PC 弹窗 / 移动端内联共用）：验证码倒计时、表单校验、手机号/微信登录提交、登录成功后 `LOGIN` 埋点 + `reloadFromTop` |
| `useSeo.js` | 提供 `useSeo` / `useShareSeo`，写入 title / meta / canonical / og / JSON-LD |
| `useAudioTrimUpload.js` | 音频剪辑 + 上传一体流程（FFmpeg）|

页面级 composables 见 `src/views/{home,creation}/composables/`。

---

## 16. 编码规范（速查）

完整规则见 `CLAUDE.md`，开发者关心的要点：

1. **简洁、基础、健壮**：能 3 行直白代码就别写 10 行抽象层；不为不可能发生的场景写兜底。
2. **不写类型判断和转换**：后端 Java 强类型，按接口文档信任返回值。禁止 `Array.isArray` / `String(...)` / `Number(...)` 包装后端字段、禁止 `?? ''` 兜底。
3. **样式优先 Tailwind**，Tailwind 表达不了再写 SCSS；SCSS 必须嵌套，默认 `<style lang="scss" scoped>`，需穿透时不带 scoped。
4. **布局主结构用 Flex**（必要时 Grid + Flex），`absolute / fixed` 仅用于角标 / 浮层。
5. **`@/...` 路径别名**，不要用相对路径。
6. **请求层不弹 toast**，调用方决定提示。
7. **SSE 必须挂 `AbortSignal`**，组件卸载时 abort。
8. **fire-and-forget 必须 `.catch`**，禁止 `void promise`。
9. **注释和 UI 文案统一中文**。

---

## 17. 常见问题

| 现象 | 排查 |
|---|---|
| 启动报 FFmpeg 错误 | 确认 `npm install` 已跑、`node_modules/@ffmpeg/core/dist/esm` 存在；可手动 `npm run copy-ffmpeg` |
| 本地接口不通 | 确认前端请求路径以 `/ohyesai-next` 开头，且 `vite.config.js` 代理目标 `dev.ohyesai.com` 可访问 |
| 路由刷新 404 | 部署侧需要 fallback 到 `index.html`（HTML5 history 模式必需） |
| 百度推广 `bd_vid` 丢失 | 见 `src/utils/baiduTrack.js`，已在 `main.js` 模块顶层抓取，跑在 router 重定向之前 |
| 「定价」按钮未登录态显示太暗 | 已统一为 `#C2FF00` 常态高亮 + hover 同色外辉光，与登录按钮 hover 风格一致 |
