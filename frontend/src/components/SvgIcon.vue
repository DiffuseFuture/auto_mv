<!-- 阿里巴巴矢量图标库 Symbol 使用组件 -->
<template>
  <svg
    :class="['svg-icon', customClass]"
    :style="{ width: `${size}px`, height: `${size}px`, fill: color }"
    :viewBox="viewBox"
    aria-hidden="true"
  >
    <use :xlink:href="`#${name}`" :fill="color" />
  </svg>
</template>

<script>
export default {
  name: 'SvgIcon',
  props: {
    // 图标名称 (与阿里巴巴图标库中的icon名称一致)
    name: {
      type: String,
      required: true
    },
    // 图标大小，单位为像素
    size: {
      type: [Number, String],
      default: 24
    },
    // 图标颜色
    color: {
      type: String,
      default: 'currentColor'
    },
    // 自定义 class
    customClass: {
      type: String,
      default: ''
    },
    // viewBox，默认为标准的 24x24
    viewBox: {
      type: String,
      default: '0 0 1024 1024'
    }
  }
}
</script>

<style scoped>
.svg-icon {
  display: inline-block;
  vertical-align: -0.125em;
  overflow: hidden;
}
</style>
