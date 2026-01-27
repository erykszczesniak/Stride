import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import Activities from "./pages/Activities";
import ActivityDetails from "./pages/ActivityDetails";
import RecordActivity from "./pages/RecordActivity";
import Profile from "./pages/Profile";
import Goals from "./pages/Goals";
import Challenges from "./pages/Challenges";
import Clubs from "./pages/Clubs";
import Events from "./pages/Events";
import Explore from "./pages/Explore";
import Admin from "./pages/Admin";
import Settings from "./pages/Settings";
import TrainingPlans from "./pages/TrainingPlans";

const App = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/explore" element={<Explore />} />
        <Route path="/activities" element={<Activities />} />
        <Route path="/activities/new" element={<RecordActivity />} />
        <Route path="/activities/:id" element={<ActivityDetails />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/profile/:id" element={<Profile />} />
        <Route path="/goals" element={<Goals />} />
        <Route path="/challenges" element={<Challenges />} />
        <Route path="/clubs" element={<Clubs />} />
        <Route path="/events" element={<Events />} />
        <Route path="/admin" element={<Admin />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="/training" element={<TrainingPlans />} />
      </Routes>
    </Router>
  );
};

export default App;
