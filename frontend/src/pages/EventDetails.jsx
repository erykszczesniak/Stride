import React, { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { FiArrowLeft, FiMapPin, FiClock, FiUsers, FiCalendar, FiDollarSign, FiExternalLink, FiTrash2 } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import EventService from "../api/EventService";
import { useToast } from "../context/ToastContext";
import { LoadingSpinner } from "./DashboardStyles";
import styled from "styled-components";

const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: ${({ theme }) => theme.textSecondary};
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 24px;

  &:hover {
    color: #fc4c02;
  }
`;

const EventHeader = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const CoverImage = styled.div`
  height: 200px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
`;

const EventIcon = styled.span`
  font-size: 64px;
`;

const HeaderContent = styled.div`
  padding: 24px;
`;

const EventMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
`;

const DateBadge = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  background: ${({ theme }) => theme.primaryLight};
  padding: 12px 16px;
  border-radius: 12px;
  min-width: 70px;
`;

const DateMonth = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: #fc4c02;
  text-transform: uppercase;
`;

const DateDay = styled.span`
  font-size: 28px;
  font-weight: 700;
  color: #fc4c02;
`;

const DateYear = styled.span`
  font-size: 11px;
  color: #999;
`;

const EventTitleSection = styled.div`
  flex: 1;
`;

const EventType = styled.span`
  display: inline-block;
  background: ${props => props.color || '#fc4c02'};
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
`;

const Title = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px 0;
`;

const Description = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin: 16px 0;
`;

const ActionButtons = styled.div`
  display: flex;
  gap: 12px;
  margin-top: 20px;
`;

const RegisterButton = styled.button`
  flex: 1;
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;

  ${props => props.registered ? `
    background: #e8f5e9;
    color: #2e7d32;

    &:hover {
      background: #ffebee;
      color: #c62828;
    }
  ` : `
    background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(252, 76, 2, 0.3);
    }
  `}

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
`;

const SecondaryButton = styled.button`
  padding: 14px 20px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;

  &:hover {
    border-color: #fc4c02;
    color: #fc4c02;
  }
`;

const InfoCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const CardTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const InfoGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
`;

const InfoItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
`;

const InfoIcon = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: ${({ theme }) => theme.bgSecondary};
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.textSecondary};
  flex-shrink: 0;
`;

const InfoContent = styled.div``;

const InfoLabel = styled.p`
  font-size: 12px;
  color: #999;
  margin: 0 0 4px 0;
`;

const InfoValue = styled.p`
  font-size: 15px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

const ParticipantsSection = styled.div`
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid ${({ theme }) => theme.borderLight};
`;

const ProgressBar = styled.div`
  height: 8px;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 4px;
  overflow: hidden;
  margin: 12px 0;
`;

const ProgressFill = styled.div`
  height: 100%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  width: ${props => props.percent}%;
  border-radius: 4px;
  transition: width 0.3s ease;
`;

const ParticipantStats = styled.div`
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
`;

const ExternalLink = styled.a`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #fc4c02;
  text-decoration: none;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
`;

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
        // TODO: Add error reporting service
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
      // TODO: Add error reporting service
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
      // TODO: Add error reporting service
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
