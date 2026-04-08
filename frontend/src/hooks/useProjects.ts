import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getProjects, getProject, createProject, retryProject, deleteProject, cancelProject, sendFeedback } from '../api/projectApi';
import type { ProjectDetail } from '../types';

export function useProjects(status?: string) {
  return useQuery({
    queryKey: ['projects', status],
    queryFn: () => getProjects(status),
  });
}

export function useProject(id: string | null) {
  return useQuery<ProjectDetail>({
    queryKey: ['project', id],
    queryFn: () => getProject(id!) as Promise<ProjectDetail>,
    enabled: !!id,
  });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createProject,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  });
}

export function useDeleteProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteProject,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  });
}

export function useCancelProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: cancelProject,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  });
}

export function useRetryProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, fromStage }: { id: string; fromStage?: string }) => retryProject(id, fromStage),
    onSuccess: (_data, variables) => {
      qc.refetchQueries({ queryKey: ['projects'] });
      qc.refetchQueries({ queryKey: ['project', variables.id] });
    },
  });
}

export function useSendFeedback() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, feedback }: { id: string; feedback: string }) => sendFeedback(id, feedback),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  });
}
