import fs from 'node:fs';
import path from 'node:path';

const projectRoot = process.cwd();
const srcDir = path.resolve(projectRoot, 'node_modules/@ffmpeg/core/dist/esm');
const outDir = path.resolve(projectRoot, 'public/ffmpeg');

const files = ['ffmpeg-core.js', 'ffmpeg-core.wasm'];

if (!fs.existsSync(srcDir)) {
  console.error(`[copy-ffmpeg] 源目录不存在: ${srcDir}`);
  process.exit(1);
}

fs.mkdirSync(outDir, { recursive: true });

for (const f of files) {
  const from = path.join(srcDir, f);
  const to = path.join(outDir, f);
  if (!fs.existsSync(from)) {
    console.error(`[copy-ffmpeg] 源文件不存在: ${from}`);
    process.exit(1);
  }
  fs.copyFileSync(from, to);
  console.log(`[copy-ffmpeg] copied: ${f}`);
}

