import { useEffect, useState } from "react";
import {
  Routes,
  Route,
  Navigate,
  NavLink,
  Outlet,
  Link,
} from "react-router-dom";
import {
  Waypoints,
  LayoutDashboard,
  ClipboardList,
  Route as RouteIcon,
  ChartNoAxesCombined,
  BookOpen,
  Users,
  Network,
  LogOut,
  Menu,
  X,
} from "lucide-react";
import { api, errorMessage, clearDrafts } from "./api/client";
import { AuthContext, useAuth } from "./auth";
import { Login, Register, ForgotPassword, ResetPassword } from "./pages/Auth";
import { ErrorBox } from "./components/UI";
import ThemeToggle from "./components/ThemeToggle";
import {
  StudentDashboard,
  TeacherDashboard,
  Progress,
  StudentReport,
  Results,
  StudyPath,
} from "./pages/Reports";
import { Assessments, Quiz } from "./pages/Assessments";
import { Topics, Materials, Classes, AssessmentEditor } from "./pages/Manage";
export { useAuth };
function Protected({ role }) {
  const { user, ready } = useAuth();
  if (!ready) return <div className="state">Opening GapMap…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return <Outlet />;
}
function Shell() {
  const { user, setUser } = useAuth();
  const [open, setOpen] = useState(false),
    [error, setError] = useState("");
  const teacher = user.role === "TEACHER";
  const links = teacher
    ? [
        ["/", "Overview", LayoutDashboard],
        ["/assessments", "Assessments", ClipboardList],
        ["/topics", "Topics & prerequisites", Network],
        ["/materials", "Learning materials", BookOpen],
        ["/classes", "My classes", Users],
      ]
    : [
        ["/", "Overview", LayoutDashboard],
        ["/assessments", "Assessments", ClipboardList],
        ["/study-path", "My learning path", RouteIcon],
        ["/progress", "My progress", ChartNoAxesCombined],
        ["/classes", "My classes", Users],
      ];
  async function logout() {
    try {
      await api.post("/auth/logout");
    } catch (e) {
      // No response at all means the cookie may still be live, so stay signed in and say so.
      // Any HTTP answer (e.g. an already-expired session) is safe to treat as signed out.
      if (!e.response) {
        setError(errorMessage(e));
        return;
      }
    }
    clearDrafts();
    setUser(null);
  }
  return (
    <div className="app-shell">
      {open && (
        <button
          className="overlay"
          onClick={() => setOpen(false)}
          aria-label="Close navigation"
        />
      )}
      <aside className={"sidebar " + (open ? "open" : "")}>
        <Link to="/" className="brand">
          <Waypoints />
          <span>
            GapMap<span className="brand-dot">.</span>
          </span>
        </Link>
        <p className="sidebar-label">
          {teacher ? "TEACHER WORKSPACE" : "STUDENT WORKSPACE"}
        </p>
        <nav>
          {links.map(([path, title, Icon]) => (
            <NavLink
              key={path}
              to={path}
              end={path === "/"}
              onClick={() => setOpen(false)}
            >
              <Icon size={19} />
              {title}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-note">
          <span className="tiny-star">✦</span>
          <h3>
            {teacher
              ? "Make every insight count."
              : "Small steps. Real progress."}
          </h3>
          <p>
            {teacher
              ? "Turn classroom patterns into meaningful support."
              : "A little practice today makes tomorrow a little easier."}
          </p>
        </div>
        <div className="profile">
          <div className="avatar">
            {user.name
              .split(" ")
              .map((x) => x[0])
              .slice(0, 2)
              .join("")}
          </div>
          <div>
            <strong>{user.name}</strong>
            <small>{teacher ? "Teacher" : "Student"}</small>
          </div>
          <button
            className="icon-button"
            onClick={logout}
            aria-label="Sign out"
          >
            <LogOut size={17} />
          </button>
        </div>
        <ErrorBox error={error} />
      </aside>
      <div className="workspace">
        <div className="topbar">
          <div>
            <button
              className="mobile-menu icon-button"
              onClick={() => setOpen(true)}
              aria-label="Open navigation"
            >
              <Menu />
            </button>
            <span>Workspace</span>
            <span className="crumb">/</span>
            <strong>{teacher ? "Teaching & insights" : "My learning"}</strong>
          </div>
          <div>
            <ThemeToggle />
            <span className="today">
              {new Date().toLocaleDateString(undefined, {
                weekday: "short",
                month: "short",
                day: "numeric",
              })}
            </span>
          </div>
        </div>
        <main className="content">
          <Outlet />
        </main>
        <footer>
          Made for progress, not just grades.{" "}
          <span>GapMap © {new Date().getFullYear()}</span>
        </footer>
      </div>
    </div>
  );
}
export default function App() {
  const [user, setUser] = useState(null),
    [ready, setReady] = useState(false);
  useEffect(() => {
    api
      .get("/auth/me")
      .then((r) => setUser(r.data))
      .catch(() => {})
      .finally(() => setReady(true));
    const id = api.interceptors.response.use(
      (r) => r,
      (e) => {
        if (e.response?.status === 401 && !e.config.url.includes("/auth/login")) {
          clearDrafts();
          setUser(null);
        }
        return Promise.reject(e);
      },
    );
    return () => api.interceptors.response.eject(id);
  }, []);
  return (
    <AuthContext.Provider value={{ user, setUser, ready }}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route element={<Protected />}>
          <Route element={<Shell />}>
            <Route
              index
              element={
                user?.role === "TEACHER" ? (
                  <TeacherDashboard />
                ) : (
                  <StudentDashboard />
                )
              }
            />
            <Route path="assessments" element={<Assessments />} />
            <Route path="classes" element={<Classes />} />
            <Route path="results/:id" element={<Results />} />
            <Route element={<Protected role="STUDENT" />}>
              <Route path="quiz/:id" element={<Quiz />} />
              <Route path="study-path" element={<StudyPath />} />
              <Route path="progress" element={<Progress />} />
            </Route>
            <Route element={<Protected role="TEACHER" />}>
              <Route path="topics" element={<Topics />} />
              <Route path="materials" element={<Materials />} />
              <Route path="assessments/new" element={<AssessmentEditor />} />
              <Route
                path="assessments/:id/edit"
                element={<AssessmentEditor />}
              />
              <Route path="students/:id" element={<StudentReport />} />
            </Route>
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthContext.Provider>
  );
}
