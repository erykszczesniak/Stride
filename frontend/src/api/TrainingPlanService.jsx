import api from "./axios";

const TrainingPlanService = {
  getTemplates() {
    return api.get("/training-plans/templates");
  },

  getTemplateById(id) {
    return api.get(`/training-plans/templates/${id}`);
  },
};

export default TrainingPlanService;
