import { useState, useCallback } from 'react';
import IntroScreen from './components/intro/IntroScreen';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import CreateGameModal from './components/modal/CreateGameModal';
import ProjectListModal from './components/modal/ProjectListModal';
import AuditLogModal from './components/modal/AuditLogModal';
import SettingsModal from './components/modal/SettingsModal';
import AuthModal from './components/modal/AuthModal';
import SlideUpModal from './components/modal/SlideUpModal';
import ProjectDetail from './components/project/ProjectDetail';
import { useModal } from './hooks/useModal';
import { useTheme } from './hooks/useTheme';
import { useProjects, useCreateProject, useDeleteProject, useCancelProject } from './hooks/useProjects';
import type { Project } from './types';


export default function App() {
  const [showIntro, setShowIntro] = useState(true);
  const { theme, toggle: toggleTheme } = useTheme();
  const [activePage, setActivePage] = useState('home');
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [isDemo, setIsDemo] = useState(false);

  const createModal = useModal();
  const projectsModal = useModal();
  const auditModal = useModal();
  const settingsModal = useModal();
  const slideUp = useModal();
  const authModal = useModal();

  // API hooks
  const { data: projectsData } = useProjects();
  const createMutation = useCreateProject();
  const deleteMutation = useDeleteProject();
  const cancelMutation = useCancelProject();

  const projects = projectsData?.content ?? [];

  const handleNavigate = useCallback((page: string) => {
    setActivePage(page);
    if (page === 'home') { /* 모든 모달 닫기 */ }
    else if (page === 'projects') projectsModal.open();
    else if (page === 'audit') auditModal.open();
    else if (page === 'settings') settingsModal.open();
    else if (page === 'create') createModal.open();
  }, [projectsModal, auditModal, settingsModal, createModal]);

  const handleSelectProject = useCallback((project: Project) => {
    setSelectedProject(project);
    slideUp.open();
  }, [slideUp]);

  const handleCreateDone = useCallback((data: { name?: string; genre?: string; requirement: string }) => {
    createMutation.mutate({ ...data, demo: true }, {
      onSuccess: (project) => {
        setSelectedProject(project);
        slideUp.open();
      },
    });
  }, [createMutation, slideUp]);

  const handleDelete = useCallback(() => {
    if (!selectedProject) return;
    if (!confirm('정말 삭제하시겠습니까?')) return;
    deleteMutation.mutate(selectedProject.id, {
      onSuccess: () => slideUp.close(),
    });
  }, [selectedProject, deleteMutation, slideUp]);

  const handleCancel = useCallback(() => {
    if (!selectedProject) return;
    if (!confirm('진행 중인 파이프라인을 중단하시겠습니까?')) return;
    cancelMutation.mutate(selectedProject.id);
  }, [selectedProject, cancelMutation]);

  return (
    <>
      {showIntro && <IntroScreen onDismiss={() => setShowIntro(false)} />}
      <Layout
        theme={theme}
        onToggleTheme={toggleTheme}
        activePage={activePage}
        onNavigate={handleNavigate}
        onLogin={authModal.open}
      >
        <HomePage
          onCreateGame={() => { setIsDemo(false); createModal.open(); }}
          onDemo={() => { setIsDemo(true); createModal.open(); }}
          onSelectProject={(project) => handleSelectProject(project)}
          projects={projects}
        />

        <CreateGameModal isOpen={createModal.isOpen} onClose={createModal.close} onCreate={handleCreateDone} isDemo={isDemo} />
        <ProjectListModal isOpen={projectsModal.isOpen} onClose={projectsModal.close} onSelect={handleSelectProject} projects={projects} />
        <AuditLogModal isOpen={auditModal.isOpen} onClose={auditModal.close} />
        <SettingsModal isOpen={settingsModal.isOpen} onClose={settingsModal.close} />
        <AuthModal isOpen={authModal.isOpen} onClose={authModal.close} />

        <SlideUpModal
          isOpen={slideUp.isOpen}
          onClose={slideUp.close}
          headerRight={
            <div className="flex gap-2">
              <button onClick={handleCancel} className="px-4 py-2 rounded-lg text-sm font-medium border border-border bg-bg-primary text-text-secondary cursor-pointer hover:bg-bg-tertiary">중단</button>
              <button onClick={handleDelete} className="px-4 py-2 rounded-lg text-sm font-medium border border-danger-bg text-danger cursor-pointer hover:bg-danger-bg">삭제</button>
            </div>
          }
        >
          {selectedProject && <ProjectDetail project={selectedProject} isDemo={selectedProject.demo} onCreateReal={() => { slideUp.close(); setIsDemo(false); setTimeout(() => createModal.open(), 600); }} onLogin={authModal.open} />}
        </SlideUpModal>
      </Layout>
    </>
  );
}
