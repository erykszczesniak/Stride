import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiAward, FiRefreshCw } from "react-icons/fi";
import PersonalRecordService from "../../api/PersonalRecordService";
import i18n from "../../i18n";
import {
  PRContainer,
  PRHeader,
  PRTitleGroup,
  PRTitle,
  PRSubtitle,
  RecalculateButton,
  PREmptyState,
  PREmptyIcon,
  PREmptyText,
  PRSportSection,
  PRSportHeader,
  PRSportIcon,
  PRSportName,
  PRSportCount,
  PRTable,
  PRTableHeader,
  PRTableHeaderCell,
  PRTableRow,
  PRTableCell,
  PRDistanceCell,
  PRDistanceBadge,
  PRDistanceLabel,
  PRTimeValue,
  PRPaceValue,
  PRImprovementBadge,
  PRDateLink,
  PRLoadingState,
} from "./PersonalRecordsStyles";

const SPORT_ICONS = {
  RUN: "🏃",
  RIDE: "🚴",
  SWIM: "🏊",
  HIKE: "⛰️",
  WALK: "🚶",
  WORKOUT: "💪",
};

const DISTANCE_ORDER = ["ONE_KM", "FIVE_KM", "TEN_KM", "HALF_MARATHON", "MARATHON"];

const formatDate = (iso) => {
  if (!iso) return "";
  const date = new Date(iso);
  return date.toLocaleDateString(i18n.language, { year: "numeric", month: "short", day: "numeric" });
};

const PersonalRecords = ({ userId, isOwnProfile = false }) => {
  const { t } = useTranslation();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [recalculating, setRecalculating] = useState(false);
  const [error, setError] = useState("");

  const fetchRecords = async () => {
    try {
      setLoading(true);
      setError("");
      const res = isOwnProfile
        ? await PersonalRecordService.getMyPersonalRecords()
        : await PersonalRecordService.getUserPersonalRecords(userId);
      setRecords(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error("Failed to load personal records:", err);
      setError(t("personalRecords.loadFailed"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecords();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId, isOwnProfile]);

  const handleRecalculate = async () => {
    if (recalculating) return;
    try {
      setRecalculating(true);
      await PersonalRecordService.recalculateAllPRs();
      await fetchRecords();
    } catch (err) {
      console.error("Failed to recalculate PRs:", err);
      setError(t("personalRecords.recalcFailed"));
    } finally {
      setRecalculating(false);
    }
  };

  const groupedBySport = useMemo(() => {
    const groups = {};
    records.forEach((pr) => {
      if (!groups[pr.activityType]) groups[pr.activityType] = [];
      groups[pr.activityType].push(pr);
    });
    Object.keys(groups).forEach((sport) => {
      groups[sport].sort((a, b) => {
        const ai = DISTANCE_ORDER.indexOf(a.distanceType);
        const bi = DISTANCE_ORDER.indexOf(b.distanceType);
        return (ai === -1 ? 999 : ai) - (bi === -1 ? 999 : bi);
      });
    });
    return groups;
  }, [records]);

  if (loading) {
    return <PRLoadingState>{t("personalRecords.loading")}</PRLoadingState>;
  }

  return (
    <PRContainer>
      <PRHeader>
        <PRTitleGroup>
          <PRTitle>
            <FiAward size={20} /> {t("personalRecords.title")}
          </PRTitle>
          <PRSubtitle>
            {t("personalRecords.subtitle")}
          </PRSubtitle>
        </PRTitleGroup>
        {isOwnProfile && records.length > 0 && (
          <RecalculateButton onClick={handleRecalculate} disabled={recalculating}>
            <FiRefreshCw size={14} className={recalculating ? "spinning" : ""} />
            {recalculating ? t("personalRecords.recalculating") : t("personalRecords.recalculate")}
          </RecalculateButton>
        )}
      </PRHeader>

      {error && <PREmptyText style={{ color: "#c00" }}>{error}</PREmptyText>}

      {records.length === 0 ? (
        <PREmptyState>
          <PREmptyIcon>🏆</PREmptyIcon>
          <PREmptyText>
            {isOwnProfile
              ? t("personalRecords.emptyOwn")
              : t("personalRecords.emptyOther")}
          </PREmptyText>
          {isOwnProfile && (
            <RecalculateButton onClick={handleRecalculate} disabled={recalculating}>
              <FiRefreshCw size={14} className={recalculating ? "spinning" : ""} />
              {recalculating ? t("personalRecords.recalculating") : t("personalRecords.recalculateFromHistory")}
            </RecalculateButton>
          )}
        </PREmptyState>
      ) : (
        Object.keys(groupedBySport).map((sport) => {
          const icon = SPORT_ICONS[sport] || "🏅";
          const label = t(`activities.types.${sport}`, { defaultValue: sport });
          const sportRecords = groupedBySport[sport];
          return (
            <PRSportSection key={sport}>
              <PRSportHeader>
                <PRSportIcon>{icon}</PRSportIcon>
                <PRSportName>{label}</PRSportName>
                <PRSportCount>
                  {t("personalRecords.prCount", { count: sportRecords.length })}
                </PRSportCount>
              </PRSportHeader>
              <PRTable>
                <PRTableHeader>
                  <PRTableHeaderCell>{t("personalRecords.columns.distance")}</PRTableHeaderCell>
                  <PRTableHeaderCell>{t("personalRecords.columns.bestTime")}</PRTableHeaderCell>
                  <PRTableHeaderCell>{t("personalRecords.columns.pace")}</PRTableHeaderCell>
                  <PRTableHeaderCell>{t("personalRecords.columns.improvement")}</PRTableHeaderCell>
                  <PRTableHeaderCell>{t("personalRecords.columns.date")}</PRTableHeaderCell>
                </PRTableHeader>
                {sportRecords.map((pr) => (
                  <PRTableRow key={pr.id}>
                    <PRDistanceCell>
                      <PRDistanceBadge>
                        {pr.distance >= 1 ? `${pr.distance.toFixed(pr.distance < 10 ? 1 : 0)}` : pr.distance}
                        <small> km</small>
                      </PRDistanceBadge>
                      <PRDistanceLabel>{pr.distanceDisplayName}</PRDistanceLabel>
                    </PRDistanceCell>
                    <PRTableCell>
                      <PRTimeValue>{pr.formattedTime}</PRTimeValue>
                    </PRTableCell>
                    <PRTableCell>
                      <PRPaceValue>{pr.formattedPace || "—"}</PRPaceValue>
                    </PRTableCell>
                    <PRTableCell>
                      {pr.improvementSeconds && pr.improvementSeconds > 0 ? (
                        <PRImprovementBadge>
                          −{pr.improvementSeconds}s ({pr.improvementPercent?.toFixed(1)}%)
                        </PRImprovementBadge>
                      ) : (
                        <span style={{ color: "#94a3b8", fontSize: "13px" }}>—</span>
                      )}
                    </PRTableCell>
                    <PRTableCell>
                      <PRDateLink as={Link} to={`/activities/${pr.activityId}`}>
                        {formatDate(pr.achievedAt)}
                      </PRDateLink>
                    </PRTableCell>
                  </PRTableRow>
                ))}
              </PRTable>
            </PRSportSection>
          );
        })
      )}
    </PRContainer>
  );
};

export default PersonalRecords;
