import React from 'react';
import { 
  Activity, 
  ShieldCheck, 
  FileText, 
  History, 
  Stethoscope, 
  Building2, 
  Lock, 
  Users, 
  Database,
  Share2
} from 'lucide-react';

export default function Sidebar({ currentView, onSelectView, persona, stats, pendingCount }) {
  const getNavItems = () => {
    if (persona.role === 'patient') {
      return [
        {
          id: 'timeline',
          label: 'Health Timeline',
          icon: Activity,
          badge: null,
        },
        {
          id: 'consents',
          label: 'Consent Requests',
          icon: Lock,
          badge: pendingCount > 0 ? pendingCount : null,
          badgeColor: 'bg-amber-500 text-white',
        },
        {
          id: 'audit',
          label: 'Access History Log',
          icon: History,
          badge: null,
        },
      ];
    }

    if (persona.role === 'doctor') {
      return [
        {
          id: 'timeline',
          label: 'Patient Workstation',
          icon: Stethoscope,
          badge: null,
        },
        {
          id: 'audit',
          label: 'Clinical Audit Trail',
          icon: History,
          badge: null,
        },
      ];
    }

    if (persona.role === 'provider') {
      return [
        {
          id: 'timeline',
          label: 'Diagnostic Dispatch',
          icon: Building2,
          badge: null,
        },
        {
          id: 'audit',
          label: 'Sharing & Audit Logs',
          icon: History,
          badge: null,
        },
      ];
    }

    return [];
  };

  const navItems = getNavItems();

  return (
    <aside className="w-full md:w-64 shrink-0 flex flex-col justify-between space-y-6">
      {/* Role Profile Card */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-xs">
        <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider mb-2">
          Current Workspace
        </div>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-600 to-[#0B3724] text-white flex items-center justify-center font-bold text-sm shadow-xs">
            {persona.name.split(' ').map(n => n[0]).join('')}
          </div>
          <div className="overflow-hidden">
            <div className="font-bold text-slate-900 text-sm truncate">
              {persona.name}
            </div>
            <div className="text-xs text-emerald-800 capitalize font-medium truncate">
              {persona.role} • {persona.id}
            </div>
          </div>
        </div>
      </div>

      {/* Nav Menu */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-3 shadow-xs space-y-1">
        <div className="px-3 py-1.5 text-[10px] uppercase font-bold text-slate-400 tracking-wider">
          Navigation
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentView === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectView(item.id)}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all ${
                isActive
                  ? 'bg-[#0B3724] text-white shadow-xs'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Icon className={`w-4 h-4 ${isActive ? 'text-emerald-400' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </div>
              {item.badge && (
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${item.badgeColor || 'bg-emerald-600 text-white'}`}>
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Platform Architecture & Stats Card */}
      {stats && (
        <div className="bg-gradient-to-br from-[#0B3724] to-[#124A32] rounded-2xl p-4 text-white shadow-md space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-emerald-300 uppercase tracking-wider flex items-center gap-1.5">
              <Database className="w-3.5 h-3.5" />
              Unified Node Stats
            </span>
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-400/20 text-emerald-300 font-mono">
              Live
            </span>
          </div>

          <div className="grid grid-cols-2 gap-2 text-xs pt-1">
            <div className="bg-emerald-950/50 p-2 rounded-lg border border-emerald-800/60">
              <div className="text-emerald-300 text-[10px]">Total Records</div>
              <div className="font-bold text-base text-white">{stats.total_records || 12}</div>
            </div>
            <div className="bg-emerald-950/50 p-2 rounded-lg border border-emerald-800/60">
              <div className="text-emerald-300 text-[10px]">Providers</div>
              <div className="font-bold text-base text-white">{stats.unique_providers || 3}</div>
            </div>
            <div className="bg-emerald-950/50 p-2 rounded-lg border border-emerald-800/60">
              <div className="text-emerald-300 text-[10px]">Active Grants</div>
              <div className="font-bold text-base text-white">{stats.active_permissions || 1}</div>
            </div>
            <div className="bg-emerald-950/50 p-2 rounded-lg border border-emerald-800/60">
              <div className="text-emerald-300 text-[10px]">Pending Consents</div>
              <div className="font-bold text-base text-amber-300">{stats.pending_requests || 1}</div>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}
