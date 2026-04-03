const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const nextCtx = document.getElementById('nextCanvas').getContext('2d');
const holdCtx = document.getElementById('holdCanvas').getContext('2d');

const COLS=10, ROWS=20, BLOCK=30;
const COLORS = ['','#ff6b6b','#74c0fc','#ffd43b','#51cf66','#ae3ec9','#ff922b','#e64980'];

const PIECES = [
  [], // 0 empty
  [[1,1,1,1]], // I
  [[1,1],[1,1]], // O
  [[0,1,0],[1,1,1]], // T
  [[1,0,0],[1,1,1]], // J
  [[0,0,1],[1,1,1]], // L
  [[0,1,1],[1,1,0]], // S
  [[1,1,0],[0,1,1]], // Z
];

let board, current, nextPiece, holdPiece, canHold;
let score, level, lines, dropTimer, dropInterval, gameRunning, animating;
let lineClearAnim = [];

function createBoard() {
  return Array.from({length:ROWS}, ()=>new Array(COLS).fill(0));
}

function randomPiece() {
  const id = 1 + Math.floor(Math.random()*7);
  return { shape:PIECES[id].map(r=>[...r]), id, x:3, y:0, rotation:0 };
}

function rotate(shape) {
  const rows=shape.length, cols=shape[0].length;
  return Array.from({length:cols}, (_,c) => Array.from({length:rows}, (_,r) => shape[rows-1-r][c]));
}

function valid(shape, px, py) {
  return shape.every((row,r) => row.every((v,c) => {
    if (!v) return true;
    const nx=px+c, ny=py+r;
    return nx>=0 && nx<COLS && ny<ROWS && (ny<0 || board[ny][nx]===0);
  }));
}

function merge() {
  current.shape.forEach((row,r) => row.forEach((v,c) => {
    if (v && current.y+r>=0) board[current.y+r][current.x+c] = current.id;
  }));
}

function clearLines() {
  const full = [];
  board.forEach((row,i) => { if (row.every(v=>v)) full.push(i); });
  if (!full.length) return;
  lineClearAnim = full.map(i=>({row:i, flash:6}));
  animating = true;
  const pts = [0,100,300,500,800][full.length] * level;
  score += pts;
  lines += full.length;
  level = Math.floor(lines/10) + 1;
  dropInterval = Math.max(50, 500 - (level-1)*40);
  setTimeout(() => {
    full.forEach(i => { board.splice(i,1); board.unshift(new Array(COLS).fill(0)); });
    lineClearAnim = [];
    animating = false;
  }, 300);
}

function drop() {
  if (animating) return;
  if (valid(current.shape, current.x, current.y+1)) { current.y++; }
  else {
    merge(); clearLines(); spawn();
  }
}

function spawn() {
  current = nextPiece || randomPiece();
  nextPiece = randomPiece();
  canHold = true;
  if (!valid(current.shape, current.x, current.y)) {
    gameRunning = false;
    document.getElementById('overlayTitle').textContent = '💀 게임 오버';
    document.getElementById('overlayText').textContent = `스코어: ${score.toLocaleString()} · 레벨: ${level}`;
    document.getElementById('overlay').classList.remove('hidden');
  }
}

function hardDrop() {
  while(valid(current.shape, current.x, current.y+1)) { current.y++; score+=2; }
  merge(); clearLines(); spawn();
}

function hold() {
  if (!canHold) return;
  canHold = false;
  const tmp = holdPiece;
  holdPiece = { shape:PIECES[current.id].map(r=>[...r]), id:current.id };
  if (tmp) { current = { ...tmp, x:3, y:0, rotation:0 }; }
  else spawn();
}

function ghostY() {
  let gy = current.y;
  while(valid(current.shape, current.x, gy+1)) gy++;
  return gy;
}

// ===== 렌더링 =====
function drawBlock(c, x, y, size, alpha=1) {
  c.globalAlpha = alpha;
  c.fillStyle = COLORS[0]; // will be overridden
  c.fillRect(x*size, y*size, size, size);
  c.globalAlpha = 1;
}

function render() {
  ctx.fillStyle = '#111';
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // 보드
  board.forEach((row,r) => row.forEach((v,c) => {
    if (!v) return;
    const isFlashing = lineClearAnim.some(a=>a.row===r);
    ctx.fillStyle = isFlashing ? '#fff' : COLORS[v];
    ctx.globalAlpha = isFlashing ? 0.8 : 1;
    ctx.fillRect(c*BLOCK+1, r*BLOCK+1, BLOCK-2, BLOCK-2);
    ctx.globalAlpha = 1;
  }));

  // 고스트
  if (current && !animating) {
    const gy = ghostY();
    current.shape.forEach((row,r) => row.forEach((v,c) => {
      if (!v) return;
      ctx.strokeStyle = COLORS[current.id];
      ctx.globalAlpha = 0.3;
      ctx.strokeRect((current.x+c)*BLOCK+2, (gy+r)*BLOCK+2, BLOCK-4, BLOCK-4);
      ctx.globalAlpha = 1;
    }));
  }

  // 현재 피스
  if (current && !animating) {
    current.shape.forEach((row,r) => row.forEach((v,c) => {
      if (!v || current.y+r<0) return;
      ctx.fillStyle = COLORS[current.id];
      ctx.fillRect((current.x+c)*BLOCK+1, (current.y+r)*BLOCK+1, BLOCK-2, BLOCK-2);
    }));
  }

  // 그리드
  ctx.strokeStyle = 'rgba(255,255,255,0.04)';
  for(let c=0;c<=COLS;c++){ctx.beginPath();ctx.moveTo(c*BLOCK,0);ctx.lineTo(c*BLOCK,ROWS*BLOCK);ctx.stroke();}
  for(let r=0;r<=ROWS;r++){ctx.beginPath();ctx.moveTo(0,r*BLOCK);ctx.lineTo(COLS*BLOCK,r*BLOCK);ctx.stroke();}

  // Next 미리보기
  nextCtx.fillStyle = 'transparent'; nextCtx.clearRect(0,0,100,100);
  if (nextPiece) {
    const s=20, ox=(100-nextPiece.shape[0].length*s)/2, oy=(100-nextPiece.shape.length*s)/2;
    nextPiece.shape.forEach((row,r) => row.forEach((v,c) => {
      if (!v) return;
      nextCtx.fillStyle = COLORS[nextPiece.id];
      nextCtx.fillRect(ox+c*s+1,oy+r*s+1,s-2,s-2);
    }));
  }

  // Hold 미리보기
  holdCtx.clearRect(0,0,100,100);
  if (holdPiece) {
    const s=20, ox=(100-holdPiece.shape[0].length*s)/2, oy=(100-holdPiece.shape.length*s)/2;
    holdPiece.shape.forEach((row,r) => row.forEach((v,c) => {
      if (!v) return;
      holdCtx.fillStyle = COLORS[holdPiece.id];
      holdCtx.globalAlpha = canHold ? 1 : 0.3;
      holdCtx.fillRect(ox+c*s+1,oy+r*s+1,s-2,s-2);
    }));
    holdCtx.globalAlpha = 1;
  }

  // UI
  document.getElementById('score').textContent = score.toLocaleString();
  document.getElementById('level').textContent = level;
  document.getElementById('lines').textContent = lines;
}

// ===== 입력 =====
document.addEventListener('keydown', e => {
  if (!gameRunning || animating) return;
  switch(e.key) {
    case 'ArrowLeft': if(valid(current.shape,current.x-1,current.y)) current.x--; break;
    case 'ArrowRight': if(valid(current.shape,current.x+1,current.y)) current.x++; break;
    case 'ArrowDown': drop(); score++; break;
    case 'ArrowUp': case 'x': {
      const rotated = rotate(current.shape);
      if(valid(rotated,current.x,current.y)) current.shape=rotated;
      else if(valid(rotated,current.x-1,current.y)){current.shape=rotated;current.x--;}
      else if(valid(rotated,current.x+1,current.y)){current.shape=rotated;current.x++;}
      break;
    }
    case ' ': hardDrop(); break;
    case 'c': case 'C': hold(); break;
  }
  e.preventDefault();
});

// ===== 게임 루프 =====
let lastTime = 0;
function gameLoop(now) {
  if (!gameRunning) return;
  const dt = now - lastTime;
  dropTimer += dt;
  lastTime = now;
  if (dropTimer > dropInterval) { drop(); dropTimer = 0; }
  render();
  requestAnimationFrame(gameLoop);
}

function startGame() {
  document.getElementById('startScreen').classList.add('hidden');
  document.getElementById('overlay').classList.add('hidden');
  board = createBoard();
  score=0; level=1; lines=0; dropTimer=0; dropInterval=500;
  holdPiece=null; canHold=true; animating=false; lineClearAnim=[];
  gameRunning = true;
  spawn();
  lastTime = performance.now();
  requestAnimationFrame(gameLoop);
}
