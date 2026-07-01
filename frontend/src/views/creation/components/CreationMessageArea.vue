<template>
  <!--
    创建对话消息区域：
    1) 根据 loadingMessages / loading / messages 渲染加载中、空状态、消息列表
    2) 用户消息 vs AI 消息（Markdown / 特殊块 / SUBJECT&SCENE 卡片 / SCENE_SCRIPT 表格 / 音视频）
    3) 三类资产在本组件中的展示与编辑入口：
       SUBJECT：msg.subjectList，版本下拉 + 卡片文案取当前 versions[].prompt（formatSubjectPromptDisplay）
       SCENE：msg.sceneList，版本下拉 + 文案取当前 versions[].visualPrompt（formatSceneDescriptionDisplay）；时段与时长仅 scene 顶层 duration/startTime/endTime（与版本无关）
       SCENE_SCRIPT：blocks 内 SCENE_SCRIPT.scenes；SceneScriptTable 内联写回 raw，经 scene-script-change → editContext
    4) SubjectEditDialog / SceneEditDialog 负责直连生成新版本；SceneScriptTable 仅本地改脚本行
  -->
  <div :class="contentWidthClass" class="creation-message-area pt-[34px] mb-[10px]">
    <!-- 历史消息正在加载 -->
    <div v-if="loadingMessages" class="flex-col-center h-full text-gray-500">
      <el-icon :size="48" class="animate-spin mb-4">
        <Loading/>
      </el-icon>
      <p class="text-lg">{{ t('creation.messageArea.loadingHistory') }}</p>
    </div>

    <!-- 没有消息且未处于加载阶段的空状态 -->
    <div v-else-if="isEmpty(messages) && !loading" class="flex-col-center h-full text-gray-500">
      <el-icon :size="64" class="mb-4">
        <ChatDotRound/>
      </el-icon>
      <p class="text-lg">{{ t('creation.messageArea.emptyStart') }}</p>
    </div>

    <!-- 消息列表渲染 -->
    <template v-else>
      <!--
        注意：
        - `msgIdx` 仅用于“主体继续生成”控制逻辑（与最后一条消息、音视频附件等条件有关）
        - 每条消息使用 `msg.messageId` 做 key，减少 DOM 重建
      -->
      <div v-for="(msg, msgIdx) in messages" :key="msg.messageId">
        <!-- 用户发送的消息：仅展示文本气泡 + 音频/图片附件（如果存在） -->
        <div v-if="msg.senderType === 'USER'" class="flex justify-end mb-4">
          <div class="flex flex-col items-end gap-2 max-w-[80%]">
            <div class="px-3 py-4 rounded-[20px] leading-relaxed break-words whitespace-pre-wrap bg-[#C2FF00] text-black rounded-tr-none">
              {{ msg.content }}
            </div>
            <!-- 用户消息附件：音频与图片列表 -->
            <div v-if="msg.attachments && (msg.attachments.audio || msg.attachments.images?.length)" class="flex items-center gap-2">
              <button
                  v-if="msg.attachments.audio"
                  class="w-12 h-12 rounded-[12px] bg-white/10 flex-center cursor-pointer transition-colors hover:bg-white/20"
                  :class="{ 'user-audio-pulse': isUserAudioPlaying(msg) }"
                  :title="t('creation.messageArea.playAudioTitle')"
                  aria-label="play-user-audio"
                  @click="emit('user-audio-play', msg)"
              >
                <svg-icon name="gy-audiofiles" size="20" color="#C2FF00"></svg-icon>
              </button>
              <!-- 用户图片附件预览（Element Plus 图片预览） -->
              <div v-for="(img, idx) in msg.attachments.images" :key="img.fileId || idx" class="w-12 h-12 rounded-[12px] overflow-hidden">
                <el-image
                    :src="img.previewUrl || img.fileUrl"
                    :preview-src-list="msg.attachments.images.map(item => item.previewUrl || item.fileUrl)"
                    :hide-on-click-modal="true"
                    :initial-index="idx"
                    preview-teleported
                    fit="cover"
                    class="w-full h-full"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- AI 生成的消息：由 blocks +（可选）subjectList/audioList/videoList 组成 -->
        <div v-else>
          <!-- blocks：通常是按顺序渲染的文本/特殊块（例如曲风提示/歌词/等） -->
          <div v-for="(block, idx) in msg.blocks" :key="idx">
            <!-- 普通文本块：支持 Markdown 渲染 -->
            <div
                v-if="block.type === 'TEXT' && (isNotEmpty(block.text) || (msg.messageId === currentAiMessageId && loading && idx === msg.blocks.length - 1))"
                class="mb-4 px-3 py-4 rounded-[20px] leading-relaxed break-words bg-white/10 text-white rounded-tl-none w-fit max-w-full"
            >
              <div
                  v-if="isNotEmpty(block.text)"
                  class="markdown-body markdown-body-chat w-full max-w-full"
                  v-html="renderCreationMarkdown(block.text)"
              ></div>

              <!-- AI 正在“最后一个 block”输出时的加载指示点 -->
              <div v-if="msg.messageId === currentAiMessageId && loading && idx === msg.blocks.length - 1" class="typing-dots flex items-center gap-[10px] mt-[10px] pl-[2px]">
                <span class="typing-dot w-[10px] h-[10px] rounded-full bg-[#c2ff00]"></span>
                <span class="typing-dot w-[10px] h-[10px] rounded-full bg-[#c2ff00]"></span>
                <span class="typing-dot w-[10px] h-[10px] rounded-full bg-[#c2ff00]"></span>
              </div>
            </div>

            <!-- 特殊块：如“曲风提示词/歌词”等（样式不同，支持复制） -->
            <div v-else-if="isSpecialBlock(block.type)" class="mb-4">
              <div class="rounded-[16px] overflow-hidden bg-[rgba(240,246,221,0.92)] border border-black/20 shadow-[0_10px_28px_rgba(0,0,0,0.35)]">
                <div class="h-[44px] flex items-center justify-between px-[14px] bg-[rgba(240,246,221,0.92)] border-b border-black/20">
                  <div class="flex items-center gap-[10px] text-[#0b0b0b] text-[16px] font-bold">
                    <svg-icon name="gy-lyrics" size="20"></svg-icon>
                    <span>{{ getSpecialBlockMeta(block.type).title }}</span>
                  </div>
                  <button
                      class="w-[28px] h-[28px] rounded-[8px] flex items-center justify-center text-black/70 transition-colors active:scale-[0.98] hover:bg-black/5 hover:text-black/90 cursor-pointer"
                      :aria-label="`copy-${block.type.toLowerCase()}`"
                      @click="handleCopyText(block.text, getSpecialBlockMeta(block.type).title)"
                  >
                    <svg-icon name="gy-copy" size="16" color="currentColor"></svg-icon>
                  </button>
                </div>
                <div class="bg-[#222222] p-[14px]" :class="getSpecialBlockMeta(block.type).bodyClass">
                  <div class="text-white/85 text-[14px] leading-[22px] whitespace-pre-wrap">{{ block.text }}</div>
                </div>
              </div>
            </div>

            <div v-else-if="block.type === 'ACTION_REQUIRED'" class="mb-4">
              <div class="rounded-[18px] px-[6px] py-[8px] text-white">
                <div class="mb-[18px] text-center">
                  <div class="text-[18px] leading-[28px] font-semibold text-white/88">
                    <span>{{
                        getActionRequiredTitleParts().before
                      }}</span><span class="text-[#C2FF00]">{{
                      getActionRequiredQuery(block)
                    }}</span><span>{{ getActionRequiredTitleParts().after }}</span>
                  </div>
                  <div class="mt-[6px] text-[14px] leading-[22px] text-white/70 whitespace-pre-wrap">
                    {{ t('creation.messageArea.actionRequired.fixedSubtitle') }}
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-[12px] items-stretch">
                  <label
                      v-for="(option, optionIdx) in getActionRequiredOptions(block)"
                      :key="option.id"
                      class="action-required-option-card w-full h-full rounded-[18px] border border-white/14 bg-[#1f2024] px-[16px] py-[16px] transition-colors"
                      :class="{
                        'hover:border-[#c2ff00]/70 hover:bg-[#23251a]': !isActionRequiredLocked(block),
                        'opacity-75': isActionRequiredLocked(block),
                        'border-[#c2ff00] bg-[#23251a]': isActionRequiredOptionSelected(block, option),
                      }"
                  >
                    <div class="flex h-full flex-col">
                      <div class="text-[18px] leading-[26px] font-semibold text-white mb-[6px]">含义{{
                          optionIdx + 1
                        }}
                      </div>
                      <div class="text-[16px] leading-[24px] font-semibold text-white/92 mb-[6px]">{{
                          option.label
                        }}
                      </div>
                      <div v-if="option.source" class="text-[13px] leading-[20px] text-white/45 mb-[8px]">
                        {{ option.source }}
                      </div>
                      <div class="flex-1 text-[14px] leading-[22px] text-white/78 whitespace-pre-wrap">{{
                          option.snippet
                        }}
                      </div>
                      <div class="mt-[14px] flex justify-end">
                        <button
                            type="button"
                            class="inline-flex h-[34px] min-w-[72px] items-center justify-center rounded-[12px] px-[14px] text-[14px] font-semibold transition-colors"
                            :class="isActionRequiredOptionSelected(block, option) ? 'bg-[#c2ff00] text-[#0d0d0d]' : 'bg-white/10 text-white/72'"
                            :disabled="isActionRequiredLocked(block)"
                            @click.stop="handleActionRequiredOptionClick(block, option)"
                        >
                          {{ isActionRequiredOptionSelected(block, option) ? '已选择' : '选这个' }}
                        </button>
                      </div>
                    </div>
                  </label>
                </div>
              </div>
            </div>

          </div>

          <!-- 人物/物体/环境参考图列表 -->
          <div v-if="isNotEmpty(msg.subjectList)" class="mb-4 reference-images-section">
            <div class="grid grid-cols-3 gap-3">
              <!-- subject 卡片：显示当前版本封面 + 可切换版本（在条件允许时）+ 编辑入口 -->
              <div v-for="(subject, subjectIdx) in msg.subjectList" :key="`subject-${subject.messageChunkId}`" class="group overflow-hidden rounded-[24px] border border-white/10 shadow-[0_8px_20px_rgba(0,0,0,0.28)]">
                <div class="relative w-full aspect-[1/1] bg-[#111317]">
                  <el-image
                      :src="getActiveSubjectVersion(subject)?.imgUrl"
                      :preview-src-list="getSubjectPreviewList(subject)"
                      :hide-on-click-modal="true"
                      :initial-index="getActiveSubjectPreviewIndex(subject)"
                      preview-teleported
                      fit="cover"
                      class="w-full h-full"
                  />

                  <!-- 左上角：参考图序号标记，序号取自 assetKey（如 subject_1 → 1），保持与后端身份一致 -->
                  <div class="h-[28px] absolute left-2 top-2 z-[3] flex-center rounded-[10px] bg-[rgba(0,0,0,0.7)] px-[10px] py-[2px] text-[12px] font-bold leading-[18px] text-[#C2FF00] shadow-[0_4px_12px_rgba(0,0,0,0.4)]">
                    {{ t('creation.messageArea.referenceLabel', {index: indexFromAssetKey(subject.assetKey)}) }}
                  </div>

                  <!-- 右上角：版本下拉 + 编辑按钮（仅在当前消息允许编辑/且没有 audio/video 附件时出现） -->
                  <div class="absolute right-2 top-2 z-[3] flex items-center">
                    <el-select
                        v-if="canShowSubjectControls(msg, msgIdx)"
                        size="small"
                        :title="formatVersionLabel(subject.activeVersion)"
                        class="subject-version-select"
                        popper-class="subject-version-select-popper"
                        :show-arrow="false"
                        :model-value="subject.activeVersion"
                        @update:model-value="(v) => handleSubjectVersionChange(msg, subject, v, subjectIdx)"
                    >
                      <el-option
                          v-for="v in (subject.versions || [])"
                          :key="v.version"
                          :label="formatVersionLabel(v.version)"
                          :value="v.version"
                      />
                    </el-select>

                    <button
                        v-if="canShowSubjectControls(msg, msgIdx)"
                        class="h-7 w-7 flex-center cursor-pointer rounded-[8px] hover:shadow-[inset_0_0_0_1px_rgba(184,255,26,0.12),0_4px_10px_rgba(0,0,0,0.38)] transition-all ease-in"
                        :title="t('creation.messageArea.editReferenceTitle')"
                        @click.stop="openSubjectEditDialog(msg, subject)"
                    >
                      <svg-icon name="gy-edit" size="16" color="#C2FF00"/>
                    </button>
                  </div>

                  <!-- 左下角：主体类型标签 -->
                  <div class="absolute left-2 bottom-2 rounded-[8px] bg-[rgba(0,0,0,0.7)] px-2 py-[2px] text-[12px] font-semibold leading-[18px] text-[#c2ff00] shadow-[0_4px_10px_rgba(0,0,0,0.35)]">
                    {{ getSubjectTypeLabel(subject.type) }}
                  </div>
                </div>

                <div class="p-3 text-[16px] leading-[24px] bg-[#181818f5] text-white/85">
                  <!-- 提示词与当前选中版本一致：优先 versions[].prompt，与返回值示例 SUBJECT 结构对齐 -->
                  <div :title="formatSubjectPromptDisplay(msg, subject)" class="line-clamp-3">
                    {{ formatSubjectPromptDisplay(msg, subject) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 当 subjectContinueMsgId 指向当前消息时：显示“确认并继续”按钮区域 -->
            <div
                v-if="shouldShowSubjectGuide(msg, msgIdx)"
                class="mt-3 flex justify-start"
            >
              <div class="flex flex-col items-start gap-4">
                <div
                    class="flex items-center gap-2"
                    :class="{
                      'opacity-60': isSubjectContinueLocked(),
                    }"
                >
                  <span class="w-2.5 h-2.5 rounded-full bg-[#C2FF00]"/>
                  <span class="text-[14px] font-semibold">
                    <span class="text-[#C2FF00]">{{ t('creation.messageArea.nextStepPrefix') }}</span>
                    <span class="text-white/85">{{ t('creation.messageArea.generateSceneScript') }}</span>
                  </span>
                </div>

                <button
                    class="w-[180px] h-[44px] rounded-[16px] bg-[#C2FF00] text-[#0d0d0d] font-extrabold text-[14px] inline-flex items-center justify-center gap-[10px] cursor-pointer border border-[#C2FF00] border-opacity-70 transition-transform active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-[#C2FF00] disabled:bg-opacity-20 disabled:border-opacity-30 disabled:text-[#0d0d0d] disabled:opacity-70"
                    :disabled="isSubjectContinueLocked()"
                    @click.stop="handleSubjectContinueClick(msg)"
                >
                  <span>{{ t('creation.messageArea.confirmAndContinue') }}</span>
                  <span class="w-[8px] h-[8px] border-t-[2px] border-r-[2px] border-current rotate-45 mt-[2px]"/>
                </button>
              </div>
            </div>
          </div>

          <!-- 分镜脚本表格：SCENE_SCRIPT 类型在同一次 model 回复底部的次序优先于 SCENE -->
          <div v-if="getSceneScriptScenes(msg).length" class="mb-4 scene-script-section">
            <SceneScriptTable
                :scenes="getSceneScriptScenes(msg)"
                :message-chunk-id="getSceneScriptChunkId(msg)"
                :can-edit="canEditCreation"
                @scene-script-change="(payload) => handleSceneScriptChange(msg, payload)"
            />
          </div>

          <!-- 确认并生成：仅由该 model 消息内是否包含 SCENE_SCRIPT 决定 -->
          <div
              v-if="shouldShowSceneScriptGuide(msg, msgIdx)"
              class="mt-1 mb-4 flex justify-start"
          >
            <div class="flex flex-col items-start gap-4">
              <div
                  class="flex items-center gap-2"
                  :class="{
                    'opacity-60': isSceneScriptContinueLocked(),
                  }"
              >
                <span class="w-2.5 h-2.5 rounded-full bg-[#C2FF00]"/>
                <span class="text-[14px] font-semibold">
                  <span class="text-[#C2FF00]">{{ t('creation.messageArea.nextStepPrefix') }}</span>
                  <span class="text-white/85">{{ t('creation.messageArea.generateSceneVideo') }}</span>
                </span>
              </div>

              <button
                  class="w-[180px] h-[44px] rounded-[16px] bg-[#C2FF00] text-[#0d0d0d] font-extrabold text-[14px] inline-flex items-center justify-center gap-[10px] cursor-pointer border border-[#C2FF00] border-opacity-70 transition-transform active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-[#C2FF00] disabled:bg-opacity-20 disabled:border-opacity-30 disabled:text-[#0d0d0d] disabled:opacity-70"
                  :disabled="isSceneScriptContinueLocked()"
                  @click.stop="handleSceneScriptContinueClick(msg)"
              >
                <span>{{ t('creation.messageArea.confirmAndGenerate') }}</span>
                <span class="text-[16px] leading-none">⚡</span>
              </button>
            </div>
          </div>

          <div v-if="isNotEmpty(msg.sceneList)" class="mb-4 mv-scenes-section">
            <div class="grid grid-cols-3 gap-3">
              <div
                  v-for="(sceneItem, sceneIdx) in msg.sceneList"
                  :key="`scene-${sceneItem.messageChunkId}`"
                  class="group overflow-hidden rounded-[24px] border border-white/10 shadow-[0_8px_20px_rgba(0,0,0,0.28)] bg-[#181818f5]"
              >
                <div
                    class="relative w-full aspect-[1/1] bg-[#111317]"
                    :class="{'cursor-pointer': canInteractWithSceneMedia(msg, msgIdx, sceneItem)}"
                    @click="handleSceneMediaClick(msg, msgIdx, sceneItem)"
                >
                  <el-image
                      v-if="getActiveSceneVersion(sceneItem)?.coverUrl"
                      :src="getActiveSceneVersion(sceneItem)?.coverUrl"
                      fit="cover"
                      class="w-full h-full"
                  />
                  <div v-else class="w-full h-full flex-center text-white/60">{{
                      t('creation.messageArea.noCover')
                    }}
                  </div>

                  <!-- 禁止编辑时：有视频则显示播放提示，点击封面打开预览弹窗 -->
                  <div
                      v-if="!canShowSceneControls(msg, msgIdx) && getActiveSceneVersion(sceneItem)?.videoUrl"
                      class="absolute inset-0 z-[2] flex items-center justify-center pointer-events-none"
                  >
                    <div
                        class="flex h-11 w-11 items-center justify-center rounded-full bg-black/55 text-white shadow-[0_8px_24px_rgba(0,0,0,0.45)] ring-1 ring-white/15"
                        aria-hidden="true"
                    >
                      <svg-icon name="gy-play2" :size="22" color="#C2FF00"/>
                    </div>
                  </div>

                  <div class="absolute left-2 top-2 z-[3] h-7 flex-center rounded-[10px] bg-[rgba(0,0,0,0.7)] px-[10px] py-[2px] text-[12px] font-bold leading-[18px] text-[#C2FF00] shadow-[0_4px_12px_rgba(0,0,0,0.4)]">
                    {{ t('creation.messageArea.sceneLabel', {index: indexFromAssetKey(sceneItem.assetKey)}) }} ·
                    {{ getSceneDurationLabel(sceneItem) }}
                  </div>

                  <div class="absolute left-2 bottom-2 z-[3] rounded-[10px] bg-[rgba(0,0,0,0.7)] px-[10px] py-[2px] text-[12px] font-medium leading-[18px] text-white/85 shadow-[0_4px_12px_rgba(0,0,0,0.35)]">
                    {{ getSceneTimeRangeLabel(sceneItem) }}
                  </div>

                  <div class="absolute right-2 top-2 z-[3] flex items-center">
                    <el-select
                        v-if="canShowSceneControls(msg, msgIdx)"
                        size="small"
                        :title="formatVersionLabel(sceneItem.activeVersion)"
                        class="subject-version-select"
                        popper-class="subject-version-select-popper"
                        :show-arrow="false"
                        :model-value="sceneItem.activeVersion"
                        @click.stop
                        @mousedown.stop
                        @update:model-value="(v) => handleSceneVersionChange(msg, sceneItem, v, sceneIdx)"
                    >
                      <el-option
                          v-for="version in (sceneItem.versions || [])"
                          :key="version.version"
                          :label="formatVersionLabel(version.version)"
                          :value="version.version"
                          @click.stop
                      />
                    </el-select>

                    <button
                        v-if="canShowSceneControls(msg, msgIdx)"
                        class="h-7 w-7 flex-center cursor-pointer rounded-[8px] hover:shadow-[inset_0_0_0_1px_rgba(184,255,26,0.12),0_4px_10px_rgba(0,0,0,0.38)] transition-all ease-in"
                        :title="t('creation.messageArea.editSceneTitle')"
                        @click.stop="openSceneEditDialog(msg, sceneItem)"
                    >
                      <svg-icon name="gy-edit" size="16" color="#C2FF00"/>
                    </button>
                  </div>
                </div>

                <div class="p-3 text-[16px] leading-[24px] text-white/85">
                  <div :title="formatSceneDescriptionDisplay(sceneItem)" class="line-clamp-3">
                    {{ formatSceneDescriptionDisplay(sceneItem) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 仅当本 MODEL 消息含 SCENE 数据时显示「完成制作」；避免纯文本等普通回复下方出现按钮 -->
            <div
                v-if="shouldShowSceneGuide(msg, msgIdx)"
                class="mt-3 mb-4"
            >
              <div class="w-full flex flex-col gap-3">
                <div
                    class="flex items-center gap-2"
                    :class="{
                      'opacity-60': isSceneContinueLocked(),
                    }"
                >
                  <span class="w-2.5 h-2.5 rounded-full bg-[#C2FF00]"/>
                  <span class="text-[14px] font-semibold">
                    <span class="text-[#C2FF00]">{{ t('creation.messageArea.finalStepPrefix') }}</span>
                    <span class="text-white/85">{{ t('creation.messageArea.exportMvVideo') }}</span>
                  </span>
                </div>

                <!-- 两个开关：自动生成歌词字幕 / 应用智能口型同步效果 -->
                <div class="w-[350px] flex gap-3">
                  <div class="flex-1 h-12 rounded-[12px] bg-[#1a1a1a] border border-white/10 flex-center gap-3">
                    <span class="text-white text-[14px] font-semibold leading-[20px]">{{ t('creation.messageArea.autoSubtitleTitle') }}</span>
                    <el-switch
                        class="scene-guide-switch"
                        v-model="subtitle"
                        :disabled="isSceneContinueLocked()"
                        inline-prompt
                        style="--el-switch-on-color: #C2FF00;"
                    ></el-switch>
                  </div>
                  <div class="flex-1 h-12 rounded-[12px] bg-[#1a1a1a] border border-white/10 flex-center gap-3">
                    <span class="text-white text-[14px] font-semibold leading-[20px]">{{ t('creation.messageArea.lipSyncTitle') }}</span>
                    <el-switch
                        class="scene-guide-switch"
                        v-model="lipSync"
                        :disabled="isSceneContinueLocked()"
                        inline-prompt
                        style="--el-switch-on-color: #C2FF00;"
                    ></el-switch>
                  </div>
                </div>

                <!-- 合成按钮 -->
                <button
                    class="w-[350px] h-[48px] rounded-[16px] bg-[#C2FF00] text-black font-semibold text-[16px] flex-center px-8 gap-[10px] cursor-pointer border border-[#C2FF00] border-opacity-70 transition-transform active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-[#C2FF00] disabled:bg-opacity-20 disabled:border-opacity-30 disabled:text-[#0d0d0d] disabled:opacity-70"
                    :disabled="isSceneContinueLocked()"
                    @click.stop="handleSceneContinueClick(msg)"
                >
                  <span>{{ t('creation.messageArea.finalComposeButton') }}</span>
                </button>
              </div>
            </div>
          </div>

          <!-- AI 消息音频列表（由 AudioCard 组件负责播放/选择交互） -->
          <AudioCard
              v-if="isNotEmpty(msg.audioList)"
              :audio-list="msg.audioList"
              :current-playing-id="playerState.currentAudio?.audioFileId || ''"
              :playing="playerState.isPlaying"
              @select="(e) => emit('audio-select', e, msg.audioList)"
              @toggle-play="(e) => emit('audio-toggle-play', e, msg.audioList)"
          ></AudioCard>

          <CreationSimpleFeedback
              v-if="canShowFeedback(msg, 'MUSIC')"
              :title="t('creation.feedback.musicTitle')"
              :message-id="msg.messageId"
              :allow-pending="isLastMessage(msgIdx)"
          />

          <!-- AI 消息视频列表 -->
          <div v-if="isNotEmpty(msg.videoList)" class="mb-4 grid grid-cols-1 gap-4">
            <!-- 每个 video：展示视频预览 + （可编辑时）入口按钮 -->
            <div v-for="video in msg.videoList" :key="video.videoFileId" class="relative rounded-[16px] overflow-hidden bg-black/20 border border-white/10 group">
              <video
                  :src="video.videoUrl"
                  :poster="video.coverUrl"
                  controls
                  controlslist="nodownload"
                  class="w-full aspect-video object-contain"
                  @play="emit('video-play')"
              ></video>
              <button
                  v-if="video.videoUrl"
                  class="absolute top-2 right-12 w-8 h-8 rounded-[8px] bg-black/60 hover:bg-[#C2FF00] flex-center transition-colors cursor-pointer"
                  :title="t('creation.messageArea.downloadVideoTitle')"
                  @click.stop="handleDownloadFinalVideo(video)"
              >
                <svg-icon name="gy-download" size="16" :color="'rgba(255,255,255,0.9)'"/>
              </button>
              <button
                  v-if="canEditCreation"
                  class="absolute top-2 right-2 w-8 h-8 rounded-[8px] bg-black/60 hover:bg-[#C2FF00] flex-center transition-colors group/btn cursor-pointer"
                  :title="t('creation.messageArea.storyboardEditTitle')"
                  @click.stop="handleEditVideo(video)"
              >
                <svg-icon name="gy-edit" size="16" :color="'rgba(255,255,255,0.9)'"/>
              </button>
            </div>
          </div>

          <CreationSimpleFeedback
              v-if="canShowFeedback(msg, 'VIDEO')"
              :title="t('creation.feedback.videoTitle')"
              :message-id="msg.messageId"
              :allow-pending="isLastMessage(msgIdx)"
          />

        </div>
      </div>
    </template>

    <SubjectEditDialog
        v-model="subjectEditDialogVisible"
        :source-msg="subjectEditMessage"
        :target-subject="subjectEditTarget"
        @edit-context-change="(payload) => emit('edit-context-change', payload)"
    />
    <SceneEditDialog
        v-model="sceneEditDialogVisible"
        :source-msg="sceneEditMessage"
        :target-scene="sceneEditTarget"
        @edit-context-change="(payload) => emit('edit-context-change', payload)"
    />

    <!-- 历史分镜禁止编辑时：仅预览视频（带原生控件） -->
    <el-dialog
        v-model="sceneVideoPreviewVisible"
        :title="t('creation.messageArea.playSceneTitle')"
        width="min(96vw, 1000px)"
        append-to-body
        destroy-on-close
        align-center
        class="scene-video-preview-dialog"
        modal-class="scene-video-preview-modal"
        :show-close="true"
        @closed="resetSceneVideoPreview"
    >
      <video
          v-if="sceneVideoPreviewUrl"
          :key="sceneVideoPreviewUrl"
          :src="sceneVideoPreviewUrl"
          :poster="sceneVideoPreviewPoster || undefined"
          controls
          playsinline
          class="scene-video-preview-player"
          @play="emit('video-play')"
      ></video>
      <div v-else class="py-10 text-center text-white/50 text-[14px]">{{ t('creation.messageArea.noVideo') }}</div>
    </el-dialog>

    <!-- 口型同步确认弹窗 -->
    <LipSyncConfirmDialog
        :visible="showLipSyncConfirm"
        :messages="messages"
        @confirm="handleLipSyncConfirm"
        @cancel="handleLipSyncCancel"
    />
  </div>
</template>

<script setup>
import {computed, ref, watch} from 'vue';
import {ElMessage} from 'element-plus';
import {ChatDotRound, Loading} from '@element-plus/icons-vue';
import {saveUserTracking} from '@/api/tracking';
import LipSyncConfirmDialog from './LipSyncConfirmDialog.vue';
import {renderCreationMarkdown} from '@/views/creation/utils/creationMarkdown';
import 'highlight.js/styles/github-dark.css';
import 'github-markdown-css/github-markdown-dark.css';
import AudioCard from './AudioCard.vue';
import CreationSimpleFeedback from './CreationSimpleFeedback.vue';
import SubjectEditDialog from './SubjectEditDialog.vue';
import SceneEditDialog from './SceneEditDialog.vue';
import SceneScriptTable from './SceneScriptTable.vue';
import {isEmpty, isNotEmpty} from '@/utils/index.js';
import {replaceMentionValuesToLabels} from '@/views/creation/utils/creationMention.js';
import {applyCreationVersionChange, formatCreationVersionLabel} from '@/views/creation/utils/creationVersionSwitch';
import {useI18nText} from '@/i18n';

/**
 * 组件输入参数：
 * - 大量控制 UI 展示与可编辑性的开关（例如 `canEditCreation`、`subjectContinueDisabled`）
 * - `playerState` 用于同步音频播放 UI
 */
const props = defineProps({ // 父组件传入属性
  contentWidthClass: {type: String, default: 'w-[800px]'},
  loadingMessages: {type: Boolean, default: false},
  loading: {type: Boolean, default: false},
  messages: {type: Array, default: () => []},
  // messageId 后端契约为 string，本地占位也已统一为 string；未发起请求时为 null
  currentAiMessageId: {type: String, default: null},
  canEditCreation: {type: Boolean, default: false},
  showFeedback: {type: Boolean, default: true},
  playerState: {type: Object, required: true},
  // messageId 后端契约为 string；未指向用户音频时为 null
  currentUserAudioMessageId: {type: String, default: null},
  // 后端 messageId 为 string；未指向时为 null
  subjectContinueMsgId: {type: String, default: null},
  subjectContinueDisabled: {type: Boolean, default: false},
  // 后端 messageId 为 string；未指向时为 null
  sceneScriptContinueMsgId: {type: String, default: null},
  sceneScriptContinueDisabled: {type: Boolean, default: false},
  // 后端 messageId 为 string；未指向时为 null
  sceneContinueMsgId: {type: String, default: null},
  sceneContinueDisabled: {type: Boolean, default: false},
});

/**
 * 事件输出：
 * - 该组件只负责展示与触发交互，把具体业务逻辑交给父组件（例如继续生成、播放控制、打开分镜编辑）
 */
const emit = defineEmits([ // 对外事件
  'user-audio-play',
  'audio-select',
  'audio-toggle-play',
  'video-play',
  'open-storyboard',
  'subject-continue',
  'scene-script-continue',
  'scene-continue',
  'action-required-submit',
  'edit-context-change',
]);
const {t} = useI18nText(); // 多语言翻译函数


/**
 * 判断消息下是否显示反馈入口。
 * @param {any} msg
 * @param {'MUSIC'|'VIDEO'} cardType
 * @returns {boolean}
 */
const canShowFeedback = (msg, cardType) => {
  if (!props.showFeedback) return false;
  if (!msg?.messageId) return false;
  if (cardType === 'MUSIC') return Array.isArray(msg?.audioList) && msg.audioList.length > 0;
  if (cardType === 'VIDEO') return Array.isArray(msg?.videoList) && msg.videoList.length > 0;
  return false;
};

/**
 * SUBJECT / SCENE / SCENE_SCRIPT 在本文件中的边界说明（仅展示与触发，业务在父级 CreationChatPanel / index）：
 *
 * - SUBJECT & SCENE：含 `versions` + `activeVersion`。切换版本会 `emit('edit-context-change')`，供发送前拼「版本选择」说明。
 *   卡片提示词必须与当前版本字段一致（SUBJECT→prompt，SCENE→visualPrompt），勿用顶层 description 代替。
 *
 * - SCENE_SCRIPT：数据在 `getSceneScriptScenes(msg)`；SceneScriptTable 写回 `scene.raw` 并 `scene-script-change` → 父级拼 `editContext`。
 *
 * @see {@link ./CreationChatPanel.vue} buildEditContext、handleEditContextChange
 *
 * 计算消息数量（容错：props.messages 可能不是数组）。
 * @returns {number} 消息数量
 */
const getMessagesCount = () => Array.isArray(props.messages) ? props.messages.length : 0;
/** 仅最后一条消息允许展示流程引导按钮。 */
const isLastMessage = (msgIdx) => msgIdx === getMessagesCount() - 1; // 是否是最后一条消息

const dismissedGuideMap = ref({ // 本地隐藏态：点击引导按钮后立即隐藏，key 为 type（subject/sceneScript/scene），value 为 { [messageId]: true }
  subject: {},
  sceneScript: {},
  scene: {},
});

const getMessageGuideKey = (msg) => String(msg?.messageId ?? ''); // 引导状态存储 key

const isGuideDismissed = (type, msg) => { // 引导是否已被本地隐藏
  const key = getMessageGuideKey(msg);
  if (!key) return false;
  return Boolean(dismissedGuideMap.value[type]?.[key]);
};

const dismissGuide = (type, msg) => { // 标记引导为已隐藏
  const key = getMessageGuideKey(msg);
  if (!key) return;
  dismissedGuideMap.value[type][key] = true;
};

const clearGuideDismissedByType = (type) => { // 清除指定类型的所有隐藏状态
  dismissedGuideMap.value[type] = {};
};

/**
 * 通用引导显示规则：
 * 1) 有编辑权限；2) 当前不在模型回复中；3) 命中父级指定的 continue 消息；
 * 4) 当前是最后一条消息；5) 通过可选附加校验（如 sceneScript 需存在脚本行）；
 * 6) 未被本地点击隐藏。
 */
const shouldShowGuide = ({type, msg, msgIdx, continueMsgId, extraCheck = null}) => {
  if (!props.canEditCreation) return false;
  if (props.loading) return false;
  if (String(continueMsgId ?? '') !== String(msg?.messageId ?? '')) return false;
  if (!isLastMessage(msgIdx)) return false;
  if (typeof extraCheck === 'function' && !extraCheck(msg)) return false;
  if (isGuideDismissed(type, msg)) return false;
  return true;
};

const shouldShowSubjectGuide = (msg, msgIdx) => { // 是否显示「确认并继续」引导
  return shouldShowGuide({
    type: 'subject',
    msg,
    msgIdx,
    continueMsgId: props.subjectContinueMsgId,
  });
};

const shouldShowSceneScriptGuide = (msg, msgIdx) => { // 是否显示「确认并生成」引导
  return shouldShowGuide({
    type: 'sceneScript',
    msg,
    msgIdx,
    continueMsgId: props.sceneScriptContinueMsgId,
    extraCheck: (targetMsg) => getSceneScriptScenes(targetMsg).length > 0,
  });
};

const shouldShowSceneGuide = (msg, msgIdx) => { // 是否显示「完成制作」引导
  return shouldShowGuide({
    type: 'scene',
    msg,
    msgIdx,
    continueMsgId: props.sceneContinueMsgId,
  });
};

const GUIDE_EVENT_MAP = { // 引导类型 → emit 事件名映射
  subject: 'subject-continue',
  sceneScript: 'scene-script-continue',
  scene: 'scene-continue',
};

/**
 * 引导按钮点击通用处理：先本地隐藏，再上抛事件，保证点击即消失。
 * @param {'subject'|'sceneScript'|'scene'} type
 * @param {any} msg
 * @returns {void}
 */
const handleGuideContinueClick = (type, msg) => {
  dismissGuide(type, msg);
  emit(GUIDE_EVENT_MAP[type], msg?.messageId);
};

const handleSubjectContinueClick = (msg) => handleGuideContinueClick('subject', msg); // SUBJECT 引导点击
const handleSceneScriptContinueClick = (msg) => handleGuideContinueClick('sceneScript', msg); // SCENE_SCRIPT 引导点击

// 最终合成阶段的两个开关，字段直传 chat 接口（subtitle / lipSync）
const subtitle = ref(false); // 是否添加字幕
const lipSync = ref(false); // 是否口型同步
const showLipSyncConfirm = ref(false); // 口型同步弹窗显隐，pendingSceneMsg 缓存目标消息
const pendingSceneMsg = ref(null); // 口型同步弹窗等待确认的 SCENE 消息

/**
 * 提交最终合成：携带两个 switch 状态。
 * 重置 switch 状态到 false，避免下次同一会话再进入引导按钮区时残留旧选择。
 */
const submitSceneContinue = (msg) => {
  const extras = {subtitle: subtitle.value, lipSync: lipSync.value};
  dismissGuide('scene', msg);
  emit('scene-continue', msg?.messageId, extras);
  subtitle.value = false;
  lipSync.value = false;
};

/**
 * 完成制作按钮：lipSync 开启时必须先弹窗确认；关闭则直走 emit。
 * 弹窗取消会把 lipSync 关掉（用户撤销选择）；弹窗确认走 submitSceneContinue。
 */
const handleSceneContinueClick = (msg) => {
  if (lipSync.value) {
    pendingSceneMsg.value = msg;
    showLipSyncConfirm.value = true;
    return;
  }
  submitSceneContinue(msg);
};

const handleLipSyncConfirm = () => { // 口型同步弹窗确认
  showLipSyncConfirm.value = false;
  const msg = pendingSceneMsg.value;
  pendingSceneMsg.value = null;
  if (msg) submitSceneContinue(msg);
};

const handleLipSyncCancel = () => { // 口型同步弹窗取消
  showLipSyncConfirm.value = false;
  pendingSceneMsg.value = null;
  lipSync.value = false;
};

/**
 * 引导按钮是否应禁用。
 * @param {'subject'|'sceneScript'|'scene'} type
 * @returns {boolean}
 */
const isGuideContinueLocked = (type) => {
  const disabledByType = {
    subject: props.subjectContinueDisabled,
    sceneScript: props.sceneScriptContinueDisabled,
    scene: props.sceneContinueDisabled,
  };
  return !props.canEditCreation || Boolean(disabledByType[type]);
};

const isSubjectContinueLocked = () => isGuideContinueLocked('subject'); // SUBJECT 引导是否禁用
const isSceneScriptContinueLocked = () => isGuideContinueLocked('sceneScript'); // SCENE_SCRIPT 引导是否禁用
const isSceneContinueLocked = () => isGuideContinueLocked('scene'); // SCENE 引导是否禁用

/**
 * 取第一条 SCENE_SCRIPT 块（若后端未来同条消息投递多块脚本，需改为合并或按业务规则选取）。
 * @param {any} msg
 * @returns {any|null}
 */
const getSceneScriptBlock = (msg) => {
  return (msg?.blocks || []).find((block) => block?.type === 'SCENE_SCRIPT') || null;
};

/**
 * 从 SCENE_SCRIPT 块中取出 scenes 数组（无块时为空数组）。
 * SCENE_SCRIPT 后端契约：data 即 scenes 数组。
 * @param {any} msg
 * @returns {any[]}
 */
const getSceneScriptScenes = (msg) => {
  return getSceneScriptBlock(msg)?.scenes || [];
};

/**
 * 读取 SCENE_SCRIPT 块的 chunkId（用于脚本单行更新接口）。
 * messageChunkId 后端契约 string，绝不可私自 Number()。
 * @param {any} msg
 * @returns {string}
 */
const getSceneScriptChunkId = (msg) => {
  return getSceneScriptBlock(msg)?.messageChunkId || '';
};

/**
 * 判断是否应该在 subject 卡片上展示“版本下拉”和“编辑按钮”。
 * @param {any} msg 当前消息对象
 * @param {number} msgIdx 消息下标
 * @returns {boolean}
 */
const canShowSubjectControls = () => {
  // SUBJECT 版本下拉与编辑入口不再依赖 subjectContinueMsgId/禁用状态/附件状态
  return props.canEditCreation;
};

const SPECIAL_BLOCK_META = { // AI blocks 特殊块配置：标题、复制按钮文案、body 样式
  LYRICS: {
    title: t('creation.messageArea.lyricsTitle'),
    bodyClass: '',
  },
};

/**
 * 判断 block.type 是否属于特殊块。
 * @param {string} type block 类型
 * @returns {boolean}
 */
const isSpecialBlock = (type) => Boolean(SPECIAL_BLOCK_META[type]);

/**
 * 获取特殊块的元信息（找不到则回退到 LYRICS 默认元信息）。
 * @param {string} type block 类型
 * @returns {{title: string, bodyClass: string}}
 */
const getSpecialBlockMeta = (type) => SPECIAL_BLOCK_META[type] || SPECIAL_BLOCK_META.LYRICS;

/**
 * 复制文本到剪贴板，并给出 Element Plus 的轻提示。
 * @param {string} text 需要复制的文本
 * @param {string} label 用户可读的复制标签（用于提示文案）
 * @returns {void}
 */
const handleCopyText = (text, label) => {
  if (!text) return;
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(t('creation.messageArea.copiedWithLabel', {label}));
  });
};

/**
 * 取候选项列表。ACTION_REQUIRED data.options 后端契约 array，最多展示 2 项。
 */
const getActionRequiredOptions = (block) => (block?.data?.options || []).slice(0, 2);

const getActionRequiredTitleParts = () => { // 解析 ACTION_REQUIRED 标题模板，返回前后两段
  const title = t('creation.messageArea.actionRequired.detectedAmbiguityTitle', {query: '__QUERY__'});
  const [before, after] = title.split('__QUERY__');
  return {before: before || '', after: after || ''};
};

/**
 * 读取 ACTION_REQUIRED 的问题描述：优先用 block.data.query；若后端未回 query，则回退到首个 option.query，保证标题可展示。
 * data.query / options[i].query 后端契约皆 string。
 * @param {any} block
 * @returns {string}
 */
const getActionRequiredQuery = (block) => {
  const direct = (block?.data?.query || '').trim();
  if (direct) return direct;
  return (block?.data?.options?.[0]?.query || '').trim();
};

/**
 * ACTION_REQUIRED 选项是否为当前已选项（用于回显）。
 * options[i].id / selectedOptionId 后端契约皆 string，直接 ===。
 * @param {any} block
 * @param {any} option
 * @returns {boolean}
 */
const isActionRequiredOptionSelected = (block, option) => {
  const selectedOptionId = block?.data?.selectedOptionId;
  return !!selectedOptionId && !!option?.id && selectedOptionId === option.id;
};

/**
 * ACTION_REQUIRED 是否已锁定（一次性选择）。
 * @param {any} block
 * @returns {boolean}
 */
const isActionRequiredLocked = (block) => !!block?.data?.selectedOptionId;

/**
 * 点击候选项后直接提交 clarify。
 * @param {any} block
 * @param {any} option
 * @returns {void}
 */
const handleActionRequiredOptionClick = (block, option) => {
  if (!option) return;
  if (isActionRequiredLocked(block)) return;
  if (isActionRequiredOptionSelected(block, option)) return;
  block.data.selectedOptionId = option.id;
  emit('action-required-submit', {
    kind: block.data.kind,
    actionId: block.data.actionId,
    optionId: option.id,
    optionLabel: option.label,
    query: option.query,
  });
};

const SUBJECT_TYPE_LABEL = { // 参考图分类标签映射
  character: t('creation.intention.character'),
  object: t('creation.messageArea.subjectTypeObject'),
  environment: t('creation.intention.environment'),
};

/**
 * 从 assetKey 取尾部序号（subject_1 / scene_3 → "1" / "3"），用作 SUBJECT / SCENE 卡片左上角标签。
 * 比"数组下标 + 1"更稳定：后端按 assetKey 区分身份，跟下标解耦。
 */
const indexFromAssetKey = (assetKey) => {
  const m = (assetKey || '').match(/(\d+)$/);
  return m ? m[1] : '';
};

/**
 * 获取 subject 当前 activeVersion 对应的版本数据。
 * SUBJECT 字段契约：versions=array、versions[].version=number、activeVersion=number。
 * @param {any} subject subject 对象（可能包含 versions/activeVersion）
 * @returns {any|null} 命中的版本，未命中/无版本则返回 null
 */
const getActiveSubjectVersion = (subject) => {
  const versions = subject.versions || [];
  if (!versions.length) return null;
  return versions.find((v) => v.version === subject.activeVersion) || versions[0];
};

/**
 * 获取 subject 的预览图片列表（只返回有 imgUrl 的版本）。
 * @param {any} subject
 * @returns {string[]} 预览 URL 列表
 */
const getSubjectPreviewList = (subject) => {
  return (subject.versions || []).map((v) => v.imgUrl).filter(Boolean);
};

/**
 * 获取当前 activeVersion 在 preview list 中的索引。
 * @param {any} subject
 * @returns {number} preview 初始索引
 */
const getActiveSubjectPreviewIndex = (subject) => {
  const idx = (subject.versions || []).findIndex((v) => v.version === subject.activeVersion);
  return idx >= 0 ? idx : 0;
};

/**
 * 版本号展示格式：V{index}（注意这里将 version 做了 +1 显示，保持与后端/业务一致）。
 * @param {number} version 后端返回的 version（number 契约）
 * @returns {string}
 */
const formatVersionLabel = (version) => formatCreationVersionLabel(version);

/**
 * 将 subject.type 转成 UI 标签文本。SUBJECT.type 后端契约 string；toLowerCase 仅做大小写收敛。
 * @param {string} type subject 类型
 * @returns {string}
 */
const getSubjectTypeLabel = (type) => {
  const key = (type || '').toLowerCase();
  return SUBJECT_TYPE_LABEL[key] || t('creation.messageArea.subjectTypeUncategorized');
};

/**
 * 参考图卡片提示词展示（与当前 activeVersion 对应版本的 prompt 一致）。
 * versions[].refImages 后端契约 array。
 * @param {any} msg 当前消息（用于 subjectList 做 @ 提及替换）
 * @param {any} subject subject 数据（含 versions / activeVersion / description）
 * @returns {string}
 */
const formatSubjectPromptDisplay = (_msg, subject) => {
  const active = getActiveSubjectVersion(subject);
  const text = active?.prompt ?? '';
  return replaceMentionValuesToLabels(text, active?.refImages || [], {
    idFields: ['fileId'],
  });
};

/**
 * 获取 scene 当前 `activeVersion` 对应的版本条目（含 visualPrompt、视频、subjects 等）。
 * SCENE 字段契约：versions=array、versions[].version=number、activeVersion=number。
 * @param {any} scene
 * @returns {any|null}
 */
const getActiveSceneVersion = (scene) => {
  const versions = scene.versions || [];
  if (!versions.length) return null;
  return versions.find((version) => version.version === scene.activeVersion) || versions[0];
};

/**
 * 读取当前分镜的时段与时长（秒）。
 * SCENE 字段契约：startTime/endTime/duration 后端皆 number；仅在 scene 顶层（versions[] 内不含）。
 * @param {any} scene
 * @returns {{ start: number, end: number, duration: number }}
 */
const getSceneTiming = (scene) => ({
  start: scene?.startTime,
  end: scene?.endTime,
  duration: scene?.duration ?? 0,
});

/**
 * 分镜卡片文案：当前版本 visualPrompt，@subject_X 替换为 @图片N。
 * SCENE versions[].subjects[].id 后端 string。
 * @param {any} scene
 * @returns {string}
 */
const formatSceneDescriptionDisplay = (scene) => {
  const active = getActiveSceneVersion(scene);
  return replaceMentionValuesToLabels(active?.visualPrompt ?? '', active?.subjects || [], {
    idFields: ['id'],
    fallbackPattern: null,
  });
};

/**
 * 秒数 → mm:ss.ff 时间码，帧位精确到 1/100 秒。
 * 0 → "0:00.00"，2.64 → "0:02.64"，65.42 → "1:05.42"
 * @param {number} seconds
 * @returns {string}
 */
const fmtPrecise = (seconds) => {
  if (!seconds) return '0:00.00';
  const total = Math.floor(seconds);
  const min = Math.floor(total / 60);
  const sec = total % 60;
  const frac = Math.round((seconds - total) * 100);
  return `${min}:${String(sec).padStart(2, '0')}.${String(frac).padStart(2, '0')}`;
};

/**
 * 当前分镜时长标签（如 `2.64s`）。优先区间长度，否则用 duration。
 * @param {any} scene
 * @returns {string}
 */
const getSceneDurationLabel = (scene) => {
  const {start, end, duration} = getSceneTiming(scene);
  const sec = (start != null && end != null && end > start) ? (end - start) : duration;
  return `${sec.toFixed(2)}s`;
};

/**
 * 时间轴区间标签（如 `0:00–2.64s`）。优先 startTime-endTime；缺 endTime 时用 start+duration 兜底。
 * @param {any} scene
 * @returns {string}
 */
const getSceneTimeRangeLabel = (scene) => {
  const {start, end, duration} = getSceneTiming(scene);
  let startSec = start != null ? start : 0;
  let endSec = end != null ? end : null;
  if (endSec == null && start != null && duration > 0) {
    endSec = start + duration;
  }
  if (endSec == null && duration > 0) {
    endSec = duration;
    startSec = 0;
  }
  if (endSec == null) {
    endSec = 0;
  }
  if (endSec <= startSec && duration > 0) {
    endSec = startSec + duration;
  }
  return `${fmtPrecise(startSec)}-${fmtPrecise(endSec)}`;
};

/**
 * 是否展示分镜卡片上的版本下拉与编辑入口（无成片视频时）。
 * @param {any} msg
 * @param {number} msgIdx
 * @returns {boolean}
 */
const canShowSceneControls = () => {
  // SCENE 版本下拉与编辑入口不再依赖 sceneContinueMsgId/禁用状态/附件状态
  return props.canEditCreation;
};

/**
 * 更新 subject 的 activeVersion，用于驱动右侧图片与预览索引变化。
 * @param {any} subject subject 对象（会直接修改其 activeVersion）
 * @param {any} version 选择的新版本号
 * @returns {void}
 */
const handleSubjectVersionChange = (msg, subject, version, subjectIdx = -1) => {
  if (!subject) return;

  applyCreationVersionChange({
    version,
    type: 'SUBJECT',
    target: subject,
    message: msg,
    emitEditContextChange: (payload) => emit('edit-context-change', payload),
  });
};

/**
 * 切换分镜 `activeVersion` 并上抛编辑上下文（供发送前拼说明）。
 * @param {any} msg
 * @param {any} scene
 * @param {any} version
 * @param {number} [sceneIdx=-1]
 * @returns {void}
 */
const handleSceneVersionChange = (msg, scene, version, sceneIdx = -1) => {
  if (!scene) return;

  applyCreationVersionChange({
    version,
    type: 'SCENE',
    target: scene,
    message: msg,
    emitEditContextChange: (payload) => emit('edit-context-change', payload),
  });
};

/**
 * 分镜脚本编辑上抛（CreationChatPanel 补全 messageId 后写入 sceneScript editContext）。
 * @param {any} msg
 * @param {Record<string, any>} payload
 */
const handleSceneScriptChange = (msg, payload = {}) => {
  emit('edit-context-change', {
    type: 'SCENE_SCRIPT',
    messageId: msg?.messageId,
    ...payload,
  });
};

// 参考图编辑弹窗目标：仅保存当前 msg + 当前 subject，内部 UI/业务逻辑由 SubjectEditDialog 负责
const subjectEditDialogVisible = ref(false); // 参考图编辑弹窗显隐
const subjectEditMessage = ref(null); // 当前编辑的目标消息
const subjectEditTarget = ref(null); // 当前编辑的 subject 对象
const sceneEditDialogVisible = ref(false); // 分镜编辑弹窗显隐
const sceneEditMessage = ref(null); // 当前编辑的目标消息
const sceneEditTarget = ref(null); // 当前编辑的 scene 对象

const sceneVideoPreviewVisible = ref(false); // 分镜视频预览弹窗显隐
const sceneVideoPreviewUrl = ref(''); // 分镜视频预览地址
const sceneVideoPreviewPoster = ref(''); // 分镜视频预览封面

/**
 * 当历史消息被 SSE 按 chunkId 回写替换时：
 * - 若对应编辑弹窗正在打开，且命中的是当前正在编辑的 chunk
 * - 则同步更新弹窗 source/target 引用，确保预览与表单数据实时刷新
 * - 未命中当前弹窗 chunk 时不做任何事，避免污染其他弹窗
 */
const syncOpenEditDialogsFromMessages = () => {
  if (subjectEditDialogVisible.value && subjectEditMessage.value && subjectEditTarget.value) {
    const subjectChunkId = subjectEditTarget.value.messageChunkId;
    if (subjectChunkId) {
      const latestSubject = (subjectEditMessage.value.subjectList || []).find(
          (item) => item?.messageChunkId === subjectChunkId,
      );
      if (latestSubject && latestSubject !== subjectEditTarget.value) {
        subjectEditTarget.value = latestSubject;
      }
    }
  }

  if (sceneEditDialogVisible.value && sceneEditMessage.value && sceneEditTarget.value) {
    const sceneChunkId = sceneEditTarget.value.messageChunkId;
    if (sceneChunkId) {
      const latestScene = (sceneEditMessage.value.sceneList || []).find(
          (item) => item?.messageChunkId === sceneChunkId,
      );
      if (latestScene && latestScene !== sceneEditTarget.value) {
        sceneEditTarget.value = latestScene;
      }
    }
  }
};

watch(
    () => subjectEditDialogVisible.value,
    (visible) => {
      if (visible) return;
      subjectEditMessage.value = null;
      subjectEditTarget.value = null;
    },
);

watch(
    () => sceneEditDialogVisible.value,
    (visible) => {
      if (visible) return;
      sceneEditMessage.value = null;
      sceneEditTarget.value = null;
    },
);

const subjectDialogSyncSignature = computed(() => { // 参考图编辑弹窗同步签名：chunkId 变化时重新同步
  if (!subjectEditDialogVisible.value || !subjectEditMessage.value || !subjectEditTarget.value) return '';
  const chunkId = subjectEditTarget.value.messageChunkId;
  if (!chunkId) return '';
  const current = (subjectEditMessage.value.subjectList || []).find((item) => item?.messageChunkId === chunkId);
  if (!current) return `${chunkId}:missing`;
  const versionsCount = (current.versions || []).length;
  const activeVersion = current.activeVersion ?? -1;
  return `${chunkId}:${versionsCount}:${activeVersion}`;
});

const sceneDialogSyncSignature = computed(() => { // 分镜编辑弹窗同步签名：chunkId 变化时重新同步
  if (!sceneEditDialogVisible.value || !sceneEditMessage.value || !sceneEditTarget.value) return '';
  const chunkId = sceneEditTarget.value.messageChunkId;
  if (!chunkId) return '';
  const current = (sceneEditMessage.value.sceneList || []).find((item) => item?.messageChunkId === chunkId);
  if (!current) return `${chunkId}:missing`;
  const versionsCount = (current.versions || []).length;
  const activeVersion = current.activeVersion ?? -1;
  return `${chunkId}:${versionsCount}:${activeVersion}`;
});

watch(subjectDialogSyncSignature, () => {
  syncOpenEditDialogsFromMessages();
});

watch(sceneDialogSyncSignature, () => {
  syncOpenEditDialogsFromMessages();
});

watch(
    () => props.subjectContinueMsgId,
    () => {
      // 流程目标消息变化时，清空旧消息的本地隐藏态。
      clearGuideDismissedByType('subject');
    },
);

watch(
    () => props.sceneScriptContinueMsgId,
    () => {
      // 流程目标消息变化时，清空旧消息的本地隐藏态。
      clearGuideDismissedByType('sceneScript');
    },
);

watch(
    () => props.sceneContinueMsgId,
    () => {
      // 流程目标消息变化时，清空旧消息的本地隐藏态。
      clearGuideDismissedByType('scene');
    },
);

/**
 * 打开参考图编辑弹窗，并传入编辑目标（由子组件初始化 prompt/refImages，并在完成后 upsert 回 msg.subjectList）。
 * @param {any} msg 当前消息（源消息，用于 upsert 回写）
 * @param {any} subject 要编辑的 subject
 * @returns {void}
 */
const openSubjectEditDialog = (msg, subject) => {
  subjectEditMessage.value = msg;
  subjectEditTarget.value = subject;
  subjectEditDialogVisible.value = true;
};

/**
 * 打开分镜直连编辑弹窗。
 * @param {any} msg
 * @param {any} scene
 * @returns {void}
 */
const openSceneEditDialog = (msg, scene) => {
  sceneEditMessage.value = msg;
  sceneEditTarget.value = scene;
  sceneEditDialogVisible.value = true;
};

/**
 * 分镜封面是否可点击：可编辑时进编辑弹窗；否则有视频时可预览。
 * @param {any} msg
 * @param {number} msgIdx
 * @param {any} sceneItem
 * @returns {boolean}
 */
const canInteractWithSceneMedia = (msg, msgIdx, sceneItem) => {
  if (canShowSceneControls(msg, msgIdx)) return true;
  return !!getActiveSceneVersion(sceneItem)?.videoUrl;
};

/**
 * 在只读模式下用当前版本视频 URL 打开弹窗预览。
 * @param {any} sceneItem
 * @returns {void}
 */
const openSceneVideoPreview = (sceneItem) => {
  const active = getActiveSceneVersion(sceneItem);
  const url = active?.videoUrl;
  if (!url) return;
  sceneVideoPreviewUrl.value = String(url);
  sceneVideoPreviewPoster.value = String(active?.coverUrl || '');
  sceneVideoPreviewVisible.value = true;
};

/** 关闭视频预览弹窗时清空 src，避免残留播放状态。 */
const resetSceneVideoPreview = () => {
  sceneVideoPreviewUrl.value = '';
  sceneVideoPreviewPoster.value = '';
};

/**
 * 分镜封面点击：可编辑 → 编辑弹窗；否则有视频 → 预览弹窗。
 * @param {any} msg
 * @param {number} msgIdx
 * @param {any} sceneItem
 * @returns {void}
 */
const handleSceneMediaClick = (msg, msgIdx, sceneItem) => {
  if (canShowSceneControls(msg, msgIdx)) {
    openSceneEditDialog(msg, sceneItem);
    return;
  }
  if (getActiveSceneVersion(sceneItem)?.videoUrl) {
    openSceneVideoPreview(sceneItem);
  }
};

/**
 * 判断当前用户音频是否正在播放（用于给播放按钮加脉冲动画）。
 * @param {any} msg 用户消息对象
 * @returns {boolean}
 */
const isUserAudioPlaying = (msg) => {
  if (!props.playerState.isPlaying) return false;
  return props.currentUserAudioMessageId === msg?.messageId;
};

const handleDownloadFinalVideo = (video) => { // 下载最终合成视频
  const url = String(video?.videoUrl || '');
  if (!url) return;

  saveUserTracking({
    target: 'CREATE_MV_DOWNLOAD_VIDEO',
  }).catch((error) => {
    console.error('创作页成品MV下载埋点上报失败:', error);
  });

  const link = document.createElement('a');
  link.href = url;
  link.download = String(video?.videoFileId || 'mv_video');
  link.target = '_blank';
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

/** VIDEO 卡片右上角「编辑分镜」：先上报埋点，再上抛打开分镜编辑。 */
const handleEditVideo = (video) => {
  saveUserTracking({
    target: 'MV_VIDEO_EDIT',
  }).catch((error) => {
    console.error('创作页视频编辑埋点上报失败:', error);
  });
  emit('open-storyboard', video);
};
</script>

<style scoped lang="scss">
.custom-scrollbar {
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background-color: rgba(255, 255, 255, 0.05);
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 3px;

    &:hover {
      background-color: rgba(255, 255, 255, 0.3);
    }
  }
}

.user-audio-pulse {
  position: relative;
  animation: userAudioPulse 1.2s ease-in-out infinite;
  box-shadow: 0 0 0 0 rgba(194, 255, 0, 0.55);
}

.user-audio-pulse::after {
  content: '';
  position: absolute;
  inset: 6px;
  border-radius: 10px;
  border: 1px solid rgba(194, 255, 0, 0.65);
  opacity: 0.8;
  animation: userAudioRing 1.2s ease-out infinite;
}


.subject-cancel-btn {
  --el-button-bg-color: rgba(255, 255, 255, 0.08);
  --el-button-border-color: rgba(255, 255, 255, 0.15);
  --el-button-text-color: #fff;
}

.subject-generate-btn {
  --el-button-bg-color: #c2ff00;
  --el-button-border-color: #c2ff00;
  --el-button-text-color: #0d0d0d;
  font-weight: 600;
}

.typing-dot {
  opacity: 0.28;
  animation: dot-flash 1.4s infinite linear;
}

.typing-dots .typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dots .typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

video::-webkit-media-controls-download-button {
  display: none !important;
}

video::-internal-media-controls-download-button {
  display: none !important;
}

@keyframes userAudioPulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(194, 255, 0, 0.5);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 6px rgba(194, 255, 0, 0.2);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(194, 255, 0, 0);
  }
}

@keyframes userAudioRing {
  0% {
    transform: scale(0.9);
    opacity: 0.75;
  }
  100% {
    transform: scale(1.2);
    opacity: 0;
  }
}

/* AI 回复中的 3 点加载动画（与模板 animate-[dot-flash_*] 对应） */
@keyframes dot-flash {
  0%,
  80%,
  100% {
    opacity: 0.28;
    transform: scale(0.86);
    filter: brightness(0.85);
  }
  40% {
    opacity: 1;
    transform: scale(1);
    filter: brightness(1.1);
  }
}
</style>

<style lang="scss">
@use '@/views/creation/styles/creationVersionSelect.scss';

.creation-message-area {
  .markdown-body-chat {
    background: transparent !important;
    color: inherit !important;
    font-family: inherit !important;
    font-size: 16px !important;
    line-height: 24px !important;

    blockquote {
      border-left: 0 !important;
      padding-bottom: 16px !important;
      border-bottom: 2px solid rgba(255, 255, 255, 0.1) !important;
    }

    hr {
      height: 0 !important;
      background: transparent !important;
      margin: 0!important;
    }

    pre {
      background: rgba(0, 0, 0, 0.35) !important;
      border-radius: 12px;
    }

    code {
      font-size: 0.92em;
    }

    table {
      margin: 12px 0;
      // 撑满父容器，让 cell-long 能吃掉除 cell-short 之外的剩余宽度
      width: 100% !important;
    }

    th,
    td {
      padding-left: 6px !important;
      padding-right: 6px !important;
    }

    // 短内容单元格：精确按 max-content 占宽、绝不换行
    // 长内容单元格：不设宽度，自动吃掉剩余宽度并按 pre-wrap 自然换行
    // 类由 markdown 解析阶段 annotateTableCells 按内容视觉宽度打上（th 永远视作短）
    th,
    td.cell-short {
      width: max-content !important;
      white-space: nowrap !important;
    }

    td.cell-long {
      white-space: pre-wrap !important;
    }
  }
}

</style>

<style lang="scss">
.scene-video-preview-modal.el-overlay {
  background: rgba(0, 0, 0, 0.7) !important;
  @apply backdrop-blur-[2px];
}

.scene-video-preview-dialog.el-dialog {
  width: min(96vw, 1000px) !important;
  margin: auto !important;
  background: #121212 !important;
  border-radius: 16px !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.55) !important;
}

.scene-video-preview-dialog .el-dialog__header {
  padding: 12px 16px 8px !important;
  margin: 0 !important;
}

.scene-video-preview-dialog .el-dialog__title {
  color: rgba(255, 255, 255, 0.92);
  font-size: 16px;
  font-weight: 600;
}

.scene-video-preview-dialog .el-dialog__headerbtn {
  top: 10px;
  right: 12px;
}

.scene-video-preview-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 255, 255, 0.65);
}

.scene-video-preview-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  color: #C2FF00;
}

.scene-video-preview-dialog .el-dialog__body {
  padding: 0 16px 16px !important;
}

.scene-video-preview-player {
  display: block;
  width: 100%;
  max-height: min(82vh, 800px);
  border-radius: 12px;
  background: #000;
  object-fit: contain;
}

/* SCENE 引导区域开关：旋钮改为黑色 */
.scene-guide-switch .el-switch__action {
  background-color: #000 !important;
}
</style>
