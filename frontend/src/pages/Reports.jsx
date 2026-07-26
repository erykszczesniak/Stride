import React, { useState, useCallback, useRef, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { FiDownload, FiFileText, FiCalendar, FiCheck, FiMenu, FiSave, FiFolder, FiX } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { useUnits } from "../context/UnitsContext";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import {
  Container,
  PageTitle,
  PageDescription,
  TopBar,
  ExportButtons,
  ExportButton,
  MainLayout,
  FieldsPanel,
  FieldsPanelTitle,
  FieldItem,
  FieldCheckbox,
  FieldLabel,
  ReportArea,
  DateBar,
  DateInput,
  DateLabel,
  Presets,
  PresetButton,
  ReportTable,
  TableHead,
  TableBody,
  Th,
  SortArrow,
  Td,
  SummaryRow,
  EmptyReport,
  DragHandle,
  DraggableFieldItem,
  FilterSelect,
  TemplateBar,
  TemplateButton,
  TemplateSelect,
  TemplateDeleteBtn,
  ModalOverlay,
  ModalContent,
  ModalTitle,
  ModalInput,
  ModalActions,
  ModalButton,
  SelectedFieldsList,
  SelectedFieldsTitle,
} from "./ReportsStyles";

const AVAILABLE_FIELD_KEYS = [
  "activityTitle",
  "activityType",
  "date",
  "distance",
  "duration",
  "elevationGain",
  "calories",
  "averageSpeed",
  "averageHeartRate",
  "pace",
  "likesCount",
  "commentsCount",
];

const DEFAULT_FIELDS = ["activityTitle", "activityType", "date", "distance", "duration", "calories"];

const TEMPLATES_KEY = "report-templates";

const loadTemplates = () => {
  try {
    const raw = localStorage.getItem(TEMPLATES_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const saveTemplates = (templates) => {
  localStorage.setItem(TEMPLATES_KEY, JSON.stringify(templates));
};

const formatDuration = (seconds) => {
  if (!seconds) return "-";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
};

// Fields needed to compute pace client-side (per sport + unit system)
const PACE_SOURCE_FIELDS = ["distance", "duration", "activityType"];

const formatValue = (key, value, unitsCtx, row) => {
  if (key === "pace" && unitsCtx && row) {
    return unitsCtx.formatPaceFromActivity(Number(row.distance), Number(row.duration), row.activityType);
  }
  if (value === null || value === undefined) return "-";
  switch (key) {
    case "distance":
      return unitsCtx
        ? `${unitsCtx.convertDistance(Number(value)).toFixed(1)} ${unitsCtx.distanceUnit}`
        : `${Number(value).toFixed(1)} km`;
    case "duration": return formatDuration(value);
    case "averageSpeed":
      return unitsCtx
        ? `${unitsCtx.convertSpeed(Number(value)).toFixed(1)} ${unitsCtx.speedUnit}`
        : `${Number(value).toFixed(1)} km/h`;
    case "elevationGain":
      return unitsCtx
        ? `${Math.round(unitsCtx.convertElevation(Number(value)))} ${unitsCtx.elevationUnit}`
        : `${value} m`;
    case "calories": return `${value} kcal`;
    default: return String(value);
  }
};

const getRawValue = (key, value, row) => {
  if (key === "pace") {
    const distance = Number(row?.distance);
    const duration = Number(row?.duration);
    return distance > 0 && duration > 0 ? duration / distance : Number.MAX_SAFE_INTEGER;
  }
  if (value === null || value === undefined) return 0;
  switch (key) {
    case "distance":
    case "averageSpeed":
      return Number(value);
    case "duration":
    case "elevationGain":
    case "calories":
    case "averageHeartRate":
    case "likesCount":
    case "commentsCount":
      return Number(value);
    case "date":
      return new Date(value).getTime() || 0;
    default:
      return String(value).toLowerCase();
  }
};

const Reports = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const toast = useToast();
  const unitsCtx = useUnits();

  const AVAILABLE_FIELDS = useMemo(
    () => AVAILABLE_FIELD_KEYS.map((key) => ({ key, label: t(`reports.fields.${key}`) })),
    [t]
  );

  const ACTIVITY_TYPES = useMemo(
    () => [
      { value: "", label: t("activities.filters.allTypes") },
      { value: "RUN", label: t("activities.typesShort.RUN") },
      { value: "RIDE", label: t("activities.typesShort.RIDE") },
      { value: "SWIM", label: t("activities.typesShort.SWIM") },
      { value: "HIKE", label: t("activities.typesShort.HIKE") },
      { value: "WALK", label: t("activities.typesShort.WALK") },
      { value: "WORKOUT", label: t("activities.typesShort.WORKOUT") },
    ],
    [t]
  );
  const [selectedFields, setSelectedFields] = useState(DEFAULT_FIELDS);
  const [rows, setRows] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);

  const today = new Date().toISOString().split("T")[0];
  const thirtyDaysAgo = new Date(Date.now() - 30 * 86400000).toISOString().split("T")[0];
  const [dateFrom, setDateFrom] = useState(thirtyDaysAgo);
  const [dateTo, setDateTo] = useState(today);

  // Activity type filter
  const [activityTypeFilter, setActivityTypeFilter] = useState("");

  // Sort state
  const [sortConfig, setSortConfig] = useState({ field: null, direction: null });

  // Drag and drop state
  const dragItem = useRef(null);
  const dragOverItem = useRef(null);
  const [draggingIndex, setDraggingIndex] = useState(null);
  const [dragOverIndex, setDragOverIndex] = useState(null);

  // Templates
  const [templates, setTemplates] = useState(loadTemplates);
  const [showSaveModal, setShowSaveModal] = useState(false);
  const [templateName, setTemplateName] = useState("");

  // Date preset name for templates
  const [activePreset, setActivePreset] = useState(null);

  const toggleField = (key) => {
    setSelectedFields((prev) =>
      prev.includes(key) ? prev.filter((f) => f !== key) : [...prev, key]
    );
  };

  const applyPreset = (days) => {
    const to = new Date();
    const from = new Date();
    if (days === "month") {
      from.setDate(1);
    } else if (days === "lastMonth") {
      from.setMonth(from.getMonth() - 1);
      from.setDate(1);
      to.setDate(0);
    } else if (days === "year") {
      from.setMonth(0, 1);
    } else {
      from.setDate(from.getDate() - days);
    }
    setDateFrom(from.toISOString().split("T")[0]);
    setDateTo(to.toISOString().split("T")[0]);
    setActivePreset(days);
  };

  // Drag and drop handlers
  const handleDragStart = (e, index) => {
    dragItem.current = index;
    setDraggingIndex(index);
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", index.toString());
  };

  const handleDragEnter = (index) => {
    dragOverItem.current = index;
    setDragOverIndex(index);
  };

  const handleDragEnd = () => {
    if (dragItem.current !== null && dragOverItem.current !== null && dragItem.current !== dragOverItem.current) {
      const newFields = [...selectedFields];
      const draggedItem = newFields[dragItem.current];
      newFields.splice(dragItem.current, 1);
      newFields.splice(dragOverItem.current, 0, draggedItem);
      setSelectedFields(newFields);
    }
    dragItem.current = null;
    dragOverItem.current = null;
    setDraggingIndex(null);
    setDragOverIndex(null);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
  };

  // Sort handler
  const handleSort = (field) => {
    setSortConfig((prev) => {
      if (prev.field !== field) return { field, direction: "asc" };
      if (prev.direction === "asc") return { field, direction: "desc" };
      if (prev.direction === "desc") return { field: null, direction: null };
      return { field, direction: "asc" };
    });
  };

  // Sorted rows
  const sortedRows = useMemo(() => {
    if (!sortConfig.field || !sortConfig.direction) return rows;
    const sorted = [...rows].sort((a, b) => {
      const aVal = getRawValue(sortConfig.field, a[sortConfig.field], a);
      const bVal = getRawValue(sortConfig.field, b[sortConfig.field], b);
      if (typeof aVal === "string" && typeof bVal === "string") {
        return sortConfig.direction === "asc" ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
      }
      return sortConfig.direction === "asc" ? aVal - bVal : bVal - aVal;
    });
    return sorted;
  }, [rows, sortConfig]);

  // Template operations
  const handleSaveTemplate = () => {
    if (!templateName.trim()) {
      toast.warning(t("reports.toast.templateNameRequired"));
      return;
    }
    const newTemplate = {
      id: Date.now(),
      name: templateName.trim(),
      selectedFields: [...selectedFields],
      datePreset: activePreset,
      activityType: activityTypeFilter,
    };
    const updated = [...templates, newTemplate];
    setTemplates(updated);
    saveTemplates(updated);
    setShowSaveModal(false);
    setTemplateName("");
    toast.success(t("reports.toast.templateSaved"));
  };

  const handleLoadTemplate = (e) => {
    const id = Number(e.target.value);
    if (!id) return;
    const tmpl = templates.find((tm) => tm.id === id);
    if (tmpl) {
      setSelectedFields(tmpl.selectedFields);
      if (tmpl.datePreset) applyPreset(tmpl.datePreset);
      if (tmpl.activityType !== undefined) setActivityTypeFilter(tmpl.activityType);
      toast.success(t("reports.toast.templateLoaded", { name: tmpl.name }));
    }
    e.target.value = "";
  };

  const handleDeleteTemplate = (id) => {
    const updated = templates.filter((tm) => tm.id !== id);
    setTemplates(updated);
    saveTemplates(updated);
    toast.success(t("reports.toast.templateDeleted"));
  };

  const generateReport = useCallback(async () => {
    if (selectedFields.length === 0) {
      toast.warning(t("reports.toast.selectField"));
      return;
    }
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      // Pace is formatted client-side per sport and unit system, so fetch its source fields too
      const queryFields = selectedFields.includes("pace")
        ? [...new Set([...selectedFields, ...PACE_SOURCE_FIELDS])]
        : selectedFields;
      const query = `
        query GenerateReport($dateFrom: String!, $dateTo: String!, $fields: [String!]!, $activityType: String) {
          generateReport(dateFrom: $dateFrom, dateTo: $dateTo, fields: $fields, activityType: $activityType) {
            rows {
              ${queryFields.join("\n              ")}
            }
            summary {
              totalDistance totalDuration totalCalories totalElevation totalActivities
              avgDistance avgDuration avgCalories
            }
          }
        }
      `;
      const res = await fetch("/graphql", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          query,
          variables: {
            dateFrom,
            dateTo,
            fields: queryFields,
            activityType: activityTypeFilter || null,
          },
        }),
      });
      const json = await res.json();
      if (json.errors) {
        toast.error(t("reports.toast.generateFailed"));
        return;
      }
      setRows(json.data.generateReport.rows);
      setSummary(json.data.generateReport.summary);
    } catch {
      toast.error(t("reports.toast.generateFailed"));
    } finally {
      setLoading(false);
    }
  }, [dateFrom, dateTo, selectedFields, activityTypeFilter, toast, t]);

  const exportCsv = async () => {
    try {
      const token = localStorage.getItem("token");
      const res = await fetch("/api/v1/export/report/csv", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          dateFrom,
          dateTo,
          fields: selectedFields,
          activityType: activityTypeFilter || null,
          units: unitsCtx.units,
        }),
      });
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `stride-report-${dateFrom}-${dateTo}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success(t("reports.toast.csvSuccess"));
    } catch {
      toast.error(t("reports.toast.csvFailed"));
    }
  };

  const exportPdf = () => {
    if (rows.length === 0) {
      toast.warning(t("reports.toast.generateFirst"));
      return;
    }
    const doc = new jsPDF({ orientation: "landscape" });
    doc.setFontSize(16);
    doc.text("Stride Activity Report", 14, 15);
    doc.setFontSize(10);
    doc.text(`${dateFrom} — ${dateTo} | ${user?.firstName} ${user?.lastName}`, 14, 22);

    const headers = selectedFields.map((f) => AVAILABLE_FIELDS.find((af) => af.key === f)?.label || f);
    const body = sortedRows.map((row) =>
      selectedFields.map((f) => {
        const formatted = formatValue(f, row[f], unitsCtx, row);
        // Pace units differ per sport, so keep them in the cell; "—" is not in jsPDF's standard fonts
        if (f === "pace") return formatted.replace("—", "-");
        return formatted.replace(/ km\/h| km| mi| mph| m| ft| kcal/g, "");
      })
    );

    autoTable(doc, {
      head: [headers],
      body,
      startY: 28,
      styles: { fontSize: 8 },
      headStyles: { fillColor: [252, 76, 2] },
    });

    doc.save(`stride-report-${dateFrom}-${dateTo}.pdf`);
    toast.success(t("reports.toast.pdfSuccess"));
  };

  return (
    <Layout>
      <Container>
        <TopBar>
          <div>
            <PageTitle>{t("reports.title")}</PageTitle>
            <PageDescription>{t("reports.description")}</PageDescription>
          </div>
          <ExportButtons>
            <ExportButton onClick={exportCsv}>
              <FiDownload size={16} /> CSV
            </ExportButton>
            <ExportButton onClick={exportPdf}>
              <FiFileText size={16} /> PDF
            </ExportButton>
          </ExportButtons>
        </TopBar>

        {/* Template bar */}
        <TemplateBar>
          <TemplateButton onClick={() => setShowSaveModal(true)}>
            <FiSave size={14} /> {t("reports.saveTemplate")}
          </TemplateButton>
          {templates.length > 0 && (
            <>
              <TemplateSelect onChange={handleLoadTemplate} defaultValue="">
                <option value="" disabled>{t("reports.loadTemplate")}</option>
                {templates.map((tmpl) => (
                  <option key={tmpl.id} value={tmpl.id}>{tmpl.name}</option>
                ))}
              </TemplateSelect>
              {templates.map((tmpl) => (
                <TemplateDeleteBtn key={tmpl.id} title={t("reports.toast.templateDelete", { name: tmpl.name })} onClick={() => handleDeleteTemplate(tmpl.id)}>
                  <FiX size={12} />
                </TemplateDeleteBtn>
              ))}
            </>
          )}
        </TemplateBar>

        <MainLayout>
          <FieldsPanel>
            <FieldsPanelTitle>{t("reports.availableFields")}</FieldsPanelTitle>
            {AVAILABLE_FIELDS.map((field) => (
              <FieldItem key={field.key} onClick={() => toggleField(field.key)}>
                <FieldCheckbox $checked={selectedFields.includes(field.key)}>
                  {selectedFields.includes(field.key) && <FiCheck size={12} />}
                </FieldCheckbox>
                <FieldLabel>{field.label}</FieldLabel>
              </FieldItem>
            ))}

            {/* Drag-and-drop reorderable selected fields */}
            {selectedFields.length > 0 && (
              <SelectedFieldsList>
                <SelectedFieldsTitle>{t("reports.columnOrder")}</SelectedFieldsTitle>
                {selectedFields.map((key, index) => {
                  const field = AVAILABLE_FIELDS.find((af) => af.key === key);
                  return (
                    <DraggableFieldItem
                      key={key}
                      draggable
                      onDragStart={(e) => handleDragStart(e, index)}
                      onDragEnter={() => handleDragEnter(index)}
                      onDragEnd={handleDragEnd}
                      onDragOver={handleDragOver}
                      onDrop={handleDragEnd}
                      $isDragging={draggingIndex === index}
                      $isDragOver={dragOverIndex === index}
                    >
                      <DragHandle>
                        <FiMenu size={14} />
                      </DragHandle>
                      <FieldLabel>{field?.label || key}</FieldLabel>
                    </DraggableFieldItem>
                  );
                })}
              </SelectedFieldsList>
            )}
          </FieldsPanel>

          <ReportArea>
            <DateBar>
              <div>
                <DateLabel><FiCalendar size={14} /> {t("reports.from")}</DateLabel>
                <DateInput type="date" value={dateFrom} onChange={(e) => { setDateFrom(e.target.value); setActivePreset(null); }} />
              </div>
              <div>
                <DateLabel><FiCalendar size={14} /> {t("reports.to")}</DateLabel>
                <DateInput type="date" value={dateTo} onChange={(e) => { setDateTo(e.target.value); setActivePreset(null); }} />
              </div>
              <div>
                <DateLabel><FiFolder size={14} /> {t("reports.activityType")}</DateLabel>
                <FilterSelect value={activityTypeFilter} onChange={(e) => setActivityTypeFilter(e.target.value)}>
                  {ACTIVITY_TYPES.map((at) => (
                    <option key={at.value} value={at.value}>{at.label}</option>
                  ))}
                </FilterSelect>
              </div>
              <Presets>
                <PresetButton onClick={() => applyPreset(7)}>{t("reports.presets.7days")}</PresetButton>
                <PresetButton onClick={() => applyPreset(30)}>{t("reports.presets.30days")}</PresetButton>
                <PresetButton onClick={() => applyPreset("month")}>{t("reports.presets.thisMonth")}</PresetButton>
                <PresetButton onClick={() => applyPreset("lastMonth")}>{t("reports.presets.lastMonth")}</PresetButton>
                <PresetButton onClick={() => applyPreset("year")}>{t("reports.presets.thisYear")}</PresetButton>
              </Presets>
              <ExportButton onClick={generateReport} disabled={loading} style={{ marginLeft: "auto" }}>
                {loading ? t("reports.generating") : t("reports.generateReport")}
              </ExportButton>
            </DateBar>

            {rows.length > 0 ? (
              <div style={{ overflowX: "auto" }}>
                <ReportTable>
                  <TableHead>
                    <tr>
                      {selectedFields.map((f) => (
                        <Th
                          key={f}
                          $active={sortConfig.field === f}
                          onClick={() => handleSort(f)}
                        >
                          {AVAILABLE_FIELDS.find((af) => af.key === f)?.label || f}
                          {sortConfig.field === f && sortConfig.direction && (
                            <SortArrow>{sortConfig.direction === "asc" ? "\u25B2" : "\u25BC"}</SortArrow>
                          )}
                        </Th>
                      ))}
                    </tr>
                  </TableHead>
                  <TableBody>
                    {sortedRows.map((row, i) => (
                      <tr key={i}>
                        {selectedFields.map((f) => (
                          <Td key={f}>{formatValue(f, row[f], unitsCtx, row)}</Td>
                        ))}
                      </tr>
                    ))}
                    {summary && (
                      <SummaryRow>
                        {selectedFields.map((f) => (
                          <Td key={f}>
                            {f === "activityTitle" ? t("reports.activitiesSuffix", { count: summary.totalActivities }) :
                             f === "distance" ? `${unitsCtx.convertDistance(summary.totalDistance).toFixed(1)} ${unitsCtx.distanceUnit}` :
                             f === "duration" ? formatDuration(summary.totalDuration) :
                             f === "calories" ? `${summary.totalCalories} kcal` :
                             f === "elevationGain" ? `${Math.round(unitsCtx.convertElevation(summary.totalElevation))} ${unitsCtx.elevationUnit}` :
                             ""}
                          </Td>
                        ))}
                      </SummaryRow>
                    )}
                  </TableBody>
                </ReportTable>
              </div>
            ) : (
              <EmptyReport>
                <FiFileText size={48} />
                <p>{t("reports.emptyReport")}</p>
              </EmptyReport>
            )}
          </ReportArea>
        </MainLayout>

        {/* Save Template Modal */}
        {showSaveModal && (
          <ModalOverlay onClick={() => setShowSaveModal(false)}>
            <ModalContent onClick={(e) => e.stopPropagation()}>
              <ModalTitle>{t("reports.saveTemplateTitle")}</ModalTitle>
              <ModalInput
                placeholder={t("reports.templateNamePlaceholder")}
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSaveTemplate()}
                autoFocus
              />
              <ModalActions>
                <ModalButton onClick={() => setShowSaveModal(false)}>{t("common.cancel")}</ModalButton>
                <ModalButton $primary onClick={handleSaveTemplate}>{t("common.save")}</ModalButton>
              </ModalActions>
            </ModalContent>
          </ModalOverlay>
        )}
      </Container>
    </Layout>
  );
};

export default Reports;
