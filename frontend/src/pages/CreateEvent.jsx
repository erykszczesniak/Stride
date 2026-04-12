import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();
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
    { type: 'RUN', label: t('activities.typesShort.RUN'), icon: '🏃', color: '#fc5200' },
    { type: 'RIDE', label: t('activities.typesShort.RIDE'), icon: '🚴', color: '#0095ff' },
    { type: 'SWIM', label: t('activities.typesShort.SWIM'), icon: '🏊', color: '#00bcd4' },
    { type: 'HIKE', label: t('activities.typesShort.HIKE'), icon: '⛰️', color: '#4caf50' },
    { type: 'WALK', label: t('activities.typesShort.WALK'), icon: '🚶', color: '#9c27b0' },
    { type: 'TRIATHLON', label: t('activities.typesShort.TRIATHLON'), icon: '🏆', color: '#ff9800' }
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
      setError(t('events.errors.nameRequired'));
      return;
    }

    if (!formData.eventDate || !formData.eventTime) {
      setError(t('events.errors.dateRequired'));
      return;
    }

    if (!formData.location.trim()) {
      setError(t('events.errors.locationRequired'));
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
      setError(err.response?.data?.message || t('events.errors.createFailed'));
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
        <PageTitle>{t('events.createEvent')}</PageTitle>
      </PageHeader>

      <form onSubmit={handleSubmit}>
        {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

        <FormCard>
          <SectionTitle>{t('events.eventType')}</SectionTitle>
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
          <SectionTitle>{t('events.basicInformation')}</SectionTitle>
          <FormGroup>
            <Label>{t('events.eventName')}</Label>
            <Input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              placeholder={t('events.eventNamePlaceholder')}
            />
          </FormGroup>

          <FormGroup>
            <Label>{t('events.description')}</Label>
            <TextArea
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder={t('events.descriptionPlaceholder')}
            />
          </FormGroup>

          <FormRow>
            <FormGroup noMargin>
              <Label>{t('events.distance')}</Label>
              <Input
                type="number"
                name="distance"
                value={formData.distance}
                onChange={handleInputChange}
                placeholder={t('events.distancePlaceholder')}
                step="0.1"
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>{t('events.externalUrl')}</Label>
              <Input
                type="url"
                name="externalUrl"
                value={formData.externalUrl}
                onChange={handleInputChange}
                placeholder={t('events.externalUrlPlaceholder')}
              />
            </FormGroup>
          </FormRow>
        </FormCard>

        <FormCard>
          <SectionTitle><FiCalendar /> {t('events.dateTime')}</SectionTitle>
          <FormRow>
            <FormGroup noMargin>
              <Label>{t('events.eventDate')}</Label>
              <Input
                type="date"
                name="eventDate"
                value={formData.eventDate}
                onChange={handleInputChange}
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>{t('events.startTime')}</Label>
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
          <SectionTitle><FiMapPin /> {t('events.location')}</SectionTitle>
          <FormGroup>
            <Label>{t('events.venue')}</Label>
            <Input
              type="text"
              name="location"
              value={formData.location}
              onChange={handleInputChange}
              placeholder={t('events.venuePlaceholder')}
            />
          </FormGroup>

          <FormRow>
            <FormGroup noMargin>
              <Label>{t('events.city')}</Label>
              <Input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleInputChange}
                placeholder={t('events.cityPlaceholder')}
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>{t('events.country')}</Label>
              <Input
                type="text"
                name="country"
                value={formData.country}
                onChange={handleInputChange}
                placeholder={t('events.countryPlaceholder')}
              />
            </FormGroup>
          </FormRow>
        </FormCard>

        <FormCard>
          <SectionTitle><FiUsers /> {t('events.registration')}</SectionTitle>
          <FormRow>
            <FormGroup noMargin>
              <Label>{t('events.maxParticipants')}</Label>
              <Input
                type="number"
                name="maxParticipants"
                value={formData.maxParticipants}
                onChange={handleInputChange}
                placeholder={t('events.maxParticipantsPlaceholder')}
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label><FiDollarSign /> {t('events.entryFee')}</Label>
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
            {t('events.publicLabel')}
          </CheckboxGroup>
        </FormCard>

        <ButtonGroup>
          <CancelButton type="button" onClick={() => navigate('/events')}>
            {t('common.cancel')}
          </CancelButton>
          <SaveButton type="submit" disabled={loading}>
            {loading ? t('common.creating') : t('events.createButton')}
          </SaveButton>
        </ButtonGroup>
      </form>
    </Layout>
  );
};

export default CreateEvent;
