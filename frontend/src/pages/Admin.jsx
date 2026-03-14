import React, { useState, useEffect } from "react";
import { FiArrowLeft, FiUsers, FiActivity, FiMap, FiCalendar, FiPlus, FiDownload, FiFileText } from "react-icons/fi";
import * as XLSX from "xlsx";
import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import Navbar from "../components/Navbar/Navbar";
import AdminService from "../api/AdminService";
import { useToast } from "../context/ToastContext";
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
      // TODO: Add error reporting service
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
      // TODO: Add error reporting service
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
      // TODO: Add error reporting service
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
    if (window.confirm("Are you sure you want to delete this activity?")) {
      try {
        await AdminService.deleteActivity(activityId);
        loadActivities();
      } catch (error) {
        // TODO: Add error reporting service
      }
    }
  };

  const handleDeleteUser = async (userId) => {
    if (window.confirm("Are you sure you want to delete this user? This action cannot be undone.")) {
      try {
        await AdminService.deleteUser(userId);
        loadUsers();
      } catch (error) {
        // TODO: Add error reporting service
      }
    }
  };

  const formatDuration = (seconds) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const getInitials = (firstName, lastName) => {
    return `${(firstName || "")[0] || ""}${(lastName || "")[0] || ""}`.toUpperCase();
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
      toast.warning("No data to export. Please load data first.");
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
            <Title>Admin Panel</Title>
            <Subtitle>Manage users, activities, and export data</Subtitle>
          </HeaderContent>
        </Header>

        <StatsGrid>
          <StatCard>
            <StatIcon color="#3b82f6">
              <FiUsers size={24} />
            </StatIcon>
            <StatValue>{stats?.totalUsers || 0}</StatValue>
            <StatLabel>Total Users</StatLabel>
          </StatCard>
          <StatCard>
            <StatIcon color="#10b981">
              <FiActivity size={24} />
            </StatIcon>
            <StatValue>{stats?.totalActivities || 0}</StatValue>
            <StatLabel>Total Activities</StatLabel>
          </StatCard>
          <StatCard>
            <StatIcon color="#f59e0b">
              <FiMap size={24} />
            </StatIcon>
            <StatValue>{stats?.totalDistanceKm?.toFixed(0) || 0}</StatValue>
            <StatLabel>Total Distance (km)</StatLabel>
          </StatCard>
          <StatCard>
            <StatIcon color="#8b5cf6">
              <FiCalendar size={24} />
            </StatIcon>
            <StatValue>{stats?.activitiesThisMonth || 0}</StatValue>
            <StatLabel>This Month</StatLabel>
          </StatCard>
        </StatsGrid>

        <TabsContainer>
          <Tab active={activeTab === "activities"} onClick={() => setActiveTab("activities")}>
            Activities
          </Tab>
          <Tab active={activeTab === "users"} onClick={() => setActiveTab("users")}>
            Users
          </Tab>
          <Tab active={activeTab === "export"} onClick={() => setActiveTab("export")}>
            Export
          </Tab>
        </TabsContainer>

        <TabContent>
          {activeTab === "activities" && (
            <>
              <TableHeader>
                <form onSubmit={handleSearch}>
                  <SearchInput
                    type="text"
                    placeholder="Search activities..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </form>
                <AddButton as="a" href="/activities/new">
                  <FiPlus size={18} />
                  Add Activity
                </AddButton>
              </TableHeader>
              {loading ? (
                <LoadingSpinner />
              ) : activities.length === 0 ? (
                <EmptyState>
                  <EmptyIcon>
                    <FiActivity size={48} />
                  </EmptyIcon>
                  <EmptyText>No activities found</EmptyText>
                </EmptyState>
              ) : (
                <>
                  <Table>
                    <thead>
                      <tr>
                        <Th>Activity</Th>
                        <Th>Type</Th>
                        <Th>Distance</Th>
                        <Th>Duration</Th>
                        <Th>Date</Th>
                        <Th>User</Th>
                        <Th>Actions</Th>
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
                              View
                            </ActionButton>
                            <ActionButton className="danger" onClick={() => handleDeleteActivity(activity.id)}>
                              Delete
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
                        Previous
                      </PageButton>
                      <span>
                        Page {activitiesPage + 1} of {activitiesTotalPages}
                      </span>
                      <PageButton
                        disabled={activitiesPage >= activitiesTotalPages - 1}
                        onClick={() => setActivitiesPage(activitiesPage + 1)}
                      >
                        Next
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
                  placeholder="Search users..."
                />
              </TableHeader>
              {loading ? (
                <LoadingSpinner />
              ) : users.length === 0 ? (
                <EmptyState>
                  <EmptyIcon>
                    <FiUsers size={48} />
                  </EmptyIcon>
                  <EmptyText>No users found</EmptyText>
                </EmptyState>
              ) : (
                <>
                  <Table>
                    <thead>
                      <tr>
                        <Th>User</Th>
                        <Th>Role</Th>
                        <Th>Status</Th>
                        <Th>Activities</Th>
                        <Th>Followers</Th>
                        <Th>Joined</Th>
                        <Th>Actions</Th>
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
                              {user.active ? "Active" : "Inactive"}
                            </Badge>
                          </Td>
                          <Td>{user.activitiesCount}</Td>
                          <Td>{user.followersCount}</Td>
                          <Td>{formatDate(user.createdAt)}</Td>
                          <Td>
                            <ActionButton as="a" href={`/profile/${user.id}`}>
                              View
                            </ActionButton>
                            <ActionButton className="danger" onClick={() => handleDeleteUser(user.id)}>
                              Delete
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
                        Previous
                      </PageButton>
                      <span>
                        Page {usersPage + 1} of {usersTotalPages}
                      </span>
                      <PageButton
                        disabled={usersPage >= usersTotalPages - 1}
                        onClick={() => setUsersPage(usersPage + 1)}
                      >
                        Next
                      </PageButton>
                    </Pagination>
                  )}
                </>
              )}
            </>
          )}

          {activeTab === "export" && (
            <ExportSection>
              <ExportTitle>Export Data</ExportTitle>
              <ExportDescription>
                Export your data in various formats for reporting and analysis.
              </ExportDescription>

              <ExportOptions>
                <ExportRow>
                  <ExportLabel>Data Type</ExportLabel>
                  <Select value={exportType} onChange={(e) => setExportType(e.target.value)}>
                    <option value="activities">Activities</option>
                    <option value="users">Users</option>
                  </Select>
                </ExportRow>

                <ExportRow>
                  <ExportLabel>Format</ExportLabel>
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
                    Include all pages (may take longer for large datasets)
                  </Checkbox>
                </ExportRow>

                <ExportButton onClick={handleExport}>
                  <FiDownload size={18} />
                  Export Data
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
