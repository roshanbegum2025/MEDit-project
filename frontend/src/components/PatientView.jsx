import React, { useState, useEffect } from 'react';
import { 
  User, 
  Heart, 
  AlertCircle, 
  Phone, 
  Mail, 
  Calendar, 
  ShieldCheck, 
  ShieldX, 
  Plus, 
  Search, 
  Filter, 
  Lock, 
  Unlock, 
  Check, 
  X, 
  Clock, 
  Activity, 
  FileText, 
  RefreshCw 
} from 'lucide-react';
import TimelineItem from './TimelineItem';
import AddRecordModal from './AddRecordModal';
import { api } from '../api';

export default function PatientView({ persona, onShowToast, onSwitchPersona }) {
  const [patient, setPatient] = useState(null);
  const [records, setRecords] = useState([]);
  const [requests, setRequests] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState('all');
  const [filterProvider, setFilterProvider] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [actionInProgress, setActionInProgress] = useState(null);

  const patientId = persona.id || 'PAT-101';

  const loadData = async () => {
    setLoading(true);
    try {
      // 1. Fetch patient profile
      const patientData = await api.getPatient(patientId, persona);
      setPatient(patientData);

      // 2. Fetch records
      const recordsData = await api.getPatientRecords(
        patientId,
        {
          type: filterType,
          provider: filterProvider,
          search: searchQuery,
        },
        persona
      );
      if (recordsData.ok) {
        setRecords(recordsData.records || []);
      }

      // 3. Fetch access requests for this patient
      const reqData = await api.getAccessRequests({ patient_id: patientId });
      setRequests(reqData.requests || []);

      // 4. Fetch active permissions
      const permData = await api.getPermissions(patientId);
      setPermissions(permData.permissions || []);
    } catch (err) {
      console.error('Failed to load patient data', err);
      onShowToast({
        type: 'error',
        title: 'Error',
        message: 'Failed to load health records. Ensure backend server is running.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [patientId, filterType, filterProvider, searchQuery]);

  const handleConsentDecision = async (requestId, decision) => {
    setActionInProgress(requestId);
    try {
      const res = await api.decideAccessRequest(
        requestId,
        {
          decision,
          decision_notes: decision === 'approved' ? 'Granted by patient via portal' : 'Declined by patient',
        },
        persona
      );

      if (res.ok) {
        onShowToast({
          type: decision === 'approved' ? 'success' : 'warning',
          title: decision === 'approved' ? 'Access Consent Granted' : 'Access Request Declined',
          message: decision === 'approved'
            ? 'Doctor can now review your chronological health timeline.'
            : 'Access request has been rejected.',
        });
        loadData();
      } else {
        throw new Error(res.error || 'Failed decision');
      }
    } catch (err) {
      onShowToast({
        type: 'error',
        title: 'Consent Update Error',
        message: err.message,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleRevokePermission = async (permissionId) => {
    setActionInProgress(permissionId);
    try {
      const res = await api.revokePermission(permissionId, persona);
      if (res.ok) {
        onShowToast({
          type: 'warning',
          title: 'Access Revoked',
          message: 'The medical provider or doctor can no longer view your protected timeline.',
        });
        loadData();
      } else {
        throw new Error(res.error || 'Failed revoke');
      }
    } catch (err) {
      onShowToast({
        type: 'error',
        title: 'Revoke Error',
        message: err.message,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleRecordCreated = (newRecord) => {
    onShowToast({
      type: 'success',
      title: 'Health Record Added',
      message: 'New record has been unified into your chronological health timeline.',
    });
    loadData();
  };

  const handleDeleteRecord = async (recordId) => {
    if (!window.confirm('Are you sure you want to delete this clinical record from your timeline?')) return;
    try {
      const res = await api.deleteRecord(recordId, persona);
      if (res.ok) {
        onShowToast({
          type: 'success',
          title: 'Record Deleted',
          message: 'The medical record was removed from your timeline.',
        });
        loadData();
      } else {
        onShowToast({
          type: 'error',
          title: 'Delete Failed',
          message: res.error || 'Could not delete record.',
        });
      }
    } catch (err) {
      onShowToast({
        type: 'error',
        title: 'Error',
        message: 'Network error while deleting record.',
      });
    }
  };

  const pendingRequests = requests.filter((r) => r.status === 'pending');
  const activePermissions = permissions.filter((p) => p.status === 'active');

  return (
    <div className="space-y-6">
      {/* Patient Profile Snapshot Card */}
      {patient && (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs p-5 md:p-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-start gap-4">
              <div className="w-14 h-14 rounded-2xl bg-[#0B3724] text-white flex items-center justify-center font-bold text-xl shadow-md shrink-0">
                {patient.name.split(' ').map((n) => n[0]).join('')}
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-xl md:text-2xl font-bold text-slate-900">{patient.name}</h1>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                    {patient.id}
                  </span>
                  <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200 flex items-center gap-1">
                    <Heart className="w-3 h-3 text-rose-500 fill-rose-500" />
                    Blood Type: {patient.blood_type}
                  </span>
                </div>

                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 mt-2">
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    DOB: {patient.dob} ({patient.gender})
                  </span>
                  {patient.phone && (
                    <span className="flex items-center gap-1">
                      <Phone className="w-3.5 h-3.5 text-slate-400" />
                      {patient.phone}
                    </span>
                  )}
                  {patient.email && (
                    <span className="flex items-center gap-1">
                      <Mail className="w-3.5 h-3.5 text-slate-400" />
                      {patient.email}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Quick action button */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsAddModalOpen(true)}
                className="w-full md:w-auto px-4 py-2.5 rounded-xl text-xs font-semibold bg-[#0B3724] hover:bg-[#124A32] text-white shadow-sm transition-all flex items-center justify-center gap-2"
              >
                <Plus className="w-4 h-4" />
                Add Health Record
              </button>
            </div>
          </div>

          {/* Clinical Alert Tags: Allergies & Chronic Conditions */}
          <div className="mt-5 pt-4 border-t border-slate-100 grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5 text-rose-500" />
                Documented Drug & Food Allergies
              </div>
              <div className="flex flex-wrap gap-1.5">
                {patient.allergies?.length > 0 ? (
                  patient.allergies.map((all, idx) => (
                    <span
                      key={idx}
                      className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200"
                    >
                      ⚠️ {all}
                    </span>
                  ))
                ) : (
                  <span className="text-xs text-slate-400">No known allergies</span>
                )}
              </div>
            </div>

            <div>
              <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1">
                <Activity className="w-3.5 h-3.5 text-emerald-600" />
                Diagnosed Chronic Conditions
              </div>
              <div className="flex flex-wrap gap-1.5">
                {patient.chronic_conditions?.length > 0 ? (
                  patient.chronic_conditions.map((cond, idx) => (
                    <span
                      key={idx}
                      className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-medium bg-emerald-50 text-emerald-800 border border-emerald-200"
                    >
                      {cond}
                    </span>
                  ))
                ) : (
                  <span className="text-xs text-slate-400">None documented</span>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* PENDING DOCTOR ACCESS REQUESTS (Consent Management Banner) */}
      {pendingRequests.length > 0 && (
        <div className="bg-gradient-to-r from-amber-500/10 via-amber-50 to-emerald-50/50 rounded-2xl border-2 border-amber-300 p-5 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-amber-500 text-white flex items-center justify-center font-bold">
                <Lock className="w-4 h-4" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-amber-950">
                  Doctor Access Requests Pending Your Consent ({pendingRequests.length})
                </h2>
                <p className="text-xs text-amber-800">
                  You have full autonomy to authorize or deny clinical access to your unified records.
                </p>
              </div>
            </div>
            <span className="animate-pulse flex h-3 w-3 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-amber-500"></span>
            </span>
          </div>

          <div className="space-y-3 mt-3">
            {pendingRequests.map((req) => (
              <div
                key={req.id}
                className="bg-white rounded-xl p-4 border border-amber-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900 text-sm">{req.requester_name}</span>
                    <span className="text-[11px] px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                      {req.requester_organization || req.requester_role}
                    </span>
                    <span className="text-[11px] px-2 py-0.5 rounded bg-amber-100 text-amber-800 font-medium">
                      Scope: {req.scope.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <p className="text-xs text-slate-600 leading-relaxed italic">
                    "{req.reason}"
                  </p>
                  <div className="text-[10px] text-slate-400">
                    Requested on {req.created_at} • Request ID: {req.id}
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <button
                    disabled={actionInProgress === req.id}
                    onClick={() => handleConsentDecision(req.id, 'rejected')}
                    className="px-3.5 py-1.5 rounded-lg text-xs font-semibold border border-rose-300 text-rose-700 hover:bg-rose-50 transition-colors flex items-center gap-1 disabled:opacity-50"
                  >
                    <X className="w-3.5 h-3.5" />
                    Reject
                  </button>
                  <button
                    disabled={actionInProgress === req.id}
                    onClick={() => handleConsentDecision(req.id, 'approved')}
                    className="px-4 py-1.5 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs transition-colors flex items-center gap-1.5 disabled:opacity-50"
                  >
                    <Check className="w-3.5 h-3.5" />
                    Approve Access
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Active Consents Card (Ability to Revoke) */}
      {activePermissions.length > 0 && (
        <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                Active Doctor & Provider Consents ({activePermissions.length})
              </h3>
            </div>
            <span className="text-[11px] text-slate-400">
              Revocable at any time
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {activePermissions.map((perm) => (
              <div
                key={perm.id}
                className="p-3 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-between gap-3 text-xs"
              >
                <div>
                  <div className="font-semibold text-slate-800 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                    {perm.grantee_name}
                  </div>
                  <div className="text-[11px] text-slate-500 mt-0.5">
                    Granted: {perm.granted_at.split(' ')[0]} • Scope: {perm.scope.replace(/_/g, ' ')}
                  </div>
                </div>
                <button
                  disabled={actionInProgress === perm.id}
                  onClick={() => handleRevokePermission(perm.id)}
                  className="px-2.5 py-1 rounded text-[11px] font-semibold text-rose-700 hover:bg-rose-100/70 border border-rose-200 transition-colors shrink-0"
                >
                  Revoke
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Unified Timeline Section */}
      <div className="space-y-4">
        {/* Filter & Search Bar */}
        <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          {/* Search */}
          <div className="relative grow max-w-md">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search records, medications, diagnoses, or lab tests..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            />
          </div>

          {/* Filters */}
          <div className="flex flex-wrap items-center gap-2">
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-emerald-500"
            >
              <option value="all">All Record Types</option>
              <option value="visit">Clinical Visits</option>
              <option value="diagnosis">Diagnoses</option>
              <option value="prescription">Prescriptions</option>
              <option value="lab_report">Lab Reports</option>
              <option value="allergy">Allergies</option>
              <option value="vital_signs">Vital Signs</option>
            </select>

            <select
              value={filterProvider}
              onChange={(e) => setFilterProvider(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-emerald-500"
            >
              <option value="all">All Providers</option>
              <option value="St. Jude">St. Jude Medical Center</option>
              <option value="Apex Diagnostics">Apex Diagnostics Lab</option>
              <option value="Metro Health">Metro Health Clinic</option>
              <option value="Patient Self-Report">Patient Self-Report</option>
            </select>

            <button
              onClick={loadData}
              className="p-2 border border-slate-200 rounded-xl text-slate-500 hover:text-emerald-700 hover:bg-slate-50 transition-colors"
              title="Refresh timeline"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Timeline Header */}
        <div className="flex items-center justify-between px-1">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-bold text-slate-900">Unified Chronological Timeline</h2>
            <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 font-semibold border border-slate-200">
              {records.length} records
            </span>
          </div>
          <span className="text-xs text-slate-500">Sorted Newest First</span>
        </div>

        {/* Timeline Items List */}
        {loading ? (
          <div className="p-12 text-center bg-white rounded-2xl border border-slate-200">
            <RefreshCw className="w-8 h-8 text-emerald-600 animate-spin mx-auto mb-3" />
            <div className="text-sm font-semibold text-slate-800">Compiling Unified Health Timeline...</div>
            <div className="text-xs text-slate-400 mt-1">Aggregating records from connected hospital networks and laboratories</div>
          </div>
        ) : records.length === 0 ? (
          <div className="p-12 text-center bg-white rounded-2xl border border-slate-200">
            <FileText className="w-10 h-10 text-slate-300 mx-auto mb-3" />
            <div className="text-sm font-bold text-slate-800">No medical records match your criteria</div>
            <div className="text-xs text-slate-500 mt-1">Try clearing your filters or add a new health record above.</div>
          </div>
        ) : (
          <div className="mt-4">
            {records.map((record, index) => (
              <TimelineItem
                key={record.id}
                record={record}
                isLast={index === records.length - 1}
                onDeleteRecord={handleDeleteRecord}
                currentUser={persona}
              />
            ))}
          </div>
        )}
      </div>

      {/* Add Record Modal */}
      <AddRecordModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        patientId={patientId}
        persona={persona}
        onRecordCreated={handleRecordCreated}
      />
    </div>
  );
}
