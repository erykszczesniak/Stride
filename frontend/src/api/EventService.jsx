import apiClient from "./ApiClient";

class EventService {
  // Get all events
  getAllEvents(page = 0, size = 10) {
    return apiClient.get(`/events?page=${page}&size=${size}`);
  }

  // Get event by ID
  getEventById(eventId, userId = 1) {
    return apiClient.get(`/events/${eventId}?userId=${userId}`);
  }

  // Get upcoming events
  getUpcomingEvents(page = 0, size = 5) {
    return apiClient.get(`/events/upcoming?page=${page}&size=${size}`);
  }

  // Get user's events
  getUserEvents(userId = 1, page = 0, size = 10) {
    return apiClient.get(`/events/user?userId=${userId}&page=${page}&size=${size}`);
  }

  // Create event
  createEvent(userId = 1, eventData) {
    return apiClient.post(`/events?userId=${userId}`, eventData);
  }

  // Update event
  updateEvent(eventId, userId = 1, eventData) {
    return apiClient.put(`/events/${eventId}?userId=${userId}`, eventData);
  }

  // Delete event
  deleteEvent(eventId, userId = 1) {
    return apiClient.delete(`/events/${eventId}?userId=${userId}`);
  }

  // Register for event
  registerForEvent(eventId, userId = 1) {
    return apiClient.post(`/events/${eventId}/register?userId=${userId}`);
  }

  // Unregister from event
  unregisterFromEvent(eventId, userId = 1) {
    return apiClient.delete(`/events/${eventId}/register?userId=${userId}`);
  }
}

export default new EventService();
