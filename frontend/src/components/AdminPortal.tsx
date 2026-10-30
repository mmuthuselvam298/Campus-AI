import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Item, Claim, AuditLog, ClaimStatus, AuthUser } from '../types';
import { Shield, CheckCircle, XCircle, AlertCircle, Eye, RefreshCw, FileText, Lock, Users, UserCheck, Trash2 } from 'lucide-react';

interface AdminPortalProps {
  user?: AuthUser | null;
}

export const AdminPortal: React.FC<AdminPortalProps> = ({ user }) => {
  const [activeTab, setActiveTab] = useState<'claims' | 'items' | 'staff' | 'audit'>('claims');
  const [claims, setClaims] = useState<Claim[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [staffRequests, setStaffRequests] = useState<AuthUser[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // Resolution modal state
  const [selectedClaim, setSelectedClaim] = useState<Claim | null>(null);
  const [decision, setDecision] = useState<ClaimStatus>('APPROVED');
  const [notes, setNotes] = useState('');
  const [staffActor, setStaffActor] = useState(user?.fullName || user?.campusEmail || 'Campus Security Office');

  const loadData = async () => {
    try {
      setLoading(true);
      if (activeTab === 'claims') {
        const data = await api.getAdminClaims();
        setClaims(data);
      } else if (activeTab === 'items') {
        const data = await api.getAdminItems();
        setItems(data);
      } else if (activeTab === 'staff') {
        const data = await api.getStaffRequests();
        setStaffRequests(data);
      } else if (activeTab === 'audit') {
        const data = await api.getAuditLogs(60);
        setAuditLogs(data);
      }
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to load staff portal data', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [activeTab]);

  const handleResolveClaim = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClaim) return;

    try {
      await api.resolveClaim(selectedClaim.id, decision, notes, staffActor);
      setMessage({
        text: `Claim successfully updated to ${decision}`,
        type: 'success'
      });
      setSelectedClaim(null);
      setNotes('');
      loadData();
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to update claim', type: 'error' });
    }
  };

  const handleQuickRejectClaim = async (claimId: string) => {
    try {
      await api.resolveClaim(claimId, 'REJECTED', 'Dismissed by authorized staff', staffActor);
      setMessage({
        text: 'Pending claim removed and item status restored to OPEN.',
        type: 'success'
      });
      loadData();
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to remove claim', type: 'error' });
    }
  };

  const handleResetItemStatus = async (itemId: string, status: string = 'OPEN') => {
    try {
      await api.updateItemStatus(itemId, status);
      setMessage({
        text: `Item status reset to ${status}.`,
        type: 'success'
      });
      loadData();
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to update item status', type: 'error' });
    }
  };

  const handleDeleteItem = async (itemId: string) => {
    if (!window.confirm('Are you sure you want to delete this report from the SQLite registry?')) {
      return;
    }
    try {
      await api.deleteItem(itemId);
      setMessage({
        text: 'Item report deleted from SQLite registry.',
        type: 'success'
      });
      loadData();
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to delete item', type: 'error' });
    }
  };

  const handleStaffApproval = async (userId: string, status: 'ACTIVE' | 'REJECTED') => {
    try {
      await api.resolveStaffRequest(userId, status);
      setMessage({
        text: `Staff application successfully updated to ${status}`,
        type: 'success'
      });
      loadData();
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to update staff application', type: 'error' });
    }
  };

  return (
    <section style={{ padding: '48px 0 80px 0' }}>
      <div className="container">
        
        {/* Header */}
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
          marginBottom: '28px'
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <span className="badge-mint">
                <Shield size={14} /> {user?.role === 'ADMIN' ? 'Campus Administrator' : 'Authorized Staff'}
              </span>
            </div>
            <h2 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#0f172a' }}>
              Campus Administration & Staff Portal
            </h2>
            <p style={{ color: '#64748b', fontSize: '0.92rem' }}>
              Verify distinguishing ownership proof, authorize returns, approve staff registrations, and inspect audit logs.
            </p>
          </div>

          <button
            onClick={loadData}
            className="btn-secondary"
            style={{ padding: '8px 16px', fontSize: '0.88rem' }}
          >
            <RefreshCw size={14} /> Refresh
          </button>
        </div>

        {message && (
          <div style={{
            marginBottom: '20px',
            padding: '12px 18px',
            borderRadius: '12px',
            backgroundColor: message.type === 'success' ? '#ecfdf5' : '#fef2f2',
            border: `1px solid ${message.type === 'success' ? '#a7f3d0' : '#fecaca'}`,
            color: message.type === 'success' ? '#065f46' : '#991b1b',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '0.9rem'
          }}>
            {message.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
            <span>{message.text}</span>
          </div>
        )}

        {/* Tab Navigation */}
        <div style={{
          display: 'flex',
          gap: '8px',
          borderBottom: '1px solid #e2e8f0',
          marginBottom: '28px'
        }}>
          <button
            onClick={() => setActiveTab('claims')}
            style={{
              padding: '12px 20px',
              fontWeight: 700,
              fontSize: '0.92rem',
              color: activeTab === 'claims' ? '#065f46' : '#64748b',
              borderBottom: activeTab === 'claims' ? '2px solid #10b981' : '2px solid transparent',
              backgroundColor: 'transparent'
            }}
          >
            Claims to Review ({claims.length})
          </button>

          <button
            onClick={() => setActiveTab('items')}
            style={{
              padding: '12px 20px',
              fontWeight: 700,
              fontSize: '0.92rem',
              color: activeTab === 'items' ? '#065f46' : '#64748b',
              borderBottom: activeTab === 'items' ? '2px solid #10b981' : '2px solid transparent',
              backgroundColor: 'transparent'
            }}
          >
            All Registered Items ({items.length})
          </button>

          {user?.role === 'ADMIN' && (
            <button
              onClick={() => setActiveTab('staff')}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '12px 20px',
                fontWeight: 700,
                fontSize: '0.92rem',
                color: activeTab === 'staff' ? '#065f46' : '#64748b',
                borderBottom: activeTab === 'staff' ? '2px solid #10b981' : '2px solid transparent',
                backgroundColor: 'transparent'
              }}
            >
              <Users size={16} />
              Staff Registrations ({staffRequests.filter(s => s.accountStatus === 'PENDING_APPROVAL').length})
            </button>
          )}

          <button
            onClick={() => setActiveTab('audit')}
            style={{
              padding: '12px 20px',
              fontWeight: 700,
              fontSize: '0.92rem',
              color: activeTab === 'audit' ? '#065f46' : '#64748b',
              borderBottom: activeTab === 'audit' ? '2px solid #10b981' : '2px solid transparent',
              backgroundColor: 'transparent'
            }}
          >
            Audit Logs
          </button>
        </div>

        {/* Tab 1: Claims */}
        {activeTab === 'claims' && (
          <div>
            {loading ? (
              <div style={{ textAlign: 'center', padding: '60px 0', color: '#64748b' }}>
                Loading claims...
              </div>
            ) : claims.length === 0 ? (
              <div style={{
                textAlign: 'center',
                padding: '60px 20px',
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                border: '1px dashed #cbd5e1'
              }}>
                <Shield size={32} color="#94a3b8" style={{ margin: '0 auto 12px auto' }} />
                <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0f172a' }}>
                  No Ownership Claims Filed Yet
                </h4>
                <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '4px' }}>
                  When students file claims against found items, they will appear here for staff review.
                </p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {claims.map((claim) => (
                  <div
                    key={claim.id}
                    className="card"
                    style={{
                      border: claim.status === 'PENDING' ? '1.5px solid #a7f3d0' : '1px solid #e2e8f0',
                      backgroundColor: '#ffffff'
                    }}
                  >
                    <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: '12px', marginBottom: '14px' }}>
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                          <span style={{
                            padding: '3px 10px',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: claim.status === 'PENDING' ? '#fef3c7' : claim.status === 'APPROVED' ? '#ecfdf5' : '#fee2e2',
                            color: claim.status === 'PENDING' ? '#92400e' : claim.status === 'APPROVED' ? '#065f46' : '#991b1b',
                          }}>
                            {claim.status}
                          </span>
                          <span style={{ fontSize: '0.82rem', color: '#64748b' }}>
                            Submitted: {new Date(claim.createdAt).toLocaleDateString()}
                          </span>
                        </div>
                        <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a' }}>
                          Claimant: {claim.claimantName} ({claim.claimantEmail})
                        </h3>
                      </div>

                      {claim.status === 'PENDING' && (
                        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                          <button
                            type="button"
                            onClick={() => handleQuickRejectClaim(claim.id)}
                            style={{
                              padding: '8px 14px',
                              fontSize: '0.84rem',
                              fontWeight: 600,
                              borderRadius: '10px',
                              border: '1px solid #fca5a5',
                              backgroundColor: '#fef2f2',
                              color: '#dc2626',
                              cursor: 'pointer'
                            }}
                          >
                            Remove Pending Request
                          </button>
                          <button
                            onClick={() => {
                              setSelectedClaim(claim);
                              setDecision('APPROVED');
                            }}
                            className="btn-primary"
                            style={{ padding: '8px 18px', fontSize: '0.88rem' }}
                          >
                            Review & Resolve &rarr;
                          </button>
                        </div>
                      )}
                    </div>

                    <div style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                      gap: '14px',
                      marginBottom: '12px'
                    }}>
                      <div style={{ backgroundColor: '#f8fafc', padding: '12px 14px', borderRadius: '10px' }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>
                          Claim Message:
                        </span>
                        <p style={{ fontSize: '0.88rem', color: '#334155', marginTop: '4px' }}>
                          "{claim.claimMessage}"
                        </p>
                      </div>

                      <div style={{ backgroundColor: '#ecfdf5', padding: '12px 14px', borderRadius: '10px', border: '1px solid #d1fae5' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Lock size={13} color="#065f46" />
                          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#065f46', textTransform: 'uppercase' }}>
                            Distinguishing Proof (Private to Staff):
                          </span>
                        </div>
                        <p style={{ fontSize: '0.88rem', color: '#047857', marginTop: '4px', fontWeight: 600 }}>
                          {claim.proofDetails}
                        </p>
                      </div>
                    </div>

                    {claim.resolutionNotes && (
                      <div style={{ fontSize: '0.82rem', color: '#64748b', borderTop: '1px solid #f1f5f9', paddingTop: '8px' }}>
                        Resolution notes: <b>{claim.resolutionNotes}</b> by <b>{claim.resolvedBy}</b>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 2: All Items */}
        {activeTab === 'items' && (
          <div>
            {items.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '60px', color: '#64748b' }}>
                No items recorded in SQLite yet.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {items.map((item) => (
                  <div key={item.id} className="card" style={{ padding: '18px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div>
                        <div style={{ display: 'flex', gap: '6px', marginBottom: '6px' }}>
                          <span style={{
                            fontSize: '0.72rem',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '9999px',
                            backgroundColor: item.reportType === 'FOUND' ? '#ecfdf5' : '#eff6ff',
                            color: item.reportType === 'FOUND' ? '#065f46' : '#1e40af'
                          }}>
                            {item.reportType}
                          </span>
                          <span style={{ fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: '9999px', backgroundColor: '#f1f5f9', color: '#475569' }}>
                            {item.status}
                          </span>
                        </div>
                        <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0f172a' }}>
                          {item.category} {item.brand ? `• ${item.brand}` : ''}
                        </h4>
                        <p style={{ fontSize: '0.85rem', color: '#475569', marginTop: '2px' }}>
                          {item.description}
                        </p>
                      </div>

                      <div style={{ textAlign: 'right', fontSize: '0.8rem', color: '#64748b' }}>
                        <div>Reporter: <b>{item.reporterName}</b></div>
                        <div>Contact: <b>{item.reporterContact}</b></div>
                        <div>Date: {item.incidentDate}</div>
                      </div>
                    </div>

                    {item.privateDetails && (
                      <div style={{
                        marginTop: '10px',
                        padding: '8px 12px',
                        backgroundColor: '#fffbeb',
                        borderRadius: '8px',
                        fontSize: '0.82rem',
                        color: '#92400e',
                        border: '1px solid #fde68a'
                      }}>
                        <b>Private Verification Key:</b> {item.privateDetails}
                      </div>
                    )}

                    <div style={{
                      display: 'flex',
                      justifyContent: 'flex-end',
                      alignItems: 'center',
                      gap: '8px',
                      marginTop: '12px',
                      paddingTop: '10px',
                      borderTop: '1px solid #f1f5f9'
                    }}>
                      {item.status !== 'OPEN' && (
                        <button
                          type="button"
                          onClick={() => handleResetItemStatus(item.id, 'OPEN')}
                          className="btn-secondary"
                          style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                        >
                          Reset to OPEN
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => handleDeleteItem(item.id)}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          padding: '6px 12px',
                          fontSize: '0.8rem',
                          borderRadius: '8px',
                          border: '1px solid #fca5a5',
                          backgroundColor: '#fef2f2',
                          color: '#dc2626',
                          cursor: 'pointer',
                          fontWeight: 600
                        }}
                      >
                        <Trash2 size={13} />
                        Delete Report
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab: Staff Registrations */}
        {activeTab === 'staff' && (
          <div>
            {staffRequests.length === 0 ? (
              <div style={{
                textAlign: 'center',
                padding: '60px 20px',
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                border: '1px dashed #cbd5e1'
              }}>
                <Users size={32} color="#94a3b8" style={{ margin: '0 auto 12px auto' }} />
                <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0f172a' }}>
                  No Staff Applications Found
                </h4>
                <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '4px' }}>
                  When campus personnel register with the Campus Staff role, their applications appear here for verification.
                </p>
              </div>
            ) : (
              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
                  <thead style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                    <tr>
                      <th style={{ padding: '12px 16px' }}>Applicant</th>
                      <th style={{ padding: '12px 16px' }}>Campus Email</th>
                      <th style={{ padding: '12px 16px' }}>Requested Role</th>
                      <th style={{ padding: '12px 16px' }}>Status</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {staffRequests.map((req) => (
                      <tr key={req.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td style={{ padding: '12px 16px', fontWeight: 600, color: '#0f172a' }}>
                          {req.fullName || '—'}
                        </td>
                        <td style={{ padding: '12px 16px', color: '#334155' }}>
                          {req.campusEmail}
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          <span style={{
                            backgroundColor: '#ecfdf5',
                            color: '#065f46',
                            padding: '3px 8px',
                            borderRadius: '6px',
                            fontSize: '0.75rem',
                            fontWeight: 700
                          }}>
                            {req.requestedRole || req.role}
                          </span>
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          <span style={{
                            padding: '3px 10px',
                            borderRadius: '9999px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            backgroundColor: req.accountStatus === 'ACTIVE' ? '#dcfce7' : req.accountStatus === 'REJECTED' ? '#fee2e2' : '#fef3c7',
                            color: req.accountStatus === 'ACTIVE' ? '#15803d' : req.accountStatus === 'REJECTED' ? '#991b1b' : '#92400e',
                          }}>
                            {req.accountStatus}
                          </span>
                        </td>
                        <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                          {req.accountStatus === 'PENDING_APPROVAL' ? (
                            <div style={{ display: 'inline-flex', gap: '8px' }}>
                              <button
                                onClick={() => handleStaffApproval(req.id, 'ACTIVE')}
                                className="btn-primary"
                                style={{ padding: '5px 12px', fontSize: '0.8rem' }}
                              >
                                Approve Staff
                              </button>
                              <button
                                onClick={() => handleStaffApproval(req.id, 'REJECTED')}
                                style={{
                                  padding: '5px 12px',
                                  fontSize: '0.8rem',
                                  borderRadius: '8px',
                                  border: '1px solid #fca5a5',
                                  backgroundColor: '#fef2f2',
                                  color: '#dc2626',
                                  fontWeight: 600,
                                  cursor: 'pointer'
                                }}
                              >
                                Reject
                              </button>
                            </div>
                          ) : (
                            <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Processed</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Tab 3: System Audit Logs */}
        {activeTab === 'audit' && (
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
              <thead style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569' }}>
                <tr>
                  <th style={{ padding: '12px 16px' }}>Timestamp</th>
                  <th style={{ padding: '12px 16px' }}>Actor</th>
                  <th style={{ padding: '12px 16px' }}>Action</th>
                  <th style={{ padding: '12px 16px' }}>Target</th>
                  <th style={{ padding: '12px 16px' }}>Details</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                      No audit events logged yet.
                    </td>
                  </tr>
                ) : (
                  auditLogs.map((log) => (
                    <tr key={log.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '12px 16px', color: '#64748b', fontSize: '0.8rem' }}>
                        {new Date(log.createdAt).toLocaleString()}
                      </td>
                      <td style={{ padding: '12px 16px', fontWeight: 600, color: '#0f172a' }}>
                        {log.actor}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span style={{
                          backgroundColor: '#f1f5f9',
                          padding: '3px 8px',
                          borderRadius: '6px',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          color: '#334155'
                        }}>
                          {log.action}
                        </span>
                      </td>
                      <td style={{ padding: '12px 16px', color: '#64748b', fontSize: '0.82rem' }}>
                        {log.targetType} ({log.targetId.substring(0, 8)}...)
                      </td>
                      <td style={{ padding: '12px 16px', color: '#334155' }}>
                        {log.details}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

      </div>

      {/* Staff Resolution Modal */}
      {selectedClaim && (
        <div style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.45)',
          backdropFilter: 'blur(6px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px',
          zIndex: 60
        }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '24px',
            width: '100%',
            maxWidth: '520px',
            padding: '24px',
            boxShadow: '0 25px 50px -12px rgba(0,0,0,0.2)'
          }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a', marginBottom: '8px' }}>
              Resolve Ownership Claim
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#64748b', marginBottom: '20px' }}>
              Claim for item by <b>{selectedClaim.claimantName}</b> ({selectedClaim.claimantEmail}).
            </p>

            <form onSubmit={handleResolveClaim}>
              <div className="form-group">
                <label className="form-label">Review Decision</label>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    type="button"
                    onClick={() => setDecision('APPROVED')}
                    style={{
                      flex: 1,
                      padding: '10px',
                      borderRadius: '10px',
                      fontWeight: 700,
                      fontSize: '0.9rem',
                      border: decision === 'APPROVED' ? '2px solid #10b981' : '1px solid #cbd5e1',
                      backgroundColor: decision === 'APPROVED' ? '#ecfdf5' : '#ffffff',
                      color: decision === 'APPROVED' ? '#065f46' : '#64748b'
                    }}
                  >
                    Approve & Return
                  </button>
                  <button
                    type="button"
                    onClick={() => setDecision('REJECTED')}
                    style={{
                      flex: 1,
                      padding: '10px',
                      borderRadius: '10px',
                      fontWeight: 700,
                      fontSize: '0.9rem',
                      border: decision === 'REJECTED' ? '2px solid #ef4444' : '1px solid #cbd5e1',
                      backgroundColor: decision === 'REJECTED' ? '#fef2f2' : '#ffffff',
                      color: decision === 'REJECTED' ? '#991b1b' : '#64748b'
                    }}
                  >
                    Reject Claim
                  </button>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Resolution Notes</label>
                <textarea
                  className="form-input"
                  rows={3}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="e.g. Verified student ID and distinctive mark in person at Campus Security desk."
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Reviewing Staff Name / Title</label>
                <input
                  type="text"
                  className="form-input"
                  value={staffActor}
                  onChange={(e) => setStaffActor(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button
                  type="button"
                  onClick={() => setSelectedClaim(null)}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                >
                  Save Decision
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </section>
  );
};
