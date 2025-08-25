import React, { useEffect, useState } from "react";
import { getAllSurveys, createSurvey ,deleteSurvey} from "./Surveys";
import axios from "axios"; // silme isteği için ekledik


const emptyQuestion = () => ({
  questionText: "",
  type: "text",   // "text" | "choice"
  options: [],    // type=choice ise doldurulur
});

const SurveyAdmin = () => {
  const [surveys, setSurveys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [questions, setQuestions] = useState([emptyQuestion()]);

  const fetchAll = async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await getAllSurveys();
      setSurveys(data || []);
    } catch (e) {
      setError("Anketler yüklenemedi.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const addQuestion = () => {
    setQuestions((prev) => [...prev, emptyQuestion()]);
  };

  const removeQuestion = (idx) => {
    setQuestions((prev) => prev.filter((_, i) => i !== idx));
  };

  const changeQuestionField = (idx, field, value) => {
    setQuestions((prev) =>
      prev.map((q, i) => (i === idx ? { ...q, [field]: value } : q))
    );
  };

  const addOption = (qIdx) => {
    setQuestions((prev) =>
      prev.map((q, i) =>
        i === qIdx ? { ...q, options: [...(q.options || []), ""] } : q
      )
    );
  };

  const changeOption = (qIdx, optIdx, value) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== qIdx) return q;
        const next = [...(q.options || [])];
        next[optIdx] = value;
        return { ...q, options: next };
      })
    );
  };

  const removeOption = (qIdx, optIdx) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== qIdx) return q;
        const next = [...(q.options || [])];
        next.splice(optIdx, 1);
        return { ...q, options: next };
      })
    );
  };

  const validatePayload = () => {
    if (!title.trim()) return "Anket başlığı zorunludur.";
    if (questions.length === 0) return "En az bir soru ekleyin.";
    for (const [i, q] of questions.entries()) {
      if (!q.questionText.trim()) return `#${i + 1} soru metni zorunlu.`;
      if (!["text", "choice"].includes(q.type)) return `#${i + 1} geçersiz tür.`;
      if (q.type === "choice") {
        const opts = (q.options || []).filter((o) => o.trim() !== "");
        if (opts.length < 2) return `#${i + 1} için en az 2 seçenek girin.`;
      }
    }
    return null;
  };

  const saveSurvey = async () => {
    setError(null);
    setSuccess(null);
    const validation = validatePayload();
    if (validation) {
      setError(validation);
      return;
    }
    setSaving(true);
    try {
      const payload = {
        title,
        description,
        questions: questions.map((q) => ({
          questionText: q.questionText.trim(),
          type: q.type,
          options: q.type === "choice" ? q.options.filter((o) => o.trim()) : [],
        })),
      };
      await createSurvey(payload);
      setSuccess("Anket başarıyla oluşturuldu.");
      setTitle("");
      setDescription("");
      setQuestions([emptyQuestion()]);
      fetchAll();
    } catch (e) {
      setError("Anket kaydedilemedi.");
    } finally {
      setSaving(false);
    }
    /*const deleteSurvey = async (id) => {
      if (!window.confirm("Bu anketi silmek istediğinize emin misiniz?")) return;
      try {
        await fetch(`/api/surveys/${id}`, {
          method: "DELETE",
          credentials: "include",
        });
        setSurveys((prev) => prev.filter((s) => s.id !== id));
      } catch (err) {
        console.error("Silme başarısız:", err);
        alert("Anket silinemedi.");
      }
    };*/

     const deleteSurvey = async (id) => {
         if (!window.confirm("Bu anketi silmek istediğinize emin misiniz?")) return;
         try {
           await axios.delete(`/api/surveys/${id}`);
           setSurveys((prev) => prev.filter((s) => s.id !== id));
         } catch (err) {
           console.error("Silme başarısız:", err);
           alert("Silme başarısız!");
         }
     };



  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Anket Yönetimi</h1>

      {error && (
        <div className="mb-4 rounded-lg border border-red-300 bg-red-50 p-3 text-red-700">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-4 rounded-lg border border-green-300 bg-green-50 p-3 text-green-700">
          {success}
        </div>
      )}

      {/* Yeni Anket Oluştur */}
      <div className="bg-white rounded-2xl shadow p-6 mb-8">
        <h2 className="text-lg font-semibold text-slate-700 mb-4">
          Yeni Anket Oluştur
        </h2>

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
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
          />

          {/* Sorular */}
          <div className="space-y-4">
            {questions.map((q, idx) => (
              <div
                key={idx}
                className="rounded-xl border p-4 bg-slate-50/60 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <h4 className="font-medium text-slate-700">
                    Soru #{idx + 1}
                  </h4>
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
                  onChange={(e) =>
                    changeQuestionField(idx, "questionText", e.target.value)
                  }
                />

                <div className="flex gap-3">
                  <select
                    className="border rounded-lg p-2"
                    value={q.type}
                    onChange={(e) =>
                      changeQuestionField(idx, "type", e.target.value)
                    }
                  >
                    <option value="text">Metin</option>
                    <option value="choice">Seçim (çoktan seçmeli)</option>
                  </select>
                </div>

                {q.type === "choice" && (
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-slate-600">
                        Seçenekler
                      </span>
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
                          onChange={(e) =>
                            changeOption(idx, oIdx, e.target.value)
                          }
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
              disabled={saving}
              className={`rounded-lg px-4 py-2 font-semibold ${
                saving
                  ? "bg-slate-300 text-slate-600 cursor-not-allowed"
                  : "bg-blue-600 text-white hover:bg-blue-700"
              }`}
            >
              {saving ? "Kaydediliyor..." : "Anketi Oluştur"}
            </button>
          </div>
        </div>
      </div>

      {/* Mevcut Anketler */}
      <div className="bg-white rounded-2xl shadow p-6">
        <h2 className="text-lg font-semibold text-slate-700 mb-4">
          Mevcut Anketler
        </h2>
        {loading ? (
          <p>Yükleniyor...</p>
        ) : surveys.length === 0 ? (
          <p className="text-slate-500">Henüz anket yok.</p>
        ) : (
          <ul className="space-y-2">
            {surveys.map((s) => (
              <li
                key={s.id}
                className="flex items-center justify-between border rounded-lg p-3"
              >
                <div>
                  <div className="font-medium">{s.title}</div>
                  {s.description && (
                    <div className="text-sm text-slate-500">
                      {s.description}
                    </div>
                  )}
                  <div className="text-xs text-slate-500 mt-1">
                    {s.questions?.length || 0} soru
                  </div>
                </div>
                {/* burada talebe göre 'detay', 'sil' butonları eklenebilir */

                    <button
                      type="button"
                      onClick={() => deleteSurvey(s.id)}
                      className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600"
                    >
                      Sil
                    </button>
                }
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
};

export default SurveyAdmin;