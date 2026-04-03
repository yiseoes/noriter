import sharp from 'sharp';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DIR = path.join(__dirname, '..', 'public', 'templates', 'vampire-survival');

const files = ['girl1','boy1','girl2','girl3','boy2','boy3','girl4','boy4'];
const TARGET_H = 120;

for (const name of files) {
  const input = path.join(DIR, `${name}.jpg`);
  const output = path.join(DIR, `${name}.png`);

  try {
    // 1. 흰배경을 투명으로
    const { data, info } = await sharp(input)
      .ensureAlpha()
      .raw()
      .toBuffer({ resolveWithObject: true });

    // 흰색 픽셀 → 투명 (flood fill 시뮬레이션: 가장자리에서 시작)
    const w = info.width, h = info.height, ch = info.channels;
    const isWhite = (i) => data[i] >= 245 && data[i+1] >= 245 && data[i+2] >= 245;
    const visited = new Uint8Array(w * h);
    const queue = [];

    // 네 변 가장자리
    for (let x = 0; x < w; x++) {
      if (isWhite(x * ch)) queue.push([x, 0]);
      if (isWhite(((h-1)*w + x) * ch)) queue.push([x, h-1]);
    }
    for (let y = 0; y < h; y++) {
      if (isWhite((y*w) * ch)) queue.push([0, y]);
      if (isWhite((y*w + w-1) * ch)) queue.push([w-1, y]);
    }

    while (queue.length) {
      const [x, y] = queue.pop();
      if (x < 0 || x >= w || y < 0 || y >= h) continue;
      const pi = y * w + x;
      if (visited[pi]) continue;
      const idx = pi * ch;
      if (!isWhite(idx)) continue;
      visited[pi] = 1;
      data[idx + 3] = 0; // 투명
      queue.push([x-1,y],[x+1,y],[x,y-1],[x,y+1]);
    }

    // 2. 투명 처리된 버퍼로 이미지 생성 + trim + 리사이즈
    await sharp(data, { raw: { width: w, height: h, channels: ch } })
      .trim({ threshold: 10 })  // 투명 영역 자동 크롭
      .resize({ height: TARGET_H, fit: 'inside' })  // 높이 통일
      .png()
      .toFile(output);

    console.log(`✅ ${name}.jpg → ${name}.png`);
  } catch (e) {
    console.error(`❌ ${name}: ${e.message}`);
  }
}

console.log('완료!');
