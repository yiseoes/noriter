/**
 * game.js 런타임 검증기
 * 브라우저 환경을 모킹해서 Game/Renderer 인스턴스화 오류를 잡는다.
 * 사용: node game-validator.js <game.js 경로>
 */
const fs = require('fs');
const vm = require('vm');

// ── 브라우저 API 모킹 ────────────────────────────────────────────────

const mockCtx = {
    canvas: { width: 800, height: 600 },
    clearRect: () => {}, fillRect: () => {}, strokeRect: () => {},
    fillText: () => {}, strokeText: () => {},
    beginPath: () => {}, closePath: () => {}, fill: () => {}, stroke: () => {},
    arc: () => {}, arcTo: () => {}, rect: () => {},
    moveTo: () => {}, lineTo: () => {}, bezierCurveTo: () => {}, quadraticCurveTo: () => {},
    save: () => {}, restore: () => {},
    translate: () => {}, rotate: () => {}, scale: () => {}, setTransform: () => {},
    drawImage: () => {},
    createLinearGradient: () => ({ addColorStop: () => {} }),
    createRadialGradient: () => ({ addColorStop: () => {} }),
    createPattern: () => ({}),
    measureText: () => ({ width: 10 }),
    getImageData: () => ({ data: new Uint8ClampedArray(4) }),
    putImageData: () => {},
    clip: () => {},
    isPointInPath: () => false,
    font: '', fillStyle: '#000', strokeStyle: '#000',
    lineWidth: 1, lineCap: 'butt', lineJoin: 'miter',
    textAlign: 'left', textBaseline: 'alphabetic',
    globalAlpha: 1, globalCompositeOperation: 'source-over',
    shadowBlur: 0, shadowColor: '', shadowOffsetX: 0, shadowOffsetY: 0,
};

const mockCanvas = {
    getContext: () => mockCtx,
    width: 800, height: 600,
    addEventListener: () => {},
    removeEventListener: () => {},
    focus: () => {},
    blur: () => {},
    getBoundingClientRect: () => ({ left: 0, top: 0, width: 800, height: 600, right: 800, bottom: 600 }),
    style: {},
};

const mockInput = {
    addEventListener: () => {},
    removeEventListener: () => {},
    focus: () => {}, blur: () => {},
    value: '', style: {},
};

global.document = {
    getElementById: (id) => {
        if (id === 'gameCanvas') return mockCanvas;
        if (id === 'hiddenInput') return mockInput;
        const el = {
            style: {}, classList: { add: () => {}, remove: () => {}, contains: () => false },
            addEventListener: () => {}, removeEventListener: () => {},
            appendChild: () => {}, removeChild: () => {},
            textContent: '', innerHTML: '',
            getBoundingClientRect: () => ({ left: 0, top: 0, width: 0, height: 0 }),
        };
        return el;
    },
    querySelector: () => null,
    querySelectorAll: () => [],
    createElement: (tag) => {
        if (tag === 'canvas') return mockCanvas;
        return { style: {}, addEventListener: () => {}, appendChild: () => {} };
    },
    addEventListener: () => {},
    removeEventListener: () => {},
    body: { appendChild: () => {}, style: {} },
};

global.window = {
    addEventListener: () => {},
    removeEventListener: () => {},
    postMessage: () => {},
    innerWidth: 800,
    innerHeight: 600,
    devicePixelRatio: 1,
    location: { href: '' },
    requestAnimationFrame: () => 1,
    cancelAnimationFrame: () => {},
};

global.requestAnimationFrame = () => 1;
global.cancelAnimationFrame = () => {};
global.performance = { now: () => Date.now() };
global.Image = function() { this.onload = null; this.onerror = null; this.src = ''; this.width = 0; this.height = 0; };
global.Audio = function() {
    this.play = () => Promise.resolve();
    this.pause = () => {};
    this.load = () => {};
    this.volume = 1;
    this.currentTime = 0;
    this.src = '';
};
global.AudioContext = function() {
    return {
        createOscillator: () => ({ connect: () => {}, start: () => {}, stop: () => {}, frequency: { setValueAtTime: () => {} }, type: '' }),
        createGain: () => ({ connect: () => {}, gain: { setValueAtTime: () => {}, exponentialRampToValueAtTime: () => {} } }),
        destination: {},
        currentTime: 0,
    };
};
global.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
global.console = console;
global.Math = Math;
global.Date = Date;
global.JSON = JSON;
global.parseInt = parseInt;
global.parseFloat = parseFloat;
global.isNaN = isNaN;
global.isFinite = isFinite;
global.setTimeout = setTimeout;
global.clearTimeout = clearTimeout;
global.setInterval = setInterval;
global.clearInterval = clearInterval;

// ── 실행 ────────────────────────────────────────────────────────────

const gameJsPath = process.argv[2];
if (!gameJsPath) {
    console.error('USAGE_ERROR: game.js 경로가 필요합니다');
    process.exit(2);
}

let gameCode;
try {
    gameCode = fs.readFileSync(gameJsPath, 'utf8');
} catch (e) {
    console.error('FILE_ERROR: ' + e.message);
    process.exit(2);
}

// vm.runInThisContext으로 로드 — global 스코프에 클래스 등록
try {
    vm.runInThisContext(gameCode);
} catch (e) {
    console.error('LOAD_ERROR: ' + e.constructor.name + ': ' + e.message);
    if (e.stack) {
        const lines = e.stack.split('\n').slice(0, 4);
        lines.forEach(l => console.error('  ' + l));
    }
    process.exit(1);
}

// Renderer 인스턴스화
let renderer;
try {
    if (typeof Renderer === 'undefined') throw new Error('Renderer 클래스가 정의되지 않았습니다');
    renderer = new Renderer();
    if (typeof renderer.init === 'function') renderer.init(mockCanvas);
} catch (e) {
    console.error('RENDERER_ERROR: ' + e.constructor.name + ': ' + e.message);
    process.exit(1);
}

// Game 인스턴스화
try {
    if (typeof Game === 'undefined') throw new Error('Game 클래스가 정의되지 않았습니다');
    const game = new Game(mockCanvas, renderer);
    void game;
} catch (e) {
    console.error('GAME_ERROR: ' + e.constructor.name + ': ' + e.message);
    process.exit(1);
}

console.log('VALIDATION_PASS');
process.exit(0);
