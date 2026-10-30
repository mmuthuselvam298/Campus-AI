import React from 'react';
import { Shield, Sparkles, FolderSearch, LogOut, User } from 'lucide-react';
import { AuthUser } from '../types';

interface NavbarProps {
  currentView: 'home' | 'directory' | 'admin';
  onNavigate: (view: 'home' | 'directory' | 'admin') => void;
  onOpenReport: (type: 'LOST' | 'FOUND') => void;
  user: AuthUser | null;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentView, onNavigate, onOpenReport, user, onLogout }) => {
  const isStaffOrAdmin = user && (user.role === 'STAFF' || user.role === 'ADMIN') && user.accountStatus === 'ACTIVE';

  return (
    <header style={{
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      backdropFilter: 'blur(10px)',
      borderBottom: '1px solid var(--border-medium)',
      position: 'sticky',
      top: 0,
      zIndex: 40
    }}>
      <div className="container" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: '72px'
      }}>
        {/* Brand Logo */}
        <div 
          onClick={() => onNavigate('home')}
          style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}
        >
          <div style={{
            width: '38px',
            height: '38px',
            borderRadius: '10px',
            backgroundColor: '#10b981',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            boxShadow: '0 4px 10px rgba(16, 185, 129, 0.28)'
          }}>
            <Sparkles size={20} />
          </div>
          <div>
            <span style={{ fontWeight: 700, fontSize: '1.15rem', color: '#0f172a', letterSpacing: '-0.02em' }}>
              Campus <span style={{ color: '#10b981' }}>Lost & Found</span>
            </span>
            <span style={{
              display: 'block',
              fontSize: '0.7rem',
              fontWeight: 600,
              color: '#64748b',
              textTransform: 'uppercase',
              letterSpacing: '0.06em'
            }}>
              AI Item Matcher
            </span>
          </div>
        </div>

        {/* Navigation Links */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={() => onNavigate('home')}
            style={{
              padding: '8px 16px',
              borderRadius: '9999px',
              fontWeight: 600,
              fontSize: '0.92rem',
              color: currentView === 'home' ? '#065f46' : '#64748b',
              backgroundColor: currentView === 'home' ? '#ecfdf5' : 'transparent',
            }}
          >
            Home
          </button>

          <button
            onClick={() => onNavigate('directory')}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 16px',
              borderRadius: '9999px',
              fontWeight: 600,
              fontSize: '0.92rem',
              color: currentView === 'directory' ? '#065f46' : '#64748b',
              backgroundColor: currentView === 'directory' ? '#ecfdf5' : 'transparent',
            }}
          >
            <FolderSearch size={16} />
            Registry
          </button>

          {isStaffOrAdmin && (
            <button
              onClick={() => onNavigate('admin')}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '8px 16px',
                borderRadius: '9999px',
                fontWeight: 600,
                fontSize: '0.92rem',
                color: currentView === 'admin' ? '#065f46' : '#64748b',
                backgroundColor: currentView === 'admin' ? '#ecfdf5' : 'transparent',
              }}
            >
              <Shield size={16} />
              {user?.role === 'ADMIN' ? 'Admin Portal' : 'Staff Review'}
            </button>
          )}
        </nav>

        {/* User Info & Quick Action */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            onClick={() => onOpenReport('FOUND')}
            className="btn-outline-mint"
            style={{ padding: '7px 14px', fontSize: '0.85rem' }}
          >
            + Report Found
          </button>
          <button
            onClick={() => onOpenReport('LOST')}
            className="btn-primary"
            style={{ padding: '7px 16px', fontSize: '0.85rem' }}
          >
            + Report Lost
          </button>

          {user && (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              paddingLeft: '12px',
              borderLeft: '1px solid #e2e8f0'
            }}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', fontSize: '0.78rem' }}>
                <span style={{ fontWeight: 600, color: '#1e293b', maxWidth: '140px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {user.fullName || user.campusEmail}
                </span>
                <span style={{
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  padding: '1px 6px',
                  borderRadius: '9999px',
                  backgroundColor: user.role === 'ADMIN' ? '#f3e8ff' : user.role === 'STAFF' ? '#dcfce7' : '#e0f2fe',
                  color: user.role === 'ADMIN' ? '#7e22ce' : user.role === 'STAFF' ? '#15803d' : '#0369a1',
                }}>
                  {user.role}
                </span>
              </div>

              <button
                onClick={onLogout}
                title="Sign Out"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  border: '1px solid #e2e8f0',
                  backgroundColor: '#ffffff',
                  color: '#64748b',
                  cursor: 'pointer'
                }}
              >
                <LogOut size={15} />
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
