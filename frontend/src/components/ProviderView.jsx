import React, { useState, useEffect } from 'react';
import { 
  Building2, 
  FlaskConical, 
  Plus, 
  CheckCircle, 
  XCircle, 
  History, 
  ShieldCheck, 
  FileText, 
  Clock, 
  Share2, 
  RefreshCw 
} from 'lucide-react';
import AddRecordModal from './AddRecordModal';
import { api } from '../api';

const SAMPLE_INCOMING_REQUESTS = [
  {
    id: 'REQ-SHR-401',
    from_clinic: 'Metro Cardiology Specialists',
    patient_id: 'PAT-101',
    patient_name: 'Sarah Jenkins',
    requested_records: 'Comprehensive Metabolic Panel & Lipid Profile (2026-03-12)',
    purpose: 'Cardiac risk assessment and pre-medication baseline reconciliation',
    status: 'pending',
    timestamp: '2026-03-20 11:20:00Z',
  },
  {
    id: 'REQ-SHR-402',
    from_clinic: 'City Endocrinology Associates',
    patient_id: 'PAT-102',
    patient_name: 'Robert Chen',
    requested_records: 'HbA1c & Glycated Protein Lab Panels',
    purpose: 'Quarterly diabetic care management review',
    status: 'approved',
    timestamp: '2026-03-19 14:05:00Z',
  },
];

export default function ProviderView({ persona, onShowToast }) {
  const [incomingRequests, setIncomingRequests] = useState(SAMPLE_INCOMING_REQUESTS);
  const [selectedPatientId, setSelectedPatientId] = useState('PAT-101');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loadingAudit, setLoadingAudit] = useState(false);

  const fetchAudit = async () => {
    setLoadingAudit(true);
    try {
      const data = await api.getAuditLogs();
      setAuditLogs(data.audit_logs || []);
    } catch (err) {
      console.error('Audit fetch error', err);
    } finally {
      setLoadingAudit(false);
    }
  };

  useEffect(() => {
    fetchAudit();
  }, []);

  const handleShareDecision = (reqId, decision) => {
    setIncomingRequests((prev) =>
      prev.map((r) => (r.id === reqId ? { ...r, status: decision } : r))
    );
    onShowToast({
      type: decision === 'approved' ? 'success' : 'warning',
      title: decision === 'approved' ? 'Clinical Records Dispatched' : 'Sharing Request Declined',
      message: `Request ${reqId} was ${decision} under provider data sharing protocol.`,
    });
    fetchAudit();
  };

  const handleRecordCreated = async (payload) => {
    const res = await api.createRecord(payload, persona);
    if (res.ok) {
      onShowToast({
        type: 'success',
        title: 'Diagnostic Record Published',
        message: 'The clinical record is now permanently integrated into the patient health timeline.',
      });
      fetchAudit();
    } else {
      throw new Error(res.error || 'Failed to create record');
    }
  };

  return (
    <div className="space-y-6">
      {/* Provider Portal Header Card */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-emerald-800 to-[#0B3724] text-white flex items-center justify-center font-bold text-2xl shadow-md">
              <FlaskConical className="w-7 h-7 text-emerald-300" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-bold text-slate-900">
                  {persona.name}
                </h1>
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-semibold border border-emerald-200">
                  Certified Diagnostic Lab (CLIA/CAP)
                </span>
              </div>
              <div className="text-xs text-slate-500 mt-1">
                Connected Node ID: <span className="font-mono text-slate-700">{persona.id}</span> • Diagnostic Laboratory & Specimen Analysis Center
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsAddModalOpen(true)}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-[#0B3724] hover:bg-[#124A32] text-white shadow-sm transition-all flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              Dispatch Diagnostic Record
            </button>
          </div>
        </div>

        {/* Quick capabilities banner */}
        <div className="mt-4 pt-3 border-t border-slate-100 flex flex-wrap items-center gap-2 text-xs">
          <span className="text-slate-400 font-medium">Capabilities:</span>
          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700">Complete Blood Count (CBC)</span>
          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700">Comprehensive Metabolic Panel</span>
          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700">Lipid Panels</span>
          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700">Pathology & Histology</span>
        </div>
      </div>

      {/* INCOMING RECORD SHARING REQUESTS */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Share2 className="w-4 h-4 text-emerald-700" />
            <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
              Incoming Inter-Provider Record Sharing Requests
            </h2>
          </div>
          <span className="text-xs text-slate-500">
            Simulated Health Information Exchange (HIE)
          </span>
        </div>

        <div className="space-y-3">
          {incomingRequests.map((req) => (
            <div
              key={req.id}
              className="p-4 rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-white transition-colors flex flex-col md:flex-row md:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-bold text-slate-900 text-xs">
                    {req.from_clinic}
                  </span>
                  <span className="text-[11px] px-2 py-0.2 rounded bg-slate-200 text-slate-700 font-mono">
                    Patient: {req.patient_name} ({req.patient_id})
                  </span>
                  {req.status === 'approved' && (
                    <span className="text-[11px] px-2 py-0.2 rounded bg-emerald-100 text-emerald-800 font-semibold">
                      Dispatched
                    </span>
                  )}
                  {req.status === 'rejected' && (
                    <span className="text-[11px] px-2 py-0.2 rounded bg-rose-100 text-rose-800 font-semibold">
                      Declined
                    </span>
                  )}
                  {req.status === 'pending' && (
                    <span className="text-[11px] px-2 py-0.2 rounded bg-amber-100 text-amber-800 font-semibold animate-pulse-subtle">
                      Pending Action
                    </span>
                  )}
                </div>
                <div className="text-xs text-slate-700 font-medium">
                  Record: {req.requested_records}
                </div>
                <div className="text-xs text-slate-500 italic">
                  Clinical Rationale: "{req.purpose}"
                </div>
                <div className="text-[10px] text-slate-400">
                  Request Ref: {req.id} • {req.timestamp}
                </div>
              </div>

              {req.status === 'pending' ? (
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    onClick={() => handleShareDecision(req.id, 'rejected')}
                    className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-rose-300 text-rose-700 hover:bg-rose-50 transition-colors flex items-center gap-1"
                  >
                    <XCircle className="w-3.5 h-3.5" />
                    Decline
                  </button>
                  <button
                    onClick={() => handleShareDecision(req.id, 'approved')}
                    className="px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-emerald-700 hover:bg-emerald-800 text-white transition-colors flex items-center gap-1.5 shadow-xs"
                  >
                    <CheckCircle className="w-3.5 h-3.5" />
                    Approve & Dispatch
                  </button>
                </div>
              ) : (
                <div className="text-xs font-semibold text-slate-500 shrink-0">
                  Status: <span className="capitalize">{req.status}</span>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* PROVIDER AUDIT TRAIL LOG */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <History className="w-4 h-4 text-emerald-700" />
            <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
              Diagnostic Network Operational Audit Trail
            </h2>
          </div>
          <button
            onClick={fetchAudit}
            className="p-1.5 rounded-lg border border-slate-200 text-slate-500 hover:text-emerald-700 hover:bg-slate-50"
            title="Refresh logs"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loadingAudit ? 'animate-spin' : ''}`} />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200 text-slate-400 font-semibold uppercase text-[10px]">
                <th className="py-2.5 px-3">Timestamp</th>
                <th className="py-2.5 px-3">Action</th>
                <th className="py-2.5 px-3">Actor Role</th>
                <th className="py-2.5 px-3">Patient Ref</th>
                <th className="py-2.5 px-3">Audit Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {auditLogs.slice(0, 8).map((log) => (
                <tr key={log.id} className="hover:bg-slate-50/50">
                  <td className="py-2.5 px-3 font-mono text-slate-500 whitespace-nowrap">
                    {log.timestamp}
                  </td>
                  <td className="py-2.5 px-3 font-semibold text-slate-800">
                    <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-mono text-[11px]">
                      {log.action}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 capitalize text-slate-600">
                    {log.actor_role} ({log.actor_name})
                  </td>
                  <td className="py-2.5 px-3 font-mono font-bold text-emerald-800">
                    {log.patient_id}
                  </td>
                  <td className="py-2.5 px-3 text-slate-600">
                    {log.details}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Record Modal */}
      <AddRecordModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        patientId={selectedPatientId}
        persona={persona}
        onRecordCreated={handleRecordCreated}
      />
    </div>
  );
}
