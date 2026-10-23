import React, { useState, useEffect } from 'react';
import { 
  Search, 
  User, 
  Lock, 
  Unlock, 
  ShieldAlert, 
  ShieldCheck, 
  AlertCircle, 
  Send, 
  Plus, 
  Calendar, 
  Phone, 
  Heart, 
  RefreshCw, 
  ArrowRight,
  Filter,
  FileText
} from 'lucide-react';
import TimelineItem from './TimelineItem';
import RequestAccessModal from './RequestAccessModal';
import AddRecordModal from './AddRecordModal';
import { api } from '../api';

const DEMO_PATIENTS = [
  { id: 'PAT-101', name: 'Sarah Jenkins', note: 'Pending Request' },
  { id: 'PAT-102', name: 'Robert Chen', note: 'Type 2 Diabetes' },
  { id: 'PAT-103', name: 'Elena Rodriguez', note: 'No Requests Yet' },
];

export default function DoctorView({ persona, onShowToast, onSwitchPersona }) {
  const [selectedPatientId, setSelectedPatientId] = useState('PAT-101');
  const [searchQuery, setSearchQuery] = useState('PAT-101');
  const [patient, setPatient] = useState(null);
  const [accessState, setAccessState] = useState(null); // { authorized: boolean, message: '', consent_required: boolean }
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filterType, setFilterType] = useState('all');
  const [filterProvider, setFilterProvider] = useState('all');
  const [isRequestModalOpen, setIsRequestModalOpen] = useState(false);
  const [isAddRecordModalOpen, setIsAddRecordModalOpen] = useState(false);

  const fetchPatientAndRecords = async (patientId) => {
    setLoading(true);
    try {
      // 1. Fetch patient demographic summary
      const pData = await api.getPatient(patientId, persona);
      setPatient(pData);

      // 2. Fetch records (backend checks consent permissions!)
      const recData = await api.getPatientRecords(
        patientId,
        {
          type: filterType,
          provider: filterProvider,
        },
        persona
      );

      if (recData.ok) {
        setAccessState({
          authorized: true,
          message: 'Access authorized by patient consent',
          grant: recData.access_grant,
        });
        setRecords(recData.records || []);
      } else if (recData.status === 403) {
        setAccessState({
          authorized: false,
          consent_required: true,
          message: recData.error || 'Patient consent required to view clinical timeline.',
          has_pending_request: recData.has_pending_request,
          pending_request_id: recData.pending_request_id,
        });
        setRecords([]);
      } else {
        setAccessState({
          authorized: false,
          message: recData.error || 'Failed to retrieve patient records',
        });
        setRecords([]);
      }
    } catch (err) {
      console.error('Doctor view fetch error', err);
      onShowToast({
        type: 'error',
        title: 'Error',
        message: 'Unable to communicate with MedIt backend.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPatientAndRecords(selectedPatientId);
  }, [selectedPatientId, filterType, filterProvider]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      setSelectedPatientId(searchQuery.trim().toUpperCase());
    }
  };

  const handleSelectPatient = (id) => {
    setSearchQuery(id);
    setSelectedPatientId(id);
  };

  const handleAccessRequestSubmitted = async (payload) => {
    const res = await api.createAccessRequest(payload, persona);
    if (res.ok) {
      onShowToast({
        type: 'success',
        title: 'Consent Request Sent',
        message: `Simulated consent request ${res.request_id} delivered to patient ${selectedPatientId}.`,
      });
      fetchPatientAndRecords(selectedPatientId);
    } else {
      throw new Error(res.error || 'Failed to submit request');
    }
  };

  const handleRecordCreated = async (payload) => {
    const res = await api.createRecord(payload, persona);
    if (res.ok) {
      onShowToast({
        type: 'success',
        title: 'Clinical Record Authored',
        message: 'Successfully recorded into patient unified timeline.',
      });
      fetchPatientAndRecords(selectedPatientId);
    } else {
      throw new Error(res.error || 'Failed to author record');
    }
  };

  return (
    <div className="space-y-6">
      {/* Doctor Header & Patient Search Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              <h1 className="text-lg font-bold text-slate-900">
                Doctor Clinical Workstation
              </h1>
              <span className="text-xs px-2 py-0.5 rounded-full bg-teal-50 text-teal-800 font-medium border border-teal-200">
                Logged in as {persona.name} ({persona.id})
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Search by Patient ID to access the unified multi-provider medical history. Protected by backend consent checks.
            </p>
          </div>

          {/* Patient Search Form */}
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 max-w-md w-full">
            <div className="relative grow">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Enter Patient ID (e.g. PAT-101)..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-xl text-xs uppercase font-medium focus:ring-2 focus:ring-emerald-500"
              />
            </div>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-[#0B3724] hover:bg-[#124A32] text-white transition-colors"
            >
              Lookup
            </button>
          </form>
        </div>

        {/* Quick Demo Patient Selectors */}
        <div className="mt-4 pt-3 border-t border-slate-100 flex flex-wrap items-center gap-2">
          <span className="text-[11px] font-semibold text-slate-500">
            Demo Patients:
          </span>
          {DEMO_PATIENTS.map((p) => {
            const isSelected = selectedPatientId === p.id;
            return (
              <button
                key={p.id}
                onClick={() => handleSelectPatient(p.id)}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 ${
                  isSelected
                    ? 'bg-emerald-800 text-white shadow-xs font-semibold'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                <span>{p.name} ({p.id})</span>
                <span className={`text-[10px] px-1.5 py-0.2 rounded ${isSelected ? 'bg-emerald-950 text-emerald-200' : 'bg-slate-200 text-slate-600'}`}>
                  {p.note}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Patient Demographic Profile Summary */}
      {patient && (
        <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-teal-800 text-white flex items-center justify-center font-bold text-lg">
                {patient.name.split(' ').map((n) => n[0]).join('')}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-bold text-slate-900">{patient.name}</h2>
                  <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {patient.id}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded bg-rose-50 text-rose-700 font-semibold border border-rose-200">
                    {patient.blood_type}
                  </span>
                </div>
                <div className="text-xs text-slate-500 mt-1">
                  DOB: {patient.dob} ({patient.gender}) • Emergency Contact: {patient.emergency_contact || 'None'}
                </div>
              </div>
            </div>

            {/* Authorization Status Badge */}
            <div>
              {accessState?.authorized ? (
                <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-800 border border-emerald-300 text-xs font-bold shadow-xs">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  Consent Active: Access Granted
                </div>
              ) : (
                <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-50 text-amber-900 border border-amber-300 text-xs font-bold shadow-xs">
                  <Lock className="w-4 h-4 text-amber-600" />
                  Protected Record: Consent Required
                </div>
              )}
            </div>
          </div>

          {/* Allergies and conditions quick pill bar */}
          <div className="mt-4 pt-3 border-t border-slate-100 flex flex-wrap items-center gap-4 text-xs">
            <div className="flex items-center gap-2">
              <span className="font-bold text-slate-500 uppercase text-[10px]">Allergies:</span>
              {patient.allergies?.length > 0 ? (
                patient.allergies.map((a, i) => (
                  <span key={i} className="px-2 py-0.5 rounded bg-rose-100 text-rose-800 font-semibold text-[11px]">
                    ⚠️ {a}
                  </span>
                ))
              ) : (
                <span className="text-slate-400">None</span>
              )}
            </div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-slate-500 uppercase text-[10px]">Conditions:</span>
              {patient.chronic_conditions?.length > 0 ? (
                patient.chronic_conditions.map((c, i) => (
                  <span key={i} className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-medium text-[11px]">
                    {c}
                  </span>
                ))
              ) : (
                <span className="text-slate-400">None</span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ACCESS CONTROL WALL (If Consent Not Granted) */}
      {!accessState?.authorized && (
        <div className="bg-white rounded-2xl border-2 border-dashed border-amber-300 p-8 text-center max-w-2xl mx-auto shadow-sm">
          <div className="w-16 h-16 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center mx-auto mb-4 shadow-xs">
            <Lock className="w-8 h-8 text-amber-600" />
          </div>
          <h3 className="text-lg font-bold text-slate-900 mb-2">
            Clinical Health Timeline is Shielded
          </h3>
          <p className="text-sm text-slate-600 max-w-md mx-auto leading-relaxed mb-6">
            Under the MedIt zero-trust simulated HIPAA consent protocol, healthcare providers cannot inspect a patient's historical diagnoses, prescriptions, and lab tests without verified patient consent.
          </p>

          {accessState?.has_pending_request ? (
            <div className="p-4 bg-amber-50 rounded-xl border border-amber-200 max-w-md mx-auto text-left space-y-3 mb-4">
              <div className="flex items-center gap-2 text-xs font-bold text-amber-950">
                <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse"></span>
                Access Request Pending Patient Decision
              </div>
              <div className="text-xs text-amber-900 leading-relaxed">
                Your request <span className="font-mono font-bold">{accessState.pending_request_id || 'REQ-101'}</span> was delivered to patient {patient?.name}.
              </div>
              {/* Shortcut to switch to Patient to simulate consent grant */}
              <button
                onClick={() => onSwitchPersona('patient', selectedPatientId)}
                className="w-full py-2 px-3 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white font-semibold text-xs flex items-center justify-center gap-2 shadow-xs transition-colors"
              >
                Switch to Patient Persona ({patient?.name}) to Approve
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              <button
                onClick={() => setIsRequestModalOpen(true)}
                className="px-6 py-2.5 rounded-xl bg-[#0B3724] hover:bg-[#124A32] text-white text-xs font-semibold shadow-md transition-all inline-flex items-center gap-2"
              >
                <Send className="w-4 h-4" />
                Request Record Access from Patient
              </button>
              <div className="text-[11px] text-slate-400">
                A consent prompt will be dispatched directly to the patient's dashboard.
              </div>
            </div>
          )}
        </div>
      )}

      {/* UNLOCKED CLINICAL TIMELINE (When Consent is Active) */}
      {accessState?.authorized && (
        <div className="space-y-4 animate-in fade-in duration-300">
          {/* Controls Bar: Filters, Categories & Add Record */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="px-3 py-2 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-emerald-500"
              >
                <option value="all">All Clinical Categories</option>
                <option value="visit">Visits Only</option>
                <option value="diagnosis">Diagnoses Only</option>
                <option value="prescription">Prescriptions Only</option>
                <option value="lab_report">Lab Panels & Reports</option>
                <option value="allergy">Allergies</option>
                <option value="vital_signs">Vitals Log</option>
              </select>

              <select
                value={filterProvider}
                onChange={(e) => setFilterProvider(e.target.value)}
                className="px-3 py-2 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-emerald-500"
              >
                <option value="all">All Contributing Providers</option>
                <option value="St. Jude">St. Jude Medical Center</option>
                <option value="Apex Diagnostics">Apex Diagnostics Lab</option>
                <option value="Metro Health">Metro Health Clinic</option>
              </select>

              <button
                onClick={() => fetchPatientAndRecords(selectedPatientId)}
                className="p-2 border border-slate-200 rounded-xl text-slate-500 hover:text-emerald-700 hover:bg-slate-50 transition-colors"
                title="Refresh timeline"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </button>
            </div>

            <button
              onClick={() => setIsAddRecordModalOpen(true)}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-[#0B3724] hover:bg-[#124A32] text-white shadow-xs transition-colors flex items-center justify-center gap-1.5"
            >
              <Plus className="w-4 h-4" />
              Author Clinical Record
            </button>
          </div>

          {/* Timeline Title & Count */}
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-2">
              <h3 className="text-base font-bold text-slate-900">
                Unified Clinical Timeline for {patient?.name}
              </h3>
              <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-semibold border border-emerald-200">
                {records.length} records retrieved
              </span>
            </div>
            <span className="text-xs text-slate-500 font-medium">
              Verified Cross-Institutional History
            </span>
          </div>

          {/* Records List */}
          {records.length === 0 ? (
            <div className="p-12 text-center bg-white rounded-2xl border border-slate-200">
              <FileText className="w-10 h-10 text-slate-300 mx-auto mb-3" />
              <div className="text-sm font-bold text-slate-800">No records found matching filters</div>
              <div className="text-xs text-slate-500 mt-1">Author a new clinical record using the button above.</div>
            </div>
          ) : (
            <div className="mt-4">
              {records.map((record, index) => (
                <TimelineItem
                  key={record.id}
                  record={record}
                  isLast={index === records.length - 1}
                  currentUser={persona}
                  onDeleteRecord={async (recordId) => {
                    if (!window.confirm('Are you sure you want to delete this clinical record?')) return;
                    try {
                      const res = await api.deleteRecord(recordId, persona);
                      if (res.ok) {
                        onShowToast({
                          type: 'success',
                          title: 'Record Removed',
                          message: 'Clinical record deleted successfully.',
                        });
                        fetchPatientAndRecords(selectedPatientId);
                      } else {
                        onShowToast({
                          type: 'error',
                          title: 'Delete Failed',
                          message: res.error || 'Permission denied or unable to delete.',
                        });
                      }
                    } catch (err) {
                      onShowToast({
                        type: 'error',
                        title: 'Error',
                        message: 'Network error deleting record.',
                      });
                    }
                  }}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* Request Access Modal */}
      <RequestAccessModal
        isOpen={isRequestModalOpen}
        onClose={() => setIsRequestModalOpen(false)}
        patientId={selectedPatientId}
        patientName={patient?.name || selectedPatientId}
        persona={persona}
        onRequestSubmitted={handleAccessRequestSubmitted}
      />

      {/* Add Record Modal */}
      <AddRecordModal
        isOpen={isAddRecordModalOpen}
        onClose={() => setIsAddRecordModalOpen(false)}
        patientId={selectedPatientId}
        persona={persona}
        onRecordCreated={handleRecordCreated}
      />
    </div>
  );
}
