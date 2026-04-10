import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { FiCamera, FiX, FiEdit3, FiUploadCloud, FiFileText } from 'react-icons/fi';
import Layout from '../components/Layout/Layout';
import ActivityService from '../api/ActivityService';
import ImportService from '../api/ImportService';
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
  DurationInputGroup,
  DurationSeparator,
  SaveButton,
  ErrorMessage,
  PhotoUploadArea,
  PhotoUploadButton,
  PhotoPreviewGrid,
  PhotoPreviewItem,
  PhotoPreviewImage,
  PhotoRemoveButton,
  PhotoCount,
  ModeTabs,
  ModeTab,
  ImportDropzone,
  DropzoneIcon,
  DropzoneText,
  DropzoneSubtext,
  SelectedFileBox,
  SelectedFileInfo,
  FileIcon,
  FileDetails,
  FileName,
  FileMeta,
  ClearFileButton,
  SamplesContainer,
  SamplesTitle,
  SamplesSubtitle,
  SamplesGrid,
  SampleCard,
  SampleHeader,
  SampleFormatBadge,
  SampleType,
  SampleName,
  SuccessMessage,
} from './RecordActivityStyles';

const ALLOWED_IMPORT_EXTENSIONS = ['gpx', 'tcx', 'fit'];
const MAX_IMPORT_SIZE = 10 * 1024 * 1024; // 10MB

const formatBytes = (bytes) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
};

const getFileExtension = (filename) => {
  if (!filename || !filename.includes('.')) return '';
  return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
};

const RecordActivity = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = Boolean(id);
  const [selectedType, setSelectedType] = useState('RUN');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [originalStartTime, setOriginalStartTime] = useState(null);
  const [photos, setPhotos] = useState([]);
  const [existingPhotos, setExistingPhotos] = useState([]);
  const fileInputRef = useRef(null);
  const importFileInputRef = useRef(null);
  const [mode, setMode] = useState('manual'); // 'manual' | 'import'
  const [importFile, setImportFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importSuccess, setImportSuccess] = useState('');
  const [sampleFiles, setSampleFiles] = useState([]);
  const [dragging, setDragging] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    distance: '',
    durationHours: '',
    durationMinutes: '',
    durationSeconds: '',
    elevation: '',
    calories: '',
    avgHeartRate: '',
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
    if (isEditMode || mode !== 'import') return;
    let cancelled = false;
    ImportService.listSampleFiles()
      .then((res) => {
        if (!cancelled) setSampleFiles(Array.isArray(res.data) ? res.data : []);
      })
      .catch((err) => {
        console.error('Failed to load sample files:', err);
      });
    return () => { cancelled = true; };
  }, [isEditMode, mode]);

  useEffect(() => {
    if (!isEditMode) return;
    const fetchActivity = async () => {
      try {
        const res = await ActivityService.getActivityById(id);
        const a = res.data;
        setSelectedType(a.activityType || 'RUN');
        setOriginalStartTime(a.startTime);
        const totalSeconds = a.duration || 0;
        setFormData({
          title: a.title || '',
          description: a.description || '',
          distance: a.distance ? String(a.distance) : '',
          durationHours: totalSeconds >= 3600 ? String(Math.floor(totalSeconds / 3600)) : '',
          durationMinutes: totalSeconds > 0 ? String(Math.floor((totalSeconds % 3600) / 60)) : '',
          durationSeconds: totalSeconds > 0 ? String(totalSeconds % 60) : '',
          elevation: a.elevationGain ? String(a.elevationGain) : '',
          calories: a.calories ? String(a.calories) : '',
          avgHeartRate: a.averageHeartRate ? String(a.averageHeartRate) : '',
        });
        if (a.photos && a.photos.length > 0) {
          setExistingPhotos(a.photos);
        }
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

  const handlePhotoSelect = (e) => {
    const files = Array.from(e.target.files);
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const maxSize = 5 * 1024 * 1024; // 5MB

    const validFiles = files.filter(file => {
      if (!allowedTypes.includes(file.type)) {
        setError('Only JPG, PNG, and WebP images are allowed');
        return false;
      }
      if (file.size > maxSize) {
        setError('Each photo must be less than 5MB');
        return false;
      }
      return true;
    });

    if (photos.length + validFiles.length > 5) {
      setError('Maximum 5 photos per activity');
      return;
    }

    const newPhotos = validFiles.map(file => ({
      file,
      preview: URL.createObjectURL(file),
    }));

    setPhotos(prev => [...prev, ...newPhotos]);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const validateImportFile = (file) => {
    const ext = getFileExtension(file.name);
    if (!ALLOWED_IMPORT_EXTENSIONS.includes(ext)) {
      setError(`Unsupported file format. Use: ${ALLOWED_IMPORT_EXTENSIONS.join(', ').toUpperCase()}`);
      return false;
    }
    if (file.size > MAX_IMPORT_SIZE) {
      setError('File must be smaller than 10MB');
      return false;
    }
    return true;
  };

  const handleImportFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    setImportSuccess('');
    if (validateImportFile(file)) {
      setImportFile(file);
    }
    if (importFileInputRef.current) importFileInputRef.current.value = '';
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (!file) return;
    setError('');
    setImportSuccess('');
    if (validateImportFile(file)) {
      setImportFile(file);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    if (!dragging) setDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setDragging(false);
  };

  const handleClearImportFile = () => {
    setImportFile(null);
    setError('');
    setImportSuccess('');
  };

  const handleImportActivity = async () => {
    if (importing || !importFile) return;
    try {
      setError('');
      setImportSuccess('');
      setImporting(true);
      const res = await ImportService.importActivity(importFile);
      const created = res.data?.activity;
      if (created?.id) {
        setImportSuccess(`Successfully imported "${created.title}"`);
        setTimeout(() => navigate(`/activities/${created.id}`), 800);
      } else {
        navigate('/activities');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to import file. Please try again.');
    } finally {
      setImporting(false);
    }
  };

  const handleDownloadSample = async (sample) => {
    try {
      const res = await ImportService.downloadSampleFile(sample.filename);
      const blob = new Blob([res.data], { type: 'application/octet-stream' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = sample.filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download sample file:', err);
      setError('Failed to download sample file');
    }
  };

  const handleRemovePhoto = (index) => {
    setPhotos(prev => {
      const updated = [...prev];
      URL.revokeObjectURL(updated[index].preview);
      updated.splice(index, 1);
      return updated;
    });
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

      const totalDurationSeconds =
        (parseInt(formData.durationHours) || 0) * 3600 +
        (parseInt(formData.durationMinutes) || 0) * 60 +
        (parseInt(formData.durationSeconds) || 0);

      if (totalDurationSeconds <= 0) {
        setError('Please enter a valid duration');
        return;
      }

      setSaving(true);

      const distanceKm = parseFloat(formData.distance);

      // Prepare activity data matching backend ActivityRequest DTO
      // Duration is stored in seconds in the backend
      const activityData = {
        title: formData.title,
        description: formData.description || null,
        activityType: selectedType,
        distance: distanceKm,
        duration: totalDurationSeconds,
        elevationGain: formData.elevation ? parseInt(formData.elevation) : 0,
        calories: formData.calories ? parseInt(formData.calories) : 0,
        averageHeartRate: formData.avgHeartRate ? parseInt(formData.avgHeartRate) : null,
        averageSpeed: distanceKm && totalDurationSeconds
          ? parseFloat((distanceKm / (totalDurationSeconds / 3600)).toFixed(2))
          : null,
        startTime: isEditMode && originalStartTime ? originalStartTime : new Date().toISOString(),
        isPrivate: false
      };

      if (isEditMode) {
        await ActivityService.updateActivity(id, activityData);
        if (photos.length > 0) {
          try {
            await ActivityService.uploadPhotos(id, photos.map(p => p.file));
          } catch (photoErr) {
            console.error('Photo upload failed:', photoErr);
          }
        }
      } else {
        const res = await ActivityService.createActivity(activityData);
        if (photos.length > 0 && res.data?.id) {
          try {
            await ActivityService.uploadPhotos(
              res.data.id,
              photos.map(p => p.file)
            );
          } catch (photoErr) {
            // Activity was created, photos failed — still navigate
            console.error('Photo upload failed:', photoErr);
          }
        }
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

      {!isEditMode && (
        <ModeTabs>
          <ModeTab $active={mode === 'manual'} onClick={() => { setMode('manual'); setError(''); setImportSuccess(''); }}>
            <FiEdit3 size={18} />
            Manual Entry
          </ModeTab>
          <ModeTab $active={mode === 'import'} onClick={() => { setMode('import'); setError(''); setImportSuccess(''); }}>
            <FiUploadCloud size={18} />
            Upload File
          </ModeTab>
        </ModeTabs>
      )}

      {!isEditMode && mode === 'import' && (
        <>
          <Section>
            <SectionTitle>Upload and Sync Your Activity</SectionTitle>
            <input
              ref={importFileInputRef}
              type="file"
              accept=".gpx,.tcx,.fit"
              onChange={handleImportFileSelect}
              style={{ display: 'none' }}
            />
            <ImportDropzone
              $dragging={dragging}
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onClick={() => importFileInputRef.current?.click()}
            >
              <DropzoneIcon>📁</DropzoneIcon>
              <DropzoneText>
                {dragging ? 'Drop file here' : 'Click to choose a file or drag it here'}
              </DropzoneText>
              <DropzoneSubtext>
                Works for <strong>.gpx</strong>, <strong>.tcx</strong>, or <strong>.fit</strong> files up to 10MB.
                Activities from Garmin, Polar, Suunto, and other devices.
              </DropzoneSubtext>
            </ImportDropzone>

            {importFile && (
              <SelectedFileBox>
                <SelectedFileInfo>
                  <FileIcon><FiFileText /></FileIcon>
                  <FileDetails>
                    <FileName>{importFile.name}</FileName>
                    <FileMeta>
                      {getFileExtension(importFile.name).toUpperCase()} · {formatBytes(importFile.size)}
                    </FileMeta>
                  </FileDetails>
                </SelectedFileInfo>
                <ClearFileButton type="button" onClick={handleClearImportFile} aria-label="Remove file">
                  <FiX size={18} />
                </ClearFileButton>
              </SelectedFileBox>
            )}

            {sampleFiles.length > 0 && (
              <SamplesContainer>
                <SamplesTitle>Don't have a file? Try a sample</SamplesTitle>
                <SamplesSubtitle>
                  Download one of these example activities to test the import feature.
                </SamplesSubtitle>
                <SamplesGrid>
                  {sampleFiles.map((sample) => (
                    <SampleCard key={sample.filename} onClick={() => handleDownloadSample(sample)}>
                      <SampleHeader>
                        <SampleFormatBadge>{sample.format}</SampleFormatBadge>
                        <SampleType>{sample.activityType}</SampleType>
                      </SampleHeader>
                      <SampleName>{sample.filename}</SampleName>
                    </SampleCard>
                  ))}
                </SamplesGrid>
              </SamplesContainer>
            )}

            {error && <ErrorMessage role="alert">{error}</ErrorMessage>}
            {importSuccess && <SuccessMessage role="status">{importSuccess}</SuccessMessage>}

            <SaveButton onClick={handleImportActivity} disabled={importing || !importFile}>
              {importing ? 'Importing...' : 'Import Activity'}
            </SaveButton>
          </Section>
        </>
      )}

      {(isEditMode || mode === 'manual') && (
      <>
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
            <StatLabel>⏱️ Duration (h : m : s)</StatLabel>
            <DurationInputGroup>
              <StatInput
                type="number"
                name="durationHours"
                value={formData.durationHours}
                onChange={handleInputChange}
                placeholder="0"
                min="0"
                aria-label="Hours"
              />
              <DurationSeparator>:</DurationSeparator>
              <StatInput
                type="number"
                name="durationMinutes"
                value={formData.durationMinutes}
                onChange={handleInputChange}
                placeholder="00"
                min="0"
                max="59"
                aria-label="Minutes"
              />
              <DurationSeparator>:</DurationSeparator>
              <StatInput
                type="number"
                name="durationSeconds"
                value={formData.durationSeconds}
                onChange={handleInputChange}
                placeholder="00"
                min="0"
                max="59"
                aria-label="Seconds"
              />
            </DurationInputGroup>
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
        </StatsGrid>
      </Section>

      <Section>
        <SectionTitle>Photos</SectionTitle>
        <PhotoUploadArea>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            onChange={handlePhotoSelect}
            style={{ display: 'none' }}
          />
          {existingPhotos.length > 0 && (
            <PhotoPreviewGrid>
              {existingPhotos.map((photoUrl, index) => (
                <PhotoPreviewItem key={`existing-${index}`}>
                  <PhotoPreviewImage
                    src={photoUrl.startsWith('http') ? photoUrl : `${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}${photoUrl}`}
                    alt={`Photo ${index + 1}`}
                  />
                  <PhotoRemoveButton onClick={async () => {
                    try {
                      await ActivityService.deletePhoto(id, index);
                      setExistingPhotos(prev => prev.filter((_, i) => i !== index));
                    } catch (err) {
                      console.error('Failed to delete photo:', err);
                    }
                  }}>
                    <FiX size={14} />
                  </PhotoRemoveButton>
                </PhotoPreviewItem>
              ))}
            </PhotoPreviewGrid>
          )}
          {photos.length > 0 && (
            <PhotoPreviewGrid>
              {photos.map((photo, index) => (
                <PhotoPreviewItem key={`new-${index}`}>
                  <PhotoPreviewImage src={photo.preview} alt={`New photo ${index + 1}`} />
                  <PhotoRemoveButton onClick={() => handleRemovePhoto(index)}>
                    <FiX size={14} />
                  </PhotoRemoveButton>
                </PhotoPreviewItem>
              ))}
            </PhotoPreviewGrid>
          )}
          {(existingPhotos.length + photos.length) < 5 && (
            <PhotoUploadButton
              type="button"
              onClick={() => fileInputRef.current?.click()}
            >
              <FiCamera size={20} />
              Add Photos
            </PhotoUploadButton>
          )}
          <PhotoCount>{existingPhotos.length + photos.length}/5 photos</PhotoCount>
        </PhotoUploadArea>
      </Section>

      {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

      <SaveButton onClick={handleSaveActivity} disabled={saving}>
        {saving ? 'Saving...' : isEditMode ? 'Update Activity' : 'Save Activity'}
      </SaveButton>
      </>
      )}
    </Container>
    </Layout>
  );
};

export default RecordActivity;
