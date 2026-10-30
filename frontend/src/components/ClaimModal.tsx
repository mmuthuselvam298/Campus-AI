import React, { useState } from 'react';
import { X, ShieldCheck, Lock, CheckCircle2, AlertCircle } from 'lucide-react';
import { api } from '../services/api';
import { Item, Claim, AuthUser } from '../types';

interface ClaimModalProps {
  item: Item;
  onClose: () => void;
  onClaimSubmitted: (claim: Claim) => void;
  user?: AuthUser | null;
}

export const ClaimModal: React.FC<ClaimModalProps> = ({ item, onClose, onClaimSubmitted, user }) => {
  const [claimantName, setClaimantName] = useState(user?.fullName || '');
  const [claimantEmail, setClaimantEmail] = useState(user?.campusEmail || '');
  const [claimMessage, setClaimMessage] = useState('');
  const [proofDetails, setProofDetails] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!claimantName.trim() || !claimantEmail.trim() || !claimMessage.trim() || !proofDetails.trim()) {
      setError('Please provide all details including proof of ownership.');
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      const claim = await api.submitClaim({
        itemId: item.id,
        claimantName: claimantName.trim(),
        claimantEmail: claimantEmail.trim(),
        claimMessage: claimMessage.trim(),
        proofDetails: proofDetails.trim()
      });

      setSuccess(true);
      setTimeout(() => {
        onClaimSubmitted(claim);
        onClose();
      }, 1800);
    } catch (err: any) {
      setError(err.message || 'Failed to submit claim.');
      setSubmitting(false);
    }
  };

  return (
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
        maxWidth: '560px',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.18)',
        border: '1px solid #e2e8f0',
        overflow: 'hidden'
      }}>
        <div style={{
          padding: '20px 24px',
          borderBottom: '1px solid #f1f5f9',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: '#fafafa'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldCheck size={20} color="#10b981" />
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a' }}>
              Submit Ownership Claim
            </h3>
          </div>
          <button
            onClick={onClose}
            style={{
              width: '32px',
              height: '32px',
              borderRadius: '50%',
              backgroundColor: '#f1f5f9',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#64748b'
            }}
          >
            <X size={16} />
          </button>
        </div>

        <div style={{ padding: '24px' }}>
          {/* Target Item Summary */}
          <div style={{
            padding: '12px 16px',
            backgroundColor: '#f8fafc',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            marginBottom: '20px'
          }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>
              Claiming Item:
            </span>
            <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#0f172a' }}>
              {item.category} {item.brand ? `• ${item.brand}` : ''} ({item.location})
            </div>
            <p style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '2px' }}>
              {item.description}
            </p>
          </div>

          {error && (
            <div style={{
              marginBottom: '16px',
              padding: '10px 14px',
              backgroundColor: '#fef2f2',
              borderRadius: '10px',
              border: '1px solid #fecaca',
              color: '#991b1b',
              fontSize: '0.85rem',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {success ? (
            <div style={{ textAlign: 'center', padding: '30px 10px' }}>
              <div style={{
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                backgroundColor: '#ecfdf5',
                color: '#10b981',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 16px auto'
              }}>
                <CheckCircle2 size={32} />
              </div>
              <h4 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a', marginBottom: '6px' }}>
                Claim Registered Successfully!
              </h4>
              <p style={{ fontSize: '0.9rem', color: '#64748b' }}>
                Your claim has been submitted to campus lost & found staff for verification. You will be contacted at your campus email.
              </p>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Your Name *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={claimantName}
                    onChange={(e) => setClaimantName(e.target.value)}
                    placeholder="Full name"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Campus Email *</label>
                  <input
                    type="email"
                    className="form-input"
                    value={claimantEmail}
                    onChange={(e) => setClaimantEmail(e.target.value)}
                    placeholder="student@campus.edu"
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Circumstance / Message *</label>
                <textarea
                  className="form-input"
                  rows={2}
                  value={claimMessage}
                  onChange={(e) => setClaimMessage(e.target.value)}
                  placeholder="Where and when did you lose this item?"
                  required
                />
              </div>

              <div className="form-group" style={{
                backgroundColor: '#ecfdf5',
                padding: '14px',
                borderRadius: '12px',
                border: '1px solid #a7f3d0'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
                  <Lock size={14} color="#065f46" />
                  <label className="form-label" style={{ margin: 0, color: '#065f46' }}>
                    Proof of Ownership Details *
                  </label>
                </div>
                <textarea
                  className="form-input"
                  rows={2}
                  value={proofDetails}
                  onChange={(e) => setProofDetails(e.target.value)}
                  placeholder="Describe a distinguishing feature NOT shown publicly (e.g. stickers inside, engravings, contents, lock screen picture, serial number)..."
                  required
                />
                <span style={{ fontSize: '0.72rem', color: '#047857', display: 'block', marginTop: '4px' }}>
                  A photo alone is not proof. Staff verify these specific details before releasing items.
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button type="button" onClick={onClose} className="btn-secondary" disabled={submitting}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={submitting}>
                  {submitting ? 'Submitting Claim...' : 'Submit Claim'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
