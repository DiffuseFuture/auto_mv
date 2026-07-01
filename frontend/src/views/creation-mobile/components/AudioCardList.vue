<template>
  <div class="mt-3 mb-4 flex flex-wrap gap-3">
    <!-- 音频卡片列表：每张卡片代表一首 AI 生成的音乐 -->
    <button
        v-for="(item, index) in audioList"
        :key="item.audioFileId || index"
        type="button"
        class="relative h-[150px] w-[130px] max-w-full overflow-hidden rounded-[10px] border border-[rgba(194,255,0,0.5)] bg-[rgba(194,255,0,0.2)] pt-[5px] pb-[13px]"
        @click="emit('open', {item, index, audioList})"
    >
      <!-- 封面区：显示音乐封面，没有封面时展示默认音频图标 -->
      <div class="relative mx-auto h-[100px] w-[100px] overflow-hidden rounded-[4px] bg-black/30">
        <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title || `audio-${index + 1}`" class="h-full w-full object-cover"/>
        <div v-else class="flex h-full w-full items-center justify-center bg-white/10">
          <svg-icon name="gy-audio" size="24" color="#FFFFFF"></svg-icon>
        </div>
        <!-- 中间播放态图标仅用于提示当前卡片是否正在播放 -->
        <div class="absolute inset-0 flex items-center justify-center">
          <svg-icon :name="isItemPlaying(item) ? 'gy-pause' : 'gy-play2'" size="15" color="#FFFFFF"></svg-icon>
        </div>
      </div>

      <!-- 标题与风格描述 -->
      <div class="mt-[5px] text-center text-[12px] leading-none text-[#C2FF00] line-clamp-1">
        {{ `${index + 1}.${item.title || '未命名'}` }}
      </div>
      <div class="mt-[2px] text-center text-[10px] leading-[12px] text-white line-clamp-1">
        {{ item.style || '暂无描述' }}
      </div>
    </button>
  </div>
</template>

<script setup>
/**
 * 移动端音频卡片列表：
 * - 展示 AI 生成的音乐封面、标题和风格描述
 * - 通过共享播放器状态高亮当前正在播放的卡片
 * - 点击卡片后把选中的音频信息交给父组件处理
 */
const props = defineProps({
  /** 当前消息下的音频列表。 */
  audioList: {type: Array, default: () => []},
  /** 共享播放器状态，用于同步当前播放中的音频。 */
  playerState: {type: Object, required: true},
});

/** 通知父组件打开对应音频。 */
const emit = defineEmits(['open']);

/**
 * 判断当前条目是否为全局正在播放的音频。
 * @param {any} item
 * @returns {boolean}
 */
const isItemPlaying = (item) => {
  return props.playerState?.isPlaying && props.playerState?.currentAudio?.audioFileId === item?.audioFileId;
};
</script>
