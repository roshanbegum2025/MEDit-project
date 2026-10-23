import React, { useState } from 'react';
import { 
  Stethoscope, 
  Activity, 
  Pill, 
  FlaskConical, 
  AlertTriangle, 
  HeartPulse, 
  CheckCircle, 
  Building2, 
  Calendar, 
  User, 
  ChevronDown, 
  ChevronUp, 
  FileText,
  Trash2,
  Paperclip,
  Tag
} from 'lucide-react';

export const RECORD_TYPE_CONFIG = {
  visit: {
    label: 'OPD Consultation',
    icon: Stethoscope,
    badgeBg: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    iconColor: 'text-[#0B3724]',
    borderColor: 'border-emerald-200',
    dotColor: 'bg-[#0B3724]',
  },
  diagnosis: {
    label: 'Clinical Diagnosis',
    icon: Activity,
    badgeBg: 'bg-amber-50 text-amber-800 border-amber-200',
    iconColor: 'text-amber-700',
    borderColor: 'border-amber-200',
    dotColor: 'bg-amber-600',
  },
  prescription: {
    label: 'Prescription',
    icon: Pill,
    badgeBg: 'bg-teal-50 text-teal-800 border-teal-200',
    iconColor: 'text-teal-700',
    borderColor: 'border-teal-200',
    dotColor: 'bg-teal-600',
  },
  lab_report: {
    label: 'Lab Report',
    icon: FlaskConical,
    badgeBg: 'bg-blue-50 text-blue-800 border-blue-200',
    iconColor: 'text-blue-700',
    borderColor: 'border-blue-200',
    dotColor: 'bg-blue-600',
  },
  allergy: {
    label: 'Allergy Record',
    icon: AlertTriangle,
    badgeBg: 'bg-rose-50 text-rose-800 border-rose-200',
    iconColor: 'text-rose-700',
    borderColor: 'border-rose-200',
    dotColor: 'bg-rose-600',
  },
  vital_signs: {
    label: 'Vital Signs',
    icon: HeartPulse,
    badgeBg: 'bg-emerald-50 text-emerald-900 border-emerald-200',
    iconColor: 'text-emerald-700',
    borderColor: 'border-emerald-200',
    dotColor: 'bg-emerald-700',
  },
};

export default function TimelineItem({ record, isLast, onDeleteRecord, currentUser }) {
  const [expanded, setExpanded] = useState(false);

  const config = RECORD_TYPE_CONFIG[record.record_type] || RECORD_TYPE_CONFIG.visit;
  const Icon = config.icon;

  // Indian Date Format: DD MMM YYYY (e.g. 18 Mar 2026)
  const formatIndianDate = (dateStr) => {
    if (!dateStr) return '—';
    try {
      const clean = dateStr.includes('T') ? dateStr.split('T')[0] : dateStr.split(' ')[0];
      const parts = clean.split('-');
      if (parts.length === 3) {
        const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        const day = parts[2];
        const month = d.toLocaleString('en-IN', { month: 'short' });
        const year = parts[0];
        return `${day} ${month} ${year}`;
      }
      return dateStr;
    } catch {
      return dateStr;
    }
  };

  const metrics = record.metrics || {};
  const metricEntries = Object.entries(metrics);
  const medications = record.medications || [];

  return (
    <div className="relative flex gap-4 md:gap-5 group">
      {/* Timeline line and node icon */}
      <div className="flex flex-col items-center">
        <div className={`w-9 h-9 rounded-full border-2 border-white shadow-sm flex items-center justify-center shrink-0 ${config.badgeBg}`}>
          <Icon className={`w-4 h-4 ${config.iconColor}`} />
        </div>
        {!isLast && (
          <div className="w-0.5 grow bg-slate-200 my-1.5 group-hover:bg-emerald-300 transition-colors"></div>
        )}
      </div>

      {/* Record Content Card */}
      <div className="grow pb-6">
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs hover:shadow-md transition-all p-4 md:p-5">
          {/* Header Row: Type pill, Provider badge, Verification tag, Indian Date */}
          <div className="flex flex-wrap items-center justify-between gap-2 mb-2.5">
            <div className="flex flex-wrap items-center gap-2">
              <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${config.badgeBg}`}>
                <Icon className="w-3 h-3" />
                {config.label}
              </span>

              {/* Source Provider Badge */}
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                <Building2 className="w-3 h-3 text-slate-500" />
                {record.provider_name}
              </span>

              {/* Verification Status */}
              {record.verification_status === 'verified' ? (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                  <CheckCircle className="w-3 h-3 text-emerald-700" />
                  Verified Clinical Record
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                  <User className="w-3 h-3 text-amber-700" />
                  Patient Self-Reported
                </span>
              )}
            </div>

            <div className="flex items-center gap-1.5 text-xs text-slate-500 font-semibold">
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              {formatIndianDate(record.date)}
            </div>
          </div>

          {/* Title & Category */}
          <div className="flex items-start justify-between gap-4 mb-2">
            <div>
              <h4 className="text-base font-bold text-slate-900 group-hover:text-[#0B3724] transition-colors">
                {record.title}
              </h4>
              <div className="flex items-center gap-2 mt-0.5 text-xs">
                {record.category && (
                  <span className="font-semibold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-100">
                    {record.category}
                  </span>
                )}
                {record.doctor_name && (
                  <span className="text-slate-500">
                    • Attending: <span className="font-medium text-slate-700">{record.doctor_name}</span>
                  </span>
                )}
              </div>
            </div>
            
            <div className="flex items-center gap-2">
              {record.cost_inr > 0 && (
                <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-800 border border-slate-200">
                  ₹{record.cost_inr}
                </span>
              )}
              <button
                onClick={() => setExpanded(!expanded)}
                className="text-xs text-slate-500 hover:text-emerald-800 font-semibold flex items-center gap-1 shrink-0 px-2 py-1 rounded-md hover:bg-slate-100 transition-colors"
              >
                {expanded ? 'Hide Details' : 'View Details'}
                {expanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          {/* Clinical Description */}
          <p className="text-xs text-slate-600 leading-relaxed mb-3">
            {record.description}
          </p>

          {/* Structured Clinical Metrics / Lab values */}
          {metricEntries.length > 0 && (
            <div className="mt-3 pt-3 border-t border-slate-100">
              <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                Clinical Metrics & Diagnostic Observations
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
                {metricEntries.map(([key, value]) => (
                  <div
                    key={key}
                    className="bg-slate-50 border border-slate-200 rounded-lg p-2 text-xs"
                  >
                    <div className="text-slate-500 text-[10px] font-medium capitalize truncate">
                      {key.replace(/_/g, ' ')}
                    </div>
                    <div className="font-bold text-slate-900 mt-0.5 truncate">
                      {String(value)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Medications details if prescription */}
          {medications.length > 0 && (
            <div className="mt-3 pt-3 border-t border-slate-100">
              <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5 flex items-center justify-between">
                <span>Prescribed Regimen ({medications.length})</span>
              </div>
              <div className="space-y-1.5">
                {medications.map((med, idx) => (
                  <div
                    key={idx}
                    className="flex flex-wrap items-center justify-between p-2 rounded-lg bg-teal-50/60 border border-teal-100 text-xs"
                  >
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-[#0B3724]">{med.name}</span>
                      {med.dosage && (
                        <span className="px-1.5 py-0.5 rounded bg-teal-100 text-teal-800 font-semibold text-[11px]">
                          {med.dosage}
                        </span>
                      )}
                      {med.frequency && (
                        <span className="text-slate-600">
                          • {med.frequency}
                        </span>
                      )}
                      {med.duration && (
                        <span className="text-slate-500 text-[11px]">
                          ({med.duration})
                        </span>
                      )}
                    </div>
                    {med.price_inr && (
                      <span className="font-semibold text-slate-700 text-[11px]">
                        ₹{med.price_inr}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Expanded Metadata and Actions */}
          {expanded && (
            <div className="mt-3 pt-3 border-t border-slate-100 text-xs text-slate-500 space-y-2 bg-slate-50/70 p-3 rounded-xl border border-slate-200">
              <div className="flex justify-between">
                <span>Record Reference ID:</span>
                <span className="font-mono font-bold text-slate-800">{record.id}</span>
              </div>
              <div className="flex justify-between">
                <span>Author Attribution:</span>
                <span className="font-medium text-slate-800 capitalize">{record.created_by_role}</span>
              </div>
              {record.follow_up_date && (
                <div className="flex justify-between text-emerald-800 font-medium">
                  <span>Follow-up Recommendation:</span>
                  <span>{record.follow_up_date}</span>
                </div>
              )}
              {record.attachment_name && (
                <div className="flex items-center gap-1.5 text-emerald-800 font-semibold pt-1">
                  <Paperclip className="w-3.5 h-3.5" />
                  <span>Prescription Attachment: {record.attachment_name}</span>
                </div>
              )}

              {/* Delete button if authorized */}
              {onDeleteRecord && (
                <div className="pt-2 border-t border-slate-200 flex justify-end">
                  <button
                    onClick={() => onDeleteRecord(record.id)}
                    className="flex items-center gap-1 px-2.5 py-1 text-xs text-red-600 hover:text-red-800 hover:bg-red-50 rounded-lg transition-colors font-semibold"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Delete Record</span>
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
