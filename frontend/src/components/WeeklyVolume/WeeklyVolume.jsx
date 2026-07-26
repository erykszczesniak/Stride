import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  ChartContainer,
  ChartHeader,
  ChartLegend,
  ChartTitle,
  ChartWrapper,
  LegendDot,
  LegendItem,
} from "../WeeklyChart/WeeklyChartStyles";

// The web's own sport palette (ActivityCard uses the same hues).
export const VOLUME_COLORS = {
  swim: "#06b6d4",
  bike: "#3b82f6",
  run: "#fc4c02",
};

// Chart frame shared with TrainingLoadChart: no negative left margin (it
// clips the Y-axis labels) and every week labelled on the X axis, angled so
// twelve ticks fit — mirrors the iOS Load-tab chart.
export const VOLUME_CHART_MARGIN = { top: 5, right: 4, left: 0, bottom: 0 };
export const VOLUME_X_AXIS = { interval: 0, angle: -40, textAnchor: "end", height: 34 };

/** Pipeline weeklyVolumes → recharts rows with a short week label. */
export const toWeeklyVolumeData = (weeklyVolumes) =>
  (weeklyVolumes || []).map((week) => ({
    week: new Date(week.weekStart).toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
    }),
    swim: week.swimKm ?? 0,
    bike: week.bikeKm ?? 0,
    run: week.runKm ?? 0,
  }));

/**
 * WeeklyVolume — the iOS Load-tab chart on the web dashboard: one stacked
 * bar per week (last 12), split by discipline. Fed by the pipeline facade;
 * renders nothing without data so the dashboard degrades gracefully.
 */
const WeeklyVolume = ({ weeklyVolumes }) => {
  const { t } = useTranslation();
  const data = useMemo(() => toWeeklyVolumeData(weeklyVolumes), [weeklyVolumes]);

  if (data.length === 0) return null;

  const sports = ["swim", "bike", "run"];
  const tooltipFormatter = (value, name) => [
    `${Number(value).toFixed(1)} km`,
    t(`dashboard.weeklyVolume.${name}`),
  ];

  return (
    <ChartContainer>
      <ChartHeader>
        <ChartTitle>{t("dashboard.weeklyVolume.title")}</ChartTitle>
        <ChartLegend>
          {sports.map((sport) => (
            <LegendItem key={sport}>
              <LegendDot color={VOLUME_COLORS[sport]} />
              {t(`dashboard.weeklyVolume.${sport}`)}
            </LegendItem>
          ))}
        </ChartLegend>
      </ChartHeader>
      <ChartWrapper>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={VOLUME_CHART_MARGIN}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
            <XAxis
              dataKey="week"
              interval={VOLUME_X_AXIS.interval}
              angle={VOLUME_X_AXIS.angle}
              textAnchor={VOLUME_X_AXIS.textAnchor}
              height={VOLUME_X_AXIS.height}
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 9, fill: "#94a3b8" }}
            />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 10, fill: "#94a3b8" }}
              unit=" km"
              width={56}
            />
            <Tooltip
              formatter={tooltipFormatter}
              contentStyle={{
                borderRadius: 8,
                border: "none",
                boxShadow: "0 4px 20px rgba(0, 0, 0, 0.12)",
                fontSize: 12,
              }}
            />
            <Bar dataKey="swim" stackId="volume" fill={VOLUME_COLORS.swim} isAnimationActive={false} />
            <Bar dataKey="bike" stackId="volume" fill={VOLUME_COLORS.bike} isAnimationActive={false} />
            <Bar
              dataKey="run"
              stackId="volume"
              fill={VOLUME_COLORS.run}
              radius={[2, 2, 0, 0]}
              isAnimationActive={false}
            />
          </BarChart>
        </ResponsiveContainer>
      </ChartWrapper>
    </ChartContainer>
  );
};

export default WeeklyVolume;
