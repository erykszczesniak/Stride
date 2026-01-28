import React from "react";
import { FiTrendingUp } from "react-icons/fi";
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
  return (
    <Card>
      <CardHeader>
        <IconWrapper bgColor={bgColor} iconColor={iconColor}>
          {icon}
        </IconWrapper>
        {trend && (
          <TrendBadge>
            <FiTrendingUp size={12} />
            +{trend}%
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

export default StatsCard;
