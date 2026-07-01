/**
 * 常见问题数据：按 locale 提供 hero 文案、tab 标签与分组 Q/A。
 *
 * 数据结构说明：
 * - heroTitle / heroSubtitle：页面顶部标题与副标题
 * - categories: 三个分组（账号及登录 / 操作使用 / 积分相关）
 *   - id：用于锚点跳转的稳定标识
 *   - title：分组标题（左侧绿条 + 文本）
 *   - tabLabel：顶部 Tab 标签文本
 *   - items: 每个分组下的问答列表
 *     - q：问题标题
 *     - a：HTML 字符串，内容为静态内部文案（无用户输入），通过 v-html 渲染
 *
 * zh-CN / en-US 两套独立数据，HTML 结构与链接保持一致，文案各自翻译。
 */
const ZH_CN_FAQ = {
  heroTitle: '常见问题解答',
  heroSubtitle: '账号登录、视频生成、积分使用……所有问题都在这里。',
  categories: [
    {
      id: 'account',
      title: '账号及登录',
      tabLabel: '账号及登录',
      items: [
        {
          q: '1. 是否需要注册账号?',
          a: '<p>无需单独注册，登录即可使用。新用户登录即注册。</p>',
        },
        {
          q: '2. 支持哪些类型的登录方式？',
          a: '<p>浏览器搜索 <a href="https://ohyesai.com" target="_blank" rel="noopener">https://ohyesai.com</a>。电脑端支持微信扫码或手机号验证码登录，移动端支持手机号验证码登录。</p>',
        },
      ],
    },
    {
      id: 'usage',
      title: '操作使用',
      tabLabel: '操作使用',
      items: [
        {
          q: '1. 生成的视频最长多久，最短多久？',
          a: '<p>最长支持5分钟成片，可完整覆盖一首标准歌曲的叙事需求。最短可制作10秒的视频。</p>',
        },
        {
          q: '2. 支持哪些视频模型？底层默认使用哪个模型？',
          a: `
            <p>目前平台支持以下四款视频模型：</p>
            <ul>
              <li>Vidu Q2（45积分/秒）</li>
              <li>Seedance 2.0 Fast（90积分/秒）</li>
              <li>Seedance 2.0（120积分/秒）</li>
              <li>Kling V3 Omni Pro（90积分/秒）</li>
            </ul>
            <p class="faq-answer-paragraph">默认使用 Vidu Q2 模型，您也可以根据需要手动切换至其他模型。</p>
          `,
        },
        {
          q: '3. 支持上传真人照片作为主体参考图吗？',
          a: `
            <ul>
              <li><strong>Vidu 模型</strong>：支持。</li>
              <li><strong>Seedance 模型</strong>：暂不支持用户上传真人图片，只支持通过平台生成的图片作为视频生成的参考图。</li>
              <li><strong>Kling 模型</strong>：支持。</li>
            </ul>
          `,
        },
        {
          q: '4. 视频生成后可以加字幕么?',
          a: '<p>字幕功能已上线，您可以在“合成最终视频”时开启字幕开关。</p>',
        },
        {
          q: '5. 是否支持对口型功能？是否支持动作模仿？',
          a: '<p>对口型功能已上线，您可以在合成最终视频时开启对口型。动作模仿暂不支持。</p>',
        },
        {
          q: '6. 生成过程中可以切换屏幕或关闭电脑？重新打开后任务是否能继续？',
          a: '<p>可以，完全不受影响。生成任务在云端服务器执行，切换屏幕或关闭电脑不会中断任务。重新打开后，您可继续查看任务状态及结果。</p>',
        },
        {
          q: '7. 为什么会出现"聊天的人太多了，请稍后再试"?',
          a: '<p>当前使用人数过多，超过系统承载上限。为保证已连接用户的稳定体验，系统会自动启动限流保护，暂停新的连接。请稍等片刻后重试。一般稍后再试可以恢复正常。避开高峰时段（如工作日下午及晚间）可降低遇到该提示的概率。</p>',
        },
        {
          q: '8. 可以同时进行多个对话么?',
          a: '<p>可以。在同一账号下同时开启多个对话，各对话工作进程独立。</p>',
        },
        {
          q: '9. 是否支持 API 调用？如何调用 API？',
          a: `
            <p>支持。我们开放小龙虾音乐/MV API 的调用。</p>
            <ul>
              <li><a href="https://clawhub.ai/bajie-git/ohyesai-mv" target="_blank" rel="noopener">https://clawhub.ai/bajie-git/ohyesai-mv</a></li>
              <li><a href="https://clawhub.ai/bajie-git/ohyesai-music" target="_blank" rel="noopener">https://clawhub.ai/bajie-git/ohyesai-music</a></li>
            </ul>
            <p class="faq-answer-paragraph">API Key 获取方式：OhYesAI 首页 → 个人头像 → 管理账号 → 个人资料。需要注意的是，若点击重新生成 API Key，当前 API Key 将立即失效，使用该 Key 的服务需要同步更新。</p>
          `,
        },
      ],
    },
    {
      id: 'credits',
      title: '积分相关',
      tabLabel: '积分相关',
      items: [
        {
          q: '1. OhYesAI 可以免费试用吗，可以体验什么功能？',
          a: '<p>可以。新用户首次登录，奖励 2700 积分，可做一条约30~50s的MV视频（含水印），积分有效期 31 天。完整体验从音乐生成到MV生成的全流程创作。适合初次尝鲜和测试效果。您可以先免费体验，再按需订阅会员计划。</p>',
        },
        {
          q: '2. 生成一个 MV 需要多少积分？',
          a: `
            <p>在上传音频文件并完成相关设置后，页面会显示本次生成的积分预估，请以实际页面提示为准。</p>
            <p class="faq-answer-paragraph">生成MV的总积分由三部分构成：<strong>音乐生成积分 + 图片生成积分 + 视频生成积分</strong>。音乐和图片的生成积分根据实际设置动态计算。不同模型、不同时长及素材会直接影响最终消耗。其中，视频生成按所选模型和时长计费，具体模型单价如下：</p>
            <div class="faq-answer-table">
              <table>
                <thead>
                  <tr>
                    <th>生成类型</th>
                    <th>对应模型</th>
                    <th>消耗积分数量</th>
                  </tr>
                </thead>
                <tbody>
                  <tr><td>MV 视频生成</td><td>Vidu Q2（720P）</td><td>45 积分 / 秒</td></tr>
                  <tr><td>MV 视频生成</td><td>Kling V3 Omni Std（720P）</td><td>70 积分 / 秒</td></tr>
                  <tr><td>MV 视频生成</td><td>Seedance 2.0 Fast（720P）</td><td>90 积分 / 秒</td></tr>
                  <tr><td>MV 视频生成</td><td>Seedance 2.0（720P）</td><td>120 积分 / 秒</td></tr>
                  <tr><td>MV 视频生成</td><td>Vidu Q2（1080P）</td><td>90 积分 / 秒</td></tr>
                  <tr><td>MV 视频生成</td><td>Kling V3 Omni Pro（1080P）</td><td>90 积分 / 秒</td></tr>
                  <tr><td>MV 视频生成</td><td>Seedance 2.0（1080P）</td><td>280 积分 / 秒</td></tr>
                  <tr><td>对口型</td><td>PixVerse Lipsync</td><td>30 积分 / 秒</td></tr>
                  <tr><td>音乐生成</td><td>Suno</td><td>50 积分 / 首</td></tr>
                  <tr><td>图片生成</td><td>Seedream-5.0</td><td>28 积分 / 张</td></tr>
                </tbody>
              </table>
            </div>
          `,
        },
        {
          q: '3. 订阅是每个月自动扣款还是手动?',
          a: '<p>月付套餐有效期为一个月，年付套餐有效期为一年。当前两种方式均需要在到期后由用户手动续费，系统不会自动扣款。自动续订的功能开发中，敬请期待。</p>',
        },
        {
          q: '4. 支持哪些支付方式?',
          a: '<p>当前平台仅支持微信扫码支付，暂未开通其他支付渠道。</p>',
        },
        {
          q: '5. 积分使用详情在哪里查看?',
          a: `
            <ul>
              <li><strong>总积分消耗</strong>：将鼠标悬停在右上角积分处，可查看剩余积分及历史消耗记录（含详情与时间）；或将鼠标悬停至个人头像处，点击"管理账号"进入资料页面，再点击"积分"即可查看全部消耗详情。</li>
              <li><strong>单个项目消耗</strong>：进入对应项目的对话页，点击右上角的积分图标，即可查看该对话消耗的积分详情。</li>
            </ul>
          `,
        },
        {
          q: '6. 邀请码的位置与使用',
          a: `
            <p>点击首页左侧下方的礼物图标，点击邀请好友，复制你的邀请链接。当其他用户通过你的邀请链接登录：</p>
            <ul>
              <li>首次完成 MV 视频制作时（7 天之内），你将获得 <strong class="faq-answer-highlight">1500 积分</strong>。（每日最多奖励 5 次）</li>
              <li>首次订阅时（7 天之内），你将获得 <strong class="faq-answer-highlight">3000 积分</strong>。</li>
            </ul>
          `,
        },
      ],
    },
  ],
};

const EN_US_FAQ = {
  heroTitle: 'Frequently Asked Questions',
  heroSubtitle: 'Login, video generation, points usage — find every answer here.',
  categories: [
    {
      id: 'account',
      title: 'Account & Login',
      tabLabel: 'Account & Login',
      items: [
        {
          q: '1. Do I need to register an account?',
          a: '<p>No separate registration required — sign in to start using the service. New users are registered automatically on first login.</p>',
        },
        {
          q: '2. Which login methods are supported?',
          a: '<p>Visit <a href="https://ohyesai.com" target="_blank" rel="noopener">https://ohyesai.com</a> in your browser. Desktop supports WeChat QR code or phone-number verification code login; mobile supports phone-number verification code login.</p>',
        },
      ],
    },
    {
      id: 'usage',
      title: 'Usage',
      tabLabel: 'Usage',
      items: [
        {
          q: '1. What are the maximum and minimum video lengths?',
          a: '<p>The platform supports videos up to 5 minutes long — enough to cover a standard song. The minimum length is 10 seconds.</p>',
        },
        {
          q: '2. Which video models are supported? Which one is used by default?',
          a: `
            <p>Four video models are currently available:</p>
            <ul>
              <li>Vidu Q2 (45 points / second)</li>
              <li>Seedance 2.0 Fast (90 points / second)</li>
              <li>Seedance 2.0 (120 points / second)</li>
              <li>Kling V3 Omni Pro (90 points / second)</li>
            </ul>
            <p class="faq-answer-paragraph">Vidu Q2 is used by default. You can manually switch to another model as needed.</p>
          `,
        },
        {
          q: '3. Can I upload real-person photos as subject reference images?',
          a: `
            <ul>
              <li><strong>Vidu models</strong>: Supported.</li>
              <li><strong>Seedance models</strong>: User-uploaded real-person photos are not yet supported; only platform-generated images can be used as video reference images.</li>
              <li><strong>Kling models</strong>: Supported.</li>
            </ul>
          `,
        },
        {
          q: '4. Can subtitles be added to generated videos?',
          a: '<p>Subtitles are now available — you can turn on the subtitle switch when compositing the final video.</p>',
        },
        {
          q: '5. Are lip-sync and motion mimicry supported?',
          a: '<p>Lip-sync is now available — you can enable it when compositing the final video. Motion mimicry is not yet supported.</p>',
        },
        {
          q: '6. Can I switch screens or shut down my computer during generation? Will the task continue when I come back?',
          a: '<p>Yes, completely unaffected. Generation tasks run on cloud servers — switching screens or shutting down your computer will not interrupt them. When you come back, you can check task status and results.</p>',
        },
        {
          q: '7. Why do I see "Too many people chatting, please try again later"?',
          a: '<p>The current load exceeds the system capacity. To keep connected users stable, the system automatically rate-limits and pauses new connections. Wait a moment and try again — it usually recovers shortly. Avoiding peak hours (weekday afternoons and evenings) reduces the chance of seeing this message.</p>',
        },
        {
          q: '8. Can I run multiple conversations at the same time?',
          a: '<p>Yes. You can open multiple conversations under the same account; each conversation runs independently.</p>',
        },
        {
          q: '9. Is API access supported? How do I call the API?',
          a: `
            <p>Yes. The OhYesAI Music / MV API is open for use:</p>
            <ul>
              <li><a href="https://clawhub.ai/bajie-git/ohyesai-mv" target="_blank" rel="noopener">https://clawhub.ai/bajie-git/ohyesai-mv</a></li>
              <li><a href="https://clawhub.ai/bajie-git/ohyesai-music" target="_blank" rel="noopener">https://clawhub.ai/bajie-git/ohyesai-music</a></li>
            </ul>
            <p class="faq-answer-paragraph">To get your API key: OhYesAI homepage → avatar → Manage account → Profile. Note: regenerating the API key invalidates the current key immediately, and any services using it must be updated.</p>
          `,
        },
      ],
    },
    {
      id: 'credits',
      title: 'Points',
      tabLabel: 'Points',
      items: [
        {
          q: '1. Is there a free trial for OhYesAI? What features can I try?',
          a: '<p>Yes. New users receive 2700 points on first login — enough for about a 30-50 second MV (with watermark). Points are valid for 31 days. You can experience the full flow from music generation to MV generation, perfect for first-time exploration. Try the platform for free, then subscribe to a plan as needed.</p>',
        },
        {
          q: '2. How many points does it take to generate an MV?',
          a: `
            <p>After uploading audio and finishing the relevant settings, the page shows an estimated point cost — refer to the actual on-screen value.</p>
            <p class="faq-answer-paragraph">Total MV points consist of three parts: <strong>music generation points + image generation points + video generation points</strong>. Music and image points are calculated dynamically based on your settings. Different models, durations, and assets directly affect the final cost. Video generation is billed by selected model and duration — per-model rates:</p>
            <div class="faq-answer-table">
              <table>
                <thead>
                  <tr>
                    <th>Generation Type</th>
                    <th>Model</th>
                    <th>Points Cost</th>
                  </tr>
                </thead>
                <tbody>
                  <tr><td>MV video generation</td><td>Vidu Q2 (720P)</td><td>45 points / second</td></tr>
                  <tr><td>MV video generation</td><td>Kling V3 Omni Std (720P)</td><td>70 points / second</td></tr>
                  <tr><td>MV video generation</td><td>Seedance 2.0 Fast (720P)</td><td>90 points / second</td></tr>
                  <tr><td>MV video generation</td><td>Seedance 2.0 (720P)</td><td>120 points / second</td></tr>
                  <tr><td>MV video generation</td><td>Vidu Q2 (1080P)</td><td>90 points / second</td></tr>
                  <tr><td>MV video generation</td><td>Kling V3 Omni Pro (1080P)</td><td>90 points / second</td></tr>
                  <tr><td>MV video generation</td><td>Seedance 2.0 (1080P)</td><td>280 points / second</td></tr>
                  <tr><td>Lip sync</td><td>PixVerse Lipsync</td><td>30 points / second</td></tr>
                  <tr><td>Music generation</td><td>Suno</td><td>50 points / track</td></tr>
                  <tr><td>Image generation</td><td>Seedream-5.0</td><td>28 points / image</td></tr>
                </tbody>
              </table>
            </div>
          `,
        },
        {
          q: '3. Is the subscription auto-renewed monthly or manual?',
          a: '<p>Monthly plans are valid for one month and yearly plans for one year. Both require manual renewal by the user after expiration — the system does not auto-charge. Auto-renewal is in development; stay tuned.</p>',
        },
        {
          q: '4. Which payment methods are supported?',
          a: '<p>Currently only WeChat QR code payment is supported; no other payment channels are available yet.</p>',
        },
        {
          q: '5. Where can I see my points usage details?',
          a: `
            <ul>
              <li><strong>Total points usage</strong>: Hover over the points indicator in the top-right to see remaining points and historical usage (with details and timestamps); or hover over your avatar, click "Manage account" to open the profile page, then click "Points" to see all usage details.</li>
              <li><strong>Per-project usage</strong>: Open the conversation page of the relevant project and click the points icon in the top-right to see point usage details for that conversation.</li>
            </ul>
          `,
        },
        {
          q: '6. Invite code: where to find it and how to use it',
          a: `
            <p>Click the gift icon at the bottom-left of the homepage, then "Invite friends" to copy your invite link. When another user signs in through your invite link:</p>
            <ul>
              <li>If they complete their first MV (within 7 days), you get <strong class="faq-answer-highlight">1500 points</strong>. (Up to 5 rewards per day)</li>
              <li>If they subscribe for the first time (within 7 days), you get <strong class="faq-answer-highlight">3000 points</strong>.</li>
            </ul>
          `,
        },
      ],
    },
  ],
};

export const FAQ_MAP = {
  'zh-CN': ZH_CN_FAQ,
  'en-US': EN_US_FAQ,
};
