import React, { useState } from 'react';
import { 
  X, 
  Plus, 
  Trash2, 
  FilePlus, 
  Upload, 
  Camera, 
  FileText, 
  CheckCircle, 
  AlertTriangle, 
  Sparkles, 
  ChevronRight, 
  ArrowLeft,
  Building,
  Calendar,
  Stethoscope,
  Pill
} from 'lucide-react';
import { api } from '../api';

const COMMON_INDIAN_PROVIDERS = [
  'Apollo Hospitals, New Delhi',
  'Fortis Escorts Heart Institute',
  'Max Super Speciality Hospital',
  'All India Institute of Medical Sciences (AIIMS)',
  'Dr. Lal PathLabs & Diagnostics',
  'Thyrocare Technologies',
  'Patient Self-Report (Omron Home Monitor)',
];

const CATEGORIES = [
  'General Medicine',
  'Cardiology',
  'Endocrinology & Diabetology',
  'Hematology & Biochemistry',
  'Pulmonology',
  'Orthopedics',
  'Gastroenterology',
  'Dermatology',
  'Preventative Health',
];

export default function AddRecordModal({ isOpen, onClose, patientId, persona, onRecordCreated }) {
  if (!isOpen) return null;

  // Step state: 'choice' -> 'manual' | 'ocr'
  const [currentStep, setCurrentStep] = useState('choice');

  // Manual Form State
  const defaultProvider = 
    persona.role === 'provider' ? 'Dr. Lal PathLabs & Diagnostics' :
    persona.role === 'doctor' ? 'Apollo Hospitals, New Delhi' :
    'Patient Self-Report';

  const [formData, setFormData] = useState({
    record_type: persona.role === 'patient' ? 'prescription' : 'visit',
    title: '',
    provider_name: defaultProvider,
    doctor_name: persona.role === 'doctor' ? 'Dr. Ananya Sharma' : '',
    doctor_specialty: persona.role === 'doctor' ? 'Interventional Cardiology' : '',
    date: new Date().toISOString().split('T')[0],
    category: 'General Medicine',
    description: '',
    follow_up_date: 'Review after 2 weeks',
    cost_inr: '500',
    attachment_name: '',
  });

  const [metrics, setMetrics] = useState([
    { key: 'Blood Pressure', value: '128/82 mmHg' },
    { key: 'Pulse Rate', value: '72 bpm' }
  ]);

  const [medications, setMedications] = useState([
    { name: 'Telmisartan', dosage: '40mg', frequency: 'Once daily morning after food', duration: '30 Days', instructions: 'Monitor BP weekly', price_inr: '185' }
  ]);

  // OCR Upload State
  const [prescriptionFile, setPrescriptionFile] = useState(null);
  const [prescriptionPreviewUrl, setPrescriptionPreviewUrl] = useState(null);
  const [isOcrProcessing, setIsOcrProcessing] = useState(false);
  const [ocrData, setOcrData] = useState(null);
  const [isUserConfirmed, setIsUserConfirmed] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const resetAll = () => {
    setCurrentStep('choice');
    setPrescriptionFile(null);
    setPrescriptionPreviewUrl(null);
    setOcrData(null);
    setIsUserConfirmed(false);
    setError(null);
  };

  const handleClose = () => {
    resetAll();
    onClose();
  };

  const handleAddMetric = () => {
    setMetrics([...metrics, { key: '', value: '' }]);
  };

  const handleRemoveMetric = (index) => {
    setMetrics(metrics.filter((_, i) => i !== index));
  };

  const handleMetricChange = (index, field, val) => {
    const updated = [...metrics];
    updated[index][field] = val;
    setMetrics(updated);
  };

  const handleAddMedication = () => {
    setMedications([...medications, { name: '', dosage: '', frequency: 'Once daily', duration: '14 Days', instructions: 'Take with water', price_inr: '100' }]);
  };

  const handleRemoveMedication = (index) => {
    setMedications(medications.filter((_, i) => i !== index));
  };

  const handleMedicationChange = (index, field, val) => {
    const updated = [...medications];
    updated[index][field] = val;
    setMedications(updated);
  };

  // Handle Prescription Image Selection
  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setPrescriptionFile(file);
      const url = URL.createObjectURL(file);
      setPrescriptionPreviewUrl(url);
      setFormData(prev => ({ ...prev, attachment_name: file.name }));
      setError(null);
    }
  };

  // Run Optical Text Recognition
  const handleRunOcr = async () => {
    setIsOcrProcessing(true);
    setError(null);
    try {
      const res = await api.ocrPrescription();
      setOcrData(res);
      // Pre-populate editable fields from OCR
      setFormData(prev => ({
        ...prev,
        title: res.title || 'Prescription Follow-up',
        provider_name: res.provider_name || 'Apollo Hospitals, New Delhi',
        doctor_name: res.doctor_name || 'Dr. Ananya Sharma',
        doctor_specialty: res.doctor_specialty || 'Cardiology',
        category: res.category || 'Cardiology',
        description: res.diagnosis || '',
        date: res.date || new Date().toISOString().split('T')[0],
        follow_up_date: res.follow_up_date || 'Review in 4 weeks',
      }));

      if (res.medications && res.medications.length > 0) {
        setMedications(res.medications.map(m => ({
          name: m.name || '',
          dosage: m.dosage || '',
          frequency: m.frequency || '',
          duration: m.duration || '30 Days',
          instructions: m.instructions || '',
          price_inr: m.price_inr ? String(m.price_inr) : '200'
        })));
      }
    } catch (err) {
      setError('OCR engine was temporarily unavailable. You can proceed with manual entry fallback.');
    } finally {
      setIsOcrProcessing(false);
    }
  };

  // Save Record
  const handleSubmitRecord = async (isFromOcr = false) => {
    if (!formData.title.trim()) {
      setError('Please provide a record title.');
      return;
    }
    if (!formData.description.trim() || formData.description.trim().length < 5) {
      setError('Clinical description/diagnosis must be at least 5 characters.');
      return;
    }
    if (isFromOcr && !isUserConfirmed) {
      setError('Please verify the extracted information and check the confirmation box before saving.');
      return;
    }

    setError(null);
    setSubmitting(true);

    const metricsDict = {};
    metrics.forEach(m => {
      if (m.key.trim() && m.value.trim()) {
        metricsDict[m.key.trim()] = m.value.trim();
      }
    });

    const validMeds = medications.filter(m => m.name.trim()).map(m => ({
      name: m.name.trim(),
      dosage: m.dosage.trim(),
      frequency: m.frequency.trim(),
      duration: m.duration.trim(),
      instructions: m.instructions.trim(),
      price_inr: parseFloat(m.price_inr) || 0
    }));

    const payload = {
      patient_id: patientId,
      record_type: formData.record_type,
      title: formData.title.trim(),
      provider_name: formData.provider_name.trim(),
      doctor_name: formData.doctor_name.trim(),
      doctor_specialty: formData.doctor_specialty.trim(),
      date: formData.date,
      description: formData.description.trim(),
      category: formData.category,
      metrics: metricsDict,
      medications: validMeds,
      verification_status: persona.role === 'patient' ? 'self_reported' : 'verified',
      follow_up_date: formData.follow_up_date.trim(),
      cost_inr: parseFloat(formData.cost_inr) || 0,
      attachment_name: formData.attachment_name || (isFromOcr ? 'prescription_scan.jpg' : ''),
    };

    try {
      const res = await api.createRecord(payload, persona);
      if (res.ok) {
        onRecordCreated(res.record);
        handleClose();
      } else {
        setError(res.error || 'Failed to save medical record.');
      }
    } catch (err) {
      setError('Network error while recording to clinical timeline.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[92vh] flex flex-col shadow-2xl border border-slate-200 overflow-hidden">
        
        {/* Header */}
        <div className="bg-[#0B3724] px-6 py-4 flex items-center justify-between text-white border-b border-[#144d34]">
          <div className="flex items-center gap-3">
            {currentStep !== 'choice' && (
              <button 
                onClick={() => setCurrentStep('choice')}
                className="p-1 rounded-lg hover:bg-white/10 text-emerald-200 transition-colors"
                title="Back to options"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
            )}
            <div>
              <h3 className="font-bold text-lg text-white">
                {currentStep === 'choice' && 'Add Medical Record'}
                {currentStep === 'manual' && 'Enter Consultation Details Manually'}
                {currentStep === 'ocr' && 'Upload Doctor\'s Prescription (OCR)'}
              </h3>
              <p className="text-xs text-emerald-200/80">
                Patient ID: <span className="font-mono text-emerald-300 font-bold">{patientId}</span> • Indian Health Records Architecture
              </p>
            </div>
          </div>
          <button 
            onClick={handleClose}
            className="p-1.5 rounded-lg text-emerald-200 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5 bg-[#F9FAF9]">

          {error && (
            <div className="p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-start gap-2.5">
              <AlertTriangle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* STEP 1: CHOICE SCREEN (Option A vs Option B) */}
          {currentStep === 'choice' && (
            <div className="space-y-4 py-2">
              <div className="text-center pb-2">
                <h4 className="font-semibold text-slate-800 text-base">Select Creation Mode</h4>
                <p className="text-xs text-slate-500 max-w-md mx-auto mt-1">
                  Choose structured manual input or photograph your prescription slip for optical text recognition.
                </p>
              </div>

              {/* Option A Card */}
              <div 
                onClick={() => setCurrentStep('manual')}
                className="group p-5 bg-white rounded-xl border border-slate-200 hover:border-emerald-600 hover:shadow-md cursor-pointer transition-all flex items-start gap-4"
              >
                <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-800 flex items-center justify-center shrink-0 border border-emerald-100 group-hover:bg-[#0B3724] group-hover:text-white transition-colors">
                  <FileText className="w-6 h-6" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 uppercase tracking-wider">
                      Option A
                    </span>
                    <span className="text-xs text-slate-400 font-medium">Direct Clinical Form</span>
                  </div>
                  <h5 className="font-bold text-slate-900 text-base mt-1 group-hover:text-emerald-800 transition-colors">
                    Enter Details Manually
                  </h5>
                  <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                    Input consultation date, hospital name, doctor details, diagnosis, prescribed medicines with dosages, vital signs, and follow-up advice.
                  </p>
                </div>
                <ChevronRight className="w-5 h-5 text-slate-400 group-hover:text-emerald-700 transition-transform group-hover:translate-x-1 shrink-0 self-center" />
              </div>

              {/* Option B Card */}
              <div 
                onClick={() => setCurrentStep('ocr')}
                className="group p-5 bg-white rounded-xl border-2 border-emerald-500/80 hover:border-emerald-700 hover:shadow-md cursor-pointer transition-all flex items-start gap-4 relative overflow-hidden"
              >
                <div className="absolute top-0 right-0 bg-[#0B3724] text-white text-[10px] font-bold px-3 py-0.5 rounded-bl-lg">
                  Smart Assisted
                </div>
                <div className="w-12 h-12 rounded-xl bg-[#0B3724] text-emerald-300 flex items-center justify-center shrink-0 border border-emerald-800">
                  <Camera className="w-6 h-6" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-emerald-700 text-emerald-100 uppercase tracking-wider">
                      Option B
                    </span>
                    <span className="text-xs font-semibold text-emerald-800">OCR Text Extraction</span>
                  </div>
                  <h5 className="font-bold text-slate-900 text-base mt-1 group-hover:text-emerald-800 transition-colors">
                    Upload Doctor's Prescription
                  </h5>
                  <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                    Pick a prescription image or document. Optical text recognition extracts medicines, dosage, dates, and doctor info for your verification before saving.
                  </p>
                </div>
                <ChevronRight className="w-5 h-5 text-emerald-700 transition-transform group-hover:translate-x-1 shrink-0 self-center" />
              </div>

              <div className="p-3.5 bg-slate-100 rounded-xl border border-slate-200 text-xs text-slate-600 flex items-center gap-2.5">
                <CheckCircle className="w-4 h-4 text-emerald-700 shrink-0" />
                <span>
                  All records are linked to your timeline in chronological order (DD MMM YYYY) and protected under patient consent rules.
                </span>
              </div>
            </div>
          )}

          {/* STEP 2: OPTION B (OCR Prescription Upload & Verification) */}
          {currentStep === 'ocr' && (
            <div className="space-y-4">
              {!ocrData ? (
                <div className="space-y-4">
                  <div className="border-2 border-dashed border-emerald-300 rounded-2xl p-6 bg-white text-center hover:bg-emerald-50/30 transition-colors">
                    <input 
                      type="file" 
                      id="prescription-file-input"
                      accept="image/*,.pdf" 
                      onChange={handleFileSelect}
                      className="hidden" 
                    />
                    <label htmlFor="prescription-file-input" className="cursor-pointer block space-y-2">
                      <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center mx-auto">
                        <Upload className="w-6 h-6" />
                      </div>
                      <div className="font-semibold text-sm text-slate-800">
                        {prescriptionFile ? prescriptionFile.name : 'Click to select prescription photo or PDF'}
                      </div>
                      <p className="text-xs text-slate-500">
                        Supports camera photo slips, JPG, PNG, or clinic PDF
                      </p>
                    </label>
                  </div>

                  {prescriptionPreviewUrl && (
                    <div className="p-3 bg-white rounded-xl border border-slate-200 flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <img 
                          src={prescriptionPreviewUrl} 
                          alt="Prescription Preview" 
                          className="w-14 h-14 object-cover rounded-lg border border-slate-200"
                        />
                        <div>
                          <div className="text-xs font-semibold text-slate-800">{prescriptionFile?.name}</div>
                          <div className="text-[11px] text-emerald-700 font-medium">Ready for OCR processing</div>
                        </div>
                      </div>
                      <button
                        onClick={handleRunOcr}
                        disabled={isOcrProcessing}
                        className="px-4 py-2 rounded-xl bg-[#0B3724] text-white text-xs font-bold hover:bg-[#124A32] flex items-center gap-2 shadow-sm transition-colors disabled:opacity-50"
                      >
                        {isOcrProcessing ? (
                          <>
                            <Sparkles className="w-3.5 h-3.5 animate-spin" />
                            <span>Extracting Data...</span>
                          </>
                        ) : (
                          <>
                            <Sparkles className="w-3.5 h-3.5 text-emerald-300" />
                            <span>Process with OCR</span>
                          </>
                        )}
                      </button>
                    </div>
                  )}

                  {/* Fallback button if user prefers manual */}
                  <div className="pt-2 text-center">
                    <button
                      type="button"
                      onClick={() => setCurrentStep('manual')}
                      className="text-xs font-semibold text-emerald-800 hover:text-emerald-950 underline underline-offset-4"
                    >
                      OCR not working or unavailable? Switch to Manual Entry Fallback →
                    </button>
                  </div>
                </div>
              ) : (
                /* OCR Extracted Data Review & Confirmation Form */
                <div className="space-y-4">
                  {/* Confidence and Disclaimer Banner */}
                  <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl text-xs space-y-1.5">
                    <div className="flex items-center gap-2 font-bold text-amber-900">
                      <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0" />
                      <span>Optical Extraction Confidence: {(ocrData.confidence_score * 100).toFixed(0)}% • Review Required</span>
                    </div>
                    <p className="text-amber-800 leading-relaxed">
                      {ocrData.disclaimer}
                    </p>
                    {ocrData.uncertain_fields?.length > 0 && (
                      <div className="pt-1 border-t border-amber-200/80">
                        <span className="font-semibold text-amber-950">Flags to check:</span>
                        <ul className="list-disc pl-4 text-amber-800 text-[11px] mt-0.5">
                          {ocrData.uncertain_fields.map((u, i) => <li key={i}>{u}</li>)}
                        </ul>
                      </div>
                    )}
                  </div>

                  {/* Editable Fields Pre-filled by OCR */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">Prescription Title *</label>
                      <input 
                        type="text" 
                        value={formData.title}
                        onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                        className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">Hospital / Clinic *</label>
                      <input 
                        type="text" 
                        value={formData.provider_name}
                        onChange={(e) => setFormData({ ...formData, provider_name: e.target.value })}
                        className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">Doctor Name</label>
                      <input 
                        type="text" 
                        value={formData.doctor_name}
                        onChange={(e) => setFormData({ ...formData, doctor_name: e.target.value })}
                        className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">Consultation Date</label>
                      <input 
                        type="date" 
                        value={formData.date}
                        onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                        className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Clinical Diagnosis & Instructions *</label>
                    <textarea 
                      rows={2}
                      value={formData.description}
                      onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                      className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                    />
                  </div>

                  {/* Extracted Medicines Table */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                        <Pill className="w-3.5 h-3.5 text-emerald-700" />
                        <span>Extracted Medicines ({medications.length})</span>
                      </label>
                      <button 
                        type="button"
                        onClick={handleAddMedication}
                        className="text-xs font-bold text-emerald-700 hover:text-emerald-900 flex items-center gap-1"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        <span>Add</span>
                      </button>
                    </div>

                    <div className="space-y-2">
                      {medications.map((med, i) => (
                        <div key={i} className="p-3 bg-white rounded-xl border border-slate-200 text-xs space-y-2">
                          <div className="flex items-center gap-2">
                            <input 
                              type="text" 
                              placeholder="Medicine Name (e.g. Telmisartan)"
                              value={med.name}
                              onChange={(e) => handleMedicationChange(i, 'name', e.target.value)}
                              className="flex-1 px-2.5 py-1.5 rounded-lg border border-slate-300 font-semibold text-slate-800"
                            />
                            <input 
                              type="text" 
                              placeholder="Dosage (40mg)"
                              value={med.dosage}
                              onChange={(e) => handleMedicationChange(i, 'dosage', e.target.value)}
                              className="w-24 px-2.5 py-1.5 rounded-lg border border-slate-300"
                            />
                            <button 
                              type="button" 
                              onClick={() => handleRemoveMedication(i)}
                              className="p-1.5 text-red-600 hover:bg-red-50 rounded-md"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                          <div className="grid grid-cols-2 gap-2 text-[11px]">
                            <input 
                              type="text" 
                              placeholder="Frequency (Once daily morning)"
                              value={med.frequency}
                              onChange={(e) => handleMedicationChange(i, 'frequency', e.target.value)}
                              className="px-2 py-1 rounded border border-slate-200 text-slate-600"
                            />
                            <input 
                              type="text" 
                              placeholder="Duration (30 Days)"
                              value={med.duration}
                              onChange={(e) => handleMedicationChange(i, 'duration', e.target.value)}
                              className="px-2 py-1 rounded border border-slate-200 text-slate-600"
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Mandatory Patient Confirmation Checkbox */}
                  <div className="p-3.5 bg-emerald-50/80 rounded-xl border border-emerald-200 flex items-start gap-2.5">
                    <input 
                      type="checkbox" 
                      id="confirm-ocr-checkbox"
                      checked={isUserConfirmed}
                      onChange={(e) => setIsUserConfirmed(e.target.checked)}
                      className="mt-0.5 h-4 w-4 rounded-sm border-slate-300 text-emerald-700 focus:ring-emerald-500"
                    />
                    <label htmlFor="confirm-ocr-checkbox" className="text-xs text-emerald-950 font-medium cursor-pointer leading-tight">
                      I have reviewed the extracted medicines, dosages, and instructions against the physical prescription and confirm this clinical data.
                    </label>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center justify-between pt-2">
                    <button
                      type="button"
                      onClick={() => setCurrentStep('manual')}
                      className="text-xs font-semibold text-slate-600 hover:text-slate-900"
                    >
                      ← Switch to Manual Form
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSubmitRecord(true)}
                      disabled={submitting || !isUserConfirmed}
                      className="px-5 py-2.5 rounded-xl bg-[#0B3724] text-white text-xs font-bold hover:bg-[#124A32] shadow-sm disabled:opacity-50 flex items-center gap-2"
                    >
                      {submitting ? 'Saving to Timeline...' : 'Confirm & Save Prescription Record'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* STEP 3: OPTION A (Manual Entry Form) */}
          {currentStep === 'manual' && (
            <form onSubmit={(e) => { e.preventDefault(); handleSubmitRecord(false); }} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Record Type *</label>
                  <select 
                    value={formData.record_type}
                    onChange={(e) => setFormData({ ...formData, record_type: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                  >
                    <option value="visit">Outpatient Consultation (OPD)</option>
                    <option value="prescription">Prescription</option>
                    <option value="lab_report">Laboratory / Diagnostic Report</option>
                    <option value="diagnosis">Clinical Diagnosis</option>
                    <option value="vital_signs">Vitals & Biometrics</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Medical Category *</label>
                  <select 
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                  >
                    {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Record Title *</label>
                <input 
                  type="text" 
                  placeholder="e.g. Cardiology OPD Follow-up & Prescription"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Hospital / Clinic Name *</label>
                  <input 
                    type="text" 
                    list="indian-hospitals-list"
                    value={formData.provider_name}
                    onChange={(e) => setFormData({ ...formData, provider_name: e.target.value })}
                    placeholder="e.g. Apollo Hospitals, New Delhi"
                    className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                  />
                  <datalist id="indian-hospitals-list">
                    {COMMON_INDIAN_PROVIDERS.map(p => <option key={p} value={p} />)}
                  </datalist>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Doctor Name & Specialty</label>
                  <input 
                    type="text" 
                    placeholder="Dr. Ananya Sharma (Cardiology)"
                    value={formData.doctor_name}
                    onChange={(e) => setFormData({ ...formData, doctor_name: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Consultation Date *</label>
                  <input 
                    type="date" 
                    value={formData.date}
                    onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Fee / Cost (₹)</label>
                  <input 
                    type="number" 
                    placeholder="500"
                    value={formData.cost_inr}
                    onChange={(e) => setFormData({ ...formData, cost_inr: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Diagnosis or Reason for Visit *</label>
                <textarea 
                  rows={3}
                  placeholder="Clinical assessment, diagnostics, complaints and recommendations..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                />
              </div>

              {/* Prescribed Medicines */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                    <Pill className="w-3.5 h-3.5 text-emerald-700" />
                    <span>Prescribed Medicines</span>
                  </label>
                  <button 
                    type="button"
                    onClick={handleAddMedication}
                    className="text-xs font-bold text-emerald-700 hover:text-emerald-900 flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Medicine</span>
                  </button>
                </div>

                <div className="space-y-2">
                  {medications.map((med, i) => (
                    <div key={i} className="p-3 bg-white rounded-xl border border-slate-200 text-xs space-y-2">
                      <div className="flex items-center gap-2">
                        <input 
                          type="text" 
                          placeholder="Medicine (e.g. Telmisartan)"
                          value={med.name}
                          onChange={(e) => handleMedicationChange(i, 'name', e.target.value)}
                          className="flex-1 px-2.5 py-1.5 rounded-lg border border-slate-300 font-semibold"
                        />
                        <input 
                          type="text" 
                          placeholder="Dosage (40mg)"
                          value={med.dosage}
                          onChange={(e) => handleMedicationChange(i, 'dosage', e.target.value)}
                          className="w-24 px-2.5 py-1.5 rounded-lg border border-slate-300"
                        />
                        <button 
                          type="button" 
                          onClick={() => handleRemoveMedication(i)}
                          className="p-1.5 text-red-600 hover:bg-red-50 rounded-md"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-[11px]">
                        <input 
                          type="text" 
                          placeholder="Frequency (Once daily morning)"
                          value={med.frequency}
                          onChange={(e) => handleMedicationChange(i, 'frequency', e.target.value)}
                          className="px-2 py-1 rounded border border-slate-200 text-slate-600"
                        />
                        <input 
                          type="text" 
                          placeholder="Duration (30 Days)"
                          value={med.duration}
                          onChange={(e) => handleMedicationChange(i, 'duration', e.target.value)}
                          className="px-2 py-1 rounded border border-slate-200 text-slate-600"
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Vitals / Test Results */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-slate-800">Clinical Metrics & Vitals</label>
                  <button 
                    type="button"
                    onClick={handleAddMetric}
                    className="text-xs font-bold text-emerald-700 hover:text-emerald-900 flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Metric</span>
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {metrics.map((m, i) => (
                    <div key={i} className="flex items-center gap-1.5 bg-white p-2 rounded-lg border border-slate-200">
                      <input 
                        type="text" 
                        placeholder="Metric Key (e.g. BP)"
                        value={m.key}
                        onChange={(e) => handleMetricChange(i, 'key', e.target.value)}
                        className="w-1/2 text-xs px-2 py-1 rounded border border-slate-300"
                      />
                      <input 
                        type="text" 
                        placeholder="Value (120/80)"
                        value={m.value}
                        onChange={(e) => handleMetricChange(i, 'value', e.target.value)}
                        className="w-1/2 text-xs px-2 py-1 rounded border border-slate-300 font-semibold"
                      />
                      <button 
                        type="button"
                        onClick={() => handleRemoveMetric(i)}
                        className="text-slate-400 hover:text-red-600 p-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Follow-up Advice</label>
                <input 
                  type="text" 
                  placeholder="e.g. Review after 2 weeks with repeat blood pressure chart"
                  value={formData.follow_up_date}
                  onChange={(e) => setFormData({ ...formData, follow_up_date: e.target.value })}
                  className="w-full text-xs px-3 py-2 bg-white rounded-lg border border-slate-300 focus:outline-hidden focus:border-emerald-600"
                />
              </div>

              {/* Footer Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
                <button
                  type="button"
                  onClick={handleClose}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/60 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2.5 rounded-xl bg-[#0B3724] text-white text-xs font-bold hover:bg-[#124A32] shadow-sm disabled:opacity-50 flex items-center gap-2"
                >
                  {submitting ? 'Recording...' : 'Commit Record to Patient Timeline'}
                </button>
              </div>
            </form>
          )}

        </div>
      </div>
    </div>
  );
}
