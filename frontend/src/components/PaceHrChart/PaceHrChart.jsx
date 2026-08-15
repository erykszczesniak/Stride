import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { FiActivity } from "react-icons/fi";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useUnits } from "../../context/UnitsContext";
import {
  ChartHeader,
  ChartSection,
  ChartTitle,
  ChartWrapper,
} from "./PaceHrChartStyles";

/**
 * Pace per the pipeline contract: sec/km (run), km/h (bike), sec/100m (swim).
 * "4:44 /km", "22.6 km/h", "2:20 /100m".
 */
export const formatPaceValue = (discipline, value) => {
  if (value == null || Number.isNaN(value)) return "";
  if (discipline === "bike") return `${Number(value).toFixed(1)} km/h`;
  const total = Math.round(value);
  const minutes = Math.floor(total / 60);
  const seconds = String(total % 60).padStart(2, "0");
  return `${minutes}:${seconds} ${discipline === "swim" ? "/100m" : "/km"}`;
};

/** Axis tick — the compact form without the unit suffix. */
export const formatPaceTick = (discipline, value) => {
  if (value == null || Number.isNaN(value)) return "";
  if (discipline === "bike") return `${Math.round(value)}`;
  const total = Math.round(value);
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
};

/**
 * PaceHrChart — the iOS detail chart on the web: pace and heart-rate series
 * from the pipeline facade over distance. Renders nothing when the pipeline
 * has no detail for the activity (non-triathlon types) or the series are
 * empty, so the parent can drop it in unguarded.
 */
const PaceHrChart = ({ detail, totalDistanceKm }) => {
  const { t } = useTranslation();
  const { convertDistance, distanceUnit } = useUnits();

  const discipline = detail?.activity?.discipline;
  const { chartData, hasPace, hasHr } = useMemo(() => {
    const pace = detail?.paceSeries || [];
    const heart = detail?.heartRateSeries || [];
    const paceOk = pace.filter((v) => v > 0).length > 1;
    const hrOk = heart.filter((v) => v > 0).length > 1;
    const count = Math.max(pace.length, heart.length);
    if (count < 2 || (!paceOk && !hrOk)) {
      return { chartData: [], hasPace: false, hasHr: false };
    }
    const totalInUserUnits = convertDistance(totalDistanceKm || 0);
    const data = Array.from({ length: count }, (_, i) => ({
      distance: (i / (count - 1)) * totalInUserUnits,
      pace: paceOk ? pace[i] : undefined,
      hr: hrOk ? heart[i] : undefined,
    }));
    return { chartData: data, hasPace: paceOk, hasHr: hrOk };
  }, [detail, totalDistanceKm, convertDistance]);

  if (chartData.length === 0) return null;

  const tooltipFormatter = (value, name) => {
    if (name === t("activities.details.paceHr.pace")) {
      return [formatPaceValue(discipline, value), name];
    }
    return [`${Math.round(value)} bpm`, name];
  };

  return (
    <ChartSection>
      <ChartHeader>
        <ChartTitle>
          <FiActivity size={20} />
          {t("activities.details.paceHr.title")}
        </ChartTitle>
      </ChartHeader>
      <ChartWrapper>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 5, right: 8, left: -6, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
            <XAxis
              dataKey="distance"
              type="number"
              domain={[0, "dataMax"]}
              tickFormatter={(v) => `${Number(v).toFixed(0)}`}
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 11, fill: "#94a3b8" }}
              unit={` ${distanceUnit}`}
            />
            {hasPace && (
              <YAxis
                yAxisId="pace"
                // Seconds-based paces read faster-is-up, like every sports app.
                reversed={discipline !== "bike"}
                domain={["auto", "auto"]}
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 11, fill: "#94a3b8" }}
                tickFormatter={(v) => formatPaceTick(discipline, v)}
                width={46}
              />
            )}
            {hasHr && (
              <YAxis
                yAxisId="hr"
                orientation="right"
                domain={["auto", "auto"]}
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 11, fill: "#94a3b8" }}
                width={40}
              />
            )}
            <Tooltip
              formatter={tooltipFormatter}
              labelFormatter={(value) => `${Number(value).toFixed(2)} ${distanceUnit}`}
              contentStyle={{
                borderRadius: 8,
                border: "none",
                boxShadow: "0 4px 20px rgba(0, 0, 0, 0.12)",
                fontSize: 12,
              }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            {hasPace && (
              <Line
                yAxisId="pace"
                dataKey="pace"
                name={t("activities.details.paceHr.pace")}
                stroke="#fc4c02"
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
            )}
            {hasHr && (
              <Line
                yAxisId="hr"
                dataKey="hr"
                name={t("activities.details.paceHr.hr")}
                stroke="#ef4444"
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
            )}
          </LineChart>
        </ResponsiveContainer>
      </ChartWrapper>
    </ChartSection>
  );
};

export default PaceHrChart;
