import apiClient from "./ApiClient";

const TrainingPlanService = {
  getTemplates() {
    return apiClient.get("/training-plans/templates");
  },

  getTemplateById(id) {
    return apiClient.get(`/training-plans/templates/${id}`);
  },
};

export default TrainingPlanService;
