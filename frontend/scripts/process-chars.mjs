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

    // 흰색 픽셀 → 투명 (flood fill: 가장자리에서 시작)
    const w = info.width, h = info.height, ch = info.channels;
    const isWhite = (i) => data[i] >= 240 && data[i+1] >= 240 && data[i+2] >= 240;
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

    // 1.5. 작은 불투명 클러스터 제거 (노이즈 점)
    const clusterVisited = new Uint8Array(w * h);
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const pi = y * w + x;
        if (clusterVisited[pi] || data[pi * ch + 3] === 0) continue;
        // BFS로 연결된 불투명 픽셀 클러스터 찾기
        const cluster = [];
        const bfs = [[x, y]];
        while (bfs.length) {
          const [cx, cy] = bfs.pop();
          const cpi = cy * w + cx;
          if (cx < 0 || cx >= w || cy < 0 || cy >= h || clusterVisited[cpi]) continue;
          if (data[cpi * ch + 3] === 0) continue;
          clusterVisited[cpi] = 1;
          cluster.push(cpi);
          bfs.push([cx-1,cy],[cx+1,cy],[cx,cy-1],[cx,cy+1]);
        }
        // 50픽셀 미만 클러스터 → 노이즈로 제거
        if (cluster.length < 50) {
          for (const cpi of cluster) data[cpi * ch + 3] = 0;
        }
      }
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
