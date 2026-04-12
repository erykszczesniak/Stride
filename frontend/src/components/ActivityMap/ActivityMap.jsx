import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { MapContainer, TileLayer, Polyline, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { parseRouteCoordinates } from "../../utils/polyline";
import {
  MapWrapper,
  EmptyMapState,
  EmptyMapIcon,
  EmptyMapText,
} from "./ActivityMapStyles";

// Fix for default marker icons in React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

// Custom start marker icon
const startIcon = new L.DivIcon({
  className: "custom-marker",
  html: `<div style="
    width: 24px;
    height: 24px;
    background: #22c55e;
    border: 3px solid white;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(0,0,0,0.3);
  "></div>`,
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

// Custom end marker icon
const endIcon = new L.DivIcon({
  className: "custom-marker",
  html: `<div style="
    width: 24px;
    height: 24px;
    background: #ef4444;
    border: 3px solid white;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(0,0,0,0.3);
  "></div>`,
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

// Component to fit map bounds to the route
const FitBounds = ({ coordinates }) => {
  const map = useMap();

  useEffect(() => {
    if (coordinates && coordinates.length > 0) {
      const bounds = L.latLngBounds(coordinates);
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [coordinates, map]);

  return null;
};

const ActivityMap = ({ coordinates, height = "400px", showMarkers = true }) => {
  const { t } = useTranslation();
  // Accepts either:
  //  - a JSON-stringified [[lat,lng],...] array (legacy manual activities)
  //  - a Google Encoded Polyline string (imported GPX/TCX/FIT activities)
  //  - an already-parsed array
  const parsedCoordinates = parseRouteCoordinates(coordinates);

  const hasValidCoordinates =
    parsedCoordinates.length > 0 &&
    parsedCoordinates.every(
      (coord) =>
        Array.isArray(coord) &&
        coord.length >= 2 &&
        typeof coord[0] === "number" &&
        typeof coord[1] === "number"
    );

  if (!hasValidCoordinates) {
    return (
      <EmptyMapState $height={height}>
        <EmptyMapIcon>🗺️</EmptyMapIcon>
        <EmptyMapText>{t("errors.noGpsData")}</EmptyMapText>
      </EmptyMapState>
    );
  }

  // Convert coordinates to [lat, lng] format if needed
  // Assuming coordinates are in [lat, lng] format
  const routePositions = parsedCoordinates;
  const startPosition = routePositions[0];
  const endPosition = routePositions[routePositions.length - 1];

  // Calculate center
  const center = routePositions[Math.floor(routePositions.length / 2)];

  return (
    <MapWrapper $height={height}>
      <MapContainer
        center={center}
        zoom={13}
        scrollWheelZoom={true}
        style={{ height: "100%", width: "100%" }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Polyline
          positions={routePositions}
          pathOptions={{
            color: "#fc4c02",
            weight: 4,
            opacity: 0.9,
            lineCap: "round",
            lineJoin: "round",
          }}
        />
        {showMarkers && (
          <>
            <Marker position={startPosition} icon={startIcon} />
            <Marker position={endPosition} icon={endIcon} />
          </>
        )}
        <FitBounds coordinates={routePositions} />
      </MapContainer>
    </MapWrapper>
  );
};

export default ActivityMap;
