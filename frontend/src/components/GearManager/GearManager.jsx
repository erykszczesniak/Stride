import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { FiPlus, FiEdit2, FiArchive, FiTrash2 } from "react-icons/fi";
import GearService from "../../api/GearService";
import { useToast } from "../../context/ToastContext";
import {
  PanelTitle,
  PanelDescription,
  LoadingSpinner,
  ModalOverlay,
  ModalContent,
  ModalTitle,
  ModalActions,
  ModalCancelButton,
  FormGroup,
  FormLabel,
  FormInput,
  FormSelect,
  FormRow,
  PasswordFormButton,
  FormError,
  GearHeader,
  AddGearButton,
  GearSectionLabel,
  GearList,
  GearCard,
  GearIcon,
  GearBody,
  GearName,
  GearMeta,
  GearStats,
  GearStatValue,
  GearActions,
  GearActionButton,
  GearEmptyState,
  GearTypeChips,
  GearTypeChip,
} from "../../pages/SettingsStyles";

// Activity types we expose as "taggable-with-gear" options in the UI. These
// match the backend ActivityType enum values the gear routes accept.
const GEAR_ACTIVITY_OPTIONS = [
  "RUN",
  "VIRTUAL_RUN",
  "WALK",
  "HIKE",
  "RIDE",
  "VIRTUAL_RIDE",
  "SWIM",
  "WORKOUT",
  "OTHER",
];

const GEAR_TYPE_ICONS = {
  SHOES: "👟",
  BIKE: "🚴",
  OTHER: "🎒",
};

const DEFAULT_TYPES_BY_GEAR = {
  SHOES: ["RUN", "VIRTUAL_RUN", "WALK", "HIKE"],
  BIKE: ["RIDE", "VIRTUAL_RIDE"],
  OTHER: [],
};

const emptyForm = () => ({
  name: "",
  brand: "",
  model: "",
  type: "SHOES",
  activityTypes: DEFAULT_TYPES_BY_GEAR.SHOES,
  notes: "",
});

const GearManager = () => {
  const { t } = useTranslation();
  const toast = useToast();
  const [loading, setLoading] = useState(true);
  const [gear, setGear] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(emptyForm());
  const [errors, setErrors] = useState({});

  const loadGear = useCallback(async () => {
    try {
      setLoading(true);
      const res = await GearService.getMyGear(true);
      setGear(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      toast.error(t("settings.gear.loadFailed"));
    } finally {
      setLoading(false);
    }
  }, [t, toast]);

  useEffect(() => {
    loadGear();
  }, [loadGear]);

  const { active, retired } = useMemo(() => {
    const activeList = gear.filter((g) => !g.retired);
    const retiredList = gear.filter((g) => g.retired);
    return { active: activeList, retired: retiredList };
  }, [gear]);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm());
    setErrors({});
    setShowModal(true);
  };

  const openEdit = (g) => {
    setEditingId(g.id);
    setForm({
      name: g.name || "",
      brand: g.brand || "",
      model: g.model || "",
      type: g.type || "SHOES",
      activityTypes: Array.isArray(g.activityTypes) ? g.activityTypes : [],
      notes: g.notes || "",
    });
    setErrors({});
    setShowModal(true);
  };

  const handleTypeChange = (newType) => {
    // When the gear type flips, swap in the default activity types for the
    // new category so the user isn't stuck with SHOES defaults on a BIKE.
    setForm((prev) => ({
      ...prev,
      type: newType,
      activityTypes: DEFAULT_TYPES_BY_GEAR[newType] || [],
    }));
  };

  const toggleActivityType = (at) => {
    setForm((prev) => {
      const has = prev.activityTypes.includes(at);
      return {
        ...prev,
        activityTypes: has
          ? prev.activityTypes.filter((x) => x !== at)
          : [...prev.activityTypes, at],
      };
    });
  };

  const validate = () => {
    const e = {};
    if (!form.name.trim()) e.name = t("settings.gear.errors.nameRequired");
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate() || saving) return;
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        brand: form.brand.trim() || null,
        model: form.model.trim() || null,
        type: form.type,
        activityTypes: form.activityTypes,
        notes: form.notes.trim() || null,
      };
      if (editingId) {
        await GearService.updateGear(editingId, payload);
        toast.success(t("settings.gear.updated"));
      } else {
        await GearService.createGear(payload);
        toast.success(t("settings.gear.created"));
      }
      setShowModal(false);
      loadGear();
    } catch (err) {
      toast.error(t("settings.gear.saveFailed"));
    } finally {
      setSaving(false);
    }
  };

  const handleRetire = async (g) => {
    if (!window.confirm(t("settings.gear.confirmRetire", { name: g.name }))) return;
    try {
      await GearService.retireGear(g.id);
      toast.success(t("settings.gear.retired"));
      loadGear();
    } catch (err) {
      toast.error(t("settings.gear.retireFailed"));
    }
  };

  const handleDelete = async (g) => {
    if (!window.confirm(t("settings.gear.confirmDelete", { name: g.name }))) return;
    try {
      await GearService.deleteGear(g.id);
      toast.success(t("settings.gear.deleted"));
      loadGear();
    } catch (err) {
      toast.error(t("settings.gear.deleteFailed"));
    }
  };

  if (loading) {
    return <LoadingSpinner />;
  }

  const renderCard = (g) => (
    <GearCard key={g.id} $retired={g.retired}>
      <GearIcon>{GEAR_TYPE_ICONS[g.type] || "🎒"}</GearIcon>
      <GearBody>
        <GearName>{g.name}</GearName>
        <GearMeta>
          {[g.brand, g.model].filter(Boolean).join(" · ") ||
            t(`settings.gear.types.${g.type}`)}
        </GearMeta>
        <GearStats>
          <span>
            <GearStatValue>{g.distanceFormatted || `${(g.distanceKmTotal || 0).toFixed(2)} km`}</GearStatValue>
            {t("settings.gear.distance")}
          </span>
          <span>
            <GearStatValue>{g.activityCount || 0}</GearStatValue>
            {t("settings.gear.activities")}
          </span>
        </GearStats>
        <GearActions>
          {!g.retired && (
            <GearActionButton onClick={() => openEdit(g)}>
              <FiEdit2 size={12} />
              {t("common.edit")}
            </GearActionButton>
          )}
          {!g.retired && (
            <GearActionButton onClick={() => handleRetire(g)}>
              <FiArchive size={12} />
              {t("settings.gear.retire")}
            </GearActionButton>
          )}
          <GearActionButton onClick={() => handleDelete(g)}>
            <FiTrash2 size={12} />
            {t("common.delete")}
          </GearActionButton>
        </GearActions>
      </GearBody>
    </GearCard>
  );

  return (
    <>
      <PanelTitle>{t("settings.gear.title")}</PanelTitle>
      <PanelDescription>{t("settings.gear.description")}</PanelDescription>

      <GearHeader>
        <GearSectionLabel style={{ margin: 0 }}>
          {t("settings.gear.activeHeading")}
        </GearSectionLabel>
        <AddGearButton onClick={openCreate}>
          <FiPlus size={16} />
          {t("settings.gear.addGear")}
        </AddGearButton>
      </GearHeader>

      {active.length === 0 ? (
        <GearEmptyState>{t("settings.gear.emptyActive")}</GearEmptyState>
      ) : (
        <GearList>{active.map(renderCard)}</GearList>
      )}

      {retired.length > 0 && (
        <>
          <GearSectionLabel>{t("settings.gear.retiredHeading")}</GearSectionLabel>
          <GearList>{retired.map(renderCard)}</GearList>
        </>
      )}

      {showModal && (
        <ModalOverlay onClick={() => !saving && setShowModal(false)}>
          <ModalContent onClick={(e) => e.stopPropagation()}>
            <ModalTitle>
              {editingId ? t("settings.gear.editTitle") : t("settings.gear.addTitle")}
            </ModalTitle>

            <FormGroup>
              <FormLabel>{t("settings.gear.name")}</FormLabel>
              <FormInput
                value={form.name}
                onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
                placeholder={t("settings.gear.namePlaceholder")}
              />
              {errors.name && <FormError>{errors.name}</FormError>}
            </FormGroup>

            <FormRow>
              <FormGroup>
                <FormLabel>{t("settings.gear.brand")}</FormLabel>
                <FormInput
                  value={form.brand}
                  onChange={(e) => setForm((p) => ({ ...p, brand: e.target.value }))}
                  placeholder={t("settings.gear.brandPlaceholder")}
                />
              </FormGroup>
              <FormGroup>
                <FormLabel>{t("settings.gear.model")}</FormLabel>
                <FormInput
                  value={form.model}
                  onChange={(e) => setForm((p) => ({ ...p, model: e.target.value }))}
                  placeholder={t("settings.gear.modelPlaceholder")}
                />
              </FormGroup>
            </FormRow>

            <FormGroup>
              <FormLabel>{t("settings.gear.type")}</FormLabel>
              <FormSelect
                value={form.type}
                onChange={(e) => handleTypeChange(e.target.value)}
              >
                <option value="SHOES">{t("settings.gear.types.SHOES")}</option>
                <option value="BIKE">{t("settings.gear.types.BIKE")}</option>
                <option value="OTHER">{t("settings.gear.types.OTHER")}</option>
              </FormSelect>
            </FormGroup>

            <FormGroup>
              <FormLabel>{t("settings.gear.activityTypes")}</FormLabel>
              <GearTypeChips>
                {GEAR_ACTIVITY_OPTIONS.map((at) => (
                  <GearTypeChip key={at} $selected={form.activityTypes.includes(at)}>
                    <input
                      type="checkbox"
                      checked={form.activityTypes.includes(at)}
                      onChange={() => toggleActivityType(at)}
                    />
                    {t(`activities.typesShort.${at}`, { defaultValue: at })}
                  </GearTypeChip>
                ))}
              </GearTypeChips>
            </FormGroup>

            <FormGroup>
              <FormLabel>{t("settings.gear.notes")}</FormLabel>
              <FormInput
                value={form.notes}
                onChange={(e) => setForm((p) => ({ ...p, notes: e.target.value }))}
                placeholder={t("settings.gear.notesPlaceholder")}
              />
            </FormGroup>

            <ModalActions>
              <ModalCancelButton onClick={() => setShowModal(false)} disabled={saving}>
                {t("common.cancel")}
              </ModalCancelButton>
              <PasswordFormButton onClick={handleSave} disabled={saving}>
                {saving ? t("common.saving") : t("common.save")}
              </PasswordFormButton>
            </ModalActions>
          </ModalContent>
        </ModalOverlay>
      )}
    </>
  );
};

export default GearManager;
