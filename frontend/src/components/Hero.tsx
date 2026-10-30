import React from 'react';
import { Camera, Search, Sparkles, CheckCircle2, ArrowRight } from 'lucide-react';

interface HeroProps {
  onOpenReport: (type: 'LOST' | 'FOUND') => void;
  onExploreRegistry: () => void;
}

export const Hero: React.FC<HeroProps> = ({ onOpenReport, onExploreRegistry }) => {
  return (
    <section className="hero-gradient" style={{ padding: '64px 0 80px 0', textAlign: 'center' }}>
      <div className="container" style={{ maxWidth: '820px' }}>
        
        {/* Status Pill */}
        <div style={{ marginBottom: '24px' }}>
          <span className="badge-mint animate-fade-in">
            <Sparkles size={14} />
            AI-Powered Visual Recognition & Match Engine
          </span>
        </div>

        {/* Hero Heading */}
        <h1 style={{
          fontSize: 'clamp(2.3rem, 5vw, 3.4rem)',
          fontWeight: 800,
          lineHeight: 1.15,
          color: '#0f172a',
          letterSpacing: '-0.03em',
          marginBottom: '20px'
        }}>
          Lost something on campus? <br />
          <span style={{
            background: 'linear-gradient(135deg, #10b981 0%, #047857 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent'
          }}>
            Let's find it.
          </span>
        </h1>

        {/* Hero Subheading */}
        <p style={{
          fontSize: '1.18rem',
          color: '#475569',
          lineHeight: 1.6,
          maxWidth: '620px',
          margin: '0 auto 36px auto'
        }}>
          Take a photo. Let AI identify your item and help you find a match.
        </p>

        {/* Primary Action Buttons */}
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '14px',
          marginBottom: '56px'
        }}>
          <button
            onClick={() => onOpenReport('FOUND')}
            className="btn-primary"
            style={{ padding: '16px 36px', fontSize: '1.05rem' }}
          >
            <Camera size={20} />
            I Found Something
          </button>

          <button
            onClick={() => onOpenReport('LOST')}
            className="btn-secondary"
            style={{ padding: '16px 36px', fontSize: '1.05rem' }}
          >
            <Search size={20} />
            I Lost Something
          </button>
        </div>

        {/* Modern Interactive Match Visual Demonstration */}
        <div style={{
          maxWidth: '560px',
          margin: '0 auto',
          padding: '24px',
          backgroundColor: 'rgba(255, 255, 255, 0.85)',
          backdropFilter: 'blur(12px)',
          borderRadius: '24px',
          border: '1.5px solid #d1fae5',
          boxShadow: '0 20px 40px -15px rgba(16, 185, 129, 0.12)',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid #f1f5f9', paddingBottom: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#10b981' }} />
              <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#334155', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Automated Verification Flow
              </span>
            </div>
            <span style={{ fontSize: '0.78rem', color: '#10b981', fontWeight: 600 }}>
              Live SQLite Database
            </span>
          </div>

          <div style={{
            display: 'grid',
            gridTemplateColumns: '1fr auto 1fr',
            alignItems: 'center',
            gap: '12px',
            textAlign: 'left'
          }}>
            {/* Left box: Photo input */}
            <div style={{
              padding: '14px',
              backgroundColor: '#f8fafc',
              borderRadius: '14px',
              border: '1px solid #e2e8f0'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
                <Camera size={14} color="#10b981" />
                <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#0f172a' }}>1. Snap Photo</span>
              </div>
              <p style={{ fontSize: '0.72rem', color: '#64748b' }}>
                Vision AI extracts colors, brand, and details
              </p>
            </div>

            {/* Middle: Match icon */}
            <div style={{
              width: '32px',
              height: '32px',
              borderRadius: '50%',
              backgroundColor: '#ecfdf5',
              border: '1px solid #a7f3d0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#059669'
            }}>
              <ArrowRight size={16} />
            </div>

            {/* Right box: Match result */}
            <div style={{
              padding: '14px',
              backgroundColor: '#ecfdf5',
              borderRadius: '14px',
              border: '1px solid #a7f3d0'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
                <CheckCircle2 size={14} color="#059669" />
                <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#065f46' }}>2. AI Match</span>
              </div>
              <p style={{ fontSize: '0.72rem', color: '#047857' }}>
                Ranks verified candidates with explanation
              </p>
            </div>
          </div>
        </div>

      </div>
    </section>
  );
};
