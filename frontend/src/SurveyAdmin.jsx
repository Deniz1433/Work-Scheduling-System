// src/SurveyAdmin.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";

const api = axios.create({
  baseURL: "",           // aynı origin/proxy ise boş bırak
  withCredentials: true, // auth cookie/Keycloak için
});

const emptyQuestion = () => ({
  questionText: "",
  type: "text",  // "text" | "choice"
  options: [],
});

const toIso = (v) => {
  // <input type="datetime-local"> string -> ISO (backend LocalDateTime uygunsa düz string de olur)
  if (!v) return null;
  // 'YYYY-MM-DDTHH:mm' -> ISO
  const dt = new Date(v);
  // LocalDateTime bekliyorsan timezone ekleme; ISO istiyorsan:
  return dt.toISOString().slice(0, 19); // 'YYYY-MM-DDTHH:mm:ss'
};

export default function SurveyAdmin() {
  const [active, setActive] = useState(null); // null | 'create' | 'results'
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);
  const [success, setSuccess] = useState(null);

  // create form
  const [title, setTitle]             = useState("");
  const [description, setDescription] = useState("");
  const [anonymous, setAnonymous]     = useState(false);
  const [deadline, setDeadline]       = useState(""); // datetime-local value
  const [questions, setQuestions]     = useState([emptyQuestion()]);

  // results (şimdilik boş / opsiyonel liste)
  const [surveys, setSurveys] = useState([]);

  // ---- helpers ----
  const resetForm = () => {
    setTitle("");
    setDescription("");
    setAnonymous(false);
    setDeadline("");
    setQuestions([emptyQuestion()]);
  };

  const addQuestion = () => setQuestions((p) => [...p, emptyQuestion()]);
  const removeQuestion = (idx) =>
      setQuestions((p) => p.filter((_, i) => i !== idx));
  const changeQuestionField = (idx, field, value) =>
      setQuestions((p) => p.map((q, i) => (i === idx ? { ...q, [field]: value } : q)));
  const addOption = (qIdx) =>
      setQuestions((p) =>
          p.map((q, i) => (i === qIdx ? { ...q, options: [...q.options, ""] } : q))
      );
  const changeOption = (qIdx, optIdx, value) =>
      setQuestions((p) =>
          p.map((q, i) => {
            if (i !== qIdx) return q;
            const next = [...q.options];
            next[optIdx] = value;
            return { ...q, options: next };
          })
      );
  const removeOption = (qIdx, optIdx) =>
      setQuestions((p) =>
          p.map((q, i) => {
            if (i !== qIdx) return q;
            const next = [...q.options];
            next.splice(optIdx, 1);
            return { ...q, options: next };
          })
      );

  const validate = () => {
    if (!title.trim()) return "Anket başlığı zorunludur.";
    if (questions.length === 0) return "En az bir soru ekleyin.";
    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.questionText.trim()) return `#${i + 1} soru metni zorunlu.`;
      if (!["text", "choice"].includes(q.type)) return `#${i + 1} geçersiz tür.`;
      if (q.type === "choice") {
        const opts = q.options.filter((o) => o.trim() !== "");
        if (opts.length < 2) return `#${i + 1} için en az 2 seçenek girin.`;
      }
    }
    return null;
  };

  // ---- actions ----
  const saveSurvey = async () => {
    setError(null);
    setSuccess(null);
    const v = validate();
    if (v) {
      setError(v);
      return;
    }
    setLoading(true);
    try {
      const payload = {
        title: title.trim(),
        description: description.trim() || null,
        anonymous,
        deadline: deadline ? toIso(deadline) : null,
        questions: questions.map((q) => ({
          questionText: q.questionText.trim(),
          type: q.type,
          options: q.type === "choice" ? q.options.filter((o) => o.trim()) : [],
        })),
      };
      await api.post("/api/surveys", payload);
      setSuccess("Anket başarıyla oluşturuldu.");
      resetForm();
      if (active === "results") await fetchSurveys(); // istersen listeyi tazele
    } catch (e) {
      setError("Anket kaydedilemedi.");
    } finally {
      setLoading(false);
    }
  };

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
      setSurveys((p) => p.filter((s) => s.id !== id));
    } catch {
      alert("Silme başarısız!");
    }
  };

  // tab değişince gerekliyse yükle
  useEffect(() => {
    if (active === "results") fetchSurveys();
  }, [active]);

  // ---- UI ----
  return (
      <div className="mx-auto max-w-5xl p-6">
        {/* Başlangıç boş ekran + iki büyük buton */}
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

        {/* Üst bar (tab gibi) */}
        {active && (
            <div className="mb-6 flex items-center justify-between">
              <div className="flex gap-2">
                <button
                    onClick={() => setActive("create")}
                    className={`px-4 py-2 rounded-lg border ${active === "create" ? "bg-blue-600 text-white border-blue-600" : "bg-white hover:bg-slate-50"}`}
                >
                  Anket Ekle
                </button>
                <button
                    onClick={() => setActive("results")}
                    className={`px-4 py-2 rounded-lg border ${active === "results" ? "bg-slate-800 text-white border-slate-800" : "bg-white hover:bg-slate-50"}`}
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

        {/* Hata/başarı */}
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

                  <div className="border rounded-lg p-3">
                    <label className="block text-sm text-slate-600 mb-1">Son Tarih (opsiyonel)</label>
                    <input
                        type="datetime-local"
                        className="border rounded-lg p-2 w-full"
                        value={deadline}
                        onChange={(e) => setDeadline(e.target.value)}
                    />
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
                            onChange={(e) => changeQuestionField(idx, "questionText", e.target.value)}
                        />

                        <div className="flex gap-3">
                          <select
                              className="border rounded-lg p-2"
                              value={q.type}
                              onChange={(e) => changeQuestionField(idx, "type", e.target.value)}
                          >
                            <option value="text">Metin</option>
                            <option value="choice">Seçim (çoktan seçmeli)</option>
                          </select>
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

        {/* RESULTS TAB (şimdilik basit liste) */}
        {active === "results" && (
            <div className="bg-white rounded-2xl shadow p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Anket Sonuçları</h2>

              {loading ? (
                  <p>Yükleniyor...</p>
              ) : surveys.length === 0 ? (
                  <p className="text-slate-500">Henüz anket yok. Sonuç ekranını burada tasarlarız.</p>
              ) : (
                  <ul className="space-y-2">
                    {surveys.map((s) => (
                        <li key={s.id} className="flex items-center justify-between border rounded-lg p-3">
                          <div>
                            <div className="font-medium">{s.title}</div>
                            {s.description && (
                                <div className="text-sm text-slate-500">{s.description}</div>
                            )}
                            <div className="text-xs text-slate-500 mt-1">
                              {s.questions?.length || 0} soru
                              {s.deadline && <> • Son gün: {new Date(s.deadline).toLocaleString()}</>}
                              {s.anonymous && <> • Anonim</>}
                            </div>
                          </div>
                          <div className="flex items-center gap-2">
                            {/* Sonuç detay butonu (ileride grafik vs.) */}
                            <button
                                type="button"
                                className="px-3 py-1 rounded border hover:bg-slate-50"
                                onClick={() => alert("Sonuç detayları burada gösterilecek.")}
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
                    ))}
                  </ul>
              )}
            </div>
        )}
      </div>
  );
}