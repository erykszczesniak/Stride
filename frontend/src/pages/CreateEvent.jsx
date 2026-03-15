import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiCalendar, FiMapPin, FiUsers, FiDollarSign } from 'react-icons/fi';
import Layout from '../components/Layout/Layout';
import EventService from '../api/EventService';
import {
  PageHeader,
  BackButton,
  PageTitle,
  FormCard,
  SectionTitle,
  FormRow,
  FormGroup,
  Label,
  Input,
  TextArea,
  Select,
  ActivityTypeGrid,
  ActivityTypeButton,
  ActivityIcon,
  ActivityLabel,
  CheckboxGroup,
  Checkbox,
  ButtonGroup,
  CancelButton,
  SaveButton,
  ErrorMessage,
} from './CreateEventStyles';

const CreateEvent = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedType, setSelectedType] = useState('RUN');
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    eventDate: '',
    eventTime: '',
    location: '',
    city: '',
    country: '',
    distance: '',
    maxParticipants: '',
    entryFee: '',
    currency: 'USD',
    externalUrl: '',
    isPublic: true
  });

  const activityTypes = [
    { type: 'RUN', label: 'Run', icon: '🏃', color: '#fc5200' },
    { type: 'RIDE', label: 'Ride', icon: '🚴', color: '#0095ff' },
    { type: 'SWIM', label: 'Swim', icon: '🏊', color: '#00bcd4' },
    { type: 'HIKE', label: 'Hike', icon: '⛰️', color: '#4caf50' },
    { type: 'WALK', label: 'Walk', icon: '🚶', color: '#9c27b0' },
    { type: 'TRIATHLON', label: 'Triathlon', icon: '🏆', color: '#ff9800' }
  ];

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validation
    if (!formData.name.trim()) {
      setError('Please enter an event name');
      return;
    }

    if (!formData.eventDate || !formData.eventTime) {
      setError('Please enter event date and time');
      return;
    }

    if (!formData.location.trim()) {
      setError('Please enter event location');
      return;
    }

    setLoading(true);

    try {
      const eventDateTime = `${formData.eventDate}T${formData.eventTime}:00`;

      const eventData = {
        name: formData.name,
        description: formData.description || null,
        activityType: selectedType,
        eventDate: eventDateTime,
        location: formData.location,
        city: formData.city || null,
        country: formData.country || null,
        distance: formData.distance ? parseFloat(formData.distance) : null,
        maxParticipants: formData.maxParticipants ? parseInt(formData.maxParticipants) : null,
        entryFee: formData.entryFee ? parseFloat(formData.entryFee) : null,
        currency: formData.entryFee ? formData.currency : null,
        externalUrl: formData.externalUrl || null,
        isPublic: formData.isPublic
      };

      await EventService.createEvent(eventData);
      navigate('/events');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create event. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <PageHeader>
        <BackButton onClick={() => navigate('/events')}>
          <FiArrowLeft size={20} />
        </BackButton>
        <PageTitle>Create Event</PageTitle>
      </PageHeader>

      <form onSubmit={handleSubmit}>
        {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

        <FormCard>
          <SectionTitle>Event Type</SectionTitle>
          <ActivityTypeGrid>
            {activityTypes.map((activity) => (
              <ActivityTypeButton
                key={activity.type}
                type="button"
                selected={selectedType === activity.type}
                color={activity.color}
                onClick={() => setSelectedType(activity.type)}
              >
                <ActivityIcon>{activity.icon}</ActivityIcon>
                <ActivityLabel>{activity.label}</ActivityLabel>
              </ActivityTypeButton>
            ))}
          </ActivityTypeGrid>
        </FormCard>

        <FormCard>
          <SectionTitle>Basic Information</SectionTitle>
          <FormGroup>
            <Label>Event Name *</Label>
            <Input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              placeholder="e.g., City Marathon 2025"
            />
          </FormGroup>

          <FormGroup>
            <Label>Description</Label>
            <TextArea
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder="Describe your event, what participants can expect..."
            />
          </FormGroup>

          <FormRow>
            <FormGroup noMargin>
              <Label>Distance (km)</Label>
              <Input
                type="number"
                name="distance"
                value={formData.distance}
                onChange={handleInputChange}
                placeholder="e.g., 42.2"
                step="0.1"
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>External URL</Label>
              <Input
                type="url"
                name="externalUrl"
                value={formData.externalUrl}
                onChange={handleInputChange}
                placeholder="https://event-website.com"
              />
            </FormGroup>
          </FormRow>
        </FormCard>

        <FormCard>
          <SectionTitle><FiCalendar /> Date & Time</SectionTitle>
          <FormRow>
            <FormGroup noMargin>
              <Label>Event Date *</Label>
              <Input
                type="date"
                name="eventDate"
                value={formData.eventDate}
                onChange={handleInputChange}
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>Start Time *</Label>
              <Input
                type="time"
                name="eventTime"
                value={formData.eventTime}
                onChange={handleInputChange}
              />
            </FormGroup>
          </FormRow>
        </FormCard>

        <FormCard>
          <SectionTitle><FiMapPin /> Location</SectionTitle>
          <FormGroup>
            <Label>Venue / Address *</Label>
            <Input
              type="text"
              name="location"
              value={formData.location}
              onChange={handleInputChange}
              placeholder="e.g., Central Park, Main Entrance"
            />
          </FormGroup>

          <FormRow>
            <FormGroup noMargin>
              <Label>City</Label>
              <Input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleInputChange}
                placeholder="e.g., New York"
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>Country</Label>
              <Input
                type="text"
                name="country"
                value={formData.country}
                onChange={handleInputChange}
                placeholder="e.g., USA"
              />
            </FormGroup>
          </FormRow>
        </FormCard>

        <FormCard>
          <SectionTitle><FiUsers /> Registration</SectionTitle>
          <FormRow>
            <FormGroup noMargin>
              <Label>Max Participants</Label>
              <Input
                type="number"
                name="maxParticipants"
                value={formData.maxParticipants}
                onChange={handleInputChange}
                placeholder="Leave empty for unlimited"
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label><FiDollarSign /> Entry Fee</Label>
              <FormRow columns="2fr 1fr" style={{ marginBottom: 0 }}>
                <Input
                  type="number"
                  name="entryFee"
                  value={formData.entryFee}
                  onChange={handleInputChange}
                  placeholder="0.00"
                  step="0.01"
                />
                <Select
                  name="currency"
                  value={formData.currency}
                  onChange={handleInputChange}
                >
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                  <option value="PLN">PLN</option>
                  <option value="GBP">GBP</option>
                </Select>
              </FormRow>
            </FormGroup>
          </FormRow>

          <CheckboxGroup>
            <Checkbox
              type="checkbox"
              name="isPublic"
              checked={formData.isPublic}
              onChange={handleInputChange}
            />
            Public event (visible to everyone)
          </CheckboxGroup>
        </FormCard>

        <ButtonGroup>
          <CancelButton type="button" onClick={() => navigate('/events')}>
            Cancel
          </CancelButton>
          <SaveButton type="submit" disabled={loading}>
            {loading ? 'Creating...' : 'Create Event'}
          </SaveButton>
        </ButtonGroup>
      </form>
    </Layout>
  );
};

export default CreateEvent;
