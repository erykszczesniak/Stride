// Responsive breakpoints
export const breakpoints = {
  xs: '480px',
  sm: '640px',
  md: '768px',
  lg: '1024px',
  xl: '1200px',
  xxl: '1400px',
};

// Media query helpers
export const media = {
  xs: `@media (max-width: ${breakpoints.xs})`,
  sm: `@media (max-width: ${breakpoints.sm})`,
  md: `@media (max-width: ${breakpoints.md})`,
  lg: `@media (max-width: ${breakpoints.lg})`,
  xl: `@media (max-width: ${breakpoints.xl})`,
  xxl: `@media (max-width: ${breakpoints.xxl})`,
};

export const lightTheme = {
  // Backgrounds
  bgPrimary: '#f5f5f5',
  bgSecondary: '#ffffff',
  bgCard: '#ffffff',
  bgHover: '#f1f5f9',
  bgInput: '#ffffff',

  // Text
  textPrimary: '#1a1a2e',
  textSecondary: '#64748b',
  textMuted: '#94a3b8',

  // Brand
  primary: '#fc4c02',
  primaryHover: '#e04400',
  primaryLight: '#fff7ed',

  // Borders
  border: '#e2e8f0',
  borderLight: '#f1f5f9',

  // Shadows
  shadow: 'rgba(0, 0, 0, 0.1)',
  shadowMedium: 'rgba(0, 0, 0, 0.15)',

  // Status
  success: '#22c55e',
  error: '#ef4444',
  warning: '#f59e0b',

  // Dropdown
  dropdownBg: '#ffffff',
  dropdownHover: '#f5f5f5',

  // Sidebar
  sidebarBg: '#ffffff',
  sidebarHover: '#f8fafc',
  sidebarActive: '#fff7ed',
};

export const darkTheme = {
  // Backgrounds
  bgPrimary: '#111113',
  bgSecondary: '#1a1a1d',
  bgCard: '#232326',
  bgHover: '#2c2c30',
  bgInput: '#1a1a1d',

  // Text
  textPrimary: '#ededef',
  textSecondary: '#a0a0a8',
  textMuted: '#63636b',

  // Brand
  primary: '#fc4c02',
  primaryHover: '#ff6b35',
  primaryLight: '#2a1a10',

  // Borders
  border: '#2c2c30',
  borderLight: '#232326',

  // Shadows
  shadow: 'rgba(0, 0, 0, 0.5)',
  shadowMedium: 'rgba(0, 0, 0, 0.6)',

  // Status
  success: '#22c55e',
  error: '#ef4444',
  warning: '#f59e0b',

  // Dropdown
  dropdownBg: '#232326',
  dropdownHover: '#2c2c30',

  // Sidebar
  sidebarBg: '#18181b',
  sidebarHover: '#232326',
  sidebarActive: '#2a1a10',
};
