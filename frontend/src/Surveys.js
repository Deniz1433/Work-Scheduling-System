import axios from "axios";

// Gerekirse burayı env ile yönet: import.meta.env.VITE_API_URL || ''
axios.create({
  baseURL: "", // aynı origin (proxy) kullanıyorsan boş bırak
  withCredentials: true, // Keycloak/sesyon cookie varsa işine yarar
});
/*import axios from "axios";

// Tek bir son aktif anketi al
export const getLatestSurvey = async () => {
  const { data } = await axios.get("/surveys/latest"); // artık tek anket dönüyor
  return data || null;
};

// Anket cevaplarını gönder
export const submitSurveyAnswers = async (surveyId, payload) => {
  return axios.post(`/surveys/${surveyId}/answers`, payload);
};*/