import React from 'react';
import { Item } from '../types';
import { MapPin, Calendar, Tag, Sparkles, ShieldCheck } from 'lucide-react';

interface ItemCardProps {
  item: Item;
  onViewMatches: (item: Item) => void;
  onClaim: (item: Item) => void;
}

export const ItemCard: React.FC<ItemCardProps> = ({ item, onViewMatches, onClaim }) => {
  const isFound = item.reportType === 'FOUND';

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OPEN':
        return <span style={{ backgroundColor: '#ecfdf5', color: '#065f46', border: '1px solid #a7f3d0', fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: '9999px' }}>OPEN</span>;
      case 'MATCHED':
        return <span style={{ backgroundColor: '#eff6ff', color: '#1e40af', border: '1px solid #bfdbfe', fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: '9999px' }}>MATCHED</span>;
      case 'CLAIMED':
        return <span style={{ backgroundColor: '#fffbeb', color: '#92400e', border: '1px solid #fde68a', fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: '9999px' }}>CLAIM PENDING</span>;
      case 'RESOLVED':
        return <span style={{ backgroundColor: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: '9999px' }}>RESOLVED</span>;
      default:
        return null;
    }
  };

  return (
    <div className="card card-hover" style={{ display: 'flex', flexDirection: 'column', height: '100%', padding: '20px' }}>
      {/* Top Banner / Image */}
      <div style={{ position: 'relative', marginBottom: '16px' }}>
        {item.imagePath ? (
          <div style={{
            height: '180px',
            borderRadius: '12px',
            overflow: 'hidden',
            backgroundColor: '#f1f5f9',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <img
              src={item.imagePath}
              alt={item.category}
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          </div>
        ) : (
          <div style={{
            height: '140px',
            borderRadius: '12px',
            backgroundColor: '#f8fafc',
            border: '1px dashed #cbd5e1',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#94a3b8'
          }}>
            <Tag size={28} />
            <span style={{ fontSize: '0.8rem', marginTop: '4px' }}>No photo uploaded</span>
          </div>
        )}

        {/* Floating Badges */}
        <div style={{ position: 'absolute', top: '10px', left: '10px', display: 'flex', gap: '6px' }}>
          <span style={{
            backgroundColor: isFound ? '#10b981' : '#3b82f6',
            color: '#ffffff',
            fontSize: '0.75rem',
            fontWeight: 700,
            padding: '3px 10px',
            borderRadius: '9999px',
            boxShadow: '0 2px 6px rgba(0,0,0,0.1)'
          }}>
            {item.reportType}
          </span>
          {getStatusBadge(item.status)}
        </div>
      </div>

      {/* Item Info */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', textTransform: 'capitalize', marginBottom: '4px' }}>
          {item.category} {item.brand ? `• ${item.brand}` : ''}
        </h3>

        <p style={{
          fontSize: '0.88rem',
          color: '#475569',
          lineHeight: 1.5,
          marginBottom: '12px',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden'
        }}>
          {item.description}
        </p>

        {/* Distinctive Features Pills */}
        {item.distinctiveFeatures && item.distinctiveFeatures.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginBottom: '14px' }}>
            {item.distinctiveFeatures.slice(0, 3).map((f, i) => (
              <span key={i} style={{
                fontSize: '0.72rem',
                backgroundColor: '#ecfdf5',
                color: '#065f46',
                border: '1px solid #d1fae5',
                padding: '2px 8px',
                borderRadius: '9999px',
                fontWeight: 600
              }}>
                {f}
              </span>
            ))}
            {item.distinctiveFeatures.length > 3 && (
              <span style={{ fontSize: '0.72rem', color: '#64748b', alignSelf: 'center' }}>
                +{item.distinctiveFeatures.length - 3} more
              </span>
            )}
          </div>
        )}

        <div style={{
          marginTop: 'auto',
          paddingTop: '12px',
          borderTop: '1px solid #f1f5f9',
          display: 'flex',
          flexDirection: 'column',
          gap: '6px',
          fontSize: '0.82rem',
          color: '#64748b'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <MapPin size={14} color="#10b981" />
            <span>{item.location}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Calendar size={14} color="#64748b" />
            <span>{item.incidentDate}</span>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginTop: '16px' }}>
        <button
          onClick={() => onViewMatches(item)}
          className="btn-outline-mint"
          style={{ padding: '8px 12px', fontSize: '0.85rem' }}
        >
          <Sparkles size={14} />
          AI Matches
        </button>

        <button
          onClick={() => onClaim(item)}
          className="btn-primary"
          style={{ padding: '8px 12px', fontSize: '0.85rem' }}
          disabled={item.status === 'RESOLVED' || item.status === 'CLOSED'}
        >
          <ShieldCheck size={14} />
          {item.status === 'RESOLVED' ? 'Resolved' : 'Claim Item'}
        </button>
      </div>
    </div>
  );
};
