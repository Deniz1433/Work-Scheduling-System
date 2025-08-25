import React, { useState, useEffect } from "react";
import axios from "axios";

const SurveyManagement = () => {
  const [surveys, setSurveys] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(false);

  const [newSurvey, setNewSurvey] = useState({ title: "", description: "" });
  const [newQuestion, setNewQuestion] = useState({ surveyId: "", text: "" });

  const API_BASE = "http://localhost:8080/api"; // backend endpoint adresin

  // Anketleri çek
  const fetchSurveys = async () => {
    setLoading(true);
    try {
      const res = await axios.get(`${API_BASE}/surveys`);
      setSurveys(res.data);
    } catch (err) {
      console.error("Anketler yüklenemedi", err);
    }
    setLoading(false);
  };

  // Soruları çek
  const fetchQuestions = async (surveyId) => {
    setLoading(true);
    try {
      const res = await axios.get(`${API_BASE}/surveys/${surveyId}/questions`);
      setQuestions(res.data);
    } catch (err) {
      console.error("Sorular yüklenemedi", err);
    }
    setLoading(false);
  };

  // Anket ekle
  const addSurvey = async () => {
    if (!newSurvey.title.trim()) return;
    try {
      await axios.post(`${API_BASE}/surveys`, newSurvey);
      setNewSurvey({ title: "", description: "" });
      fetchSurveys();
    } catch (err) {
      console.error("Anket eklenemedi", err);
    }
  };

  // Soru ekle
  const addQuestion = async () => {
    if (!newQuestion.text.trim() || !newQuestion.surveyId) return;
    try {
      await axios.post(
        `${API_BASE}/surveys/${newQuestion.surveyId}/questions`,
        { text: newQuestion.text }
      );
      setNewQuestion({ surveyId: "", text: "" });
      fetchQuestions(newQuestion.surveyId);
    } catch (err) {
      console.error("Soru eklenemedi", err);
    }
  };

  useEffect(() => {
    fetchSurveys();
  }, []);

  return (
    <div style={{ padding: "20px" }}>
      <h1>Anket Yönetimi</h1>

      {/* Yeni Anket Ekleme */}
      <div style={{ marginBottom: "20px" }}>
        <h3>Yeni Anket Ekle</h3>
        <input
          placeholder="Anket Başlığı"
          value={newSurvey.title}
          onChange={(e) => setNewSurvey({ ...newSurvey, title: e.target.value })}
        />
        <input
          placeholder="Açıklama"
          value={newSurvey.description}
          onChange={(e) =>
            setNewSurvey({ ...newSurvey, description: e.target.value })
          }
        />
        <button onClick={addSurvey}>Ekle</button>
      </div>

      {/* Anket Listesi */}
      <h3>Mevcut Anketler</h3>
      {loading && <p>Yükleniyor...</p>}
      <ul>
        {surveys.map((s) => (
          <li key={s.id}>
            <strong>{s.title}</strong> - {s.description}
            <button onClick={() => fetchQuestions(s.id)}>Soruları Gör</button>
          </li>
        ))}
      </ul>

      {/* Soru Ekleme */}
      {surveys.length > 0 && (
        <div style={{ marginTop: "20px" }}>
          <h3>Yeni Soru Ekle</h3>
          <select
            value={newQuestion.surveyId}
            onChange={(e) =>
              setNewQuestion({ ...newQuestion, surveyId: e.target.value })
            }
          >
            <option value="">Anket Seç</option>
            {surveys.map((s) => (
              <option key={s.id} value={s.id}>
                {s.title}
              </option>
            ))}
          </select>
          <input
            placeholder="Soru Metni"
            value={newQuestion.text}
            onChange={(e) =>
              setNewQuestion({ ...newQuestion, text: e.target.value })
            }
          />
          <button onClick={addQuestion}>Soru Ekle</button>
        </div>
      )}

      {/* Sorular Listesi */}
      {questions.length > 0 && (
        <div style={{ marginTop: "20px" }}>
          <h3>Seçilen Anketin Soruları</h3>
          <ul>
            {questions.map((q) => (
              <li key={q.id}>{q.text}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default SurveyManagement;