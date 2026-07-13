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

  // { activity, paceSeries, heartRateSeries, splits, routePolyline, elevationSeries }
  // Pace units per contract: sec/km (run), km/h (bike), sec/100m (swim).
  getActivityDetail: async (activityId) => {
    const response = await pipelineClient.get(`/activities/act-${activityId}`);
    return response.data;
  },

  // { ctl, atl, weeklyVolumes: [{ weekStart, swimKm, bikeKm, runKm }], ... }
  getLoad: async (range = "3m") => {
    const response = await pipelineClient.get(`/metrics/load?range=${range}`);
    return response.data;
  },
};

export default PipelineService;
