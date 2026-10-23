import React from 'react';
import { CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-react';

export default function Toast({ toast, onClose }) {
  if (!toast) return null;

  const bgStyles = {
    success: 'bg-emerald-900/90 border-emerald-500 text-white',
    error: 'bg-rose-900/90 border-rose-500 text-white',
    warning: 'bg-amber-900/90 border-amber-500 text-white',
    info: 'bg-slate-900/90 border-emerald-500 text-white',
  }[toast.type || 'info'];

  const icon = {
    success: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />,
    error: <XCircle className="w-5 h-5 text-rose-400 shrink-0" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />,
    info: <Info className="w-5 h-5 text-emerald-300 shrink-0" />,
  }[toast.type || 'info'];

  return (
    <div className="fixed bottom-6 right-6 z-50 max-w-md animate-in slide-in-from-bottom-4 duration-300 shadow-2xl">
      <div className={`flex items-start gap-3 p-4 rounded-xl border backdrop-blur-md ${bgStyles}`}>
        {icon}
        <div className="flex-1 text-sm">
          {toast.title && <div className="font-semibold text-white mb-0.5">{toast.title}</div>}
          <div className="text-slate-200 text-xs leading-relaxed">{toast.message}</div>
        </div>
        <button
          onClick={onClose}
          className="text-slate-400 hover:text-white transition-colors p-1"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
