import React, { useEffect, useState } from "react";
import { FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import GoalProgress from "../components/GoalProgress/GoalProgress";
import GoalService from "../api/GoalService";
import { LoadingSpinner, AddButton, EmptyState, EmptyIcon, EmptyText } from "./DashboardStyles";
import styled from "styled-components";

const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
`;

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #333;
`;

const GoalsList = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
`;

const Goals = () => {
  const [loading, setLoading] = useState(true);
  const [goals, setGoals] = useState([]);

  useEffect(() => {
    const fetchGoals = async () => {
      try {
        setLoading(true);
        const res = await GoalService.getUserGoals();
        setGoals(Array.isArray(res.data) ? res.data : []);
      } catch (error) {
        console.error("Error fetching goals:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchGoals();
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
        <PageTitle>My Goals</PageTitle>
        <AddButton to="/goals/new">
          <FiPlus size={18} />
          Create Goal
        </AddButton>
      </PageHeader>

      {goals.length > 0 ? (
        <GoalsList>
          {goals.map((goal) => (
            <GoalProgress key={goal.id} goal={goal} />
          ))}
        </GoalsList>
      ) : (
        <EmptyState>
          <EmptyIcon>🎯</EmptyIcon>
          <EmptyText>No goals yet. Set your first goal to track your progress!</EmptyText>
          <AddButton to="/goals/new">
            <FiPlus size={18} />
            Create Goal
          </AddButton>
        </EmptyState>
      )}
    </Layout>
  );
};

export default Goals;
