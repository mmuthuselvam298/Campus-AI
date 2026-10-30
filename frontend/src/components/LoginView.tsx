import React, { useState, useEffect } from 'react';
import { AuthUser, UserRole, RequestCodeResponse } from '../types';
import { api } from '../services/api';

interface LoginViewProps {
  onLoginSuccess: (user: AuthUser) => void;
}

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess }) => {
  const [step, setStep] = useState<'EMAIL' | 'OTP'>('EMAIL');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<UserRole>('STUDENT');
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(0);
  const [devOtpInfo, setDevOtpInfo] = useState<RequestCodeResponse | null>(null);

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;
    if (countdown > 0) {
      timer = setTimeout(() => setCountdown(countdown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [countdown]);

  const handleRequestCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    const cleanEmail = email.trim().toLowerCase();
    if (!cleanEmail || !cleanEmail.includes('@')) {
      setError('Please enter a valid campus email address.');
      return;
    }

    try {
      setLoading(true);
      const data = await api.requestCode(cleanEmail, fullName.trim() || undefined, role);
      setDevOtpInfo(data);
      setSuccessMsg(data.message);
      setStep('OTP');
      setCountdown(data.cooldownSeconds || 60);
    } catch (err: any) {
      setError(err.message || 'Failed to send verification code. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const cleanCode = code.trim();
    if (cleanCode.length !== 6) {
      setError('Please enter the complete 6-digit verification code.');
      return;
    }

    try {
      setLoading(true);
      const user = await api.verifyCode(email.trim().toLowerCase(), cleanCode, role, fullName.trim() || undefined);
      onLoginSuccess(user);
    } catch (err: any) {
      setError(err.message || 'Verification failed. Please check the code.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (countdown > 0 || loading) return;
    setError(null);
    try {
      setLoading(true);
      const data = await api.requestCode(email.trim().toLowerCase(), fullName.trim() || undefined, role);
      setDevOtpInfo(data);
      setSuccessMsg(data.message || 'A fresh verification code has been issued.');
      setCountdown(data.cooldownSeconds || 60);
    } catch (err: any) {
      setError(err.message || 'Could not resend code. Please wait a moment.');
    } finally {
      setLoading(false);
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
      {/* Branding Header */}
      <div style={{ textAlign: 'center', maxWidth: '440px', width: '100%', marginBottom: '24px' }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '64px',
          height: '64px',
          borderRadius: '18px',
          backgroundColor: '#10b981',
          color: '#ffffff',
          boxShadow: '0 10px 25px -4px rgba(16, 185, 129, 0.35)',
          marginBottom: '16px'
        }}>
          <svg width="32" height="32" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7" />
          </svg>
        </div>
        <h1 style={{
          fontSize: '1.85rem',
          fontWeight: 800,
          color: '#0f172a',
          letterSpacing: '-0.02em',
          margin: 0
        }}>
          Campus Lost &amp; Found AI
        </h1>
        <p style={{
          marginTop: '8px',
          fontSize: '0.92rem',
          color: '#64748b',
          lineHeight: 1.5
        }}>
          Sign in with your campus credentials to report items, search matches, or review claims.
        </p>
      </div>

      {/* Main Card */}
      <div style={{
        maxWidth: '440px',
        width: '100%',
        backgroundColor: '#ffffff',
        borderRadius: '24px',
        border: '1px solid #e2e8f0',
        boxShadow: '0 20px 45px -10px rgba(15, 23, 42, 0.08)',
        padding: '32px 28px',
        boxSizing: 'border-box'
      }}>
        {error && (
          <div style={{
            marginBottom: '20px',
            padding: '12px 14px',
            borderRadius: '12px',
            backgroundColor: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#b91c1c',
            fontSize: '0.88rem',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px'
          }}>
            <span style={{ fontSize: '1.1rem', lineHeight: 1 }}>⚠️</span>
            <span style={{ flex: 1 }}>{error}</span>
          </div>
        )}

        {successMsg && !error && (
          <div style={{
            marginBottom: '20px',
            padding: '12px 14px',
            borderRadius: '12px',
            backgroundColor: '#ecfdf5',
            border: '1px solid #a7f3d0',
            color: '#065f46',
            fontSize: '0.88rem',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px'
          }}>
            <span style={{ fontSize: '1.1rem', lineHeight: 1 }}>✅</span>
            <span style={{ flex: 1 }}>{successMsg}</span>
          </div>
        )}

        {step === 'EMAIL' ? (
          <form onSubmit={handleRequestCode} style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            {/* Development Mode Notice */}
            <div style={{
              padding: '10px 12px',
              borderRadius: '12px',
              backgroundColor: '#f8fafc',
              border: '1px dashed #cbd5e1',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              fontSize: '0.8rem',
              color: '#475569'
            }}>
              <span style={{ fontSize: '1.05rem' }}>⚡</span>
              <span><strong>Local Dev Mode:</strong> Real email delivery is disabled. Test codes will be displayed on the next screen.</span>
            </div>

            {/* Role Switcher */}
            <div>
              <label style={{
                display: 'block',
                fontSize: '0.78rem',
                fontWeight: 700,
                color: '#475569',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
                marginBottom: '8px'
              }}>
                I am a
              </label>
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '6px',
                padding: '4px',
                backgroundColor: '#f1f5f9',
                borderRadius: '12px'
              }}>
                <button
                  type="button"
                  onClick={() => setRole('STUDENT')}
                  style={{
                    padding: '9px 12px',
                    fontSize: '0.9rem',
                    fontWeight: role === 'STUDENT' ? 700 : 500,
                    borderRadius: '8px',
                    backgroundColor: role === 'STUDENT' ? '#ffffff' : 'transparent',
                    color: role === 'STUDENT' ? '#047857' : '#64748b',
                    boxShadow: role === 'STUDENT' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                    border: 'none',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                >
                  🎓 Student
                </button>
                <button
                  type="button"
                  onClick={() => setRole('STAFF')}
                  style={{
                    padding: '9px 12px',
                    fontSize: '0.9rem',
                    fontWeight: role === 'STAFF' ? 700 : 500,
                    borderRadius: '8px',
                    backgroundColor: role === 'STAFF' ? '#ffffff' : 'transparent',
                    color: role === 'STAFF' ? '#047857' : '#64748b',
                    boxShadow: role === 'STAFF' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                    border: 'none',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                >
                  🛡️ Campus Staff
                </button>
              </div>
              {role === 'STAFF' && (
                <div style={{
                  marginTop: '10px',
                  fontSize: '0.8rem',
                  color: '#065f46',
                  backgroundColor: '#ecfdf5',
                  padding: '10px 12px',
                  borderRadius: '10px',
                  border: '1px solid #a7f3d0',
                  lineHeight: 1.45
                }}>
                  🛡️ Staff accounts grant immediate access to review claims, inspect verification proofs, and resolve items.
                </div>
              )}
            </div>

            {/* Full Name */}
            <div>
              <label htmlFor="fullName" style={{
                display: 'block',
                fontSize: '0.85rem',
                fontWeight: 600,
                color: '#334155',
                marginBottom: '6px'
              }}>
                Full Name <span style={{ color: '#94a3b8', fontWeight: 400 }}>(optional)</span>
              </label>
              <input
                id="fullName"
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Alex Morgan"
                style={{
                  width: '100%',
                  padding: '11px 14px',
                  backgroundColor: '#ffffff',
                  border: '1.5px solid #cbd5e1',
                  borderRadius: '12px',
                  color: '#0f172a',
                  fontSize: '0.92rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            {/* Email Address */}
            <div>
              <label htmlFor="email" style={{
                display: 'block',
                fontSize: '0.85rem',
                fontWeight: 600,
                color: '#334155',
                marginBottom: '6px'
              }}>
                Campus Email Address <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input
                id="email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="student@campus.edu"
                style={{
                  width: '100%',
                  padding: '11px 14px',
                  backgroundColor: '#ffffff',
                  border: '1.5px solid #cbd5e1',
                  borderRadius: '12px',
                  color: '#0f172a',
                  fontSize: '0.92rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
              <p style={{
                marginTop: '6px',
                fontSize: '0.78rem',
                color: '#64748b'
              }}>
                We'll send a 6-digit one-time verification code to this address.
              </p>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              style={{
                width: '100%',
                padding: '13px',
                backgroundColor: '#10b981',
                color: '#ffffff',
                border: 'none',
                borderRadius: '12px',
                fontWeight: 700,
                fontSize: '0.98rem',
                boxShadow: '0 4px 14px rgba(16, 185, 129, 0.3)',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.7 : 1,
                transition: 'background-color 0.15s ease'
              }}
            >
              {loading ? 'Generating code...' : 'Continue with email →'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyCode} style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <div style={{ textAlign: 'center', paddingBottom: '4px' }}>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '44px',
                height: '44px',
                borderRadius: '50%',
                backgroundColor: '#ecfdf5',
                color: '#059669',
                marginBottom: '8px'
              }}>
                <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
                Enter verification code
              </h3>
              
              {devOtpInfo?.devMode && !devOtpInfo.emailSent ? (
                <div style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  marginTop: '8px',
                  padding: '4px 10px',
                  borderRadius: '16px',
                  backgroundColor: '#fef3c7',
                  border: '1px solid #fde68a',
                  color: '#92400e',
                  fontSize: '0.78rem',
                  fontWeight: 600
                }}>
                  <span>⚡</span> Local Dev Mode &bull; Email delivery disabled
                </div>
              ) : (
                <p style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '4px' }}>
                  Sent to <strong style={{ color: '#1e293b' }}>{email}</strong>
                </p>
              )}
            </div>

            {/* Local Development OTP Banner & Autofill Action */}
            {devOtpInfo?.devMode && devOtpInfo.devOtp && (
              <div style={{
                backgroundColor: '#f0fdf4',
                border: '1.5px dashed #10b981',
                borderRadius: '16px',
                padding: '14px 16px',
                textAlign: 'center',
                boxShadow: '0 4px 12px rgba(16, 185, 129, 0.08)'
              }}>
                <div style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  padding: '3px 10px',
                  borderRadius: '14px',
                  backgroundColor: '#dcfce7',
                  color: '#15803d',
                  fontSize: '0.72rem',
                  fontWeight: 800,
                  letterSpacing: '0.05em',
                  textTransform: 'uppercase',
                  marginBottom: '8px'
                }}>
                  <span>🛠️</span> Dev OTP Active
                </div>

                <p style={{
                  margin: '0 0 10px 0',
                  fontSize: '0.82rem',
                  color: '#166534',
                  fontWeight: 500,
                  lineHeight: 1.4
                }}>
                  No SMTP provider configured. Use this generated code:
                </p>

                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '12px',
                  backgroundColor: '#ffffff',
                  padding: '8px 14px',
                  borderRadius: '12px',
                  border: '1px solid #86efac',
                  marginBottom: '8px'
                }}>
                  <span style={{
                    fontFamily: "'Courier New', Courier, monospace",
                    fontSize: '1.65rem',
                    fontWeight: 800,
                    letterSpacing: '0.22em',
                    color: '#047857'
                  }}>
                    {devOtpInfo.devOtp}
                  </span>
                  <button
                    type="button"
                    onClick={() => setCode(devOtpInfo.devOtp || '')}
                    title="Click to paste code into input"
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      padding: '6px 12px',
                      fontSize: '0.8rem',
                      fontWeight: 700,
                      backgroundColor: '#10b981',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '8px',
                      cursor: 'pointer',
                      boxShadow: '0 2px 6px rgba(16, 185, 129, 0.3)',
                      transition: 'transform 0.1s ease'
                    }}
                  >
                    Autofill ⚡
                  </button>
                </div>

                <div style={{
                  fontSize: '0.74rem',
                  color: '#059669',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px'
                }}>
                  <span>⏱️ 10-minute expiry</span>
                  <span>&bull;</span>
                  <span>🔒 Single-use</span>
                </div>
              </div>
            )}

            <div>
              <label htmlFor="code" style={{ display: 'none' }}>Verification Code</label>
              <input
                id="code"
                type="text"
                maxLength={6}
                autoFocus
                required
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                placeholder="••••••"
                style={{
                  width: '100%',
                  textAlign: 'center',
                  letterSpacing: '0.45em',
                  fontSize: '1.75rem',
                  fontFamily: 'monospace',
                  padding: '12px 14px',
                  borderRadius: '12px',
                  border: '2px solid #10b981',
                  color: '#0f172a',
                  fontWeight: 700,
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <button
              type="submit"
              disabled={loading || code.trim().length !== 6}
              style={{
                width: '100%',
                padding: '13px',
                backgroundColor: '#10b981',
                color: '#ffffff',
                border: 'none',
                borderRadius: '12px',
                fontWeight: 700,
                fontSize: '0.98rem',
                boxShadow: '0 4px 14px rgba(16, 185, 129, 0.3)',
                cursor: (loading || code.trim().length !== 6) ? 'not-allowed' : 'pointer',
                opacity: (loading || code.trim().length !== 6) ? 0.6 : 1,
                transition: 'background-color 0.15s ease'
              }}
            >
              {loading ? 'Verifying...' : 'Verify and Sign In'}
            </button>

            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingTop: '12px',
              borderTop: '1px solid #f1f5f9',
              fontSize: '0.82rem'
            }}>
              <button
                type="button"
                onClick={() => {
                  setStep('EMAIL');
                  setError(null);
                  setDevOtpInfo(null);
                  setCode('');
                }}
                style={{
                  color: '#64748b',
                  fontWeight: 600,
                  border: 'none',
                  background: 'none',
                  cursor: 'pointer',
                  padding: '4px 0'
                }}
              >
                ← Change email
              </button>

              <button
                type="button"
                disabled={countdown > 0 || loading}
                onClick={handleResend}
                style={{
                  fontWeight: 700,
                  color: countdown > 0 ? '#94a3b8' : '#059669',
                  cursor: countdown > 0 ? 'not-allowed' : 'pointer',
                  border: 'none',
                  background: 'none',
                  padding: '4px 0'
                }}
              >
                {countdown > 0 ? `Resend code (${countdown}s)` : 'Resend code'}
              </button>
            </div>
          </form>
        )}

        <div style={{
          marginTop: '24px',
          paddingTop: '16px',
          borderTop: '1px solid #f1f5f9',
          textAlign: 'center'
        }}>
          <p style={{
            fontSize: '0.75rem',
            color: '#94a3b8',
            margin: 0,
            lineHeight: 1.5
          }}>
            Protected by Campus Lost &amp; Found Security &bull; Single-use OTP &bull; 10-minute expiry
          </p>
        </div>
      </div>
    </div>
  );
};
