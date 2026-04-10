import { useQuery } from '@tanstack/react-query';
import { getGameFiles, getGameSource } from '../api/gameApi';

interface SourceFile {
  name: string;
  content: string;
}

export function useGameFiles(projectId: string | null, enabled = true) {
  return useQuery<SourceFile[]>({
    queryKey: ['gameFiles', projectId],
    queryFn: async () => {
      if (!projectId) return [];
      const files = await getGameFiles(projectId);
      const sources = await Promise.all(
        files.map(async (f) => {
          try {
            const content = await getGameSource(projectId, f.name);
            return { name: f.name, content: typeof content === 'string' ? content : JSON.stringify(content, null, 2) };
          } catch {
            return { name: f.name, content: '// 파일을 불러올 수 없습니다.' };
          }
        })
      );
      return sources;
    },
    enabled: !!projectId && enabled,
  });
}
