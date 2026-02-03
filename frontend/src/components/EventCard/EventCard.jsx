import React from "react";
import { FiMapPin, FiUsers, FiClock } from "react-icons/fi";
import {
  Card,
  DateBox,
  DateMonth,
  DateDay,
  EventInfo,
  EventName,
  EventLocation,
  EventMeta,
  MetaItem,
} from "./EventCardStyles";

const months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];

const formatTime = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  });
};

const EventCard = ({ event, compact = false }) => {
  const eventDate = new Date(event.eventDate);
  const month = months[eventDate.getMonth()];
  const day = eventDate.getDate();

  return (
    <Card to={`/events/${event.id}`}>
      <DateBox>
        <DateMonth>{month}</DateMonth>
        <DateDay>{day}</DateDay>
      </DateBox>
      <EventInfo>
        <EventName>{event.name}</EventName>
        {compact ? (
          <EventLocation>
            {event.distance && `${event.distance} km • `}{event.city || event.location}
          </EventLocation>
        ) : (
          <>
            <EventLocation>
              <FiMapPin size={14} />
              {event.location}{event.city && `, ${event.city}`}
            </EventLocation>
            <EventMeta>
              <MetaItem>
                <FiClock size={14} />
                {formatTime(event.eventDate)}
              </MetaItem>
              <MetaItem>
                <FiUsers size={14} />
                {event.registeredCount || 0} / {event.maxParticipants}
              </MetaItem>
            </EventMeta>
          </>
        )}
      </EventInfo>
    </Card>
  );
};

export default EventCard;
