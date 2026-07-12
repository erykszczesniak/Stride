import React, { useCallback, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { FiChevronLeft, FiChevronRight, FiX } from "react-icons/fi";
import {
  CloseButton,
  Counter,
  LightboxImage,
  NavButton,
  Overlay,
} from "./PhotoLightboxStyles";

/**
 * Fullscreen photo viewer for the activity gallery. Closes on overlay click
 * or Escape; ←/→ (and the side buttons) page through multiple photos.
 */
const PhotoLightbox = ({ photos, index, onClose, onNavigate }) => {
  const { t } = useTranslation();
  const count = photos.length;

  const goTo = useCallback(
    (next) => onNavigate(((next % count) + count) % count),
    [count, onNavigate],
  );

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === "Escape") onClose();
      if (count > 1 && event.key === "ArrowLeft") goTo(index - 1);
      if (count > 1 && event.key === "ArrowRight") goTo(index + 1);
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [index, count, goTo, onClose]);

  if (index == null || count === 0) return null;

  return (
    <Overlay onClick={onClose} data-testid="photo-lightbox">
      <CloseButton
        aria-label={t("activities.details.photoViewer.close")}
        onClick={onClose}
      >
        <FiX size={22} />
      </CloseButton>
      {count > 1 && (
        <NavButton
          $side="left"
          aria-label={t("activities.details.photoViewer.prev")}
          onClick={(event) => {
            event.stopPropagation();
            goTo(index - 1);
          }}
        >
          <FiChevronLeft size={26} />
        </NavButton>
      )}
      <LightboxImage
        src={photos[index]}
        alt={t("activities.details.photoAlt", { index: index + 1 })}
        onClick={(event) => event.stopPropagation()}
      />
      {count > 1 && (
        <>
          <NavButton
            $side="right"
            aria-label={t("activities.details.photoViewer.next")}
            onClick={(event) => {
              event.stopPropagation();
              goTo(index + 1);
            }}
          >
            <FiChevronRight size={26} />
          </NavButton>
          <Counter>
            {index + 1} / {count}
          </Counter>
        </>
      )}
    </Overlay>
  );
};

export default PhotoLightbox;
