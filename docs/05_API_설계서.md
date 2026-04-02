# 놀이터 (NoriTer) - API 설계서

> **문서 버전**: v1.0
> **작성일**: 2026-03-24
> **상태**: 초안

---

## 1. API 설계 원칙

| 항목 | 원칙 |
|------|------|
| **프로토콜** | REST (JSON), SSE (실시간 스트리밍) |
| **Base URL** | `/api` |
| **인코딩** | UTF-8 |
| **날짜 형식** | ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss`) |
| **에러 형식** | 공통 에러 응답 구조 (§8) |
| **페이징** | `page` (0-based), `size` (기본 20) |

---

## 2. API ID 체계

```
API-[컨트롤러]-[번호]

컨트롤러:
  PRJ  = ProjectController     (03_아키텍처 §6)
  LOG  = LogController          (03_아키텍처 §6)
  GAM  = GameController         (03_아키텍처 §6)
  SET  = SettingsController     (03_아키텍처 §6)
  SSE  = SSE 스트리밍            (03_아키텍처 §5.2)
```

---

## 3. API 목록 총괄

| API ID | Method | Endpoint | 설명 | 관련 기능 |
|--------|--------|----------|------|-----------|
| API-PRJ-001 | POST | /api/projects | 게임 생성 요청 | NT-PRJ-001 |
| API-PRJ-002 | GET | /api/projects | 프로젝트 목록 조회 | NT-PRJ-002 |
| API-PRJ-003 | GET | /api/projects/{id} | 프로젝트 상세 조회 | NT-PRJ-003 |
| API-PRJ-004 | POST | /api/projects/{id}/retry | 프로젝트 재시도 | NT-PRJ-004 |
| API-PRJ-005 | DELETE | /api/projects/{id} | 프로젝트 삭제 | NT-PRJ-005 |
| API-PRJ-006 | POST | /api/projects/{id}/feedback | 수정 요청 | NT-PRJ-006 |
| API-PRJ-007 | POST | /api/projects/{id}/cancel | 파이프라인 중단 | NT-AGT-006 |
| API-LOG-001 | GET | /api/projects/{id}/logs | 프로젝트 로그 조회 | NT-MON-001, NT-MON-004 |
| API-LOG-002 | GET | /api/projects/{id}/messages | 에이전트 대화 로그 | NT-MON-003 |
| API-LOG-003 | GET | /api/audit-logs | 감사 로그 조회 | NT-MON-005 |
| API-LOG-004 | GET | /api/error-codes/{code} | 에러 코드 상세 | NT-MON-006 |
| API-GAM-001 | GET | /api/projects/{id}/preview | 게임 미리보기 HTML | NT-GAM-001 |
| API-GAM-002 | GET | /api/projects/{id}/source | 게임 소스 코드 목록 | NT-GAM-002 |
| API-GAM-003 | GET | /api/projects/{id}/source/{path} | 소스 파일 내용 | NT-GAM-002 |
| API-GAM-004 | GET | /api/projects/{id}/download | 게임 ZIP 다운로드 | NT-GAM-003 |
| API-SET-001 | GET | /api/settings/api-key | API 키 조회 (마스킹) | NT-SYS-001 |
| API-SET-002 | PUT | /api/settings/api-key | API 키 설정/변경 | NT-SYS-001 |
| API-SET-003 | POST | /api/settings/api-key/validate | API 키 유효성 검증 | NT-SYS-001 |
| API-SYS-001 | GET | /api/health | 시스템 헬스체크 | NT-SYS-002 |
| API-SSE-001 | GET | /api/projects/{id}/stream | 실시간 이벤트 스트림 | NT-MON-001, NT-MON-002 |

---

## 4. API 상세 정의

---

### API-PRJ-001: 게임 생성 요청

| 항목 | 내용 |
|------|------|
| **Method** | POST |
| **URL** | `/api/projects` |
| **관련 기능** | NT-PRJ-001 |
| **컨트롤러** | ProjectController (03_아키텍처 §6) |
| **서비스** | ProjectService → PipelineOrchestrator.startPipeline() |

#### Request Body

```json
{
  "name": "뱀파이어 서바이벌",          // 선택, 미입력 시 자동 생성
  "requirement": "뱀파이어 서바이벌 류 미니게임을 만들어줘. 사방에서 몬스터가 몰려오고 자동 공격으로 처치하는 게임",  // 필수, 최소 10자
  "genre": "ACTION"                   // 선택, enum: PUZZLE, ACTION, ARCADE, SHOOTING, STRATEGY, OTHER
}
```

#### Validation

| 필드 | 규칙 | 에러 코드 |
|------|------|-----------|
| requirement | 필수, 최소 10자 | NT-ERR-V001 |
| name | 최대 100자 | NT-ERR-V002 |
| genre | enum 값만 허용 | NT-ERR-V003 |

#### Response — 201 Created

```json
{
  "id": "prj_a1b2c3d4",
  "name": "뱀파이어 서바이벌",
  "requirement": "뱀파이어 서바이벌 류 미니게임을 만들어줘...",
  "genre": "ACTION",
  "status": "CREATED",
  "currentStage": null,
  "progress": 0,
  "createdAt": "2026-03-24T14:30:00"
}
```

#### Response — 400 Bad Request

```json
{
  "error": {
    "code": "NT-ERR-V001",
    "message": "요구사항은 최소 10자 이상 입력해야 합니다.",
    "field": "requirement"
  }
}
```

#### Response — 503 Service Unavailable

```json
{
  "error": {
    "code": "NT-ERR-S001",
    "message": "API 키가 설정되지 않았습니다. 설정 페이지에서 API 키를 등록해주세요."
  }
}
```

---

### API-PRJ-002: 프로젝트 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects` |
| **관련 기능** | NT-PRJ-002 |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| status | String | X | 전체 | 필터: CREATED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED, REVISION |
| page | int | X | 0 | 페이지 번호 (0-based) |
| size | int | X | 20 | 페이지 크기 |
| sort | String | X | createdAt,desc | 정렬 |

#### Response — 200 OK

```json
{
  "content": [
    {
      "id": "prj_a1b2c3d4",
      "name": "뱀파이어 서바이벌",
      "status": "IN_PROGRESS",
      "genre": "ACTION",
      "currentStage": "IMPLEMENTATION",
      "progress": 57,
      "createdAt": "2026-03-24T14:30:00",
      "completedAt": null
    },
    {
      "id": "prj_e5f6g7h8",
      "name": "테트리스 클론",
      "status": "COMPLETED",
      "genre": "PUZZLE",
      "currentStage": "RELEASE",
      "progress": 100,
      "createdAt": "2026-03-24T10:00:00",
      "completedAt": "2026-03-24T10:25:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

---

### API-PRJ-003: 프로젝트 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}` |
| **관련 기능** | NT-PRJ-003 |

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | String | 프로젝트 ID |

#### Response — 200 OK

```json
{
  "id": "prj_a1b2c3d4",
  "name": "뱀파이어 서바이벌",
  "requirement": "뱀파이어 서바이벌 류 미니게임을 만들어줘...",
  "genre": "ACTION",
  "status": "IN_PROGRESS",
  "progress": 57,
  "createdAt": "2026-03-24T14:30:00",
  "completedAt": null,
  "stages": [
    {
      "type": "PLANNING",
      "status": "COMPLETED",
      "agentRole": "PLANNING",
      "startedAt": "2026-03-24T14:30:01",
      "completedAt": "2026-03-24T14:31:15",
      "artifactId": "art_001"
    },
    {
      "type": "ARCHITECTURE",
      "status": "COMPLETED",
      "agentRole": "CTO",
      "startedAt": "2026-03-24T14:31:16",
      "completedAt": "2026-03-24T14:32:00",
      "artifactId": "art_002"
    },
    {
      "type": "DESIGN",
      "status": "COMPLETED",
      "agentRole": "DESIGN",
      "startedAt": "2026-03-24T14:32:01",
      "completedAt": "2026-03-24T14:33:10",
      "artifactId": "art_003"
    },
    {
      "type": "IMPLEMENTATION",
      "status": "IN_PROGRESS",
      "agentRole": "FRONTEND,BACKEND",
      "startedAt": "2026-03-24T14:33:11",
      "completedAt": null,
      "artifactId": null
    },
    {
      "type": "QA",
      "status": "PENDING",
      "agentRole": "QA",
      "startedAt": null,
      "completedAt": null,
      "artifactId": null
    },
    {
      "type": "RELEASE",
      "status": "PENDING",
      "agentRole": "SYSTEM",
      "startedAt": null,
      "completedAt": null,
      "artifactId": null
    }
  ],
  "artifacts": [
    {
      "id": "art_001",
      "type": "PLAN",
      "agentRole": "PLANNING",
      "version": 1,
      "createdAt": "2026-03-24T14:31:15"
    },
    {
      "id": "art_002",
      "type": "ARCHITECTURE",
      "agentRole": "CTO",
      "version": 1,
      "createdAt": "2026-03-24T14:32:00"
    },
    {
      "id": "art_003",
      "type": "DESIGN",
      "agentRole": "DESIGN",
      "version": 1,
      "createdAt": "2026-03-24T14:33:10"
    }
  ],
  "debugAttempts": 0,
  "maxDebugAttempts": 3,
  "tokenUsage": {
    "total": 12500,
    "byAgent": {
      "PLANNING": 3200,
      "CTO": 2800,
      "DESIGN": 3500,
      "FRONTEND": 1500,
      "BACKEND": 1500
    }
  }
}
```

#### Response — 404 Not Found

```json
{
  "error": {
    "code": "NT-ERR-P001",
    "message": "프로젝트를 찾을 수 없습니다.",
    "detail": "id: prj_invalid"
  }
}
```

---

### API-PRJ-004: 프로젝트 재시도

| 항목 | 내용 |
|------|------|
| **Method** | POST |
| **URL** | `/api/projects/{id}/retry` |
| **관련 기능** | NT-PRJ-004 |
| **선행 조건** | 프로젝트 상태 = FAILED |

#### Request Body

```json
{
  "fromStage": "IMPLEMENTATION"   // 선택, 미입력 시 처음부터
}
```

| fromStage 값 | 설명 |
|--------------|------|
| null (미입력) | STAGE 1부터 전체 재실행 |
| "PLANNING" | 기획부터 재실행 |
| "ARCHITECTURE" | CTO 검토부터 재실행 |
| "DESIGN" | 디자인부터 재실행 |
| "IMPLEMENTATION" | 구현부터 재실행 |
| "QA" | QA부터 재실행 |

#### Response — 200 OK

```json
{
  "id": "prj_a1b2c3d4",
  "status": "IN_PROGRESS",
  "currentStage": "IMPLEMENTATION",
  "message": "IMPLEMENTATION 스테이지부터 재시도합니다."
}
```

#### Response — 409 Conflict

```json
{
  "error": {
    "code": "NT-ERR-P002",
    "message": "재시도는 실패(FAILED) 상태의 프로젝트만 가능합니다.",
    "detail": "현재 상태: IN_PROGRESS"
  }
}
```

---

### API-PRJ-005: 프로젝트 삭제

| 항목 | 내용 |
|------|------|
| **Method** | DELETE |
| **URL** | `/api/projects/{id}` |
| **관련 기능** | NT-PRJ-005 |
| **제약** | 진행 중(IN_PROGRESS) 상태에서는 삭제 불가 |

#### Response — 204 No Content

(본문 없음)

#### Response — 409 Conflict

```json
{
  "error": {
    "code": "NT-ERR-P003",
    "message": "진행 중인 프로젝트는 삭제할 수 없습니다. 먼저 중단해주세요."
  }
}
```

---

### API-PRJ-006: 수정 요청 (피드백)

| 항목 | 내용 |
|------|------|
| **Method** | POST |
| **URL** | `/api/projects/{id}/feedback` |
| **관련 기능** | NT-PRJ-006 |
| **선행 조건** | 프로젝트 상태 = COMPLETED |
| **서비스** | PipelineOrchestrator → CTO 에이전트가 수정 범위 판단 (04_에이전트 §2.1) |

#### Request Body

```json
{
  "feedback": "몬스터 스폰 속도가 너무 빨라서 시작하자마자 죽어요. 초반 난이도를 낮춰주세요."
}
```

#### Validation

| 필드 | 규칙 | 에러 코드 |
|------|------|-----------|
| feedback | 필수, 최소 5자 | NT-ERR-V004 |

#### Response — 200 OK

```json
{
  "id": "prj_a1b2c3d4",
  "status": "REVISION",
  "message": "수정 요청이 접수되었습니다. CTO가 수정 범위를 판단 중입니다."
}
```

#### Response — 409 Conflict

```json
{
  "error": {
    "code": "NT-ERR-P004",
    "message": "수정 요청은 완료(COMPLETED) 상태의 프로젝트만 가능합니다."
  }
}
```

---

### API-PRJ-007: 파이프라인 중단

| 항목 | 내용 |
|------|------|
| **Method** | POST |
| **URL** | `/api/projects/{id}/cancel` |
| **관련 기능** | NT-AGT-006 |
| **서비스** | PipelineOrchestrator.cancelPipeline() (03_아키텍처 §3.4) |

#### Response — 200 OK

```json
{
  "id": "prj_a1b2c3d4",
  "status": "CANCELLED",
  "message": "파이프라인이 중단되었습니다. 기존 산출물은 보존됩니다."
}
```

#### Response — 409 Conflict

```json
{
  "error": {
    "code": "NT-ERR-P005",
    "message": "중단은 진행 중(IN_PROGRESS/REVISION) 상태의 프로젝트만 가능합니다."
  }
}
```

---

### API-LOG-001: 프로젝트 로그 조회

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}/logs` |
| **관련 기능** | NT-MON-001 (로그 스트리밍의 히스토리 조회), NT-MON-004 (에러 로그) |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| level | String | X | 전체 | 필터: INFO, AGENT, WARN, ERROR, AUDIT, DEBUG |
| agentRole | String | X | 전체 | 필터: CTO, PLANNING, DESIGN, FRONTEND, BACKEND, QA |
| page | int | X | 0 | 페이지 |
| size | int | X | 50 | 페이지 크기 |

> 로그 레벨: 04_에이전트 §4 참조, NT-MON-001 로그 종류와 일치

#### Response — 200 OK

```json
{
  "content": [
    {
      "id": "log_001",
      "level": "INFO",
      "agentRole": "PLANNING",
      "stage": "PLANNING",
      "message": "기획팀이 게임 기획서 작성을 시작합니다.",
      "timestamp": "2026-03-24T14:30:01"
    },
    {
      "id": "log_002",
      "level": "AGENT",
      "agentRole": "PLANNING",
      "stage": "PLANNING",
      "message": "요구사항 분석 완료. 액션/서바이벌 장르로 기획서를 작성합니다.",
      "timestamp": "2026-03-24T14:30:15"
    },
    {
      "id": "log_003",
      "level": "ERROR",
      "agentRole": "FRONTEND",
      "stage": "IMPLEMENTATION",
      "message": "Claude API 호출 실패: Rate Limit Exceeded",
      "errorCode": "NT-ERR-A001",
      "stackTrace": "...",
      "resolved": true,
      "resolvedBy": "AUTO_RETRY",
      "timestamp": "2026-03-24T14:33:45"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 3,
  "totalPages": 1
}
```

---

### API-LOG-002: 에이전트 대화 로그 조회

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}/messages` |
| **관련 기능** | NT-MON-003, NT-AGT-002 |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| agentRole | String | X | 전체 | 특정 에이전트 발신/수신 필터 |

#### Response — 200 OK

```json
{
  "messages": [
    {
      "id": "msg_001",
      "fromAgent": "PLANNING",
      "toAgent": "CTO",
      "type": "HANDOFF",
      "content": "게임 기획서 작성을 완료했습니다. 검토 부탁드립니다.",
      "artifactRef": "art_001",
      "timestamp": "2026-03-24T14:31:15"
    },
    {
      "id": "msg_002",
      "fromAgent": "CTO",
      "toAgent": "DESIGN",
      "type": "HANDOFF",
      "content": "아키텍처 결정 완료. Canvas 2D 기반으로 진행합니다. 디자인 스펙 작성 부탁합니다.",
      "artifactRef": "art_002",
      "timestamp": "2026-03-24T14:32:00"
    }
  ]
}
```

> 메시지 타입 4종: HANDOFF, REVIEW_REQUEST, BUG_REPORT, FEEDBACK (04_에이전트 §3, NT-AGT-002)

---

### API-LOG-003: 감사 로그 조회

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/audit-logs` |
| **관련 기능** | NT-MON-005 |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| startDate | String | X | - | 시작일 (ISO 8601) |
| endDate | String | X | - | 종료일 (ISO 8601) |
| eventType | String | X | 전체 | PROJECT_CREATED, PROJECT_DELETED, STAGE_STARTED, STAGE_COMPLETED, STAGE_FAILED, ARTIFACT_CREATED, USER_ACTION, SETTING_CHANGED |
| projectId | String | X | 전체 | 특정 프로젝트 필터 |
| page | int | X | 0 | 페이지 |
| size | int | X | 50 | 페이지 크기 |

> 감사 로그 보존: 최소 90일 (NFR-03)

#### Response — 200 OK

```json
{
  "content": [
    {
      "id": "audit_001",
      "eventType": "PROJECT_CREATED",
      "projectId": "prj_a1b2c3d4",
      "description": "프로젝트 '뱀파이어 서바이벌' 생성",
      "detail": {
        "projectName": "뱀파이어 서바이벌",
        "genre": "ACTION"
      },
      "timestamp": "2026-03-24T14:30:00"
    },
    {
      "id": "audit_002",
      "eventType": "STAGE_COMPLETED",
      "projectId": "prj_a1b2c3d4",
      "description": "PLANNING 스테이지 완료 (AGT-PLAN)",
      "detail": {
        "stage": "PLANNING",
        "agentRole": "PLANNING",
        "duration": "74초",
        "tokensUsed": 3200
      },
      "timestamp": "2026-03-24T14:31:15"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 2,
  "totalPages": 1
}
```

---

### API-LOG-004: 에러 코드 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/error-codes/{code}` |
| **관련 기능** | NT-MON-006 |
| **데이터 소스** | 정적 매핑 (03_아키텍처 §9: ErrorCodeRegistry) |

#### Response — 200 OK

```json
{
  "code": "NT-ERR-A001",
  "name": "Claude API Rate Limit",
  "description": "Claude API 호출 빈도 제한에 도달했습니다.",
  "cause": "단시간에 너무 많은 API 호출이 발생한 경우",
  "resolution": "자동 재시도(지수 백오프)가 수행됩니다. 지속적으로 발생하면 파이프라인 동시 실행 수를 줄여주세요.",
  "category": "API",
  "severity": "WARN"
}
```

---

### API-GAM-001: 게임 미리보기

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}/preview` |
| **관련 기능** | NT-GAM-001 |
| **선행 조건** | 프로젝트 상태 = COMPLETED 또는 game 파일 존재 |
| **응답 타입** | text/html |

#### Response — 200 OK

```
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
  <head>...</head>
  <body>
    <!-- 생성된 게임 HTML 전체 -->
  </body>
</html>
```

> 프론트엔드에서 iframe src로 이 URL 사용 (NT-GAM-001: iframe 내에 생성된 HTML 게임 로드)

#### Response — 404 Not Found

```json
{
  "error": {
    "code": "NT-ERR-G001",
    "message": "게임 파일이 아직 생성되지 않았습니다."
  }
}
```

---

### API-GAM-002: 게임 소스 코드 목록

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}/source` |
| **관련 기능** | NT-GAM-002 |

#### Response — 200 OK

```json
{
  "files": [
    { "path": "index.html", "size": 2048, "type": "html" },
    { "path": "style.css", "size": 1024, "type": "css" },
    { "path": "game.js", "size": 8192, "type": "javascript" }
  ]
}
```

---

### API-GAM-003: 소스 파일 내용 조회

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}/source/{path}` |
| **관련 기능** | NT-GAM-002 |
| **응답 타입** | text/plain |

#### Response — 200 OK

```
Content-Type: text/plain; charset=UTF-8

// game.js 내용...
class Game {
  constructor() { ... }
}
```

---

### API-GAM-004: 게임 ZIP 다운로드

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}/download` |
| **관련 기능** | NT-GAM-003 |
| **서비스** | FileStorageService.packageAsZip() (03_아키텍처 §5.3) |
| **응답 타입** | application/zip |

#### Response — 200 OK

```
Content-Type: application/zip
Content-Disposition: attachment; filename="뱀파이어_서바이벌.zip"

(ZIP 바이너리)
```

> ZIP 포함: 게임 소스 코드 + README.md (NT-GAM-003)

---

### API-SET-001: API 키 조회 (마스킹)

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/settings/api-key` |
| **관련 기능** | NT-SYS-001 |

#### Response — 200 OK

```json
{
  "configured": true,
  "maskedKey": "sk-ant-a********...****",
  "updatedAt": "2026-03-24T10:00:00"
}
```

> 마스킹: 앞 8자리만 노출 (NT-SYS-001 보안 요구사항)

---

### API-SET-002: API 키 설정/변경

| 항목 | 내용 |
|------|------|
| **Method** | PUT |
| **URL** | `/api/settings/api-key` |
| **관련 기능** | NT-SYS-001 |

#### Request Body

```json
{
  "apiKey": "sk-ant-api03-..."
}
```

#### Response — 200 OK

```json
{
  "configured": true,
  "maskedKey": "sk-ant-a********...****",
  "validated": true,
  "updatedAt": "2026-03-24T14:00:00"
}
```

> API 키는 암호화하여 저장 (NT-SYS-001, NFR-05)
> 설정 변경 시 감사 로그 기록 (NT-MON-005: SETTING_CHANGED)

---

### API-SET-003: API 키 유효성 검증

| 항목 | 내용 |
|------|------|
| **Method** | POST |
| **URL** | `/api/settings/api-key/validate` |
| **관련 기능** | NT-SYS-001 |

#### Request Body

```json
{
  "apiKey": "sk-ant-api03-..."
}
```

#### Response — 200 OK

```json
{
  "valid": true,
  "model": "claude-sonnet-4-6",
  "message": "API 키가 유효합니다."
}
```

#### Response — 200 OK (유효하지 않음)

```json
{
  "valid": false,
  "message": "API 키가 유효하지 않습니다. 키를 확인해주세요."
}
```

---

### API-SYS-001: 시스템 헬스체크

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/health` |
| **관련 기능** | NT-SYS-002 |
| **구현** | Spring Actuator 기반 (03_아키텍처 §9) |

#### Response — 200 OK

```json
{
  "status": "UP",
  "components": {
    "server": { "status": "UP" },
    "database": { "status": "UP" },
    "claudeApi": { "status": "UP", "configured": true },
    "activePipelines": { "count": 1 }
  },
  "timestamp": "2026-03-24T14:30:00"
}
```

---

### API-SSE-001: 실시간 이벤트 스트림

| 항목 | 내용 |
|------|------|
| **Method** | GET |
| **URL** | `/api/projects/{id}/stream` |
| **관련 기능** | NT-MON-001, NT-MON-002 |
| **프로토콜** | SSE (Server-Sent Events) |
| **구현** | SseEmitterService (03_아키텍처 §5.2) |
| **Content-Type** | text/event-stream |

#### SSE 이벤트 타입

> 03_아키텍처 §5.2에서 정의된 이벤트 타입과 동일

| 이벤트 타입 | 설명 | 관련 기능 |
|-------------|------|-----------|
| log | 실시간 로그 | NT-MON-001 |
| stage-update | 스테이지 상태 변경 | NT-MON-002 |
| agent-msg | 에이전트 간 메시지 | NT-MON-003 |
| error | 에러 발생 | NT-MON-004 |
| complete | 파이프라인 완료 | NT-AGT-001 |
| cancelled | 파이프라인 취소 | NT-AGT-006 |

#### SSE 이벤트 예시

```
event: log
data: {"level":"INFO","agentRole":"PLANNING","message":"기획팀이 게임 기획서 작성을 시작합니다.","timestamp":"2026-03-24T14:30:01"}

event: stage-update
data: {"stage":"PLANNING","status":"COMPLETED","progress":14,"timestamp":"2026-03-24T14:31:15"}

event: agent-msg
data: {"fromAgent":"PLANNING","toAgent":"CTO","type":"HANDOFF","content":"기획서 작성 완료. 검토 부탁드립니다.","timestamp":"2026-03-24T14:31:15"}

event: error
data: {"code":"NT-ERR-A001","agentRole":"FRONTEND","message":"Claude API Rate Limit","resolved":false,"timestamp":"2026-03-24T14:33:45"}

event: complete
data: {"projectId":"prj_a1b2c3d4","status":"COMPLETED","totalDuration":"25분 30초","timestamp":"2026-03-24T14:55:30"}
```

---

## 5. 프로젝트 상태별 허용 API

> 참조: 03_아키텍처 §4.1 프로젝트 상태 전이도

| API | CREATED | IN_PROGRESS | COMPLETED | REVISION | FAILED | CANCELLED |
|-----|---------|-------------|-----------|----------|--------|-----------|
| PRJ-003 상세 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| PRJ-004 재시도 | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| PRJ-005 삭제 | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ |
| PRJ-006 수정요청 | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| PRJ-007 중단 | ❌ | ✅ | ❌ | ✅ | ❌ | ❌ |
| GAM-001 미리보기 | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ |
| GAM-004 다운로드 | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ |
| SSE-001 스트림 | ❌ | ✅ | ❌ | ✅ | ❌ | ❌ |

---

## 6. 공통 응답 구조

### 성공 응답

단건 조회는 객체 직접 반환, 목록 조회는 페이징 래퍼 사용.

```json
// 페이징 래퍼
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

### 에러 응답

```json
{
  "error": {
    "code": "NT-ERR-XXXX",       // 에러 코드 (§7 참조)
    "message": "사용자 표시용 메시지",
    "detail": "개발자용 상세 정보",  // 선택
    "field": "에러 필드명",         // 선택, 유효성 검증 에러 시
    "timestamp": "2026-03-24T14:30:00"
  }
}
```

---

## 7. HTTP 상태 코드 사용

| 코드 | 사용 |
|------|------|
| 200 | 조회·수정 성공 |
| 201 | 생성 성공 (API-PRJ-001) |
| 204 | 삭제 성공 (API-PRJ-005) |
| 400 | 유효성 검증 실패 |
| 404 | 리소스 없음 |
| 409 | 상태 충돌 (허용되지 않는 상태에서의 작업) |
| 500 | 서버 내부 에러 |
| 503 | 서비스 불가 (API 키 미설정 등) |

---

## 8. 에러 코드 체계

```
NT-ERR-[카테고리][번호]

카테고리:
  V = Validation (유효성 검증)
  P = Project (프로젝트)
  A = Agent/API (에이전트·외부 API)
  G = Game (게임 출력)
  S = System (시스템)
```

| 에러 코드 | HTTP | 메시지 | 발생 상황 |
|-----------|------|--------|-----------|
| NT-ERR-V001 | 400 | 요구사항은 최소 10자 이상 입력해야 합니다 | PRJ-001 requirement 검증 |
| NT-ERR-V002 | 400 | 프로젝트명은 100자를 초과할 수 없습니다 | PRJ-001 name 검증 |
| NT-ERR-V003 | 400 | 유효하지 않은 장르입니다 | PRJ-001 genre 검증 |
| NT-ERR-V004 | 400 | 수정 요청은 최소 5자 이상 입력해야 합니다 | PRJ-006 feedback 검증 |
| NT-ERR-P001 | 404 | 프로젝트를 찾을 수 없습니다 | 존재하지 않는 프로젝트 ID |
| NT-ERR-P002 | 409 | 재시도는 실패 상태의 프로젝트만 가능합니다 | PRJ-004 상태 불일치 |
| NT-ERR-P003 | 409 | 진행 중인 프로젝트는 삭제할 수 없습니다 | PRJ-005 상태 불일치 |
| NT-ERR-P004 | 409 | 수정 요청은 완료 상태의 프로젝트만 가능합니다 | PRJ-006 상태 불일치 |
| NT-ERR-P005 | 409 | 중단은 진행 중 상태의 프로젝트만 가능합니다 | PRJ-007 상태 불일치 |
| NT-ERR-A001 | - | Claude API Rate Limit 초과 | API 호출 빈도 제한 |
| NT-ERR-A002 | - | Claude API 인증 실패 | 잘못된 API 키 |
| NT-ERR-A003 | - | Claude API 타임아웃 | 응답 지연 |
| NT-ERR-A004 | - | 에이전트 실행 실패 | 에이전트 내부 오류 |
| NT-ERR-A005 | - | 파이프라인 타임아웃 | 전체 30분 초과 |
| NT-ERR-G001 | 404 | 게임 파일이 아직 생성되지 않았습니다 | 미완성 프로젝트 미리보기 |
| NT-ERR-S001 | 503 | API 키가 설정되지 않았습니다 | API 키 미등록 상태 |
| NT-ERR-S002 | 500 | 내부 서버 오류가 발생했습니다 | 예상치 못한 서버 에러 |

---

## 9. 정합성 검증

### vs 02_기능_요구사항_정의서 (기능 → API 매핑)

| 기능 ID | 기능명 | API ID | 매핑 상태 |
|---------|--------|--------|-----------|
| NT-PRJ-001 | 게임 생성 요청 | API-PRJ-001 | ✅ |
| NT-PRJ-002 | 프로젝트 목록 조회 | API-PRJ-002 | ✅ |
| NT-PRJ-003 | 프로젝트 상세 조회 | API-PRJ-003 | ✅ |
| NT-PRJ-004 | 프로젝트 재시도 | API-PRJ-004 | ✅ |
| NT-PRJ-005 | 프로젝트 삭제 | API-PRJ-005 | ✅ |
| NT-PRJ-006 | 수정 요청 | API-PRJ-006 | ✅ |
| NT-AGT-001 | 파이프라인 실행 | API-PRJ-001 (트리거) | ✅ |
| NT-AGT-002 | 메시지 전달 | API-LOG-002 (조회) | ✅ |
| NT-AGT-003 | 상태 관리 | API-PRJ-003 (stages 포함) | ✅ |
| NT-AGT-004 | 산출물 저장 | API-PRJ-003 (artifacts 포함) | ✅ |
| NT-AGT-005 | 에러 복구 | 내부 로직 (API 불필요) | ✅ |
| NT-AGT-006 | 중단/취소 | API-PRJ-007 | ✅ |
| NT-MON-001 | 실시간 로그 | API-SSE-001 + API-LOG-001 | ✅ |
| NT-MON-002 | 진행률 표시 | API-SSE-001 (stage-update) | ✅ |
| NT-MON-003 | 대화 로그 | API-LOG-002 | ✅ |
| NT-MON-004 | 에러 로그 | API-LOG-001 (level=ERROR) | ✅ |
| NT-MON-005 | 감사 로그 | API-LOG-003 | ✅ |
| NT-MON-006 | 에러 코드 상세 | API-LOG-004 | ✅ |
| NT-GAM-001 | 미리보기 | API-GAM-001 | ✅ |
| NT-GAM-002 | 소스 코드 보기 | API-GAM-002 + API-GAM-003 | ✅ |
| NT-GAM-003 | 다운로드 | API-GAM-004 | ✅ |
| NT-SYS-001 | API 키 설정 | API-SET-001~003 | ✅ |
| NT-SYS-002 | 헬스체크 | API-SYS-001 | ✅ |

**22개 기능 → 20개 API 전체 매핑 완료 (누락 0건)**

### vs 03_아키텍처_설계서

| 아키텍처 항목 | API 대응 | 일치 |
|-------------|----------|------|
| ProjectController (§6) | API-PRJ-001~007 | ✅ |
| LogController (§6) | API-LOG-001~004 | ✅ |
| GameController (§6) | API-GAM-001~004 | ✅ |
| SettingsController (§6) | API-SET-001~003 | ✅ |
| SSE 엔드포인트 (§5.2) | API-SSE-001 | ✅ |
| SSE 이벤트 6종 (§5.2) | API-SSE-001 이벤트 타입 6종 | ✅ |
| 프로젝트 상태 전이 (§4.1) | §5 상태별 허용 API 매트릭스 | ✅ |
| FileStorage.packageAsZip (§5.3) | API-GAM-004 | ✅ |
| Spring Actuator /health (§9) | API-SYS-001 | ✅ |

### vs 04_에이전트_역할_정의서

| 에이전트 항목 | API 대응 | 일치 |
|-------------|----------|------|
| 산출물 파일명 (plan.json 등) | API-PRJ-003 artifacts | ✅ |
| 메시지 타입 4종 (§3) | API-LOG-002 메시지 조회 | ✅ |
| CTO 수정요청 판단 (§2.1) | API-PRJ-006 서비스 흐름 | ✅ |
| 로그 레벨 6종 (NT-MON-001) | API-LOG-001 level 필터 | ✅ |
| 에이전트 role 6종 | API-LOG-001 agentRole 필터 | ✅ |

**전 문서 정합성 검증 완료 (불일치 0건)**

---

## 10. 변경 이력

| 버전 | 일자 | 내용 |
|------|------|------|
| v1.0 | 2026-03-24 | 최초 작성 |
