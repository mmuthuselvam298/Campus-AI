import React, { useState, useRef } from 'react';
import { Camera, Upload, X, RotateCcw, Sparkles, Check, AlertCircle, ArrowRight, ShieldCheck, Tag, MapPin, Calendar, User, Mail, Lock } from 'lucide-react';
import { api } from '../services/api';
import { Item, ItemAnalysisResponse, ItemMatch, ReportType, AuthUser } from '../types';

interface ReportWorkflowProps {
  initialType: ReportType;
  onClose: () => void;
  onCompleted: (createdItem: Item, matches: ItemMatch[]) => void;
  onOpenClaimModal?: (item: Item) => void;
  user?: AuthUser | null;
}

type Stage = 'UPLOAD' | 'ANALYZING' | 'REVIEW' | 'SUBMITTING' | 'RESULTS';

export const ReportWorkflow: React.FC<ReportWorkflowProps> = ({
  initialType,
  onClose,
  onCompleted,
  onOpenClaimModal,
  user
}) => {
  const [reportType, setReportType] = useState<ReportType>(initialType);

  React.useEffect(() => {
    setReportType(initialType);
  }, [initialType]);

  const [stage, setStage] = useState<Stage>('UPLOAD');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [serverImagePath, setServerImagePath] = useState<string>('');
  const [aiNote, setAiNote] = useState<string>('');
  const [isAiFallback, setIsAiFallback] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>('');

  // Editable Form Fields (filled by AI + user corrections)
  const [category, setCategory] = useState('');
  const [colors, setColors] = useState<string[]>([]);
  const [newColorInput, setNewColorInput] = useState('');
  const [brand, setBrand] = useState('');
  const [description, setDescription] = useState('');
  const [features, setFeatures] = useState<string[]>([]);
  const [newFeatureInput, setNewFeatureInput] = useState('');
  const [keywords, setKeywords] = useState<string[]>([]);
  const [confidence, setConfidence] = useState<number>(0.85);

  // Essential additional details
  const [location, setLocation] = useState('');
  const [incidentDate, setIncidentDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [reporterName, setReporterName] = useState(user?.fullName || '');
  const [reporterContact, setReporterContact] = useState(user?.campusEmail || '');
  const [privateDetails, setPrivateDetails] = useState('');

  // Post-submission matches
  const [createdItem, setCreatedItem] = useState<Item | null>(null);
  const [matches, setMatches] = useState<ItemMatch[]>([]);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (file: File) => {
    if (!file.type.startsWith('image/')) {
      setErrorMessage('Please select a valid image file (JPG, PNG, WebP).');
      return;
    }
    if (file.size > 15 * 1024 * 1024) {
      setErrorMessage('File size exceeds 15MB limit.');
      return;
    }
    setErrorMessage('');
    setSelectedFile(file);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
  };

  const handleRetake = () => {
    setSelectedFile(null);
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setServerImagePath('');
    setErrorMessage('');
  };

  const startAiAnalysis = async () => {
    if (!selectedFile) {
      // If lost item without photo, proceed straight to manual entry
      if (reportType === 'LOST') {
        setStage('REVIEW');
        setConfidence(0.5);
        return;
      }
      setErrorMessage('Please provide a photo of the item you found.');
      return;
    }

    try {
      setStage('ANALYZING');
      setErrorMessage('');

      const result = await api.analyzeImage(selectedFile);
      setServerImagePath(result.imagePath);

      const ai = result.analysis;
      setCategory(ai.category || '');
      setColors(ai.colour || []);
      setBrand(ai.brand || '');
      setDescription(ai.description || '');
      setFeatures(ai.distinctiveFeatures || []);
      setKeywords(ai.searchKeywords || []);
      setConfidence(ai.confidence || 0.85);
      setIsAiFallback(ai.fallback || false);
      if (ai.note) setAiNote(ai.note);

      setStage('REVIEW');
    } catch (err: any) {
      console.error('AI Analysis failed:', err);
      // Honest error fallback
      setIsAiFallback(true);
      setAiNote('Vision AI analysis was unavailable. You can enter the item details manually below.');
      setStage('REVIEW');
    }
  };

  const handleAddColor = () => {
    if (newColorInput.trim() && !colors.includes(newColorInput.trim())) {
      setColors([...colors, newColorInput.trim()]);
      setNewColorInput('');
    }
  };

  const handleRemoveColor = (index: number) => {
    setColors(colors.filter((_, i) => i !== index));
  };

  const handleAddFeature = () => {
    if (newFeatureInput.trim() && !features.includes(newFeatureInput.trim())) {
      setFeatures([...features, newFeatureInput.trim()]);
      setNewFeatureInput('');
    }
  };

  const handleRemoveFeature = (index: number) => {
    setFeatures(features.filter((_, i) => i !== index));
  };

  const handleSubmitReport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!category.trim() || !description.trim() || !location.trim() || !incidentDate || !reporterName.trim() || !reporterContact.trim()) {
      setErrorMessage('Please complete all required fields.');
      return;
    }

    try {
      setStage('SUBMITTING');
      setErrorMessage('');

      const payload = {
        reportType,
        category: category.trim(),
        description: description.trim(),
        color: colors.join(', '),
        brand: brand.trim() || undefined,
        distinctiveFeatures: features,
        searchKeywords: keywords,
        imagePath: serverImagePath || undefined,
        location: location.trim(),
        incidentDate,
        reporterName: reporterName.trim(),
        reporterContact: reporterContact.trim(),
        privateDetails: privateDetails.trim() || undefined
      };

      const savedItem = reportType === 'LOST' 
        ? await api.reportLostItem(payload)
        : await api.reportFoundItem(payload);

      setCreatedItem(savedItem);

      // Search real database matches
      const matchResults = await api.getItemMatches(savedItem.id);
      setMatches(matchResults);
      setStage('RESULTS');
      onCompleted(savedItem, matchResults);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to submit report. Please try again.');
      setStage('REVIEW');
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
      zIndex: 50,
      overflowY: 'auto'
    }}>
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '24px',
        width: '100%',
        maxWidth: '720px',
        maxHeight: '90vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.18)',
        border: '1px solid #e2e8f0',
        overflow: 'hidden'
      }}>
        {/* Header */}
        <div style={{
          padding: '20px 24px',
          borderBottom: '1px solid #f1f5f9',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: '#fafafa'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{
              display: 'inline-block',
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              backgroundColor: reportType === 'FOUND' ? '#10b981' : '#3b82f6'
            }} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>
              {stage === 'RESULTS' 
                ? 'Report Registered & Possible Matches'
                : `Report ${reportType === 'FOUND' ? 'Found' : 'Lost'} Item`}
            </h2>
          </div>
          <button
            onClick={onClose}
            style={{
              width: '34px',
              height: '34px',
              borderRadius: '50%',
              backgroundColor: '#f1f5f9',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#64748b'
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Modal Body */}
        <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
          {errorMessage && (
            <div style={{
              marginBottom: '18px',
              padding: '12px 16px',
              backgroundColor: '#fef2f2',
              border: '1px solid #fecaca',
              borderRadius: '12px',
              color: '#991b1b',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '0.9rem'
            }}>
              <AlertCircle size={18} />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* STAGE 1: UPLOAD & PREVIEW */}
          {stage === 'UPLOAD' && (
            <div>
              {/* Type Switcher */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '10px',
                backgroundColor: '#f1f5f9',
                padding: '6px',
                borderRadius: '16px',
                marginBottom: '24px'
              }}>
                <button
                  type="button"
                  onClick={() => setReportType('LOST')}
                  style={{
                    padding: '12px 14px',
                    borderRadius: '12px',
                    fontWeight: 700,
                    fontSize: '0.92rem',
                    backgroundColor: reportType === 'LOST' ? '#2563eb' : 'transparent',
                    color: reportType === 'LOST' ? '#ffffff' : '#64748b',
                    boxShadow: reportType === 'LOST' ? '0 4px 12px rgba(37, 99, 235, 0.25)' : 'none',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    border: 'none',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease'
                  }}
                >
                  <span style={{ fontSize: '1.1rem' }}>🔍</span> I Lost An Item
                </button>
                <button
                  type="button"
                  onClick={() => setReportType('FOUND')}
                  style={{
                    padding: '12px 14px',
                    borderRadius: '12px',
                    fontWeight: 700,
                    fontSize: '0.92rem',
                    backgroundColor: reportType === 'FOUND' ? '#059669' : 'transparent',
                    color: reportType === 'FOUND' ? '#ffffff' : '#64748b',
                    boxShadow: reportType === 'FOUND' ? '0 4px 12px rgba(5, 150, 105, 0.25)' : 'none',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    border: 'none',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease'
                  }}
                >
                  <span style={{ fontSize: '1.1rem' }}>📦</span> I Found An Item
                </button>
              </div>

              {!previewUrl ? (
                <div>
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    style={{
                      border: '2px dashed #cbd5e1',
                      borderRadius: '18px',
                      padding: '48px 20px',
                      textAlign: 'center',
                      cursor: 'pointer',
                      backgroundColor: '#f8fafc',
                      transition: 'border-color 0.2s ease',
                      marginBottom: '16px'
                    }}
                  >
                    <div style={{
                      width: '64px',
                      height: '64px',
                      borderRadius: '50%',
                      backgroundColor: '#ecfdf5',
                      color: '#10b981',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      margin: '0 auto 16px auto'
                    }}>
                      <Camera size={30} />
                    </div>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#0f172a', marginBottom: '6px' }}>
                      Take a photo or upload an image
                    </h3>
                    <p style={{ fontSize: '0.9rem', color: '#64748b', maxWidth: '400px', margin: '0 auto 16px auto' }}>
                      Our Vision AI will automatically detect category, colors, brand markings, and distinguishing features.
                    </p>
                    <span className="btn-outline-mint" style={{ display: 'inline-flex' }}>
                      <Upload size={16} /> Choose File
                    </span>
                  </div>

                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    style={{ display: 'none' }}
                    onChange={(e) => {
                      if (e.target.files?.[0]) handleFileSelect(e.target.files[0]);
                    }}
                  />
                  
                  {/* Camera capture button for mobile */}
                  <input
                    ref={cameraInputRef}
                    type="file"
                    accept="image/*"
                    capture="environment"
                    style={{ display: 'none' }}
                    onChange={(e) => {
                      if (e.target.files?.[0]) handleFileSelect(e.target.files[0]);
                    }}
                  />

                  <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                    <button
                      type="button"
                      onClick={() => cameraInputRef.current?.click()}
                      className="btn-secondary"
                      style={{ padding: '10px 20px', fontSize: '0.9rem' }}
                    >
                      <Camera size={16} /> Use Camera
                    </button>

                    {reportType === 'LOST' && (
                      <button
                        type="button"
                        onClick={() => {
                          setStage('REVIEW');
                          setConfidence(0.5);
                        }}
                        style={{
                          padding: '10px 18px',
                          color: '#64748b',
                          fontSize: '0.9rem',
                          fontWeight: 600,
                          textDecoration: 'underline'
                        }}
                      >
                        Don't have a photo? Enter details manually &rarr;
                      </button>
                    )}
                  </div>
                </div>
              ) : (
                <div>
                  <div style={{
                    position: 'relative',
                    borderRadius: '16px',
                    overflow: 'hidden',
                    maxHeight: '340px',
                    backgroundColor: '#0f172a',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '16px',
                    border: '1px solid #e2e8f0'
                  }}>
                    <img
                      src={previewUrl}
                      alt="Item preview"
                      style={{ maxHeight: '340px', width: 'auto', maxWidth: '100%', objectFit: 'contain' }}
                    />
                    <button
                      onClick={handleRetake}
                      style={{
                        position: 'absolute',
                        top: '12px',
                        right: '12px',
                        backgroundColor: 'rgba(15, 23, 42, 0.75)',
                        color: '#ffffff',
                        padding: '8px 14px',
                        borderRadius: '9999px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        fontSize: '0.85rem',
                        fontWeight: 600
                      }}
                    >
                      <RotateCcw size={14} /> Retake / Replace
                    </button>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <button
                      type="button"
                      onClick={handleRetake}
                      className="btn-secondary"
                    >
                      Change Photo
                    </button>
                    <button
                      type="button"
                      onClick={startAiAnalysis}
                      className="btn-primary"
                    >
                      <Sparkles size={18} />
                      Analyze with AI &rarr;
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* STAGE 2: AI ANALYZING SCANNER */}
          {stage === 'ANALYZING' && (
            <div style={{ textAlign: 'center', padding: '60px 20px' }}>
              <div style={{
                width: '72px',
                height: '72px',
                borderRadius: '50%',
                backgroundColor: '#ecfdf5',
                border: '2px solid #10b981',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 24px auto',
                color: '#10b981'
              }} className="pulse-glow">
                <Sparkles size={36} />
              </div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#0f172a', marginBottom: '8px' }}>
                AI Vision Model is analyzing your photo...
              </h3>
              <p style={{ color: '#64748b', fontSize: '0.95rem', maxWidth: '440px', margin: '0 auto' }}>
                Detecting item category, brand markings, color palette, and distinctive features to find candidate matches.
              </p>
            </div>
          )}

          {/* STAGE 3: REVIEW AND CONFIRM */}
          {(stage === 'REVIEW' || stage === 'SUBMITTING') && (
            <form onSubmit={handleSubmitReport}>
              {/* AI Notice Banner */}
              <div style={{
                marginBottom: '20px',
                padding: '12px 16px',
                backgroundColor: isAiFallback ? '#fffbeb' : '#ecfdf5',
                border: `1px solid ${isAiFallback ? '#fde68a' : '#a7f3d0'}`,
                borderRadius: '12px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '10px'
              }}>
                <Sparkles size={18} color={isAiFallback ? '#d97706' : '#059669'} style={{ marginTop: '2px' }} />
                <div style={{ fontSize: '0.88rem' }}>
                  <span style={{ fontWeight: 700, color: isAiFallback ? '#92400e' : '#065f46' }}>
                    {isAiFallback ? 'Manual Description Mode' : 'AI Analysis Suggestions'}
                  </span>
                  <p style={{ color: isAiFallback ? '#b45309' : '#047857', marginTop: '2px' }}>
                    {aiNote || 'AI extracted the details below. Please review, edit, and add campus details before saving.'}
                  </p>
                </div>
              </div>

              {previewUrl && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '20px', padding: '10px', backgroundColor: '#f8fafc', borderRadius: '12px' }}>
                  <img src={previewUrl} alt="Thumbnail" style={{ width: '60px', height: '60px', borderRadius: '8px', objectFit: 'cover' }} />
                  <div>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#334155' }}>Uploaded Photograph</span>
                    <p style={{ fontSize: '0.75rem', color: '#64748b' }}>Will be securely stored and linked to this report.</p>
                  </div>
                </div>
              )}

              {/* Report Classification Selector */}
              <div style={{
                marginBottom: '22px',
                padding: '16px',
                backgroundColor: reportType === 'LOST' ? '#eff6ff' : '#ecfdf5',
                border: `1.5px solid ${reportType === 'LOST' ? '#93c5fd' : '#a7f3d0'}`,
                borderRadius: '16px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                  <label style={{ fontWeight: 700, fontSize: '0.92rem', color: reportType === 'LOST' ? '#1e3a8a' : '#065f46', margin: 0 }}>
                    Report Classification: <span style={{ textTransform: 'uppercase' }}>{reportType} ITEM</span>
                  </label>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Switch if needed before saving</span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <button
                    type="button"
                    onClick={() => setReportType('LOST')}
                    style={{
                      padding: '12px 14px',
                      borderRadius: '12px',
                      border: reportType === 'LOST' ? '2px solid #2563eb' : '1px solid #cbd5e1',
                      backgroundColor: reportType === 'LOST' ? '#ffffff' : 'rgba(255, 255, 255, 0.6)',
                      color: reportType === 'LOST' ? '#1d4ed8' : '#64748b',
                      fontWeight: 700,
                      fontSize: '0.88rem',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      boxShadow: reportType === 'LOST' ? '0 2px 6px rgba(37, 99, 235, 0.15)' : 'none'
                    }}
                  >
                    <span>🔍</span> I Lost This (Find matching found items)
                  </button>
                  <button
                    type="button"
                    onClick={() => setReportType('FOUND')}
                    style={{
                      padding: '12px 14px',
                      borderRadius: '12px',
                      border: reportType === 'FOUND' ? '2px solid #059669' : '1px solid #cbd5e1',
                      backgroundColor: reportType === 'FOUND' ? '#ffffff' : 'rgba(255, 255, 255, 0.6)',
                      color: reportType === 'FOUND' ? '#065f46' : '#64748b',
                      fontWeight: 700,
                      fontSize: '0.88rem',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      boxShadow: reportType === 'FOUND' ? '0 2px 6px rgba(5, 150, 105, 0.15)' : 'none'
                    }}
                  >
                    <span>📦</span> I Found This (Match with lost reports)
                  </button>
                </div>
              </div>

              {/* Form Fields */}
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Item Category *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    placeholder="e.g. water bottle, laptop, backpack, keys"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Visible Brand / Make</label>
                  <input
                    type="text"
                    className="form-input"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    placeholder="e.g. Hydro Flask, Apple, Nike (or leave blank)"
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Item Description *</label>
                <textarea
                  className="form-input"
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Clear description of the item's appearance..."
                  required
                />
              </div>

              {/* Colors */}
              <div className="form-group">
                <label className="form-label">Color(s)</label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '8px' }}>
                  {colors.map((c, i) => (
                    <span key={i} style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      backgroundColor: '#f1f5f9',
                      padding: '4px 10px',
                      borderRadius: '9999px',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      color: '#334155'
                    }}>
                      {c}
                      <button type="button" onClick={() => handleRemoveColor(i)} style={{ color: '#94a3b8' }}>&times;</button>
                    </span>
                  ))}
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input
                    type="text"
                    className="form-input"
                    value={newColorInput}
                    onChange={(e) => setNewColorInput(e.target.value)}
                    placeholder="Add color (e.g. blue, silver)"
                    onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleAddColor(); }}}
                  />
                  <button type="button" onClick={handleAddColor} className="btn-secondary" style={{ padding: '8px 14px', fontSize: '0.85rem' }}>
                    Add
                  </button>
                </div>
              </div>

              {/* Distinctive Features */}
              <div className="form-group">
                <label className="form-label">Public Distinctive Characteristics</label>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '8px' }}>
                  {features.map((f, i) => (
                    <span key={i} style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      backgroundColor: '#ecfdf5',
                      border: '1px solid #d1fae5',
                      padding: '4px 10px',
                      borderRadius: '9999px',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      color: '#065f46'
                    }}>
                      <Tag size={12} />
                      {f}
                      <button type="button" onClick={() => handleRemoveFeature(i)} style={{ color: '#047857' }}>&times;</button>
                    </span>
                  ))}
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input
                    type="text"
                    className="form-input"
                    value={newFeatureInput}
                    onChange={(e) => setNewFeatureInput(e.target.value)}
                    placeholder="Add feature (e.g. black screw cap, small sticker on corner)"
                    onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleAddFeature(); }}}
                  />
                  <button type="button" onClick={handleAddFeature} className="btn-secondary" style={{ padding: '8px 14px', fontSize: '0.85rem' }}>
                    Add
                  </button>
                </div>
              </div>

              {/* Campus Location & Incident Date */}
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Campus Location *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    placeholder="e.g. Main Library 2nd Floor, Student Union, Gym"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Date {reportType === 'FOUND' ? 'Found' : 'Lost'} *</label>
                  <input
                    type="date"
                    className="form-input"
                    value={incidentDate}
                    onChange={(e) => setIncidentDate(e.target.value)}
                    required
                  />
                </div>
              </div>

              {/* Reporter Contact */}
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Your Full Name *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={reporterName}
                    onChange={(e) => setReporterName(e.target.value)}
                    placeholder="e.g. Jordan Smith"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Campus Email or Phone *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={reporterContact}
                    onChange={(e) => setReporterContact(e.target.value)}
                    placeholder="e.g. jsmith@campus.edu"
                    required
                  />
                  <span style={{ fontSize: '0.72rem', color: '#64748b' }}>
                    Privacy protected: contact information is masked on public listings.
                  </span>
                </div>
              </div>

              {/* Private ownership proof details */}
              <div className="form-group" style={{
                backgroundColor: '#f8fafc',
                padding: '16px',
                borderRadius: '14px',
                border: '1px solid #e2e8f0'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
                  <Lock size={15} color="#059669" />
                  <label className="form-label" style={{ margin: 0, color: '#065f46' }}>
                    Private Ownership Verification Detail (Hidden from Public)
                  </label>
                </div>
                <textarea
                  className="form-input"
                  rows={2}
                  value={privateDetails}
                  onChange={(e) => setPrivateDetails(e.target.value)}
                  placeholder="e.g. Specific sticker hidden inside case, lock screen wallpaper, engraving, serial number..."
                />
                <span style={{ fontSize: '0.72rem', color: '#64748b', display: 'block', marginTop: '4px' }}>
                  Only authorized staff and you can see this. It is used to verify true ownership when someone files a claim.
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '24px' }}>
                <button
                  type="button"
                  onClick={() => setStage('UPLOAD')}
                  className="btn-secondary"
                  disabled={stage === 'SUBMITTING'}
                >
                  &larr; Back to Photo
                </button>

                <button
                  type="submit"
                  className="btn-primary"
                  disabled={stage === 'SUBMITTING'}
                  style={{
                    backgroundColor: reportType === 'LOST' ? '#2563eb' : '#059669',
                    borderColor: reportType === 'LOST' ? '#1d4ed8' : '#047857'
                  }}
                >
                  {stage === 'SUBMITTING' ? (
                    'Saving & Matching...'
                  ) : (
                    <>
                      <Check size={18} />
                      {reportType === 'LOST' ? 'Save Lost Report & Find Matches →' : 'Save Found Report & Find Matches →'}
                    </>
                  )}
                </button>
              </div>
            </form>
          )}

          {/* STAGE 4: RESULTS DISPLAY */}
          {stage === 'RESULTS' && (
            <div>
              <div style={{
                padding: '16px 20px',
                backgroundColor: '#ecfdf5',
                borderRadius: '16px',
                border: '1px solid #a7f3d0',
                marginBottom: '24px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}>
                <div>
                  <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#065f46', textTransform: 'uppercase' }}>
                    Report Saved to SQLite Registry
                  </span>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0f172a' }}>
                    {createdItem?.category} ({createdItem?.reportType})
                  </h3>
                  <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                    Location: {createdItem?.location} &bull; Date: {createdItem?.incidentDate}
                  </p>
                </div>
                <span className="badge-mint">Active in Registry</span>
              </div>

              {/* Match Header */}
              <div style={{ marginBottom: '16px' }}>
                <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a' }}>
                  Potential Counterpart Matches ({matches.length})
                </h4>
                <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                  Ranked by AI category, brand, colour, distinctive feature overlap, and location signals.
                </p>
              </div>

              {matches.length === 0 ? (
                <div style={{
                  padding: '40px 20px',
                  textAlign: 'center',
                  backgroundColor: '#f8fafc',
                  borderRadius: '16px',
                  border: '1px dashed #cbd5e1'
                }}>
                  <div style={{
                    width: '48px',
                    height: '48px',
                    borderRadius: '50%',
                    backgroundColor: '#e2e8f0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 12px auto',
                    color: '#64748b'
                  }}>
                    <ShieldCheck size={24} />
                  </div>
                  <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', marginBottom: '4px' }}>
                    No matching counterpart reports found yet
                  </h4>
                  <p style={{ fontSize: '0.88rem', color: '#64748b', maxWidth: '440px', margin: '0 auto 18px auto' }}>
                    Your report is permanently saved in the campus SQLite registry as an active <b>{createdItem?.reportType}</b> item. 
                    {createdItem?.reportType === 'FOUND' 
                      ? ' When a student registers a matching LOST report, the AI matcher will pair them.' 
                      : ' When someone finds and logs your item, the AI matcher will pair them.'}
                  </p>
                  <button onClick={onClose} className="btn-primary" style={{ padding: '10px 24px', fontSize: '0.9rem' }}>
                    Done & Return to Home
                  </button>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  {/* Celebration Banner */}
                  <div style={{
                    padding: '16px 20px',
                    backgroundColor: '#ecfdf5',
                    borderRadius: '16px',
                    border: '2px solid #10b981',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '14px',
                    boxShadow: '0 4px 14px rgba(16, 185, 129, 0.12)'
                  }}>
                    <div style={{
                      width: '44px',
                      height: '44px',
                      borderRadius: '50%',
                      backgroundColor: '#10b981',
                      color: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '1.4rem',
                      flexShrink: 0
                    }}>
                      🎯
                    </div>
                    <div style={{ flex: 1 }}>
                      <h4 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#065f46', margin: 0 }}>
                        Counterpart Match Identified! ({matches.length} found)
                      </h4>
                      <p style={{ fontSize: '0.84rem', color: '#047857', margin: '2px 0 0 0' }}>
                        AI paired your {createdItem?.reportType} report with matching {createdItem?.reportType === 'LOST' ? 'FOUND' : 'LOST'} items in the registry.
                      </p>
                    </div>
                  </div>

                  {matches.map((m) => (
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
                                width: '76px',
                                height: '76px',
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
                                padding: '2px 8px',
                                borderRadius: '9999px'
                              }}>
                                {m.matchPercentage}% ESTIMATED MATCH
                              </span>
                              <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#059669' }}>
                                {m.confidenceLabel}
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
                            <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a' }}>
                              {m.item.category} {m.item.brand ? `(${m.item.brand})` : ''}
                            </h4>
                            <p style={{ fontSize: '0.85rem', color: '#475569', marginTop: '2px' }}>
                              {m.item.description}
                            </p>
                          </div>
                        </div>

                        {onOpenClaimModal && (
                          <button
                            onClick={() => onOpenClaimModal(m.item)}
                            className="btn-primary"
                            style={{ padding: '8px 16px', fontSize: '0.85rem', whiteSpace: 'nowrap' }}
                          >
                            Submit Claim
                          </button>
                        )}
                      </div>

                      {/* Match Reasons */}
                      <div style={{
                        backgroundColor: '#f8fafc',
                        padding: '10px 14px',
                        borderRadius: '10px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '4px'
                      }}>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>
                          Why AI flagged this match:
                        </span>
                        {m.matchReasons.map((reason, idx) => (
                          <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.82rem', color: '#334155' }}>
                            <Check size={13} color="#10b981" />
                            <span>{reason}</span>
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

                  <div style={{ textAlign: 'center', marginTop: '14px' }}>
                    <button onClick={onClose} className="btn-secondary" style={{ padding: '10px 24px', fontSize: '0.9rem' }}>
                      Close & Go to Registry
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

        </div>
      </div>
    </div>
  );
};
