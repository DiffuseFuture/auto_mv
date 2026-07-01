<!-- 移动端首页 -->
<template>
  <div class="h-full w-full">
    <!-- 背景动图：双图层交叉淡入淡出，定时轮换三段 GIF -->
    <img
        :src="layerSources[0]"
        :class="activeLayer === 0 ? 'opacity-100' : 'opacity-0'"
        class="pointer-events-none fixed inset-0 z-0 h-full w-full object-cover transition-opacity duration-700 ease-in-out"
        alt=""
    />
    <img
        :src="layerSources[1]"
        :class="activeLayer === 1 ? 'opacity-100' : 'opacity-0'"
        class="pointer-events-none fixed inset-0 z-0 h-full w-full object-cover transition-opacity duration-700 ease-in-out"
        alt=""
    />

    <!-- 底部输入框容器（固定底部，滚过首屏后收缩） -->
    <div class="fixed bottom-[85px] z-[5] w-full px-[15px]">
      <input
          ref="fileInputRef"
          type="file"
          :accept="UPLOAD_ACCEPT"
          class="hidden"
          multiple
          @change="handleFileChange"
      />
      <MobileMusicTrimmer
          v-model="showTrimmer"
          :raw-file="rawFile"
          :duration="audioDuration"
          :preparing="isPreparingAudio"
          :dialog-width="'calc(100vw - 24px)'"
          @confirm="handleTrimmerConfirm"
          @cancel="showTrimmer = false"
      />
      <!--
        阴影外层 wrapper：单独负责 box-shadow + border-radius，不带 bg / backdrop-filter。
        iOS Safari 在同一元素上叠加 (半透明 bg + backdrop-blur + box-shadow + border-radius) 时，
        圆角 mask 经过多层合成后精度变差，会在圆角内侧露出一条阴影色（看上去就是绿色尖角）。
        把 shadow 剥离到独立 wrapper 上，shadow 的合成只过一遍简单圆角 mask，就不会再漏色。
      -->
      <div
          class="w-full rounded-[10px] shadow-[0px_0px_10px_0px_#C2FF00] transition-all duration-300"
          :class="isPromptCompact && !hasAttachments ? 'h-[40px]' : 'h-[145px]'"
      >
      <div
          class="w-full h-full rounded-[10px] bg-[rgba(216,216,216,0.2)] backdrop-blur-[40px] overflow-hidden"
      >
        <div v-if="!isPromptCompact || hasAttachments" class="h-full flex flex-col">
          <!-- 上传附件列表 -->
          <div v-if="hasAttachments" class="flex gap-2 px-[10px] pt-[10px]">
            <div v-for="(img, idx) in uploadedImages" :key="img.id" class="intention-select-host relative group h-[40px] w-[40px] shrink-0">
              <el-image
                  :src="img.previewUrl"
                  :preview-src-list="uploadedImages.map(i => i.previewUrl)"
                  :hide-on-click-modal="true"
                  :initial-index="idx"
                  preview-teleported
                  fit="cover"
                  class="h-[40px] w-[40px] rounded-[5px] object-cover"
              />
              <div
                  v-if="img.uploading"
                  class="absolute inset-0 z-[3] flex-center rounded-[5px] bg-black/45 backdrop-blur-[1px]"
              >
                <div class="h-4 w-4 animate-spin rounded-full border-2 border-[#C2FF00]/30 border-t-[#C2FF00]"></div>
              </div>
              <button
                  type="button"
                  class="absolute -right-[3px] -top-[6px] z-[2] flex-center border-0"
                  :disabled="img.uploading"
                  :class="img.uploading ? 'pointer-events-none opacity-40' : ''"
                  @click="handleRemoveImage(idx)"
              >
                <svg-icon name="gy-closure" size="12" color="white"></svg-icon>
              </button>
              <el-select
                  v-model="img.intention"
                  size="small"
                  class="mobile-intention-select"
                  popper-class="intention-dropdown"
                  :teleported="true"
                  :show-arrow="false"
                  :disabled="img.uploading"
                  :class="img.uploading ? 'pointer-events-none opacity-40' : ''"
                  @click.stop
              >
                <template #prefix>
                  <svg-icon :name="getIntentionIcon(img.intention)" size="10" color="#C2FF00"></svg-icon>
                </template>
                <el-option v-for="option in intentionOptions" :key="`${img.id}-${option.value}`" :label="option.label" :value="option.value">
                  <div class="intention-option">
                    <svg-icon :name="option.icon" size="12" color="currentColor"></svg-icon>
                    <span>{{ option.label }}</span>
                  </div>
                </el-option>
              </el-select>
            </div>
            <div v-if="uploadedAudio" class="relative flex-center h-[40px] w-[40px] shrink-0 rounded-[5px] bg-[rgba(255,255,255,0.1)]">
              <svg-icon name="gy-audiofiles" size="18" color="#C2FF00"></svg-icon>
              <div
                  v-if="isUploadingTrimmedAudio"
                  class="absolute inset-0 z-[3] flex-center rounded-[5px] bg-black/45 backdrop-blur-[1px]"
              >
                <el-icon class="animate-spin text-[#C2FF00]" size="16">
                  <Loading />
                </el-icon>
              </div>
              <button
                  type="button"
                  class="absolute -right-[3px] -top-[6px] z-[2] flex-center border-0"
                  :disabled="isUploadingTrimmedAudio"
                  :class="isUploadingTrimmedAudio ? 'pointer-events-none opacity-40' : ''"
                  @click="handleRemoveAudio"
              >
                <svg-icon name="gy-closure" size="12" color="white"></svg-icon>
              </button>
            </div>
          </div>

          <!-- 文本域 -->
          <div class="relative min-h-0 flex-1 overflow-hidden px-[10px] pt-[10px]">
            <el-input
                v-model="promptText"
                type="textarea"
                resize="none"
                :placeholder="''"
                class="home-mobile-prompt-input h-full"
                :class="hasAttachments ? 'home-mobile-prompt-input--with-attachments' : ''"
                @compositionstart="handleCompositionStart"
                @compositionend="handleCompositionEnd"
            />
            <Transition name="placeholder-slide">
              <span
                  v-if="!promptText && !isComposing"
                  :key="placeholderIndex"
                  class="pointer-events-none absolute left-[10px] right-[10px] top-[10px] whitespace-normal break-words text-[14px] leading-6 text-white/65"
              >{{ rotatingPlaceholder }}</span>
            </Transition>
          </div>

          <!-- 底部控件 -->
          <div class="flex-between shrink-0 p-[10px]">
            <div class="flex-center gap-3">
              <span class="flex-center" @click="ensureLoggedInForUpload() && fileInputRef.click()">
                <svg-icon name="gy-upload" size="25" color="#ADE300"></svg-icon>
              </span>
              <span class="flex-center cursor-pointer" @click="modelPopoverVisible = true">
                <svg-icon name="gy-model" size="23" color="#ADE300"></svg-icon>
              </span>
              <el-popover
                  v-model:visible="resolutionPopoverVisible"
                  placement="top-start"
                  trigger="click"
                  popper-class="resolution-select-dropdown-m"
                  :show-arrow="false"
                  :offset="10"
              >
                <template #reference>
                  <div class="inline-flex h-[25px] items-center gap-1 rounded-[3px] bg-[rgba(216,216,216,0.15)] px-[5px] cursor-pointer text-[12px] shrink-0">
                    <span class="text-[#C2FF00]">{{ selectedResolutionOption?.tag }}</span>
                    <span class="text-white">{{ selectedResolutionOption?.label }}</span>
                  </div>
                </template>
                <div class="resolution-select-body">
                  <div
                      v-for="item in RESOLUTION_OPTIONS"
                      :key="item.value"
                      class="el-select-dropdown__item"
                      :class="{ 'is-selected': item.value === modelStore.selectedResolution }"
                      @click="handleSelectResolution(item.value)"
                  >
                    <span class="scene-model-item-main">{{ item.label }}</span>
                  </div>
                </div>
              </el-popover>
            </div>
            <button
                type="button"
                class="h-[31px] px-[15px] bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] rounded-[5px] flex-center text-[12px] disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="audioTrimPending"
                @click="handleStartCreation"
            >
              <span>{{ t('home.createNow') }}</span>
              <span v-if="isNotEmpty(uploadedAudioEstimatedPoints)" class="mx-1.5 h-3.5 w-px bg-black/30"></span>
              <span v-if="isNotEmpty(uploadedAudioEstimatedPoints)" class="text-[12px]">
                {{ uploadedAudioEstimatedPoints }}{{ t('common.points') }}
              </span>
            </button>
          </div>
        </div>
        <div v-else class="h-full px-[10px] flex-between">
          <div class="flex items-center gap-3 text-[12px] text-white">
            <span class="flex-center" @click="ensureLoggedInForUpload() && fileInputRef.click()">
              <svg-icon name="gy-upload" size="25" color="#ADE300"></svg-icon>
            </span>
            <span class="flex-center cursor-pointer" @click="modelPopoverVisible = true">
              <svg-icon name="gy-model" size="23" color="#C2FF00"></svg-icon>
            </span>
            <el-popover
                v-model:visible="resolutionPopoverVisible"
                placement="top-start"
                trigger="click"
                popper-class="resolution-select-dropdown-m"
                :show-arrow="false"
                :offset="10"
            >
              <template #reference>
                <div class="inline-flex h-[25px] items-center gap-1 rounded-[3px] bg-[rgba(216,216,216,0.15)] px-[5px] cursor-pointer text-[12px] shrink-0">
                  <span class="text-[#C2FF00]">{{ selectedResolutionOption?.tag }}</span>
                  <span class="text-white">{{ selectedResolutionOption?.label }}</span>
                </div>
              </template>
              <div class="resolution-select-body">
                <div
                    v-for="item in RESOLUTION_OPTIONS"
                    :key="item.value"
                    class="el-select-dropdown__item"
                    :class="{ 'is-selected': item.value === modelStore.selectedResolution }"
                    @click="handleSelectResolution(item.value)"
                >
                  <span class="scene-model-item-main">{{ item.label }}</span>
                </div>
              </div>
            </el-popover>
            <div class="relative w-[110px] h-[24px] overflow-hidden">
              <el-input
                  v-model="promptText"
                  :placeholder="''"
                  class="home-mobile-mini-input"
                  @compositionstart="handleCompositionStart"
                  @compositionend="handleCompositionEnd"
              />
              <Transition name="placeholder-slide">
                <span
                    v-if="!promptText && !isComposing"
                    :key="placeholderIndex"
                    class="pointer-events-none absolute inset-y-0 left-0 right-0 truncate leading-6 text-white/65"
                >{{ rotatingPlaceholder }}</span>
              </Transition>
            </div>
          </div>
          <button
              type="button"
              class="flex-center disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="audioTrimPending"
              @click="handleStartCreation"
          >
            <svg-icon name="gy-fasong" size="25" color="#C2FF00"></svg-icon>
          </button>
        </div>
      </div>
      </div>
    </div>

    <!-- 第一屏内容 -->
    <section class="relative z-[1] h-screen">
      <!-- 页面顶部 -->
      <div class="flex-between px-5 pt-7">
        <!-- 左侧logo -->
        <div class="flex-center">
          <el-image :src="logo2Img" class="w-[25px] aspect-square mr-[7px]"></el-image>
          <div class="text-white text-[18px] font-extrabold">Oh<span class="text-[#C2FF00]">Yes</span>AI</div>
        </div>

        <!-- 右侧用户相关 -->
        <div class="flex-center gap-[5px]">
          <div
              v-if="userStore.isLoggedIn"
              class="w-[25px] aspect-square rounded-[5px] bg-[rgba(76,83,52,0.6)] flex-center"
              @click="handleOpenInviteDialog"
          >
            <svg-icon name="gy-share-gift" color="#C2FF00" size="16"></svg-icon>
          </div>
          <el-popover
              trigger="click"
              placement="bottom-end"
              :show-arrow="false"
              popper-class="home-mobile-wechat-popover"
              :offset="4"
              @show="handleViewWechatQr"
          >
            <template #reference>
              <div class="w-[25px] aspect-square rounded-[5px] bg-[rgba(76,83,52,0.6)] flex-center">
                <svg-icon name="gy-WeChat" color="#C2FF00" size="16"></svg-icon>
              </div>
            </template>
            <div class="w-full h-full flex justify-between">
              <div class="flex-col-center">
                <el-image class="w-[100px] mb-[5px] rounded-[10px] aspect-square" :src="wechatGroupImg"></el-image>
                <div class="text-[#C2FF00] text-[8px] leading-[11px]">{{ t('layout.wechat.joinGroup') }}</div>
                <div class="text-[#A8A8A8] text-[6px] leading-[11px]">{{ t('layout.wechat.joinGroupDesc') }}</div>
              </div>
              <div class="flex-col-center">
                <el-image class="w-[100px] mb-[5px] rounded-[10px] aspect-square" :src="kefuImg"></el-image>
                <div class="text-[#C2FF00] text-[8px] leading-[11px]">{{ t('layout.wechat.contactSupport') }}</div>
                <div class="text-[#A8A8A8] text-[6px] leading-[11px]">{{ t('layout.wechat.contactSupportDesc') }}</div>
              </div>
            </div>
          </el-popover>
          <!-- 用户头像，点击出现用户信息气泡 -->
          <el-popover
              v-if="userStore.isLoggedIn"
              v-model:visible="accountPopoverVisible"
              trigger="click"
              placement="bottom-end"
              :show-arrow="false"
              popper-class="home-mobile-account-popover"
              :width="162"
              :offset="4"
          >
            <template #reference>
              <el-avatar
                  :src="userStore.avatar"
                  :size="30"
              />
            </template>
            <div class="px-[6px] py-2">
              <div class="rounded-[5px] border border-[#585858] bg-[#2D2D2D] p-[6px] mb-[10px]">
                <div class="flex-between leading-none">
                  <span class="text-[11px] text-white">{{ t('layout.account.currentPlan') }}</span>
                  <span class="text-[9px] text-[#BDBDBD]">{{ userStore.userPlan?.tierName }}</span>
                </div>
                <div class="border-b border-b-[#585858] my-[7px]"></div>
                <div class="flex-between leading-none text-[11px] text-[#BDBDBD]">
                  <span class="text-[11px]">{{ t('layout.account.remainPoints') }}</span>
                  <span class="text-[9px]">{{ userStore.userPlan?.pointsBalance }}</span>
                </div>
              </div>
              <button
                  class="w-full mb-[7px] border-none px-[7px] text-left text-[11px] leading-[30px] text-white"
                  type="button"
                  @click="handleOpenPointsDetailDialog"
              >
                {{ t('layout.account.pointsDetail') }}
              </button>
              <button
                  class="block mb-[15px] border-none bg-transparent text-left text-[11px] px-[7px] leading-[15px] text-white"
                  type="button"
                  @click="openEditProfileDialog"
              >
                {{ t('layout.account.editProfile') }}
              </button>
              <button
                  class="flex-between w-full mb-[9px] border-none bg-transparent text-left text-[11px] px-[7px] leading-[15px] text-white"
                  type="button"
                  @click="handleLogoutClick"
              >
                <span>{{ t('layout.account.logout') }}</span>
                <svg-icon name="gy-quit" size="12"></svg-icon>
              </button>
            </div>
          </el-popover>
          <div
              v-else
              class="w-14 h-[25px] rounded-[5px] bg-[linear-gradient(299deg,#BEFA00_0%,#82FF79_100%)] flex-center text-black text-[12px]"
              @click="loginVisible = true"
          >
            {{ t('layout.account.pleaseLogin') }}
          </div>
        </div>
      </div>

      <!-- 大标题 -->
      <div class="px-4 pt-[157px] text-center">
        <h1 class="hero-title-gradient mb-[6px] text-[32px] font-bold">
          Visualize your sound
        </h1>
        <p class="text-[15px] text-white">
          {{ t('home.heroSubtitle') }}
        </p>
      </div>
    </section>

    <!-- 创作广场 -->
    <section class="relative z-[1] bg-[linear-gradient(180deg,rgba(0,0,0,0)_0%,rgba(0,0,0,1)_20%,rgba(0,0,0,1)_100%)] px-3 pt-6 pb-[136px]">
      <div class="mb-4 text-[20px] font-semibold leading-7 text-white">
        {{ t('home.worksSquareTitle') }}
      </div>

      <div v-if="worksLoading && worksList.length === 0" class="flex-center py-12 text-white/50">
        {{ t('common.loading') }}
      </div>
      <div v-else-if="!worksLoading && worksList.length === 0" class="flex-center py-12 text-white/50">
        {{ t('home.emptyWorks') }}
      </div>

      <!-- 瀑布流 -->
      <MasonryWall
          v-else
          :items="worksList"
          :column-width="168"
          :gap="10"
          :max-columns="2"
      >
        <template #default="{ item }">
          <div class="mb-2" @click="handleOpenShare(item)">
            <div class="relative overflow-hidden rounded-xl bg-white/5" :style="{aspectRatio: item._coverRatio || 16 / 9}">
              <img
                  v-if="item.fileCoverUrl"
                  :src="item.fileCoverUrl"
                  :alt="item.projectName"
                  class="block h-full w-full object-cover"
                  loading="lazy"
              />
              <div v-else class="flex-center h-full w-full bg-white/10">
                <svg-icon name="gy-MV" size="24" color="rgba(255,255,255,0.3)"></svg-icon>
              </div>
              <div class="absolute bottom-0 left-0 right-0 flex items-end justify-between bg-gradient-to-t from-black/70 to-transparent px-2 pb-1.5 pt-6">
                <div class="min-w-0 flex items-center gap-1">
                  <img :src="item.avatar" :alt="item.nickName" class="h-4 w-4 shrink-0 rounded-full object-cover"/>
                  <span class="truncate text-[10px] text-white">{{ item.nickName }}</span>
                </div>
                <span v-if="item.duration" class="ml-1 shrink-0 text-[10px] text-white/80">{{
                    formatDuration(item.duration)
                  }}</span>
              </div>
            </div>
            <div class="mt-1 truncate px-1 text-[12px] text-white/85">《{{ item.projectName }}》</div>
          </div>
        </template>
      </MasonryWall>

      <div v-if="worksLoading && worksList.length > 0" class="flex-center py-4 text-[12px] text-white/50">
        {{ t('home.loadMore') }}
      </div>
      <div v-if="!worksHasMore && worksList.length > 0" class="flex-center py-4 text-[12px] text-white/30">
        {{ t('account.points.noMore') }}
      </div>
    </section>

    <!-- 积分使用详情弹窗 -->
    <el-dialog
        v-model="pointsDetailDialogVisible"
        class="home-mobile-points-dialog"
        :show-close="false"
        :append-to-body="true"
        width="calc(100vw - 40px)"
        align-center
        destroy-on-close
        @opened="handlePointsDetailOpened"
    >
      <template #default>
        <div class="w-full rounded-[10px] bg-white px-[5px] pb-[33px] pt-[19px] box-border">
          <div class="mb-[12px] px-[5px] flex-between">
            <div class="text-[15px] font-medium leading-none text-[#192100]">{{ t('account.points.detail') }}</div>
            <div @click="pointsDetailDialogVisible = false">
              <svg-icon name="gy-closure" size="15" color="#3D3D3D"></svg-icon>
            </div>
          </div>

          <div class="h-[74px] px-[10px] mb-[18px] rounded-[10px] bg-[#F0F6DD] flex flex-col justify-center">
            <div class="flex-between">
              <span class="text-[16px] leading-none text-[#2a2a0e]">{{
                  userStore.userPlan?.tierName || t('layout.account.freePlan')
                }}</span>
              <button class="w-[48px] h-[25px] rounded-[5px] border-0 bg-[#292929] text-[14px] font-medium text-[#C2FF00]" type="button" @click="handleUpgradeFromPointsDetail">
                {{ t('common.upgrade') }}
              </button>
            </div>
            <div class="mt-1 mb-[10px] h-px bg-[#AEAEAE]"></div>
            <div class="flex items-center justify-between">
              <span class="text-[16px] leading-none text-[#192100]">{{ t('common.points') }}</span>
              <span class="text-[14px] leading-none text-[#192100]">{{ userStore.userPlan?.pointsBalance ?? 0 }}</span>
            </div>
          </div>

          <div class="overflow-hidden rounded-[10px] bg-[#F0F6DD]">
            <div class="h-6 px-[10px] flex items-center bg-[#192100] text-[14px] font-medium text-[#C2FF00]">
              <span class="flex-1">{{ t('account.points.detail') }}</span>
              <span class="w-[60px] text-right">{{ t('account.points.value') }}</span>
            </div>
            <div class="px-[10px] py-[7px] h-64 overflow-y-auto no-scrollbar" @scroll="handlePointsLogScroll">
              <div v-if="pointsLogLoading && !pointsLogList.length" class="py-8 text-center text-[#A7A7A6] text-[14px]">
                {{ t('common.loading') }}
              </div>
              <div v-else-if="!pointsLogList.length" class="py-8 text-center text-[#A7A7A6] text-[14px]">
                {{ t('account.points.empty') }}
              </div>
              <template v-else>
                <div v-for="item in pointsLogList" :key="item.id" class="flex justify-between py-[6.5px]">
                  <span class="flex-1 text-[12px] text-[#192100] truncate">{{ item.description }}</span>
                  <span class="w-[60px] text-right text-[12px]" :class="item.amount > 0 ? 'text-[#52c41a]' : 'text-[#192100]'">{{
                      item.amount > 0 ? '+' : ''
                    }}{{ item.amount }}</span>
                </div>
                <div v-if="pointsLogLoading" class="py-[6px] text-center text-[#A7A7A6] text-[12px]">
                  {{ t('common.loading') }}
                </div>
                <div v-else-if="!hasMorePointsLog" class="py-[6px] text-center text-[#A7A7A6] text-[12px]">
                  {{ t('account.points.noMore') }}
                </div>
              </template>
            </div>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 个人资料编辑弹窗 -->
    <el-dialog
        v-model="editProfileDialogVisible"
        class="home-mobile-edit-profile-dialog"
        :show-close="false"
        :append-to-body="true"
        width="calc(100vw - 40px)"
        align-center
        destroy-on-close
        @opened="syncEditProfileState"
        @closed="handleEditProfileDialogClosed"
    >
      <template #default>
        <div class="w-full bg-white">
          <div class="mb-[16px] flex items-center justify-between">
            <div class="text-[18px] font-medium leading-none text-[#192100]">{{ t('layout.account.profile') }}</div>
            <button type="button" class="flex-center border-0 bg-transparent p-0" @click="editProfileDialogVisible = false">
              <svg-icon name="gy-closure" size="18" color="#3D3D3D"></svg-icon>
            </button>
          </div>

          <!-- 头像 -->
          <div class="mb-[18px] flex flex-col items-center">
            <button
                type="button"
                class="relative h-[90px] w-[90px] overflow-hidden rounded-full border-0 bg-[#F0F6DD] p-0"
                @click="editProfileFileInputRef?.click()"
            >
              <el-avatar :src="editProfileAvatarUrl || defaultAvatar" :size="90"/>
              <div class="absolute inset-0 flex-center bg-black/50">
                <svg-icon name="gy-avatar" size="15" color="white"></svg-icon>
              </div>
            </button>
            <input
                ref="editProfileFileInputRef"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleEditProfileAvatarChange"
            />
          </div>

          <!-- 昵称 -->
          <div>
            <div class="mb-[6px] text-[12px] leading-4 text-[#192100]">{{ t('account.profile.nickname') }}</div>
            <div class="flex items-center gap-[8px] rounded-[10px] bg-[#F0F6DD] px-[10px] py-[8px]">
              <input
                  v-model="editProfileNickName"
                  type="text"
                  inputmode="text"
                  enterkeyhint="done"
                  autocapitalize="off"
                  autocomplete="nickname"
                  class="w-full border-0 bg-transparent text-[14px] leading-5 text-[#192100] outline-none placeholder:text-[#A7A7A6]"
                  :placeholder="t('account.profile.nicknamePlaceholder')"
                  @blur="handleEditProfileSave"
              />
              <button v-if="editProfileNickName" type="button" class="flex-center border-0 bg-transparent p-0" @click="editProfileNickName = ''">
                <svg-icon name="gy-closure" size="14" color="#A7A7A6"></svg-icon>
              </button>
            </div>
          </div>

          <div class="mt-[28px] mb-[26px] h-px bg-[#D8D8D8]"></div>

          <!-- 删除账号 -->
          <div class="flex-between">
            <div>
              <div class="mb-[5px] text-[14px] font-medium leading-[16px] text-[#0A0B0E]">
                {{ t('account.profile.deleteAccount') }}
              </div>
              <div class="text-[12px] leading-[14px] text-[#9F9F9F]">{{ t('account.profile.deleteDesc') }}</div>
            </div>
            <button type="button" class="border-0 bg-transparent text-[12px] text-[#FF2836]" @click="handleDeleteAccount">
              {{ t('account.profile.deleteAction') }}
            </button>
          </div>

          <div class="mb-[9px] mt-[20px] h-px bg-[#D8D8D8]"></div>

          <!-- API key生成 -->
          <div class="mb-[5px] text-[14px] font-medium leading-[16px] text-[#0A0B0E]">{{
              t('account.apiKey.title')
            }}
          </div>
          <div class="flex-between">
            <div class="flex-center gap-2">
              <div class="text-[12px] leading-[14px] text-[#9F9F9F] truncate">
                {{ userStore.apiKey ? apiKeyMasked : t('account.apiKey.desc') }}
              </div>
              <button
                  v-if="userStore.apiKey"
                  type="button"
                  class="border-0 bg-transparent"
                  @click="handleCopyApiKey"
              >
                <svg-icon name="gy-copy" size="16" color="#9F9F9F"></svg-icon>
              </button>
            </div>
            <button
                type="button"
                class="border-0 bg-transparent text-[12px] leading-4 text-[#52c41a]"
                @click="handleCreateApiKey"
            >
              {{ userStore.apiKey ? t('account.apiKey.regenerate') : t('account.apiKey.generate') }}
            </button>
          </div>

        </div>
      </template>
    </el-dialog>

    <invite-dialog v-model="inviteDialogVisible"></invite-dialog>

    <MobileLoginSheet v-model="loginVisible" />

    <!-- 模型选择底部抽屉：从底部上滑、宽度占满，替代旧的 popover -->
    <el-drawer
        v-model="modelPopoverVisible"
        direction="btt"
        :show-close="false"
        :with-header="false"
        size="auto"
        class="home-mobile-model-drawer"
    >
      <div class="px-[18px] pt-[20px] pb-[calc(env(safe-area-inset-bottom)+18px)]">
        <div class="mb-3 text-[15px] font-bold leading-5 text-white">{{ t('home.model') }}</div>
        <div
            v-for="item in modelStore.modelOptions"
            :key="item.value"
            class="mb-2 flex h-12 items-center rounded-[10px] px-3 transition-colors"
            :class="item.value === modelStore.selectedModel ? 'bg-[linear-gradient(299deg,rgba(190,250,0,0.5)_0%,rgba(130,255,121,0.5)_100%)]' : 'bg-transparent active:bg-[rgba(164,228,60,0.2)]'"
            @click="handleSelectModel(item.value); modelPopoverVisible = false"
        >
          <span class="text-[14px] font-semibold leading-5 text-white">{{ item.label }}</span>
          <span class="ml-auto text-[12px] font-medium leading-4 text-white/55">{{ item.costText }}</span>
        </div>
      </div>
    </el-drawer>

  </div>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import MasonryWall from '@yeger/vue-masonry-wall';
import {ElMessage, ElIcon} from 'element-plus';
import {Loading} from '@element-plus/icons-vue';
import InviteDialog from '@/components/InviteDialog.vue';
import MobileLoginSheet from '@/components/mobile/MobileLoginSheet.vue';
import {useAudioTrimUpload} from '@/composables/useAudioTrimUpload.js';
import {
  AUDIO_UPLOAD_ACCEPT,
  compressImageBeforeUpload,
  isSupportedAudioUpload,
  isSupportedImageUpload,
  IMAGE_UPLOAD_ACCEPT,
} from '@/views/creation/utils/upload.js';
import {formatDuration, isNotEmpty} from '@/utils/index.js';
import {getPointsPrice} from '@/api/creation.js';
import {shareLink} from '@/api/share';
import {saveUserTracking} from '@/api/tracking';
import {uploadFile} from '@/api/creation.js';
import logo2Img from '@/assets/common/logo2.png';
import kefuImg from '@/assets/common/kefu.jpeg';
import wechatGroupImg from '@/assets/common/wechat-group.jpeg';
import bg1Gif from '@/assets/home/bg1.gif';
import bg2Gif from '@/assets/home/bg2.gif';
import bg3Gif from '@/assets/home/bg3.gif';
import {useI18nText} from '@/i18n';
import {useUserStore} from '@/store/user';
import {useModelStore, RESOLUTION_OPTIONS} from '@/store/model';
import {useRotatingPlaceholder} from './composables/useRotatingPlaceholder.js';
import {useHomeWorksWall} from './composables/useHomeWorksWall.js';
import {usePointsLog} from './composables/usePointsLog.js';
import {useAccountProfileDialog} from './composables/useAccountProfileDialog.js';
import MobileMusicTrimmer from '@/views/home/components/MobileMusicTrimmer.vue';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const modelStore = useModelStore();
const isPromptCompact = ref(false);
const accountPopoverVisible = ref(false);
const inviteDialogVisible = ref(false);
const pointsDetailDialogVisible = ref(false);
const {
  editProfileDialogVisible,
  editProfileNickName,
  editProfileAvatarUrl,
  apiKeyMasked,
  openEditProfileDialog,
  syncEditProfileState,
  handleEditProfileAvatarChange,
  handleEditProfileSave,
  handleDeleteAccount,
  handleCreateApiKey,
  handleCopyApiKey,
  handleEditProfileDialogClosed,
} = useAccountProfileDialog();
const editProfileFileInputRef = ref(null);
const modelPopoverVisible = ref(false);
const resolutionPopoverVisible = ref(false);
/** 当前选中分辨率选项（含清晰度标签 tag 与展示名 label）。 */
const selectedResolutionOption = computed(() => RESOLUTION_OPTIONS.find((item) => item.value === modelStore.selectedResolution));
const fileInputRef = ref(null);
const uploadedImages = ref([]);
const uploadedAudio = ref(null);
const hasAttachments = computed(() => uploadedImages.value.length > 0 || !!uploadedAudio.value);
const UPLOAD_ACCEPT = `${IMAGE_UPLOAD_ACCEPT},${AUDIO_UPLOAD_ACCEPT}`;
const {t} = useI18nText();
const {showTrimmer, rawFile, audioDuration, isPreparingAudio, isUploadingTrimmedAudio, openWithFile, uploadTrimmedBlob} = useAudioTrimUpload();
const uploadedAudioEstimatedPoints = ref(null);
const uploadedAudioTrimDuration = ref(0);
const TEMP_AUDIO_FILE_ID_PREFIX = 'temp_audio_';
const TEMP_IMAGE_FILE_ID_PREFIX = 'temp_img_';
const DEFAULT_IMAGE_COUNT = 7;
const IMAGE_POINTS_PER_UNIT = 28;
const POINTS_UPLIFT_RATE = 0.15;
const audioTrimPending = computed(() => String(uploadedAudio?.value?.fileId || '').startsWith(TEMP_AUDIO_FILE_ID_PREFIX) || isPreparingAudio.value || isUploadingTrimmedAudio.value);
const imageUploadPending = computed(() => uploadedImages.value.some((img) => img?.uploading || String(img?.fileId || '').startsWith(TEMP_IMAGE_FILE_ID_PREFIX)));
const isAudioPointsInsufficient = computed(() => {
  if (!isNotEmpty(uploadedAudioEstimatedPoints.value)) return false;
  if (!isNotEmpty(userStore.pointsBalance)) return false;
  return uploadedAudioEstimatedPoints.value > userStore.pointsBalance;
});
const intentionOptions = computed(() => [
  {value: 'CHARACTER', label: t('creation.intention.character'), icon: 'gy-character'},
  {value: 'COSTUME', label: t('creation.intention.costume'), icon: 'gy-clothes'},
  {value: 'ENVIRONMENT', label: t('creation.intention.environment'), icon: 'gy-environment'},
  {value: 'PROP', label: t('creation.intention.prop'), icon: 'gy-prop'},
  {value: 'STYLE', label: t('creation.intention.style'), icon: 'gy-style'},
]);
const getIntentionIcon = (value) => intentionOptions.value.find((item) => item.value === value)?.icon || 'gy-character';

/**
 * 校验当前用户是否已登录。
 *
 * 未登录时给出统一提示并阻止后续上传或创建操作。
 * @returns {boolean} 是否已登录
 */
const ensureLoggedInForUpload = () => {
  if (userStore.isLoggedIn) return true;
  ElMessage.warning(t('common.loginRequired'));
  return false;
};

/**
 * 释放图片预览地址，避免对象 URL 泄漏。
 * @param {{ previewUrl?: string } | null | undefined} img 图片条目
 * @returns {void}
 */
const revokeImagePreview = (img) => {
  if (img?.previewUrl) URL.revokeObjectURL(img.previewUrl);
};

/**
 * 释放音频预览地址，避免对象 URL 泄漏。
 * @param {{ previewUrl?: string } | null | undefined} audio 音频条目
 * @returns {void}
 */
const revokeAudioPreview = (audio) => {
  if (audio?.previewUrl) URL.revokeObjectURL(audio.previewUrl);
};

/**
 * 清空已上传的音频附件与相关预估状态。
 *
 * 这个方法会同时移除缩略图、裁剪时长和预估积分，确保下次上传从干净状态开始。
 * @returns {void}
 */
const clearUploadedAudio = () => {
  revokeAudioPreview(uploadedAudio.value);
  uploadedAudio.value = null;
  uploadedAudioTrimDuration.value = 0;
  uploadedAudioEstimatedPoints.value = null;
};

/**
 * 删除指定索引的图片附件。
 * @param {number} idx 图片索引
 * @returns {void}
 */
const handleRemoveImage = (idx) => {
  const [removed] = uploadedImages.value.splice(idx, 1);
  revokeImagePreview(removed);
};

/**
 * 删除当前音频附件。
 * @returns {void}
 */
const handleRemoveAudio = () => {
  clearUploadedAudio();
};

onBeforeUnmount(() => {
  uploadedImages.value.forEach(revokeImagePreview);
  clearUploadedAudio();
});

/**
 * 计算当前音频在所选模型下的预消耗积分。
 * 公式与 PC 端保持一致：先获取音频主体积分，再叠加默认图片数量的成本，最后乘以统一上浮比例。
 * @returns {Promise<number>}
 */
const estimateUploadedAudioPoints = async () => {
  const mvPoints = await getPointsPrice({
    modelName: modelStore.selectedModel,
    taskType: 'MAKE_MV',
    duration: uploadedAudioTrimDuration.value,
    // 首页为全新项目（无 session），分辨率由用户当前选择，后端无 session 可取，需显式带上
    resolution: modelStore.selectedResolution,
  });
  const imagePoints = DEFAULT_IMAGE_COUNT * IMAGE_POINTS_PER_UNIT;
  return Math.ceil((mvPoints + imagePoints) * (1 + POINTS_UPLIFT_RATE));
};

/**
 * 重新计算音频预消耗积分，并按需提示用户。
 * @param {{ showTip?: boolean }} [options]
 * @returns {Promise<number|null>}
 */
const refreshUploadedAudioPoints = async ({showTip = false} = {}) => {
  if (!uploadedAudioTrimDuration.value) return null;
  if (audioTrimPending.value) return null;
  try {
    const latestPoints = await estimateUploadedAudioPoints();
    const previousPoints = uploadedAudioEstimatedPoints.value;
    uploadedAudioEstimatedPoints.value = latestPoints;
    if (showTip && isNotEmpty(previousPoints) && previousPoints !== latestPoints) {
      ElMessage.warning(t('creation.musicTrimmer.pointsRecalculatedByModel', {points: latestPoints}));
    }
    return latestPoints;
  } catch (error) {
    console.error('预消耗积分预估失败:', error);
    return null;
  }
};

/**
 * 切换生成模型，并在已有音频附件时重新计算预消耗积分。
 * 如果当前没有可用音频，或者音频还在上传中，就只切换模型不重新预估。
 * @param {string} value 模型标识
 * @returns {void}
 */
const handleSelectModel = (value) => {
  const previousModel = modelStore.selectedModel;
  modelStore.setSelectedModel(value);
  modelPopoverVisible.value = false;

  if (value === previousModel) return;
  if (!isNotEmpty(uploadedAudioEstimatedPoints.value) || !uploadedAudioTrimDuration.value || audioTrimPending.value) return;

  refreshUploadedAudioPoints({showTip: true});
};

const handleSelectResolution = (value) => {
  modelStore.setSelectedResolution(value);
  resolutionPopoverVisible.value = false;
};

/**
 * 处理文件输入变更，将图片与音频分流到对应附件列表。
 *
 * 图片直接上传，音频先进入裁剪器；如果选择超过限制，会提前给出提示。
 * @param {Event} event 文件选择事件
 * @returns {Promise<void>}
 */
const handleFileChange = async (event) => {
  const files = Array.from(event.target.files || []);
  event.target.value = '';
  if (!files.length) return;
  if (!ensureLoggedInForUpload()) return;

  const imageFiles = files.filter(isSupportedImageUpload);
  const audioFiles = files.filter(isSupportedAudioUpload);
  const nextImageFiles = imageFiles.slice(0, Math.max(0, 6 - uploadedImages.value.length));
  const hasImageOverflow = imageFiles.length > nextImageFiles.length;
  const nextAudioFile = audioFiles[0] || null;
  const hasAudioOverflow = audioFiles.length > 1 || (!!nextAudioFile && !!uploadedAudio.value);

  if (hasImageOverflow || hasAudioOverflow) {
    const tips = [];
    if (hasImageOverflow) tips.push(t('home.uploadLimit', {count: 6}));
    if (hasAudioOverflow) tips.push(t('home.audioLimit'));
    ElMessage.warning(tips.join('，'));
  }

  try {
    if (nextImageFiles.length) {
      await Promise.all(
        nextImageFiles.map(async (file) => {
          const compressedFile = await compressImageBeforeUpload(file);
          const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
          const tempFileId = `${TEMP_IMAGE_FILE_ID_PREFIX}${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
          const placeholder = {
            id,
            previewUrl: URL.createObjectURL(compressedFile),
            fileName: compressedFile.name,
            fileId: tempFileId,
            fileUrl: '',
            intention: 'CHARACTER',
            uploading: true,
          };
          uploadedImages.value = [...uploadedImages.value, placeholder].slice(0, 6);

          try {
            const formData = new FormData();
            formData.append('file', compressedFile, compressedFile.name);
            const result = await uploadFile(formData);
            const target = uploadedImages.value.find((img) => img.id === id);
            if (!target) return;
            target.fileId = result.fileId;
            target.fileUrl = result.fileUrl || '';
            target.uploading = false;
          } catch (error) {
            const idx = uploadedImages.value.findIndex((img) => img.id === id);
            if (idx >= 0) {
              const [removed] = uploadedImages.value.splice(idx, 1);
              revokeImagePreview(removed);
            }
            ElMessage.error(error?.message || t('home.imageUploadFail'));
          }
        }),
      );
    }

    if (nextAudioFile) {
      const opened = await openWithFile(nextAudioFile);
      if (!opened) return;
      uploadedAudioTrimDuration.value = audioDuration.value;
      uploadedAudioEstimatedPoints.value = null;
      showTrimmer.value = true;
    }
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.message || t('home.imageUploadFail'));
  }
};

/**
 * 处理音频裁剪确认结果。
 *
 * 流程说明：
 * 1. 读取当前待裁剪的原始音频文件，准备临时占位态；
 * 2. 同步更新裁剪后的时长，保证界面展示和积分预估使用最新值；
 * 3. 调用上传接口提交裁剪后的音频；
 * 4. 上传成功后回填服务端返回的真实文件信息，并关闭裁剪弹窗；
 * 5. 上传失败时清理临时占位态，避免页面残留无效数据。
 *
 * @param {{ blob: Blob, timeRange: string, durationSeconds: number }} payload 裁剪组件返回的数据
 * @returns {Promise<void>}
 */
const handleTrimmerConfirm = async (payload) => {
  const sourceFile = rawFile.value;
  const tempFileId = `${TEMP_AUDIO_FILE_ID_PREFIX}${Date.now()}`;
  const tempFileName = sourceFile?.name ? sourceFile.name : 'audio_trimmed.mp3';
  uploadedAudio.value = {id: tempFileId, previewUrl: '', fileName: tempFileName, fileId: tempFileId};
  uploadedAudioTrimDuration.value = payload?.durationSeconds || audioDuration.value || 0;
  showTrimmer.value = false;
  try {
    const result = await uploadTrimmedBlob(payload, {silent: true, showSuccess: false, sourceFile});
    uploadedAudio.value = {
      id: result.fileId,
      previewUrl: result.fileUrl || '',
      fileName: result.fileName,
      fileId: result.fileId,
      fileUrl: result.fileUrl || '',
    };
    await refreshUploadedAudioPoints();
  } catch (error) {
    clearUploadedAudio();
    throw error;
  }
};

/**
 * 若 URL 带 `?login=1`，未登录用户自动打开登录界面并把 query 清掉，
 * 用于"未登录从订阅页跳回首页"的统一登录入口。
 */
const consumeLoginQuery = () => {
  if (route.query.login !== '1') return;
  if (!userStore.isLoggedIn) loginVisible.value = true;
  const {login: _login, ...rest} = route.query;
  router.replace({query: rest});
};

/**
 * 组件挂载后初始化作品列表并绑定滚动监听。
 */
onMounted(() => {
  saveUserTracking({target: 'HOME_PAGE'}).catch((error) => {
    console.error('移动端首页埋点上报失败:', error);
  });
  loadWorksList(true);
  window.addEventListener('scroll', handlePageScroll, {passive: true});
  handlePageScroll();
  if (userStore.isLoggedIn) userStore.fetchUserPlan();
  consumeLoginQuery();

  // 预加载三段背景动图，避免轮换淡入时因未加载完成而闪白
  bgList.forEach((src) => {
    const img = new Image();
    img.src = src;
  });
  scheduleNextBg();
});

watch(() => route.query.login, () => consumeLoginQuery());

/**
 * 组件卸载前解绑滚动监听。
 */
onBeforeUnmount(() => {
  window.removeEventListener('scroll', handlePageScroll);
  window.clearTimeout(bgRotateTimer);
});

// 首页背景动图列表，与 bgDurations 一一对应。
const bgList = [bg1Gif, bg2Gif, bg3Gif];
// 每段 GIF 单次循环时长减去 700ms 过渡时间。
// GIF 是循环的——播完立刻跳回第0帧。若 rotateBg 在 GIF 循环后才触发，
// 旧图层已经从头开始播了，交叉淡化时会露出新循环的首帧，造成抽搐。
// 所以必须提前 700ms 切：让 700ms 过渡恰好覆盖 GIF 循环点，
// 循环发生时旧图层已完全透明（opacity 0），用户看不到闪回。
// GIF1: 3625-700=2925 | GIF2: 4417-700=3717 | GIF3: 3750-700=3050
const bgDurations = [2925, 3717, 3050];
// 双图层：A 与 B 交替作为当前可见层，交叉淡入淡出避免硬切。
const activeLayer = ref(0);
// 当前正在展示的动图下标。
const currentBgIndex = ref(0);
// 两层各自绑定的动图地址。两层都先填 bg1：首次切到 bg2 即为真实 src 变更，
// 才能让隐藏层从第 0 帧重新播放（GIF 只播一次，src 不变会停在定格帧）。
const layerSources = ref([bgList[0], bgList[0]]);
// 背景轮换定时器句柄，卸载时清除。
let bgRotateTimer = null;
// 底部文本输入框内容。
const promptText = ref('');
// 登录由 MobileLoginSheet 内部接管 useLogin；这里只用一个 visible 开关控制显隐。
const loginVisible = ref(false);
const {
  worksList,
  worksLoading,
  worksHasMore,
  loadWorksList,
  handleWorksScroll,
} = useHomeWorksWall({
  scrollBottomOffset: 120,
  onError: (error) => {
    console.error('移动端创作广场加载失败:', error);
    ElMessage.error(error?.message || t('home.worksLoadingFail'));
  },
});
const {
  placeholderIndex,
  rotatingPlaceholder,
  isComposing,
  handleCompositionStart,
  handleCompositionEnd,
} = useRotatingPlaceholder(() => t('home.placeholders'));

/**
 * 统一监听浏览器窗口滚动，处理输入框收缩与创作广场触底加载。
 */
/**
 * 统一监听页面滚动，控制输入框收缩并触发创作广场懒加载。
 */
const handlePageScroll = () => {
  const scrollEl = document.scrollingElement || document.documentElement;
  const scrollTop = scrollEl?.scrollTop || 0;
  const firstScreenHeight = window.innerHeight || 0;
  isPromptCompact.value = scrollTop >= firstScreenHeight;
  handleWorksScroll({target: scrollEl});
};

/**
 * 退出当前账号并关闭账户弹窗。
 */
const handleLogoutClick = async () => {
  accountPopoverVisible.value = false;
  await userStore.logoutUser();
};

/** 客服微信二维码弹层显示时上报埋点（与 PC 端一致语义）。 */
const handleViewWechatQr = () => {
  saveUserTracking({target: 'HOME_VIEW_QR_CODE'}).catch((error) => {
    console.error('查看二维码埋点上报失败:', error);
  });
};

/**
 * 打开邀请弹窗。
 */
const handleOpenInviteDialog = () => {
  inviteDialogVisible.value = true;
  saveUserTracking({target: 'HOME_VIEW_INVITE_RULES'}).catch((error) => {
    console.error('查看邀请规则埋点上报失败:', error);
  });
};

/**
 * 打开积分使用详情弹窗。
 */
const handleOpenPointsDetailDialog = () => {
  pointsDetailDialogVisible.value = true;
  resetAndFetchPointsLog();
};

const { // 积分交易日志
  pointsLogList,
  pointsLogLoading,
  hasMorePointsLog,
  resetAndFetchPointsLog,
  handlePointsLogScroll,
} = usePointsLog();

/**
 * 积分详情弹窗打开后的回调。
 */
const handlePointsDetailOpened = () => {
  if (!pointsLogList.value.length) {
    resetAndFetchPointsLog();
  }
};

/** 积分详情弹窗内点击"升级"：关闭弹窗并跳转到订阅页。 */
const handleUpgradeFromPointsDetail = () => {
  saveUserTracking({target: 'HOME_CLICK_UPGRADE'}).catch((error) => {
    console.error('点击升级埋点上报失败:', error);
  });
  pointsDetailDialogVisible.value = false;
  const lang = router.currentRoute.value?.params?.lang || 'zh';
  router.push({name: 'subscribe', params: {lang}});
};

/**
 * 向创作页提交前做统一校验，并携带 prompt / 模型 / 附件信息跳转。
 * 校验顺序与 PC 端保持一致：
 * 1. 文本是否为空；
 * 2. 音频是否仍在隐式上传中；
 * 3. 预估积分是否超过当前余额。
 * @returns {Promise<void>}
 */
const handleStartCreation = async () => {
  // 未登录直接拦截，避免跳转到创作页后才提示
  if (!ensureLoggedInForUpload()) {
    return;
  }

  if (!promptText.value.trim()) {
    ElMessage.warning(t('home.createPromptEmpty'));
    return;
  }

  if (audioTrimPending.value) {
    ElMessage.warning(t('home.audioUploading'));
    return;
  }

  if (imageUploadPending.value) {
    ElMessage.warning(t('home.imageUploading'));
    return;
  }

  if (uploadedAudio.value?.fileId && !isNotEmpty(uploadedAudioEstimatedPoints.value)) {
    await refreshUploadedAudioPoints();
  }

  if (isAudioPointsInsufficient.value) {
    ElMessage.warning(t('account.points.insufficientForSend'));
    return;
  }

  const query = {prompt: promptText.value.trim(), model: modelStore.selectedModel};
  if (uploadedAudio?.value?.fileId) {
    query.audioFileId = uploadedAudio.value.fileId;
    query.fileName = uploadedAudio.value.fileName;
    query.audioUrl = uploadedAudio.value.fileUrl || '';
  }
  if (uploadedImages.value.length > 0) {
    query.images = JSON.stringify(uploadedImages.value.map((img) => ({
      fileId: img.fileId,
      fileName: img.fileName,
      intention: img.intention || 'CHARACTER',
      fileUrl: img.fileUrl || '',
    })));
  }

  // 校验全通过、即将跳创作页 = 用户确实在发起一次新对话，打 CREATE_NEW_PROJECT 埋点
  saveUserTracking({target: 'CREATE_NEW_PROJECT'}).catch((error) => {
    console.error('移动端首页发起新对话埋点上报失败:', error);
  });

  const lang = router.currentRoute.value?.params?.lang || 'zh';
  router.push({name: 'creation', params: {lang}, query});
};

/**
 * 轮换背景动图：隐藏层换成下一段 GIF（src 变更触发其从第 0 帧重播），
 * 再翻转可见层做交叉淡入淡出，最后按新当前段的时长排定下一次切换。
 */
const rotateBg = () => {
  const nextIndex = (currentBgIndex.value + 1) % bgList.length;
  const hiddenLayer = activeLayer.value === 0 ? 1 : 0;
  layerSources.value[hiddenLayer] = bgList[nextIndex];
  activeLayer.value = hiddenLayer;
  currentBgIndex.value = nextIndex;
  scheduleNextBg();
};

/**
 * 按当前 GIF 的时长排定下一次切换。
 * 用 setTimeout 而非 setInterval：每段时长不同，且 GIF 不暴露播放进度，只能按已知时长定时。
 */
const scheduleNextBg = () => {
  bgRotateTimer = window.setTimeout(rotateBg, bgDurations[currentBgIndex.value]);
};

/**
 * 点击瀑布流卡片：调 shareLink 拿到 shareId，跳到分享页。
 * 与 PC 端 home/index.vue 的 handleOpenShare 行为一致。
 * 分享页路由 name: 'share'，需要 params.lang + query.shareId。
 */
const handleOpenShare = async (item) => {
  try {
    const shareId = await shareLink({projectId: item.projectId});
    const lang = router.currentRoute.value?.params?.lang || 'zh';
    router.push({name: 'share', params: {lang}, query: {shareId}});
  } catch (error) {
    console.error(error);
    ElMessage.error(error?.message || t('home.shareFailed'));
  }
};
</script>

<style scoped lang="scss">
.placeholder-slide-enter-active,
.placeholder-slide-leave-active {
  transition: transform 0.35s ease, opacity 0.35s ease;
}

.placeholder-slide-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.placeholder-slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.home-mobile-prompt-input {
  :deep(.el-textarea) {
    height: 100%;
  }

  :deep(.el-textarea__inner) {
    height: 100%;
    border: none;
    box-shadow: none;
    background: transparent;
    color: #ffffff;
    padding: 0;
    font-size: 14px;
    line-height: 18px;

    &::placeholder {
      color: rgba(255, 255, 255, 0.65);
    }
  }

  &.home-mobile-prompt-input--with-attachments {
    :deep(.el-textarea__inner) {
      min-height: 56px;
    }
  }
}

.home-mobile-mini-input {
  :deep(.el-input__wrapper) {
    box-shadow: none;
    border: none;
    background: transparent;
    padding: 0;
  }

  :deep(.el-input__inner) {
    height: 24px;
    line-height: 24px;
    color: #ffffff;
    background: transparent;
    border: none;
    box-shadow: none;
    padding: 0;
  }
}

.hero-title-gradient {
  background: linear-gradient(206.99070598411055deg, #BEFA00 0%, #CFFFCC 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.no-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.home-mobile-login-checkbox {
  :deep(.el-checkbox__inner) {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    border-color: rgba(255, 255, 255, 0.45);
    background-color: transparent;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #C2FF00;
    border-color: #C2FF00;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
    border-color: #000;
  }
}
</style>

<style lang="scss">
// 移动端分辨率下拉：与 PC 的 resolution-select-dropdown 基本一致，仅尺寸放大适配触控
.resolution-select-dropdown-m {
  width: 63px !important;
  min-width: 63px !important;
  height: 45px !important;
  box-sizing: border-box !important;
  padding: 3px !important;
  background: rgba(27, 31, 12, 0.1) !important;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: 5px !important;
  border: 1px solid #7D7D7D !important;
  outline: none !important;

  .el-popper__arrow {
    display: none !important;
  }

  .resolution-select-body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    height: 100%;
  }

  .el-select-dropdown__item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0;
    padding: 0;
    border-radius: 5px;
    color: #fff;
    background: transparent !important;
    cursor: pointer;

    &.is-selected {
      background: linear-gradient(299deg, rgba(190, 250, 0, 0.5) 0%, rgba(130, 255, 121, 0.5) 100%) !important;
    }
  }

  .scene-model-item-main {
    font-size: 12px;
    font-weight: normal;
    color: #fff;
    text-align: center;
  }
}

.home-mobile-wechat-popover {
  width: 215px!important;

  &.el-popper {
    border: none;
    background: #211E1E;
    padding: 5px;
    border-radius: 10px;
  }
}

.home-mobile-account-popover {
  &.el-popper {
    border: none;
    border-radius: 5px;
    padding: 0;
    background: rgba(16, 17, 20, 0.96);
    box-shadow: 0 18px 48px rgba(0, 0, 0, 0.45);
  }
}

// 模型选择底部抽屉：占满宽度、从底部上滑，顶部圆角 + lime 光晕
.el-drawer.home-mobile-model-drawer {
  background: #0A0A0A;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
  box-shadow: 0 -8px 30px rgba(194, 255, 0, 0.5);

  .el-drawer__body {
    padding: 0;
  }
}

.home-mobile-points-dialog {
  &.el-dialog {
    border-radius: 16px;
    padding: 0;
    background: transparent;
    box-shadow: none;
  }

  .el-dialog__header,
  .el-dialog__body {
    padding: 0;
    margin: 0;
  }

  .el-dialog__body {
    overflow: visible;
  }
}

.home-mobile-edit-profile-dialog {
  &.el-dialog {
    border-radius: 10px;
  }

  .el-dialog__header,
  .el-dialog__body {
    padding: 0;
    margin: 0;
  }
}

.account-confirm {
  width: calc(100vw - 32px) !important;
  max-width: 343px !important;
  min-height: 192px !important;
  background: #FFFFFF !important;
  border-radius: 16px !important;
  padding: 20px 16px !important;

  .el-message-box__header {
    padding: 0 !important;
  }

  .el-message-box__title {
    font-size: 18px;
    font-weight: 700;
    color: #000000;
    line-height: 26px;
  }

  .el-message-box__content {
    padding: 10px 0 0 !important;
  }

  .el-message-box__message {
    font-size: 14px;
    font-weight: 400;
    color: #666666;
    line-height: 22px;

    p {
      margin: 0;
    }
  }

  .el-message-box__btns {
    display: flex;
    gap: 12px;
    padding: 24px 0 0 !important;

    .el-button {
      flex: 1;
      width: auto !important;
      height: 40px !important;
      border-radius: 8px !important;
      font-size: 14px !important;
      font-weight: 500 !important;
      margin: 0 !important;
      color: #000000;

      &:first-child {
        margin-right: 0 !important;
        border: 1px solid #000000;

        &:hover {
          background-color: transparent;
        }
      }
    }

    .el-button--primary {
      border: none !important;
      background: #000000 !important;
      color: #C2FF00 !important;
    }
  }

  // 危险操作变体：确认按钮红色
  &.account-confirm--danger .el-message-box__btns .el-button--primary {
    background: #FF4D4F !important;
    color: #FFFFFF !important;
  }
}
</style>
