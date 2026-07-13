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

export const LOAD_COLORS = {
  ctl: "#fc4c02",
  atl: "#94a3b8",
};

/** Pipeline ctl/atl point series → recharts rows keyed by a short date label. */
export const toTrainingLoadData = (ctl, atl) => {
  const fitness = ctl || [];
  const fatigue = atl || [];
  const count = Math.max(fitness.length, fatigue.length);
  return Array.from({ length: count }, (_, i) => {
    const point = fitness[i] || fatigue[i];
    return {
      date: new Date(point.date).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
      }),
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
          <LineChart data={data} margin={{ top: 5, right: 4, left: -24, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
            <XAxis
              dataKey="date"
              interval="preserveStartEnd"
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 10, fill: "#94a3b8" }}
            />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 10, fill: "#94a3b8" }}
              width={40}
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
