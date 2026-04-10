import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { FiAward, FiRefreshCw } from "react-icons/fi";
import PersonalRecordService from "../../api/PersonalRecordService";
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

const SPORT_META = {
  RUN: { icon: "🏃", label: "Running" },
  RIDE: { icon: "🚴", label: "Cycling" },
  SWIM: { icon: "🏊", label: "Swimming" },
  HIKE: { icon: "⛰️", label: "Hiking" },
  WALK: { icon: "🚶", label: "Walking" },
  WORKOUT: { icon: "💪", label: "Workout" },
};

const DISTANCE_ORDER = ["ONE_KM", "FIVE_KM", "TEN_KM", "HALF_MARATHON", "MARATHON"];

const formatDate = (iso) => {
  if (!iso) return "";
  const date = new Date(iso);
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
};

const PersonalRecords = ({ userId, isOwnProfile = false }) => {
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
      setError("Failed to load personal records");
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
      setError("Failed to recalculate personal records");
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
    return <PRLoadingState>Loading personal records...</PRLoadingState>;
  }

  return (
    <PRContainer>
      <PRHeader>
        <PRTitleGroup>
          <PRTitle>
            <FiAward size={20} /> Personal Records
          </PRTitle>
          <PRSubtitle>
            Your fastest times for standard distances across each sport
          </PRSubtitle>
        </PRTitleGroup>
        {isOwnProfile && records.length > 0 && (
          <RecalculateButton onClick={handleRecalculate} disabled={recalculating}>
            <FiRefreshCw size={14} className={recalculating ? "spinning" : ""} />
            {recalculating ? "Recalculating..." : "Recalculate"}
          </RecalculateButton>
        )}
      </PRHeader>

      {error && <PREmptyText style={{ color: "#c00" }}>{error}</PREmptyText>}

      {records.length === 0 ? (
        <PREmptyState>
          <PREmptyIcon>🏆</PREmptyIcon>
          <PREmptyText>
            {isOwnProfile
              ? "No personal records yet. Complete an activity covering at least 1km to set your first PR!"
              : "This user hasn't set any personal records yet."}
          </PREmptyText>
          {isOwnProfile && (
            <RecalculateButton onClick={handleRecalculate} disabled={recalculating}>
              <FiRefreshCw size={14} className={recalculating ? "spinning" : ""} />
              {recalculating ? "Recalculating..." : "Recalculate from history"}
            </RecalculateButton>
          )}
        </PREmptyState>
      ) : (
        Object.keys(groupedBySport).map((sport) => {
          const meta = SPORT_META[sport] || { icon: "🏅", label: sport };
          const sportRecords = groupedBySport[sport];
          return (
            <PRSportSection key={sport}>
              <PRSportHeader>
                <PRSportIcon>{meta.icon}</PRSportIcon>
                <PRSportName>{meta.label}</PRSportName>
                <PRSportCount>
                  {sportRecords.length} PR{sportRecords.length === 1 ? "" : "s"}
                </PRSportCount>
              </PRSportHeader>
              <PRTable>
                <PRTableHeader>
                  <PRTableHeaderCell>Distance</PRTableHeaderCell>
                  <PRTableHeaderCell>Best Time</PRTableHeaderCell>
                  <PRTableHeaderCell>Pace</PRTableHeaderCell>
                  <PRTableHeaderCell>Improvement</PRTableHeaderCell>
                  <PRTableHeaderCell>Date</PRTableHeaderCell>
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
