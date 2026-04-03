const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
canvas.width = window.innerWidth;
canvas.height = window.innerHeight;
window.addEventListener('resize', () => { canvas.width = innerWidth; canvas.height = innerHeight; });

// ===== 게임 상태 =====
let player, enemies, particles, coins, platforms, damageTexts;
let score, gameTime, kills, paused, gameRunning;
let camera = { x:0, y:0 };

const WORLD_W = 3000, WORLD_H = 1200;
const GRAVITY = 0.6, JUMP_FORCE = -13;

const SKILLS = [
  { id:'atk', icon:'⚔️', name:'공격력 +', desc:'공격력 25% 증가', apply:()=>{ player.damage=Math.floor(player.damage*1.25); }},
  { id:'spd', icon:'💨', name:'이동속도 +', desc:'이동속도 20% 증가', apply:()=>{ player.speed*=1.2; }},
  { id:'jump', icon:'🦘', name:'점프력 +', desc:'점프력 15% 증가', apply:()=>{ player.jumpForce*=1.15; }},
  { id:'hp', icon:'❤️', name:'체력 회복', desc:'체력 50% 회복', apply:()=>{ player.hp=Math.min(player.maxHp,player.hp+Math.floor(player.maxHp*0.5)); }},
  { id:'maxhp', icon:'💖', name:'최대체력 +', desc:'최대 체력 30% 증가', apply:()=>{ player.maxHp=Math.floor(player.maxHp*1.3); player.hp+=30; }},
  { id:'range', icon:'🎯', name:'공격범위 +', desc:'공격 범위 25% 증가', apply:()=>{ player.atkRange*=1.25; }},
  { id:'crit', icon:'💥', name:'크리티컬 +', desc:'크리티컬 확률 10% 증가', apply:()=>{ player.critChance=Math.min(0.8,player.critChance+0.1); }},
  { id:'coin', icon:'💰', name:'코인 보너스', desc:'코인 획득량 2배', apply:()=>{ player.coinMult*=2; }},
];

function initGame() {
  player = {
    x:200, y:0, w:28, h:40, vx:0, vy:0, speed:4.5, jumpForce:JUMP_FORCE,
    hp:100, maxHp:100, xp:0, xpToNext:15, level:1, damage:20,
    atkRange:50, atkCooldown:0, atkSpeed:400, facing:1, grounded:false,
    attacking:false, atkTimer:0, critChance:0.1, coinMult:1,
    invincible:0, combo:0, comboTimer:0,
  };
  enemies=[]; particles=[]; coins=[]; damageTexts=[];
  score=0; gameTime=0; kills=0; paused=false; gameRunning=true;

  // 플랫폼 생성
  platforms = [
    // 바닥
    {x:0, y:WORLD_H-40, w:WORLD_W, h:40, color:'#2d5016'},
    // 플랫폼들
    {x:150, y:WORLD_H-160, w:200, h:20, color:'#4a7c28'},
    {x:500, y:WORLD_H-240, w:180, h:20, color:'#4a7c28'},
    {x:800, y:WORLD_H-180, w:250, h:20, color:'#4a7c28'},
    {x:1150, y:WORLD_H-300, w:200, h:20, color:'#4a7c28'},
    {x:1500, y:WORLD_H-200, w:220, h:20, color:'#4a7c28'},
    {x:1800, y:WORLD_H-280, w:180, h:20, color:'#4a7c28'},
    {x:2100, y:WORLD_H-160, w:250, h:20, color:'#4a7c28'},
    {x:2450, y:WORLD_H-240, w:200, h:20, color:'#4a7c28'},
    {x:2750, y:WORLD_H-320, w:180, h:20, color:'#4a7c28'},
  ];
  player.y = WORLD_H - 40 - player.h;
}

// ===== 입력 =====
const keys = {};
window.addEventListener('keydown', e => { keys[e.key]=true; if(e.key===' ')e.preventDefault(); });
window.addEventListener('keyup', e => keys[e.key]=false);

// ===== 적 생성 =====
function spawnEnemy() {
  const tier = Math.min(3, Math.floor(gameTime/40)+1);
  const isBoss = gameTime>30 && Math.random()<0.05;
  const side = Math.random()<0.5 ? camera.x-60 : camera.x+canvas.width+60;
  const platIdx = Math.floor(Math.random()*platforms.length);
  const plat = platforms[platIdx];
  const ex = side;
  const ey = plat.y - (isBoss?50:30);
  const names = ['박쥐','좀비','뱀파이어','드라큘라'];
  const colors = [['#51cf66','#37b24d'],['#ff922b','#e67700'],['#ae3ec9','#862e9c'],['#fa5252','#e03131']];
  const ci = Math.min(tier, colors.length-1);
  enemies.push({
    x:ex, y:ey, w:isBoss?50:22+tier*4, h:isBoss?50:26+tier*4,
    vx:0, vy:0, speed:1+tier*0.4+(isBoss?-0.3:0),
    hp:isBoss?80+tier*40:8+tier*6, maxHp:isBoss?80+tier*40:8+tier*6,
    damage:isBoss?15+tier*5:5+tier*3, xp:isBoss?12:2+tier,
    score:isBoss?300:50+tier*30, coins:isBoss?5:1+Math.floor(Math.random()*tier),
    color:colors[ci][0], colorDark:colors[ci][1],
    name:isBoss?'보스 '+names[ci]:names[ci],
    boss:isBoss, flash:0, grounded:false, dir:1, patrol:0,
    knockback:0,
  });
}

// ===== 공격 =====
function playerAttack() {
  if(player.atkCooldown>0||player.attacking) return;
  player.attacking=true; player.atkTimer=150;
  player.atkCooldown=player.atkSpeed;

  const ax = player.x + player.facing*player.atkRange/2;
  const ay = player.y + player.h/2;
  let hitAny = false;

  enemies.forEach(e=>{
    const dx=Math.abs(e.x+e.w/2-(player.x+player.w/2+player.facing*player.atkRange/2));
    const dy=Math.abs(e.y+e.h/2-(player.y+player.h/2));
    if(dx<player.atkRange/2+e.w/2 && dy<e.h){
      const crit = Math.random()<player.critChance;
      const dmg = crit?player.damage*2:player.damage;
      e.hp-=dmg; e.flash=0.15;
      e.knockback=player.facing*8;
      hitAny=true;

      damageTexts.push({x:e.x+e.w/2,y:e.y-10,text:crit?dmg+'!':String(dmg),color:crit?'#ffd43b':'#fff',size:crit?18:14,life:40});
      spawnParticles(e.x+e.w/2,e.y+e.h/2,e.color,3);

      if(e.hp<=0){
        kills++; score+=e.score;
        // 경험치
        player.xp+=e.xp;
        if(player.xp>=player.xpToNext) levelUp();
        // 코인 드롭
        for(let i=0;i<e.coins*player.coinMult;i++){
          coins.push({x:e.x+Math.random()*e.w,y:e.y,vx:(Math.random()-0.5)*4,vy:-3-Math.random()*4,size:6,life:300});
        }
        spawnParticles(e.x+e.w/2,e.y+e.h/2,e.color,10);
        e.dead=true;
        // 콤보
        player.combo++; player.comboTimer=120;
      }
    }
  });
  // 공격 이펙트
  spawnParticles(ax,ay,'rgba(255,255,255,0.6)',2);
}

// ===== 업데이트 =====
function update(dt) {
  // 이동
  player.vx=0;
  if(keys['a']||keys['ArrowLeft']){player.vx=-player.speed;player.facing=-1;}
  if(keys['d']||keys['ArrowRight']){player.vx=player.speed;player.facing=1;}
  if((keys['w']||keys['ArrowUp']||keys[' '])&&player.grounded){player.vy=player.jumpForce;player.grounded=false;}
  if(keys['z']||keys['x']||keys['Control']) playerAttack();

  // 물리
  player.vy+=GRAVITY;
  player.x+=player.vx; player.y+=player.vy;
  player.x=Math.max(0,Math.min(WORLD_W-player.w,player.x));
  player.grounded=false;

  // 플랫폼 충돌
  platforms.forEach(p=>{
    if(player.x+player.w>p.x && player.x<p.x+p.w && player.y+player.h>p.y && player.y+player.h<p.y+p.h+player.vy+2 && player.vy>=0){
      player.y=p.y-player.h; player.vy=0; player.grounded=true;
    }
  });

  // 쿨다운
  if(player.atkCooldown>0) player.atkCooldown-=dt*1000;
  if(player.atkTimer>0) player.atkTimer-=dt*1000; else player.attacking=false;
  if(player.invincible>0) player.invincible-=dt;
  if(player.comboTimer>0) player.comboTimer--; else player.combo=0;

  // 적 AI
  enemies.forEach(e=>{
    if(e.knockback){e.x+=e.knockback;e.knockback*=0.8;if(Math.abs(e.knockback)<0.5)e.knockback=0;}
    const toPlayer=player.x-e.x;
    e.dir=toPlayer>0?1:-1;
    const dist=Math.abs(toPlayer);
    if(dist<400) e.vx=e.dir*e.speed; else{e.patrol+=dt;e.vx=Math.sin(e.patrol)*e.speed*0.5;}
    e.vy+=GRAVITY; e.x+=e.vx; e.y+=e.vy;
    e.grounded=false;
    platforms.forEach(p=>{
      if(e.x+e.w>p.x&&e.x<p.x+p.w&&e.y+e.h>p.y&&e.y+e.h<p.y+p.h+e.vy+2&&e.vy>=0){
        e.y=p.y-e.h;e.vy=0;e.grounded=true;
      }
    });
    if(e.flash>0)e.flash-=dt;
    // 플레이어 충돌 데미지
    if(player.invincible<=0&&Math.abs(e.x+e.w/2-player.x-player.w/2)<(e.w/2+player.w/2)&&Math.abs(e.y+e.h/2-player.y-player.h/2)<(e.h/2+player.h/2)){
      player.hp-=e.damage; player.invincible=1;
      damageTexts.push({x:player.x+player.w/2,y:player.y-10,text:'-'+e.damage,color:'#ff6b6b',size:16,life:40});
      spawnParticles(player.x+player.w/2,player.y+player.h/2,'#ff6b6b',5);
      if(player.hp<=0){gameRunning=false;showGameOver();}
    }
  });
  enemies=enemies.filter(e=>!e.dead);

  // 코인
  coins.forEach(c=>{c.vy+=0.3;c.x+=c.vx;c.y+=c.vy;c.vx*=0.98;c.life--;
    platforms.forEach(p=>{if(c.x>p.x&&c.x<p.x+p.w&&c.y+c.size>p.y&&c.vy>0){c.y=p.y-c.size;c.vy=-c.vy*0.3;c.vx*=0.5;}});
    if(Math.hypot(c.x-player.x-player.w/2,c.y-player.y-player.h/2)<40){c.dead=true;score+=10;}
  });
  coins=coins.filter(c=>!c.dead&&c.life>0);

  // 데미지 텍스트
  damageTexts.forEach(d=>{d.y-=1.2;d.life--;});
  damageTexts=damageTexts.filter(d=>d.life>0);

  // 파티클
  particles.forEach(p=>{p.x+=p.vx;p.y+=p.vy;p.vy+=0.1;p.life--;});
  particles=particles.filter(p=>p.life>0);

  // 카메라
  camera.x+=(player.x-canvas.width/2-camera.x)*0.08;
  camera.y+=(player.y-canvas.height/2+100-camera.y)*0.08;
  camera.x=Math.max(0,Math.min(WORLD_W-canvas.width,camera.x));
  camera.y=Math.max(0,Math.min(WORLD_H-canvas.height,camera.y));
}

// ===== 렌더링 =====
function render() {
  // 하늘 그라디언트
  const grad=ctx.createLinearGradient(0,0,0,canvas.height);
  grad.addColorStop(0,'#1a1a3e');grad.addColorStop(0.5,'#2d1b4e');grad.addColorStop(1,'#0d1117');
  ctx.fillStyle=grad;ctx.fillRect(0,0,canvas.width,canvas.height);

  // 배경 별
  ctx.fillStyle='rgba(255,255,255,0.3)';
  for(let i=0;i<30;i++){const sx=(i*137+50)%canvas.width,sy=(i*97+30)%canvas.height*0.5;ctx.fillRect(sx,sy,1.5,1.5);}

  ctx.save();ctx.translate(-camera.x,-camera.y);

  // 배경 나무/풀
  for(let i=0;i<WORLD_W;i+=200){
    ctx.fillStyle='#1a4d0c';
    ctx.beginPath();ctx.arc(i+100,WORLD_H-60,40+Math.sin(i)*15,0,Math.PI*2);ctx.fill();
    ctx.fillStyle='#3d2010';ctx.fillRect(i+95,WORLD_H-50,10,20);
  }

  // 플랫폼
  platforms.forEach(p=>{
    ctx.fillStyle=p.color;ctx.fillRect(p.x,p.y,p.w,p.h);
    // 잔디 위
    ctx.fillStyle='#5c9e2f';ctx.fillRect(p.x,p.y,p.w,4);
  });

  // 코인
  coins.forEach(c=>{
    ctx.fillStyle='#ffd43b';ctx.beginPath();ctx.arc(c.x,c.y,c.size*Math.abs(Math.cos(c.life*0.05)),0,Math.PI*2);ctx.fill();
    ctx.fillStyle='#ff922b';ctx.beginPath();ctx.arc(c.x,c.y,c.size*0.5*Math.abs(Math.cos(c.life*0.05)),0,Math.PI*2);ctx.fill();
  });

  // 적
  enemies.forEach(e=>{
    const c=e.flash>0?'#fff':e.color;
    // 몸통
    ctx.fillStyle=c;
    ctx.beginPath();ctx.roundRect(e.x,e.y,e.w,e.h,e.boss?8:5);ctx.fill();
    // 배
    ctx.fillStyle=e.flash>0?'#fff':e.colorDark;
    ctx.beginPath();ctx.roundRect(e.x+e.w*0.15,e.y+e.h*0.5,e.w*0.7,e.h*0.35,3);ctx.fill();
    // 눈
    const ew=e.w*0.2,ex1=e.x+e.w*0.25,ex2=e.x+e.w*0.55,ey=e.y+e.h*0.25;
    ctx.fillStyle='white';ctx.beginPath();ctx.ellipse(ex1,ey,ew,ew*1.2,0,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.ellipse(ex2,ey,ew,ew*1.2,0,0,Math.PI*2);ctx.fill();
    ctx.fillStyle='#111';ctx.beginPath();ctx.arc(ex1+e.dir*2,ey,ew*0.5,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.arc(ex2+e.dir*2,ey,ew*0.5,0,Math.PI*2);ctx.fill();
    // HP바 (보스)
    if(e.boss){ctx.fillStyle='rgba(0,0,0,0.5)';ctx.fillRect(e.x,e.y-10,e.w,5);ctx.fillStyle='#fa5252';ctx.fillRect(e.x,e.y-10,e.w*(e.hp/e.maxHp),5);}
    // 이름
    if(e.boss){ctx.fillStyle='#ffd43b';ctx.font='bold 11px sans-serif';ctx.textAlign='center';ctx.fillText(e.name,e.x+e.w/2,e.y-14);}
  });

  // 플레이어
  const pa=player.invincible>0?0.6:1;ctx.globalAlpha=pa;
  // 몸통
  ctx.fillStyle='#74c0fc';
  ctx.beginPath();ctx.roundRect(player.x,player.y,player.w,player.h,6);ctx.fill();
  // 얼굴
  ctx.fillStyle='#ffe8cc';
  ctx.beginPath();ctx.roundRect(player.x+4,player.y+4,player.w-8,player.h*0.4,4);ctx.fill();
  // 눈
  const px=player.facing>0?player.x+player.w*0.6:player.x+player.w*0.25;
  ctx.fillStyle='#111';ctx.beginPath();ctx.arc(px,player.y+player.h*0.22,2.5,0,Math.PI*2);ctx.fill();
  ctx.beginPath();ctx.arc(px+player.facing*8,player.y+player.h*0.22,2.5,0,Math.PI*2);ctx.fill();
  // 공격 이펙트
  if(player.attacking){
    ctx.strokeStyle='rgba(255,255,255,0.6)';ctx.lineWidth=3;
    ctx.beginPath();ctx.arc(player.x+player.w/2+player.facing*30,player.y+player.h/2,player.atkRange/2,0,Math.PI*2);ctx.stroke();
  }
  ctx.globalAlpha=1;

  // 데미지 텍스트
  damageTexts.forEach(d=>{
    ctx.globalAlpha=d.life/40;ctx.fillStyle=d.color;ctx.font=`bold ${d.size}px sans-serif`;ctx.textAlign='center';
    ctx.fillText(d.text,d.x,d.y);
  });ctx.globalAlpha=1;

  // 파티클
  particles.forEach(p=>{ctx.fillStyle=p.color;ctx.globalAlpha=p.life/p.maxLife;ctx.beginPath();ctx.arc(p.x,p.y,p.size,0,Math.PI*2);ctx.fill();});
  ctx.globalAlpha=1;

  ctx.restore();

  // UI
  const hpPct=player.hp/player.maxHp, xpPct=player.xp/player.xpToNext;
  document.getElementById('hpFill').style.width=(hpPct*100)+'%';
  document.getElementById('hpText').textContent=`HP ${Math.ceil(player.hp)}/${player.maxHp}`;
  document.getElementById('xpFill').style.width=(xpPct*100)+'%';
  document.getElementById('xpText').textContent=`XP ${player.xp}/${player.xpToNext}`;
  document.getElementById('level').textContent=`Lv.${player.level}`;
  const m=Math.floor(gameTime/60),s=Math.floor(gameTime%60);
  document.getElementById('timer').textContent=`${m}:${s.toString().padStart(2,'0')}`;
  document.getElementById('kills').textContent=`${kills} KILLS · ${score} PTS`;
  // 콤보
  if(player.combo>1){
    ctx.fillStyle='#ffd43b';ctx.font='bold 24px sans-serif';ctx.textAlign='center';ctx.globalAlpha=Math.min(1,player.comboTimer/30);
    ctx.fillText(`${player.combo} COMBO!`,canvas.width/2,80);ctx.globalAlpha=1;
  }
}

function spawnParticles(x,y,color,count){
  for(let i=0;i<count;i++) particles.push({x,y,vx:(Math.random()-0.5)*5,vy:(Math.random()-0.5)*5-2,size:2+Math.random()*3,color,life:15+Math.random()*10,maxLife:25});
}

// ===== 레벨업 =====
function levelUp(){
  player.xp-=player.xpToNext; player.xpToNext=Math.floor(player.xpToNext*1.35); player.level++;
  paused=true;
  const picks=[...SKILLS].sort(()=>Math.random()-0.5).slice(0,3);
  const div=document.getElementById('skills');div.innerHTML='';
  picks.forEach(s=>{
    const btn=document.createElement('div');btn.className='skill-btn';
    btn.innerHTML=`<div class="skill-icon">${s.icon}</div><div class="skill-name">${s.name}</div><div class="skill-desc">${s.desc}</div>`;
    btn.onclick=()=>{s.apply();paused=false;document.getElementById('levelUp').classList.add('hidden');};
    div.appendChild(btn);
  });
  document.getElementById('levelUp').classList.remove('hidden');
  spawnParticles(player.x+player.w/2,player.y,'#ffd43b',15);
}

function showGameOver(){
  const m=Math.floor(gameTime/60),s=Math.floor(gameTime%60);
  document.getElementById('finalScore').textContent=`Lv.${player.level} · ${kills} KILLS · ${score} PTS · ${m}:${s.toString().padStart(2,'0')}`;
  document.getElementById('gameOver').classList.remove('hidden');
}

// ===== 게임 루프 =====
let lastTime=0, spawnTimer=0;
function gameLoop(now){
  if(!gameRunning) return;
  const dt=Math.min((now-lastTime)/1000,0.05);lastTime=now;
  if(!paused){
    gameTime+=dt; spawnTimer+=dt;
    const rate=Math.max(0.8, 2.5-gameTime*0.01);
    while(spawnTimer>rate){spawnEnemy();spawnTimer-=rate;}
    update(dt);
  }
  render();
  requestAnimationFrame(gameLoop);
}

function startGame(){
  document.getElementById('startScreen').classList.add('hidden');
  initGame(); lastTime=performance.now();
  requestAnimationFrame(gameLoop);
}
