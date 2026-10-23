import React, { useState } from 'react';
import { ShieldAlert, X, Send, Lock } from 'lucide-react';

const COMMON_REASONS = [
  'Comprehensive cardiovascular assessment & medication reconciliation',
  'Secondary diagnostic review of recent laboratory panels',
  'Specialist referral and clinical history evaluation',
  'Chronic condition management follow-up',
];

export default function RequestAccessModal({ isOpen, onClose, patientId, patientName, persona, onRequestSubmitted }) {
  if (!isOpen) return null;

  const [reason, setReason] = useState(COMMON_REASONS[0]);
  const [scope, setScope] = useState('all_records');
  const [organization, setOrganization] = useState(
    persona.role === 'doctor' ? 'City Heart Institute / St. Jude Cardiology' : 'Apex Diagnostics Facility'
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason.trim() || reason.trim().length < 5) {
      setError('Please provide a clinical rationale of at least 5 characters.');
      return;
    }

    setSubmitting(true);
    setError(null);

    const payload = {
      patient_id: patientId,
      requester_id: persona.id,
      requester_name: persona.name,
      requester_role: persona.role,
      requester_organization: organization.trim(),
      reason: reason.trim(),
      scope: scope,
    };

    try {
      await onRequestSubmitted(payload);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to submit request');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-200">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center">
              <Lock className="w-5 h-5 text-amber-700" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">Request Patient Record Access</h2>
              <p className="text-xs text-slate-500">
                Patient: <span className="font-semibold text-slate-800">{patientName} ({patientId})</span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
              {error}
            </div>
          )}

          <div className="p-3 rounded-xl bg-emerald-50/80 border border-emerald-200 text-xs text-emerald-900 leading-relaxed">
            <div className="font-semibold flex items-center gap-1.5 mb-1 text-emerald-950">
              <ShieldAlert className="w-4 h-4 text-emerald-700" />
              Patient-Centric Zero-Trust Governance
            </div>
            Under MedIt privacy controls, health records remain shielded until the patient reviews and grants affirmative consent.
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Clinical Rationale / Purpose *
            </label>
            <textarea
              rows={3}
              required
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Explain why clinical access is required for patient care..."
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            />
            <div className="mt-1 flex flex-wrap gap-1">
              {COMMON_REASONS.map((r, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => setReason(r)}
                  className="text-[10px] text-slate-500 hover:text-emerald-700 bg-slate-100 hover:bg-emerald-50 px-2 py-0.5 rounded transition-colors text-left"
                >
                  Quick: {r.slice(0, 32)}...
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Requesting Organization / Affiliation
            </label>
            <input
              type="text"
              required
              value={organization}
              onChange={(e) => setOrganization(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Scope of Access Requested
            </label>
            <select
              value={scope}
              onChange={(e) => setScope(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 bg-white"
            >
              <option value="all_records">Full Unified Timeline (All Records)</option>
              <option value="lab_reports">Diagnostic & Laboratory Reports Only</option>
              <option value="prescriptions">Medications & Prescriptions Only</option>
            </select>
          </div>

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2 rounded-xl text-xs font-semibold bg-[#0B3724] hover:bg-[#124A32] text-white shadow-sm transition-all flex items-center gap-1.5 disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              {submitting ? 'Submitting...' : 'Send Access Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
