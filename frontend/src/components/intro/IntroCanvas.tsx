import { useEffect, useRef } from 'react';

export default function IntroCanvas() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const wrapRef = useRef<HTMLDivElement>(null);
  const animRef = useRef<number>(0);
  const stoppedRef = useRef(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    const wrap = wrapRef.current;
    if (!canvas || !wrap) return;
    stoppedRef.current = false;
    const ctx = canvas.getContext('2d')!;

    let W = 0, H = 0;
    function resize() {
      const rect = wrap!.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;
      canvas!.width = rect.width * dpr;
      canvas!.height = rect.height * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      W = rect.width; H = rect.height;
    }
    setTimeout(resize, 100);
    window.addEventListener('resize', () => setTimeout(resize, 50));

    let elapsed = 0, lastTime = performance.now();
    const CYCLE = 15;

    function getAlpha(idx: number, t: number) {
      const phase = (t / CYCLE * Math.PI * 2) - (idx * Math.PI * 2 / 3);
      const raw = (Math.sin(phase) + 1) / 2;
      return raw * raw * (3 - 2 * raw);
    }

    // 로켓
    const rColors = ['#6c5ce7','#e64980','#339af0','#0ca678','#f76707','#ae3ec9'];
    const rockets = Array.from({length:5}, (_, i) => ({
      x: Math.random()*960, y: Math.random()*400,
      vx: (Math.random()-0.5)*1, vy: -Math.random()*1.2-0.6,
      size: 10+Math.random()*6, color: rColors[i%6]
    }));
    const stars = Array.from({length:30}, () => ({
      x: Math.random()*960, y: Math.random()*400,
      size: Math.random()*1.5+0.5, speed: Math.random()*0.3+0.1, tw: Math.random()*Math.PI*2
    }));
    const bullets: {x:number;y:number;vy:number;color:string;life:number}[] = [];

    function drawRockets(a: number) {
      if (a < 0.01) return;
      ctx.globalAlpha = a * 0.5;
      stars.forEach(s => {
        s.tw += 0.02;
        ctx.fillStyle = `rgba(108,92,231,${0.1+Math.abs(Math.sin(s.tw))*0.2})`;
        ctx.beginPath(); ctx.arc(s.x, s.y, s.size, 0, Math.PI*2); ctx.fill();
        s.y += s.speed; if (s.y > H) { s.y = -5; s.x = Math.random()*W; }
      });
      rockets.forEach(r => {
        ctx.save(); ctx.translate(r.x, r.y); ctx.globalAlpha = Math.min(1, a*1.3);
        ctx.fillStyle = r.color;
        ctx.beginPath(); ctx.moveTo(0,-r.size); ctx.lineTo(-r.size*0.4,r.size*0.5);
        ctx.lineTo(r.size*0.4,r.size*0.5); ctx.closePath(); ctx.fill();
        ctx.fillStyle = '#ffd43b'; ctx.globalAlpha = a*(0.3+Math.random()*0.2);
        ctx.beginPath(); ctx.moveTo(-r.size*0.2,r.size*0.5);
        ctx.lineTo(0,r.size*0.5+r.size*0.3+Math.random()*5);
        ctx.lineTo(r.size*0.2,r.size*0.5); ctx.closePath(); ctx.fill();
        ctx.restore();
        r.x += r.vx; r.y += r.vy;
        if (r.y < -40) { r.y = H+40; r.x = Math.random()*W; }
        if (r.x < -30) r.x = W+30; if (r.x > W+30) r.x = -30;
        if (Math.random() < 0.015) bullets.push({x:r.x,y:r.y-r.size,vy:-5,color:r.color,life:1});
      });
      bullets.forEach(b => {
        b.life -= 0.02; ctx.fillStyle = b.color; ctx.globalAlpha = a*b.life*0.4;
        ctx.fillRect(b.x-1, b.y, 2, 7); b.y += b.vy;
      });
      while (bullets.length > 30) bullets.shift();
    }

    // 테트리스
    const tColors = ['#6c5ce7','#ae3ec9','#e64980','#0ca678','#e67700','#339af0','#f76707'];
    const BS = 22;
    const streams = [{blocks:[] as any[], x:0, cols:3}, {blocks:[] as any[], x:0, cols:3}];
    function spawnBlock(s: typeof streams[0]) {
      s.blocks.push({col:Math.floor(Math.random()*s.cols), w:1+Math.floor(Math.random()*2),
        y:-BS*2, speed:0.6+Math.random(), color:tColors[Math.floor(Math.random()*tColors.length)]});
    }
    streams.forEach(s => { for(let i=0;i<8;i++) spawnBlock(s); });

    function drawTetris(a: number) {
      if (a < 0.01) return;
      streams[0].x = 0; streams[1].x = W - 3*BS;
      streams.forEach(s => {
        s.blocks.forEach(b => {
          b.y += b.speed; ctx.fillStyle = b.color; ctx.globalAlpha = a*0.35;
          for(let i=0;i<b.w;i++) ctx.fillRect(s.x+(b.col+i)*BS+1, b.y+1, BS-2, BS-2);
          if (b.y > H+BS) b.y = -BS*3;
        });
        if (Math.random() < 0.008) spawnBlock(s);
      });
    }

    // 픽셀 캐릭터
    const pColors = ['#6c5ce7','#e64980','#0ca678','#e67700','#339af0'];
    const chars = Array.from({length:4}, (_, i) => ({
      x:100+Math.random()*800, vx:(Math.random()-0.5)*2, size:12+Math.random()*5,
      color:pColors[i%5], jumpY:0, jumping:false, jumpVY:0, frame:Math.random()*100
    }));
    const coins = Array.from({length:8}, () => ({
      x:80+Math.random()*840, size:6, rot:Math.random()*Math.PI*2
    }));

    function drawPixels(a: number) {
      if (a < 0.01) return;
      const groundY = H - 5;
      coins.forEach(c => {
        const cy = groundY - 20 - Math.sin(c.rot*0.5)*8; c.rot += 0.025;
        ctx.fillStyle = '#ffd43b'; ctx.globalAlpha = Math.min(1, a*1.3);
        ctx.save(); ctx.translate(c.x, cy); ctx.scale(Math.abs(Math.cos(c.rot)),1);
        ctx.fillRect(-c.size/2,-c.size/2,c.size,c.size); ctx.restore();
      });
      chars.forEach(ch => {
        ch.frame++; ch.x += ch.vx;
        if (!ch.jumping && Math.random()<0.008) {ch.jumping=true; ch.jumpVY=-4.5;}
        if (ch.jumping) {ch.jumpY+=ch.jumpVY; ch.jumpVY+=0.18; if(ch.jumpY>=0){ch.jumpY=0;ch.jumping=false;}}
        if (ch.x<20){ch.x=20;ch.vx*=-1;} if(ch.x>W-20){ch.x=W-20;ch.vx*=-1;}
        ctx.fillStyle=ch.color; ctx.globalAlpha=Math.min(1,a*1.5);
        const y=groundY+ch.jumpY;
        ctx.fillRect(ch.x-ch.size/2, y-ch.size, ch.size, ch.size);
        ctx.fillStyle='white'; ctx.globalAlpha=Math.min(1,a*1.8);
        ctx.fillRect(ch.x-ch.size*0.3, y-ch.size*0.8, ch.size*0.18, ch.size*0.18);
        ctx.fillRect(ch.x+ch.size*0.12, y-ch.size*0.8, ch.size*0.18, ch.size*0.18);
        const leg=Math.sin(ch.frame*0.15)*ch.size*0.3;
        ctx.fillStyle=ch.color; ctx.globalAlpha=Math.min(1,a*1.2);
        ctx.fillRect(ch.x-ch.size*0.35,y,ch.size*0.22,ch.size*0.35+leg);
        ctx.fillRect(ch.x+ch.size*0.13,y,ch.size*0.22,ch.size*0.35-leg);
      });
    }

    // 메인 루프
    function loop(now: number) {
      const dt = Math.min((now-lastTime)/1000, 0.05);
      lastTime = now; elapsed += dt;
      ctx.clearRect(0, 0, W, H);
      drawRockets(getAlpha(0, elapsed));
      drawTetris(getAlpha(1, elapsed));
      drawPixels(getAlpha(2, elapsed));
      ctx.globalAlpha = 1;
      if (!stoppedRef.current) animRef.current = requestAnimationFrame(loop);
    }
    animRef.current = requestAnimationFrame(loop);

    return () => { stoppedRef.current = true; cancelAnimationFrame(animRef.current); };
  }, []);

  return (
    <div ref={wrapRef}
      className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2
                 w-[min(1000px,92vw)] h-[min(420px,55vh)] rounded-[20px] overflow-hidden"
      style={{ animation: 'canvasIn 1.5s cubic-bezier(0.16,1,0.3,1) 0.5s both' }}>
      {/* 소프트 마스크 */}
      <div className="absolute inset-x-0 top-0 h-[60px] bg-gradient-to-b from-bg-secondary to-transparent z-[1] rounded-t-[20px]" />
      <div className="absolute inset-x-0 bottom-0 h-[60px] bg-gradient-to-t from-bg-secondary to-transparent z-[1] rounded-b-[20px]" />
      <div className="absolute inset-y-0 left-0 w-[50px] bg-gradient-to-r from-bg-secondary to-transparent z-[1]" />
      <div className="absolute inset-y-0 right-0 w-[50px] bg-gradient-to-l from-bg-secondary to-transparent z-[1]" />
      <canvas ref={canvasRef} className="w-full h-full" />
    </div>
  );
}
