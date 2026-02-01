import apiClient from "./ApiClient";

class EventService {
  getAllEvents(page = 0, size = 10) {
    return apiClient.get(`/events?page=${page}&size=${size}`);
  }

  getEventById(eventId) {
    return apiClient.get(`/events/${eventId}`);
  }

  getUpcomingEvents(page = 0, size = 5) {
    return apiClient.get(`/events/upcoming?page=${page}&size=${size}`);
  }

  getMyEvents() {
    return apiClient.get(`/events/my`);
  }

  createEvent(eventData) {
    return apiClient.post(`/events`, eventData);
  }

  updateEvent(eventId, eventData) {
    return apiClient.put(`/events/${eventId}`, eventData);
  }

  deleteEvent(eventId) {
    return apiClient.delete(`/events/${eventId}`);
  }

  registerForEvent(eventId) {
    return apiClient.post(`/events/${eventId}/register`);
  }

  unregisterFromEvent(eventId) {
    return apiClient.delete(`/events/${eventId}/register`);
  }
}

export default new EventService();
