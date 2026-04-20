import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { FiTrendingUp } from "react-icons/fi";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { useUnits } from "../../context/UnitsContext";
import {
  ProfileSection,
  ProfileHeader,
  ProfileTitle,
  ProfileSummary,
  GainValue,
  LossValue,
  ChartWrapper,
} from "./ElevationProfileStyles";

/**
 * ElevationProfile — altitude series chart for an activity.
 *
 * @param {number[]|null} elevations - per-point altitudes in METERS. The series
 *   is assumed to be evenly spaced along the route; x-axis positions are derived
 *   from the point index and totalDistanceKm.
 * @param {number} totalDistanceKm - total activity distance in km, used to label
 *   the x-axis. Converted to mi when the user has imperial units on.
 *
 * Renders null (no DOM at all) when there's insufficient data (< 2 points or
 * the caller passed null/undefined). This lets the parent just drop the
 * component in without guarding — legacy activities without a stored profile
 * produce no UI.
 */
const ElevationProfile = ({ elevations, totalDistanceKm }) => {
  const { t } = useTranslation();
  const {
    isImperial,
    convertDistance,
    convertElevation,
    distanceUnit,
    elevationUnit,
  } = useUnits();

  // Build the chart data + gain/loss summary in one pass. useMemo so we don't
  // recompute on every parent re-render — elevation arrays can be hundreds of
  // points long after a GPX import.
  const { chartData, gain, loss } = useMemo(() => {
    if (!elevations || elevations.length < 2) {
      return { chartData: [], gain: 0, loss: 0 };
    }

    // Compute gain/loss in METERS from the raw series, then convert for display.
    // Doing the diff in native units keeps rounding error out of the totals.
    let gainMeters = 0;
    let lossMeters = 0;
    for (let i = 1; i < elevations.length; i++) {
      const delta = elevations[i] - elevations[i - 1];
      if (delta > 0) gainMeters += delta;
      else lossMeters += Math.abs(delta);
    }

    const totalDistInUserUnits = convertDistance(totalDistanceKm || 0);
    const stepCount = Math.max(elevations.length - 1, 1);

    const data = elevations.map((m, i) => ({
      // Distance is interpolated evenly. We don't have per-point distance here
      // (polyline encodes only lat/lon) so linear spacing is the best we can do.
      distance: (i / stepCount) * totalDistInUserUnits,
      elevation: convertElevation(m),
    }));

    return {
      chartData: data,
      gain: Math.round(convertElevation(gainMeters)),
      loss: Math.round(convertElevation(lossMeters)),
    };
  }, [elevations, totalDistanceKm, convertDistance, convertElevation]);

  // Null render when there's nothing useful to show. Deliberately no "empty
  // state" card — activities without GPS routes don't show the map either, so
  // showing an empty elevation card would be noise.
  if (!elevations || elevations.length < 2) {
    return null;
  }

  // Tooltip formatter: show distance + altitude in the user's units.
  const tooltipFormatter = (value, name) => {
    if (name === "elevation") {
      return [`${Math.round(value)} ${elevationUnit}`, t("activities.stats.elevation")];
    }
    return [value, name];
  };
  const tooltipLabelFormatter = (value) => {
    const decimals = isImperial ? 2 : 2;
    return `${Number(value).toFixed(decimals)} ${distanceUnit}`;
  };

  return (
    <ProfileSection>
      <ProfileHeader>
        <ProfileTitle>
          <FiTrendingUp size={20} />
          {t("activities.details.elevationProfile.title")}
        </ProfileTitle>
        <ProfileSummary>
          <span>
            {t("activities.details.elevationProfile.gain")}:{" "}
            <GainValue>
              +{gain} {elevationUnit}
            </GainValue>
          </span>
          <span>
            {t("activities.details.elevationProfile.loss")}:{" "}
            <LossValue>
              -{loss} {elevationUnit}
            </LossValue>
          </span>
        </ProfileSummary>
      </ProfileHeader>

      <ChartWrapper>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart
            data={chartData}
            margin={{ top: 5, right: 12, left: -12, bottom: 0 }}
          >
            <defs>
              {/* Brand-orange gradient fill: solid at the top, transparent at the baseline.
                  Recharts re-renders the <defs> each frame, which is fine — a single
                  <linearGradient> is cheap. */}
              <linearGradient id="elevationGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#fc4c02" stopOpacity={0.85} />
                <stop offset="100%" stopColor="#fc4c02" stopOpacity={0} />
              </linearGradient>
            </defs>
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
            <YAxis
              axisLine={false}
              tickLine={false}
              tick={{ fontSize: 11, fill: "#94a3b8" }}
              tickFormatter={(v) => `${Math.round(v)}`}
              unit={` ${elevationUnit}`}
              width={60}
            />
            <Tooltip
              formatter={tooltipFormatter}
              labelFormatter={tooltipLabelFormatter}
              contentStyle={{
                borderRadius: 8,
                border: "none",
                boxShadow: "0 4px 20px rgba(0, 0, 0, 0.12)",
                fontSize: 12,
              }}
            />
            <Area
              type="monotone"
              dataKey="elevation"
              stroke="#fc4c02"
              strokeWidth={2}
              fill="url(#elevationGradient)"
              isAnimationActive={false}
            />
          </AreaChart>
        </ResponsiveContainer>
      </ChartWrapper>
    </ProfileSection>
  );
};

export default ElevationProfile;
