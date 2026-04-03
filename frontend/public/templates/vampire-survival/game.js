const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
canvas.width = window.innerWidth;
canvas.height = window.innerHeight;
window.addEventListener('resize', () => { canvas.width = innerWidth; canvas.height = innerHeight; });

// ===== 게임 상태 =====
let player, enemies, bullets, particles, xpOrbs, gameTime, kills, paused, gameRunning;

const SKILLS = [
  { id:'dmg', icon:'⚔️', name:'공격력 +', desc:'공격력 20% 증가', apply:()=>{ player.damage*=1.2; }},
  { id:'spd', icon:'💨', name:'이동속도 +', desc:'이동속도 15% 증가', apply:()=>{ player.speed*=1.15; }},
  { id:'rate', icon:'🔥', name:'공격속도 +', desc:'공격 간격 15% 감소', apply:()=>{ player.fireRate*=0.85; }},
  { id:'hp', icon:'❤️', name:'체력 회복', desc:'체력 30% 회복', apply:()=>{ player.hp=Math.min(player.maxHp, player.hp+player.maxHp*0.3); }},
  { id:'maxhp', icon:'💖', name:'최대체력 +', desc:'최대 체력 25% 증가', apply:()=>{ player.maxHp*=1.25; player.hp+=player.maxHp*0.1; }},
  { id:'range', icon:'🎯', name:'사거리 +', desc:'공격 범위 20% 증가', apply:()=>{ player.range*=1.2; }},
  { id:'multi', icon:'✨', name:'다중 발사', desc:'발사 수 +1', apply:()=>{ player.bulletCount++; }},
  { id:'pierce', icon:'💎', name:'관통', desc:'관통력 +1', apply:()=>{ player.pierce++; }},
];

function initGame() {
  player = {
    x: canvas.width/2, y: canvas.height/2, size: 16, speed: 3, hp: 100, maxHp: 100,
    xp: 0, xpToNext: 20, level: 1, damage: 15, fireRate: 500, lastFire: 0,
    range: 200, bulletCount: 1, pierce: 1, invincible: 0,
  };
  enemies = []; bullets = []; particles = []; xpOrbs = [];
  gameTime = 0; kills = 0; paused = false; gameRunning = true;
}

// ===== 입력 =====
const keys = {};
window.addEventListener('keydown', e => keys[e.key] = true);
window.addEventListener('keyup', e => keys[e.key] = false);

// ===== 적 생성 =====
function spawnEnemy() {
  const angle = Math.random() * Math.PI * 2;
  const dist = Math.max(canvas.width, canvas.height) * 0.6;
  const tier = gameTime > 120 ? 3 : gameTime > 60 ? 2 : 1;
  const isBoss = gameTime > 30 && Math.random() < 0.03;
  enemies.push({
    x: player.x + Math.cos(angle) * dist,
    y: player.y + Math.sin(angle) * dist,
    size: isBoss ? 28 : 8 + tier * 3,
    speed: isBoss ? 0.8 : 0.8 + tier * 0.3,
    hp: isBoss ? 100 + tier * 80 : 5 + tier * 8,
    maxHp: isBoss ? 100 + tier * 80 : 5 + tier * 8,
    damage: isBoss ? 25 : 5 + tier * 3,
    xp: isBoss ? 15 : 2 + tier,
    color: isBoss ? '#ff6b6b' : ['#ae3ec9','#e64980','#f76707'][tier-1],
    boss: isBoss,
    flash: 0,
  });
}

// ===== 자동 공격 =====
function autoFire(now) {
  if (now - player.lastFire < player.fireRate) return;
  player.lastFire = now;
  // 가장 가까운 적 찾기
  let closest = null, minDist = player.range;
  enemies.forEach(e => {
    const d = Math.hypot(e.x - player.x, e.y - player.y);
    if (d < minDist) { minDist = d; closest = e; }
  });
  if (!closest) return;
  const angle = Math.atan2(closest.y - player.y, closest.x - player.x);
  for (let i = 0; i < player.bulletCount; i++) {
    const spread = (i - (player.bulletCount-1)/2) * 0.15;
    bullets.push({
      x: player.x, y: player.y, angle: angle + spread,
      speed: 8, damage: player.damage, pierce: player.pierce, life: 60,
    });
  }
}

// ===== 업데이트 =====
function update(dt, now) {
  // 이동
  let dx=0, dy=0;
  if (keys['w']||keys['ArrowUp']) dy=-1;
  if (keys['s']||keys['ArrowDown']) dy=1;
  if (keys['a']||keys['ArrowLeft']) dx=-1;
  if (keys['d']||keys['ArrowRight']) dx=1;
  if (dx||dy) { const len=Math.hypot(dx,dy); player.x+=dx/len*player.speed; player.y+=dy/len*player.speed; }

  // 무적 타이머
  if (player.invincible > 0) player.invincible -= dt;

  // 적 이동 + 충돌
  enemies.forEach(e => {
    const a = Math.atan2(player.y-e.y, player.x-e.x);
    e.x += Math.cos(a)*e.speed; e.y += Math.sin(a)*e.speed;
    if (e.flash > 0) e.flash -= dt;
    // 플레이어 충돌
    if (Math.hypot(e.x-player.x, e.y-player.y) < player.size+e.size && player.invincible<=0) {
      player.hp -= e.damage; player.invincible = 0.5;
      spawnParticles(player.x, player.y, '#ff6b6b', 5);
      if (player.hp <= 0) { gameRunning = false; showGameOver(); }
    }
  });

  // 총알
  bullets.forEach(b => {
    b.x += Math.cos(b.angle)*b.speed; b.y += Math.sin(b.angle)*b.speed; b.life--;
    enemies.forEach(e => {
      if (b.pierce <= 0) return;
      if (Math.hypot(b.x-e.x, b.y-e.y) < e.size+4) {
        e.hp -= b.damage; b.pierce--; e.flash = 0.1;
        spawnParticles(b.x, b.y, e.color, 3);
        if (e.hp <= 0) {
          kills++;
          xpOrbs.push({ x:e.x, y:e.y, xp:e.xp, size:4, life:300 });
          spawnParticles(e.x, e.y, e.color, 8);
          e.dead = true;
        }
      }
    });
  });
  bullets = bullets.filter(b => b.life > 0 && b.pierce > 0);
  enemies = enemies.filter(e => !e.dead);

  // 경험치 오브 수집
  xpOrbs.forEach(o => {
    o.life--;
    const d = Math.hypot(o.x-player.x, o.y-player.y);
    if (d < 80) { // 자석 효과
      const a=Math.atan2(player.y-o.y, player.x-o.x);
      o.x+=Math.cos(a)*5; o.y+=Math.sin(a)*5;
    }
    if (d < player.size+o.size) {
      player.xp += o.xp; o.dead = true;
      if (player.xp >= player.xpToNext) levelUp();
    }
  });
  xpOrbs = xpOrbs.filter(o => !o.dead && o.life > 0);

  // 파티클
  particles.forEach(p => { p.x+=p.vx; p.y+=p.vy; p.life--; p.vy+=0.05; });
  particles = particles.filter(p => p.life > 0);

  autoFire(now);
}

// ===== 렌더링 =====
function render() {
  ctx.fillStyle = '#0a0a1a';
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // 카메라 (플레이어 중심)
  const cx = canvas.width/2 - player.x, cy = canvas.height/2 - player.y;
  ctx.save(); ctx.translate(cx, cy);

  // 그리드
  ctx.strokeStyle = 'rgba(255,255,255,0.03)'; ctx.lineWidth = 1;
  const gs=60, sx=Math.floor((player.x-canvas.width/2)/gs)*gs, sy=Math.floor((player.y-canvas.height/2)/gs)*gs;
  for(let x=sx;x<player.x+canvas.width/2;x+=gs){ctx.beginPath();ctx.moveTo(x,-10000);ctx.lineTo(x,10000);ctx.stroke();}
  for(let y=sy;y<player.y+canvas.height/2;y+=gs){ctx.beginPath();ctx.moveTo(-10000,y);ctx.lineTo(10000,y);ctx.stroke();}

  // XP 오브
  xpOrbs.forEach(o => {
    ctx.fillStyle = `rgba(51,154,240,${Math.min(1,o.life/30)})`;
    ctx.beginPath(); ctx.arc(o.x, o.y, o.size, 0, Math.PI*2); ctx.fill();
    ctx.fillStyle = `rgba(116,192,252,${Math.min(1,o.life/30)*0.5})`;
    ctx.beginPath(); ctx.arc(o.x, o.y, o.size*2, 0, Math.PI*2); ctx.fill();
  });

  // 적
  enemies.forEach(e => {
    ctx.fillStyle = e.flash > 0 ? '#fff' : e.color;
    ctx.beginPath(); ctx.arc(e.x, e.y, e.size, 0, Math.PI*2); ctx.fill();
    if (e.boss) { // 보스 HP바
      ctx.fillStyle='rgba(0,0,0,0.5)'; ctx.fillRect(e.x-20,e.y-e.size-8,40,5);
      ctx.fillStyle='#fa5252'; ctx.fillRect(e.x-20,e.y-e.size-8,40*(e.hp/e.maxHp),5);
    }
    // 눈
    const ex=e.size*0.3;
    ctx.fillStyle='white';
    ctx.beginPath();ctx.arc(e.x-ex,e.y-e.size*0.2,e.size*0.2,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.arc(e.x+ex,e.y-e.size*0.2,e.size*0.2,0,Math.PI*2);ctx.fill();
    ctx.fillStyle='#0a0a1a';
    ctx.beginPath();ctx.arc(e.x-ex,e.y-e.size*0.15,e.size*0.1,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.arc(e.x+ex,e.y-e.size*0.15,e.size*0.1,0,Math.PI*2);ctx.fill();
  });

  // 총알
  ctx.fillStyle = '#ffd43b';
  bullets.forEach(b => { ctx.beginPath(); ctx.arc(b.x, b.y, 3, 0, Math.PI*2); ctx.fill(); });

  // 플레이어
  const pa = player.invincible > 0 ? 0.5 : 1;
  ctx.globalAlpha = pa;
  ctx.fillStyle = '#74c0fc'; ctx.beginPath(); ctx.arc(player.x, player.y, player.size, 0, Math.PI*2); ctx.fill();
  ctx.fillStyle = '#339af0'; ctx.beginPath(); ctx.arc(player.x, player.y, player.size*0.6, 0, Math.PI*2); ctx.fill();
  // 사거리 표시
  ctx.strokeStyle = 'rgba(51,154,240,0.1)'; ctx.lineWidth = 1;
  ctx.beginPath(); ctx.arc(player.x, player.y, player.range, 0, Math.PI*2); ctx.stroke();
  ctx.globalAlpha = 1;

  // 파티클
  particles.forEach(p => {
    ctx.fillStyle = p.color; ctx.globalAlpha = p.life/p.maxLife;
    ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, Math.PI*2); ctx.fill();
  });
  ctx.globalAlpha = 1;

  ctx.restore();

  // UI 업데이트
  document.getElementById('hpFill').style.width = (player.hp/player.maxHp*100)+'%';
  document.getElementById('hpText').textContent = `HP ${Math.ceil(player.hp)}/${Math.ceil(player.maxHp)}`;
  document.getElementById('xpFill').style.width = (player.xp/player.xpToNext*100)+'%';
  document.getElementById('xpText').textContent = `XP ${player.xp}/${player.xpToNext}`;
  document.getElementById('level').textContent = `Lv.${player.level}`;
  const m=Math.floor(gameTime/60), s=Math.floor(gameTime%60);
  document.getElementById('timer').textContent = `${m}:${s.toString().padStart(2,'0')}`;
  document.getElementById('kills').textContent = `${kills} KILLS`;
}

// ===== 파티클 =====
function spawnParticles(x, y, color, count) {
  for(let i=0;i<count;i++){
    particles.push({
      x, y, vx:(Math.random()-0.5)*4, vy:(Math.random()-0.5)*4-1,
      size:2+Math.random()*3, color, life:20+Math.random()*15, maxLife:35,
    });
  }
}

// ===== 레벨업 =====
function levelUp() {
  player.xp -= player.xpToNext;
  player.xpToNext = Math.floor(player.xpToNext * 1.3);
  player.level++;
  paused = true;
  // 랜덤 3개 스킬
  const picks = [...SKILLS].sort(()=>Math.random()-0.5).slice(0,3);
  const div = document.getElementById('skills');
  div.innerHTML = '';
  picks.forEach(s => {
    const btn = document.createElement('div');
    btn.className = 'skill-btn';
    btn.innerHTML = `<div class="skill-icon">${s.icon}</div><div class="skill-name">${s.name}</div><div class="skill-desc">${s.desc}</div>`;
    btn.onclick = () => { s.apply(); paused=false; document.getElementById('levelUp').classList.add('hidden'); };
    div.appendChild(btn);
  });
  document.getElementById('levelUp').classList.remove('hidden');
}

// ===== 게임 오버 =====
function showGameOver() {
  const m=Math.floor(gameTime/60), s=Math.floor(gameTime%60);
  document.getElementById('finalScore').textContent = `Lv.${player.level} · ${kills} KILLS · ${m}:${s.toString().padStart(2,'0')}`;
  document.getElementById('gameOver').classList.remove('hidden');
}

// ===== 게임 루프 =====
let lastTime = 0, spawnTimer = 0;
function gameLoop(now) {
  if (!gameRunning) return;
  const dt = Math.min((now - lastTime) / 1000, 0.05);
  lastTime = now;

  if (!paused) {
    gameTime += dt;
    spawnTimer += dt;
    const spawnRate = Math.max(0.3, 1.5 - gameTime * 0.01);
    while (spawnTimer > spawnRate) { spawnEnemy(); spawnTimer -= spawnRate; }
    update(dt, now);
  }
  render();
  requestAnimationFrame(gameLoop);
}

function startGame() {
  document.getElementById('startScreen').classList.add('hidden');
  initGame();
  lastTime = performance.now();
  requestAnimationFrame(gameLoop);
}
