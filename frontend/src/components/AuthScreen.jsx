import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Lock, 
  User, 
  Stethoscope, 
  Key, 
  AlertCircle, 
  CheckCircle2, 
  ArrowRight,
  ShieldAlert
} from 'lucide-react';
import { api } from '../api';

export default function AuthScreen({ onAuthSuccess, onShowToast }) {
  const [isSignInTab, setIsSignInTab] = useState(true);
  const [username, setUsername] = useState('sarah.jenkins');
  const [password, setPassword] = useState('patient123');

  // Registration state
  const [regData, setRegData] = useState({
    name: 'Rajesh Kumar',
    username: 'rajesh.kumar',
    password: 'patient123',
    dob: '1988-04-12',
    gender: 'Male',
    blood_type: 'O+',
    phone: '+91 98201 44521',
    emergency_contact: 'Sunita Kumar (Spouse) - +91 98201 44522',
    abha_id: '91-4452-9812-4019'
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSignIn = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.login(username, password);
      if (res.ok) {
        onAuthSuccess(res.user);
        onShowToast({
          type: 'success',
          title: 'Authentication Successful',
          message: `Logged in as ${res.user.name} (${res.user.role.toUpperCase()})`,
        });
      } else {
        setError(res.error || 'Invalid credentials. Please verify your username and password.');
      }
    } catch (err) {
      setError('Unable to communicate with MedIt backend server.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.register(regData);
      if (res.ok) {
        onAuthSuccess(res.user);
        onShowToast({
          type: 'success',
          title: 'Account Registered',
          message: `Patient account created with MedIt ID ${res.user.id}`,
        });
      } else {
        setError(res.error || 'Registration failed.');
      }
    } catch (err) {
      setError('Registration failed due to network error.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-center items-center p-4 selection:bg-emerald-100 selection:text-emerald-900">
      <div className="max-w-md w-full space-y-6">
        
        {/* Brand Hero */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-[#0B3724] text-emerald-400 shadow-lg shadow-emerald-950/20 mb-1">
            <ShieldCheck className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">MedIt</h1>
          <p className="text-xs text-slate-600 font-medium max-w-sm mx-auto">
            Unified Patient Health Records Platform • Indian Health Ecosystem (ABDM Aligned)
          </p>
        </div>

        {/* Auth Card */}
        <div className="bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
          
          {/* Tabs */}
          <div className="grid grid-cols-2 border-b border-slate-200 bg-slate-50 text-xs font-bold text-slate-500">
            <button
              type="button"
              onClick={() => { setIsSignInTab(true); setError(null); }}
              className={`py-3.5 text-center transition-colors ${isSignInTab ? 'bg-white text-[#0B3724] border-b-2 border-[#0B3724]' : 'hover:text-slate-900'}`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => { setIsSignInTab(false); setError(null); }}
              className={`py-3.5 text-center transition-colors ${!isSignInTab ? 'bg-white text-[#0B3724] border-b-2 border-[#0B3724]' : 'hover:text-slate-900'}`}
            >
              Create Account
            </button>
          </div>

          <div className="p-6 space-y-4">
            
            {/* Quick Demo Fill Chips (Hackathon Presentation) */}
            {isSignInTab && (
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-700">
                  <Key className="w-3.5 h-3.5 text-emerald-700" />
                  <span>Hackathon Demo Credentials:</span>
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => {
                      setUsername('admin123');
                      setPassword('Doctor@MedIt2026');
                      setError(null);
                    }}
                    className="p-2 rounded-lg bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 text-left transition-colors"
                  >
                    <div className="font-bold text-[#0B3724] text-[11px]">Doctor Workspace</div>
                    <div className="text-[10px] text-emerald-800">admin123</div>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setUsername('sarah.jenkins');
                      setPassword('patient123');
                      setError(null);
                    }}
                    className="p-2 rounded-lg bg-slate-100 hover:bg-slate-200/80 border border-slate-200 text-left transition-colors"
                  >
                    <div className="font-bold text-slate-800 text-[11px]">Patient Portal</div>
                    <div className="text-[10px] text-slate-600">sarah.jenkins</div>
                  </button>
                </div>
              </div>
            )}

            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            {isSignInTab ? (
              /* Sign In Form */
              <form onSubmit={handleSignIn} className="space-y-3.5">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Username or MedIt ID</label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                    <input
                      type="text"
                      required
                      placeholder="e.g. admin123 or sarah.jenkins"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      className="w-full text-xs pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 focus:outline-hidden focus:border-emerald-600 bg-white"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Password</label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                    <input
                      type="password"
                      required
                      placeholder="Enter password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full text-xs pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 focus:outline-hidden focus:border-emerald-600 bg-white"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-2.5 rounded-xl bg-[#0B3724] text-white text-xs font-bold hover:bg-[#124A32] shadow-sm flex items-center justify-center gap-2 transition-colors disabled:opacity-50 mt-2"
                >
                  {loading ? 'Authenticating...' : 'Sign In to Dashboard'}
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </form>
            ) : (
              /* Registration Form */
              <form onSubmit={handleRegister} className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Full Patient Name *</label>
                  <input
                    type="text"
                    required
                    value={regData.name}
                    onChange={(e) => setRegData({ ...regData, name: e.target.value })}
                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-300 focus:outline-hidden focus:border-emerald-600 bg-white"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Date of Birth</label>
                    <input
                      type="date"
                      value={regData.dob}
                      onChange={(e) => setRegData({ ...regData, dob: e.target.value })}
                      className="w-full text-xs px-2.5 py-2 rounded-xl border border-slate-300 bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Blood Group</label>
                    <select
                      value={regData.blood_type}
                      onChange={(e) => setRegData({ ...regData, blood_type: e.target.value })}
                      className="w-full text-xs px-2.5 py-2 rounded-xl border border-slate-300 bg-white"
                    >
                      {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map(b => <option key={b} value={b}>{b}</option>)}
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Mobile Phone Number</label>
                  <input
                    type="tel"
                    value={regData.phone}
                    onChange={(e) => setRegData({ ...regData, phone: e.target.value })}
                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-300 bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Emergency Contact</label>
                  <input
                    type="text"
                    value={regData.emergency_contact}
                    onChange={(e) => setRegData({ ...regData, emergency_contact: e.target.value })}
                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-300 bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">ABHA ID (Ayushman Bharat, Optional)</label>
                  <input
                    type="text"
                    value={regData.abha_id}
                    onChange={(e) => setRegData({ ...regData, abha_id: e.target.value })}
                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-300 bg-white"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Username *</label>
                    <input
                      type="text"
                      required
                      value={regData.username}
                      onChange={(e) => setRegData({ ...regData, username: e.target.value })}
                      className="w-full text-xs px-2.5 py-2 rounded-xl border border-slate-300 bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Password *</label>
                    <input
                      type="password"
                      required
                      value={regData.password}
                      onChange={(e) => setRegData({ ...regData, password: e.target.value })}
                      className="w-full text-xs px-2.5 py-2 rounded-xl border border-slate-300 bg-white"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-2.5 rounded-xl bg-[#0B3724] text-white text-xs font-bold hover:bg-[#124A32] shadow-sm flex items-center justify-center gap-2 transition-colors disabled:opacity-50 mt-2"
                >
                  {loading ? 'Creating Account...' : 'Register & Open Portal'}
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </form>
            )}

          </div>
        </div>

        {/* Security and privacy footnote */}
        <div className="text-center text-[11px] text-slate-500 flex items-center justify-center gap-1.5">
          <ShieldAlert className="w-3.5 h-3.5 text-emerald-700" />
          <span>Patient-gated consent • ABDM & DPDP privacy safeguards</span>
        </div>

      </div>
    </div>
  );
}
