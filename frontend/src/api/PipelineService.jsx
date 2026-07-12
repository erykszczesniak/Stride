import axios from "axios";

// The pipeline facade lives outside /api/v1 but accepts the same JWT
// (PipelineTokenFilter), so a thin bearer-attaching client is enough.
const pipelineClient = axios.create({
  baseURL: process.env.REACT_APP_PIPELINE_API_URL || "/pipeline/v1",
  timeout: 30000,
});

pipelineClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const PipelineService = {
  // { form: { date, ctl, atl, tsb }, todayWorkout, snapshots, recentActivities }
  getDashboard: async () => {
    const response = await pipelineClient.get("/dashboard");
    return response.data;
  },
};

export default PipelineService;
