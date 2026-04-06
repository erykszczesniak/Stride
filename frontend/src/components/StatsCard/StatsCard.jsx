import React from "react";
import { FiTrendingUp, FiTrendingDown } from "react-icons/fi";
import {
  Card,
  CardHeader,
  IconWrapper,
  TrendBadge,
  Value,
  ValueNumber,
  ValueUnit,
  Label,
} from "./StatsCardStyles";

const StatsCard = ({ icon, label, value, unit, trend, bgColor, iconColor }) => {
  const isPositive = trend > 0;
  const TrendIcon = isPositive ? FiTrendingUp : FiTrendingDown;

  return (
    <Card>
      <CardHeader>
        <IconWrapper bgColor={bgColor} iconColor={iconColor}>
          {icon}
        </IconWrapper>
        {trend != null && trend !== 0 && (
          <TrendBadge $isPositive={isPositive}>
            <TrendIcon size={12} />
            {isPositive ? `+${trend}` : trend}%
          </TrendBadge>
        )}
      </CardHeader>
      <Label>{label}</Label>
      <Value>
        <ValueNumber>{value}</ValueNumber>
        {unit && <ValueUnit>{unit}</ValueUnit>}
      </Value>
    </Card>
  );
};

export default React.memo(StatsCard);
