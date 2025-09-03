// src/SurveyAdmin.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";

/* -------------------- API -------------------- */
const api = axios.create({
  baseURL: "",            // aynı origin/proxy
  withCredentials: true,  // auth cookie/Keycloak
});
const toLocalLdt = (v) => {
  if (!v) return null;
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(v)) return v + ":00";

  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(v)) return v;

  const d = new Date(v);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};
/* -------------- Yardımcı Fonksiyonlar -------------- */
const emptyQuestion = () => ({ questionText: "", type: "text", options: [], multiple: false });

const toIso = (v) => (v ? `${v}:00` : null);

// Küçük rozet
const Badge = ({ children, color = "slate" }) => {
  const map = {
    slate: "bg-slate-100 text-slate-800 border-slate-200",
    green: "bg-emerald-100 text-emerald-800 border-emerald-200",
    red:   "bg-rose-100 text-rose-800 border-rose-200",
    blue:  "bg-blue-100 text-blue-800 border-blue-200",
    amber: "bg-amber-100 text-amber-800 border-amber-200",
  };
  return (
      <span className={`text-xs px-2 py-1 rounded-full border ${map[color]}`}>
      {children}
    </span>
  );
};

/* ---- Dikey sütun grafiği (library-free) ---- */
const ColumnChart = ({ data, onBarClick }) => {
  // data: [{ label, count }...]
  const total = data.reduce((a, r) => a + (r.count || 0), 0);
  const max = data.reduce((m, r) => Math.max(m, r.count || 0), 0);
  const items = data.map(r => {
    const pct = total > 0 ? Math.round((r.count / total) * 100) : 0;
    const h = max > 0 ? Math.round((r.count / max) * 100) : 0; // görsel yükseklik
    return { ...r, pct, h };
  });

  const colWidth =
      items.length <= 4 ? "w-24" : items.length <= 8 ? "w-16" : "w-12";

  return (
      <div className="overflow-x-auto">
        <div className="min-h-[220px] flex items-end gap-4 px-2 pt-6 pb-2 border rounded-lg bg-slate-50/60">
          {items.map((it, i) => (
              <button
                  key={i}
                  type="button"
                  className={`flex flex-col items-center ${colWidth} select-none focus:outline-none`}
                  onClick={() => onBarClick && onBarClick(it)}
                  title={onBarClick ? "Tıklayınca seçenekteki katılımcıları göster" : ""}
              >
                {/* yüzde etiketi */}
                <div className="text-xs text-slate-600 mb-1 h-5">{it.pct}%</div>
                {/* sütun */}
                <div className="w-full h-40 bg-slate-200 rounded relative overflow-hidden">
                  <div
                      className="absolute bottom-0 left-0 right-0 bg-blue-600 transition-[height] duration-500 rounded-t"
                      style={{ height: `${it.h}%` }}
                      aria-label={`${it.label}: ${it.pct}% (${it.count})`}
                  />
                </div>
                {/* etiket & sayı */}
                <div className="mt-2 text-xs text-center text-slate-700 truncate w-full" title={it.label}>
                  {it.label || <em>(boş)</em>}
                </div>
                <div className="text-[11px] text-slate-500">{it.count}</div>
              </button>
          ))}
        </div>
      </div>
  );
};

function normalizeResultsPayload(raw) {
  let survey, aggregates;

  if (raw && raw.survey) {
    // Şekil A: { survey, aggregates }
    survey = raw.survey;
    aggregates = (raw.aggregates || []).map(a => ({
      ...a,
      choiceCounts: Array.isArray(a.choiceCounts)
          ? a.choiceCounts
          : a.choiceCounts && typeof a.choiceCounts === "object"
              ? Object.entries(a.choiceCounts).map(([answer, count]) => ({
                answer,
                count: Number(count) || 0,
              }))
              : [],
      choiceVoters:
          a.choiceVoters && typeof a.choiceVoters === "object" ? a.choiceVoters : undefined,
    }));
  } else {
    // Şekil B: doğrudan survey
    survey = raw || {};
    aggregates = (survey.questions || [])
        .filter(q => (q.type || "").toLowerCase() === "choice")
        .map(q => {
          let choiceCounts = [];
          if (Array.isArray(q.counts)) {
            choiceCounts = q.counts.map(c => ({
              answer: c.answer,
              count: Number(c.count) || 0,
            }));
          } else if (q.counts && typeof q.counts === "object") {
            choiceCounts = Object.entries(q.counts).map(([answer, count]) => ({
              answer,
              count: Number(count) || 0,
            }));
          }
          return {
            questionId: q.id,
            questionText: q.questionText,
            choiceCounts,
            choiceVoters:
                q.choiceVoters && typeof q.choiceVoters === "object" ? q.choiceVoters : undefined,
            multiple: !!q.multiple,
          };
        });
  }

  const getTextsForQuestion = (qid) => {
    const q = (survey.questions || []).find(x => x.id === qid);
    return Array.isArray(q?.texts) ? q.texts : [];
  };

  return { survey, aggregates, getTextsForQuestion };
}

/* -------------------- Ana Bileşen -------------------- */
export default function SurveyAdmin() {
  const [active, setActive] = useState(null); // null | 'create' | 'results'
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);
  const [success, setSuccess] = useState(null);

  // Create form state
  const [title, setTitle]             = useState("");
  const [description, setDescription] = useState("");
  const [anonymous, setAnonymous]     = useState(false);// datetime-local (zorunlu)
  const [questions, setQuestions]     = useState([emptyQuestion()]);
  const [deadline, setDeadline]       = useState("");
  const [hideAfter, setHideAfter]     = useState("");

  // Results state
  const [surveys, setSurveys] = useState([]);
  const [resultsOpen, setResultsOpen] = useState(false);
  const [resultsLoading, setResultsLoading] = useState(false);
  const [resultsError, setResultsError] = useState(null);
  const [resultsData, setResultsData] = useState(null); // normalized
  const [selectedSurvey, setSelectedSurvey] = useState(null);

  // Voter drill-down
  const [voterOpen, setVoterOpen] = useState(false);
  const [voterLoading, setVoterLoading] = useState(false);
  const [voterError, setVoterError] = useState(null);
  const [voterList, setVoterList] = useState([]); // string[] (emails)
  const [voterMeta, setVoterMeta] = useState({ questionText: "", answerLabel: "" });

  /* ---------- Create helpers ---------- */
  const resetForm = () => {
    setTitle("");
    setDescription("");
    setAnonymous(false);
    setDeadline("");
    setHideAfter("");
    setQuestions([emptyQuestion()]);
  };
  const addQuestion       = () => setQuestions(p => [...p, emptyQuestion()]);
  const removeQuestion    = (idx) => setQuestions(p => p.filter((_, i) => i !== idx));
  const updateQuestion = (idx, next) =>
      setQuestions(p => p.map((q, i) => (i === idx ? { ...q, ...next } : q)));
  const changeQuestionFld = (idx, field, value) =>
      setQuestions(p => p.map((q, i) => (i === idx ? { ...q, [field]: value } : q)));
  const addOption         = (qIdx) => setQuestions(p =>
      p.map((q, i) => (i === qIdx ? { ...q, options: [...q.options, ""] } : q)));
  const changeOption      = (qIdx, optIdx, value) =>
      setQuestions(p => p.map((q, i) => {
        if (i !== qIdx) return q;
        const next = [...q.options];
        next[optIdx] = value;
        return { ...q, options: next };
      }));
  const removeOption      = (qIdx, optIdx) =>
      setQuestions(p => p.map((q, i) => {
        if (i !== qIdx) return q;
        const next = [...q.options];
        next.splice(optIdx, 1);
        return { ...q, options: next };
      }));

  const validate = () => {
    if (!title.trim()) return "Anket başlığı zorunludur.";
    if (!deadline) return "Son tarih zorunludur.";
    if (questions.length === 0) return "En az bir soru ekleyin.";
    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.questionText.trim()) return `#${i + 1} soru metni zorunlu.`;
      if (!["text", "choice"].includes(q.type)) return `#${i + 1} geçersiz tür.`;
      if (q.type === "choice") {
        const opts = q.options.filter(o => o.trim() !== "");
        if (opts.length < 2) return `#${i + 1} için en az 2 seçenek girin.`;
      }
    }
    return null;
  };

  /* ---------- Actions: Create ---------- */
  const saveSurvey = async () => {
    setError(null);
    setSuccess(null);
    const v = validate();
    if (v) { setError(v); return; }

    setLoading(true);
    try {
      const payload = {
        title: title.trim(),
        description: description.trim() || null,
        anonymous, // zorunlu
        deadline: deadline ? toLocalLdt(deadline) : null,
        hideAfter: toLocalLdt(hideAfter),
        questions: questions.map(q => ({
          questionText: q.questionText.trim(),
          type: q.type,
          options: q.type === "choice" ? q.options.filter(o => o.trim()) : [],
          multiple: q.type === "choice" ? !!q.multiple : false,
        })),
      };
      const { data } = await api.post("/api/surveys", payload);
      setSuccess("Anket başarıyla oluşturuldu.");
      resetForm();
      if (active === "results") {
        setSurveys(prev => [{ ...data, questions: data.questions || [] }, ...prev]);
      }
    } catch {
      setError("Anket kaydedilemedi.");
    } finally {
      setLoading(false);
    }
  };

  /* ---------- Actions: Results ---------- */
  const fetchSurveys = async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get("/api/surveys");
      setSurveys(data || []);
    } catch {
      setError("Anketler yüklenemedi.");
    } finally {
      setLoading(false);
    }
  };

  const removeSurvey = async (id) => {
    if (!window.confirm("Bu anketi silmek istiyor musunuz?")) return;
    try {
      await api.delete(`/api/surveys/${id}`);
      setSurveys(p => p.filter(s => s.id !== id));
    } catch {
      alert("Silme başarısız!");
    }
  };

  const openResults = async (survey) => {
    setSelectedSurvey(survey);
    setResultsOpen(true);
    setResultsLoading(true);
    setResultsError(null);
    setResultsData(null);

    try {
      const { data } = await api.get(`/api/surveys/${survey.id}/results`);
      setResultsData(normalizeResultsPayload(data));
    } catch {
      setResultsError("Sonuçlar alınamadı.");
    } finally {
      setResultsLoading(false);
    }
  };

  // Drill-down: bir bar’a tıklanınca
  const openVoters = async ({ questionId, questionText }, answerLabel) => {
    if (!resultsData) return;
    setVoterOpen(true);
    setVoterLoading(true);
    setVoterError(null);
    setVoterList([]);
    setVoterMeta({ questionText, answerLabel });

    // anonim ise direkt uyar
    if (resultsData.survey.anonymous) {
      setVoterError("Anonim anketlerde katılımcı listesi gösterilmez.");
      setVoterLoading(false);
      return;
    }

    // 1) Payload içinde choiceVoters varsa doğrudan kullan
    const agg = (resultsData.aggregates || []).find(a => a.questionId === questionId);
    const fromPayload =
        agg && agg.choiceVoters && Array.isArray(agg.choiceVoters[answerLabel])
            ? agg.choiceVoters[answerLabel]
            : null;

    if (fromPayload) {
      setVoterList(fromPayload);
      setVoterLoading(false);
      return;
    }

    // 2) Fallback: backend uç noktası (opsiyonel) /results/{qid}/voters?answer=
    try {
      const { data } = await api.get(
          `/api/surveys/${resultsData.survey.id}/results/${questionId}/voters`,
          { params: { answer: answerLabel } }
      );
      // beklenen: { voters: ["a@x", "b@y"] }
      const list = Array.isArray(data?.voters) ? data.voters : [];
      setVoterList(list);
    } catch {
      setVoterError("Katılımcı listesi alınamadı.");
    } finally {
      setVoterLoading(false);
    }
  };

  useEffect(() => {
    if (active === "results") fetchSurveys();
  }, [active]);

  /* -------------------- UI -------------------- */
  return (
      <div className="mx-auto max-w-5xl p-6">
        {/* Başlangıç ekranı */}
        {!active && (
            <div className="grid gap-6 place-items-center mt-12">
              <h1 className="text-2xl font-bold text-slate-800">Anket Yönetimi</h1>
              <div className="flex flex-col sm:flex-row gap-4">
                <button
                    onClick={() => setActive("create")}
                    className="rounded-2xl px-6 py-4 text-lg font-semibold bg-blue-600 text-white hover:bg-blue-700 shadow"
                >
                  + Anket Ekle
                </button>
                <button
                    onClick={() => setActive("results")}
                    className="rounded-2xl px-6 py-4 text-lg font-semibold bg-slate-800 text-white hover:bg-slate-900 shadow"
                >
                  Anket Sonuçlarına Bak
                </button>
              </div>
            </div>
        )}

        {/* Sekmeler */}
        {active && (
            <div className="mb-6 flex items-center justify-between">
              <div className="flex gap-2">
                <button
                    onClick={() => setActive("create")}
                    className={`px-4 py-2 rounded-lg border ${
                        active === "create" ? "bg-blue-600 text-white border-blue-600" : "bg-white hover:bg-slate-50"
                    }`}
                >
                  Anket Ekle
                </button>
                <button
                    onClick={() => setActive("results")}
                    className={`px-4 py-2 rounded-lg border ${
                        active === "results" ? "bg-slate-800 text-white border-slate-800" : "bg-white hover:bg-slate-50"
                    }`}
                >
                  Anket Sonuçları
                </button>
              </div>
              <button
                  onClick={() => setActive(null)}
                  className="text-slate-600 hover:text-slate-900"
                  title="Anasayfaya dön"
              >
                Kapat
              </button>
            </div>
        )}

        {/* Hata / Başarı */}
        {active && error && (
            <div className="mb-4 rounded-lg border border-red-300 bg-red-50 p-3 text-red-700">
              {error}
            </div>
        )}
        {active && success && (
            <div className="mb-4 rounded-lg border border-emerald-300 bg-emerald-50 p-3 text-emerald-800">
              {success}
            </div>
        )}

        {/* CREATE TAB */}
        {active === "create" && (
            <div className="bg-white rounded-2xl shadow p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Yeni Anket Oluştur</h2>

              <div className="grid gap-4">
                <input
                    className="border rounded-lg p-2"
                    placeholder="Anket Başlığı *"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                />

                <textarea
                    className="border rounded-lg p-2"
                    placeholder="Açıklama (opsiyonel)"
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                />

                <div className="grid sm:grid-cols-2 gap-4">
                  <label className="flex items-center gap-2 border rounded-lg p-3">
                    <input
                        type="checkbox"
                        checked={anonymous}
                        onChange={(e) => setAnonymous(e.target.checked)}
                    />
                    <span>Anonim anket (cevaplarda mail saklanmaz)</span>
                  </label>
                  <div className="grid sm:grid-cols-3 gap-4">
                    <div className="border rounded-lg p-3">
                      <label className="block text-sm text-slate-600 mb-1">Son Tarih (opsiyonel)</label>
                      <input
                          type="datetime-local"
                          className="border rounded-lg p-2 w-full"
                          value={deadline}
                          onChange={(e) => setDeadline(e.target.value)}
                      />
                    </div>

                    <div className="border rounded-lg p-3">
                      <label className="block text-sm text-slate-600 mb-1">Kaybolma Tarihi (opsiyonel)</label>
                      <input
                          type="datetime-local"
                          className="border rounded-lg p-2 w-full"
                          value={hideAfter}
                          onChange={(e) => setHideAfter(e.target.value)}
                      />
                      <p className="text-xs text-slate-500 mt-1">
                        Bu tarih geçince anket kullanıcı listesinde görünmez.
                      </p>
                    </div>
                  </div>
                </div>

                {/* Sorular */}
                <div className="space-y-4">
                  {questions.map((q, idx) => (
                      <div key={idx} className="rounded-xl border p-4 bg-slate-50/60 space-y-3">
                        <div className="flex items-center justify-between">
                          <h4 className="font-medium text-slate-700">Soru #{idx + 1}</h4>
                          <button
                              type="button"
                              className="text-sm text-red-600 hover:underline"
                              onClick={() => removeQuestion(idx)}
                              disabled={questions.length === 1}
                          >
                            Kaldır
                          </button>
                        </div>

                        <input
                            className="border rounded-lg p-2 w-full"
                            placeholder="Soru metni *"
                            value={q.questionText}
                            onChange={(e) => changeQuestionFld(idx, "questionText", e.target.value)}
                        />

                        <div className="flex gap-3">
                          <select
                              className="border rounded-lg p-2"
                              value={q.type}
                              onChange={(e) => {
                                const nextType = e.target.value;
                                if (nextType === "choice") {
                                  // enable options + keep multiple (default false if unset)
                                  updateQuestion(idx, {
                                    type: "choice",
                                    multiple: q.multiple ?? false,
                                    options: Array.isArray(q.options) && q.options.length >= 2
                                        ? q.options
                                        : ["", ""],
                                  });
                                } else {
                                  // reset extras when switching back to text
                                  updateQuestion(idx, {
                                    type: "text",
                                    multiple: false,
                                    options: [],
                                  });
                                }
                              }}
                          >
                            <option value="text">Metin</option>
                            <option value="choice">Seçim (çoktan seçmeli)</option>
                          </select>
                          {q.type === "choice" && (
                              <label className="inline-flex items-center gap-2 ml-1">
                                <input
                                    type="checkbox"
                                    checked={!!q.multiple}
                                    onChange={(e) => updateQuestion(idx, { multiple: e.target.checked })}
                                />
                                <span>Birden fazla seçeneğin seçilmesine izin ver</span>
                              </label>
                          )}
                        </div>

                        {q.type === "choice" && (
                            <div className="space-y-2">
                              <div className="flex items-center justify-between">
                                <span className="text-sm text-slate-600">Seçenekler</span>
                                <button
                                    type="button"
                                    className="text-sm text-blue-600 hover:underline"
                                    onClick={() => addOption(idx)}
                                >
                                  + seçenek ekle
                                </button>
                              </div>
                              {(q.options || []).map((opt, oIdx) => (
                                  <div key={oIdx} className="flex gap-2">
                                    <input
                                        className="border rounded-lg p-2 w-full"
                                        placeholder={`Seçenek #${oIdx + 1}`}
                                        value={opt}
                                        onChange={(e) => changeOption(idx, oIdx, e.target.value)}
                                    />
                                    <button
                                        type="button"
                                        className="text-red-600 px-2"
                                        onClick={() => removeOption(idx, oIdx)}
                                        aria-label="seçenek sil"
                                    >
                                      ✕
                                    </button>
                                  </div>
                              ))}
                            </div>
                        )}
                      </div>
                  ))}

                  <div>
                    <button
                        type="button"
                        onClick={addQuestion}
                        className="rounded-lg px-3 py-2 border bg-white hover:bg-slate-50"
                    >
                      + Soru ekle
                    </button>
                  </div>
                </div>

                <div className="pt-2">
                  <button
                      onClick={saveSurvey}
                      disabled={loading}
                      className={`rounded-lg px-4 py-2 font-semibold ${
                          loading
                              ? "bg-slate-300 text-slate-600 cursor-not-allowed"
                              : "bg-blue-600 text-white hover:bg-blue-700"
                      }`}
                  >
                    {loading ? "Kaydediliyor..." : "Anketi Oluştur"}
                  </button>
                </div>
              </div>
            </div>
        )}

        {/* RESULTS TAB */}
        {active === "results" && (
            <div className="bg-white rounded-2xl shadow p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Anket Sonuçları</h2>

              {loading ? (
                  <p>Yükleniyor...</p>
              ) : surveys.length === 0 ? (
                  <p className="text-slate-500">Henüz anket yok.</p>
              ) : (
                  <ul className="space-y-2">
                    {surveys.map((s) => {
                      const expired = s.deadline ? new Date(s.deadline) < new Date() : false;
                      return (
                          <li key={s.id} className="flex items-center justify-between border rounded-lg p-3">
                            <div className="min-w-0">
                              <div className="font-medium flex items-center gap-2">
                                <span className="truncate">{s.title}</span>
                                {s.anonymous && <Badge color="blue">Anonim</Badge>}
                                {s.deadline && (
                                    <Badge color={expired ? "red" : "amber"}>
                                      {expired ? "Süre doldu" : "Son gün: " + new Date(s.deadline).toLocaleString()}
                                    </Badge>
                                )}
                                {s.createdAt && (
                                    <Badge color="slate">Oluşturuldu: {new Date(s.createdAt).toLocaleString()}</Badge>
                                )}
                              </div>
                              {s.description && (
                                  <div className="text-sm text-slate-500 truncate">{s.description}</div>
                              )}
                              <div className="text-xs text-slate-500 mt-1">
                                {s.questions?.length || 0} soru
                              </div>
                            </div>
                            <div className="flex items-center gap-2">
                              <button
                                  type="button"
                                  className="px-3 py-1 rounded border hover:bg-slate-50"
                                  onClick={() => openResults(s)}
                              >
                                Detay
                              </button>
                              <button
                                  type="button"
                                  onClick={() => removeSurvey(s.id)}
                                  className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600"
                              >
                                Sil
                              </button>
                            </div>
                          </li>
                      );
                    })}
                  </ul>
              )}
            </div>
        )}

        {/* RESULTS MODAL */}
        {resultsOpen && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
              <div className="w-full max-w-3xl bg-white rounded-2xl shadow-xl p-6 relative">
                <button
                    onClick={() => { setResultsOpen(false); setResultsData(null); setSelectedSurvey(null); }}
                    className="absolute top-3 right-3 text-slate-500 hover:text-slate-800"
                    aria-label="Kapat"
                    title="Kapat"
                >
                  ✕
                </button>

                <h3 className="text-lg font-semibold mb-2">
                  {selectedSurvey?.title || "Anket Sonuçları"}
                </h3>
                <div className="flex flex-wrap gap-2 mb-2">
                  {selectedSurvey?.deadline && (
                      <Badge color={new Date(selectedSurvey.deadline) < new Date() ? "red" : "amber"}>
                        Son gün: {new Date(selectedSurvey.deadline).toLocaleString()}
                      </Badge>
                  )}
                  {selectedSurvey?.createdAt && (
                      <Badge>Oluşturuldu: {new Date(selectedSurvey.createdAt).toLocaleString()}</Badge>
                  )}
                  {selectedSurvey?.anonymous && <Badge color="blue">Anonim</Badge>}
                </div>

                {resultsLoading && <p>Sonuçlar yükleniyor...</p>}
                {resultsError && (
                    <div className="rounded-lg border border-red-300 bg-red-50 text-red-700 p-3">
                      {resultsError}
                    </div>
                )}

                {!resultsLoading && resultsData && (
                    <div className="mt-3 grid gap-6">
                      {/* ÖZET: Choice soruları için dikey sütun grafiği */}
                      {resultsData.aggregates && resultsData.aggregates.length > 0 && (
                          <div>
                            <h4 className="font-semibold mb-2">Özet (Seçim Soruları)</h4>
                            <div className="space-y-6">
                              {resultsData.aggregates.map((qAgg) => {
                                const rows = (qAgg.choiceCounts || []).map(r => ({
                                  label: r.answer,
                                  count: r.count || 0,
                                }));
                                const total = rows.reduce((a, r) => a + r.count, 0);

                                return (
                                    <div key={qAgg.questionId} className="border rounded-lg p-4 bg-white">
                                      <div className="font-medium mb-3">{qAgg.questionText}</div>
                                      {total === 0 ? (
                                          <div className="text-sm text-slate-500">Henüz yanıt yok.</div>
                                      ) : (
                                          <ColumnChart
                                              data={rows}
                                              onBarClick={(bar) => openVoters(
                                                  { questionId: qAgg.questionId, questionText: qAgg.questionText },
                                                  bar.label
                                              )}
                                          />
                                      )}
                                    </div>
                                );
                              })}
                            </div>
                          </div>
                      )}

                      {/* TEXT sorular: survey.questions[].texts'ten göster */}
                      <div>
                        <h4 className="font-semibold mb-2">Metin Yanıtları</h4>
                        <div className="space-y-4">
                          {(resultsData.survey.questions || [])
                              .filter(q => (q.type || "").toLowerCase() === "text")
                              .map((q) => {
                                const texts = resultsData.getTextsForQuestion(q.id);
                                return (
                                    <div key={q.id} className="border rounded-lg p-3">
                                      <div className="font-medium mb-2">{q.questionText}</div>
                                      {texts.length === 0 ? (
                                          <div className="text-sm text-slate-500">Henüz yanıt yok.</div>
                                      ) : (
                                          <ul className="space-y-2">
                                            {texts.map((t, i) => (
                                                <li key={i} className="border rounded p-2">
                                                  <div className="text-slate-800">{t.answer}</div>
                                                  <div className="text-xs text-slate-500 mt-1">
                                                    {resultsData.survey.anonymous ? "Anonim" : (t.userEmail || "—")}
                                                  </div>
                                                </li>
                                            ))}
                                          </ul>
                                      )}
                                    </div>
                                );
                              })}
                        </div>
                      </div>
                    </div>
                )}
              </div>
            </div>
        )}

        {/* VOTERS (Drill-down) MODAL */}
        {voterOpen && (
            <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 p-4">
              <div className="w-full max-w-lg bg-white rounded-2xl shadow-xl p-6 relative">
                <button
                    onClick={() => { setVoterOpen(false); setVoterList([]); setVoterError(null); }}
                    className="absolute top-3 right-3 text-slate-500 hover:text-slate-800"
                    aria-label="Kapat"
                    title="Kapat"
                >
                  ✕
                </button>

                <h4 className="font-semibold mb-1">Seçenek Katılımcıları</h4>
                <div className="text-sm text-slate-600 mb-4">
                  <div className="truncate"><span className="font-medium">Soru:</span> {voterMeta.questionText}</div>
                  <div className="truncate"><span className="font-medium">Seçenek:</span> {voterMeta.answerLabel}</div>
                </div>

                {voterLoading && <p>Yükleniyor...</p>}
                {voterError && (
                    <div className="rounded-lg border border-red-300 bg-red-50 text-red-700 p-3 mb-2">
                      {voterError}
                    </div>
                )}

                {!voterLoading && !voterError && (
                    voterList.length === 0 ? (
                        <div className="text-sm text-slate-500">Kayıt bulunamadı.</div>
                    ) : (
                        <ul className="divide-y">
                          {voterList.map((email, idx) => (
                              <li key={idx} className="py-2 text-slate-800">{email}</li>
                          ))}
                        </ul>
                    )
                )}
              </div>
            </div>
        )}
      </div>
  );
}