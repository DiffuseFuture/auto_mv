<template>
  <el-config-provider :locale="elementLocale">
    <div class="h-full">
      <router-view></router-view>
    </div>
  </el-config-provider>
</template>

<script setup>
import {computed} from 'vue';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import en from 'element-plus/es/locale/lang/en';
import {useI18nText} from '@/i18n';

const {locale} = useI18nText();

const elementLocale = computed(() => (locale.value === 'zh-CN' ? zhCn : en));
</script>