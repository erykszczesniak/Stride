import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import EventCard from "../components/EventCard/EventCard";
import EventService from "../api/EventService";
import { LoadingSpinner, AddButton, EmptyState, EmptyIcon, EmptyText } from "./DashboardStyles";
import { PageHeader, PageTitle, EventsList } from "./EventsStyles";

const Events = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(true);
  const [events, setEvents] = useState([]);

  useEffect(() => {
    const fetchEvents = async () => {
      try {
        setLoading(true);
        const res = await EventService.getAllEvents(0, 20);
        setEvents(Array.isArray(res.data) ? res.data :
                  res.data?.content ? res.data.content : []);
      } catch (error) {
      } finally {
        setLoading(false);
      }
    };

    fetchEvents();
  }, []);

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  return (
    <Layout>
      <PageHeader>
        <PageTitle>{t("events.title")}</PageTitle>
        <AddButton to="/events/new">
          <FiPlus size={18} />
          {t("events.createEvent")}
        </AddButton>
      </PageHeader>

      {events.length > 0 ? (
        <EventsList>
          {events.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </EventsList>
      ) : (
        <EmptyState>
          <EmptyIcon>📅</EmptyIcon>
          <EmptyText>{t("events.empty")}</EmptyText>
          <AddButton to="/events/new">
            <FiPlus size={18} />
            {t("events.createEvent")}
          </AddButton>
        </EmptyState>
      )}
    </Layout>
  );
};

export default Events;
