import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { ThreeSteps } from './components/ThreeSteps';
import { ItemsDirectory } from './components/ItemsDirectory';
import { AdminPortal } from './components/AdminPortal';
import { ReportWorkflow } from './components/ReportWorkflow';
import { ClaimModal } from './components/ClaimModal';
import { LoginView } from './components/LoginView';
import { PendingApprovalView } from './components/PendingApprovalView';
import { Item, ItemMatch, ReportType, AuthUser } from './types';
import { api } from './services/api';
import { Sparkles } from 'lucide-react';

export const App: React.FC = () => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loadingAuth, setLoadingAuth] = useState(true);
  const [currentView, setCurrentView] = useState<'home' | 'directory' | 'admin'>('home');
  const [workflowType, setWorkflowType] = useState<ReportType | null>(null);
  const [claimTargetItem, setClaimTargetItem] = useState<Item | null>(null);

  useEffect(() => {
    const checkAuthSession = async () => {
      try {
        const currentUser = await api.getMe();
        setUser(currentUser);
      } catch {
        setUser(null);
      } finally {
        setLoadingAuth(false);
      }
    };
    checkAuthSession();
  }, []);

  const handleLogout = async () => {
    try {
      await api.logout();
    } finally {
      setUser(null);
      setCurrentView('home');
    }
  };

  const handleOpenReport = (type: ReportType) => {
    setWorkflowType(type);
  };

  const handleCloseReport = () => {
    setWorkflowType(null);
  };

  const handleReportCompleted = (_item: Item, _matches: ItemMatch[]) => {
    // Keep dialog open on matches stage until user closes or clicks to registry
  };

  // 1. Initial loading screen
  if (loadingAuth) {
    return (
      <div style={{
        minHeight: '100vh',
        backgroundColor: '#f8fafc',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px'
      }}>
        <div style={{
          width: '56px',
          height: '56px',
          borderRadius: '16px',
          backgroundColor: '#10b981',
          color: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 10px 25px -5px rgba(16, 185, 129, 0.4)',
          marginBottom: '16px'
        }}>
          <Sparkles size={28} />
        </div>
        <div style={{ color: '#0f172a', fontWeight: 700, fontSize: '1.25rem' }}>Campus Lost & Found AI</div>
        <div style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '6px' }}>Connecting campus registry...</div>
      </div>
    );
  }

  // 2. Unauthenticated visitor: display login gate
  if (!user) {
    return <LoginView onLoginSuccess={(u) => setUser(u)} />;
  }

  // 3. Staff awaiting approval from campus administration
  if (user.accountStatus === 'PENDING_APPROVAL') {
    return (
      <PendingApprovalView
        user={user}
        onRefreshUser={(u) => setUser(u)}
        onLogout={handleLogout}
      />
    );
  }

  const isStaffOrAdmin = (user.role === 'STAFF' || user.role === 'ADMIN') && user.accountStatus === 'ACTIVE';

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#ffffff' }}>
      {/* Navigation Bar */}
      <Navbar
        currentView={currentView}
        onNavigate={(view) => {
          if (view === 'admin' && !isStaffOrAdmin) {
            setCurrentView('home');
          } else {
            setCurrentView(view);
          }
        }}
        onOpenReport={handleOpenReport}
        user={user}
        onLogout={handleLogout}
      />

      {/* Main Content Router */}
      <main style={{ flex: 1 }}>
        {currentView === 'home' && (
          <div>
            <Hero
              onOpenReport={handleOpenReport}
              onExploreRegistry={() => setCurrentView('directory')}
            />
            <ThreeSteps />
            {/* Embedded Live Registry section on home page */}
            <div style={{ padding: '20px 0' }}>
              <ItemsDirectory
                onOpenReport={handleOpenReport}
                onOpenClaim={(item) => setClaimTargetItem(item)}
              />
            </div>
          </div>
        )}

        {currentView === 'directory' && (
          <ItemsDirectory
            onOpenReport={handleOpenReport}
            onOpenClaim={(item) => setClaimTargetItem(item)}
          />
        )}

        {currentView === 'admin' && isStaffOrAdmin && (
          <AdminPortal user={user} />
        )}
      </main>

      {/* Report Modal / Workflow */}
      {workflowType && (
        <ReportWorkflow
          key={workflowType}
          initialType={workflowType}
          onClose={handleCloseReport}
          onCompleted={handleReportCompleted}
          onOpenClaimModal={(item) => {
            handleCloseReport();
            setClaimTargetItem(item);
          }}
          user={user}
        />
      )}

      {/* Claim Submission Modal */}
      {claimTargetItem && (
        <ClaimModal
          item={claimTargetItem}
          onClose={() => setClaimTargetItem(null)}
          onClaimSubmitted={() => {
            // Refreshed when needed
          }}
          user={user}
        />
      )}

      {/* Footer */}
      <footer style={{
        backgroundColor: '#f8fafc',
        borderTop: '1px solid #e2e8f0',
        padding: '40px 0',
        marginTop: 'auto'
      }}>
        <div className="container" style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '20px',
          fontSize: '0.88rem',
          color: '#64748b'
        }}>
          <div>
            <span style={{ fontWeight: 700, color: '#0f172a' }}>Campus Lost & Found AI System</span>
            <p style={{ fontSize: '0.8rem', marginTop: '4px' }}>
              Signed in as <strong className="text-slate-700">{user.campusEmail}</strong> ({user.role}) &bull; Zero Preloaded Records &bull; SQLite JDBC
            </p>
          </div>

          <div style={{ display: 'flex', gap: '20px', fontSize: '0.82rem' }}>
            <span>Privacy Preserving</span>
            <span>&bull;</span>
            <span>Role-Based Access</span>
            <span>&bull;</span>
            <span>Verified Returns</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;
