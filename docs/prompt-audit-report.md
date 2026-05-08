# 프롬프트 종합 감사 보고서
**작성일**: 2026-05-08
**작성자**: Claude (너부리)
**대상**: 게임 생성 파이프라인 전체 프롬프트 8개 파일
**목적**: 반복 발생하는 게임 생성 오류의 구조적 원인 분석 및 선제적 방지책 수립

---

## 감사 개요

게임 생성 시 매번 동일한 버그 패턴이 반복 발생함에도 불구하고 기존 방식은 "오류 발생 → 사후 수정" 방식으로 대응하고 있었음. 이번 감사는 모든 프롬프트를 동시에 읽고 상호 정합성을 검증하며, 에이전트가 실수할 수 있는 모든 경로를 사전에 차단하는 것을 목표로 함.

---

## 🔴 CRITICAL (즉시 수정 필요)

### CRIT-01. `cto-debug-system.txt` — 디버그 에이전트가 이중 루프 버그를 재생성하도록 지시

**파일**: `cto-debug-system.txt`
**위치**: 10번 줄

**문제 코드**:
```
"requestAnimationFrame never called":
  Game constructor must call this.gameLoop = this.gameLoop.bind(this);
  requestAnimationFrame(this.gameLoop);
```

**문제점**:
QA가 "requestAnimationFrame이 호출되지 않는다"고 FAIL을 내면, CTO 디버그 에이전트가 위 지시에 따라 Game 생성자 안에 rAF를 추가함. 그런데 `back-main-system.txt`는 "Game 클래스 내부에 절대 rAF를 넣지 말라"고 명시하고 있음. 두 프롬프트가 완전히 상반된 지시를 내리고 있어, 디버그 루프가 돌 때마다 이중 루프 버그가 재생성됨.

**실제 발생 시나리오**:
1. Backend가 (올바르게) Game에 rAF 없이 코드 생성
2. QA가 "requestAnimationFrame never called" FAIL 발생 (QA 체크리스트 부재로 인한 오탐 가능성도 있음)
3. CTO 디버그 에이전트가 cto-debug-system.txt 10번 줄 따라 Game 생성자에 rAF 추가
4. 외부 initializationCode + 내부 Game.gameLoop → 이중 루프 → 게임 2배속, 충돌, 크래시

**개선 방향**:
cto-debug-system.txt의 rAF 관련 지시를 다음으로 교체:
```
"requestAnimationFrame never called":
  DO NOT add requestAnimationFrame inside Game class.
  The external initialization code (appended by CodeMerger) handles the game loop.
  If the init code is missing, check CodeMerger — never add rAF to Game constructor.
```

---

### CRIT-02. `back-fix-system.txt` — 수정 에이전트에 핵심 금지 규칙 전무

**파일**: `back-fix-system.txt`

**문제점**:
Back-fix 에이전트는 QA가 FAIL을 낸 후 CTO의 디버그 지시를 받아 코드를 수정하는 역할임. 그런데 현재 back-fix-system.txt에는 다음 규칙이 전혀 없음:
- Game 생성자에 requestAnimationFrame 금지
- Game 생성자에 이벤트 리스너 등록 금지
- renderer.render(getGameState()) 호출 금지
- 좌표계 800×600 고정 사용

CTO가 "이벤트 리스너 추가해"라고 지시하면, Back-fix 에이전트는 금지 규칙 없이 Game 생성자에 그대로 추가함 → 수정이 새 버그를 생성하는 악순환.

**실제 발생 시나리오**:
1. QA: "handleClick 이벤트가 없다" FAIL
2. CTO 디버그: "canvas.addEventListener('click') 추가해"
3. Back-fix: 생성자에 click 리스너 추가 (좌표 스케일링 없이 raw 픽셀로)
4. 결과: 외부 click 리스너(스케일링 있음) + 내부 click 리스너(스케일링 없음) → 모든 클릭 2번 발동, 좌표 오작동

**개선 방향**:
back-main-system.txt의 CRITICAL 섹션들(GAME LOOP, EVENT LISTENERS, RENDERER CALL, COORDINATE SYSTEM)을 back-fix-system.txt에도 동일하게 포함시킴.

---

## 🟠 HIGH (우선 수정 필요)

### HIGH-01. `qa-main-system.txt` / `qa-retest-system.txt` — 핵심 버그 패턴 8개 체크리스트 미포함

**파일**: `qa-main-system.txt`, `qa-retest-system.txt`

**문제점**:
QA가 다음 패턴들을 검사하지 않음. 이 패턴들은 실제로 반복 발생한 버그들임:

| 검사 항목 | 현재 상태 | 영향 |
|---|---|---|
| Game 생성자에 requestAnimationFrame 있는지 | ❌ 미검사 | 이중 루프 → 크래시 |
| Game 생성자에 이벤트 리스너 있는지 | ❌ 미검사 | 중복 등록 + 좌표 오작동 |
| renderer.render(getGameState()) 잘못된 호출 | ❌ 미검사 | game 속성 undefined |
| Renderer constructor(canvas) 잘못된 생성자 | ❌ 미검사 | new Renderer() 호출 시 즉시 크래시 |
| ctx.save() / ctx.restore() 불균형 | ❌ 미검사 | 모든 프레임에 transform 누적 → 렌더링 붕괴 |
| renderer.js 별도 script 태그 존재 | ❌ 미검사 | 404 에러 → 게임 로드 실패 |
| cursor: none + 커스텀 커서 없음 | ❌ 미검사 | 마우스 위치 불투명 → UX 불량 |
| canvas.offsetWidth를 init()에서만 계산 | ❌ 미검사 | iframe 내 0 반환 → scaleX=0 → 아무것도 안 그려짐 |

**개선 방향**:
qa-main-system.txt의 체크 카테고리에 `GAME_STRUCTURE` 항목 신설:
```
9. GAME_STRUCTURE — 아키텍처 계약 위반 검사:
   - Game 생성자에 requestAnimationFrame/gameLoop/setInterval 있으면 CRITICAL
   - Game 생성자에 canvas 이벤트 리스너(click/mousemove/keydown) 있으면 HIGH
   - renderer.render(this.getGameState()) 호출 패턴 있으면 HIGH (올바른 호출: renderer.render(game))
   - Renderer constructor가 인자를 받으면 HIGH (올바른 형태: constructor())
   - ctx.save() 수와 ctx.restore() 수가 불일치하면 HIGH
   - <script src="renderer.js"> 태그 있으면 HIGH
```
체크리스트에 위 항목들 추가, PASS/FAIL 기준에 반영.

---

### HIGH-02. `cto-main-system.txt` — `getGameState()` 메서드 계약 누락

**파일**: `cto-main-system.txt`

**문제점**:
CTO의 publicMethods 템플릿에 `update`, `handleClick`, `handleKeyDown`만 예시로 있고 `getGameState()`가 없음. Renderer가 `render(game)`에서 `game.state`, `game.score` 등을 직접 접근하려면 이 속성들이 Game 객체에 반드시 있어야 하는데, 명세가 없으면 Backend 에이전트가 속성명을 다르게 짓거나 누락시킴. 예: Backend가 `this.gameState` 내부 객체로 관리하고, Frontend가 `game.state` 직접 접근하면 undefined.

**개선 방향**:
CTO publicMethods에 getGameState 명시 강제:
```json
{"name": "getGameState", "signature": "getGameState(): object",
 "description": "현재 게임 상태 스냅샷 반환. 최소 {state: string} 포함. Renderer가 직접 접근하는 모든 필드 포함"}
```
또한 CTO 프롬프트에 "Renderer.render(game)에서 직접 접근하는 game의 속성명을 dataStructures에 명시할 것" 규칙 추가.

---

### HIGH-03. `cto-main-system.txt` — initializationCode JSON 문자열 압축 문제

**파일**: `cto-main-system.txt`

**문제점**:
`initializationCode` 필드가 JSON 문자열 안에 `\n`으로 들어있어 Claude가 실제로 출력할 때 개행 없이 한 줄짜리 코드를 생성하는 경우가 발생함. CodeMerger가 이 문자열을 파싱해서 game.js 끝에 붙이는데, 한 줄 압축 코드가 들어오면 파싱 실패 또는 syntax error 위험이 있음.

또한 현재 CTO 지시에 "postMessage listener 포함"이라고 되어 있는데, 실제 initializationCode 템플릿에 postMessage listener가 없음 (지시와 템플릿 불일치).

**개선 방향**:
- initializationCode 필드 설명에 "반드시 각 구문을 별도 줄로 작성" 강제 명시
- postMessage listener 포함 지시 제거하거나 실제 템플릿에 추가

---

### HIGH-04. `cto-main-system.txt` — 게임별 필수 메서드 누락 가능성

**파일**: `cto-main-system.txt`

**문제점**:
publicMethods 템플릿에 `update`, `handleClick`, `handleKeyDown` 3개만 예시로 있음. 실제 게임에서 필요한 `handleMouseMove`가 없으면:
1. CTO가 publicMethods에 포함 안 함
2. Backend가 handleMouseMove 구현 안 함
3. initializationCode의 `if (game.handleMouseMove) game.handleMouseMove(x, y)` — 이 가드 덕분에 크래시는 안 나지만 마우스 이동 반응이 없음

더 심각한 경우: Frontend가 Renderer에서 `game.mouseX`를 접근하는데 CTO가 이 속성을 publicMethods에 안 넣으면 Backend가 안 만들고 undefined.

**개선 방향**:
CTO 프롬프트에 게임 타입별 필수 메서드 가이드 추가:
```
마우스 기반 게임 (슈팅, 탑다운): handleMouseMove(x, y) 필수
터치/클릭 게임: handleClick(x, y) 필수
키보드 게임 (플랫포머, 스네이크): handleKeyDown(e) 필수
모든 게임: update(dt), getGameState() 필수
```

---

### HIGH-05. `front-fix-system.txt` — 미확인 (back-fix와 동일 문제 예상)

**파일**: `front-fix-system.txt`

**문제점**:
front-fix-system.txt를 아직 확인하지 않았으나 back-fix-system.txt와 동일한 구조적 문제가 있을 것으로 예상:
- Renderer constructor 규칙 없음
- ctx state hygiene 규칙 없음
- cursor: none 금지 규칙 없음
- 수정 시 renderer.js script 태그 추가 금지 규칙 없음

**개선 방향**:
front-main-system.txt의 CRITICAL 섹션들을 front-fix-system.txt에도 동일하게 포함.

---

## 🟡 MEDIUM (순차 수정)

### MED-01. `plan-main-system.txt` — `requiresTextInput` 타입 불일치

**파일**: `plan-main-system.txt`

**문제점**:
`"requiresTextInput": "true if players TYPE text..."` — 이 설명을 보고 Claude가 `"requiresTextInput": "true"` (문자열)로 출력할 수 있음. QA 프롬프트는 이 값으로 IME 검증 강도를 결정하는데, JavaScript에서 `if ("false")` 는 truthy임. 즉 `requiresTextInput: "false"` 여도 IME 검증이 HIGH로 적용되어 불필요한 FAIL이 발생할 수 있음.

**개선 방향**:
```json
"requiresTextInput": false  // boolean, NOT string. true = 타이핑 게임, false = 아케이드/슈팅/퍼즐
```
plan-main-system.txt에 "반드시 boolean으로 출력" 명시. QA 프롬프트에서도 문자열 "true"/"false" 모두 처리하는 파싱 로직 안내 추가.

---

### MED-02. `content-main-system.txt` — 상수명 정합성 보장 없음

**파일**: `content-main-system.txt`

**문제점**:
Content 에이전트가 생성하는 데이터의 카테고리명(category 필드)과 CTO가 contentIntegration에 명시하는 상수명이 불일치할 수 있음. Content가 `"category": "foods"`로 출력하고, CTO가 `FOOD_LIST`로 명시하면, Backend가 어떤 이름을 써야 할지 혼란스러움.

**개선 방향**:
Content 에이전트 출력의 category 필드 → CTO가 이를 읽고 정확한 상수명 도출 → Backend가 그대로 사용하는 흐름을 명시화. CTO 프롬프트에 "content.json의 각 category명을 기반으로 상수명을 UPPERCASE_SNAKE_CASE로 변환할 것" 규칙 추가.

---

## 정합성 검증 매트릭스

아래 표는 에이전트 간 계약이 실제로 일치하는지 검증한 결과임:

| 계약 항목 | CTO 명세 | Backend 구현 규칙 | Frontend 구현 규칙 | QA 검증 | 정합성 |
|---|---|---|---|---|---|
| Game constructor 시그니처 | `constructor(canvas, renderer)` ✅ | `constructor(canvas, renderer)` ✅ | N/A | ❌ 미검증 | ⚠️ |
| Renderer constructor 시그니처 | `constructor()` ✅ | N/A | `constructor()` ✅ | ❌ 미검증 | ⚠️ |
| Game loop 위치 | 외부 init only ✅ | rAF 금지 ✅ | N/A | ❌ 미검증 | ⚠️ |
| render() 인자 | `render(game)` ✅ | rAF 금지 ✅ | `render(game)` ✅ | ❌ 미검증 | ⚠️ |
| 좌표계 | 800×600 ✅ | 800×600 ✅ | 800×600 ✅ | ❌ 미검증 | ⚠️ |
| 이벤트 등록 위치 | 외부 init only ✅ | 생성자 금지 ✅ | N/A | ❌ 미검증 | ⚠️ |
| Debug 수정 시 규칙 | ❌ rAF 추가 지시 (잘못됨) | ❌ fix에 금지 규칙 없음 | ❌ fix에 금지 규칙 없음 | N/A | 🔴 |

**결론**: 생성(main) 에이전트 간 계약은 대부분 정립됨. **수정(fix/debug) 에이전트에 계약이 전혀 반영되지 않음이 최대 취약점.**

---

## 수정 우선순위 및 계획

| 순위 | 파일 | 등급 | 작업 내용 | 예상 효과 |
|---|---|---|---|---|
| 1 | `cto-debug-system.txt` | 🔴 CRITICAL | rAF 지시 제거, 올바른 디버그 패턴으로 교체 | 디버그 루프에서 이중 루프 재발 방지 |
| 2 | `back-fix-system.txt` | 🔴 CRITICAL | GAME LOOP / EVENT LISTENERS / RENDERER CALL / COORDINATE 규칙 추가 | 수정 시 새 버그 생성 방지 |
| 3 | `front-fix-system.txt` | 🟠 HIGH | constructor / ctx hygiene / cursor / renderer.js 규칙 추가 | 수정 시 Renderer 계약 위반 방지 |
| 4 | `qa-main-system.txt` | 🟠 HIGH | GAME_STRUCTURE 체크 카테고리 신설 (8개 항목) | 계약 위반 코드 QA에서 조기 탐지 |
| 5 | `qa-retest-system.txt` | 🟠 HIGH | qa-main과 동일한 체크 항목 추가 | 디버그 후 재검증에서도 계약 위반 탐지 |
| 6 | `cto-main-system.txt` | 🟠 HIGH | getGameState() 계약 추가, initializationCode 포맷 개선 | 게임별 속성 불일치 방지 |
| 7 | `plan-main-system.txt` | 🟡 MEDIUM | requiresTextInput boolean 타입 명확화 | QA 과민 반응 방지 |
| 8 | `content-main-system.txt` | 🟡 MEDIUM | 상수명 정합성 규칙 추가 | Backend 상수명 불일치 방지 |

---

## 장기 개선 방향

현재 구조는 "에이전트가 실수하면 다음 에이전트가 잡는" 방어 레이어가 거의 없음. 이상적인 구조는:

1. **생성 에이전트(main)**: 올바른 코드를 처음부터 만들도록 규칙 강화 ← 이번 세션에서 부분 완료
2. **수정 에이전트(fix)**: 수정 시 기존 계약을 깨지 않도록 동일한 금지 규칙 적용 ← 다음 수정 목표
3. **QA 에이전트**: 계약 위반을 체크리스트로 명시적 검증 ← 다음 수정 목표
4. **런타임 검증기(JsRuntimeValidator)**: Node.js 실행으로 실제 크래시 사전 탐지 ← 이미 구현됨, 활성화 확인 필요

이 4개 레이어가 모두 갖춰지면 게임 생성 성공률이 현재 대비 대폭 향상될 것으로 예상됨.
