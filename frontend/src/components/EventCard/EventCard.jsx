import React from "react";
import {
  Card,
  DateBox,
  DateMonth,
  DateDay,
  EventInfo,
  EventName,
  EventLocation,
} from "./EventCardStyles";

const months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];

const EventCard = ({ event }) => {
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
        <EventLocation>
          {event.distance && `${event.distance} km • `}{event.city || event.location}
        </EventLocation>
      </EventInfo>
    </Card>
  );
};

export default EventCard;
