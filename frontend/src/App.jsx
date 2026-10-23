import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import PatientView from './components/PatientView';
import DoctorView from './components/DoctorView';
import ProviderView from './components/ProviderView';
import AuditView from './components/AuditView';
import AuthScreen from './components/AuthScreen';
import CardiacRiskModal from './components/CardiacRiskModal';
import Toast from './components/Toast';
import { api } from './api';
import { ShieldCheck, HeartPulse } from 'lucide-react';

export default function App() {
  const [currentUser, setCurrentUser] = useState(() => {
    try {
      const saved = localStorage.getItem('medit_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [showSplash, setShowSplash] = useState(true);
  const [currentView, setCurrentView] = useState('timeline');
  const [toast, setToast] = useState(null);
  const [stats, setStats] = useState(null);
  const [isResetting, setIsResetting] = useState(false);
  const [pendingRequestsCount, setPendingRequestsCount] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isCardiacRiskOpen, setIsCardiacRiskOpen] = useState(false);

  // Splash Screen timeout for initial branded launch
  useEffect(() => {
    const timer = setTimeout(() => {
      setShowSplash(false);
    }, 1100);
    return () => clearTimeout(timer);
  }, []);

  const fetchStats = async () => {
    try {
      const data = await api.getStats();
      setStats(data);
      if (data.pending_requests !== undefined) {
        setPendingRequestsCount(data.pending_requests);
      }
    } catch (err) {
      console.error('Stats fetch error', err);
    }
  };

  useEffect(() => {
    if (currentUser) {
      fetchStats();
      const interval = setInterval(fetchStats, 10000);
      return () => clearInterval(interval);
    }
  }, [currentUser, refreshKey]);

  const showToast = (toastConfig) => {
    setToast(toastConfig);
    setTimeout(() => {
      setToast(null);
    }, 4500);
  };

  const handleAuthSuccess = (user) => {
    setCurrentUser(user);
    try {
      localStorage.setItem('medit_user', JSON.stringify(user));
    } catch (e) {
      console.warn('LocalStorage error', e);
    }
    setCurrentView('timeline');
  };

  const handleSignOut = () => {
    try {
      localStorage.removeItem('medit_user');
    } catch (e) {
      console.warn('LocalStorage error', e);
    }
    setCurrentUser(null);
    showToast({
      type: 'info',
      title: 'Signed Out',
      message: 'You have safely signed out of MedIt.',
    });
  };

  const handleResetDemo = async () => {
    if (!window.confirm('Reset database to clean initial demo state? All demo records, requests, and permissions will be restored.')) {
      return;
    }
    setIsResetting(true);
    try {
      await api.resetDemoSeed();
      showToast({
        type: 'success',
        title: 'Demo State Reset',
        message: 'Database has been restored to clean initial seed data.',
      });
      setRefreshKey((prev) => prev + 1);
    } catch (err) {
      showToast({
        type: 'error',
        title: 'Reset Error',
        message: 'Failed to reset demo state.',
      });
    } finally {
      setIsResetting(false);
    }
  };

  // 1. Splash Screen
  if (showSplash) {
    return (
      <div className="fixed inset-0 z-50 bg-[#0B3724] flex flex-col items-center justify-center text-white selection:bg-emerald-400">
        <div className="flex flex-col items-center space-y-4 animate-in fade-in zoom-in-95 duration-700">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-tr from-[#10B981] to-emerald-300 text-[#0B3724] flex items-center justify-center shadow-2xl shadow-emerald-950/40">
            <ShieldCheck className="w-12 h-12 text-[#0B3724]" />
          </div>
          <div className="text-center space-y-1">
            <h1 className="text-3xl font-black tracking-tight text-white">MedIt</h1>
            <p className="text-xs text-emerald-200/80 font-medium">Unified Patient Health Records Platform</p>
          </div>
          <div className="flex items-center gap-2 pt-2">
            <div className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            <span className="text-[11px] text-emerald-300 font-mono">Initializing Clinical Core...</span>
          </div>
        </div>
      </div>
    );
  }

  // 2. Authentication Screen (when no authenticated user)
  if (!currentUser) {
    return (
      <>
        <AuthScreen onAuthSuccess={handleAuthSuccess} onShowToast={showToast} />
        <Toast toast={toast} onClose={() => setToast(null)} />
      </>
    );
  }

  // 3. Authenticated Clinical Application
  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col font-sans text-slate-800 antialiased selection:bg-emerald-100 selection:text-emerald-900">
      {/* Top Navigation & Clinical Bar */}
      <Navbar
        currentUser={currentUser}
        onSignOut={handleSignOut}
        onOpenCardiacRisk={() => setIsCardiacRiskOpen(true)}
        onOpenAuditTrail={() => setCurrentView('audit')}
        onResetDemo={handleResetDemo}
        isResetting={isResetting}
      />

      {/* Main Body Layout */}
      <main className="grow max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col md:flex-row items-start gap-8">
          {/* Role Navigation Sidebar */}
          <Sidebar
            currentView={currentView}
            onSelectView={setCurrentView}
            persona={currentUser}
            stats={stats}
            pendingCount={pendingRequestsCount}
          />

          {/* Dynamic Content Views */}
          <div className="grow w-full max-w-5xl">
            {currentView === 'audit' ? (
              <AuditView
                persona={currentUser}
                onShowToast={showToast}
              />
            ) : currentUser.role === 'patient' ? (
              <PatientView
                key={`${currentUser.id}-${refreshKey}`}
                persona={currentUser}
                onShowToast={showToast}
                onSwitchPersona={(role, id) => {
                  // For demo testing, allow quick switch if needed or show info
                  showToast({
                    type: 'info',
                    title: 'Role Switch Requested',
                    message: `Sign out and log in as ${role} to verify authorization.`,
                  });
                }}
              />
            ) : currentUser.role === 'doctor' ? (
              <DoctorView
                key={`${currentUser.id}-${refreshKey}`}
                persona={currentUser}
                onShowToast={showToast}
                onSwitchPersona={(role, id) => {
                  showToast({
                    type: 'info',
                    title: 'Consent Simulation Notice',
                    message: `Patient ${id} must log in to grant access.`,
                  });
                }}
              />
            ) : currentUser.role === 'provider' ? (
              <ProviderView
                key={`${currentUser.id}-${refreshKey}`}
                persona={currentUser}
                onShowToast={showToast}
              />
            ) : null}
          </div>
        </div>
      </main>

      {/* Professional Healthcare Footer */}
      <footer className="mt-auto border-t border-slate-200 bg-white py-6 px-4 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="font-bold text-[#0B3724]">MedIt</span>
            <span>— Unified Patient Health Records Platform</span>
          </div>
          <div className="text-[11px] text-slate-400">
            ABDM Aligned • Dark Medical Green #0B3724 • End-to-End Patient Consent • Indian Health Terminology (₹ INR, DD MMM YYYY)
          </div>
        </div>
      </footer>

      {/* ICMR Cardiac Risk Stratification Modal */}
      <CardiacRiskModal
        isOpen={isCardiacRiskOpen}
        onClose={() => setIsCardiacRiskOpen(false)}
      />

      {/* Global Toast Notification */}
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
