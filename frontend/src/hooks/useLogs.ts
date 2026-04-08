import { useQuery } from '@tanstack/react-query';
import { getLogs, getMessages } from '../api/logApi';

export function useLogs(projectId: string | null) {
  return useQuery({
    queryKey: ['logs', projectId],
    queryFn: () => getLogs(projectId!),
    enabled: !!projectId,
  });
}

export function useMessages(projectId: string | null) {
  return useQuery({
    queryKey: ['messages', projectId],
    queryFn: () => getMessages(projectId!),
    enabled: !!projectId,
  });
}
