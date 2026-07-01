import pxtorem from 'postcss-pxtorem';
import tailwindcss from 'tailwindcss';
import autoprefixer from 'autoprefixer';

/**
 * 全局 px -> rem（包含 Tailwind 生成的 utility）
 *
 * 为什么要全局转：
 * - 你在模板里写的 `w-[78px]` 最终由 Tailwind 编译进全局 CSS，并不是“某个 mobile.vue 的 CSS”
 * - 如果只对移动端文件做转换，Tailwind 产物仍是 px，看起来就是“死值”
 *
 * 为什么不会影响 PC：
 * - PC 端 html 根字号保持 16px 时：1rem == 16px，表现与 px 基本等价
 * - 只有移动端（html.is-mobile）我们才会让根字号随 vw 缩放，从而让 rem 随屏幕变化
 */
const pxToRem = pxtorem({
  // 现有页面的 px 数值以 375 设计稿为主，先保持 16px => 1rem 的稳定基准，避免整体尺寸“翻倍”。
  rootValue: 16,
  unitPrecision: 6,
  propList: ['*'],
  replace: true,
  mediaQuery: false,
  minPixelValue: 0,
  // 不处理三方包（避免意外影响 Element Plus 等库的内部细节）
  exclude: /node_modules/i,
});

export default {
  plugins: [tailwindcss(), autoprefixer(), pxToRem],
};
