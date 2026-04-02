import { useEffect, useRef, useCallback } from 'react';

type SseEventType = 'log' | 'stage-update' | 'agent-msg' | 'error' | 'complete' | 'cancelled';

interface UseSseOptions {
  projectId: string | null;
  onEvent: (type: SseEventType, data: unknown) => void;
}

export function useSse({ projectId, onEvent }: UseSseOptions) {
  const sourceRef = useRef<EventSource | null>(null);

  const connect = useCallback(() => {
    if (!projectId) return;
    const es = new EventSource(`/api/projects/${projectId}/sse`);

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

    sourceRef.current = es;
  }, [projectId, onEvent]);

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
