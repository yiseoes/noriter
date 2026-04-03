const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
canvas.width = window.innerWidth;
canvas.height = window.innerHeight;
window.addEventListener('resize', () => { canvas.width = innerWidth; canvas.height = innerHeight; });

let player, bullets, enemies, particles, powerUps, stars;
let score, lives, wave, waveTimer, enemiesRemaining, gameRunning;
let autoFireTimer = 0;

const POWER_TYPES = [
  { id:'triple', icon:'🔥', name:'트리플 샷', duration:8000, color:'#ff6b6b' },
  { id:'shield', icon:'🛡️', name:'쉴드', duration:6000, color:'#339af0' },
  { id:'speed', icon:'⚡', name:'스피드 업', duration:7000, color:'#ffd43b' },
  { id:'bomb', icon:'💣', name:'화면 폭탄', duration:0, color:'#ae3ec9' },
];

function initGame() {
  player = { x:canvas.width/2, y:canvas.height-80, width:30, height:30, speed:6, power:null, powerTimer:0 };
  bullets=[]; enemies=[]; particles=[]; powerUps=[];
  stars = Array.from({length:80}, ()=>({x:Math.random()*canvas.width, y:Math.random()*canvas.height, size:Math.random()*2+0.5, speed:Math.random()*1+0.5}));
  score=0; lives=3; wave=1; waveTimer=0; enemiesRemaining=0; gameRunning=true;
  spawnWave();
}

function spawnWave() {
  const count = 5 + wave * 3;
  enemiesRemaining = count;
  for(let i=0; i<count; i++) {
    const type = wave > 3 && Math.random()<0.2 ? 'boss' : wave > 1 && Math.random()<0.3 ? 'fast' : 'normal';
    setTimeout(()=>{
      if(!gameRunning) return;
      enemies.push(createEnemy(type));
    }, i * 400);
  }
}

function createEnemy(type) {
  const configs = {
    normal: { width:24, height:24, hp:1, speed:1.5+wave*0.2, score:100, color:'#e64980' },
    fast: { width:18, height:18, hp:1, speed:3+wave*0.3, score:150, color:'#ff922b' },
    boss: { width:40, height:40, hp:5+wave, speed:1, score:500, color:'#ae3ec9' },
  };
  const c = configs[type];
  return { x:Math.random()*(canvas.width-60)+30, y:-40-Math.random()*200, ...c, type, maxHp:c.hp, flash:0, shootTimer:Math.random()*2000 };
}

// ===== 입력 =====
const keys = {};
window.addEventListener('keydown', e => { keys[e.key]=true; if(e.key===' ') e.preventDefault(); });
window.addEventListener('keyup', e => keys[e.key]=false);

function shoot() {
  const bx=player.x, by=player.y;
  if (player.power==='triple') {
    bullets.push({x:bx,y:by-15,vx:0,vy:-10,size:3,player:true});
    bullets.push({x:bx-10,y:by-10,vx:-1.5,vy:-9,size:3,player:true});
    bullets.push({x:bx+10,y:by-10,vx:1.5,vy:-9,size:3,player:true});
  } else {
    bullets.push({x:bx,y:by-15,vx:0,vy:-10,size:3,player:true});
  }
}

function showPowerUp(name) {
  const el = document.getElementById('powerUpNotice');
  el.textContent = name;
  el.classList.remove('hidden');
  el.style.animation='none'; el.offsetHeight; el.style.animation='fadeOut 2s forwards';
  setTimeout(()=>el.classList.add('hidden'), 2000);
}

// ===== 업데이트 =====
function update(dt) {
  // 플레이어 이동
  if (keys['ArrowLeft']||keys['a']) player.x = Math.max(15, player.x-player.speed*(player.power==='speed'?1.5:1));
  if (keys['ArrowRight']||keys['d']) player.x = Math.min(canvas.width-15, player.x+player.speed*(player.power==='speed'?1.5:1));

  // 자동 발사
  autoFireTimer += dt*1000;
  if ((keys[' '] || true) && autoFireTimer > 180) { shoot(); autoFireTimer=0; }

  // 파워업 타이머
  if (player.power && player.powerTimer > 0) {
    player.powerTimer -= dt*1000;
    if (player.powerTimer <= 0) player.power = null;
  }

  // 총알 이동
  bullets.forEach(b => { b.x+=b.vx; b.y+=b.vy; });
  bullets = bullets.filter(b => b.y>-20 && b.y<canvas.height+20 && b.x>-20 && b.x<canvas.width+20);

  // 적 이동 + 발사
  enemies.forEach(e => {
    e.y += e.speed;
    if (e.flash > 0) e.flash -= dt;
    if (e.type==='boss') {
      e.shootTimer -= dt*1000;
      if (e.shootTimer <= 0) {
        bullets.push({x:e.x,y:e.y+e.height/2,vx:0,vy:5,size:4,player:false});
        e.shootTimer = 1500;
      }
    }
    // 화면 밖으로 나가면 제거
    if (e.y > canvas.height+50) { e.dead=true; enemiesRemaining--; }
  });

  // 충돌: 플레이어 총알 → 적
  bullets.filter(b=>b.player).forEach(b => {
    enemies.forEach(e => {
      if (e.dead) return;
      if (Math.abs(b.x-e.x)<(e.width/2+b.size) && Math.abs(b.y-e.y)<(e.height/2+b.size)) {
        e.hp--; e.flash=0.1; b.dead=true;
        spawnParticles(b.x, b.y, e.color, 4);
        if (e.hp<=0) {
          e.dead=true; score+=e.score; enemiesRemaining--;
          spawnParticles(e.x, e.y, e.color, 10);
          if (Math.random()<0.15) spawnPowerUp(e.x, e.y);
        }
      }
    });
  });

  // 충돌: 적 총알/적 → 플레이어
  if (player.power !== 'shield') {
    bullets.filter(b=>!b.player).forEach(b => {
      if (Math.abs(b.x-player.x)<20 && Math.abs(b.y-player.y)<20) {
        b.dead=true; loseLife();
      }
    });
    enemies.forEach(e => {
      if (!e.dead && Math.abs(e.x-player.x)<(e.width/2+15) && Math.abs(e.y-player.y)<(e.height/2+15)) {
        e.dead=true; enemiesRemaining--; loseLife();
        spawnParticles(e.x, e.y, e.color, 8);
      }
    });
  }

  bullets = bullets.filter(b=>!b.dead);
  enemies = enemies.filter(e=>!e.dead);

  // 파워업 수집
  powerUps.forEach(p => {
    p.y += 1.5; p.bob += dt*3;
    if (Math.abs(p.x-player.x)<25 && Math.abs(p.y-player.y)<25) {
      p.dead = true;
      if (p.type.id==='bomb') {
        enemies.forEach(e=>{e.dead=true;enemiesRemaining--;score+=e.score;spawnParticles(e.x,e.y,e.color,6);});
        showPowerUp('💣 화면 폭탄!');
      } else {
        player.power = p.type.id;
        player.powerTimer = p.type.duration;
        showPowerUp(`${p.type.icon} ${p.type.name}!`);
      }
    }
  });
  powerUps = powerUps.filter(p=>!p.dead && p.y<canvas.height+30);

  // 파티클
  particles.forEach(p=>{p.x+=p.vx;p.y+=p.vy;p.life--;});
  particles = particles.filter(p=>p.life>0);

  // 별
  stars.forEach(s=>{s.y+=s.speed;if(s.y>canvas.height){s.y=-5;s.x=Math.random()*canvas.width;}});

  // 웨이브 완료 체크
  if (enemiesRemaining<=0 && enemies.length===0) {
    waveTimer += dt*1000;
    if (waveTimer > 2000) { wave++; waveTimer=0; spawnWave(); }
  }
}

function loseLife() {
  lives--;
  spawnParticles(player.x, player.y, '#ff6b6b', 12);
  if (lives<=0) {
    gameRunning=false;
    document.getElementById('finalScore').textContent = `SCORE: ${score.toLocaleString()} · WAVE: ${wave}`;
    document.getElementById('gameOver').classList.remove('hidden');
  }
}

function spawnPowerUp(x, y) {
  const type = POWER_TYPES[Math.floor(Math.random()*POWER_TYPES.length)];
  powerUps.push({ x, y, type, bob:0 });
}

function spawnParticles(x, y, color, count) {
  for(let i=0;i<count;i++) particles.push({
    x, y, vx:(Math.random()-0.5)*6, vy:(Math.random()-0.5)*6,
    size:2+Math.random()*3, color, life:15+Math.random()*10, maxLife:25,
  });
}

// ===== 렌더링 =====
function render() {
  ctx.fillStyle='#0a0a1a'; ctx.fillRect(0,0,canvas.width,canvas.height);

  // 별
  stars.forEach(s=>{ctx.fillStyle=`rgba(255,255,255,${0.3+s.size*0.2})`;ctx.beginPath();ctx.arc(s.x,s.y,s.size,0,Math.PI*2);ctx.fill();});

  // 파워업
  powerUps.forEach(p=>{
    const by=Math.sin(p.bob)*4;
    ctx.fillStyle=p.type.color; ctx.globalAlpha=0.3;
    ctx.beginPath();ctx.arc(p.x,p.y+by,18,0,Math.PI*2);ctx.fill();
    ctx.globalAlpha=1; ctx.font='20px serif'; ctx.textAlign='center';
    ctx.fillText(p.type.icon, p.x, p.y+by+7);
  });

  // 적
  enemies.forEach(e=>{
    ctx.fillStyle = e.flash>0?'#fff':e.color;
    ctx.fillRect(e.x-e.width/2, e.y-e.height/2, e.width, e.height);
    if(e.type==='boss'){
      ctx.fillStyle='rgba(0,0,0,0.5)';ctx.fillRect(e.x-20,e.y-e.height/2-6,40,4);
      ctx.fillStyle='#fa5252';ctx.fillRect(e.x-20,e.y-e.height/2-6,40*(e.hp/e.maxHp),4);
    }
    // 눈
    ctx.fillStyle='rgba(255,255,255,0.8)';
    ctx.beginPath();ctx.arc(e.x-e.width*0.2,e.y,e.width*0.12,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.arc(e.x+e.width*0.2,e.y,e.width*0.12,0,Math.PI*2);ctx.fill();
  });

  // 총알
  bullets.forEach(b=>{
    ctx.fillStyle=b.player?'#ffd43b':'#ff6b6b';
    ctx.beginPath();ctx.arc(b.x,b.y,b.size,0,Math.PI*2);ctx.fill();
  });

  // 플레이어
  ctx.fillStyle = player.power==='shield'?'#339af0':'#51cf66';
  ctx.beginPath();ctx.moveTo(player.x,player.y-15);ctx.lineTo(player.x-12,player.y+12);ctx.lineTo(player.x+12,player.y+12);ctx.closePath();ctx.fill();
  // 엔진 불꽃
  ctx.fillStyle='#ff922b';ctx.globalAlpha=0.5+Math.random()*0.3;
  ctx.beginPath();ctx.moveTo(player.x-5,player.y+12);ctx.lineTo(player.x,player.y+20+Math.random()*6);ctx.lineTo(player.x+5,player.y+12);ctx.closePath();ctx.fill();
  ctx.globalAlpha=1;
  if(player.power==='shield'){ctx.strokeStyle='rgba(51,154,240,0.4)';ctx.lineWidth=2;ctx.beginPath();ctx.arc(player.x,player.y,22,0,Math.PI*2);ctx.stroke();}

  // 파티클
  particles.forEach(p=>{ctx.fillStyle=p.color;ctx.globalAlpha=p.life/p.maxLife;ctx.beginPath();ctx.arc(p.x,p.y,p.size,0,Math.PI*2);ctx.fill();});
  ctx.globalAlpha=1;

  // UI
  document.getElementById('score').textContent = `SCORE: ${score.toLocaleString()}`;
  document.getElementById('lives').textContent = '❤️'.repeat(Math.max(0,lives));
  document.getElementById('wave').textContent = `WAVE ${wave}`;
}

// ===== 게임 루프 =====
let lastTime=0;
function gameLoop(now) {
  if(!gameRunning) return;
  const dt=Math.min((now-lastTime)/1000, 0.05);
  lastTime=now;
  update(dt); render();
  requestAnimationFrame(gameLoop);
}

function startGame() {
  document.getElementById('startScreen').classList.add('hidden');
  document.getElementById('gameOver').classList.add('hidden');
  initGame();
  lastTime=performance.now();
  requestAnimationFrame(gameLoop);
}
