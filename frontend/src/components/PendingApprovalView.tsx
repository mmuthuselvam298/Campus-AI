import React, { useState } from 'react';
import { AuthUser } from '../types';
import { api } from '../services/api';

interface PendingApprovalViewProps {
  user: AuthUser;
  onRefreshUser: (updatedUser: AuthUser) => void;
  onLogout: () => void;
}

export const PendingApprovalView: React.FC<PendingApprovalViewProps> = ({
  user,
  onRefreshUser,
  onLogout,
}) => {
  const [checking, setChecking] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const handleCheckStatus = async () => {
    try {
      setChecking(true);
      setStatusMessage(null);
      const fresh = await api.getMe();
      if (fresh) {
        if (fresh.accountStatus === 'ACTIVE') {
          onRefreshUser(fresh);
        } else if (fresh.accountStatus === 'REJECTED') {
          setStatusMessage('Your staff application has been rejected by administration. Please contact support.');
          onRefreshUser(fresh);
        } else {
          setStatusMessage('Status is still pending review by campus administration.');
        }
      } else {
        onLogout();
      }
    } catch {
      setStatusMessage('Unable to check status. Please try again.');
    } finally {
      setChecking(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#f8fafc',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      padding: '40px 16px',
      fontFamily: "'Plus Jakarta Sans', -apple-system, sans-serif"
    }}>
      <div style={{
        maxWidth: '480px',
        width: '100%',
        backgroundColor: '#ffffff',
        borderRadius: '24px',
        border: '1px solid #e2e8f0',
        boxShadow: '0 20px 45px -10px rgba(15, 23, 42, 0.08)',
        padding: '36px 30px',
        textAlign: 'center',
        boxSizing: 'border-box'
      }}>
        <div style={{
          margin: '0 auto 18px auto',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '64px',
          height: '64px',
          borderRadius: '50%',
          backgroundColor: '#fef3c7',
          color: '#d97706'
        }}>
          <svg width="32" height="32" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>

        <span style={{
          display: 'inline-flex',
          alignItems: 'center',
          padding: '4px 12px',
          borderRadius: '9999px',
          fontSize: '0.78rem',
          fontWeight: 700,
          backgroundColor: '#fffbeb',
          color: '#b45309',
          border: '1px solid #fde68a',
          marginBottom: '12px'
        }}>
          Application Under Review
        </span>

        <h2 style={{
          fontSize: '1.5rem',
          fontWeight: 800,
          color: '#0f172a',
          letterSpacing: '-0.02em',
          margin: '0 0 10px 0'
        }}>
          Staff Account Pending Approval
        </h2>

        <p style={{
          fontSize: '0.9rem',
          color: '#64748b',
          lineHeight: 1.55,
          margin: '0 auto 20px auto'
        }}>
          Hello <strong style={{ color: '#0f172a' }}>{user.fullName || user.campusEmail}</strong>! You registered as <span style={{ fontWeight: 600, color: '#047857' }}>Campus Staff</span>.
          To protect student privacy and access to confidential item proofs, your staff permissions require review by a campus administrator.
        </p>

        <div style={{
          padding: '14px 16px',
          borderRadius: '14px',
          backgroundColor: '#f8fafc',
          border: '1px solid #e2e8f0',
          textAlign: 'left',
          fontSize: '0.82rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '6px',
          color: '#475569',
          marginBottom: '20px'
        }}>
          <div><span style={{ fontWeight: 600, color: '#334155' }}>Registered Email:</span> {user.campusEmail}</div>
          <div><span style={{ fontWeight: 600, color: '#334155' }}>Role Requested:</span> Campus Staff (Administrative Reviewer)</div>
          <div><span style={{ fontWeight: 600, color: '#334155' }}>Account Status:</span> <span style={{ color: '#d97706', fontWeight: 700 }}>Pending Approval</span></div>
        </div>

        {statusMessage && (
          <div style={{
            marginBottom: '20px',
            padding: '12px',
            borderRadius: '12px',
            backgroundColor: '#eff6ff',
            border: '1px solid #bfdbfe',
            color: '#1e40af',
            fontSize: '0.85rem'
          }}>
            {statusMessage}
          </div>
        )}

        <div style={{
          display: 'flex',
          flexDirection: 'row',
          gap: '10px',
          justifyContent: 'center',
          flexWrap: 'wrap'
        }}>
          <button
            type="button"
            onClick={handleCheckStatus}
            disabled={checking}
            style={{
              padding: '10px 18px',
              borderRadius: '12px',
              border: 'none',
              fontSize: '0.9rem',
              fontWeight: 700,
              color: '#ffffff',
              backgroundColor: '#10b981',
              boxShadow: '0 2px 8px rgba(16, 185, 129, 0.3)',
              cursor: checking ? 'not-allowed' : 'pointer',
              opacity: checking ? 0.6 : 1,
              transition: 'all 0.15s ease'
            }}
          >
            {checking ? 'Checking status...' : '🔄 Check Approval Status'}
          </button>

          <button
            type="button"
            onClick={onLogout}
            style={{
              padding: '10px 18px',
              borderRadius: '12px',
              border: '1.5px solid #cbd5e1',
              fontSize: '0.9rem',
              fontWeight: 600,
              color: '#475569',
              backgroundColor: '#ffffff',
              cursor: 'pointer',
              transition: 'all 0.15s ease'
            }}
          >
            Sign Out
          </button>
        </div>

        <p style={{
          marginTop: '24px',
          fontSize: '0.78rem',
          color: '#94a3b8',
          margin: '24px 0 0 0'
        }}>
          Need faster activation? Notify your campus security administrator with your email.
        </p>
      </div>
    </div>
  );
};
