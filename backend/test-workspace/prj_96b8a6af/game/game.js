// === 게임 로직 (백엔드팀) ===
// 게임 로직 (백엔드팀 담당)
class Game {
    constructor() {
        this.state = 'TITLE';
        this.score = 0;
        this.player = { x: 400, y: 300, hp: 100 };
        this.objects = [];
        this.canvas = document.getElementById('gameCanvas');
        this.renderer = new Renderer(this.canvas);
        this.lastTime = 0;
        this.gameLoop = this.gameLoop.bind(this);
        requestAnimationFrame(this.gameLoop);
    }
    gameLoop(timestamp) {
        const deltaTime = (timestamp - this.lastTime) / 1000;
        this.lastTime = timestamp;
        this.update(deltaTime);
        this.render();
        requestAnimationFrame(this.gameLoop);
    }
    update(dt) {
        if (this.state !== 'PLAYING') return;
        // TODO: 게임 오브젝트 업데이트
    }
    render() {
        this.renderer.clear();
        if (this.state === 'PLAYING') {
            this.renderer.drawPlayer(this.player);
        }
    }
}


// === 렌더링·UI (프론트팀) ===
// 렌더링 코드 (프론트팀 담당)
class Renderer {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
    }
    clear() { this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height); }
    drawPlayer(player) {
        this.ctx.fillStyle = '#e94560';
        this.ctx.beginPath();
        this.ctx.arc(player.x, player.y, 15, 0, Math.PI * 2);
        this.ctx.fill();
    }
}


// === 게임 시작 ===
window.addEventListener('load', () => { const game = new Game(); });
