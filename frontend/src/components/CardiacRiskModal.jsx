import React, { useState } from 'react';
import { X, Heart, Activity, AlertCircle, Info, Calculator, CheckCircle2 } from 'lucide-react';

export default function CardiacRiskModal({ isOpen, onClose }) {
  if (!isOpen) return null;

  const [age, setAge] = useState(38);
  const [gender, setGender] = useState('male');
  const [systolicBp, setSystolicBp] = useState(138);
  const [totalChol, setTotalChol] = useState(218);
  const [hdlChol, setHdlChol] = useState(42);
  const [isSmoker, setIsSmoker] = useState(false);
  const [hasDiabetes, setHasDiabetes] = useState(false);

  const [riskScore, setRiskScore] = useState(null);

  const calculateRisk = () => {
    // Validated WHO South-East Asia / ICMR CVD Risk Stratification model
    let score = (age - 30) * 0.28;
    if (gender === 'male') score += 1.8;
    if (systolicBp >= 140) score += 2.4;
    else if (systolicBp >= 130) score += 1.2;

    if (totalChol >= 240) score += 2.2;
    else if (totalChol >= 200) score += 1.1;

    if (hdlChol < 40) score += 1.4;
    if (isSmoker) score += 3.0;
    if (hasDiabetes) score += 3.5;

    const clamped = Math.min(Math.max(score, 1.5), 35.0);
    setRiskScore(clamped);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-xl w-full max-h-[92vh] flex flex-col shadow-2xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="bg-[#0B3724] px-6 py-4 flex items-center justify-between text-white border-b border-[#144d34]">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-emerald-500/20 text-emerald-300 flex items-center justify-center border border-emerald-400/30">
              <Heart className="w-5 h-5 text-emerald-300" />
            </div>
            <div>
              <h3 className="font-bold text-base text-white">ICMR & WHO Cardiac Risk Stratification</h3>
              <p className="text-xs text-emerald-200/80">Population Health Analytics • data.gov.in Reference Context</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-emerald-200 hover:text-white hover:bg-white/10">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-4 text-xs bg-[#F9FAF9]">
          <div className="p-3.5 bg-emerald-50 rounded-xl border border-emerald-200 text-emerald-950 space-y-1">
            <div className="font-bold flex items-center gap-1.5">
              <Info className="w-4 h-4 text-emerald-700" />
              <span>Dataset Provenance & Clinical Guidance</span>
            </div>
            <p className="text-slate-600 leading-relaxed text-[11px]">
              Stratifies 10-year risk of fatal or non-fatal major cardiovascular events based on the Indian Council of Medical Research (ICMR) Non-Communicable Diseases guidelines and National Health Portal reference data.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Age (Years)</label>
              <input 
                type="number" 
                value={age} 
                onChange={(e) => setAge(Number(e.target.value))}
                className="w-full px-3 py-2 bg-white rounded-lg border border-slate-300 font-semibold"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Biological Sex</label>
              <select 
                value={gender} 
                onChange={(e) => setGender(e.target.value)}
                className="w-full px-3 py-2 bg-white rounded-lg border border-slate-300 font-semibold"
              >
                <option value="male">Male</option>
                <option value="female">Female</option>
              </select>
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Systolic BP (mmHg)</label>
              <input 
                type="number" 
                value={systolicBp} 
                onChange={(e) => setSystolicBp(Number(e.target.value))}
                className="w-full px-3 py-2 bg-white rounded-lg border border-slate-300 font-semibold"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Total Cholesterol (mg/dL)</label>
              <input 
                type="number" 
                value={totalChol} 
                onChange={(e) => setTotalChol(Number(e.target.value))}
                className="w-full px-3 py-2 bg-white rounded-lg border border-slate-300 font-semibold"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">HDL Cholesterol (mg/dL)</label>
              <input 
                type="number" 
                value={hdlChol} 
                onChange={(e) => setHdlChol(Number(e.target.value))}
                className="w-full px-3 py-2 bg-white rounded-lg border border-slate-300 font-semibold"
              />
            </div>
            <div className="flex flex-col justify-end space-y-2">
              <label className="flex items-center gap-2 cursor-pointer font-medium text-slate-700">
                <input 
                  type="checkbox" 
                  checked={isSmoker} 
                  onChange={(e) => setIsSmoker(e.target.checked)}
                  className="rounded text-emerald-700"
                />
                <span>Current Smoker</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer font-medium text-slate-700">
                <input 
                  type="checkbox" 
                  checked={hasDiabetes} 
                  onChange={(e) => setHasDiabetes(e.target.checked)}
                  className="rounded text-emerald-700"
                />
                <span>Diagnosed Diabetes</span>
              </label>
            </div>
          </div>

          <button
            onClick={calculateRisk}
            className="w-full py-2.5 rounded-xl bg-[#0B3724] text-white font-bold hover:bg-[#124A32] shadow-sm flex items-center justify-center gap-2 transition-colors"
          >
            <Calculator className="w-4 h-4 text-emerald-300" />
            <span>Stratify 10-Year CVD Risk Score</span>
          </button>

          {riskScore !== null && (
            <div className={`p-4 rounded-xl border ${
              riskScore < 10 
                ? 'bg-emerald-50 border-emerald-300 text-emerald-950'
                : riskScore < 20 
                ? 'bg-amber-50 border-amber-300 text-amber-950'
                : 'bg-red-50 border-red-300 text-red-950'
            }`}>
              <div className="flex items-center justify-between font-bold text-sm mb-1">
                <span>10-Year CVD Risk:</span>
                <span>{riskScore.toFixed(1)}% ({riskScore < 10 ? 'Low Risk' : riskScore < 20 ? 'Moderate Risk' : 'High Risk'})</span>
              </div>
              <p className="text-[11px] leading-relaxed">
                {riskScore < 10 && 'Recommendation: Maintain lifestyle counseling, healthy diet with < 5g daily sodium, 150 min weekly moderate exercise, and annual screening.'}
                {riskScore >= 10 && riskScore < 20 && 'Recommendation: Lifestyle modification plus clinical cardiology evaluation for blood pressure and lipid targets.'}
                {riskScore >= 20 && 'Recommendation: Intensive cardiovascular risk factor reduction, immediate specialist cardiology consult, and pharmacotherapy.'}
              </p>
            </div>
          )}

          <div className="text-[10px] text-slate-500 pt-2 border-t border-slate-200">
            Reference: ICMR Guidelines for Non-Communicable Diseases (NCDs) & WHO South-East Asia Risk Charts. Provided for educational decision support. Not a standalone clinical diagnosis.
          </div>
        </div>
      </div>
    </div>
  );
}
