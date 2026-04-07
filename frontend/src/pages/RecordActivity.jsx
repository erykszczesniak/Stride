import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Layout from '../components/Layout/Layout';
import ActivityService from '../api/ActivityService';
import {
  Container,
  Header,
  Title,
  Section,
  SectionTitle,
  ActivityTypeGrid,
  ActivityTypeButton,
  ActivityIcon,
  ActivityLabel,
  FormGroup,
  Label,
  Input,
  TextArea,
  StatsGrid,
  StatInput,
  StatLabel,
  SaveButton,
  ErrorMessage
} from './RecordActivityStyles';

const RecordActivity = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = Boolean(id);
  const [selectedType, setSelectedType] = useState('RUN');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [originalStartTime, setOriginalStartTime] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    distance: '',
    duration: '',
    elevation: '',
    calories: '',
    avgHeartRate: '',
    avgPace: ''
  });

  const activityTypes = [
    { type: 'RUN', label: 'Run', icon: '🏃', color: '#fc5200' },
    { type: 'RIDE', label: 'Ride', icon: '🚴', color: '#0095ff' },
    { type: 'SWIM', label: 'Swim', icon: '🏊', color: '#00bcd4' },
    { type: 'HIKE', label: 'Hike', icon: '⛰️', color: '#4caf50' },
    { type: 'WALK', label: 'Walk', icon: '🚶', color: '#9c27b0' },
    { type: 'WORKOUT', label: 'Workout', icon: '💪', color: '#37474f' }
  ];

  useEffect(() => {
    if (!isEditMode) return;
    const fetchActivity = async () => {
      try {
        const res = await ActivityService.getActivityById(id);
        const a = res.data;
        setSelectedType(a.activityType || 'RUN');
        setOriginalStartTime(a.startTime);
        setFormData({
          title: a.title || '',
          description: a.description || '',
          distance: a.distance ? String(a.distance) : '',
          duration: a.duration ? String(Math.round(a.duration / 60)) : '',
          elevation: a.elevationGain ? String(a.elevationGain) : '',
          calories: a.calories ? String(a.calories) : '',
          avgHeartRate: a.averageHeartRate ? String(a.averageHeartRate) : '',
          avgPace: ''
        });
      } catch (err) {
        setError('Failed to load activity');
      }
    };
    fetchActivity();
  }, [id, isEditMode]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSaveActivity = async () => {
    if (saving) return;

    try {
      setError('');

      // Validation
      if (!formData.title.trim()) {
        setError('Please enter an activity title');
        return;
      }

      if (!formData.distance || parseFloat(formData.distance) <= 0) {
        setError('Please enter a valid distance');
        return;
      }

      if (!formData.duration || parseInt(formData.duration) <= 0) {
        setError('Please enter a valid duration');
        return;
      }

      setSaving(true);

      const durationMinutes = parseInt(formData.duration);
      const distanceKm = parseFloat(formData.distance);

      // Prepare activity data matching backend ActivityRequest DTO
      // Duration is stored in seconds in the backend
      const activityData = {
        title: formData.title,
        description: formData.description || null,
        activityType: selectedType,
        distance: distanceKm,
        duration: durationMinutes * 60,
        elevationGain: formData.elevation ? parseInt(formData.elevation) : 0,
        calories: formData.calories ? parseInt(formData.calories) : 0,
        averageHeartRate: formData.avgHeartRate ? parseInt(formData.avgHeartRate) : null,
        averageSpeed: distanceKm && durationMinutes
          ? parseFloat((distanceKm / (durationMinutes / 60)).toFixed(2))
          : null,
        startTime: isEditMode && originalStartTime ? originalStartTime : new Date().toISOString(),
        isPrivate: false
      };

      if (isEditMode) {
        await ActivityService.updateActivity(id, activityData);
      } else {
        await ActivityService.createActivity(activityData);
      }
      navigate('/activities');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save activity. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
    <Container>
      <Header>
        <Title>{isEditMode ? 'Edit Activity' : 'Record Activity'}</Title>
      </Header>

      <Section>
        <SectionTitle>What did you do?</SectionTitle>
        <ActivityTypeGrid>
          {activityTypes.map((activity) => (
            <ActivityTypeButton
              key={activity.type}
              selected={selectedType === activity.type}
              color={activity.color}
              onClick={() => setSelectedType(activity.type)}
            >
              <ActivityIcon>{activity.icon}</ActivityIcon>
              <ActivityLabel>{activity.label}</ActivityLabel>
            </ActivityTypeButton>
          ))}
        </ActivityTypeGrid>
      </Section>

      <Section>
        <FormGroup>
          <Label>Activity Title</Label>
          <Input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleInputChange}
            placeholder="My run workout"
          />
        </FormGroup>

        <FormGroup>
          <Label>Description (optional)</Label>
          <TextArea
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            placeholder="How did it feel? Any notes..."
            rows={4}
          />
        </FormGroup>
      </Section>

      <Section>
        <SectionTitle>Activity Stats</SectionTitle>
        <StatsGrid>
          <div>
            <StatLabel>📍 Distance (km)</StatLabel>
            <StatInput
              type="number"
              name="distance"
              value={formData.distance}
              onChange={handleInputChange}
              placeholder="0.0"
              step="0.1"
            />
          </div>

          <div>
            <StatLabel>⏱️ Duration (min)</StatLabel>
            <StatInput
              type="number"
              name="duration"
              value={formData.duration}
              onChange={handleInputChange}
              placeholder="0"
            />
          </div>

          <div>
            <StatLabel>⛰️ Elevation (m)</StatLabel>
            <StatInput
              type="number"
              name="elevation"
              value={formData.elevation}
              onChange={handleInputChange}
              placeholder="0"
            />
          </div>

          <div>
            <StatLabel>🔥 Calories</StatLabel>
            <StatInput
              type="number"
              name="calories"
              value={formData.calories}
              onChange={handleInputChange}
              placeholder="0"
            />
          </div>

          <div>
            <StatLabel>❤️ Avg Heart Rate</StatLabel>
            <StatInput
              type="number"
              name="avgHeartRate"
              value={formData.avgHeartRate}
              onChange={handleInputChange}
              placeholder="0"
            />
          </div>

          <div>
            <StatLabel>⚡ Avg Pace</StatLabel>
            <StatInput
              type="text"
              name="avgPace"
              value={formData.avgPace}
              onChange={handleInputChange}
              placeholder="5:30/km"
            />
          </div>
        </StatsGrid>
      </Section>

      {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

      <SaveButton onClick={handleSaveActivity} disabled={saving}>
        {saving ? 'Saving...' : isEditMode ? 'Update Activity' : 'Save Activity'}
      </SaveButton>
    </Container>
    </Layout>
  );
};

export default RecordActivity;
