<template>
  <div class="flex gap-3 flex-wrap mt-4 mb-4">
    <div
      v-for="(item, index) in audioList"
      :key="item.audioFileId || index"
      class="audio-card w-[290px] h-[100px] flex items-center gap-2.5 rounded-[10px] px-3 py-[10px] cursor-pointer transition-colors"
      :class="{ 'audio-card--active': isItemPlaying(item) }"
      @click="$emit('select', { item, index })"
    >
      <!-- 封面图 + 播放按钮 -->
      <div class="relative w-[80px] h-[80px] rounded-[10px] overflow-hidden shrink-0 group">
        <img
          v-if="item.coverUrl"
          :src="item.coverUrl"
          :alt="`歌曲 ${index + 1}`"
          class="w-full h-full object-cover"
        />
        <div v-else class="w-full h-full bg-[#C2FF00]/15 flex-center">
          <svg-icon name="gy-audio" size="24" color="#C2FF00"></svg-icon>
        </div>
        <!-- 播放/暂停覆盖层 -->
        <div
          class="absolute inset-0 flex-center bg-black/40 transition-opacity"
          :class="isItemPlaying(item) ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'"
          @click.stop="$emit('toggle-play', { item, index })"
        >
          <svg-icon v-if="isItemPlaying(item)" name="gy-pause" size="20" color="#fff"></svg-icon>
          <svg-icon v-else name="gy-play2" size="20" color="#fff"></svg-icon>
        </div>
      </div>

      <!-- 歌曲信息 -->
      <div class="flex-1 min-w-0 overflow-hidden">
        <div :title="item.title" class="text-[#C2FF00] text-[18px] leading-[26px] truncate mb-1">
          {{ `${index + 1}.${item.title || '未命名'}` }}
        </div>
        <div v-if="item.style" :title="item.style" class="text-white text-[16px] leading-6 truncate">
          {{ item.style }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>

const props = defineProps({
  audioList: {
    type: Array,
    default: () => []
  },
  /** 当前正在播放的 audioFileId */
  currentPlayingId: {
    type: String,
    default: ''
  },
  /** 是否正在播放 */
  playing: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select', 'toggle-play'])

/**
 * 当前条目是否为全局正在播放的音频。
 * @param {any} item
 * @returns {boolean}
 */
const isItemPlaying = (item) => {
  return props.playing && props.currentPlayingId === item.audioFileId;
};
</script>

<style scoped>
.audio-card {
  background: rgba(194, 255, 0, 0.1);
  border: none;
}

.audio-card--active {
  background: rgba(194, 255, 0, 0.2);
  border: 1px solid rgba(194, 255, 0, 0.5);
}
</style>

