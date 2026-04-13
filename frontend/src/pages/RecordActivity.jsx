import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FiCamera, FiX, FiEdit3, FiUploadCloud, FiFileText } from 'react-icons/fi';
import Layout from '../components/Layout/Layout';
import ActivityService from '../api/ActivityService';
import ImportService from '../api/ImportService';
import { useUnits } from '../context/UnitsContext';
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
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams();
  const unitsCtx = useUnits();
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
    { type: 'RUN', label: t('activities.typesShort.RUN'), icon: '🏃', color: '#fc5200' },
    { type: 'RIDE', label: t('activities.typesShort.RIDE'), icon: '🚴', color: '#0095ff' },
    { type: 'SWIM', label: t('activities.typesShort.SWIM'), icon: '🏊', color: '#00bcd4' },
    { type: 'HIKE', label: t('activities.typesShort.HIKE'), icon: '⛰️', color: '#4caf50' },
    { type: 'WALK', label: t('activities.typesShort.WALK'), icon: '🚶', color: '#9c27b0' },
    { type: 'WORKOUT', label: t('activities.typesShort.WORKOUT'), icon: '💪', color: '#37474f' }
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
        // Convert metric values from backend to display units
        const displayDistance = a.distance ? unitsCtx.convertDistance(a.distance) : '';
        const displayElevation = a.elevationGain ? Math.round(unitsCtx.convertElevation(a.elevationGain)) : '';
        setFormData({
          title: a.title || '',
          description: a.description || '',
          distance: displayDistance ? String(parseFloat(displayDistance.toFixed(2))) : '',
          durationHours: totalSeconds >= 3600 ? String(Math.floor(totalSeconds / 3600)) : '',
          durationMinutes: totalSeconds > 0 ? String(Math.floor((totalSeconds % 3600) / 60)) : '',
          durationSeconds: totalSeconds > 0 ? String(totalSeconds % 60) : '',
          elevation: displayElevation ? String(displayElevation) : '',
          calories: a.calories ? String(a.calories) : '',
          avgHeartRate: a.averageHeartRate ? String(a.averageHeartRate) : '',
        });
        if (a.photos && a.photos.length > 0) {
          setExistingPhotos(a.photos);
        }
      } catch (err) {
        setError(t('activities.record.errors.loadFailed'));
      }
    };
    fetchActivity();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, isEditMode, unitsCtx.isImperial]);

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
        setError(t('activities.record.errors.invalidImageFormat'));
        return false;
      }
      if (file.size > maxSize) {
        setError(t('activities.record.errors.photoTooLarge'));
        return false;
      }
      return true;
    });

    if (photos.length + validFiles.length > 5) {
      setError(t('activities.record.errors.tooManyPhotos'));
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
      setError(t('activities.record.errors.unsupportedFormat', { formats: ALLOWED_IMPORT_EXTENSIONS.join(', ').toUpperCase() }));
      return false;
    }
    if (file.size > MAX_IMPORT_SIZE) {
      setError(t('activities.record.errors.fileTooLarge'));
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
        setImportSuccess(t('activities.record.importSuccess', { title: created.title }));
        setTimeout(() => navigate(`/activities/${created.id}`), 800);
      } else {
        navigate('/activities');
      }
    } catch (err) {
      setError(err.response?.data?.message || t('activities.record.errors.importFailed'));
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
      setError(t('activities.record.errors.importFailed'));
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
        setError(t('activities.record.errors.titleRequired'));
        return;
      }

      if (!formData.distance || parseFloat(formData.distance) <= 0) {
        setError(t('activities.record.errors.distanceRequired'));
        return;
      }

      const totalDurationSeconds =
        (parseInt(formData.durationHours) || 0) * 3600 +
        (parseInt(formData.durationMinutes) || 0) * 60 +
        (parseInt(formData.durationSeconds) || 0);

      if (totalDurationSeconds <= 0) {
        setError(t('activities.record.errors.durationRequired'));
        return;
      }

      setSaving(true);

      // Convert input values from display units to metric for backend storage
      const distanceKm = unitsCtx.convertDistanceToMetric(parseFloat(formData.distance));
      const elevationM = formData.elevation
        ? Math.round(unitsCtx.convertElevationToMetric(parseFloat(formData.elevation)))
        : 0;

      // Prepare activity data matching backend ActivityRequest DTO
      // Duration is stored in seconds in the backend
      const activityData = {
        title: formData.title,
        description: formData.description || null,
        activityType: selectedType,
        distance: parseFloat(distanceKm.toFixed(4)),
        duration: totalDurationSeconds,
        elevationGain: elevationM,
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
      setError(err.response?.data?.message || t('activities.record.errors.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
    <Container>
      <Header>
        <Title>{isEditMode ? t('activities.record.titleEdit') : t('activities.record.titleNew')}</Title>
      </Header>

      {!isEditMode && (
        <ModeTabs>
          <ModeTab $active={mode === 'manual'} onClick={() => { setMode('manual'); setError(''); setImportSuccess(''); }}>
            <FiEdit3 size={18} />
            {t('activities.record.manualEntry')}
          </ModeTab>
          <ModeTab $active={mode === 'import'} onClick={() => { setMode('import'); setError(''); setImportSuccess(''); }}>
            <FiUploadCloud size={18} />
            {t('activities.record.uploadFile')}
          </ModeTab>
        </ModeTabs>
      )}

      {!isEditMode && mode === 'import' && (
        <>
          <Section>
            <SectionTitle>{t('activities.record.uploadSyncTitle')}</SectionTitle>
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
                {dragging ? t('activities.record.dropFileHere') : t('activities.record.chooseOrDrag')}
              </DropzoneText>
              <DropzoneSubtext>
                {t('activities.record.dropzoneSubtext')}
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
                <ClearFileButton type="button" onClick={handleClearImportFile} aria-label={t('activities.record.removeFile')}>
                  <FiX size={18} />
                </ClearFileButton>
              </SelectedFileBox>
            )}

            {sampleFiles.length > 0 && (
              <SamplesContainer>
                <SamplesTitle>{t('activities.record.samplesTitle')}</SamplesTitle>
                <SamplesSubtitle>
                  {t('activities.record.samplesSubtitle')}
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
              {importing ? t('common.importing') : t('activities.record.importActivity')}
            </SaveButton>
          </Section>
        </>
      )}

      {(isEditMode || mode === 'manual') && (
      <>
      <Section>
        <SectionTitle>{t('activities.record.whatDidYouDo')}</SectionTitle>
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
          <Label>{t('activities.record.activityTitle')}</Label>
          <Input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleInputChange}
            placeholder={t('activities.record.activityTitlePlaceholder')}
          />
        </FormGroup>

        <FormGroup>
          <Label>{t('activities.record.descriptionOptional')}</Label>
          <TextArea
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            placeholder={t('activities.record.descriptionPlaceholder')}
            rows={4}
          />
        </FormGroup>
      </Section>

      <Section>
        <SectionTitle>{t('activities.record.activityStats')}</SectionTitle>
        <StatsGrid>
          <div>
            <StatLabel>📍 {t('activities.stats.distance')} ({unitsCtx.distanceUnit})</StatLabel>
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
            <StatLabel>⏱️ {t('activities.record.duration')}</StatLabel>
            <DurationInputGroup>
              <StatInput
                type="number"
                name="durationHours"
                value={formData.durationHours}
                onChange={handleInputChange}
                placeholder="0"
                min="0"
                aria-label={t('activities.record.hours')}
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
                aria-label={t('activities.record.minutes')}
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
                aria-label={t('activities.record.seconds')}
              />
            </DurationInputGroup>
          </div>

          <div>
            <StatLabel>⛰️ {t('activities.stats.elevation')} ({unitsCtx.elevationUnit})</StatLabel>
            <StatInput
              type="number"
              name="elevation"
              value={formData.elevation}
              onChange={handleInputChange}
              placeholder="0"
            />
          </div>

          <div>
            <StatLabel>🔥 {t('activities.record.calories')}</StatLabel>
            <StatInput
              type="number"
              name="calories"
              value={formData.calories}
              onChange={handleInputChange}
              placeholder="0"
            />
          </div>

          <div>
            <StatLabel>❤️ {t('activities.record.avgHeartRate')}</StatLabel>
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
        <SectionTitle>{t('activities.record.photos')}</SectionTitle>
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
              {t('activities.record.addPhotos')}
            </PhotoUploadButton>
          )}
          <PhotoCount>{t('activities.record.photoCount', { current: existingPhotos.length + photos.length })}</PhotoCount>
        </PhotoUploadArea>
      </Section>

      {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

      <SaveButton onClick={handleSaveActivity} disabled={saving}>
        {saving ? t('common.saving') : isEditMode ? t('activities.record.updateActivity') : t('activities.record.saveActivity')}
      </SaveButton>
      </>
      )}
    </Container>
    </Layout>
  );
};

export default RecordActivity;
