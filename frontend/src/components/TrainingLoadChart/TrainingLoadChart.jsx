import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
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
import {
  ChartContainer,
  ChartHeader,
  ChartTitle,
  ChartWrapper,
} from "../WeeklyChart/WeeklyChartStyles";
import { formatWeekRange } from "../WeeklyVolume/WeeklyVolume";

export const LOAD_COLORS = {
  ctl: "#fc4c02",
  atl: "#94a3b8",
};

// Same frame rules as WeeklyVolume: no negative left margin (it clips the
// Y-axis labels), no X-axis tick labels — the tooltip names the week the
// point closes (the pipeline samples the quarter range weekly).
export const LOAD_CHART_MARGIN = { top: 5, right: 4, left: 0, bottom: 0 };

/** Pipeline ctl/atl point series → recharts rows keyed by the trailing week. */
export const toTrainingLoadData = (ctl, atl) => {
  const fitness = ctl || [];
  const fatigue = atl || [];
  const count = Math.max(fitness.length, fatigue.length);
  return Array.from({ length: count }, (_, i) => {
    const point = fitness[i] || fatigue[i];
    const end = new Date(point.date);
    const start = new Date(end);
    start.setDate(start.getDate() - 7);
    return {
      date: formatWeekRange(start),
      ctl: fitness[i] ? Math.round(fitness[i].value) : undefined,
      atl: fatigue[i] ? Math.round(fatigue[i].value) : undefined,
    };
  });
};

/**
 * TrainingLoadChart — the iOS Load tab's FITNESS/FATIGUE card on the web:
 * CTL and ATL over the last quarter, from the pipeline facade. Renders
 * nothing without data.
 */
const TrainingLoadChart = ({ ctl, atl }) => {
  const { t } = useTranslation();
  const data = useMemo(() => toTrainingLoadData(ctl, atl), [ctl, atl]);

  if (data.length < 2) return null;

  return (
    <ChartContainer>
      <ChartHeader>
        <ChartTitle>{t("dashboard.trainingLoad.title")}</ChartTitle>
      </ChartHeader>
      <ChartWrapper>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={LOAD_CHART_MARGIN}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
            <XAxis dataKey="date" axisLine={false} tickLine={false} tick={false} height={8} />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 10, fill: "#94a3b8" }}
              width={36}
            />
            <Tooltip
              contentStyle={{
                borderRadius: 8,
                border: "none",
                boxShadow: "0 4px 20px rgba(0, 0, 0, 0.12)",
                fontSize: 12,
              }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Line
              dataKey="ctl"
              name={t("dashboard.form.ctl")}
              stroke={LOAD_COLORS.ctl}
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
            <Line
              dataKey="atl"
              name={t("dashboard.form.atl")}
              stroke={LOAD_COLORS.atl}
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartWrapper>
    </ChartContainer>
  );
};

export default TrainingLoadChart;
