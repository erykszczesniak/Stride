import React from "react";
import { useTranslation } from "react-i18next";
import {
  GaugeCard,
  GaugeHeader,
  GaugeSvgWrap,
  GaugeTitle,
  NumberCell,
  NumberLabel,
  NumbersRow,
  NumberValue,
  StatusWord,
} from "./FormGaugeStyles";

const clamp = (value, low, high) => Math.min(high, Math.max(low, value));

// Same thresholds the iOS dial uses: TSB > 5 Fresh, < -10 Fatigued.
export const formStateFor = (tsb) => {
  if (tsb > 5) return "fresh";
  if (tsb < -10) return "fatigued";
  return "neutral";
};

/**
 * Training-form card (CTL fitness / ATL fatigue / TSB form) mirroring the
 * iOS dashboard dial: a -30..+30 semicircle gauge with the needle at TSB.
 */
const FormGauge = ({ form }) => {
  const { t } = useTranslation();
  if (!form) return null;

  const { ctl, atl, tsb } = form;
  const state = formStateFor(tsb);

  // -30 maps to 180°, +30 to 0° on the semicircle.
  const angle = ((clamp(tsb, -30, 30) + 30) / 60) * Math.PI;
  const cx = 100;
  const cy = 90;
  const needleX = cx - Math.cos(angle) * 62;
  const needleY = cy - Math.sin(angle) * 62;

  return (
    <GaugeCard data-testid="form-gauge">
      <GaugeHeader>
        <GaugeTitle>{t("dashboard.form.title")}</GaugeTitle>
        <StatusWord $state={state}>{t(`dashboard.form.${state}`)}</StatusWord>
      </GaugeHeader>
      <GaugeSvgWrap>
        <svg
          viewBox="0 0 200 104"
          role="img"
          aria-label={t("dashboard.form.gaugeAlt", { tsb })}
        >
          <path
            d="M 30 90 A 70 70 0 0 1 170 90"
            fill="none"
            stroke="currentColor"
            strokeOpacity="0.15"
            strokeWidth="10"
            strokeLinecap="round"
          />
          <line
            x1={cx}
            y1={cy}
            x2={needleX}
            y2={needleY}
            stroke="currentColor"
            strokeWidth="4"
            strokeLinecap="round"
          />
          <circle cx={cx} cy={cy} r="5" fill="currentColor" />
          <text x="24" y="102" fontSize="10" fill="currentColor" opacity="0.6">
            -30
          </text>
          <text x="97" y="12" fontSize="10" fill="currentColor" opacity="0.6">
            0
          </text>
          <text x="160" y="102" fontSize="10" fill="currentColor" opacity="0.6">
            +30
          </text>
        </svg>
      </GaugeSvgWrap>
      <NumbersRow>
        <NumberCell>
          <NumberValue>{ctl}</NumberValue>
          <NumberLabel>{t("dashboard.form.ctl")}</NumberLabel>
        </NumberCell>
        <NumberCell>
          <NumberValue>{atl}</NumberValue>
          <NumberLabel>{t("dashboard.form.atl")}</NumberLabel>
        </NumberCell>
        <NumberCell>
          <NumberValue>{tsb > 0 ? `+${tsb}` : tsb}</NumberValue>
          <NumberLabel>{t("dashboard.form.tsb")}</NumberLabel>
        </NumberCell>
      </NumbersRow>
    </GaugeCard>
  );
};

export default FormGauge;
