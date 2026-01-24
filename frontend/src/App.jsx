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

const App = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/activities" element={<Activities />} />
        <Route path="/activities/new" element={<RecordActivity />} />
        <Route path="/activities/:id" element={<ActivityDetails />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/profile/:id" element={<Profile />} />
        <Route path="/goals" element={<Goals />} />
        <Route path="/challenges" element={<Challenges />} />
        <Route path="/clubs" element={<Clubs />} />
        <Route path="/events" element={<Events />} />
      </Routes>
    </Router>
  );
};

export default App;
