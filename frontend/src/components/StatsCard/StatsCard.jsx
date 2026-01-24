import React from "react";
import { FiTrendingUp, FiTrendingDown } from "react-icons/fi";
import {
  Card,
  IconWrapper,
  Content,
  Label,
  Value,
  Trend,
} from "./StatsCardStyles";

const StatsCard = ({ icon, label, value, trend, bgColor, iconColor }) => {
  const isPositive = trend && !trend.startsWith("-");

  return (
    <Card>
      <IconWrapper bgColor={bgColor} iconColor={iconColor}>
        {icon}
      </IconWrapper>
      <Content>
        <Label>{label}</Label>
        <Value>{value}</Value>
        {trend && (
          <Trend positive={isPositive}>
            {isPositive ? <FiTrendingUp size={14} /> : <FiTrendingDown size={14} />}
            {trend} vs last month
          </Trend>
        )}
      </Content>
    </Card>
  );
};

export default StatsCard;
