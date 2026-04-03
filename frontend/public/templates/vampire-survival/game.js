const canvas=document.getElementById('gameCanvas'),ctx=canvas.getContext('2d');
canvas.width=innerWidth;canvas.height=innerHeight;
window.addEventListener('resize',()=>{canvas.width=innerWidth;canvas.height=innerHeight;});

// ===== 상수 =====
const G=0.55,TILE=40;
const keys={};
window.addEventListener('keydown',e=>{keys[e.key]=true;if([' ','ArrowUp','ArrowDown','i','s'].includes(e.key))e.preventDefault();});
window.addEventListener('keyup',e=>keys[e.key]=false);

// ===== 메이플 SD 캐릭터 (초거대 머리 + 반짝 눈 + 미니 몸) =====
const SKIN='#ffe0bd',SKIN_S='#f5c69a',BLUSH='#ffb3b3';
// Canvas로 직접 그리기 (도트 배열 대신 함수)
function drawMapleChar(cx,cy,sprite,dir,walking,attacking,grounded){
  const d=dir; // 1=right, -1=left
  ctx.save();
  if(d<0){ctx.translate(cx+sprite.w,cy);ctx.scale(-1,1);cx=0;cy=0;}
  else{ctx.translate(cx,cy);cx=0;cy=0;}

  const hw=sprite.headW,hh=sprite.headH; // 머리 크기
  const bw=sprite.bodyW,bh=sprite.bodyH;
  const hx=cx+(sprite.w-hw)/2, hy=cy;
  const bx=cx+(sprite.w-bw)/2, by=cy+hh-4;

  // === 머리카락 (풍성하게) ===
  ctx.fillStyle=sprite.hair;
  // 뒤 머리카락
  roundRect(hx-3,hy-2,hw+6,hh+8,hw/2);
  ctx.fill();
  // 앞머리
  ctx.fillStyle=sprite.hairDk;
  roundRect(hx,hy,hw,hh*0.45,hw/3);
  ctx.fill();
  // 옆머리
  ctx.fillStyle=sprite.hair;
  ctx.fillRect(hx-4,hy+hh*0.3,5,hh*0.5);
  ctx.fillRect(hx+hw-1,hy+hh*0.3,5,hh*0.5);

  // === 얼굴 ===
  ctx.fillStyle=SKIN;
  roundRect(hx+2,hy+hh*0.3,hw-4,hh*0.6,6);
  ctx.fill();

  // === 눈 (거대 + 반짝) ===
  const eyeY=hy+hh*0.48, eyeW=hw*0.22, eyeH=hw*0.28;
  const lex=hx+hw*0.22, rex=hx+hw*0.58;
  // 흰자
  ctx.fillStyle='#fff';
  ctx.beginPath();ctx.ellipse(lex,eyeY,eyeW,eyeH,0,0,Math.PI*2);ctx.fill();
  ctx.beginPath();ctx.ellipse(rex,eyeY,eyeW,eyeH,0,0,Math.PI*2);ctx.fill();
  // 동공 (큰 원)
  ctx.fillStyle=sprite.eyeColor||'#2c2c2c';
  ctx.beginPath();ctx.ellipse(lex+1,eyeY+1,eyeW*0.7,eyeH*0.75,0,0,Math.PI*2);ctx.fill();
  ctx.beginPath();ctx.ellipse(rex+1,eyeY+1,eyeW*0.7,eyeH*0.75,0,0,Math.PI*2);ctx.fill();
  // 하이라이트 (별 반짝)
  ctx.fillStyle='#fff';
  ctx.beginPath();ctx.arc(lex-eyeW*0.2,eyeY-eyeH*0.25,eyeW*0.3,0,Math.PI*2);ctx.fill();
  ctx.beginPath();ctx.arc(rex-eyeW*0.2,eyeY-eyeH*0.25,eyeW*0.3,0,Math.PI*2);ctx.fill();
  // 작은 하이라이트
  ctx.beginPath();ctx.arc(lex+eyeW*0.25,eyeY+eyeH*0.2,eyeW*0.15,0,Math.PI*2);ctx.fill();
  ctx.beginPath();ctx.arc(rex+eyeW*0.25,eyeY+eyeH*0.2,eyeW*0.15,0,Math.PI*2);ctx.fill();

  // 볼터치
  ctx.fillStyle='rgba(255,150,150,0.35)';
  ctx.beginPath();ctx.ellipse(hx+hw*0.12,hy+hh*0.62,4,3,0,0,Math.PI*2);ctx.fill();
  ctx.beginPath();ctx.ellipse(hx+hw*0.72,hy+hh*0.62,4,3,0,0,Math.PI*2);ctx.fill();

  // 입 (작은 미소)
  ctx.fillStyle='#e07070';
  ctx.beginPath();ctx.arc(hx+hw*0.44,hy+hh*0.7,2,0,Math.PI);ctx.fill();

  // === 몸통 ===
  ctx.fillStyle=sprite.shirt;
  roundRect(bx,by,bw,bh*0.55,3);ctx.fill();
  // 팔
  const armY=by+2;
  ctx.fillStyle=SKIN;
  ctx.fillRect(bx-3,armY,4,bh*0.35);
  ctx.fillRect(bx+bw-1,armY,4,bh*0.35);

  // === 하의 ===
  ctx.fillStyle=sprite.pants;
  ctx.fillRect(bx+1,by+bh*0.5,bw-2,bh*0.25);

  // === 다리 + 신발 ===
  const legY=by+bh*0.7;
  const walkOff=walking?Math.sin(Date.now()/120)*4:0;
  // 왼다리
  ctx.fillStyle=SKIN_S;
  ctx.fillRect(bx+2,legY+walkOff,bw*0.3,bh*0.2);
  ctx.fillStyle=sprite.shoe;
  ctx.fillRect(bx+1,legY+bh*0.18+walkOff,bw*0.35,bh*0.12);
  // 오른다리
  ctx.fillStyle=SKIN_S;
  ctx.fillRect(bx+bw*0.55,legY-walkOff,bw*0.3,bh*0.2);
  ctx.fillStyle=sprite.shoe;
  ctx.fillRect(bx+bw*0.5,legY+bh*0.18-walkOff,bw*0.35,bh*0.12);

  // === 무기 ===
  if(sprite.weaponDraw) sprite.weaponDraw(ctx,bx,by,bw,bh,attacking);

  ctx.restore();
}

function roundRect(x,y,w,h,r){ctx.beginPath();ctx.moveTo(x+r,y);ctx.lineTo(x+w-r,y);ctx.quadraticCurveTo(x+w,y,x+w,y+r);ctx.lineTo(x+w,y+h-r);ctx.quadraticCurveTo(x+w,y+h,x+w-r,y+h);ctx.lineTo(x+r,y+h);ctx.quadraticCurveTo(x,y+h,x,y+h-r);ctx.lineTo(x,y+r);ctx.quadraticCurveTo(x,y,x+r,y);ctx.closePath();}

const SPRITES={
  warrior:{
    w:36,headW:32,headH:28,bodyW:20,bodyH:22,h:48,
    hair:'#c92a2a',hairDk:'#8b1a1a',shirt:'#4a6fa5',pants:'#495057',shoe:'#3d2b1f',
    eyeColor:'#1a1a2e',
    weaponDraw(c,bx,by,bw,bh,atk){
      // 검
      const wx=bx+bw+2,wy=by-5;
      c.fillStyle='#adb5bd';c.fillRect(wx,wy,3,22);
      c.fillStyle='#ffd43b';c.fillRect(wx-3,wy+16,9,3);
      c.fillStyle='#862e9c';c.fillRect(wx,wy+18,3,6);
      if(atk){c.fillStyle='rgba(255,215,0,0.3)';c.beginPath();c.arc(wx+1,wy+8,18,0,Math.PI*2);c.fill();}
    }
  },
  mage:{
    w:36,headW:32,headH:28,bodyW:20,bodyH:22,h:48,
    hair:'#7048e8',hairDk:'#4c2db3',shirt:'#845ef7',pants:'#5c7cfa',shoe:'#364fc7',
    eyeColor:'#4c1d95',
    weaponDraw(c,bx,by,bw,bh,atk){
      const wx=bx+bw+2,wy=by-10;
      c.fillStyle='#795548';c.fillRect(wx+1,wy,2,28);
      c.fillStyle='#e040fb';c.beginPath();c.arc(wx+2,wy-2,5,0,Math.PI*2);c.fill();
      c.fillStyle='rgba(224,64,251,0.25)';c.beginPath();c.arc(wx+2,wy-2,9,0,Math.PI*2);c.fill();
      if(atk){c.fillStyle='rgba(116,192,252,0.25)';c.beginPath();c.arc(bx+bw/2,by,28,0,Math.PI*2);c.fill();}
    }
  },
  archer:{
    w:36,headW:32,headH:28,bodyW:20,bodyH:22,h:48,
    hair:'#2b8a3e',hairDk:'#1a5c28',shirt:'#82c91e',pants:'#795548',shoe:'#4e342e',
    eyeColor:'#1b5e20',
    weaponDraw(c,bx,by,bw,bh,atk){
      const wx=bx+bw+4,wy=by-2;
      c.strokeStyle='#6d4c41';c.lineWidth=2;
      c.beginPath();c.arc(wx,wy+8,12,-0.9,0.9);c.stroke();
      c.strokeStyle='#bbb';c.lineWidth=1;
      c.beginPath();c.moveTo(wx+Math.cos(-0.9)*12,wy+8+Math.sin(-0.9)*12);
      c.lineTo(wx+Math.cos(0.9)*12,wy+8+Math.sin(0.9)*12);c.stroke();
      if(atk){c.fillStyle='#ffd43b';c.beginPath();c.moveTo(wx+14,wy+7);c.lineTo(wx+26,wy+8);c.lineTo(wx+14,wy+9);c.fill();}
    }
  }
};

// ===== 맵 데이터 =====
const MAPS={
  village:{
    name:'🏘️ 리프레 마을', bg1:'#87CEEB',bg2:'#228B22',groundColor:'#4a7c28',
    width:2000,height:800,
    platforms:[
      {x:0,y:760,w:2000,h:40}, // 바닥
      {x:300,y:620,w:150,h:16},{x:600,y:540,w:120,h:16},{x:900,y:620,w:180,h:16},
      {x:1200,y:560,w:150,h:16},{x:1500,y:640,w:160,h:16},
    ],
    enemies:[],
    npcs:[{x:500,y:720,name:'상점 NPC',icon:'🧙',dialog:'어서와! 뭐가 필요하니?',shop:true}],
    portals:[{x:1900,y:720,target:'forest',tx:60,ty:0,label:'헤네시스 숲 →'}],
    trees:[100,400,700,1000,1300,1600,1850],
    houses:[{x:150,y:660,w:120,h:100},{x:800,y:640,w:140,h:120}],
  },
  forest:{
    name:'🌲 헤네시스 숲', bg1:'#2d5016',bg2:'#1a3a0a',groundColor:'#3d6b1e',
    width:3000,height:900,
    platforms:[
      {x:0,y:860,w:3000,h:40},
      {x:200,y:700,w:180,h:16},{x:500,y:620,w:160,h:16},{x:800,y:700,w:200,h:16},
      {x:1100,y:580,w:150,h:16},{x:1400,y:660,w:180,h:16},{x:1700,y:560,w:160,h:16},
      {x:2000,y:700,w:200,h:16},{x:2300,y:620,w:150,h:16},{x:2600,y:700,w:180,h:16},
    ],
    enemies:['slime','slime','slime','mushroom','mushroom','bat'],
    npcs:[],
    portals:[
      {x:30,y:820,target:'village',tx:1800,ty:0,label:'← 마을'},
      {x:2920,y:820,target:'dungeon',tx:60,ty:0,label:'던전 →'},
    ],
    trees:[150,350,600,900,1200,1500,1800,2100,2400,2700],
    houses:[],
  },
  dungeon:{
    name:'🏚️ 어둠의 던전', bg1:'#1a1a2e',bg2:'#16213e',groundColor:'#2d2d44',
    width:2500,height:900,
    platforms:[
      {x:0,y:860,w:2500,h:40},
      {x:150,y:700,w:160,h:16},{x:400,y:600,w:140,h:16},{x:700,y:700,w:180,h:16},
      {x:1000,y:560,w:160,h:16},{x:1300,y:680,w:200,h:16},{x:1600,y:580,w:150,h:16},
      {x:1900,y:700,w:180,h:16},{x:2200,y:620,w:140,h:16},
    ],
    enemies:['zombie','zombie','vampire','vampire','vampire','dracula'],
    npcs:[],
    portals:[{x:30,y:820,target:'forest',tx:2850,ty:0,label:'← 숲'}],
    trees:[],
    houses:[],
  }
};

const ENEMY_TYPES={
  slime:{name:'초록 슬라임',w:24,h:20,hp:30,dmg:8,xp:5,gold:3,speed:1.2,color:'#51cf66',dark:'#2b8a3e'},
  mushroom:{name:'머쉬룸',w:22,h:28,hp:50,dmg:12,xp:8,gold:5,speed:1.5,color:'#ff922b',dark:'#e67700'},
  bat:{name:'박쥐',w:26,h:18,hp:35,dmg:10,xp:6,gold:4,speed:2.5,color:'#ae3ec9',dark:'#862e9c',flies:true},
  zombie:{name:'좀비',w:26,h:34,hp:100,dmg:20,xp:15,gold:10,speed:1,color:'#868e96',dark:'#495057'},
  vampire:{name:'뱀파이어',w:28,h:36,hp:150,dmg:28,xp:22,gold:15,speed:1.8,color:'#e64980',dark:'#c2255c'},
  dracula:{name:'⭐드라큘라',w:40,h:48,hp:500,dmg:40,xp:100,gold:80,speed:1.3,color:'#fa5252',dark:'#c92a2a',boss:true},
};

const ITEMS=[
  {id:'hpPot',name:'HP 포션',icon:'❤️',desc:'HP 50 회복',price:30,type:'consumable',effect:p=>{p.hp=Math.min(p.maxHp,p.hp+50);}},
  {id:'mpPot',name:'MP 포션',icon:'💙',desc:'MP 30 회복',price:25,type:'consumable',effect:p=>{p.mp=Math.min(p.maxMp,p.mp+30);}},
  {id:'strScroll',name:'힘의 두루마리',icon:'📜',desc:'STR +2',price:100,type:'consumable',effect:p=>{p.str+=2;}},
  {id:'sword1',name:'나무 검',icon:'🗡️',desc:'공격력 +10',price:80,type:'equip',slot:'weapon',atk:10},
  {id:'sword2',name:'강철 검',icon:'⚔️',desc:'공격력 +25',price:250,type:'equip',slot:'weapon',atk:25},
  {id:'armor1',name:'가죽 갑옷',icon:'🛡️',desc:'방어력 +5',price:120,type:'equip',slot:'armor',def:5},
];

// ===== 게임 상태 =====
let player,camera={x:0,y:0},currentMap,enemies=[],drops=[],particles=[],dmgTexts=[];
let gameRunning=false,gold=0;
let inventory=[],equipped={weapon:null,armor:null};
let statusOpen=false,invOpen=false,shopOpen=false;

// ===== 캐릭터 선택 (이미지 스프라이트) =====
const CHAR_IMAGES=[
  {file:'girl1.jpg',label:'🌸 소녀'},
  {file:'boy1.jpg',label:'🖤 소년'},
  {file:'girl2.jpg',label:'🐰 바니'},
  {file:'girl3.jpg',label:'👓 글래스'},
  {file:'boy2.jpg',label:'😎 선글'},
  {file:'boy3.jpg',label:'😈 데빌'},
  {file:'girl4.jpg',label:'🌷 핑크'},
  {file:'boy4.jpg',label:'💐 플라워'},
];
let selectedCharImg=null, charSpriteImg=null;

// 캐릭터 그리드 생성
(function initCharGrid(){
  const grid=document.getElementById('charGrid');
  if(!grid)return;
  CHAR_IMAGES.forEach((ch,i)=>{
    const card=document.createElement('div');
    card.className='char-card';
    card.innerHTML=`<img src="${ch.file}" alt="${ch.label}"><div class="char-label">${ch.label}</div>`;
    card.onclick=()=>{
      selectedCharImg=ch.file;
      // 이미지 로드 + 흰배경 제거
      const img=new Image();
      img.src=ch.file;
      img.onload=()=>{
        const oc=document.createElement('canvas');
        oc.width=img.width;oc.height=img.height;
        const ox=oc.getContext('2d');
        ox.drawImage(img,0,0);
        const id=ox.getImageData(0,0,oc.width,oc.height);
        const d=id.data;
        for(let i=0;i<d.length;i+=4){
          // 흰색/거의 흰색 → 투명
          if(d[i]>230&&d[i+1]>230&&d[i+2]>230) d[i+3]=0;
          // 밝은 회색도 반투명
          else if(d[i]>210&&d[i+1]>210&&d[i+2]>210) d[i+3]=Math.floor(d[i+3]*0.3);
        }
        ox.putImageData(id,0,0);
        charSpriteImg=oc;
      };
      // UI 전환
      document.getElementById('step1').classList.add('hidden');
      document.getElementById('step2').classList.remove('hidden');
    };
    grid.appendChild(card);
  });
})();

function selectChar(type){
  const stats={
    warrior:{str:8,dex:3,int_:2,hp:150,mp:30,atkRange:55,atkType:'melee'},
    mage:{str:2,dex:3,int_:8,hp:80,mp:100,atkRange:200,atkType:'magic'},
    archer:{str:3,dex:8,int_:2,hp:100,mp:50,atkRange:250,atkType:'ranged'},
  }[type];
  player={
    type,x:200,y:0,w:40,h:52,vx:0,vy:0,speed:4,jumpForce:-12,
    hp:stats.hp,maxHp:stats.hp,mp:stats.mp,maxMp:stats.mp,
    xp:0,xpToNext:20,level:1,
    str:stats.str,dex:stats.dex,int_:stats.int_,statPoints:0,
    atkRange:stats.atkRange,atkType:stats.atkType,atkCooldown:0,atkSpeed:400,
    attacking:false,atkTimer:0,facing:1,grounded:false,invincible:0,
    critChance:0.05+stats.dex*0.01,sprite:SPRITES[type],
  };
  currentMap='village';
  loadMap();
  document.getElementById('charSelect').classList.add('hidden');
  gameRunning=true;
  lastTime=performance.now();
  requestAnimationFrame(gameLoop);
}

function loadMap(){
  const map=MAPS[currentMap];
  enemies=[];
  if(map.enemies.length){
    map.enemies.forEach(type=>{
      const et=ENEMY_TYPES[type];
      const x=200+Math.random()*(map.width-400);
      const y=map.platforms[0].y-et.h;
      enemies.push({...et,x,y,vx:0,vy:0,grounded:false,flash:0,dir:1,patrol:Math.random()*100,
        maxHp:et.hp,spawnX:x,type,dead:false,knockback:0});
    });
  }
  drops=[];
  // 플레이어 위치
  if(player.y===0) player.y=map.platforms[0].y-player.h;
}

// ===== 공격 =====
function attack(){
  if(player.atkCooldown>0||player.attacking)return;
  player.attacking=true;player.atkTimer=200;player.atkCooldown=player.atkSpeed;
  const totalAtk=player.str*2+(equipped.weapon?equipped.weapon.atk:0);
  const range=player.atkRange;

  if(player.atkType==='magic'&&player.mp>=5) player.mp-=5;
  if(player.atkType==='ranged'){
    // 화살 발사
    drops.push({type:'arrow',x:player.x+player.w/2,y:player.y+player.h/2,
      vx:player.facing*10,vy:0,dmg:totalAtk+player.dex*2,life:40,size:4});
    return;
  }

  enemies.forEach(e=>{
    if(e.dead)return;
    const dx=Math.abs(e.x+e.w/2-(player.x+player.w/2+player.facing*range/2));
    const dy=Math.abs(e.y+e.h/2-(player.y+player.h/2));
    if(dx<range/2+e.w/2&&dy<Math.max(e.h,player.h)){
      const crit=Math.random()<player.critChance;
      let dmg=totalAtk+(player.atkType==='magic'?player.int_*3:0);
      if(crit) dmg=Math.floor(dmg*1.8);
      dmg=Math.max(1,dmg-(0)); // 적 방어력 없음
      e.hp-=dmg;e.flash=0.12;e.knockback=player.facing*6;
      dmgTexts.push({x:e.x+e.w/2,y:e.y-8,text:crit?dmg+'!':String(dmg),color:crit?'#ffd43b':'#fff',size:crit?18:13,life:35});
      spawnP(e.x+e.w/2,e.y+e.h/2,e.color,4);
      if(e.hp<=0) killEnemy(e);
    }
  });
}

function useSkill(){
  if(player.mp<15)return;
  player.mp-=15;
  // 광역 스킬
  const range=120;
  const totalAtk=player.str*2+player.int_*4+(equipped.weapon?equipped.weapon.atk:0);
  enemies.forEach(e=>{
    if(e.dead)return;
    if(Math.abs(e.x+e.w/2-player.x-player.w/2)<range&&Math.abs(e.y+e.h/2-player.y-player.h/2)<range){
      const dmg=Math.floor(totalAtk*1.5);
      e.hp-=dmg;e.flash=0.15;
      dmgTexts.push({x:e.x+e.w/2,y:e.y-8,text:dmg+'✨',color:'#74c0fc',size:16,life:35});
      spawnP(e.x+e.w/2,e.y+e.h/2,'#74c0fc',6);
      if(e.hp<=0) killEnemy(e);
    }
  });
  // 스킬 이펙트
  for(let i=0;i<12;i++){
    const a=Math.PI*2*i/12;
    particles.push({x:player.x+player.w/2+Math.cos(a)*40,y:player.y+player.h/2+Math.sin(a)*40,
      vx:Math.cos(a)*3,vy:Math.sin(a)*3,size:4,color:'#74c0fc',life:20,maxLife:20});
  }
}

function killEnemy(e){
  e.dead=true;player.xp+=e.xp;gold+=e.gold;
  // 아이템 드롭
  if(Math.random()<0.25){
    const item=ITEMS[Math.floor(Math.random()*3)]; // 소비 아이템만 드롭
    drops.push({type:'item',x:e.x,y:e.y,vy:-3,item:{...item},life:400,size:10});
  }
  spawnP(e.x+e.w/2,e.y+e.h/2,e.color,12);
  // 경험치 체크
  if(player.xp>=player.xpToNext) levelUp();
  // 리스폰 타이머
  setTimeout(()=>{
    if(currentMap!=='village'){
      const et=ENEMY_TYPES[e.type];
      const map=MAPS[currentMap];
      const nx=200+Math.random()*(map.width-400);
      enemies.push({...et,x:nx,y:map.platforms[0].y-et.h,vx:0,vy:0,grounded:false,flash:0,
        dir:1,patrol:Math.random()*100,maxHp:et.hp,hp:et.hp,spawnX:nx,type:e.type,dead:false,knockback:0});
    }
  },5000);
}

function levelUp(){
  player.xp-=player.xpToNext;player.xpToNext=Math.floor(player.xpToNext*1.4);
  player.level++;player.statPoints+=5;
  player.maxHp+=10;player.hp=player.maxHp;player.maxMp+=5;player.mp=player.maxMp;
  document.getElementById('levelUpNotice').classList.remove('hidden');
  setTimeout(()=>document.getElementById('levelUpNotice').classList.add('hidden'),1500);
  spawnP(player.x+player.w/2,player.y,'#ffd43b',20);
}

// ===== 업데이트 =====
function update(dt){
  const map=MAPS[currentMap];
  // 이동
  player.vx=0;
  if(keys['a']||keys['ArrowLeft']){player.vx=-player.speed;player.facing=-1;}
  if(keys['d']||keys['ArrowRight']){player.vx=player.speed;player.facing=1;}
  if((keys['w']||keys['ArrowUp']||keys[' '])&&player.grounded){player.vy=player.jumpForce;player.grounded=false;}
  if(keys['z']||keys['Z']) attack();
  if(keys['x']||keys['X']) useSkill();

  // 포탈
  if(keys['ArrowUp']){
    map.portals.forEach(p=>{
      if(Math.abs(player.x-p.x)<40&&Math.abs(player.y+player.h-map.platforms[0].y)<20){
        currentMap=p.target;player.x=p.tx;player.y=0;loadMap();
      }
    });
  }

  // 물리
  player.vy+=G;player.x+=player.vx;player.y+=player.vy;
  player.x=Math.max(0,Math.min(map.width-player.w,player.x));
  player.grounded=false;
  map.platforms.forEach(p=>{
    if(player.x+player.w>p.x&&player.x<p.x+p.w&&player.y+player.h>p.y&&player.y+player.h<p.y+p.h+player.vy+2&&player.vy>=0){
      player.y=p.y-player.h;player.vy=0;player.grounded=true;
    }
  });
  if(player.invincible>0)player.invincible-=dt;
  if(player.atkCooldown>0)player.atkCooldown-=dt*1000;
  if(player.atkTimer>0)player.atkTimer-=dt*1000;else player.attacking=false;

  // MP 자연 회복
  player.mp=Math.min(player.maxMp,player.mp+dt*2);

  // 적 AI
  enemies.forEach(e=>{
    if(e.dead)return;
    if(e.knockback){e.x+=e.knockback;e.knockback*=0.85;if(Math.abs(e.knockback)<0.3)e.knockback=0;}
    const dist=player.x-e.x;
    if(Math.abs(dist)<300){e.dir=dist>0?1:-1;e.vx=e.dir*e.speed;}
    else{e.patrol+=dt;e.vx=Math.sin(e.patrol)*e.speed*0.5;e.dir=e.vx>0?1:-1;}
    if(e.flies){e.y+=Math.sin(e.patrol*3)*0.8;}
    else{e.vy+=G;e.y+=e.vy;e.grounded=false;
      map.platforms.forEach(p=>{if(e.x+e.w>p.x&&e.x<p.x+p.w&&e.y+e.h>p.y&&e.y+e.h<p.y+p.h+2&&e.vy>=0){e.y=p.y-e.h;e.vy=0;e.grounded=true;}});
    }
    e.x+=e.vx;
    e.x=Math.max(0,Math.min(map.width-e.w,e.x));
    if(e.flash>0)e.flash-=dt;
    // 플레이어 충돌
    if(player.invincible<=0&&Math.abs(e.x+e.w/2-player.x-player.w/2)<(e.w/2+player.w/2)&&
       Math.abs(e.y+e.h/2-player.y-player.h/2)<(e.h/2+player.h/2)){
      const def=equipped.armor?equipped.armor.def:0;
      const dmg=Math.max(1,e.dmg-def);
      player.hp-=dmg;player.invincible=1;
      dmgTexts.push({x:player.x+player.w/2,y:player.y-8,text:'-'+dmg,color:'#ff6b6b',size:14,life:35});
      spawnP(player.x+player.w/2,player.y+player.h/2,'#ff6b6b',5);
      if(player.hp<=0){gameRunning=false;showGameOver();}
    }
  });

  // 드롭/화살
  drops.forEach(d=>{
    if(d.type==='arrow'){d.x+=d.vx;d.life--;
      enemies.forEach(e=>{if(!e.dead&&Math.abs(d.x-e.x-e.w/2)<e.w/2&&Math.abs(d.y-e.y-e.h/2)<e.h/2){
        e.hp-=d.dmg;e.flash=0.1;d.life=0;dmgTexts.push({x:e.x+e.w/2,y:e.y-8,text:String(d.dmg),color:'#fff',size:13,life:35});
        if(e.hp<=0)killEnemy(e);}});
    }else if(d.type==='item'){
      d.vy+=0.2;d.y+=d.vy;d.life--;
      const map2=MAPS[currentMap];
      map2.platforms.forEach(p=>{if(d.x>p.x&&d.x<p.x+p.w&&d.y+d.size>p.y&&d.vy>0){d.y=p.y-d.size;d.vy=0;}});
      if(Math.hypot(d.x-player.x-player.w/2,d.y-player.y-player.h/2)<35){
        addToInventory(d.item);d.dead=true;
      }
    }
  });
  drops=drops.filter(d=>!d.dead&&d.life>0);

  // 텍스트/파티클
  dmgTexts.forEach(d=>{d.y-=1;d.life--;});dmgTexts=dmgTexts.filter(d=>d.life>0);
  particles.forEach(p=>{p.x+=p.vx;p.y+=p.vy;p.vy+=0.08;p.life--;});particles=particles.filter(p=>p.life>0);

  // 카메라
  camera.x+=(player.x-canvas.width/2-camera.x)*0.1;
  camera.y+=(player.y-canvas.height/2+80-camera.y)*0.1;
  camera.x=Math.max(0,Math.min(map.width-canvas.width,camera.x));
  camera.y=Math.max(0,Math.min(map.height-canvas.height,camera.y));
}

// ===== 렌더링 =====
function drawDot(x,y,s,dots,sprite,dir){
  const colorMap={
    'H':sprite.hair,'h':sprite.hairDk||sprite.hair,
    'S':SKIN,'s':SKIN_S,
    'C':sprite.shirt,'c':sprite.shirtDk||sprite.shirt,
    'P':sprite.pants,'p':sprite.pantsDk||sprite.pants,
    'B':sprite.shoe,
    'W':'#fff','w':'#aee6ff','K':'#222',
    'R':BLUSH,'m':SKIN_S,'Y':'#ffd43b',
  };
  const d=dir<0?dots.map(r=>[...r].reverse()):dots;
  d.forEach((row,r)=>row.forEach((c,col)=>{
    if(c&&c!=='0'&&colorMap[c]){ctx.fillStyle=colorMap[c];ctx.fillRect(x+col*s,y+r*s,s,s);}
  }));
}

function drawPlayerFull(px,py){
  if(charSpriteImg){
    const bob=Math.abs(player.vx)>0.5?Math.sin(Date.now()/100)*2:0;
    const sw=player.w+8, sh=player.h+8;
    ctx.save();
    if(player.facing<0){
      ctx.translate(px+player.w/2,0);
      ctx.scale(-1,1);
      ctx.drawImage(charSpriteImg,-sw/2,py+bob-4,sw,sh);
    }else{
      ctx.drawImage(charSpriteImg,px-4,py+bob-4,sw,sh);
    }
    ctx.restore();
    // 공격 이펙트
    if(player.attacking){
      ctx.strokeStyle='rgba(255,215,0,0.5)';ctx.lineWidth=2;
      ctx.beginPath();ctx.arc(px+player.w/2+player.facing*25,py+player.h*0.4,player.atkRange*0.3,0,Math.PI*2);ctx.stroke();
    }
  }else{
    drawMapleChar(px,py,player.sprite,player.facing,Math.abs(player.vx)>0.5,player.attacking,player.grounded);
  }
}

function render(){
  const map=MAPS[currentMap];
  // 배경
  const g=ctx.createLinearGradient(0,0,0,canvas.height);
  g.addColorStop(0,map.bg1);g.addColorStop(1,map.bg2);
  ctx.fillStyle=g;ctx.fillRect(0,0,canvas.width,canvas.height);

  ctx.save();ctx.translate(-camera.x,-camera.y);

  // 배경 장식
  map.trees.forEach(tx=>{
    ctx.fillStyle='#2d5016';ctx.beginPath();ctx.arc(tx,map.platforms[0].y-50,35,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.arc(tx,map.platforms[0].y-80,28,0,Math.PI*2);ctx.fill();
    ctx.fillStyle='#5c3a1e';ctx.fillRect(tx-6,map.platforms[0].y-30,12,30);
  });
  map.houses.forEach(h=>{
    ctx.fillStyle='#8B7355';ctx.fillRect(h.x,h.y,h.w,h.h);
    ctx.fillStyle='#A0522D';ctx.beginPath();ctx.moveTo(h.x-10,h.y);ctx.lineTo(h.x+h.w/2,h.y-40);ctx.lineTo(h.x+h.w+10,h.y);ctx.fill();
    ctx.fillStyle='#4a3728';ctx.fillRect(h.x+h.w/2-15,h.y+h.h-40,30,40);
    ctx.fillStyle='#87CEEB';ctx.fillRect(h.x+15,h.y+20,25,25);ctx.fillRect(h.x+h.w-40,h.y+20,25,25);
  });

  // 플랫폼
  map.platforms.forEach((p,i)=>{
    ctx.fillStyle=map.groundColor;ctx.fillRect(p.x,p.y,p.w,p.h);
    if(i>0){ctx.fillStyle='#5c9e2f';ctx.fillRect(p.x,p.y,p.w,3);}
    else{ctx.fillStyle='#5c9e2f';ctx.fillRect(p.x,p.y,p.w,4);}
  });

  // 포탈
  map.portals.forEach(p=>{
    ctx.fillStyle=`rgba(51,154,240,${0.3+Math.sin(Date.now()/300)*0.2})`;
    ctx.beginPath();ctx.ellipse(p.x+15,p.y+15,18,25,0,0,Math.PI*2);ctx.fill();
    ctx.strokeStyle='#74c0fc';ctx.lineWidth=2;ctx.beginPath();ctx.ellipse(p.x+15,p.y+15,18,25,0,0,Math.PI*2);ctx.stroke();
    ctx.fillStyle='#fff';ctx.font='10px sans-serif';ctx.textAlign='center';ctx.fillText(p.label,p.x+15,p.y-8);
  });

  // NPC
  map.npcs.forEach(n=>{
    ctx.font='28px serif';ctx.textAlign='center';ctx.fillText(n.icon,n.x,n.y);
    ctx.fillStyle='#ffd43b';ctx.font='bold 11px sans-serif';ctx.fillText(n.name,n.x,n.y-20);
  });

  // 드롭
  drops.forEach(d=>{
    if(d.type==='arrow'){ctx.fillStyle='#ffd43b';ctx.fillRect(d.x-8,d.y-1,16,2);ctx.fillRect(d.x+(d.vx>0?8:-8),d.y-3,2,6);}
    else if(d.type==='item'){ctx.font='18px serif';ctx.textAlign='center';ctx.fillText(d.item.icon,d.x,d.y+5);
      ctx.fillStyle='rgba(255,255,255,0.15)';ctx.beginPath();ctx.arc(d.x,d.y,12,0,Math.PI*2);ctx.fill();}
  });

  // 적
  enemies.forEach(e=>{
    if(e.dead)return;
    const f=e.flash>0;
    const s=e.boss?4:3;
    // 간단 도트 몬스터
    ctx.fillStyle=f?'#fff':e.color;
    ctx.beginPath();ctx.roundRect(e.x,e.y,e.w,e.h,e.boss?6:4);ctx.fill();
    ctx.fillStyle=f?'#eee':e.dark;
    ctx.beginPath();ctx.roundRect(e.x+e.w*0.1,e.y+e.h*0.55,e.w*0.8,e.h*0.3,3);ctx.fill();
    // 눈
    const ew=e.w*0.16;
    ctx.fillStyle='#fff';
    ctx.beginPath();ctx.arc(e.x+e.w*0.3,e.y+e.h*0.3,ew+1,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.arc(e.x+e.w*0.7,e.y+e.h*0.3,ew+1,0,Math.PI*2);ctx.fill();
    ctx.fillStyle='#111';
    ctx.beginPath();ctx.arc(e.x+e.w*0.3+e.dir*1.5,e.y+e.h*0.3,ew*0.6,0,Math.PI*2);ctx.fill();
    ctx.beginPath();ctx.arc(e.x+e.w*0.7+e.dir*1.5,e.y+e.h*0.3,ew*0.6,0,Math.PI*2);ctx.fill();
    // HP바
    if(e.boss||e.hp<e.maxHp){
      ctx.fillStyle='rgba(0,0,0,0.5)';ctx.fillRect(e.x,e.y-8,e.w,4);
      ctx.fillStyle='#fa5252';ctx.fillRect(e.x,e.y-8,e.w*(e.hp/e.maxHp),4);
    }
    if(e.boss){ctx.fillStyle='#ffd43b';ctx.font='bold 10px sans-serif';ctx.textAlign='center';ctx.fillText(e.name,e.x+e.w/2,e.y-12);}
  });

  // 플레이어
  ctx.globalAlpha=player.invincible>0?0.5:1;
  drawPlayerFull(player.x,player.y);
  ctx.globalAlpha=1;

  // 텍스트
  dmgTexts.forEach(d=>{ctx.globalAlpha=d.life/35;ctx.fillStyle=d.color;ctx.font=`bold ${d.size}px sans-serif`;ctx.textAlign='center';ctx.fillText(d.text,d.x,d.y);});
  ctx.globalAlpha=1;
  // 파티클
  particles.forEach(p=>{ctx.fillStyle=p.color;ctx.globalAlpha=p.life/p.maxLife;ctx.beginPath();ctx.arc(p.x,p.y,p.size,0,Math.PI*2);ctx.fill();});
  ctx.globalAlpha=1;

  ctx.restore();

  // NPC 상호작용 힌트
  const map2=MAPS[currentMap];
  map2.npcs.forEach(n=>{
    if(Math.abs(player.x-n.x)<50){
      ctx.fillStyle='rgba(255,255,255,0.8)';ctx.font='12px sans-serif';ctx.textAlign='center';
      ctx.fillText('↑ 대화하기',canvas.width/2,canvas.height-60);
    }
  });
  // 포탈 힌트
  map2.portals.forEach(p=>{
    if(Math.abs(player.x-p.x)<40){
      ctx.fillStyle='rgba(116,192,252,0.9)';ctx.font='12px sans-serif';ctx.textAlign='center';
      ctx.fillText('↑ '+p.label,canvas.width/2,canvas.height-60);
    }
  });

  // UI 업데이트
  document.getElementById('hpFill').style.width=(player.hp/player.maxHp*100)+'%';
  document.getElementById('hpText').textContent=`HP ${Math.ceil(player.hp)}/${player.maxHp}`;
  document.getElementById('mpFill').style.width=(player.mp/player.maxMp*100)+'%';
  document.getElementById('mpText').textContent=`MP ${Math.ceil(player.mp)}/${player.maxMp}`;
  document.getElementById('xpFill').style.width=(player.xp/player.xpToNext*100)+'%';
  document.getElementById('xpText').textContent=`EXP ${player.xp}/${player.xpToNext}`;
  document.getElementById('level').textContent=`Lv.${player.level}`;
  document.getElementById('mapName').textContent=MAPS[currentMap].name;
  document.getElementById('goldText').textContent=`💰 ${gold}`;

  // 미니맵
  renderMinimap();
}

function renderMinimap(){
  const mm=document.getElementById('minimap');
  const map=MAPS[currentMap];
  const mc=mm.getContext?.('2d');
  if(!mc){
    const c=document.createElement('canvas');c.width=140;c.height=80;c.id='minimapCanvas';
    mm.appendChild(c);
  }
  const c=mm.querySelector('canvas')||mm;
  if(!c.getContext)return;
  const cx=c.getContext('2d');
  cx.fillStyle='rgba(0,0,0,0.3)';cx.fillRect(0,0,140,80);
  const sx=140/map.width,sy=80/map.height;
  map.platforms.forEach(p=>{cx.fillStyle='rgba(255,255,255,0.3)';cx.fillRect(p.x*sx,p.y*sy,Math.max(2,p.w*sx),Math.max(1,p.h*sy));});
  cx.fillStyle='#74c0fc';cx.fillRect(player.x*sx-1,player.y*sy-1,3,3);
  enemies.forEach(e=>{if(!e.dead){cx.fillStyle=e.color;cx.fillRect(e.x*sx,e.y*sy,2,2);}});
  map.portals.forEach(p=>{cx.fillStyle='#339af0';cx.fillRect(p.x*sx,p.y*sy,3,3);});
}

function spawnP(x,y,color,n){for(let i=0;i<n;i++)particles.push({x,y,vx:(Math.random()-0.5)*5,vy:(Math.random()-0.5)*5-2,size:2+Math.random()*3,color,life:18+Math.random()*10,maxLife:28});}

// ===== 인벤토리 =====
function addToInventory(item){
  const existing=inventory.find(i=>i.id===item.id);
  if(existing) existing.qty++;
  else inventory.push({...item,qty:1});
}

function useItem(idx){
  const item=inventory[idx];if(!item)return;
  if(item.type==='consumable'){item.effect(player);item.qty--;if(item.qty<=0)inventory.splice(idx,1);}
  else if(item.type==='equip'){equipped[item.slot]=item;inventory.splice(idx,1);}
  renderInventory();
}

function renderInventory(){
  const grid=document.getElementById('inventoryGrid');grid.innerHTML='';
  for(let i=0;i<24;i++){
    const slot=document.createElement('div');slot.className='inv-slot';
    if(inventory[i]){
      slot.innerHTML=`${inventory[i].icon}${inventory[i].qty>1?`<span class="inv-qty">${inventory[i].qty}</span>`:''}`;
      slot.title=`${inventory[i].name}: ${inventory[i].desc}`;
      slot.onclick=()=>useItem(i);
    }
    grid.appendChild(slot);
  }
}

function toggleInventory(){invOpen=!invOpen;document.getElementById('inventoryWindow').classList.toggle('hidden');if(invOpen)renderInventory();}
function toggleStatus(){
  statusOpen=!statusOpen;document.getElementById('statusWindow').classList.toggle('hidden');
  if(statusOpen) renderStatus();
}
function toggleShop(){shopOpen=!shopOpen;document.getElementById('shopWindow').classList.toggle('hidden');if(shopOpen)renderShop();}

function renderStatus(){
  const c=document.getElementById('statusContent');
  const sp=player.statPoints;
  const btn=sp>0?`<button onclick="addStat('STR')">+</button>`:'';
  c.innerHTML=`
    <div class="stat-row"><span>직업: ${player.type==='warrior'?'⚔️전사':player.type==='mage'?'🔮마법사':'🏹궁수'}</span><span>Lv.${player.level}</span></div>
    <div class="stat-row"><span>STR: ${player.str}</span>${sp>0?`<button onclick="addStat('str')">+</button>`:''}</div>
    <div class="stat-row"><span>DEX: ${player.dex}</span>${sp>0?`<button onclick="addStat('dex')">+</button>`:''}</div>
    <div class="stat-row"><span>INT: ${player.int_}</span>${sp>0?`<button onclick="addStat('int_')">+</button>`:''}</div>
    <div class="stat-row"><span>HP: ${Math.ceil(player.hp)}/${player.maxHp}</span></div>
    <div class="stat-row"><span>MP: ${Math.ceil(player.mp)}/${player.maxMp}</span></div>
    <div class="stat-row"><span>공격력: ${player.str*2+(equipped.weapon?equipped.weapon.atk:0)}</span></div>
    <div class="stat-row"><span>방어력: ${equipped.armor?equipped.armor.def:0}</span></div>
    <div class="stat-row"><span>크리티컬: ${(player.critChance*100).toFixed(1)}%</span></div>
    <div class="stat-row"><span>스탯 포인트: ${sp}</span></div>
    <div class="stat-row"><span>무기: ${equipped.weapon?equipped.weapon.icon+equipped.weapon.name:'없음'}</span></div>
    <div class="stat-row"><span>방어구: ${equipped.armor?equipped.armor.icon+equipped.armor.name:'없음'}</span></div>
  `;
}
window.addStat=function(stat){if(player.statPoints>0){player[stat]++;player.statPoints--;
  if(stat==='str')player.critChance=Math.min(0.8,player.critChance);
  if(stat==='dex')player.critChance=0.05+player.dex*0.01;
  renderStatus();}};

function renderShop(){
  const c=document.getElementById('shopContent');
  c.innerHTML=ITEMS.map((item,i)=>`
    <div class="shop-item" onclick="buyItem(${i})">
      <div class="shop-item-icon">${item.icon}</div>
      <div class="shop-item-info"><div class="shop-item-name">${item.name}</div><div class="shop-item-desc">${item.desc}</div></div>
      <div class="shop-item-price">💰${item.price}</div>
    </div>
  `).join('');
}
window.buyItem=function(i){
  const item=ITEMS[i];
  if(gold>=item.price){gold-=item.price;addToInventory({...item});}
};

function showGameOver(){
  document.getElementById('finalScore').textContent=`Lv.${player.level} · 💰${gold}`;
  document.getElementById('gameOver').classList.remove('hidden');
}

// ===== 키보드 단축키 =====
window.addEventListener('keydown',e=>{
  if(e.key==='i'||e.key==='I') toggleInventory();
  if(e.key==='s'||e.key==='S') toggleStatus();
  // NPC 대화
  if(e.key==='ArrowUp'){
    const map=MAPS[currentMap];
    map.npcs.forEach(n=>{
      if(Math.abs(player.x-n.x)<50&&n.shop) toggleShop();
    });
  }
});

// ===== 게임 루프 =====
let lastTime=0;
function gameLoop(now){
  if(!gameRunning)return;
  const dt=Math.min((now-lastTime)/1000,0.05);lastTime=now;
  if(!statusOpen&&!invOpen&&!shopOpen) update(dt);
  render();
  requestAnimationFrame(gameLoop);
}
