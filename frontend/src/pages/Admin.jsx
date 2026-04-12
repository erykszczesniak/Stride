import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { FiArrowLeft, FiUsers, FiActivity, FiMap, FiCalendar, FiPlus, FiDownload, FiFileText } from "react-icons/fi";
import * as XLSX from "xlsx";
import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import Navbar from "../components/Navbar/Navbar";
import AdminService from "../api/AdminService";
import { useToast } from "../context/ToastContext";
import { formatDuration, formatDate, getInitials } from "../utils/formatters";
import {
  Container,
  Header,
  BackButton,
  HeaderContent,
  Title,
  Subtitle,
  StatsGrid,
  StatCard,
  StatIcon,
  StatValue,
  StatLabel,
  TabsContainer,
  Tab,
  TabContent,
  TableHeader,
  SearchInput,
  AddButton,
  Table,
  Th,
  Td,
  Tr,
  UserCell,
  UserAvatar,
  UserInfo,
  UserName,
  UserEmail,
  Badge,
  ActionButton,
  Pagination,
  PageButton,
  ExportSection,
  ExportTitle,
  ExportDescription,
  ExportOptions,
  ExportRow,
  ExportLabel,
  Select,
  FormatCards,
  FormatCard,
  FormatIcon,
  FormatName,
  Checkbox,
  ExportButton,
  LoadingSpinner,
  EmptyState,
  EmptyIcon,
  EmptyText,
} from "./AdminStyles";

const Admin = () => {
  const { t } = useTranslation();
  const toast = useToast();
  const [activeTab, setActiveTab] = useState("activities");
  const [stats, setStats] = useState(null);
  const [activities, setActivities] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activitiesPage, setActivitiesPage] = useState(0);
  const [usersPage, setUsersPage] = useState(0);
  const [activitiesTotalPages, setActivitiesTotalPages] = useState(0);
  const [usersTotalPages, setUsersTotalPages] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [exportType, setExportType] = useState("activities");
  const [exportFormat, setExportFormat] = useState("xlsx");
  const [includeAllData, setIncludeAllData] = useState(false);

  useEffect(() => {
    loadStats();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (activeTab === "activities") {
      loadActivities();
    } else if (activeTab === "users") {
      loadUsers();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, activitiesPage, usersPage]);

  const loadStats = async () => {
    try {
      const response = await AdminService.getAdminStats();
      setStats(response.data);
    } catch (error) {
      if (error.response?.status === 403) {
        toast.error(t("admin.toast.accessDenied"));
      }
    }
  };

  const loadActivities = async () => {
    setLoading(true);
    try {
      const response = searchTerm
        ? await AdminService.searchActivities(searchTerm, activitiesPage)
        : await AdminService.getAllActivities(activitiesPage);
      setActivities(response.data.items || []);
      setActivitiesTotalPages(response.data.totalPages || 0);
    } catch (error) {
      if (error.response?.status === 403) {
        toast.error(t("admin.toast.accessDeniedActivities"));
      }
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    setLoading(true);
    try {
      const response = await AdminService.getAllUsers(usersPage);
      setUsers(response.data.items || []);
      setUsersTotalPages(response.data.totalPages || 0);
    } catch (error) {
      if (error.response?.status === 403) {
        toast.error(t("admin.toast.accessDeniedUsers"));
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setActivitiesPage(0);
    loadActivities();
  };

  const handleDeleteActivity = async (activityId) => {
    if (window.confirm(t("admin.toast.confirmDeleteActivity"))) {
      try {
        await AdminService.deleteActivity(activityId);
        toast.success(t("admin.toast.activityDeleted"));
        loadActivities();
      } catch (error) {
        toast.error(t("admin.toast.activityDeleteFailed"));
      }
    }
  };

  const handleDeleteUser = async (userId) => {
    if (window.confirm(t("admin.toast.confirmDeleteUser"))) {
      try {
        await AdminService.deleteUser(userId);
        toast.success(t("admin.toast.userDeleted"));
        loadUsers();
      } catch (error) {
        toast.error(t("admin.toast.userDeleteFailed"));
      }
    }
  };

  const exportToTxt = (data, type) => {
    let content = `${type.toUpperCase()} EXPORT\n`;
    content += `Generated: ${new Date().toLocaleString()}\n`;
    content += "=".repeat(50) + "\n\n";

    if (type === "activities") {
      data.forEach((item, index) => {
        content += `Activity ${index + 1}\n`;
        content += `-`.repeat(30) + "\n";
        content += `Title: ${item.title}\n`;
        content += `Type: ${item.activityType}\n`;
        content += `Distance: ${item.distance?.toFixed(2)} km\n`;
        content += `Duration: ${formatDuration(item.duration)}\n`;
        content += `Calories: ${item.calories || "N/A"}\n`;
        content += `Date: ${formatDate(item.startTime)}\n`;
        content += `User: ${item.user?.firstName} ${item.user?.lastName}\n\n`;
      });
    } else {
      data.forEach((item, index) => {
        content += `User ${index + 1}\n`;
        content += `-`.repeat(30) + "\n";
        content += `Name: ${item.firstName} ${item.lastName}\n`;
        content += `Email: ${item.email}\n`;
        content += `Role: ${item.role}\n`;
        content += `Status: ${item.active ? "Active" : "Inactive"}\n`;
        content += `Activities: ${item.activitiesCount}\n`;
        content += `Joined: ${formatDate(item.createdAt)}\n\n`;
      });
    }

    const blob = new Blob([content], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${type}_export_${new Date().toISOString().split("T")[0]}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const exportToXlsx = (data, type) => {
    let wsData;
    if (type === "activities") {
      wsData = data.map((item) => ({
        Title: item.title,
        Type: item.activityType,
        "Distance (km)": item.distance?.toFixed(2),
        Duration: formatDuration(item.duration),
        Calories: item.calories || "N/A",
        Date: formatDate(item.startTime),
        User: `${item.user?.firstName} ${item.user?.lastName}`,
      }));
    } else {
      wsData = data.map((item) => ({
        Name: `${item.firstName} ${item.lastName}`,
        Email: item.email,
        Role: item.role,
        Status: item.active ? "Active" : "Inactive",
        Activities: item.activitiesCount,
        Followers: item.followersCount,
        Following: item.followingCount,
        Joined: formatDate(item.createdAt),
      }));
    }

    const ws = XLSX.utils.json_to_sheet(wsData);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, type.charAt(0).toUpperCase() + type.slice(1));
    XLSX.writeFile(wb, `${type}_export_${new Date().toISOString().split("T")[0]}.xlsx`);
  };

  const exportToPdf = (data, type) => {
    const doc = new jsPDF();
    const title = `${type.charAt(0).toUpperCase() + type.slice(1)} Export`;

    doc.setFontSize(18);
    doc.text(title, 14, 20);
    doc.setFontSize(10);
    doc.text(`Generated: ${new Date().toLocaleString()}`, 14, 28);

    let columns;
    let rows;

    if (type === "activities") {
      columns = ["Title", "Type", "Distance", "Duration", "Date", "User"];
      rows = data.map((item) => [
        item.title,
        item.activityType,
        `${item.distance?.toFixed(2)} km`,
        formatDuration(item.duration),
        formatDate(item.startTime),
        `${item.user?.firstName} ${item.user?.lastName}`,
      ]);
    } else {
      columns = ["Name", "Email", "Role", "Status", "Activities", "Joined"];
      rows = data.map((item) => [
        `${item.firstName} ${item.lastName}`,
        item.email,
        item.role,
        item.active ? "Active" : "Inactive",
        item.activitiesCount,
        formatDate(item.createdAt),
      ]);
    }

    autoTable(doc, {
      head: [columns],
      body: rows,
      startY: 35,
      styles: { fontSize: 9 },
      headStyles: { fillColor: [252, 82, 0] },
    });

    doc.save(`${type}_export_${new Date().toISOString().split("T")[0]}.pdf`);
  };

  const handleExport = () => {
    const data = exportType === "activities" ? activities : users;

    if (data.length === 0) {
      toast.warning(t("admin.toast.noData"));
      return;
    }

    switch (exportFormat) {
      case "txt":
        exportToTxt(data, exportType);
        break;
      case "xlsx":
        exportToXlsx(data, exportType);
        break;
      case "pdf":
        exportToPdf(data, exportType);
        break;
      default:
        break;
    }
  };

  return (
    <>
      <Navbar />
      <Container>
        <Header>
          <BackButton to="/">
            <FiArrowLeft size={20} />
          </BackButton>
          <HeaderContent>
            <Title>{t("admin.title")}</Title>
            <Subtitle>{t("admin.subtitle")}</Subtitle>
          </HeaderContent>
        </Header>

        <StatsGrid>
          <StatCard>
            <StatIcon color="#3b82f6">
              <FiUsers size={24} />
            </StatIcon>
            <StatValue>{stats?.totalUsers || 0}</StatValue>
            <StatLabel>{t("admin.totalUsers")}</StatLabel>
          </StatCard>
          <StatCard>
            <StatIcon color="#10b981">
              <FiActivity size={24} />
            </StatIcon>
            <StatValue>{stats?.totalActivities || 0}</StatValue>
            <StatLabel>{t("admin.totalActivities")}</StatLabel>
          </StatCard>
          <StatCard>
            <StatIcon color="#f59e0b">
              <FiMap size={24} />
            </StatIcon>
            <StatValue>{stats?.totalDistanceKm?.toFixed(0) || 0}</StatValue>
            <StatLabel>{t("admin.totalDistance")}</StatLabel>
          </StatCard>
          <StatCard>
            <StatIcon color="#8b5cf6">
              <FiCalendar size={24} />
            </StatIcon>
            <StatValue>{stats?.activitiesThisMonth || 0}</StatValue>
            <StatLabel>{t("admin.thisMonth")}</StatLabel>
          </StatCard>
        </StatsGrid>

        <TabsContainer>
          <Tab active={activeTab === "activities"} onClick={() => setActiveTab("activities")}>
            {t("admin.tabs.activities")}
          </Tab>
          <Tab active={activeTab === "users"} onClick={() => setActiveTab("users")}>
            {t("admin.tabs.users")}
          </Tab>
          <Tab active={activeTab === "export"} onClick={() => setActiveTab("export")}>
            {t("admin.tabs.export")}
          </Tab>
        </TabsContainer>

        <TabContent>
          {activeTab === "activities" && (
            <>
              <TableHeader>
                <form onSubmit={handleSearch}>
                  <SearchInput
                    type="text"
                    placeholder={t("admin.searchActivities")}
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </form>
                <AddButton as="a" href="/activities/new">
                  <FiPlus size={18} />
                  {t("nav.addActivity")}
                </AddButton>
              </TableHeader>
              {loading ? (
                <LoadingSpinner />
              ) : activities.length === 0 ? (
                <EmptyState>
                  <EmptyIcon>
                    <FiActivity size={48} />
                  </EmptyIcon>
                  <EmptyText>{t("admin.noActivitiesFound")}</EmptyText>
                </EmptyState>
              ) : (
                <>
                  <Table>
                    <thead>
                      <tr>
                        <Th>{t("admin.columns.activity")}</Th>
                        <Th>{t("admin.columns.type")}</Th>
                        <Th>{t("admin.columns.distance")}</Th>
                        <Th>{t("admin.columns.duration")}</Th>
                        <Th>{t("admin.columns.date")}</Th>
                        <Th>{t("admin.columns.user")}</Th>
                        <Th>{t("admin.columns.actions")}</Th>
                      </tr>
                    </thead>
                    <tbody>
                      {activities.map((activity) => (
                        <Tr key={activity.id}>
                          <Td>
                            <strong>{activity.title}</strong>
                          </Td>
                          <Td>
                            <Badge>{activity.activityType}</Badge>
                          </Td>
                          <Td>{activity.distance?.toFixed(2)} km</Td>
                          <Td>{formatDuration(activity.duration)}</Td>
                          <Td>{formatDate(activity.startTime)}</Td>
                          <Td>
                            <UserCell>
                              <UserAvatar>
                                {activity.user?.avatar ? (
                                  <img src={activity.user.avatar} alt="" />
                                ) : (
                                  getInitials(activity.user?.firstName, activity.user?.lastName)
                                )}
                              </UserAvatar>
                              <span>{activity.user?.firstName} {activity.user?.lastName}</span>
                            </UserCell>
                          </Td>
                          <Td>
                            <ActionButton as="a" href={`/activities/${activity.id}`}>
                              {t("admin.view")}
                            </ActionButton>
                            <ActionButton className="danger" onClick={() => handleDeleteActivity(activity.id)}>
                              {t("admin.delete")}
                            </ActionButton>
                          </Td>
                        </Tr>
                      ))}
                    </tbody>
                  </Table>
                  {activitiesTotalPages > 1 && (
                    <Pagination>
                      <PageButton
                        disabled={activitiesPage === 0}
                        onClick={() => setActivitiesPage(activitiesPage - 1)}
                      >
                        {t("admin.previous")}
                      </PageButton>
                      <span>
                        {t("admin.pageOf", { current: activitiesPage + 1, total: activitiesTotalPages })}
                      </span>
                      <PageButton
                        disabled={activitiesPage >= activitiesTotalPages - 1}
                        onClick={() => setActivitiesPage(activitiesPage + 1)}
                      >
                        {t("admin.next")}
                      </PageButton>
                    </Pagination>
                  )}
                </>
              )}
            </>
          )}

          {activeTab === "users" && (
            <>
              <TableHeader>
                <SearchInput
                  type="text"
                  placeholder={t("admin.searchUsers")}
                />
              </TableHeader>
              {loading ? (
                <LoadingSpinner />
              ) : users.length === 0 ? (
                <EmptyState>
                  <EmptyIcon>
                    <FiUsers size={48} />
                  </EmptyIcon>
                  <EmptyText>{t("admin.noUsersFound")}</EmptyText>
                </EmptyState>
              ) : (
                <>
                  <Table>
                    <thead>
                      <tr>
                        <Th>{t("admin.columns.user")}</Th>
                        <Th>{t("admin.columns.role")}</Th>
                        <Th>{t("admin.columns.status")}</Th>
                        <Th>{t("admin.columns.activities")}</Th>
                        <Th>{t("admin.columns.followers")}</Th>
                        <Th>{t("admin.columns.joined")}</Th>
                        <Th>{t("admin.columns.actions")}</Th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map((user) => (
                        <Tr key={user.id}>
                          <Td>
                            <UserCell>
                              <UserAvatar>
                                {user.avatar ? (
                                  <img src={user.avatar} alt="" />
                                ) : (
                                  getInitials(user.firstName, user.lastName)
                                )}
                              </UserAvatar>
                              <UserInfo>
                                <UserName>{user.firstName} {user.lastName}</UserName>
                                <UserEmail>{user.email}</UserEmail>
                              </UserInfo>
                            </UserCell>
                          </Td>
                          <Td>
                            <Badge type={user.role === "ADMIN" ? "admin" : "user"}>
                              {user.role}
                            </Badge>
                          </Td>
                          <Td>
                            <Badge type={user.active ? "active" : "inactive"}>
                              {user.active ? t("admin.active") : t("admin.inactive")}
                            </Badge>
                          </Td>
                          <Td>{user.activitiesCount}</Td>
                          <Td>{user.followersCount}</Td>
                          <Td>{formatDate(user.createdAt)}</Td>
                          <Td>
                            <ActionButton as="a" href={`/profile/${user.id}`}>
                              {t("admin.view")}
                            </ActionButton>
                            <ActionButton className="danger" onClick={() => handleDeleteUser(user.id)}>
                              {t("admin.delete")}
                            </ActionButton>
                          </Td>
                        </Tr>
                      ))}
                    </tbody>
                  </Table>
                  {usersTotalPages > 1 && (
                    <Pagination>
                      <PageButton
                        disabled={usersPage === 0}
                        onClick={() => setUsersPage(usersPage - 1)}
                      >
                        {t("admin.previous")}
                      </PageButton>
                      <span>
                        {t("admin.pageOf", { current: usersPage + 1, total: usersTotalPages })}
                      </span>
                      <PageButton
                        disabled={usersPage >= usersTotalPages - 1}
                        onClick={() => setUsersPage(usersPage + 1)}
                      >
                        {t("admin.next")}
                      </PageButton>
                    </Pagination>
                  )}
                </>
              )}
            </>
          )}

          {activeTab === "export" && (
            <ExportSection>
              <ExportTitle>{t("admin.export.title")}</ExportTitle>
              <ExportDescription>
                {t("admin.export.description")}
              </ExportDescription>

              <ExportOptions>
                <ExportRow>
                  <ExportLabel>{t("admin.export.dataType")}</ExportLabel>
                  <Select value={exportType} onChange={(e) => setExportType(e.target.value)}>
                    <option value="activities">{t("admin.export.activities")}</option>
                    <option value="users">{t("admin.export.users")}</option>
                  </Select>
                </ExportRow>

                <ExportRow>
                  <ExportLabel>{t("admin.export.format")}</ExportLabel>
                  <FormatCards>
                    <FormatCard
                      selected={exportFormat === "txt"}
                      onClick={() => setExportFormat("txt")}
                    >
                      <FormatIcon>
                        <FiFileText size={24} />
                      </FormatIcon>
                      <FormatName selected={exportFormat === "txt"}>TXT</FormatName>
                    </FormatCard>
                    <FormatCard
                      selected={exportFormat === "xlsx"}
                      onClick={() => setExportFormat("xlsx")}
                    >
                      <FormatIcon>
                        <FiFileText size={24} />
                      </FormatIcon>
                      <FormatName selected={exportFormat === "xlsx"}>XLSX</FormatName>
                    </FormatCard>
                    <FormatCard
                      selected={exportFormat === "pdf"}
                      onClick={() => setExportFormat("pdf")}
                    >
                      <FormatIcon>
                        <FiFileText size={24} />
                      </FormatIcon>
                      <FormatName selected={exportFormat === "pdf"}>PDF</FormatName>
                    </FormatCard>
                  </FormatCards>
                </ExportRow>

                <ExportRow>
                  <Checkbox>
                    <input
                      type="checkbox"
                      checked={includeAllData}
                      onChange={(e) => setIncludeAllData(e.target.checked)}
                    />
                    {t("admin.export.includeAll")}
                  </Checkbox>
                </ExportRow>

                <ExportButton onClick={handleExport}>
                  <FiDownload size={18} />
                  {t("admin.export.exportButton")}
                </ExportButton>
              </ExportOptions>
            </ExportSection>
          )}
        </TabContent>
      </Container>
    </>
  );
};

export default Admin;
