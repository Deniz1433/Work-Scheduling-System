import React, { useEffect, useMemo, useState } from "react";
import {
  getLatestSurvey,
  submitSurveyAnswers,getAllSurveys
} from "./Surveys";
import axios from "axios";


const SurveyTake = () => {
  const [survey, setSurvey] = useState(null);
  const [answers, setAnswers] = useState({});
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getLatestSurvey(); // axios response
      const data = response.data;               // JSON kısmı
      setSurvey(data);

      if (data?.questions?.length) {
        const init = {};
        data.questions.forEach((q) => (init[q.id] = ""));
        setAnswers(init);
      }
    } catch (e) {
      setError("Anket yüklenemedi.");
    } finally {
      setLoading(false);
    }
  };



  useEffect(() => {
    load();
  }, []);

  const canSubmit = useMemo(() => {
    if (!survey?.questions) return false;
    return survey.questions.every((q) => {
      const val = answers[q.id];
      return typeof val === "string" ? val.trim() !== "" : val != null;
    });
  }, [survey, answers]);

  const handleChange = (qId, value) => {
    setAnswers((prev) => ({ ...prev, [qId]: value }));
  };

  const submit = async () => {
    if (!survey) return;
    setSending(true);
    setError(null);
    try {
      await submitSurveyAnswers(survey.id, { answers });
      setDone(true);
    } catch (e) {
      setError("Cevaplar gönderilemedi.");
    } finally {
      setSending(false);
    }
  };

  if (loading) return <p>Anket yükleniyor...</p>;
  if (!survey) return <p>Görüntülenecek aktif anket bulunamadı.</p>;
  if (done) return <p>Teşekkürler! Cevaplarınız kaydedildi.</p>;

  return (
    <div className="max-w-3xl mx-auto p-8">
      {error && <div className="text-red-600 mb-4">{error}</div>}

      <h1 className="text-2xl font-bold mb-2">{survey.title}</h1>
      {survey.description && <p className="mb-6">{survey.description}</p>}

      <div className="space-y-6">
        {survey.questions?.map((q, idx) => (
          <div key={q.id} className="border rounded-xl p-4 bg-white">
            <div className="mb-2 font-medium">
              {idx + 1}. {q.questionText}
            </div>

            {q.type === "text" && (
              <textarea
                className="border rounded-lg p-2 w-full"
                placeholder="Yanıtınız..."
                rows={3}
                value={answers[q.id] || ""}
                onChange={(e) => handleChange(q.id, e.target.value)}
              />
            )}

            {q.type === "choice" && (
              <div className="grid gap-2">
                {(q.options || []).map((opt, oIdx) => (
                  <label key={oIdx} className="inline-flex items-center gap-2">
                    <input
                      type="radio"
                      name={`q-${q.id}`}
                      value={opt}
                      checked={answers[q.id] === opt}
                      onChange={(e) => handleChange(q.id, e.target.value)}
                    />
                    <span>{opt}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="pt-6">
        <button
          onClick={submit}
          disabled={!canSubmit || sending}
          className={`rounded-lg px-4 py-2 font-semibold ${
            !canSubmit || sending
              ? "bg-slate-300 text-slate-600 cursor-not-allowed"
              : "bg-green-600 text-white hover:bg-green-700"
          }`}
        >
          {sending ? "Gönderiliyor..." : "Gönder"}
        </button>
      </div>
    </div>
  );
};

export default SurveyTake;