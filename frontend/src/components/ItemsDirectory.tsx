import React, { useState, useEffect } from 'react';
import { Item, ItemMatch } from '../types';
import { api } from '../services/api';
import { ItemCard } from './ItemCard';
import { Search, Filter, Plus, Sparkles, X, Check, ShieldAlert, FolderX } from 'lucide-react';

interface ItemsDirectoryProps {
  onOpenReport: (type: 'LOST' | 'FOUND') => void;
  onOpenClaim: (item: Item) => void;
}

export const ItemsDirectory: React.FC<ItemsDirectoryProps> = ({ onOpenReport, onOpenClaim }) => {
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Matches modal state
  const [activeItemMatches, setActiveItemMatches] = useState<{ item: Item; matches: ItemMatch[] } | null>(null);
  const [loadingMatches, setLoadingMatches] = useState(false);

  const fetchItems = async () => {
    try {
      setLoading(true);
      const data = await api.getItems(statusFilter || undefined, typeFilter || undefined, search || undefined);
      setItems(data);
    } catch (err) {
      console.error('Failed to load items', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchItems();
  }, [typeFilter, statusFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchItems();
  };

  const handleViewMatches = async (item: Item) => {
    try {
      setLoadingMatches(true);
      const matches = await api.getItemMatches(item.id);
      setActiveItemMatches({ item, matches });
    } catch (err) {
      console.error('Failed to load matches', err);
    } finally {
      setLoadingMatches(false);
    }
  };

  return (
    <section style={{ padding: '48px 0 80px 0' }}>
      <div className="container">
        
        {/* Header & Controls */}
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
          marginBottom: '32px'
        }}>
          <div>
            <h2 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.02em' }}>
              Campus Registry
            </h2>
            <p style={{ color: '#64748b', fontSize: '0.95rem' }}>
              Real reports recorded in the local SQLite database. No synthetic sample items.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              onClick={() => onOpenReport('FOUND')}
              className="btn-outline-mint"
              style={{ padding: '10px 18px', fontSize: '0.9rem' }}
            >
              <Plus size={16} /> Report Found
            </button>
            <button
              onClick={() => onOpenReport('LOST')}
              className="btn-primary"
              style={{ padding: '10px 20px', fontSize: '0.9rem' }}
            >
              <Plus size={16} /> Report Lost
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <div style={{
          backgroundColor: '#f8fafc',
          padding: '16px 20px',
          borderRadius: '16px',
          border: '1px solid #e2e8f0',
          marginBottom: '32px',
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          gap: '14px'
        }}>
          {/* Search */}
          <form onSubmit={handleSearchSubmit} style={{ flex: '1 1 280px', display: 'flex', gap: '8px' }}>
            <div style={{ position: 'relative', width: '100%' }}>
              <input
                type="text"
                className="form-input"
                placeholder="Search category, brand, description, location..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{ paddingLeft: '38px', borderRadius: '9999px' }}
              />
              <Search size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
            </div>
            <button type="submit" className="btn-secondary" style={{ padding: '10px 18px', borderRadius: '9999px', fontSize: '0.9rem' }}>
              Search
            </button>
          </form>

          {/* Type Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#64748b' }}>Type:</span>
            <select
              className="form-input"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              style={{ width: 'auto', padding: '8px 14px', borderRadius: '9999px' }}
            >
              <option value="">All Types</option>
              <option value="LOST">Lost Items</option>
              <option value="FOUND">Found Items</option>
            </select>
          </div>

          {/* Status Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#64748b' }}>Status:</span>
            <select
              className="form-input"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ width: 'auto', padding: '8px 14px', borderRadius: '9999px' }}
            >
              <option value="">All Statuses</option>
              <option value="OPEN">Open</option>
              <option value="MATCHED">Matched</option>
              <option value="CLAIMED">Claim Pending</option>
              <option value="RESOLVED">Resolved</option>
            </select>
          </div>
        </div>

        {/* Content Section */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '80px 20px', color: '#64748b' }}>
            <div className="pulse-glow" style={{ fontSize: '1.1rem', fontWeight: 600 }}>
              Querying SQLite Database...
            </div>
          </div>
        ) : items.length === 0 ? (
          /* Honest Empty State */
          <div style={{
            textAlign: 'center',
            padding: '72px 24px',
            backgroundColor: '#ffffff',
            borderRadius: '24px',
            border: '1.5px dashed #cbd5e1',
            maxWidth: '640px',
            margin: '0 auto'
          }}>
            <div style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              backgroundColor: '#ecfdf5',
              color: '#10b981',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 18px auto'
            }}>
              <FolderX size={32} />
            </div>
            <h3 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#0f172a', marginBottom: '8px' }}>
              Registry is Currently Empty
            </h3>
            <p style={{ color: '#64748b', fontSize: '0.95rem', lineHeight: 1.6, maxWidth: '440px', margin: '0 auto 28px auto' }}>
              The application starts completely clean with zero preloaded records. When students report lost or found items, they will appear here.
            </p>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
              <button
                onClick={() => onOpenReport('FOUND')}
                className="btn-primary"
                style={{ padding: '12px 24px', fontSize: '0.95rem' }}
              >
                + Report Found Item
              </button>
              <button
                onClick={() => onOpenReport('LOST')}
                className="btn-secondary"
                style={{ padding: '12px 24px', fontSize: '0.95rem' }}
              >
                + Report Lost Item
              </button>
            </div>
          </div>
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(310px, 1fr))',
            gap: '24px'
          }}>
            {items.map((item) => (
              <ItemCard
                key={item.id}
                item={item}
                onViewMatches={handleViewMatches}
                onClaim={onOpenClaim}
              />
            ))}
          </div>
        )}

      </div>

      {/* Matches Modal */}
      {activeItemMatches && (
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
            maxWidth: '680px',
            maxHeight: '85vh',
            display: 'flex',
            flexDirection: 'column',
            boxShadow: '0 25px 50px -12px rgba(0,0,0,0.2)',
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
              <div>
                <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#10b981', textTransform: 'uppercase' }}>
                  Matching Counterparts
                </span>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#0f172a' }}>
                  Matches for {activeItemMatches.item.category} ({activeItemMatches.item.reportType})
                </h3>
              </div>
              <button
                onClick={() => setActiveItemMatches(null)}
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

            <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
              {activeItemMatches.matches.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 10px' }}>
                  <p style={{ color: '#64748b', fontSize: '0.95rem' }}>
                    No counter-reports in the database meet the matching threshold for this item yet.
                  </p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  {activeItemMatches.matches.map((m) => (
                    <div
                      key={m.candidateItemId}
                      style={{
                        padding: '16px',
                        borderRadius: '16px',
                        border: '1.5px solid #a7f3d0',
                        backgroundColor: '#ffffff',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '12px',
                        boxShadow: '0 4px 10px rgba(16, 185, 129, 0.06)'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '14px' }}>
                        <div style={{ display: 'flex', gap: '14px', flex: 1 }}>
                          {m.item.imagePath && (
                            <img
                              src={m.item.imagePath}
                              alt={m.item.category}
                              style={{
                                width: '74px',
                                height: '74px',
                                borderRadius: '12px',
                                objectFit: 'cover',
                                border: '1px solid #e2e8f0',
                                flexShrink: 0
                              }}
                            />
                          )}
                          <div style={{ flex: 1 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px', flexWrap: 'wrap' }}>
                              <span style={{
                                backgroundColor: '#ecfdf5',
                                color: '#065f46',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                padding: '3px 8px',
                                borderRadius: '9999px'
                              }}>
                                {m.matchPercentage}% ESTIMATED MATCH &bull; {m.confidenceLabel}
                              </span>
                              <span style={{
                                backgroundColor: m.item.reportType === 'FOUND' ? '#ecfdf5' : '#eff6ff',
                                color: m.item.reportType === 'FOUND' ? '#065f46' : '#1d4ed8',
                                border: `1px solid ${m.item.reportType === 'FOUND' ? '#a7f3d0' : '#bfdbfe'}`,
                                fontSize: '0.72rem',
                                fontWeight: 700,
                                padding: '2px 8px',
                                borderRadius: '9999px'
                              }}>
                                COUNTERPART: {m.item.reportType}
                              </span>
                            </div>
                            <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0f172a' }}>
                              {m.item.category} {m.item.brand ? `• ${m.item.brand}` : ''}
                            </h4>
                            <p style={{ fontSize: '0.85rem', color: '#475569', marginTop: '2px' }}>
                              {m.item.description}
                            </p>
                          </div>
                        </div>
                        <button
                          onClick={() => {
                            setActiveItemMatches(null);
                            onOpenClaim(m.item);
                          }}
                          className="btn-primary"
                          style={{ padding: '8px 16px', fontSize: '0.82rem', whiteSpace: 'nowrap' }}
                        >
                          Claim Item
                        </button>
                      </div>

                      {/* Reasons */}
                      <div style={{ backgroundColor: '#f8fafc', padding: '10px 12px', borderRadius: '10px' }}>
                        <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>
                          Why AI matched this item:
                        </span>
                        {m.matchReasons.map((r, i) => (
                          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.82rem', color: '#334155' }}>
                            <Check size={13} color="#10b981" />
                            <span>{r}</span>
                          </div>
                        ))}
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.78rem', color: '#64748b', flexWrap: 'wrap' }}>
                        <span>Location: <b>{m.item.location}</b></span>
                        <span>Date: <b>{m.item.incidentDate}</b></span>
                        <span>Reporter: <b>{m.item.reporterName}</b></span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

    </section>
  );
};
