import React, { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  FiSearch,
  FiHome,
  FiActivity,
  FiUser,
  FiTarget,
  FiAward,
  FiBook,
  FiUsers,
  FiCalendar,
  FiSettings,
  FiPlus,
  FiSun,
  FiMail,
  FiBell,
  FiFileText,
  FiLock,
  FiTrash2,
  FiShield,
  FiGlobe,
  FiSliders,
} from "react-icons/fi";
import {
  Overlay,
  ModalContainer,
  SearchInputWrapper,
  SearchInput,
  ShortcutHint,
  ResultsList,
  CategoryLabel,
  ResultItem,
  ResultIcon,
  ResultLabel,
  ResultCategory,
  EmptyState,
  Footer,
  FooterHint,
} from "./SearchModalStyles";

const SearchModal = ({ isOpen, onClose }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef(null);
  const resultsRef = useRef(null);

  const SEARCH_ITEMS = useMemo(() => {
    const pages = t("search.categories.pages");
    const settings = t("search.categories.settings");
    return [
      // Settings
      { id: "dark-mode", label: t("search.items.darkMode"), category: settings, icon: FiSun, path: "/settings", settingsSection: "preferences" },
      { id: "email-notifications", label: t("search.items.emailNotifications"), category: settings, icon: FiMail, path: "/settings", settingsSection: "notifications" },
      { id: "push-notifications", label: t("search.items.pushNotifications"), category: settings, icon: FiBell, path: "/settings", settingsSection: "notifications" },
      { id: "weekly-summary", label: t("search.items.weeklySummary"), category: settings, icon: FiFileText, path: "/settings", settingsSection: "notifications" },
      { id: "change-password", label: t("search.items.changePassword"), category: settings, icon: FiLock, path: "/settings", settingsSection: "account" },
      { id: "delete-account", label: t("search.items.deleteAccount"), category: settings, icon: FiTrash2, path: "/settings", settingsSection: "account" },
      { id: "privacy-settings", label: t("search.items.privacySettings"), category: settings, icon: FiShield, path: "/settings", settingsSection: "preferences" },
      { id: "language", label: t("search.items.language"), category: settings, icon: FiGlobe, path: "/settings", settingsSection: "preferences" },
      { id: "units", label: t("search.items.units"), category: settings, icon: FiSliders, path: "/settings", settingsSection: "preferences" },

      // Pages
      { id: "dashboard", label: t("search.items.dashboard"), category: pages, icon: FiHome, path: "/" },
      { id: "activities", label: t("search.items.activities"), category: pages, icon: FiActivity, path: "/activities" },
      { id: "profile", label: t("search.items.profile"), category: pages, icon: FiUser, path: "/profile" },
      { id: "goals", label: t("search.items.goals"), category: pages, icon: FiTarget, path: "/goals" },
      { id: "challenges", label: t("search.items.challenges"), category: pages, icon: FiAward, path: "/challenges" },
      { id: "training", label: t("search.items.trainingPlans"), category: pages, icon: FiBook, path: "/training" },
      { id: "clubs", label: t("search.items.clubs"), category: pages, icon: FiUsers, path: "/clubs" },
      { id: "events", label: t("search.items.events"), category: pages, icon: FiCalendar, path: "/events" },
      { id: "settings", label: t("search.items.settings"), category: pages, icon: FiSettings, path: "/settings" },
      { id: "record-activity", label: t("search.items.recordActivity"), category: pages, icon: FiPlus, path: "/activities/new" },
    ];
  }, [t]);

  const filteredItems = useMemo(() => {
    if (!query.trim()) return SEARCH_ITEMS;
    const lower = query.toLowerCase();
    return SEARCH_ITEMS.filter(
      (item) =>
        item.label.toLowerCase().includes(lower) ||
        item.category.toLowerCase().includes(lower)
    );
  }, [query, SEARCH_ITEMS]);

  const groupedResults = useMemo(() => {
    const groups = [];
    let currentCategory = null;
    filteredItems.forEach((item, index) => {
      if (item.category !== currentCategory) {
        currentCategory = item.category;
        groups.push({ type: "category", label: currentCategory });
      }
      groups.push({ type: "item", item, flatIndex: index });
    });
    return groups;
  }, [filteredItems]);

  // Reset state when modal opens
  useEffect(() => {
    if (isOpen) {
      setQuery("");
      setSelectedIndex(0);
      // Focus input after a frame so the modal is rendered
      requestAnimationFrame(() => {
        inputRef.current?.focus();
      });
    }
  }, [isOpen]);

  // Reset selected index when results change
  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  const handleSelect = useCallback(
    (item) => {
      onClose();
      if (item.settingsSection) {
        // Use a unique key to force React Router to detect the change
        navigate(`${item.path}?section=${item.settingsSection}&t=${Date.now()}`, { replace: false });
      } else {
        navigate(item.path);
      }
    },
    [navigate, onClose]
  );

  // Scroll selected item into view
  useEffect(() => {
    if (!resultsRef.current) return;
    const activeEl = resultsRef.current.querySelector("[data-active='true']");
    if (activeEl) {
      activeEl.scrollIntoView({ block: "nearest" });
    }
  }, [selectedIndex]);

  const handleKeyDown = useCallback(
    (e) => {
      switch (e.key) {
        case "ArrowDown":
          e.preventDefault();
          setSelectedIndex((prev) =>
            prev < filteredItems.length - 1 ? prev + 1 : 0
          );
          break;
        case "ArrowUp":
          e.preventDefault();
          setSelectedIndex((prev) =>
            prev > 0 ? prev - 1 : filteredItems.length - 1
          );
          break;
        case "Enter":
          e.preventDefault();
          if (filteredItems[selectedIndex]) {
            handleSelect(filteredItems[selectedIndex]);
          }
          break;
        case "Escape":
          e.preventDefault();
          onClose();
          break;
        default:
          break;
      }
    },
    [filteredItems, selectedIndex, handleSelect, onClose]
  );

  if (!isOpen) return null;

  return (
    <Overlay onClick={onClose}>
      <ModalContainer onClick={(e) => e.stopPropagation()} onKeyDown={handleKeyDown}>
        <SearchInputWrapper>
          <FiSearch size={18} />
          <SearchInput
            ref={inputRef}
            type="text"
            placeholder={t("search.placeholder")}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <ShortcutHint>ESC</ShortcutHint>
        </SearchInputWrapper>

        <ResultsList ref={resultsRef}>
          {filteredItems.length === 0 ? (
            <EmptyState>{t("search.noResults", { query })}</EmptyState>
          ) : (
            groupedResults.map((entry, i) => {
              if (entry.type === "category") {
                return (
                  <CategoryLabel key={`cat-${entry.label}`} $first={i === 0}>
                    {entry.label}
                  </CategoryLabel>
                );
              }
              const { item, flatIndex } = entry;
              const isActive = flatIndex === selectedIndex;
              const Icon = item.icon;
              return (
                <ResultItem
                  key={item.id}
                  $active={isActive}
                  data-active={isActive}
                  onClick={() => handleSelect(item)}
                  onMouseEnter={() => setSelectedIndex(flatIndex)}
                >
                  <ResultIcon $active={isActive}>
                    <Icon size={16} />
                  </ResultIcon>
                  <ResultLabel>{item.label}</ResultLabel>
                  <ResultCategory>{item.category}</ResultCategory>
                </ResultItem>
              );
            })
          )}
        </ResultsList>

        <Footer>
          <FooterHint>
            <kbd>↑</kbd><kbd>↓</kbd> {t("search.hints.navigate")}
          </FooterHint>
          <FooterHint>
            <kbd>↵</kbd> {t("search.hints.select")}
          </FooterHint>
          <FooterHint>
            <kbd>esc</kbd> {t("search.hints.close")}
          </FooterHint>
        </Footer>
      </ModalContainer>
    </Overlay>
  );
};

export default SearchModal;
