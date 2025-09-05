// src/SurveyTake.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";

/* -------------------- API -------------------- */
const api = axios.create({
  baseURL: "",
  withCredentials: true,
});

/* -------------------- Yardımcılar -------------------- */
const nowMs = () => Date.now();
const isExpired = (deadline) => !!deadline && new Date(deadline).getTime() < nowMs();
const isHiddenByHideAfter = (hideAfter) => !!hideAfter && new Date(hideAfter).getTime() < nowMs();
const fmtDateTime = (dt) =>
    !dt ? "" : new Date(dt).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
const sortSurveys = (arr) => {
  const toMs = (v, def) => (v ? new Date(v).getTime() : def);
  return [...arr].sort((a, b) => {
    // 1) Cevaplanmamış (alreadyAnswered=false) → üstte
    const aa = !!a.alreadyAnswered, bb = !!b.alreadyAnswered;
    if (aa !== bb) return aa - bb; // false(0) önce

    // 2) Süresi dolmamış → dolmuşlardan önce
    const ax = isExpired(a.deadline), bx = isExpired(b.deadline);
    if (ax !== bx) return ax - bx; // false(0) önce

    // 3) Deadline yakın olan önce (deadline yoksa sona)
    const adl = toMs(a.deadline, Number.POSITIVE_INFINITY);
    const bdl = toMs(b.deadline, Number.POSITIVE_INFINITY);
    if (adl !== bdl) return adl - bdl;

    // 4) Son çare: createdAt yeni olan üste
    const acr = toMs(a.createdAt, 0);
    const bcr = toMs(b.createdAt, 0);
    return bcr - acr;
  });
};
/* basit ikonlar */
const ChevronDown = ({ className = "w-5 h-5" }) => (
    <svg className={className} viewBox="0 0 20 20" fill="currentColor">
      <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.085l3.71-3.855a.75.75 0 111.08 1.04l-4.24 4.4a.75.75 0 01-1.08 0l-4.24-4.4a.75.75 0 01.02-1.06z" clipRule="evenodd" />
    </svg>
);
const ChevronUp = ({ className = "w-5 h-5" }) => (
    <svg className={className} viewBox="0 0 20 20" fill="currentColor">
      <path fillRule="evenodd" d="M14.77 12.79a.75.75 0 01-1.06-.02L10 8.915l-3.71 3.855a.75.75 0 11-1.08-1.04l4.24-4.4a.75.75 0 011.08 0l4.24 4.4a.75.75 0 01-.02 1.06z" clipRule="evenodd" />
    </svg>
);

/* -------------------- Bileşen -------------------- */
export default function SurveyTake() {
  const [surveys, setSurveys] = useState([]);
  const [answers, setAnswers] = useState({});
  const [sending, setSending] = useState({});
  const [errors, setErrors] = useState({});
  const [infos, setInfos] = useState({});
  const [loading, setLoading] = useState(true);

  // hangi kart(lar) açık?  true/false map
  const [expanded, setExpanded] = useState({}); // { [surveyId]: boolean }

  const toggle = (id) =>
      setExpanded((p) => ({ ...p, [id]: !p[id] }));

  const openOneOnly = (id) =>
      setExpanded((p) => {
        const next = {};
        surveys.forEach((s) => (next[s.id] = false));
        next[id] = !p[id]; // aynı karta tıklanınca kapansın
        return next;
      });

  const load = async () => {
    setLoading(true);
    setErrors({});
    setInfos({});
    try {
      const { data } = await api.get("/api/surveys");
      const visible = (data || []).filter(s => !isHiddenByHideAfter(s.hideAfter));
      setSurveys(sortSurveys(visible));

      const initAns = {};
      const initExp = {};
      visible.forEach((s, i) => {
        const mineByQ = s.alreadyAnswered && s.myAnswers ? s.myAnswers : {};
        const a = {};
        (s.questions || []).forEach((q) => {
          const mineArr = Array.isArray(mineByQ[q.id]) ? mineByQ[q.id] : [];
          if (q.type === "choice" && q.multiple) a[q.id] = [...mineArr];
          else a[q.id] = mineArr.length ? mineArr[0] : "";
        });
        initAns[s.id] = a;
        // ilk kartı açık getir (istersen tümünü kapalı başlat: false)
        initExp[s.id] = i === 0;
      });
      setAnswers(initAns);
      setExpanded(initExp);
    } catch {
      setInfos((p) => ({ ...p, _global: "Anketler yüklenemedi." }));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const canSubmit = (s) => {
    if (!s?.questions?.length) return false;
    if (s.alreadyAnswered) return false;
    if (isExpired(s.deadline)) return false;
    const a = answers[s.id] || {};
    return s.questions.every((q) => {
      const v = a[q.id];
      if (q.type === "choice" && q.multiple) return Array.isArray(v) && v.length > 0;
      return typeof v === "string" ? v.trim() !== "" : v != null;
    });
  };

  const handleChange = (surveyId, qId, value) => {
    setAnswers((prev) => ({
      ...prev,
      [surveyId]: { ...(prev[surveyId] || {}), [qId]: value },
    }));
  };

  const submit = async (s) => {
    if (s.alreadyAnswered) {
      setInfos((p) => ({ ...p, [s.id]: "Bu anketi daha önce yanıtladınız." }));
      return;
    }
    if (isExpired(s.deadline)) {
      setInfos((p) => ({ ...p, [s.id]: "Bu anketin süresi dolmuş, cevap gönderilemez." }));
      return;
    }
    setSending((p) => ({ ...p, [s.id]: true }));
    setErrors((p) => ({ ...p, [s.id]: null }));
    setInfos((p) => ({ ...p, [s.id]: null }));

    try {
      const normalized = {};
      (s.questions || []).forEach((q) => {
        const v = (answers[s.id] || {})[q.id];
        if (q.type === "choice" && q.multiple) normalized[q.id] = Array.isArray(v) ? v : [];
        else normalized[q.id] = [typeof v === "string" ? v : ""];
      });

      await api.post(`/api/surveys/${s.id}/submit`, { answers: normalized });
      setInfos((p) => ({ ...p, [s.id]: "Teşekkürler! Cevabınız kaydedildi." }));
      setSurveys(prev => prev.map(it => it.id === s.id ? { ...it, alreadyAnswered: true } : it));
          } catch (e) {
      const status = e?.response?.status;
      if (status === 409) {
        setInfos((p) => ({ ...p, [s.id]: "Bu anketi zaten yanıtladınız." }));
        setSurveys((prev) => prev.map((it) => (it.id === s.id ? { ...it, alreadyAnswered: true } : it)));
      } else if (status === 403) {
        setInfos((p) => ({ ...p, [s.id]: "Bu anketin süresi doldu." }));
      } else {
        setErrors((p) => ({ ...p, [s.id]: "Cevap gönderilemedi." }));
      }
    } finally {
      setSending((p) => ({ ...p, [s.id]: false }));
    }
  };

  /* -------------------- UI -------------------- */
  if (loading) return <p>Anketler yükleniyor...</p>;
  if (!surveys.length) return <p>Görüntülenecek anket yok.</p>;

  return (
      <div className="max-w-3xl mx-auto p-6 space-y-4">
        {infos._global && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 text-amber-800 p-3">{infos._global}</div>
        )}

        {surveys.map((s) => {
          if (isHiddenByHideAfter(s.hideAfter)) return null;
          const expired = isExpired(s.deadline);
          const disabled = expired || s.alreadyAnswered;
          const isOpen = !!expanded[s.id];

          return (
              <div key={s.id} className="border rounded-xl bg-white overflow-hidden">
                {/* Header (kapalıyken sadece burası görünür) */}
                <button
                    type="button"
                    onClick={() => openOneOnly(s.id)} // tek açılır istiyorsan; hepsi bağımsız açılsın istersen toggle(s.id) yap
                    className={`w-full flex items-center gap-3 justify-between px-4 py-3 text-left
                          ${isOpen ? "bg-slate-50" : "bg-white"} hover:bg-slate-50`}
                >
                  <div className="flex-1 min-w-0">
                    <div className="font-semibold truncate">{s.title}</div>
                    {/* küçük açıklama satırı istersen: */}
                    {s.description && <div className="text-slate-600 text-sm line-clamp-1">{s.description}</div>}
                  </div>

                  <div className="flex items-center gap-2 flex-shrink-0">
                    {s.deadline && (
                        <span className="text-xs rounded-full px-2 py-1 border bg-slate-50 text-slate-700">
                    Son gün: {fmtDateTime(s.deadline)}
                  </span>
                    )}
                    {expired && (
                        <span className="text-xs rounded-full px-2 py-1 bg-amber-100 text-amber-800 border border-amber-300">
                    Süresi doldu
                  </span>
                    )}
                    {!expired && s.alreadyAnswered && (
                        <span className="text-xs rounded-full px-2 py-1 bg-emerald-100 text-emerald-800 border border-emerald-300">
                    Cevaplandı
                  </span>
                    )}
                    <span className="text-slate-600 ml-2">{isOpen ? <ChevronUp /> : <ChevronDown />}</span>
                  </div>
                </button>

                {/* Body (sorular) */}
                {isOpen && (
                    <div className="px-4 pb-4 pt-2 border-t">
                      {expired && (
                          <div className="mb-3 rounded-lg border border-amber-300 bg-amber-50 text-amber-800 p-2">
                            Bu anketin süresi doldu. Cevap gönderilemez.
                          </div>
                      )}
                      {s.alreadyAnswered && !expired && (
                          <div className="mb-3 rounded-lg border border-emerald-300 bg-emerald-50 text-emerald-800 p-2">
                            Bu anketi daha önce yanıtladınız. Yanıtlarınız kilitli olarak gösteriliyor.
                          </div>
                      )}
                      {errors[s.id] && (
                          <div className="mb-3 rounded-lg border border-red-300 bg-red-50 text-red-800 p-2">{errors[s.id]}</div>
                      )}
                      {infos[s.id] && (
                          <div className="mb-3 rounded-lg border border-emerald-300 bg-emerald-50 text-emerald-800 p-2">
                            {infos[s.id]}
                          </div>
                      )}

                      <div className="space-y-4">
                        {(s.questions || []).map((q, idx) => (
                            <div key={q.id} className="relative border rounded-lg p-3">
                              <div className="mb-2 font-medium">
                                {idx + 1}. {q.questionText}
                              </div>

                              {q.type === "text" && (
                                  <textarea
                                      className="border rounded-md p-2 w-full disabled:bg-slate-100"
                                      rows={3}
                                      placeholder="Yanıtınız..."
                                      value={answers[s.id]?.[q.id] ?? ""}
                                      disabled={disabled}
                                      onChange={(e) => handleChange(s.id, q.id, e.target.value)}
                                  />
                              )}

                              {q.type === "choice" && !q.multiple && (
                                  <div className={`grid gap-2 ${disabled ? "opacity-80" : ""}`}>
                                    {(q.options || []).map((opt, oIdx) => (
                                        <label key={oIdx} className="inline-flex items-center gap-2">
                                          <input
                                              type="radio"
                                              name={`s-${s.id}-q-${q.id}`}
                                              value={opt}
                                              checked={answers[s.id]?.[q.id] === opt}
                                              disabled={disabled}
                                              onChange={(e) => handleChange(s.id, q.id, e.target.value)}
                                          />
                                          <span>{opt}</span>
                                        </label>
                                    ))}
                                  </div>
                              )}

                              {q.type === "choice" && q.multiple && (
                                  <div className={`grid gap-2 ${disabled ? "opacity-80" : ""}`}>
                                    {(q.options || []).map((opt, oIdx) => {
                                      const arr = Array.isArray(answers[s.id]?.[q.id]) ? answers[s.id][q.id] : [];
                                      const checked = arr.includes(opt);
                                      return (
                                          <label key={oIdx} className="inline-flex items-center gap-2">
                                            <input
                                                type="checkbox"
                                                value={opt}
                                                checked={checked}
                                                disabled={disabled}
                                                onChange={(e) => {
                                                  const next = new Set(arr);
                                                  if (e.target.checked) next.add(opt);
                                                  else next.delete(opt);
                                                  handleChange(s.id, q.id, Array.from(next));
                                                }}
                                            />
                                            <span>{opt}</span>
                                          </label>
                                      );
                                    })}
                                  </div>
                              )}
                            </div>
                        ))}
                      </div>

                      <div className="pt-4 flex justify-end">
                        <button
                            onClick={() => submit(s)}
                            disabled={!canSubmit(s) || sending[s.id] || disabled}
                            className={`rounded-lg px-4 py-2 font-semibold ${
                                !canSubmit(s) || sending[s.id] || disabled
                                    ? "bg-slate-300 text-slate-600 cursor-not-allowed"
                                    : "bg-green-600 text-white hover:bg-green-700"
                            }`}
                        >
                          {isExpired(s.deadline)
                              ? "Süresi doldu"
                              : s.alreadyAnswered
                                  ? "Zaten yanıtlanmış"
                                  : sending[s.id]
                                      ? "Gönderiliyor..."
                                      : "Gönder"}
                        </button>
                      </div>
                    </div>
                )}
              </div>
          );
        })}
      </div>
  );
}