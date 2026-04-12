import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import GoalProgress from "../components/GoalProgress/GoalProgress";
import GoalService from "../api/GoalService";
import { LoadingSpinner, AddButton, EmptyState, EmptyIcon, EmptyText } from "./DashboardStyles";
import {
  PageHeader,
  PageTitle,
  GoalsList,
  ConfirmOverlay,
  ConfirmBox,
  ConfirmTitle,
  ConfirmText,
  ConfirmButtons,
  CancelBtn,
  DeleteConfirmBtn,
} from "./GoalsStyles";

const Goals = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [goals, setGoals] = useState([]);
  const [deleteGoal, setDeleteGoal] = useState(null);

  useEffect(() => {
    const fetchGoals = async () => {
      try {
        setLoading(true);
        const res = await GoalService.getUserGoals();
        setGoals(Array.isArray(res.data) ? res.data : []);
      } catch (error) {
      } finally {
        setLoading(false);
      }
    };

    fetchGoals();
  }, []);

  const handleDelete = async () => {
    await GoalService.deleteGoal(deleteGoal.id);
    setGoals((prev) => prev.filter((g) => g.id !== deleteGoal.id));
    setDeleteGoal(null);
  };

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
        <PageTitle>{t("goals.myGoals")}</PageTitle>
        <AddButton to="/goals/new">
          <FiPlus size={18} />
          {t("goals.createGoal")}
        </AddButton>
      </PageHeader>

      {goals.length > 0 ? (
        <GoalsList>
          {goals.map((goal) => (
            <GoalProgress
              key={goal.id}
              goal={goal}
              onEdit={(g) => navigate(`/goals/${g.id}`)}
              onDelete={(g) => setDeleteGoal(g)}
            />
          ))}
        </GoalsList>
      ) : (
        <EmptyState>
          <EmptyIcon>🎯</EmptyIcon>
          <EmptyText>{t("goals.empty")}</EmptyText>
          <AddButton to="/goals/new">
            <FiPlus size={18} />
            {t("goals.createGoal")}
          </AddButton>
        </EmptyState>
      )}

      {deleteGoal && (
        <ConfirmOverlay onClick={() => setDeleteGoal(null)}>
          <ConfirmBox onClick={(e) => e.stopPropagation()}>
            <ConfirmTitle>{t("goals.deleteTitle")}</ConfirmTitle>
            <ConfirmText>{t("goals.deleteText")}</ConfirmText>
            <ConfirmButtons>
              <CancelBtn onClick={() => setDeleteGoal(null)}>{t("common.cancel")}</CancelBtn>
              <DeleteConfirmBtn onClick={handleDelete}>{t("common.delete")}</DeleteConfirmBtn>
            </ConfirmButtons>
          </ConfirmBox>
        </ConfirmOverlay>
      )}
    </Layout>
  );
};

export default Goals;
