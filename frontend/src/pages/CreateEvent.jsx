import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiCalendar, FiMapPin, FiUsers, FiDollarSign } from 'react-icons/fi';
import Layout from '../components/Layout/Layout';
import EventService from '../api/EventService';
import styled from 'styled-components';

const PageHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
`;

const BackButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 8px;
  cursor: pointer;
  color: ${({ theme }) => theme.textSecondary};
  transition: all 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
    color: ${({ theme }) => theme.textPrimary};
  }
`;

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
`;

const FormCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
`;

const SectionTitle = styled.h2`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const FormRow = styled.div`
  display: grid;
  grid-template-columns: ${props => props.columns || '1fr 1fr'};
  gap: 16px;
  margin-bottom: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const FormGroup = styled.div`
  margin-bottom: ${props => props.noMargin ? '0' : '16px'};
`;

const Label = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

const Input = styled.input`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};
  transition: border-color 0.2s;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }

  &::placeholder {
    color: #999;
  }
`;

const TextArea = styled.textarea`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};
  min-height: 100px;
  resize: vertical;
  font-family: inherit;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }

  &::placeholder {
    color: #999;
  }
`;

const Select = styled.select`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  cursor: pointer;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

const ActivityTypeGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
`;

const ActivityTypeButton = styled.button`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  border: 2px solid ${props => props.selected ? props.color : props.theme.border};
  background: ${props => props.selected ? `${props.color}10` : props.theme.bgCard};
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: ${props => props.color};
  }
`;

const ActivityIcon = styled.span`
  font-size: 28px;
`;

const ActivityLabel = styled.span`
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
`;

const CheckboxGroup = styled.label`
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
`;

const Checkbox = styled.input`
  width: 18px;
  height: 18px;
  cursor: pointer;
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
`;

const CancelButton = styled.button`
  padding: 12px 24px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textSecondary};
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }
`;

const SaveButton = styled.button`
  padding: 12px 32px;
  border: none;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(252, 76, 2, 0.3);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }
`;

const ErrorMessage = styled.div`
  background: #ffebee;
  color: #c62828;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
`;

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

      await EventService.createEvent(1, eventData);
      navigate('/events');
    } catch (err) {
      // TODO: Add error reporting service
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
        {error && <ErrorMessage>{error}</ErrorMessage>}

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
