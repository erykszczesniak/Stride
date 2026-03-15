import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
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

const activityTypeConfig = {
  RUN: { icon: '🏃', label: 'Run', color: '#fc5200' },
  RIDE: { icon: '🚴', label: 'Cycling', color: '#0095ff' },
  SWIM: { icon: '🏊', label: 'Swimming', color: '#00bcd4' },
  HIKE: { icon: '⛰️', label: 'Hiking', color: '#4caf50' },
  WALK: { icon: '🚶', label: 'Walking', color: '#9c27b0' },
  TRIATHLON: { icon: '🏆', label: 'Triathlon', color: '#ff9800' },
};

const months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

const EventDetails = () => {
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
      toast.error("Failed to update registration. Please try again.");
    } finally {
      setRegistering(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to delete this event?")) return;
    try {
      await EventService.deleteEvent(event.id);
      navigate("/events");
    } catch (error) {
      toast.error("Failed to delete event.");
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
          <FiArrowLeft /> Back to Events
        </BackLink>
        <InfoCard>
          <p>Event not found.</p>
        </InfoCard>
      </Layout>
    );
  }

  const eventDate = new Date(event.eventDate);
  const typeConfig = activityTypeConfig[event.activityType] || activityTypeConfig.RUN;
  const participantPercent = event.maxParticipants
    ? Math.min(100, (event.registeredCount / event.maxParticipants) * 100)
    : 0;
  const isFull = event.maxParticipants && event.registeredCount >= event.maxParticipants;
  const isPast = eventDate < new Date();

  return (
    <Layout>
      <BackLink to="/events">
        <FiArrowLeft /> Back to Events
      </BackLink>

      <EventHeader>
        <CoverImage>
          <EventIcon>{typeConfig.icon}</EventIcon>
        </CoverImage>
        <HeaderContent>
          <EventMeta>
            <DateBadge>
              <DateMonth>{months[eventDate.getMonth()].substring(0, 3)}</DateMonth>
              <DateDay>{eventDate.getDate()}</DateDay>
              <DateYear>{eventDate.getFullYear()}</DateYear>
            </DateBadge>
            <EventTitleSection>
              <EventType color={typeConfig.color}>{typeConfig.label}</EventType>
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
              {registering ? "Processing..." :
               isPast ? "Event Ended" :
               event.isRegistered ? "✓ Registered - Click to Cancel" :
               isFull ? "Event Full" : "Register for Event"}
            </RegisterButton>
            <SecondaryButton onClick={handleDelete}>
              <FiTrash2 size={16} />
            </SecondaryButton>
          </ActionButtons>
        </HeaderContent>
      </EventHeader>

      <InfoCard>
        <CardTitle><FiCalendar /> Event Details</CardTitle>
        <InfoGrid>
          <InfoItem>
            <InfoIcon><FiCalendar size={18} /></InfoIcon>
            <InfoContent>
              <InfoLabel>Date</InfoLabel>
              <InfoValue>
                {months[eventDate.getMonth()]} {eventDate.getDate()}, {eventDate.getFullYear()}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIcon><FiClock size={18} /></InfoIcon>
            <InfoContent>
              <InfoLabel>Start Time</InfoLabel>
              <InfoValue>
                {eventDate.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIcon><FiMapPin size={18} /></InfoIcon>
            <InfoContent>
              <InfoLabel>Location</InfoLabel>
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
                <InfoLabel>Distance</InfoLabel>
                <InfoValue>{event.distance} km</InfoValue>
              </InfoContent>
            </InfoItem>
          )}

          {event.entryFee && (
            <InfoItem>
              <InfoIcon><FiDollarSign size={18} /></InfoIcon>
              <InfoContent>
                <InfoLabel>Entry Fee</InfoLabel>
                <InfoValue>{event.entryFee} {event.currency || 'USD'}</InfoValue>
              </InfoContent>
            </InfoItem>
          )}

          {event.externalUrl && (
            <InfoItem>
              <InfoIcon><FiExternalLink size={18} /></InfoIcon>
              <InfoContent>
                <InfoLabel>Website</InfoLabel>
                <ExternalLink href={event.externalUrl} target="_blank" rel="noopener noreferrer">
                  Visit Event Website <FiExternalLink size={14} />
                </ExternalLink>
              </InfoContent>
            </InfoItem>
          )}
        </InfoGrid>

        <ParticipantsSection>
          <CardTitle><FiUsers /> Participants</CardTitle>
          {event.maxParticipants ? (
            <>
              <ProgressBar>
                <ProgressFill percent={participantPercent} />
              </ProgressBar>
              <ParticipantStats>
                <span>{event.registeredCount} registered</span>
                <span>{event.maxParticipants - event.registeredCount} spots left</span>
              </ParticipantStats>
            </>
          ) : (
            <InfoValue>{event.registeredCount} participants registered</InfoValue>
          )}
        </ParticipantsSection>
      </InfoCard>
    </Layout>
  );
};

export default EventDetails;
