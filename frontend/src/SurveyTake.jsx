// src/SurveyTake.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";

/* -------------------- API -------------------- */
const api = axios.create({
  baseURL: "",            // aynı origin/proxy
  withCredentials: true,  // auth cookie/Keycloak
});

/* -------------------- Yardımcılar -------------------- */
const nowMs = () => Date.now();

const isExpired = (deadline) =>
    !!deadline && new Date(deadline).getTime() < nowMs();

const isHiddenByHideAfter = (hideAfter) =>
    !!hideAfter && new Date(hideAfter).getTime() < nowMs();

const fmtDateTime = (dt) =>
    !dt ? "" : new Date(dt).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });

/* -------------------- Bileşen -------------------- */
export default function SurveyTake() {
  const [surveys, setSurveys]   = useState([]);  // listelenen anketler
  const [answers, setAnswers]   = useState({});  // { [surveyId]: { [qId]: string | string[] } }
  const [sending, setSending]   = useState({});  // { [surveyId]: bool }
  const [errors, setErrors]     = useState({});  // { [surveyId]: string|null }
  const [infos, setInfos]       = useState({});  // { [surveyId]: string|null }
  const [loading, setLoading]   = useState(true);

  // Anketleri çek + hideAfter'a göre yerelde filtrele + cevap state'ini hazırla
  const load = async () => {
    setLoading(true);
    setErrors({});
    setInfos({});
    try {
      const { data } = await api.get("/api/surveys");

      // HideAfter geçmiş olanlar kullanıcıya görünmesin (backend de süzüyor olabilir)
      const visible = (data || []).filter(s => !isHiddenByHideAfter(s.hideAfter));

      setSurveys(visible);

      // myAnswers artık Map<Long, List<String>> — state ön dolumu
      const init = {};
      visible.forEach(s => {
        const mineByQ = s.alreadyAnswered && s.myAnswers ? s.myAnswers : {};
        const a = {};
        (s.questions || []).forEach(q => {
          const mineArr = Array.isArray(mineByQ[q.id]) ? mineByQ[q.id] : [];
          if (q.type === "choice" && q.multiple) {
            a[q.id] = [...mineArr];                 // çoklu → dizi
          } else {
            a[q.id] = mineArr.length ? mineArr[0] : ""; // tekli/text → string
          }
        });
        init[s.id] = a;
      });
      setAnswers(init);
    } catch {
      setInfos(prev => ({ ...prev, _global: "Anketler yüklenemedi." }));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  // Form uygun mu?
  const canSubmit = (s) => {
    if (!s?.questions?.length) return false;
    if (s.alreadyAnswered) return false;
    if (isExpired(s.deadline)) return false; // deadline varsa ve geçtiyse gönderme
    const a = answers[s.id] || {};
    return s.questions.every(q => {
      const v = a[q.id];
      if (q.type === "choice" && q.multiple) {
        return Array.isArray(v) && v.length > 0;
      }
      return typeof v === "string" ? v.trim() !== "" : v != null;
    });
  };

  // Cevap değişimi
  const handleChange = (surveyId, qId, value) => {
    setAnswers(prev => ({
      ...prev,
      [surveyId]: { ...(prev[surveyId] || {}), [qId]: value }
    }));
  };

  // Gönder
  const submit = async (s) => {
    if (s.alreadyAnswered) {
      setInfos(prev => ({ ...prev, [s.id]: "Bu anketi daha önce yanıtladınız." }));
      return;
    }
    if (isExpired(s.deadline)) {
      setInfos(prev => ({ ...prev, [s.id]: "Bu anketin süresi dolmuş, cevap gönderilemez." }));
      return;
    }

    setSending(prev => ({ ...prev, [s.id]: true }));
    setErrors(prev => ({ ...prev, [s.id]: null }));
    setInfos(prev  => ({ ...prev, [s.id]: null }));

    try {
      // Backend beklenen: { answers: Map<Long, List<String>> }
      const normalized = {};
      (s.questions || []).forEach(q => {
        const v = (answers[s.id] || {})[q.id];
        if (q.type === "choice" && q.multiple) {
          normalized[q.id] = Array.isArray(v) ? v : [];
        } else {
          const one = typeof v === "string" ? v : "";
          normalized[q.id] = [one];
        }
      });

      await api.post(`/api/surveys/${s.id}/submit`, { answers: normalized });

      setInfos(prev => ({ ...prev, [s.id]: "Teşekkürler! Cevabınız kaydedildi." }));
      // UI'da kilitle
      setSurveys(prev => prev.map(it => it.id === s.id ? { ...it, alreadyAnswered: true } : it));
    } catch (e) {
      const status = e?.response?.status;
      if (status === 409) {
        setInfos(prev => ({ ...prev, [s.id]: "Bu anketi zaten yanıtladınız." }));
        setSurveys(prev => prev.map(it => it.id === s.id ? { ...it, alreadyAnswered: true } : it));
      } else if (status === 403) {
        setInfos(prev => ({ ...prev, [s.id]: "Bu anketin süresi doldu." }));
      } else {
        setErrors(prev => ({ ...prev, [s.id]: "Cevap gönderilemedi." }));
      }
    } finally {
      setSending(prev => ({ ...prev, [s.id]: false }));
    }
  };

  /* -------------------- UI -------------------- */
  if (loading) return <p>Anketler yükleniyor...</p>;
  if (!surveys.length) return <p>Görüntülenecek anket yok.</p>;

  return (
      <div className="max-w-3xl mx-auto p-6 space-y-6">
        {infos._global && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 text-amber-800 p-3">
              {infos._global}
            </div>
        )}

        {surveys.map(s => {
          // Güvenlik: hideAfter geçtiyse hiç renderlama (yukarıda zaten filtreliyoruz)
          if (isHiddenByHideAfter(s.hideAfter)) return null;

          const expired = isExpired(s.deadline);
          const disabled = expired || s.alreadyAnswered;

          return (
              <div key={s.id} className="relative border rounded-xl p-4 bg-white">
                {/* Sağ üst rozetler: createdAt, deadline, durum */}
                <div className="absolute top-2 right-3 flex items-center gap-2 flex-wrap justify-end">
                  {s.createdAt && (
                      <span className="text-xs rounded-full px-2 py-1 border bg-slate-50 text-slate-700">
                  Oluşturuldu: {fmtDateTime(s.createdAt)}
                </span>
                  )}
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
                </div>

                <h2 className="text-xl font-bold mb-1">{s.title}</h2>
                {s.description && (
                    <p className="mb-4 text-slate-700">{s.description}</p>
                )}

                {/* Bilgilendirme kutuları */}
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
                    <div className="mb-3 rounded-lg border border-red-300 bg-red-50 text-red-800 p-2">
                      {errors[s.id]}
                    </div>
                )}
                {infos[s.id] && (
                    <div className="mb-3 rounded-lg border border-emerald-300 bg-emerald-50 text-emerald-800 p-2">
                      {infos[s.id]}
                    </div>
                )}

                {/* Sorular */}
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
                                value={(answers[s.id]?.[q.id]) ?? ""}
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
                                        checked={(answers[s.id]?.[q.id]) === opt}
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
                                            if (e.target.checked) next.add(opt); else next.delete(opt);
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

                {/* Gönder butonu */}
                <div className="pt-4">
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
                            : (sending[s.id] ? "Gönderiliyor..." : "Gönder")}
                  </button>
                </div>
              </div>
          );
        })}
      </div>
  );
}