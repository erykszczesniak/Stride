import apiClient from "./ApiClient";

class ImportService {
  importActivity(file, options = {}) {
    const formData = new FormData();
    formData.append('file', file);
    if (options.title) formData.append('title', options.title);
    if (options.activityType) formData.append('activityType', options.activityType);
    if (options.isPrivate !== undefined) formData.append('isPrivate', options.isPrivate);
    return apiClient.post(`/import/activities`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  }

  listSampleFiles() {
    return apiClient.get(`/import/samples`);
  }

  downloadSampleFile(filename) {
    return apiClient.get(`/import/samples/${filename}`, {
      responseType: 'blob'
    });
  }
}

const importService = new ImportService();
export default importService;
