import { useEffect, useRef, useCallback } from 'react';

type SseEventType = 'log' | 'stage-update' | 'agent-msg' | 'error' | 'complete' | 'cancelled';

interface UseSseOptions {
  projectId: string | null;
  onEvent: (type: SseEventType, data: unknown) => void;
  enabled?: boolean;
}

export function useSse({ projectId, onEvent, enabled = true }: UseSseOptions) {
  const sourceRef = useRef<EventSource | null>(null);

  const connect = useCallback(() => {
    if (!projectId || !enabled) return;

    // 이전 연결 정리
    sourceRef.current?.close();

    const es = new EventSource(`/api/projects/${projectId}/stream`);

    const eventTypes: SseEventType[] = ['log', 'stage-update', 'agent-msg', 'error', 'complete', 'cancelled'];
    eventTypes.forEach(type => {
      es.addEventListener(type, (e) => {
        try {
          onEvent(type, JSON.parse((e as MessageEvent).data));
        } catch {
          onEvent(type, (e as MessageEvent).data);
        }
      });
    });

    es.onerror = () => {
      // 연결 끊기면 3초 후 재시도
      es.close();
      setTimeout(connect, 3000);
    };

    sourceRef.current = es;
  }, [projectId, onEvent, enabled]);

  const disconnect = useCallback(() => {
    sourceRef.current?.close();
    sourceRef.current = null;
  }, []);

  useEffect(() => {
    connect();
    return disconnect;
  }, [connect, disconnect]);

  return { disconnect };
}
