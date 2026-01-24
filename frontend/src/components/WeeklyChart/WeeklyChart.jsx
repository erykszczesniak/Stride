import React from "react";
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

const defaultData = [
  { dayName: "Mon", distance: 0, duration: 0, activities: 0 },
  { dayName: "Tue", distance: 0, duration: 0, activities: 0 },
  { dayName: "Wed", distance: 0, duration: 0, activities: 0 },
  { dayName: "Thu", distance: 0, duration: 0, activities: 0 },
  { dayName: "Fri", distance: 0, duration: 0, activities: 0 },
  { dayName: "Sat", distance: 0, duration: 0, activities: 0 },
  { dayName: "Sun", distance: 0, duration: 0, activities: 0 },
];

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div style={{
        backgroundColor: "white",
        padding: "12px",
        borderRadius: "8px",
        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
        border: "none",
      }}>
        <p style={{ fontWeight: 600, marginBottom: 8 }}>{label}</p>
        <p style={{ color: "#fc4c02", fontSize: 14 }}>
          Distance: {payload[0]?.value?.toFixed(2)} km
        </p>
        {payload[1] && (
          <p style={{ color: "#666", fontSize: 14 }}>
            Duration: {Math.floor(payload[1].value / 60)}min
          </p>
        )}
      </div>
    );
  }
  return null;
};

const WeeklyChart = ({ data, trendPercentage }) => {
  const chartData = data || defaultData;
  const isPositive = trendPercentage && !trendPercentage.startsWith("-");

  return (
    <ChartContainer>
      <ChartHeader>
        <ChartTitle>Weekly Activity</ChartTitle>
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
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" vertical={false} />
            <XAxis
              dataKey="dayName"
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 12, fill: "#666" }}
            />
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 12, fill: "#666" }}
              tickFormatter={(value) => `${value}`}
            />
            <Tooltip content={<CustomTooltip />} />
            <Bar
              dataKey="distance"
              fill="#fc4c02"
              radius={[4, 4, 0, 0]}
              maxBarSize={40}
            />
          </BarChart>
        </ResponsiveContainer>
      </ChartWrapper>

      <ChartLegend>
        <LegendItem>
          <LegendDot color="#fc4c02" />
          Distance (km)
        </LegendItem>
      </ChartLegend>
    </ChartContainer>
  );
};

export default WeeklyChart;
