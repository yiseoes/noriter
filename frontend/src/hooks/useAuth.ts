import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { login, signup, getMe } from '../api/authApi';
import { getToken, setToken, removeToken } from '../api/client';
import type { AuthUser } from '../types';

export function useAuth() {
  const queryClient = useQueryClient();
  const hasToken = !!getToken();

  const { data: user, isLoading } = useQuery<AuthUser>({
    queryKey: ['auth', 'me'],
    queryFn: getMe,
    enabled: hasToken,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      if (data.token) setToken(data.token);
      queryClient.setQueryData(['auth', 'me'], { userId: data.userId, email: data.email, name: data.name });
    },
  });

  const signupMutation = useMutation({
    mutationFn: signup,
    onSuccess: (data) => {
      if (data.token) setToken(data.token);
      queryClient.setQueryData(['auth', 'me'], { userId: data.userId, email: data.email, name: data.name });
    },
  });

  const logout = () => {
    removeToken();
    queryClient.setQueryData(['auth', 'me'], null);
    queryClient.invalidateQueries({ queryKey: ['projects'] });
  };

  return {
    user: user ?? null,
    isLoading: hasToken && isLoading,
    isLoggedIn: !!user,
    login: loginMutation,
    signup: signupMutation,
    logout,
  };
}
