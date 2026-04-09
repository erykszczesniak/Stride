import React, { useState, useCallback, useRef, useMemo } from "react";
import { FiDownload, FiFileText, FiCalendar, FiCheck, FiMenu, FiSave, FiFolder, FiX } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
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

const AVAILABLE_FIELDS = [
  { key: "activityTitle", label: "Title" },
  { key: "activityType", label: "Type" },
  { key: "date", label: "Date" },
  { key: "distance", label: "Distance (km)" },
  { key: "duration", label: "Duration" },
  { key: "elevationGain", label: "Elevation (m)" },
  { key: "calories", label: "Calories" },
  { key: "averageSpeed", label: "Avg Speed (km/h)" },
  { key: "averageHeartRate", label: "Avg Heart Rate" },
  { key: "pace", label: "Pace (min/km)" },
  { key: "likesCount", label: "Likes" },
  { key: "commentsCount", label: "Comments" },
];

const DEFAULT_FIELDS = ["activityTitle", "activityType", "date", "distance", "duration", "calories"];

const ACTIVITY_TYPES = [
  { value: "", label: "All Types" },
  { value: "RUN", label: "Run" },
  { value: "RIDE", label: "Ride" },
  { value: "SWIM", label: "Swim" },
  { value: "HIKE", label: "Hike" },
  { value: "WALK", label: "Walk" },
  { value: "WORKOUT", label: "Workout" },
];

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

const formatValue = (key, value) => {
  if (value === null || value === undefined) return "-";
  switch (key) {
    case "distance": return `${Number(value).toFixed(1)} km`;
    case "duration": return formatDuration(value);
    case "averageSpeed": return `${Number(value).toFixed(1)} km/h`;
    case "elevationGain": return `${value} m`;
    case "calories": return `${value} kcal`;
    default: return String(value);
  }
};

const getRawValue = (key, value) => {
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
  const { user } = useAuth();
  const toast = useToast();
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
      const aVal = getRawValue(sortConfig.field, a[sortConfig.field]);
      const bVal = getRawValue(sortConfig.field, b[sortConfig.field]);
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
      toast.warning("Enter a template name");
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
    toast.success("Template saved!");
  };

  const handleLoadTemplate = (e) => {
    const id = Number(e.target.value);
    if (!id) return;
    const tmpl = templates.find((t) => t.id === id);
    if (tmpl) {
      setSelectedFields(tmpl.selectedFields);
      if (tmpl.datePreset) applyPreset(tmpl.datePreset);
      if (tmpl.activityType !== undefined) setActivityTypeFilter(tmpl.activityType);
      toast.success(`Loaded "${tmpl.name}"`);
    }
    e.target.value = "";
  };

  const handleDeleteTemplate = (id) => {
    const updated = templates.filter((t) => t.id !== id);
    setTemplates(updated);
    saveTemplates(updated);
    toast.success("Template deleted");
  };

  const generateReport = useCallback(async () => {
    if (selectedFields.length === 0) {
      toast.warning("Select at least one field");
      return;
    }
    setLoading(true);
    try {
      const token = localStorage.getItem("token");
      const query = `
        query GenerateReport($dateFrom: String!, $dateTo: String!, $fields: [String!]!, $activityType: String) {
          generateReport(dateFrom: $dateFrom, dateTo: $dateTo, fields: $fields, activityType: $activityType) {
            rows {
              ${selectedFields.join("\n              ")}
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
            fields: selectedFields,
            activityType: activityTypeFilter || null,
          },
        }),
      });
      const json = await res.json();
      if (json.errors) {
        toast.error("Failed to generate report");
        return;
      }
      setRows(json.data.generateReport.rows);
      setSummary(json.data.generateReport.summary);
    } catch {
      toast.error("Failed to generate report");
    } finally {
      setLoading(false);
    }
  }, [dateFrom, dateTo, selectedFields, activityTypeFilter, toast]);

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
        }),
      });
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `stride-report-${dateFrom}-${dateTo}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success("CSV exported!");
    } catch {
      toast.error("CSV export failed");
    }
  };

  const exportPdf = () => {
    if (rows.length === 0) {
      toast.warning("Generate a report first");
      return;
    }
    const doc = new jsPDF({ orientation: "landscape" });
    doc.setFontSize(16);
    doc.text("Stride Activity Report", 14, 15);
    doc.setFontSize(10);
    doc.text(`${dateFrom} — ${dateTo} | ${user?.firstName} ${user?.lastName}`, 14, 22);

    const headers = selectedFields.map((f) => AVAILABLE_FIELDS.find((af) => af.key === f)?.label || f);
    const body = sortedRows.map((row) =>
      selectedFields.map((f) => formatValue(f, row[f]).replace(/ km\/h| km| m| kcal/g, ""))
    );

    autoTable(doc, {
      head: [headers],
      body,
      startY: 28,
      styles: { fontSize: 8 },
      headStyles: { fillColor: [252, 76, 2] },
    });

    doc.save(`stride-report-${dateFrom}-${dateTo}.pdf`);
    toast.success("PDF exported!");
  };

  return (
    <Layout>
      <Container>
        <TopBar>
          <div>
            <PageTitle>Report Builder</PageTitle>
            <PageDescription>Select fields, date range, and generate your training report</PageDescription>
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
            <FiSave size={14} /> Save Template
          </TemplateButton>
          {templates.length > 0 && (
            <>
              <TemplateSelect onChange={handleLoadTemplate} defaultValue="">
                <option value="" disabled>Load Template...</option>
                {templates.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </TemplateSelect>
              {templates.map((t) => (
                <TemplateDeleteBtn key={t.id} title={`Delete "${t.name}"`} onClick={() => handleDeleteTemplate(t.id)}>
                  <FiX size={12} />
                </TemplateDeleteBtn>
              ))}
            </>
          )}
        </TemplateBar>

        <MainLayout>
          <FieldsPanel>
            <FieldsPanelTitle>Available Fields</FieldsPanelTitle>
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
                <SelectedFieldsTitle>Column Order (drag to reorder)</SelectedFieldsTitle>
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
                <DateLabel><FiCalendar size={14} /> From</DateLabel>
                <DateInput type="date" value={dateFrom} onChange={(e) => { setDateFrom(e.target.value); setActivePreset(null); }} />
              </div>
              <div>
                <DateLabel><FiCalendar size={14} /> To</DateLabel>
                <DateInput type="date" value={dateTo} onChange={(e) => { setDateTo(e.target.value); setActivePreset(null); }} />
              </div>
              <div>
                <DateLabel><FiFolder size={14} /> Activity Type</DateLabel>
                <FilterSelect value={activityTypeFilter} onChange={(e) => setActivityTypeFilter(e.target.value)}>
                  {ACTIVITY_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </FilterSelect>
              </div>
              <Presets>
                <PresetButton onClick={() => applyPreset(7)}>7 days</PresetButton>
                <PresetButton onClick={() => applyPreset(30)}>30 days</PresetButton>
                <PresetButton onClick={() => applyPreset("month")}>This month</PresetButton>
                <PresetButton onClick={() => applyPreset("lastMonth")}>Last month</PresetButton>
                <PresetButton onClick={() => applyPreset("year")}>This year</PresetButton>
              </Presets>
              <ExportButton onClick={generateReport} disabled={loading} style={{ marginLeft: "auto" }}>
                {loading ? "Generating..." : "Generate Report"}
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
                          <Td key={f}>{formatValue(f, row[f])}</Td>
                        ))}
                      </tr>
                    ))}
                    {summary && (
                      <SummaryRow>
                        {selectedFields.map((f) => (
                          <Td key={f}>
                            {f === "activityTitle" ? `${summary.totalActivities} activities` :
                             f === "distance" ? `${summary.totalDistance} km` :
                             f === "duration" ? formatDuration(summary.totalDuration) :
                             f === "calories" ? `${summary.totalCalories} kcal` :
                             f === "elevationGain" ? `${summary.totalElevation} m` :
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
                <p>Select fields, choose a date range, and click "Generate Report"</p>
              </EmptyReport>
            )}
          </ReportArea>
        </MainLayout>

        {/* Save Template Modal */}
        {showSaveModal && (
          <ModalOverlay onClick={() => setShowSaveModal(false)}>
            <ModalContent onClick={(e) => e.stopPropagation()}>
              <ModalTitle>Save Report Template</ModalTitle>
              <ModalInput
                placeholder="Template name..."
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSaveTemplate()}
                autoFocus
              />
              <ModalActions>
                <ModalButton onClick={() => setShowSaveModal(false)}>Cancel</ModalButton>
                <ModalButton $primary onClick={handleSaveTemplate}>Save</ModalButton>
              </ModalActions>
            </ModalContent>
          </ModalOverlay>
        )}
      </Container>
    </Layout>
  );
};

export default Reports;
