import React from "react";
import { FiMapPin, FiUsers, FiClock } from "react-icons/fi";
import i18n from "../../i18n";
import { useUnits } from "../../context/UnitsContext";
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

const formatTime = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleTimeString(i18n.language, {
    hour: "2-digit",
    minute: "2-digit",
  });
};

const formatMonthAbbr = (date) => {
  return date.toLocaleDateString(i18n.language, { month: "short" }).toUpperCase();
};

const EventCard = ({ event, compact = false }) => {
  const unitsCtx = useUnits();
  const eventDate = new Date(event.eventDate);
  const month = formatMonthAbbr(eventDate);
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
            {event.distance && `${unitsCtx.convertDistance(event.distance).toFixed(1)} ${unitsCtx.distanceUnit} • `}{event.city || event.location}
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

export default React.memo(EventCard);
