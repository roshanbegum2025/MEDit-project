import React from 'react';
import { 
  ShieldCheck, 
  User, 
  Stethoscope, 
  Building2, 
  RotateCcw, 
  Heart,
  FileText,
  LogOut,
  History
} from 'lucide-react';

export default function Navbar({ 
  currentUser, 
  onSignOut, 
  onOpenCardiacRisk,
  onOpenAuditTrail,
  onResetDemo, 
  isResetting 
}) {
  return (
    <header className="sticky top-0 z-40 bg-[#0B3724] border-b border-[#144d34] shadow-md text-white">
      {/* Top Clinical Utility Bar (Professional branding without fake disclaimers) */}
      <div className="bg-[#072418] px-4 py-1.5 text-xs text-emerald-200/90 flex flex-wrap items-center justify-between border-b border-emerald-950">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 uppercase tracking-wider">
            ABDM Aligned
          </span>
          <span className="text-emerald-100/90 font-medium hidden sm:inline">
            Unified Patient Health Records Platform • End-to-End Consent Architecture
          </span>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={onOpenCardiacRisk}
            className="flex items-center gap-1.5 px-2 py-0.5 rounded text-[11px] bg-emerald-900/60 hover:bg-emerald-800 text-emerald-200 border border-emerald-700/50 transition-colors"
            title="ICMR Population Cardiovascular Risk Reference"
          >
            <Heart className="w-3 h-3 text-emerald-400" />
            <span className="hidden md:inline">ICMR Cardiac Risk Reference</span>
          </button>

          <button
            onClick={onOpenAuditTrail}
            className="flex items-center gap-1.5 px-2 py-0.5 rounded text-[11px] bg-emerald-900/60 hover:bg-emerald-800 text-emerald-200 border border-emerald-700/50 transition-colors"
            title="View Immutable Consent Audit Trail"
          >
            <History className="w-3 h-3 text-emerald-400" />
            <span className="hidden md:inline">Audit Trail</span>
          </button>

          <button
            onClick={onResetDemo}
            disabled={isResetting}
            className="flex items-center gap-1 px-2 py-0.5 rounded text-[11px] bg-emerald-950 hover:bg-emerald-900 text-emerald-300 border border-emerald-800 transition-colors disabled:opacity-50"
            title="Reset database to initial seed state"
          >
            <RotateCcw className={`w-3 h-3 ${isResetting ? 'animate-spin' : ''}`} />
            <span>{isResetting ? 'Resetting...' : 'Reset Data'}</span>
          </button>
        </div>
      </div>

      {/* Main navigation header */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        {/* Brand logo */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-400 to-[#10B981] flex items-center justify-center text-[#0B3724] shadow-md font-black text-xl">
            <ShieldCheck className="w-6 h-6 text-[#0B3724]" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-xl tracking-tight text-white">MedIt</span>
              <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-400/20 text-emerald-300 border border-emerald-400/30">
                Unified Health
              </span>
            </div>
            <div className="text-xs text-emerald-200/80 hidden sm:block">
              One Patient • One Timeline • Consent-Based Access
            </div>
          </div>
        </div>

        {/* Authenticated User Status & Logout */}
        {currentUser && (
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <div className="text-xs font-bold text-white flex items-center justify-end gap-1.5">
                <span>{currentUser.name}</span>
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-emerald-400/20 text-emerald-300 uppercase tracking-wider">
                  {currentUser.role}
                </span>
              </div>
              <div className="text-[11px] text-emerald-200/70">
                {currentUser.organization || `ID: ${currentUser.id}`}
              </div>
            </div>

            <button
              onClick={onSignOut}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-950/70 hover:bg-emerald-900 text-emerald-200 border border-emerald-700/60 text-xs font-semibold transition-colors"
              title="Sign Out"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Sign Out</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
