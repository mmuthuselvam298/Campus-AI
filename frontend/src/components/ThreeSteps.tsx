import React from 'react';
import { Camera, Cpu, Users } from 'lucide-react';

export const ThreeSteps: React.FC = () => {
  const steps = [
    {
      num: '01',
      title: 'Photograph your item',
      desc: 'Use your phone camera or upload a clear photo. No need to write lengthy descriptions by hand.',
      icon: <Camera size={22} color="#10b981" />,
    },
    {
      num: '02',
      title: 'AI analyses and finds matches',
      desc: 'Vision AI extracts category, colours, brand, and distinctive features, then ranks real reports.',
      icon: <Cpu size={22} color="#10b981" />,
    },
    {
      num: '03',
      title: 'Connect with the right person',
      desc: 'Verify proof of ownership through private distinctive details and safely coordinate returns.',
      icon: <Users size={22} color="#10b981" />,
    },
  ];

  return (
    <section style={{ padding: '64px 0', borderBottom: '1px solid var(--border-medium)' }}>
      <div className="container">
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#0f172a', letterSpacing: '-0.02em', marginBottom: '8px' }}>
            How Campus Lost & Found AI Works
          </h2>
          <p style={{ color: '#64748b', fontSize: '1rem', maxWidth: '520px', margin: '0 auto' }}>
            A fast, three-step workflow designed to return belongings to their rightful owners safely.
          </p>
        </div>

        <div className="grid-3">
          {steps.map((step) => (
            <div
              key={step.num}
              className="card card-hover"
              style={{
                position: 'relative',
                display: 'flex',
                flexDirection: 'column',
                gap: '14px',
                borderRadius: '16px',
                border: '1px solid #e2e8f0'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{
                  width: '46px',
                  height: '46px',
                  borderRadius: '12px',
                  backgroundColor: '#ecfdf5',
                  border: '1px solid #d1fae5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  {step.icon}
                </div>
                <span style={{ fontSize: '1.25rem', fontWeight: 800, color: '#cbd5e1' }}>
                  {step.num}
                </span>
              </div>

              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '6px' }}>
                  {step.title}
                </h3>
                <p style={{ fontSize: '0.92rem', color: '#64748b', lineHeight: 1.55 }}>
                  {step.desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
