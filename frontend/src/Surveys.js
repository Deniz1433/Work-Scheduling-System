import axios from "axios";

// Gerekirse burayı env ile yönet: import.meta.env.VITE_API_URL || ''
const api = axios.create({
  baseURL: "", // aynı origin (proxy) kullanıyorsan boş bırak
  withCredentials: true, // Keycloak/sesyon cookie varsa işine yarar
});

export const getAllSurveys = () => api.get("/api/surveys");
export const createSurvey = (payload) => api.post("/api/surveys", payload);
export const getLatestSurvey = () => api.get("/api/surveys/latest");
export const deleteSurvey = (id) => axios.delete(`/api/surveys/${id}`    , { withCredentials: true });
export const submitSurveyAnswers = (surveyId, answers) =>
  api.post(`/api/surveys/${surveyId}/submit`, answers);

export default api;



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