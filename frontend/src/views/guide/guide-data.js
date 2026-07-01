/**
 * 使用指南静态数据：按 locale 提供 hero / 章节 / 步骤 等所有文案。
 * 中文文案严格对照 ohyesai-guide-v1/index.html，不做任何措辞改动。
 * 英文版独立翻译，保持 HTML 内联标签（<strong>/<em>/<code>）结构与中文版一致。
 */

/** 21 张教程截图的 alt 文案——img.alt 直接用，搜索引擎按 alt 索引内容。 */
const ZH_IMAGE_ALTS = {
  1: 'OhYesAI 视频模型选择界面',
  2: 'OhYesAI 模型切换操作',
  3: 'OhYesAI 画面比例设置界面',
  4: 'OhYesAI 本地上传音频界面',
  5: 'OhYesAI 对话框输入生成音乐需求',
  6: 'OhYesAI AI 生成音乐结果展示',
  7: 'OhYesAI 选择音乐版本',
  8: 'OhYesAI 点击制作 MV 按钮',
  9: 'OhYesAI 上传主体参考图界面',
  10: 'OhYesAI 输入视觉风格提示词',
  11: 'OhYesAI 视觉参考图确认界面',
  12: 'OhYesAI 编辑单张参考图',
  13: 'OhYesAI 输入"确认并继续"推进流程',
  14: 'OhYesAI 分镜脚本规划界面',
  15: 'OhYesAI 编辑单条分镜描述',
  16: 'OhYesAI 发送"确认并生成"指令',
  17: 'OhYesAI 分镜视频审阅界面',
  18: 'OhYesAI 精细化编辑分镜弹窗',
  19: 'OhYesAI MV 下载界面',
  20: 'OhYesAI 资源中心分享作品界面',
  21: 'OhYesAI 作品一键分享界面',
  22: 'OhYesAI 字幕与智能口型设置界面',
};

const EN_IMAGE_ALTS = {
  1: 'OhYesAI video model selection panel',
  2: 'OhYesAI model switching operation',
  3: 'OhYesAI aspect ratio settings',
  4: 'OhYesAI local audio upload',
  5: 'OhYesAI prompt for AI music generation',
  6: 'OhYesAI AI-generated music results',
  7: 'OhYesAI music version selection',
  8: 'OhYesAI Make MV button',
  9: 'OhYesAI subject reference image upload',
  10: 'OhYesAI visual style prompt input',
  11: 'OhYesAI subject reference confirmation',
  12: 'OhYesAI single reference image editor',
  13: 'OhYesAI "confirm and continue" prompt',
  14: 'OhYesAI storyboard planning view',
  15: 'OhYesAI edit single scene description',
  16: 'OhYesAI "confirm and generate" prompt',
  17: 'OhYesAI scene video review',
  18: 'OhYesAI fine-tune scene editing dialog',
  19: 'OhYesAI MV download page',
  20: 'OhYesAI resource center sharing',
  21: 'OhYesAI one-click work sharing',
  22: 'OhYesAI subtitle and lip-sync settings',
};

const ZH_CN_GUIDE = {
  imageAlts: ZH_IMAGE_ALTS,
  toc: [
    {id: 'hero', title: '使用指南'},
    {id: 's1', title: '一、开机准备'},
    {id: 's1-1', title: '1.1 模型选择指南', sub: true},
    {id: 's1-2', title: '1.2 画面比例设置', sub: true},
    {id: 's2', title: '二、准备创作'},
    {id: 's2-1', title: '2.1 上传或生成音乐', sub: true},
    {id: 's2-2', title: '2.2 上传主体参考图', sub: true},
    {id: 's3', title: '三、正式开工'},
    {id: 's3-1', title: '3.1 视觉风格确立', sub: true},
    {id: 's3-2', title: '3.2 确认主体参考图', sub: true},
    {id: 's3-3', title: '3.3 确认分镜脚本', sub: true},
    {id: 's3-4', title: '3.4 审阅分镜视频', sub: true},
    {id: 's3-5', title: '3.5 字幕与智能口型', sub: true},
    {id: 's3-6', title: '3.6 一键成片与下载', sub: true},
  ],

  hero: {
    titlePrefix: 'OhYesAI',
    titleEm: '使用指南',
    intro: '欢迎使用 OhYesAI —— 您的专属 AI MV 创作智能体。无论是原创音乐人还是普通用户，只需一段音频或一个想法，即可零门槛一键生成高质量 MV。',
  },

  s1: {
    title: '一、开机准备：选定你的创作"引擎"与"画布"',
    intro: '在开始创作前，了解底层的模型与设定，能让你的作品事半功倍。',
  },
  s1_1: {
    num: '1.1',
    title: '模型选择指南',
    desc: 'OhYesAI 平台全面支持多种行业顶尖的视频生成模型，你可以根据对画质和积分的要求自由切换：',
    operationHtml: '<strong>操作</strong>：在会话界面的左下角，点击模型图标即可随时切换。',
  },
  models: [
    {tag: '推荐 / 默认', name: 'Vidu Q2', desc: '性价比之王。光影细腻，适合绝大多数风格。', prices: [{res: '720p', credit: 45}, {res: '1080p', credit: 90}]},
    {tag: '运镜大师', name: 'Kling V3 Omni Pro', desc: '擅长处理大范围肢体动作和写实场景。', prices: [{res: '720p', credit: 70}, {res: '1080p', credit: 90}]},
    {tag: '极速预览', name: 'Seedance 2.0 Fast', desc: '适合快速迭代创意，验证想法。', prices: [{res: '720p', credit: 90}]},
    {tag: '画质巅峰', name: 'Seedance 2.0', desc: '细节表现力极强，适合追求极致成片质感的创作者。', prices: [{res: '720p', credit: 120}, {res: '1080p', credit: 280}]},
  ],
  pointsUnit: '积分/秒',
  s1_2: {
    num: '1.2',
    title: '画面比例设置',
    desc: '在对话框发送指令即可设定：',
  },
  ratios: [
    {title: '横屏（16:9）', desc: '适合传统音乐 MV、横版电影感视频。'},
    {title: '竖屏（9:16）', desc: '适合抖音等短视频宣发。'},
  ],

  s2: {title: '二、准备创作：配置你的素材'},
  s2_1: {
    num: '2.1',
    title: '上传或生成音乐',
    desc: '音乐是 MV 的灵魂，平台支持以下两种方式获取音频：',
    uploadHtml: '<strong>本地上传</strong>：支持 <code>MP3</code> / <code>WAV</code> / <code>M4A</code> 等格式，文件大小无限制，时长最多支持 6 分钟。',
    aiGenHtml: '<strong>AI 智能生成</strong>：直接在对话框输入需求（例如："<em>生成一首欢快的歌曲，电子流行风格</em>"），系统会为你创作两版音乐，挑一版你喜欢的点击"制作 MV"即可。',
    tipHtml: '<strong>省钱 Tips</strong>：系统支持生成的视频时长为 10 秒至 5 分钟。为了快速测试效果并节省积分，首次尝试时建议在截取页面<strong>裁剪出一段 30s～60s 的音乐片段</strong>进行制作。',
  },
  s2_2: {
    num: '2.2',
    title: '上传主体参考图',
    descHtml: '上传参考图可固定 MV 中的特定元素（最多支持 6 张）。你可以根据需要设定图片类型：<strong>人物、服装、环境、道具或视觉风格</strong>。',
    notice: '为了保证 AI 识别的精准度，请注意以下两点：',
    rule1Html: '<strong>单张图片内只有一人</strong>：确保参考图中只有单独的主角，背景尽量干净、面部清晰无遮挡，这样能最大程度维持后续分镜的角色一致性。',
    rule2Html: '<strong>无图片也能生成</strong>：如果没有准备参考图也不用担心，系统会完全根据你的文字描述自动生成匹配的角色与环境。',
  },

  s3: {
    title: '三、正式开工：六步掌握全流程',
    introHtml: 'OhYesAI 采用"<strong>对话式协同</strong>"工作流，你可以全程把控创意。',
  },
  steps: [
    {
      id: 's3-1',
      timestamp: '01',
      title: '视觉风格确立',
      bodyHtml: '在对话框输入并发送你想要的画面视觉风格，如"<em>动漫风格</em>"、"<em>写实风格</em>"或"<em>唯美梦幻</em>"等提示词。',
      imageIdx: [10],
    },
    {
      id: 's3-2',
      timestamp: '02',
      title: '确认主体参考图',
      bodyHtml: '在此阶段，系统会根据你上传的<strong>音乐、参考图以及输入的提示词</strong>，智能规划 MV 视频全程所需要的人物形象、关键道具与环境背景，并渲染出视觉参考图供你确认。',
      substeps: [
        {icon: '🔍', textHtml: '<strong>查看与精修</strong>：点击图片中央可放大查看。若对某张图不满意，点击图片右上角的"编辑"按钮，即可单独上传本地图片或直接修改提示词，重新生成 V2 版本。'},
        {icon: '✅', textHtml: '<strong>推进下一步</strong>：如果对当前设计的角色和场景满意，在对话框输入"<em>确认并继续</em>"并发送。'},
      ],
      imageIdx: [11, 12, 13],
    },
    {
      id: 's3-3',
      timestamp: '03',
      title: '确认分镜脚本',
      badge: '关键步骤 ✨',
      bodyHtml: '系统会根据音乐的节奏和歌词，自动规划出详细的分镜脚本（包括时间戳和镜头画面描述，此步骤不消耗积分）。',
      alertHtml: '<strong>为什么这一步最关键</strong>：分镜脚本是视频的蓝图。在开始生成视频前，请务必仔细审阅分镜文字。在这里把画面对齐，能帮你避免后面生成出废片，省下大量积分。',
      alertType: 'warning',
      substeps: [
        {icon: '✏️', textHtml: '<strong>如何修改</strong>：觉得某个镜头的画面安排不对，可以直接在对话框里发消息让 AI 修改；或者点击该分镜框右上角的"编辑"按钮直接动手改写描述并保存。'},
        {icon: '▶️', textHtml: '<strong>推进</strong>：发送"<strong>确认并生成</strong>"。'},
      ],
      imageIdx: [14, 15, 16],
    },
    {
      id: 's3-4',
      timestamp: '04',
      title: '审阅分镜视频',
      bodyHtml: '系统会开始逐镜生成视频片段。在此阶段，您可以针对每一个镜头的表现进行审阅与精修，确保每一秒画面都符合预期。',
      methods: [
        {
          title: '方式一：对话式快速修改',
          desc: '如果您觉得某个镜头的整体感觉不对，可以直接在主界面的对话框输入指令。',
          note: '示例："第 3 个镜头的运镜再慢一点"、"让主角在第 5 个镜头里笑一下"。',
        },
        {
          title: '方式二：精细化弹窗编辑',
          recommendTag: '推荐 ✨',
          desc: '点击单个分镜视频右上角的"编辑分镜"按钮，即可打开深度定制窗口。',
        },
      ],
      methodsHint: '精细化弹窗支持：',
      bulletsHtml: [
        '<strong>图片引用与重置</strong>：您可以点击"+"号上传新的参考图，或通过 <code>@图片1</code>、<code>@图片2</code> 的方式在提示词中精准指引 AI 调用特定的视觉素材。',
        '<strong>提示词改写</strong>：直接修改文本框内的描述。您可以细化光影、动作或构图细节，赋予镜头更精准的灵魂。',
        '<strong>独立切换模型</strong>：如果当前模型（如 Vidu Q2）对该动作的处理不够完美，您可以尝试在弹窗右下角切换为 Kling V3 Omni Pro 或 Seedance 2.0，为该镜头更换更强的"大脑"。',
      ],
      imageIdx: [17, 18],
    },
    {
      id: 's3-5',
      timestamp: '05',
      title: '字幕与智能口型（最后一步 ✨）',
      bodyHtml: '当所有分镜视频都调整满意后，在导出前，您可以根据需求开启：',
      substeps: [
        {icon: '📝', textHtml: '<strong>歌词字幕</strong>：打开此开关，系统将根据音频内容，自动在成片中嵌入歌词字幕，省去后期剪辑麻烦。'},
        {icon: '💡', textHtml: '<strong>字幕校准</strong>：如果发现系统生成的字幕与音频时间轴没对齐，您无需担心。直接在对话框告诉 AI："字幕没对准，请重新校准"。此校准操作完全免费，不消耗任何积分。'},
        {icon: '👄', textHtml: '<strong>智能口型同步</strong>：若您的 MV 中有人物正面唱歌的镜头，开启此功能后，AI 将自动识别并让画面中人物的口型与歌词音频精准匹配，大幅提升成片的写实感与代入感。'},
      ],
      imageIdx: [22],
    },
    {
      id: 's3-6',
      timestamp: '06',
      title: '一键成片与下载',
      bulletGroups: [
        {html: '<strong>下载保存</strong>：渲染完成后，点击右上角"下载"即可保存视频。', imageIdx: [19, 20]},
        {html: '<strong>资源分享</strong>：您生成的音乐和 MV 均可在侧边栏【资源】版块中查看，并支持一键分享给好友展示您的创作过程。', imageIdx: [21]},
      ],
    },
  ],
};

const EN_US_GUIDE = {
  imageAlts: EN_IMAGE_ALTS,
  toc: [
    {id: 'hero', title: 'Guide'},
    {id: 's1', title: '1. Get Ready'},
    {id: 's1-1', title: '1.1 Model selection', sub: true},
    {id: 's1-2', title: '1.2 Aspect ratio', sub: true},
    {id: 's2', title: '2. Prepare assets'},
    {id: 's2-1', title: '2.1 Music: upload or generate', sub: true},
    {id: 's2-2', title: '2.2 Upload subject references', sub: true},
    {id: 's3', title: '3. Start creating'},
    {id: 's3-1', title: '3.1 Visual style', sub: true},
    {id: 's3-2', title: '3.2 Confirm subjects', sub: true},
    {id: 's3-3', title: '3.3 Confirm storyboard', sub: true},
    {id: 's3-4', title: '3.4 Review scenes', sub: true},
    {id: 's3-5', title: '3.5 Subtitles & lip-sync', sub: true},
    {id: 's3-6', title: '3.6 Export & share', sub: true},
  ],

  hero: {
    titlePrefix: 'OhYesAI',
    titleEm: 'User Guide',
    intro: 'Welcome to OhYesAI — your dedicated AI MV creation agent. Whether you are a music creator or a casual user, a single audio clip or idea is enough to one-click generate a high-quality MV.',
  },

  s1: {
    title: '1. Get ready: pick your "engine" and "canvas"',
    intro: 'Understanding the underlying models and settings before you start will save time and credits.',
  },
  s1_1: {
    num: '1.1',
    title: 'Model selection',
    desc: 'OhYesAI supports several industry-leading video generation models. Switch freely based on your quality and points needs:',
    operationHtml: '<strong>How</strong>: Click the model icon at the bottom-left of the chat to switch anytime.',
  },
  models: [
    {tag: 'Recommended / Default', name: 'Vidu Q2', desc: 'Best value. Refined lighting, fits most styles.', prices: [{res: '720p', credit: 45}, {res: '1080p', credit: 90}]},
    {tag: 'Motion master', name: 'Kling V3 Omni Pro', desc: 'Strong at large body motion and realistic scenes.', prices: [{res: '720p', credit: 70}, {res: '1080p', credit: 90}]},
    {tag: 'Fast preview', name: 'Seedance 2.0 Fast', desc: 'Great for quick creative iteration.', prices: [{res: '720p', credit: 90}]},
    {tag: 'Top quality', name: 'Seedance 2.0', desc: 'Outstanding detail, for those chasing peak quality.', prices: [{res: '720p', credit: 120}, {res: '1080p', credit: 280}]},
  ],
  pointsUnit: 'points / sec',
  s1_2: {
    num: '1.2',
    title: 'Aspect ratio',
    desc: 'Just send a message in the chat to set it:',
  },
  ratios: [
    {title: 'Landscape (16:9)', desc: 'Classic music MVs and cinematic horizontal videos.'},
    {title: 'Portrait (9:16)', desc: 'Short videos for platforms like TikTok / Douyin.'},
  ],

  s2: {title: '2. Prepare your assets'},
  s2_1: {
    num: '2.1',
    title: 'Music: upload or generate',
    desc: 'Music is the soul of an MV. Two ways to get audio:',
    uploadHtml: '<strong>Local upload</strong>: Supports <code>MP3</code> / <code>WAV</code> / <code>M4A</code> and more. No file size limit, up to 6 minutes.',
    aiGenHtml: '<strong>AI generation</strong>: Type your request in the chat (e.g. "<em>generate an upbeat song in electronic pop style</em>"). The system will create two versions; pick one you like and click "Make MV".',
    tipHtml: '<strong>Save credits</strong>: Generated videos can be 10 seconds to 5 minutes. For fast tests and saving points, <strong>trim a 30s–60s clip</strong> on the trim page for your first try.',
  },
  s2_2: {
    num: '2.2',
    title: 'Upload subject references',
    descHtml: 'Upload reference images to lock down specific MV elements (up to 6). Tag each as: <strong>person, clothing, environment, prop, or visual style</strong>.',
    notice: 'For accurate AI recognition, please note:',
    rule1Html: '<strong>One person per image</strong>: Keep a single subject, clean background, clear face — this preserves character consistency across scenes.',
    rule2Html: '<strong>References are optional</strong>: Without references, the system generates characters and environments purely from your text description.',
  },

  s3: {
    title: '3. Start creating: 6 steps to the full workflow',
    introHtml: 'OhYesAI uses a "<strong>conversational co-creation</strong>" workflow. You stay in control of the creative direction throughout.',
  },
  steps: [
    {
      id: 's3-1',
      timestamp: '01',
      title: 'Define visual style',
      bodyHtml: 'Send your desired visual style in the chat, e.g. "<em>anime style</em>", "<em>photorealistic</em>", or "<em>dreamy aesthetic</em>".',
      imageIdx: [10],
    },
    {
      id: 's3-2',
      timestamp: '02',
      title: 'Confirm subject references',
      bodyHtml: 'Based on your <strong>music, reference images, and prompt</strong>, the system plans the characters, key props, and environments needed for the whole MV and renders visual references for your review.',
      substeps: [
        {icon: '🔍', textHtml: '<strong>Inspect & refine</strong>: Click an image center to zoom in. If you are not satisfied, click the "Edit" button in the top-right corner to upload a new local image or change the prompt and regenerate a V2.'},
        {icon: '✅', textHtml: '<strong>Move on</strong>: If you are happy with the characters and scenes, type "<em>confirm and continue</em>" in the chat and send.'},
      ],
      imageIdx: [11, 12, 13],
    },
    {
      id: 's3-3',
      timestamp: '03',
      title: 'Confirm the storyboard',
      badge: 'Key step ✨',
      bodyHtml: 'The system automatically plans a detailed storyboard from the music\'s rhythm and lyrics (with timestamps and scene descriptions; this step costs no points).',
      alertHtml: '<strong>Why this step matters most</strong>: The storyboard is the blueprint of the video. Carefully review the scene texts before generation — aligning visuals here prevents wasted generations and saves a lot of points.',
      alertType: 'warning',
      substeps: [
        {icon: '✏️', textHtml: '<strong>How to edit</strong>: If a scene\'s framing is wrong, ask the AI directly in the chat to revise; or click "Edit" in the top-right of the scene block to rewrite the description yourself and save.'},
        {icon: '▶️', textHtml: '<strong>Move on</strong>: Send "<strong>confirm and generate</strong>".'},
      ],
      imageIdx: [14, 15, 16],
    },
    {
      id: 's3-4',
      timestamp: '04',
      title: 'Review scene videos',
      bodyHtml: 'The system generates videos scene by scene. You can review and refine each shot to make sure every second meets expectations.',
      methods: [
        {
          title: 'Option 1: Quick chat-based edits',
          desc: 'If a scene feels off overall, simply send instructions in the main chat.',
          note: 'Examples: "slow down the camera motion in scene 3", "make the protagonist smile in scene 5".',
        },
        {
          title: 'Option 2: Fine-tuned dialog editing',
          recommendTag: 'Recommended ✨',
          desc: 'Click the "Edit scene" button at the top-right of a scene video to open the deep customization dialog.',
        },
      ],
      methodsHint: 'The fine-tune dialog supports:',
      bulletsHtml: [
        '<strong>Image referencing and reset</strong>: Click "+" to upload new references, or reference images precisely with <code>@image1</code> / <code>@image2</code> in the prompt.',
        '<strong>Prompt rewriting</strong>: Edit the description directly. Refine lighting, motion, or framing to give the shot more precise direction.',
        '<strong>Per-scene model switching</strong>: If the current model (e.g. Vidu Q2) does not handle a motion perfectly, switch this single scene to Kling V3 Omni Pro or Seedance 2.0 from the dialog\'s bottom-right.',
      ],
      imageIdx: [17, 18],
    },
    {
      id: 's3-5',
      timestamp: '05',
      title: 'Subtitles & smart lip-sync (final step ✨)',
      bodyHtml: 'Once you are happy with all the scene videos, you can enable the following before exporting, as needed:',
      substeps: [
        {icon: '📝', textHtml: '<strong>Lyric subtitles</strong>: Turn this on and the system automatically embeds lyric subtitles into the final video based on the audio, saving you post-editing.'},
        {icon: '💡', textHtml: '<strong>Subtitle calibration</strong>: If the generated subtitles are out of sync with the audio, no worries — just tell the AI in the chat: "the subtitles are off, please re-calibrate". Calibration is completely free and costs no credits.'},
        {icon: '👄', textHtml: '<strong>Smart lip-sync</strong>: If your MV has shots of a person singing to camera, enabling this lets AI automatically detect and match the on-screen mouth movements to the lyric audio, greatly boosting realism and immersion.'},
      ],
      imageIdx: [22],
    },
    {
      id: 's3-6',
      timestamp: '06',
      title: 'Export & share',
      bulletGroups: [
        {html: '<strong>Download</strong>: When rendering finishes, click "Download" in the top-right to save the video.', imageIdx: [19, 20]},
        {html: '<strong>Share resources</strong>: Your generated music and MVs are visible in the sidebar "Resources" section, and can be shared with friends with one click.', imageIdx: [21]},
      ],
    },
  ],
};

export const GUIDE_MAP = {
  'zh-CN': ZH_CN_GUIDE,
  'en-US': EN_US_GUIDE,
};
