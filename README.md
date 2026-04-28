# 🎮 NoriTer (놀이터)
### AI Multi-Agent Game Studio — 자연어 한 줄이 완성된 웹 게임이 되는 플랫폼

<br>

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Claude](https://img.shields.io/badge/Claude_API-Anthropic-CC785C?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

<br>

> 사용자가 요구사항을 입력하면, 가상의 AI 개발 스튜디오가
> **기획 → 설계 → 구현 → 테스트 → 디버깅 → 출시**까지
> 7단계를 자율적으로 수행해 플레이 가능한 HTML5 Canvas 게임을 생성합니다.

<br>

```
입력  →  "고양이를 모으는 귀여운 퍼즐 게임 만들어줘"

           ↓ PlanningAgent   기획서 작성 (장르·규칙·콘텐츠)
           ↓ ContentAgent    게임 데이터 100+ 항목 생성
           ↓ CtoAgent        인터페이스 계약 설계
           ↓ DesignAgent     색상·레이아웃 스펙
           ↓ FrontendAgent   HTML / CSS / Renderer 클래스
           ↓ BackendAgent    Game 클래스 / 게임 로직
           ↓ QaAgent         정적 코드 분석 · PASS 판정
           ↓ Release         파일 저장

출력  →  브라우저에서 바로 실행 가능한 HTML5 Canvas 게임  (~2분)
```

<br>

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [핵심 아이디어: 왜 멀티 에이전트인가](#2-핵심-아이디어-왜-멀티-에이전트인가)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [7단계 자율 파이프라인](#4-7단계-자율-파이프라인)
5. [에이전트 상세 설계](#5-에이전트-상세-설계)
6. [기술 스택](#6-기술-스택)
7. [도메인 모델 설계](#7-도메인-모델-설계)
8. [API 설계](#8-api-설계)
9. [프롬프트 엔지니어링 시스템](#9-프롬프트-엔지니어링-시스템)
10. [코드 생성 파이프라인 상세](#10-코드-생성-파이프라인-상세)
11. [실시간 모니터링 (SSE)](#11-실시간-모니터링-sse)
12. [보안 설계](#12-보안-설계)
13. [개발 회고 및 트러블슈팅](#13-개발-회고-및-트러블슈팅)
14. [테스트 전략](#14-테스트-전략)
15. [성능 및 비용 분석](#15-성능-및-비용-분석)
16. [설계 의사결정 기록 (ADR)](#16-설계-의사결정-기록-adr)
17. [향후 로드맵](#17-향후-로드맵)
18. [로컬 실행 방법](#18-로컬-실행-방법)
19. [프로젝트 구조](#19-프로젝트-구조)

<br>

---

## 1. 프로젝트 개요

**NoriTer(놀이터)**는 AI 멀티 에이전트 기반 미니게임 자동 생성 플랫폼입니다.

### 탄생 배경

"AI가 코드를 작성할 수 있다면, AI 에이전트들이 서로 협업해서 완성된 소프트웨어를 만들 수 있을까?"

이 질문에서 출발했습니다. 단순히 LLM에게 "게임 만들어줘"라고 요청하는 것이 아니라, **실제 소프트웨어 개발 팀의 역할 구조를 AI로 재현**하는 것이 목표였습니다.

### 결과

```
입력:  "고양이를 모으는 캐주얼 퍼즐 게임 만들어줘"
         ↓  (약 2~3분)
출력:  플레이 가능한 HTML5 Canvas 게임 (index.html + style.css + game.js)
```

생성된 게임은 브라우저에서 바로 실행되며, 다운로드(ZIP), 수정 요청도 가능합니다.

### 주요 기능

| 기능 | 설명 |
|------|------|
| **게임 자동 생성** | 자연어 요구사항 → 완성된 HTML5 게임 |
| **7개 전문 AI 에이전트** | 기획, 콘텐츠, 아키텍처, 디자인, 프론트, 백엔드, QA |
| **자율 디버깅 루프** | QA 실패 → CTO 버그 분류 → 해당 에이전트 수정 → 재검증 (최대 3회) |
| **실시간 파이프라인 모니터링** | SSE로 진행 단계, 에이전트 대화, 로그를 실시간으로 확인 |
| **수정 요청** | 완성된 게임에 자연어로 수정 요청 → 파이프라인 재실행 |
| **게임 미리보기·다운로드** | 브라우저 내 iframe 실행 + ZIP 다운로드 |

<br>

---

## 2. 핵심 아이디어: 왜 멀티 에이전트인가

### 단일 에이전트의 한계

단일 LLM 호출로 복잡한 게임을 생성하려 하면 다음 문제가 생깁니다:

- **컨텍스트 폭발**: 기획 + 설계 + 코드를 한번에 요청하면 응답 품질이 급격히 하락
- **역할 혼동**: "기획자이면서 개발자이면서 QA"로 동시 작동 시 최적화가 어려움
- **재시도 불가능**: 일부가 실패해도 처음부터 다시 해야 함

### 멀티 에이전트 접근법

NoriTer는 실제 소프트웨어 개발 조직 구조를 모방합니다:

```
실제 개발팀              NoriTer 에이전트
──────────────────       ────────────────────
PM / 기획자          →   PlanningAgent
콘텐츠 작가          →   ContentAgent
기술 아키텍트 (CTO)  →   CtoAgent
UI/UX 디자이너       →   DesignAgent
프론트엔드 개발자    →   FrontendAgent
백엔드 개발자        →   BackendAgent
QA 엔지니어          →   QaAgent
```

각 에이전트는:
- **전문화된 시스템 프롬프트**로 역할이 명확히 정의됩니다
- **이전 에이전트의 산출물**을 컨텍스트로 받아 이어서 작업합니다
- **명확한 출력 형식**(JSON 또는 섹션 블록)을 생성합니다
- **실패 시 해당 에이전트만** 재실행되므로 비용과 시간이 절약됩니다

<br>

---

## 3. 시스템 아키텍처

### 전체 계층 구조

```
┌────────────────────────────────────────────────────────────────┐
│  Client (React 19 + TypeScript)                               │
│  ├─ 게임 생성 요청 UI                                          │
│  ├─ 실시간 파이프라인 모니터링 (SSE)                           │
│  ├─ 에이전트 대화 로그 뷰어                                    │
│  └─ 게임 미리보기 iframe                                       │
└───────────────────────┬────────────────────────────────────────┘
                        │ REST API + SSE
┌───────────────────────▼────────────────────────────────────────┐
│  Presentation Layer (Spring MVC)                              │
│  ProjectController / GameController / LogController / SSE     │
└───────────────────────┬────────────────────────────────────────┘
                        │
┌───────────────────────▼────────────────────────────────────────┐
│  Service Layer                                                │
│  ProjectService / PipelineService / ArtifactService          │
└───────────────────────┬────────────────────────────────────────┘
                        │
┌───────────────────────▼────────────────────────────────────────┐
│  Agent Engine Layer  ← 이 프로젝트의 핵심                      │
│                                                               │
│  PipelineOrchestrator                                         │
│  ├─ StageExecutor (각 단계 실행 + 로그/감사 기록)             │
│  ├─ 7개 Agent (각자 Claude API 호출)                          │
│  ├─ CodeMerger (Frontend + Backend 코드 자동 병합)            │
│  ├─ JsSyntaxValidator (JS 문법 검증)                          │
│  ├─ PromptRegistry (프롬프트 로드 + 캐싱)                     │
│  └─ MessageBus (에이전트 간 비동기 메시지)                    │
└───────────────────────┬────────────────────────────────────────┘
                        │
┌───────────────────────▼────────────────────────────────────────┐
│  Infrastructure Layer                                         │
│  ClaudeApiClient / SseEmitterService / FileStorageService    │
│  EncryptionService / Spring Data JPA                         │
└───────────────┬───────────────┬───────────────┬───────────────┘
                ▼               ▼               ▼
             MySQL          Claude API      FileSystem
                         (claude-3-5-haiku  /workspace/
                          -20241022)         {projectId}/
```

### 비동기 파이프라인 실행

파이프라인은 `@Async`로 별도 스레드에서 실행됩니다. 사용자는 API 요청 후 즉시 응답을 받고, 이후 진행 상황은 SSE 스트림으로 실시간 전달됩니다.

```
POST /api/projects
    │
    ├─ [Main Thread] 프로젝트 DB 저장 → 202 Accepted 반환
    │
    └─ [Async Thread] PipelineOrchestrator.startPipeline()
           │
           ├─ STAGE 1~7 순차 실행
           └─ 각 단계마다 SSE 이벤트 전송 → 클라이언트 실시간 업데이트
```

<br>

---

## 4. 7단계 자율 파이프라인

```
사용자 입력: "요구사항 텍스트"
     │
     ▼
┌──────────────────────────────────────────────────────────────┐
│ STAGE 1: PLANNING                                           │
│ PlanningAgent → plan.json                                   │
│ 게임명, 장르, 규칙, 조작법, 콘텐츠 100+개, 점수 시스템       │
└────────────────────────────┬─────────────────────────────────┘
                             │ plan.json
                             ▼
┌──────────────────────────────────────────────────────────────┐
│ STAGE 1.5: CONTENT                                          │
│ ContentAgent → content.json                                 │
│ 게임 데이터 (고양이 목록, 스테이지 정보, 아이템 DB 등)       │
└────────────────────────────┬─────────────────────────────────┘
                             │ plan.json + content.json
                             ▼
┌──────────────────────────────────────────────────────────────┐
│ STAGE 2: ARCHITECTURE                                       │
│ CtoAgent → architecture.json                                │
│ 기술 스택, 파일 구조, Game/Renderer 인터페이스 계약 정의     │
│ (publicMethods: update, handleKeyDown, handleClick, render) │
└────────────────────────────┬─────────────────────────────────┘
                             │ + architecture.json
                             ▼
┌──────────────────────────────────────────────────────────────┐
│ STAGE 3: DESIGN                                             │
│ DesignAgent → design.json                                   │
│ 색상 팔레트, 타이포그래피, UI 레이아웃, 컴포넌트 스펙        │
└────────────────────────────┬─────────────────────────────────┘
                             │ + design.json
                             ▼
┌──────────────────────────────────────────────────────────────┐
│ STAGE 4: IMPLEMENTATION                                     │
│                                                            │
│  FrontendAgent ──────────────────────────────────────────  │
│  ├─ index.html  (Canvas + hiddenInput)                     │
│  ├─ style.css   (반응형, 모바일 지원)                       │
│  └─ Renderer class (Canvas 2D 렌더링)                      │
│                                                            │
│  BackendAgent ───────────────────────────────────────────  │
│  └─ Game class (게임 루프, 물리, 충돌, 상태 관리)           │
│                                                            │
│  CodeMerger ─────────────────────────────────────────────  │
│  ├─ Game + Renderer 병합                                   │
│  ├─ 중복 클래스 제거                                        │
│  └─ 초기화 코드 자동 추가 → game.js                        │
│                                                            │
│  JsSyntaxValidator ──────────────────────────────────────  │
│  └─ 문법 오류 시 BackendAgent 1회 재시도                   │
└────────────────────────────┬─────────────────────────────────┘
                             │ index.html + style.css + game.js
                             ▼
┌──────────────────────────────────────────────────────────────┐
│ STAGE 5: QA                                                 │
│ QaAgent → test-report.json                                  │
│ 정적 코드 분석 (SYNTAX / STRUCTURE / LOGIC / KOREAN_IME 등)  │
│ PASS / FAIL 판정 (CRITICAL/HIGH 버그 없으면 PASS)           │
└──────────────┬──────────────────────┬────────────────────────┘
               │ PASS                 │ FAIL
               ▼                      ▼
┌─────────────────┐    ┌──────────────────────────────────────┐
│ STAGE 7: RELEASE│    │ STAGE 6: DEBUG  (최대 3회)           │
│                 │    │                                       │
│ 파일 저장       │    │ CtoAgent: 버그 분류 → 담당 에이전트   │
│ 상태: COMPLETED │    │ ├─ 로직 버그   → BackendAgent        │
│ SSE 완성 알림   │    │ ├─ UI 버그     → FrontendAgent       │
└─────────────────┘    │ └─ 구조적 문제 → DesignAgent         │
                       │                                       │
                       │ CodeMerger → 재병합                   │
                       │ QaAgent    → 재테스트                 │
                       │                                       │
                       │ 3회 실패 → status: FAILED             │
                       └───────────────────────────────────────┘
```

### 파이프라인 상태 관리

| ProjectStatus | 의미 |
|--------------|------|
| `CREATED` | 생성 대기 |
| `IN_PROGRESS` | 파이프라인 실행 중 |
| `REVISION` | 수정 요청 처리 중 |
| `COMPLETED` | 게임 완성 |
| `FAILED` | 파이프라인 실패 (부분 재시도 가능) |
| `CANCELLED` | 사용자가 중단 |

<br>

---

## 5. 에이전트 상세 설계

### 에이전트 역할 분담

| 에이전트 | 스테이지 | 입력 | 출력 | 주요 책임 |
|---------|---------|------|------|----------|
| **PlanningAgent** | STAGE 1 | 요구사항 텍스트 | plan.json | 게임 기획서, 규칙, 콘텐츠 목록 |
| **ContentAgent** | STAGE 1.5 | plan.json | content.json | 게임 데이터 (DB 없음, artifacts만) |
| **CtoAgent** | STAGE 2, 6 | plan.json | architecture.json | 인터페이스 계약, 버그 분류 배정 |
| **DesignAgent** | STAGE 3 | plan+arch | design.json | 색상, 폰트, 레이아웃 스펙 |
| **FrontendAgent** | STAGE 4, 6 | plan+arch+design | HTML/CSS/Renderer | Canvas 렌더링, UI 화면 |
| **BackendAgent** | STAGE 4, 6 | plan+arch+renderCode | Game class | 게임 루프, 물리, 충돌, 한글 IME |
| **QaAgent** | STAGE 5, 6 | 3개 파일 전체 | test-report.json | 정적 분석, PASS/FAIL 판정 |

### CtoAgent: 아키텍처 계약 설계

CtoAgent가 생성하는 `architecture.json`의 핵심은 **Game ↔ Renderer 인터페이스 계약**입니다.

```json
{
  "gameInterface": {
    "gameClass": {
      "publicMethods": [
        { "name": "update",       "signature": "update(dt: number): void" },
        { "name": "handleKeyDown","signature": "handleKeyDown(e: KeyboardEvent): void" },
        { "name": "handleClick",  "signature": "handleClick(x: number, y: number): void" }
      ]
    },
    "rendererClass": {
      "publicMethods": [
        { "name": "init",    "signature": "init(canvas: HTMLCanvasElement): void" },
        { "name": "render",  "signature": "render(game: Game): void" },
        { "name": "setGame", "signature": "setGame(game: Game): void" }
      ]
    }
  },
  "contentIntegration": "WORD_DATABASE 배열로 content.json 데이터 직접 삽입"
}
```

BackendAgent와 FrontendAgent는 이 계약을 **반드시** 따라야 합니다. 계약 위반은 런타임 크래시로 이어지기 때문에, 프롬프트에 INTERFACE CONTRACT 규칙으로 강제하고 있습니다.

### CodeMerger: 자동 코드 병합

두 에이전트가 별도로 생성한 코드를 하나의 `game.js`로 병합하는 컴포넌트입니다.

```
BackendAgent  →  Game class
FrontendAgent →  Renderer class
                             } CodeMerger
                   + initializationCode (window.addEventListener('load', ...))
                             ↓
                          game.js
```

**병합 과정에서 처리하는 예외 케이스:**
- AI가 실수로 포함시킨 반대쪽 클래스 제거 (`stripClass`)
- 중복 클래스 정의 제거 (`deduplicateClasses`)
- 중복 초기화 코드 제거 (`removeInitCode`)
- 중괄호 depth 추적으로 클래스 블록 정확히 파악

<br>

---

## 6. 기술 스택

### 백엔드

| 분류 | 기술 | 버전 | 선택 이유 |
|------|------|------|----------|
| **Language** | Java | 17 (LTS) | 안정성, Spring 생태계 |
| **Framework** | Spring Boot | 3.4.4 | DI, JPA, Security 통합 |
| **Database** | MySQL | 8.x | 운영, H2는 테스트용 |
| **ORM** | Spring Data JPA + Hibernate | - | 엔티티 매핑, 쿼리 추상화 |
| **Auth** | Spring Security + JWT | jjwt 0.12.6 | Stateless 인증 |
| **AI** | Claude API (Anthropic) | claude-3-5-haiku | 빠른 응답, 한국어 품질 |
| **Realtime** | SSE (Server-Sent Events) | - | 단방향 실시간 스트리밍 |
| **Logging** | Log4j2 | - | 성능, 비동기 로깅 |
| **Build** | Maven | 3.x | 의존성 관리 |

### 프론트엔드

| 분류 | 기술 | 버전 | 선택 이유 |
|------|------|------|----------|
| **Language** | TypeScript | 5.9.3 | 타입 안정성 |
| **Framework** | React | 19.2 | 최신 Concurrent Features |
| **Build** | Vite | 8.0 | 빠른 HMR, 번들 최적화 |
| **Styling** | Tailwind CSS | 4.2 | 유틸리티 기반 빠른 UI |
| **State** | TanStack Query | 5.95 | 서버 상태, 캐싱, 리페치 |
| **HTTP** | Axios | 1.14 | 인터셉터, 타임아웃 제어 |
| **Router** | React Router | 7.13 | SPA 라우팅 |

<br>

---

## 7. 도메인 모델 설계

### ERD (핵심 관계)

```
┌──────────────────────────────────┐
│           project                │
├──────────────────────────────────┤
│ PK  id         VARCHAR(20)       │  ← "prj_" + UUID 단축형
│     name       VARCHAR(100)      │
│     requirement TEXT             │  ← 사용자 입력 원문
│     genre      ENUM              │  ← PUZZLE, ACTION, ARCADE...
│     status     ENUM              │  ← CREATED, IN_PROGRESS...
│     currentStage ENUM            │
│     progress   INT (0~100)       │
│     debugAttempts INT            │
│     maxDebugAttempts INT         │  ← 기본값: 3
│     userId     BIGINT            │  ← 로그인 사용자
│     guestId    VARCHAR           │  ← 비로그인 사용자
│     feedbackCount INT            │
└──────────┬───────────────────────┘
           │ 1:N (Cascade)
     ┌─────┼──────────────┬──────────────┬──────────────┐
     ▼     ▼              ▼              ▼              ▼
  stage  artifact     log_entry   agent_message    token_usage
  (파이프    (산출물:       (작업 로그:     (에이전트       (토큰 사용량:
  라인 단계)  plan.json,    INFO/WARN/      대화 기록)      에이전트별
             game.js 등)   ERROR/AGENT)                   집계)

┌──────────────┐   ┌────────────────┐   ┌─────────────┐
│  audit_log   │   │    user        │   │   setting   │
│  (전체 감사)  │   │  (JWT 인증)    │   │  (API 키)   │
└──────────────┘   └────────────────┘   └─────────────┘
```

### 기본 키 설계

```java
// 사람이 읽을 수 있는 접두사 + UUID 단축형
"prj_a1b2c3d4"  → 프로젝트
"stg_x9y8z7w6"  → 파이프라인 스테이지
"art_m5n4o3p2"  → 산출물 (Artifact)
"log_q1r2s3t4"  → 로그 엔트리
```

### 주요 설계 결정

**`feedbackCount`의 null 안전성 처리**
기존 DB 레코드에서 `NULL`인 경우를 방어하기 위해 Lombok 자동 getter 대신 수동 구현:

```java
@Column(name = "feedback_count", nullable = false)
@Getter(AccessLevel.NONE)
private Integer feedbackCount;

public int getFeedbackCount() {
    return this.feedbackCount != null ? this.feedbackCount : 0;
}
```

<br>

---

## 8. API 설계

### 엔드포인트 목록

#### 프로젝트 (ProjectController)
| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/projects` | 게임 생성 요청 (파이프라인 비동기 시작) |
| `GET` | `/api/projects` | 프로젝트 목록 (페이징, 상태 필터) |
| `GET` | `/api/projects/{id}` | 상세 (스테이지, 산출물, 토큰 사용량 포함) |
| `POST` | `/api/projects/{id}/retry` | 실패 지점부터 재시도 |
| `POST` | `/api/projects/{id}/feedback` | 자연어 수정 요청 |
| `POST` | `/api/projects/{id}/cancel` | 파이프라인 중단 |
| `DELETE` | `/api/projects/{id}` | 프로젝트 삭제 |

#### 게임 (GameController)
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/projects/{id}/preview` | 완성된 게임 HTML 렌더링 |
| `GET` | `/api/projects/{id}/download` | 게임 파일 ZIP 다운로드 |
| `GET` | `/api/projects/{id}/source` | 소스 파일 목록 |
| `GET` | `/api/projects/{id}/source/{path}` | 파일 내용 조회 |

#### 로그 & 감사 (LogController)
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/projects/{id}/logs` | 작업 로그 (레벨 필터 가능) |
| `GET` | `/api/projects/{id}/messages` | 에이전트 대화 기록 |
| `GET` | `/api/audit-logs` | 전체 감사 로그 |

#### 실시간 (SseController)
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/projects/{id}/stream` | SSE 스트림 연결 |

#### 인증 (AuthController)
| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/auth/signup` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 → JWT 토큰 반환 |
| `GET` | `/api/auth/me` | 내 정보 조회 |

### SSE 이벤트 타입

```
event: stage-update  → 파이프라인 단계 진행 (stageName, status)
event: log           → 새 로그 생성
event: complete      → 파이프라인 완료
event: error         → 에러 발생
event: cancelled     → 취소됨
```

<br>

---

## 9. 프롬프트 엔지니어링 시스템

### 프롬프트 구조 설계

NoriTer의 프롬프트 시스템은 단순한 "지시문"이 아니라, **에이전트 간 계약을 강제하는 메커니즘**으로 설계되었습니다.

```
templates/
├── {agent}-main-system.txt   ← 에이전트 역할 + 제약 조건 + 출력 형식
├── {agent}-main-user.txt     ← 실제 입력 데이터 + 템플릿 변수
├── {agent}-fix-system.txt    ← 디버깅 모드 역할 정의
└── {agent}-fix-user.txt      ← 버그 리포트 + 수정 지시
```

총 **24개 프롬프트 파일** (에이전트 × 용도 × system/user)

### 핵심 프롬프트 설계 원칙

**1. 출력 형식 강제**

모든 에이전트는 지정된 형식으로만 응답합니다:
```
기획/설계/검증 에이전트: JSON only (마크다운, 설명 금지)
코드 에이전트: ===SECTION_NAME=== ... ===END_SECTION_NAME=== 블록
```

**2. 인터페이스 계약 강제**

```
INTERFACE CONTRACT — MANDATORY:
- 반드시 architecture.json의 gameClass.publicMethods 모든 메서드 구현
- 누락된 메서드 1개 = 런타임 크래시
```

**3. 한글 IME 필수 패턴**

한국어 입력을 지원하는 게임을 위한 필수 구현 패턴을 프롬프트에 명시:
```javascript
// Chrome IME 음절 누적 처리 (e.data는 현재 음절만 반환)
handleCompositionEnd(value) {
  const hiddenValue = this._hiddenInput.value;
  const currentValue = hiddenValue.startsWith(this.inputText)
    ? hiddenValue
    : this.inputText + hiddenValue;
  this.inputText = currentValue;
}
```

**4. postMessage 대응**

iframe 내 게임은 부모 페이지에서 `postMessage`로 키보드 이벤트를 전달받습니다. 이 이벤트는 DOM 메서드가 없는 순수 객체이므로:
```javascript
// 직접 호출 금지: e.preventDefault()
// 가드 필수: if (e.preventDefault) e.preventDefault();
```

**5. QA 에이전트 검사 항목**

```
CRITICAL: 즉시 SyntaxError/ReferenceError 발생
HIGH:     핵심 기능 완전히 부재 (게임 루프 없음, 클래스 미인스턴스화)
MEDIUM:   동작하나 결과가 의도와 다름
LOW:      네이밍, 미사용 변수 등

Pass 조건: CRITICAL 0개 AND HIGH 0개

검사 항목:
- SYNTAX / STRUCTURE / LOGIC / UI / INTERACTION
- KOREAN_IME (hiddenInput 존재, _bindHiddenInput, isComposing 가드)
- PERFORMANCE / SECURITY
```

### 프롬프트 반복 개선 이력

| 이슈 | 원인 | 해결 방법 |
|------|------|----------|
| 코드가 한 줄로 압축 생성됨 | 압축 금지 규칙 없음 | CODE READABILITY MANDATORY 섹션 추가 |
| WORD_DATABASE 미정의 오류 | 상수명 규칙 없음 | UPPERCASE_SNAKE_CASE + contentIntegration 필드 명시 필수 |
| 한글 IME 작동 안 함 | AI가 isComposing 가드 누락 | KOREAN IME MANDATORY 섹션 + QA 검사 항목 추가 |
| Renderer 클래스 중복 | AI가 지시 무시 | CodeMerger stripClass + deduplicateClasses 안전망 |
| QA 오탐으로 FAIL 과다 | 기준이 느슨 | STATIC ANALYSIS ONLY 규칙 + 정확한 severity 기준 명시 |

<br>

---

## 10. 코드 생성 파이프라인 상세

### 생성되는 게임 구조

```
/workspace/{projectId}/
├── index.html    ← FrontendAgent 생성
├── style.css     ← FrontendAgent 생성
└── game.js       ← CodeMerger가 자동 병합
```

### game.js 내부 구조

```javascript
// ==============================
// === Game 로직 (BackendAgent) ===
// ==============================

class Game {
  constructor(canvas, renderer) {
    this.canvas = canvas;
    this.renderer = renderer;
    this.gameState = 'start';
    this._bindHiddenInput();  // ← 한글 IME 필수
  }

  _bindHiddenInput() {
    const hi = document.getElementById('hiddenInput');
    hi.addEventListener('compositionstart', () => { this.isComposing = true; });
    hi.addEventListener('compositionend', (e) => {
      this.isComposing = false;
      this.handleCompositionEnd(e.data || '');
    });
    hi.addEventListener('input', (e) => {
      if (e.isComposing || this.isComposing) return;  // ← 이중 가드
      this.handleInput(hi.value);
    });
  }

  update(dt) { /* 매 프레임 게임 상태 업데이트 */ }
  handleKeyDown(e) {
    if (e.preventDefault) e.preventDefault();  // ← postMessage 객체 대응
    // ...
  }
  handleClick(x, y) { /* 클릭 처리 */ }
}

// =================================
// === Renderer (FrontendAgent) ===
// =================================

class Renderer {
  init(canvas) { this.ctx = canvas.getContext('2d'); }
  render(game) { /* Canvas 2D 렌더링 */ }
  resizeCanvas() { /* 창 크기 대응 */ }
}

// ========================================
// === 초기화 코드 (CodeMerger 자동 추가) ===
// ========================================

window.addEventListener('load', () => {
  const canvas = document.getElementById('gameCanvas');
  const renderer = new Renderer();
  renderer.init(canvas);
  const game = new Game(canvas, renderer);

  canvas.setAttribute('tabindex', '0');
  canvas.focus();

  // iframe ↔ 부모 페이지 키보드 이벤트 브리지
  window.addEventListener('message', (e) => {
    if (e.data && e.data.type === 'keydown') {
      game.handleKeyDown(e.data);
    }
  });

  document.addEventListener('keydown', (e) => { game.handleKeyDown(e); });

  let lastTime = 0;
  function gameLoop(timestamp) {
    const dt = Math.min((timestamp - lastTime) / 1000, 0.05);
    lastTime = timestamp;
    game.update(dt);
    renderer.render(game);
    requestAnimationFrame(gameLoop);
  }
  requestAnimationFrame(gameLoop);
});
```

<br>

---

## 11. 실시간 모니터링 (SSE)

### 클라이언트-서버 실시간 연결

```
[React 클라이언트]                    [Spring Server]
    useSse() hook                       SseController
         │                                    │
         │  GET /api/projects/{id}/stream     │
         ├───────────────────────────────────>│
         │  Content-Type: text/event-stream   │
         │<───────────────────────────────────│
         │                                    │
         │  event: stage-update              │
         │  data: {"stageName":"PLANNING"}   │
         │<───────────────────────────────────│
         │                                    │
         │  event: stage-update              │
         │  data: {"stageName":"ARCHITECTURE"}│
         │<───────────────────────────────────│
         │            ...                     │
         │  event: complete                  │
         │  data: {"status":"COMPLETED"}     │
         │<───────────────────────────────────│
```

### 프론트엔드 SSE 처리 흐름

```typescript
// ProjectDetail.tsx
useSse({
  projectId: project.id,
  onEvent: handleSseEvent,
  enabled: isInProgress,
});

const handleSseEvent = (type: string, data: unknown) => {
  if (type === 'stage-update') {
    invalidateAll();          // React Query 캐시 무효화 → 자동 리페치
    setSseStatus(message);    // 상태 메시지 업데이트
  }
  if (type === 'complete') {
    invalidateAll();
    setPreviewRefreshKey(k => k + 1);  // iframe 게임 자동 새로고침
  }
};
```

<br>

---

## 12. 보안 설계

### JWT 기반 인증

```
로그인 요청
    │
    ├─ 비밀번호 검증 (BCrypt)
    └─ JWT 생성 (HS256, 만료: 24h)
         │
         └─ 클라이언트 로컬스토리지 저장

API 요청
    │
    ├─ JwtAuthenticationFilter
    ├─ 토큰 파싱 + 만료 체크
    └─ SecurityContext 설정
```

### 소유권 격리

프로젝트는 **생성자만 접근 가능**합니다:

```java
// ProjectService.java
public Project getProject(String id, Long userId, String guestId) {
    Project project = projectRepository.findById(id)...;
    if (!isOwner(project, userId, guestId)) {
        throw new NoriterException(ErrorCode.PROJECT_NOT_FOUND);
    }
    return project;
}
```

비로그인 사용자는 `guestId` (세션 기반), 로그인 사용자는 `userId`로 구분합니다.

### API 키 암호화

사용자가 입력한 Anthropic API 키는 `EncryptionService`로 암호화되어 DB에 저장됩니다.

<br>

---

## 13. 개발 회고 및 트러블슈팅

이 프로젝트를 개발하면서 겪은 핵심 기술적 도전들입니다.

### ① Chrome 한글 IME 음절 누적 버그

**증상**: '축구' 입력 시 '구'만 인식됨
**원인**: Chrome에서 `compositionend` 이벤트의 `e.data`는 현재 음절만 반환 (누적값 아님)
**해결**:
```javascript
handleCompositionEnd(value) {
  const hiddenValue = this._hiddenInput.value;
  // hiddenInput이 이미 누적값을 가지고 있으면 그것을 사용
  // 아니면 현재 inputText에 새 음절 추가
  const currentValue = hiddenValue.startsWith(this.inputText)
    ? hiddenValue
    : this.inputText + hiddenValue;
  this.inputText = currentValue;
}
```

### ② iframe 키보드 포커스 문제

**증상**: 게임이 iframe 안에 있어서 스페이스바, 방향키로 조작 안 됨
**원인**: 브라우저가 user gesture 없이 `contentWindow.focus()` 차단
**해결**: PostMessage 릴레이 패턴 도입

```typescript
// PreviewTab.tsx — 부모 페이지에서 키 이벤트 캡처 → iframe으로 전달
window.addEventListener('keydown', (e) => {
  iframeRef.current?.contentWindow?.postMessage(
    { type: 'keydown', code: e.code, key: e.key },
    '*'
  );
});
```

```javascript
// game.js — iframe 안에서 메시지 수신
window.addEventListener('message', (e) => {
  if (e.data && e.data.type === 'keydown') {
    game.handleKeyDown(e.data);
  }
});
```

이때 postMessage로 전달된 객체는 DOM Event가 아니므로 `e.preventDefault`가 없어, 모든 `e.preventDefault()` 호출에 가드를 추가했습니다.

### ③ AI가 생성한 코드의 중복 클래스 문제

**증상**: AI가 지시를 무시하고 FrontendAgent가 Game 클래스를, BackendAgent가 Renderer 클래스를 포함하는 경우 발생
**해결**: CodeMerger에 중괄호 depth 추적 기반 클래스 블록 탐지 + 제거 로직 구현

```java
// 클래스 선언에서 시작해 매칭되는 닫는 중괄호까지 정확히 파악
private String stripClass(String code, String className) {
    int classIdx = code.indexOf("class " + className);
    int braceStart = code.indexOf('{', classIdx);
    int depth = 0;
    for (int i = braceStart; i < code.length(); i++) {
        if (code.charAt(i) == '{') depth++;
        else if (code.charAt(i) == '}' && --depth == 0) {
            // 이 위치가 클래스 블록의 끝
        }
    }
}
```

### ④ 수정 요청(Feedback) 500 에러

**증상**: `POST /api/projects/{id}/feedback` → 500 Internal Server Error
**원인**: `Project.feedbackCount`가 기존 DB 레코드에서 `NULL`이었고, Lombok 자동 생성 getter가 `null`을 그대로 반환해 `>= FEEDBACK_LIMIT` 비교에서 NPE 발생
**해결**: `@Getter(AccessLevel.NONE)`으로 자동 getter 비활성화 + null-safe 수동 getter 작성

### ⑤ QA 에이전트 과민 반응

**증상**: 정상 작동하는 코드를 QA가 FAIL로 판정해 불필요한 디버깅 루프 발생
**원인**: 프롬프트에 severity 기준이 명확하지 않아 MEDIUM 이슈를 HIGH로 분류
**해결**: 프롬프트에 "STATIC ANALYSIS ONLY", 정확한 severity 정의, "benefit of the doubt" 원칙 명시

<br>

---

## 14. 로컬 실행 방법

### 사전 요구사항

- Java 17+
- Maven 3.x
- Node.js 20+
- MySQL 8.x
- Anthropic API Key

### 백엔드 실행

```bash
# 1. DB 생성
mysql -u root -p -e "CREATE DATABASE noriter CHARACTER SET utf8mb4;"

# 2. application.yml 설정 (DB 연결 정보)
cd backend/src/main/resources
cp application.yml.example application.yml
# datasource URL, username, password 수정

# 3. 서버 실행
cd backend
mvn spring-boot:run
# http://localhost:8080
```

### 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

### API 키 설정

1. 앱 접속 → 설정(⚙) → API Key 입력
2. 또는 `PUT /api/settings/api-key` 직접 호출

<br>

---

## 15. 프로젝트 구조

```
workspace_NORITER/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/noriter/
│       ├── agent/
│       │   ├── core/          # BaseAgent, AgentContext, AgentResult
│       │   ├── impl/          # 7개 에이전트 구현체
│       │   ├── pipeline/      # PipelineOrchestrator, StageExecutor, CodeMerger
│       │   ├── prompt/        # PromptRegistry, PromptTemplate
│       │   └── message/       # MessageBus
│       ├── auth/              # JWT 인증
│       ├── controller/        # REST API + DTO
│       ├── domain/            # JPA 엔티티 + Enum
│       ├── service/           # 비즈니스 로직
│       ├── repository/        # Spring Data JPA
│       ├── infrastructure/    # Claude API, SSE, 파일, 암호화
│       ├── exception/         # NoriterException, ErrorCode
│       ├── config/            # Async, CORS, Security
│       └── util/              # IdGenerator, JsSyntaxValidator
│   └── src/main/resources/
│       ├── templates/         # 24개 AI 프롬프트 파일
│       ├── application.yml
│       └── log4j2.xml
│
├── frontend/
│   ├── package.json
│   └── src/
│       ├── components/        # React 컴포넌트 (홈, 게임, 모달, 프로젝트)
│       ├── hooks/             # useAuth, useProjects, useSse, ...
│       ├── api/               # Axios 클라이언트 + API 함수
│       └── types/             # TypeScript 타입 정의
│
├── docs/                      # 설계 문서 9개
│   ├── 01_프로젝트_개요서.md
│   ├── 03_시스템_아키텍처_설계서.md
│   ├── 04_에이전트_역할_정의서.md
│   ├── 05_API_설계서.md
│   ├── 06_데이터베이스_설계서.md
│   └── ...
│
└── wireframe/                 # 초기 UI 와이어프레임
```

<br>

---

## 기술적 도전 요약

| 도전 | 접근 방식 |
|------|----------|
| AI 출력의 비결정성 | 섹션 블록 파싱, 실패 시 재시도, 안전망 클린업 |
| 에이전트 간 계약 유지 | CTO 아키텍처 JSON으로 인터페이스 명세화, 프롬프트 강제 |
| 브라우저 iframe 보안 | postMessage 릴레이 패턴으로 키 이벤트 전달 |
| Chrome 한글 IME 특이사항 | compositionend 음절 누적 + isComposing 이중 가드 |
| 실시간 UX | SSE + React Query 연계로 파이프라인 진행 즉시 반영 |
| 비용 절감 | 실패 단계만 재실행, 디버깅 최대 3회 제한, 토큰 사용량 추적 |

<br>

---

*NoriTer는 "AI가 실제로 소프트웨어를 만들 수 있는가"라는 질문에 대한 실험적 답변입니다.*
*단순한 코드 생성이 아닌, 전문화된 에이전트들이 각자의 역할을 맡아 협업하는 시스템을 구현했습니다.*

<br>

---

## 14. 테스트 전략

### 테스트 피라미드

```
               ┌─────────────────────────┐
               │     E2E 테스트           │  ← 실제 파이프라인 실행
               │  (비용 발생, 사전 승인)   │     Claude API 호출 포함
               └───────────┬─────────────┘
              ┌────────────┴────────────┐
              │     통합 테스트          │  ← Spring + H2
              │  (MockMvc + TestContainers)│   JWT 인증, API 전체 흐름
              └──────────┬──────────────┘
             ┌───────────┴───────────────┐
             │       단위 테스트          │  ← 순수 Java 로직
             │   (JUnit 5, Mockito)       │   에이전트, CodeMerger, 파서
             └───────────────────────────┘
```

### 단위 테스트 (Unit Tests)

| 테스트 클래스 | 검증 대상 |
|-------------|---------|
| `PipelineOrchestratorTest` | 파이프라인 단계 순서, 분기 조건 |
| `CodeMergerTest` | 클래스 병합, 중복 제거, 초기화 코드 추가 |
| `JsSyntaxValidatorTest` | JS 문법 오류 감지, 오탐 방지 |
| `PromptRegistryTest` | 프롬프트 로드, 캐싱, 템플릿 변수 치환 |
| `AgentResultTest` | 결과 파싱, 섹션 블록 추출 |
| `IdGeneratorTest` | ID 형식, 접두사, 유일성 |
| `ProjectServiceTest` | 소유권 검증, 상태 전이 |

### 통합 테스트 (Integration Tests)

```java
// AuthIntegrationTest — 인증 전체 흐름
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Test
    void 회원가입_로그인_JWT_보호_API_전체_흐름() {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")...);

        // 2. 로그인 → JWT 토큰 수신
        String token = mockMvc.perform(post("/api/auth/login")...)
                              .andReturn()...;

        // 3. JWT로 보호된 API 접근
        mockMvc.perform(get("/api/projects")
                    .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk());
    }
}
```

```java
// ProjectOwnershipIntegrationTest — 소유권 격리
@Test
void 다른_사용자의_프로젝트_접근_불가() {
    // userA의 프로젝트를 userB의 JWT로 접근 시도
    mockMvc.perform(get("/api/projects/{id}", projectA.getId())
                .header("Authorization", "Bearer " + tokenB))
           .andExpect(status().isNotFound());  // 존재하지 않는 것처럼 처리
}
```

### 파이프라인 테스트 원칙

AI API를 호출하는 E2E 테스트는 **비용이 발생**하므로 실행 전 반드시 예상 비용 고지 후 승인을 받습니다.

- **파이프라인 1회 실행**: 약 15,000~25,000 토큰 소모 (입력 + 출력 합산)
- **테스트 격리**: H2 인메모리 DB 사용, 실제 파일 저장소는 임시 디렉터리

<br>

---

## 15. 성능 및 비용 분석

### 파이프라인 실행 시간 (실측)

| 단계 | 에이전트 | 평균 소요 시간 | 비고 |
|------|---------|-------------|------|
| PLANNING | PlanningAgent | ~15초 | 기획서 JSON 생성 |
| CONTENT | ContentAgent | ~10초 | 콘텐츠 데이터 100+ 항목 |
| ARCHITECTURE | CtoAgent | ~10초 | 인터페이스 계약 JSON |
| DESIGN | DesignAgent | ~8초 | 색상·레이아웃 JSON |
| IMPLEMENTATION | FrontendAgent | ~25초 | HTML + CSS + Renderer |
| IMPLEMENTATION | BackendAgent | ~30초 | Game 클래스 전체 |
| QA | QaAgent | ~15초 | 코드 정적 분석 |
| DEBUG (발생 시) | 해당 에이전트 | ~20초 | 최대 3회 |
| **전체 (PASS 시)** | — | **~2분** | 디버깅 없는 경우 |
| **전체 (1회 DEBUG)** | — | **~3분** | |

### 토큰 사용량 (실측 추정)

| 에이전트 | 입력 토큰 | 출력 토큰 | 합산 |
|---------|---------|---------|------|
| PlanningAgent | ~2,000 | ~1,500 | ~3,500 |
| ContentAgent | ~1,500 | ~2,000 | ~3,500 |
| CtoAgent | ~2,500 | ~1,000 | ~3,500 |
| DesignAgent | ~3,000 | ~800 | ~3,800 |
| FrontendAgent | ~3,500 | ~2,500 | ~6,000 |
| BackendAgent | ~4,000 | ~3,000 | ~7,000 |
| QaAgent | ~5,000 | ~500 | ~5,500 |
| **총합** | | | **~32,800** |

- 모델: claude-3-5-haiku-20241022 (빠른 응답 + 낮은 비용 선택)
- 게임 1개 생성 비용: **약 $0.01 ~ $0.02** (하이쿠 기준)
- 에이전트별 토큰 사용량은 DB에 기록되어 관리자가 조회 가능

### 비용 최적화 전략

```
1. 재시도 최소화
   - 파이프라인 실패 시 실패 단계부터만 재시도
   - "이어서 재시도" 기능으로 완료 단계 스킵

2. 모델 선택
   - claude-3-5-haiku: 빠름 + 저렴 + 코드 생성 품질 충분
   - Opus/Sonnet 대비 약 5~10배 저렴

3. 디버깅 횟수 제한
   - 최대 3회 디버깅 루프
   - 초과 시 FAILED 처리 → 사용자가 이어서 재시도 선택

4. 프롬프트 최적화
   - 불필요한 설명 제거, 출력 형식 명확히 → 출력 토큰 절감
   - 섹션 블록 방식으로 마크다운 코드펜스 제거 → 파싱 오류 방지
```

<br>

---

## 16. 설계 의사결정 기록 (ADR)

프로젝트에서 내린 주요 기술 결정과 그 이유를 기록합니다.

### ADR-001: 멀티 에이전트 vs 단일 에이전트

**결정**: 전문화된 7개 에이전트 분리
**대안**: 단일 LLM 호출로 전부 생성
**이유**:
- 컨텍스트가 커질수록 응답 품질 하락 (컨텍스트 희석 문제)
- 역할을 좁혀줄수록 각 에이전트의 출력 품질이 높아짐
- 실패 단계만 재실행 가능 → 비용/시간 절약
- 실제 소프트웨어 팀 구조를 모방해 이해와 디버깅이 쉬움

**트레이드오프**: 총 API 호출 횟수 증가, 에이전트 간 계약 관리 복잡도 증가

---

### ADR-002: 코드 출력 형식 — JSON vs 섹션 블록

**결정**: 코드는 `===SECTION===...===END_SECTION===` 블록, 기획/설계는 JSON
**대안**: 코드도 JSON으로 감싸기 (`{"code": "..."}`)
**이유**:
- JSON 내부 코드 문자열은 이스케이프 문제 잦음 (줄바꿈 `\n`, 따옴표 `\"`)
- AI가 JSON 인코딩을 실수하면 파싱 완전 실패
- 섹션 블록은 코드를 있는 그대로 포함 가능 → 파싱 안정성 높음
- 마크다운 코드펜스(` ``` `)와 달리 섹션 이름이 명확해 파싱 혼동 없음

---

### ADR-003: 실시간 통신 — SSE vs WebSocket

**결정**: SSE (Server-Sent Events)
**대안**: WebSocket
**이유**:
- 파이프라인은 서버 → 클라이언트 **단방향** 스트리밍이면 충분
- WebSocket은 양방향 통신 오버헤드와 세션 관리 복잡도가 있음
- SSE는 HTTP 위에서 동작해 인프라 제약 없음 (프록시, 방화벽 친화적)
- Spring의 `SseEmitter`로 간단하게 구현 가능

---

### ADR-004: 프론트엔드 상태 관리 — TanStack Query vs Redux

**결정**: TanStack Query (React Query)
**대안**: Redux Toolkit, Zustand
**이유**:
- NoriTer의 상태는 대부분 **서버 상태** (DB에서 오는 프로젝트, 로그 등)
- TanStack Query는 서버 상태 캐싱, 백그라운드 리페치, stale-while-revalidate 기본 제공
- SSE 이벤트 수신 후 `queryClient.invalidateQueries()`로 즉시 최신화 가능
- Redux 대비 보일러플레이트 대폭 감소

---

### ADR-005: 게임 코드 병합 전략 — Runtime vs Build Time

**결정**: 서버사이드 CodeMerger로 Build Time 병합
**대안**: 브라우저에서 두 파일을 별도 `<script>` 태그로 로드
**이유**:
- 두 파일로 분리 시 로드 순서 보장이 복잡 (Game 클래스가 Renderer보다 먼저 로드되면 오류)
- 단일 `game.js`로 병합하면 미리보기·다운로드 모두 단순해짐
- 초기화 코드를 항상 마지막에 1번만 삽입 보장 (중복 방지)
- AI가 실수로 포함시킨 반대편 클래스를 서버에서 정리 가능

---

### ADR-006: 게임 장르 분류

**결정**: 6개 장르 Enum (PUZZLE, ACTION, ARCADE, SHOOTING, STRATEGY, OTHER)
**이유**:
- 너무 세분화하면 기획 AI가 장르를 혼동
- 6개로 단순화하면 PlanningAgent의 장르 선택이 일관되고 콘텐츠 생성 품질이 높아짐
- OTHER 포함으로 새 장르 수용 가능

<br>

---

## 17. 향후 로드맵

### Phase 1 — 품질 고도화 (진행 중)

- [x] 프롬프트 전면 강화 (가독성, IME, 인터페이스 계약)
- [x] CodeMerger 안전망 (중복 제거, 클래스 스트립)
- [x] QA 에이전트 오탐 방지 (정적 분석 기준 명확화)
- [ ] 새 게임으로 강화된 프롬프트 E2E 검증
- [ ] 생성 실패율 통계 수집 및 분석

### Phase 2 — 게임 다양성 확장

- [ ] **게임 템플릿 시스템**: 장르별 베이스 코드 제공으로 AI 생성 품질 보조
- [ ] **에셋 지원**: 스프라이트, 사운드 파일 업로드 후 게임에 통합
- [ ] **모바일 터치 지원**: 터치 이벤트 기반 조작 게임 생성

### Phase 3 — 플랫폼 기능

- [ ] **게임 공유**: 생성된 게임을 퍼블릭 URL로 공유
- [ ] **게임 갤러리**: 공개 게임 목록 + 플레이 카운트
- [ ] **수정 요청 고도화**: 게임 내 특정 요소(색상, 난이도) 핀포인트 수정
- [ ] **버전 관리**: 수정 이력 보존 + 특정 버전으로 롤백

### Phase 4 — AI 고도화

- [ ] **멀티 모달**: 스케치 이미지를 입력받아 게임 UI로 구현
- [ ] **에이전트 메모리**: 이전 성공 패턴 학습 및 재활용
- [ ] **자동 플레이테스트**: Headless 브라우저로 실제 게임 실행 검증

<br>

---

## 18. 로컬 실행 방법

### 사전 요구사항

| 도구 | 버전 | 확인 방법 |
|------|------|---------|
| Java | 17+ | `java -version` |
| Maven | 3.6+ | `mvn -version` |
| Node.js | 20+ | `node -version` |
| MySQL | 8.x | `mysql --version` |
| Anthropic API Key | - | [console.anthropic.com](https://console.anthropic.com) |

### 1단계: 데이터베이스 설정

```sql
-- MySQL 접속 후 실행
CREATE DATABASE noriter CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'noriter'@'localhost' IDENTIFIED BY 'yourpassword';
GRANT ALL PRIVILEGES ON noriter.* TO 'noriter'@'localhost';
FLUSH PRIVILEGES;
```

### 2단계: 백엔드 설정

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/noriter?serverTimezone=UTC&characterEncoding=UTF-8
    username: noriter
    password: yourpassword
  jpa:
    hibernate:
      ddl-auto: update    # 최초 실행 시 테이블 자동 생성
```

```bash
cd backend
mvn spring-boot:run
# 서버 시작: http://localhost:8080
# 헬스체크: http://localhost:8080/actuator/health
```

### 3단계: 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
# 개발 서버: http://localhost:5173
```

### 4단계: API 키 등록

앱 접속 → 우상단 설정(⚙) → Anthropic API Key 입력

또는 REST API 직접 호출:

```bash
curl -X PUT http://localhost:8080/api/settings/api-key \
  -H "Content-Type: application/json" \
  -d '{"apiKey": "sk-ant-..."}'
```

### 5단계: 게임 생성 테스트

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "내 첫 게임",
    "requirement": "화면에서 떨어지는 과일을 바구니로 받는 캐주얼 게임",
    "genre": "ARCADE"
  }'
```

응답으로 받은 `projectId`로 SSE 스트림 연결:

```bash
curl -N http://localhost:8080/api/projects/{projectId}/stream
```

<br>

---

## 19. 프로젝트 구조

```
workspace_NORITER/
│
├── backend/                               # Spring Boot 3.4.4
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/noriter/
│       │   ├── agent/
│       │   │   ├── core/                  # BaseAgent, AgentContext, AgentResult
│       │   │   ├── impl/                  # 7개 에이전트 구현체
│       │   │   │   ├── PlanningAgent.java
│       │   │   │   ├── ContentAgent.java
│       │   │   │   ├── CtoAgent.java
│       │   │   │   ├── DesignAgent.java
│       │   │   │   ├── FrontendAgent.java
│       │   │   │   ├── BackendAgent.java
│       │   │   │   └── QaAgent.java
│       │   │   ├── pipeline/
│       │   │   │   ├── PipelineOrchestrator.java  ← 핵심: 7단계 제어
│       │   │   │   ├── StageExecutor.java
│       │   │   │   └── CodeMerger.java            ← 코드 자동 병합
│       │   │   ├── prompt/                # PromptRegistry, PromptTemplate
│       │   │   └── message/               # MessageBus
│       │   ├── auth/                      # JWT 인증 (Spring Security)
│       │   ├── controller/                # REST API 컨트롤러 + DTO
│       │   ├── domain/                    # JPA 엔티티 + Enum 12개
│       │   ├── service/                   # 비즈니스 로직 8개
│       │   ├── repository/                # Spring Data JPA 8개
│       │   ├── infrastructure/            # Claude API / SSE / 파일 / 암호화
│       │   ├── exception/                 # NoriterException, ErrorCode 30+
│       │   ├── config/                    # Async, CORS, Security
│       │   └── util/                      # IdGenerator, JsSyntaxValidator
│       ├── main/resources/
│       │   ├── templates/                 # 24개 AI 프롬프트 파일
│       │   ├── application.yml
│       │   └── log4j2.xml
│       └── test/                          # 단위 + 통합 테스트 25개+ 클래스
│
├── frontend/                              # React 19 + TypeScript + Vite
│   ├── package.json
│   └── src/
│       ├── components/
│       │   ├── home/                      # HeroSection, Playground, PipelinePreview
│       │   ├── modal/                     # CreateGame, ProjectList, AuditLog, Settings, Auth
│       │   ├── project/                   # ProjectDetail, PreviewTab, SourceTab, LogTab
│       │   └── common/                    # Badge, FilterBar, ThemeToggle
│       ├── hooks/                         # useAuth, useProjects, useSse, useGameFiles
│       ├── api/                           # projectApi, gameApi, logApi, authApi
│       └── types/                         # TypeScript 타입 정의
│
├── docs/                                  # 설계 문서 9개 (기획~배포)
│   ├── 01_프로젝트_개요서.md
│   ├── 02_기능_요구사항_정의서.md
│   ├── 03_시스템_아키텍처_설계서.md
│   ├── 04_에이전트_역할_정의서.md
│   ├── 05_API_설계서.md
│   ├── 06_데이터베이스_설계서.md
│   ├── 07_화면_설계서.md
│   ├── 08_에이전트_프롬프트_설계서.md
│   └── 09_프로젝트_디렉토리_구조서.md
│
└── wireframe/                             # 초기 UI 와이어프레임
```

<br>

---

## 기술적 도전 요약

| 도전 | 접근 방식 | 결과 |
|------|----------|------|
| AI 출력의 비결정성 | 섹션 블록 파싱 + 재시도 + CodeMerger 안전망 | 안정적 파싱 |
| 에이전트 간 계약 유지 | CTO 아키텍처 JSON 인터페이스 명세 + 프롬프트 강제 | 런타임 오류 최소화 |
| 브라우저 iframe 보안 | postMessage 릴레이 패턴 | 키보드 이벤트 정상 전달 |
| Chrome 한글 IME | compositionend 음절 누적 + isComposing 이중 가드 | 한글 타이핑 정상 작동 |
| 실시간 UX | SSE + React Query `invalidateQueries` 연계 | 파이프라인 진행 즉시 반영 |
| 비용 절감 | 실패 단계만 재실행 + 토큰 사용량 추적 + claude-haiku | 게임 1개 약 $0.01~$0.02 |
| QA 오탐 | 정적 분석 전용 + severity 기준 명확화 + benefit of doubt | 불필요한 디버깅 루프 감소 |
| 코드 중복 생성 | `stripClass` + `deduplicateClasses` (중괄호 depth 추적) | 중복 클래스 완전 제거 |

<br>

---

> **NoriTer**는 "AI가 실제로 소프트웨어를 만들 수 있는가"라는 질문에 대한 실험적 답변입니다.
>
> 단순히 LLM에게 코드를 요청하는 수준을 넘어, **전문화된 에이전트들이 각자의 역할을 맡아 협업하는 자율 시스템**을 구현했습니다. 이 과정에서 AI 에이전트 설계, 프롬프트 엔지니어링, 브라우저 플랫폼 특성, 실시간 스트리밍 등 다양한 기술적 도전을 경험하고 해결했습니다.

<br>

---

*Java 17 · Spring Boot 3.4.4 · React 19 · TypeScript · TanStack Query · Tailwind CSS · MySQL · Claude API · SSE · JWT*
