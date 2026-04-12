import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import i18n from "../i18n";
import { FiArrowLeft, FiMapPin, FiClock, FiUsers, FiCalendar, FiDollarSign, FiExternalLink, FiTrash2 } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import EventService from "../api/EventService";
import { useToast } from "../context/ToastContext";
import { LoadingSpinner } from "./DashboardStyles";
import {
  BackLink,
  EventHeader,
  CoverImage,
  EventIcon,
  HeaderContent,
  EventMeta,
  DateBadge,
  DateMonth,
  DateDay,
  DateYear,
  EventTitleSection,
  EventType,
  Title,
  Description,
  ActionButtons,
  RegisterButton,
  SecondaryButton,
  InfoCard,
  CardTitle,
  InfoGrid,
  InfoItem,
  InfoIcon,
  InfoContent,
  InfoLabel,
  InfoValue,
  ParticipantsSection,
  ProgressBar,
  ProgressFill,
  ParticipantStats,
  ExternalLink,
} from "./EventDetailsStyles";

const activityTypeIcons = {
  RUN: '🏃',
  RIDE: '🚴',
  SWIM: '🏊',
  HIKE: '⛰️',
  WALK: '🚶',
  TRIATHLON: '🏆',
};

const activityTypeColors = {
  RUN: '#fc5200',
  RIDE: '#0095ff',
  SWIM: '#00bcd4',
  HIKE: '#4caf50',
  WALK: '#9c27b0',
  TRIATHLON: '#ff9800',
};

const EventDetails = () => {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [registering, setRegistering] = useState(false);

  useEffect(() => {
    const fetchEvent = async () => {
      try {
        setLoading(true);
        const res = await EventService.getEventById(id);
        setEvent(res.data);
      } catch (error) {
      } finally {
        setLoading(false);
      }
    };

    fetchEvent();
  }, [id]);

  const handleRegister = async () => {
    if (!event) return;
    setRegistering(true);
    try {
      if (event.isRegistered) {
        await EventService.unregisterFromEvent(event.id);
        setEvent(prev => ({
          ...prev,
          isRegistered: false,
          registeredCount: prev.registeredCount - 1
        }));
      } else {
        await EventService.registerForEvent(event.id);
        setEvent(prev => ({
          ...prev,
          isRegistered: true,
          registeredCount: prev.registeredCount + 1
        }));
      }
    } catch (error) {
      toast.error(t("events.errors.registrationFailed"));
    } finally {
      setRegistering(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(t("events.confirmDelete"))) return;
    try {
      await EventService.deleteEvent(event.id);
      navigate("/events");
    } catch (error) {
      toast.error(t("events.errors.deleteFailed"));
    }
  };

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  if (!event) {
    return (
      <Layout>
        <BackLink to="/events">
          <FiArrowLeft /> {t("events.backToEvents")}
        </BackLink>
        <InfoCard>
          <p>{t("events.notFound")}</p>
        </InfoCard>
      </Layout>
    );
  }

  const eventDate = new Date(event.eventDate);
  const typeIcon = activityTypeIcons[event.activityType] || activityTypeIcons.RUN;
  const typeColor = activityTypeColors[event.activityType] || activityTypeColors.RUN;
  const typeLabel = t(`activities.types.${event.activityType}`, { defaultValue: event.activityType });
  const participantPercent = event.maxParticipants
    ? Math.min(100, (event.registeredCount / event.maxParticipants) * 100)
    : 0;
  const isFull = event.maxParticipants && event.registeredCount >= event.maxParticipants;
  const isPast = eventDate < new Date();

  return (
    <Layout>
      <BackLink to="/events">
        <FiArrowLeft /> {t("events.backToEvents")}
      </BackLink>

      <EventHeader>
        <CoverImage>
          <EventIcon>{typeIcon}</EventIcon>
        </CoverImage>
        <HeaderContent>
          <EventMeta>
            <DateBadge>
              <DateMonth>{eventDate.toLocaleDateString(i18n.language, { month: "short" }).toUpperCase()}</DateMonth>
              <DateDay>{eventDate.getDate()}</DateDay>
              <DateYear>{eventDate.getFullYear()}</DateYear>
            </DateBadge>
            <EventTitleSection>
              <EventType color={typeColor}>{typeLabel}</EventType>
              <Title>{event.name}</Title>
            </EventTitleSection>
          </EventMeta>

          {event.description && (
            <Description>{event.description}</Description>
          )}

          <ActionButtons>
            <RegisterButton
              registered={event.isRegistered}
              onClick={handleRegister}
              disabled={registering || isPast || (isFull && !event.isRegistered)}
            >
              {registering ? t("challenges.processing") :
               isPast ? t("events.eventEnded") :
               event.isRegistered ? t("events.registered") :
               isFull ? t("events.eventFull") : t("events.registerForEvent")}
            </RegisterButton>
            <SecondaryButton onClick={handleDelete}>
              <FiTrash2 size={16} />
            </SecondaryButton>
          </ActionButtons>
        </HeaderContent>
      </EventHeader>

      <InfoCard>
        <CardTitle><FiCalendar /> {t("events.details")}</CardTitle>
        <InfoGrid>
          <InfoItem>
            <InfoIcon><FiCalendar size={18} /></InfoIcon>
            <InfoContent>
              <InfoLabel>{t("events.date")}</InfoLabel>
              <InfoValue>
                {eventDate.toLocaleDateString(i18n.language, { month: "long", day: "numeric", year: "numeric" })}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIcon><FiClock size={18} /></InfoIcon>
            <InfoContent>
              <InfoLabel>{t("events.startTime").replace(" *", "")}</InfoLabel>
              <InfoValue>
                {eventDate.toLocaleTimeString(i18n.language, { hour: "2-digit", minute: "2-digit" })}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIcon><FiMapPin size={18} /></InfoIcon>
            <InfoContent>
              <InfoLabel>{t("events.location")}</InfoLabel>
              <InfoValue>
                {event.location}
                {event.city && `, ${event.city}`}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          {event.distance && (
            <InfoItem>
              <InfoIcon>📏</InfoIcon>
              <InfoContent>
                <InfoLabel>{t("activities.stats.distance")}</InfoLabel>
                <InfoValue>{event.distance} km</InfoValue>
              </InfoContent>
            </InfoItem>
          )}

          {event.entryFee && (
            <InfoItem>
              <InfoIcon><FiDollarSign size={18} /></InfoIcon>
              <InfoContent>
                <InfoLabel>{t("events.entryFee")}</InfoLabel>
                <InfoValue>{event.entryFee} {event.currency || 'USD'}</InfoValue>
              </InfoContent>
            </InfoItem>
          )}

          {event.externalUrl && (
            <InfoItem>
              <InfoIcon><FiExternalLink size={18} /></InfoIcon>
              <InfoContent>
                <InfoLabel>{t("events.website")}</InfoLabel>
                <ExternalLink href={event.externalUrl} target="_blank" rel="noopener noreferrer">
                  {t("events.visitWebsite")} <FiExternalLink size={14} />
                </ExternalLink>
              </InfoContent>
            </InfoItem>
          )}
        </InfoGrid>

        <ParticipantsSection>
          <CardTitle><FiUsers /> {t("events.participants")}</CardTitle>
          {event.maxParticipants ? (
            <>
              <ProgressBar>
                <ProgressFill percent={participantPercent} />
              </ProgressBar>
              <ParticipantStats>
                <span>{t("events.registeredCount", { count: event.registeredCount })}</span>
                <span>{t("events.spotsLeft", { count: event.maxParticipants - event.registeredCount })}</span>
              </ParticipantStats>
            </>
          ) : (
            <InfoValue>{t("events.participantsRegistered", { count: event.registeredCount })}</InfoValue>
          )}
        </ParticipantsSection>
      </InfoCard>
    </Layout>
  );
};

export default EventDetails;
