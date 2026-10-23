import React, { useState, useEffect } from 'react';
import { ShieldCheck, History, Search, RefreshCw, Filter, Lock } from 'lucide-react';
import { api } from '../api';

export default function AuditView({ persona, onShowToast }) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filterAction, setFilterAction] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const data = await api.getAuditLogs();
      setLogs(data.audit_logs || []);
    } catch (err) {
      console.error('Failed to fetch audit logs', err);
      onShowToast({
        type: 'error',
        title: 'Audit Log Error',
        message: 'Could not retrieve audit trail.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const filteredLogs = logs.filter((log) => {
    if (filterAction !== 'all' && log.action !== filterAction) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        log.actor_name.toLowerCase().includes(q) ||
        log.details.toLowerCase().includes(q) ||
        log.patient_id.toLowerCase().includes(q) ||
        log.action.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const getActionBadge = (action) => {
    if (action.includes('GRANTED') || action.includes('APPROVED')) {
      return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    }
    if (action.includes('DENIED') || action.includes('REVOKED') || action.includes('UNAUTHORIZED')) {
      return 'bg-rose-100 text-rose-800 border-rose-200';
    }
    if (action.includes('REQUESTED')) {
      return 'bg-amber-100 text-amber-800 border-amber-200';
    }
    return 'bg-slate-100 text-slate-800 border-slate-200';
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-[#0B3724] text-white flex items-center justify-center font-bold text-xl">
              <ShieldCheck className="w-6 h-6 text-emerald-400" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-bold text-slate-900">
                  Access History & Operational Audit Trail
                </h1>
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-semibold border border-emerald-200">
                  Simulated HIPAA Audit Log
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Every consent grant, denial, search, and clinical record access is immutably logged for privacy accountability.
              </p>
            </div>
          </div>

          <button
            onClick={fetchLogs}
            className="px-4 py-2 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-50 flex items-center gap-2 transition-colors self-start md:self-auto"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh Audit Stream
          </button>
        </div>

        {/* Privacy Safeguards Callout */}
        <div className="mt-4 p-3 rounded-xl bg-emerald-50/70 border border-emerald-200 text-xs text-emerald-900 flex items-start gap-2.5">
          <Lock className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
          <div>
            <span className="font-bold">Privacy Safeguard Notice:</span> Audit records contain metadata, actors, timestamps, and patient references only. Raw medical descriptions, diagnoses, and sensitive clinical notes are excluded to prevent privacy leakage.
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        <div className="relative grow max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by actor, patient ID, or action..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500"
          />
        </div>

        <div className="flex items-center gap-2">
          <select
            value={filterAction}
            onChange={(e) => setFilterAction(e.target.value)}
            className="px-3 py-2 border border-slate-200 rounded-xl text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-emerald-500"
          >
            <option value="all">All Action Types</option>
            <option value="CONSENT_GRANTED">Consent Granted</option>
            <option value="CONSENT_DENIED">Consent Denied</option>
            <option value="CONSENT_REVOKED">Consent Revoked</option>
            <option value="ACCESS_REQUESTED">Access Requested</option>
            <option value="RECORD_VIEWED">Record Viewed</option>
            <option value="RECORD_CREATED">Record Created</option>
            <option value="PATIENT_SEARCH">Patient Search</option>
            <option value="UNAUTHORIZED_ACCESS_ATTEMPT">Unauthorized Attempt</option>
          </select>
        </div>
      </div>

      {/* Audit Log Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-[10px]">
                <th className="py-3 px-4">Timestamp (UTC)</th>
                <th className="py-3 px-4">Action Event</th>
                <th className="py-3 px-4">Actor</th>
                <th className="py-3 px-4">Role</th>
                <th className="py-3 px-4">Patient Ref</th>
                <th className="py-3 px-4">Operational Audit Summary</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400">
                    No audit records match the current filters.
                  </td>
                </tr>
              ) : (
                filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 font-mono text-slate-500 whitespace-nowrap">
                      {log.timestamp}
                    </td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex px-2 py-0.5 rounded text-[11px] font-mono font-bold border ${getActionBadge(log.action)}`}>
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-800">
                      {log.actor_name}
                      <span className="block text-[10px] font-mono text-slate-400 font-normal">
                        {log.actor_id}
                      </span>
                    </td>
                    <td className="py-3 px-4 capitalize text-slate-600">
                      {log.actor_role}
                    </td>
                    <td className="py-3 px-4 font-mono font-bold text-emerald-800">
                      {log.patient_id}
                    </td>
                    <td className="py-3 px-4 text-slate-700 leading-relaxed max-w-md">
                      {log.details}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
