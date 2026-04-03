import { useQuery } from '@tanstack/react-query';
import { getAuditLogs } from '../api/logApi';

export function useAuditLogs(eventType?: string) {
  return useQuery({
    queryKey: ['auditLogs', eventType],
    queryFn: () => getAuditLogs(eventType),
  });
}
