import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  const [selectedType, setSelectedType] = useState('RUN');
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    distance: '',
    duration: '',
    elevation: '',
    calories: '',
    avgHeartRate: '',
    avgPace: '5:30/km'
  });

  const activityTypes = [
    { type: 'RUN', label: 'Run', icon: '🏃', color: '#fc5200' },
    { type: 'RIDE', label: 'Ride', icon: '🚴', color: '#0095ff' },
    { type: 'SWIM', label: 'Swim', icon: '🏊', color: '#00bcd4' },
    { type: 'HIKE', label: 'Hike', icon: '⛰️', color: '#4caf50' },
    { type: 'WALK', label: 'Walk', icon: '🚶', color: '#9c27b0' },
    { type: 'WORKOUT', label: 'Workout', icon: '💪', color: '#37474f' }
  ];

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSaveActivity = async () => {
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

      // Prepare activity data matching backend ActivityRequest DTO
      const activityData = {
        title: formData.title,
        description: formData.description || null,
        activityType: selectedType,
        distance: parseFloat(formData.distance),
        duration: parseInt(formData.duration),
        elevationGain: formData.elevation ? parseInt(formData.elevation) : 0,
        calories: formData.calories ? parseInt(formData.calories) : 0,
        averageHeartRate: formData.avgHeartRate ? parseInt(formData.avgHeartRate) : null,
        averageSpeed: formData.distance && formData.duration
          ? parseFloat((parseFloat(formData.distance) / (parseInt(formData.duration) / 60)).toFixed(2))
          : null,
        startTime: new Date().toISOString(),
        isPrivate: false
      };

      // Create activity
      await ActivityService.createActivity(activityData);

      // Navigate to activities page or dashboard
      navigate('/activities');
    } catch (err) {
      // TODO: Add error reporting service
      setError(err.response?.data?.message || 'Failed to save activity. Please try again.');
    }
  };

  return (
    <Layout>
    <Container>
      <Header>
        <Title>Record Activity</Title>
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

      {error && <ErrorMessage>{error}</ErrorMessage>}

      <SaveButton onClick={handleSaveActivity}>
        💾 Save Activity
      </SaveButton>
    </Container>
    </Layout>
  );
};

export default RecordActivity;
