import React from "react";
import { useTranslation } from "react-i18next";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { FiTrendingUp, FiTrendingDown } from "react-icons/fi";
import {
  ChartContainer,
  ChartHeader,
  ChartTitle,
  TrendBadge,
  ChartWrapper,
  ChartLegend,
  LegendItem,
  LegendDot,
} from "./WeeklyChartStyles";

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div style={{
        backgroundColor: "white",
        padding: "12px 16px",
        borderRadius: "12px",
        boxShadow: "0 4px 20px rgba(0, 0, 0, 0.12)",
        border: "none",
      }}>
        <p style={{ fontWeight: 600, marginBottom: 8, color: "#1a1a2e", fontSize: 13 }}>{label}</p>
        <p style={{ color: "#fc4c02", fontSize: 14, fontWeight: 600 }}>
          {payload[0]?.value?.toFixed(2)} km
        </p>
        {payload[1] && (
          <p style={{ color: "#64748b", fontSize: 12, marginTop: 4 }}>
            {Math.floor(payload[1].value / 60)} min
          </p>
        )}
      </div>
    );
  }
  return null;
};

const WeeklyChart = ({ data, trendPercentage }) => {
  const { t } = useTranslation();
  const defaultData = [
    { dayName: "Mon", distance: 0, duration: 0, activities: 0 },
    { dayName: "Tue", distance: 0, duration: 0, activities: 0 },
    { dayName: "Wed", distance: 0, duration: 0, activities: 0 },
    { dayName: "Thu", distance: 0, duration: 0, activities: 0 },
    { dayName: "Fri", distance: 0, duration: 0, activities: 0 },
    { dayName: "Sat", distance: 0, duration: 0, activities: 0 },
    { dayName: "Sun", distance: 0, duration: 0, activities: 0 },
  ];
  const chartData = data || defaultData;
  const isPositive = trendPercentage && !trendPercentage.startsWith("-");

  return (
    <ChartContainer>
      <ChartHeader>
        <ChartTitle>{t("dashboard.weeklyActivity")}</ChartTitle>
        {trendPercentage && (
          <TrendBadge positive={isPositive}>
            {isPositive ? <FiTrendingUp size={14} /> : <FiTrendingDown size={14} />}
            {trendPercentage}
          </TrendBadge>
        )}
      </ChartHeader>

      <ChartWrapper>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
            <XAxis
              dataKey="dayName"
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 12, fill: "#94a3b8", fontWeight: 500 }}
            />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 12, fill: "#94a3b8" }}
              tickFormatter={(value) => `${value}`}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: "#f8fafc" }} />
            <Bar
              dataKey="distance"
              fill="url(#colorGradient)"
              radius={[6, 6, 0, 0]}
              maxBarSize={36}
            />
            <defs>
              <linearGradient id="colorGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#fc4c02" />
                <stop offset="100%" stopColor="#ff6b35" />
              </linearGradient>
            </defs>
          </BarChart>
        </ResponsiveContainer>
      </ChartWrapper>

      <ChartLegend>
        <LegendItem>
          <LegendDot color="#fc4c02" />
          {t("dashboard.distanceLabel")}
        </LegendItem>
      </ChartLegend>
    </ChartContainer>
  );
};

export default WeeklyChart;
