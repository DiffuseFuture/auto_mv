<template>
  <div class="h-full flex flex-col">
    <main class="flex-1 min-h-0">
      <RouterView />
    </main>
    <MobileTabBar v-if="showTabBar" />
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';

const route = useRoute();
const showTabBar = computed(() => route.meta?.showMobileTabBar !== false);
</script>
