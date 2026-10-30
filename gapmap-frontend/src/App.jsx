import { createContext, useContext, useEffect, useState } from "react";
import {
  Routes,
  Route,
  Navigate,
  NavLink,
  Outlet,
  useNavigate,
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
  ArrowUpRight,
  GraduationCap,
} from "lucide-react";
import { api, errorMessage } from "./api/client";
import { ErrorBox } from "./components/UI";
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
const AuthContext = createContext();
export const useAuth = () => useContext(AuthContext);
function Protected({ role }) {
  const { user, ready } = useAuth();
  if (!ready) return <div className="state">Opening GapMap…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return <Outlet />;
}
function AuthPage() {
  const [register, setRegister] = useState(false),
    [form, setForm] = useState({
      name: "",
      email: "",
      password: "",
      role: "STUDENT",
      teacherCode: "",
    }),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const { user, setUser } = useAuth(),
    navigate = useNavigate();
  const [demo, setDemo] = useState(false);
  useEffect(() => {
    api
      .get("/health")
      .then((r) => setDemo(r.data.demoLogin))
      .catch(() => {});
  }, []);
  async function demoSignIn(role) {
    setBusy(true);
    setError("");
    try {
      const { data } = await api.post("/auth/demo", { role });
      setUser(data);
      navigate("/");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  if (user) return <Navigate to="/" replace />;
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const { data } = await api.post(
        "/auth/" + (register ? "register" : "login"),
        form,
      );
      setUser(data);
      navigate("/");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-page">
      <aside className="auth-story">
        <Link className="brand" to="/">
          <Waypoints />
          <span>
            GapMap<span className="brand-dot">.</span>
          </span>
        </Link>
        <div>
          <div className="pill">A LITTLE CLARITY. A LOT OF POSSIBILITY.</div>
          <h1>
            Every learning gap
            <br />
            is a place
            <br />
            <em>to grow.</em>
          </h1>
          <p>
            Understand where you are. Discover what comes next. Build
            confidence, one topic at a time.
          </p>
          <div className="map-art">
            <span>
              Variables <b>✓</b>
            </span>
            <i />
            <span>
              Loops <b>↗</b>
            </span>
            <i />
            <span>
              Arrays <b>○</b>
            </span>
          </div>
        </div>
        <small>GAPMAP / LEARNING WITH DIRECTION</small>
      </aside>
      <main className="auth-form">
        <div>
          <div className="icon-box">
            <GraduationCap />
          </div>
          <p className="eyebrow">LET’S FIND YOUR NEXT STEP</p>
          <h1>{register ? "Start your learning journey" : "Welcome back."}</h1>
          <p className="muted">
            {register
              ? "Create an account to make learning more personal."
              : "Your next breakthrough starts with a little insight."}
          </p>
          <form onSubmit={submit}>
            {register && (
              <label>
                Full name
                <input
                  name="name"
                  value={form.name}
                  onChange={update}
                  required
                  maxLength={100}
                  autoComplete="name"
                />
              </label>
            )}
            <label>
              Email address
              <input
                name="email"
                type="email"
                value={form.email}
                onChange={update}
                required
                autoComplete="email"
                placeholder="you@example.com"
              />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                value={form.password}
                onChange={update}
                required
                minLength={register ? 8 : undefined}
                maxLength={72}
                autoComplete={register ? "new-password" : "current-password"}
                placeholder="Enter your password"
              />
            </label>
            {register && (
              <>
                <label>
                  I am a
                  <select name="role" value={form.role} onChange={update}>
                    <option value="STUDENT">Student</option>
                    <option value="TEACHER">Teacher</option>
                  </select>
                </label>
                {form.role === "TEACHER" && (
                  <label>
                    Teacher invitation code
                    <input
                      name="teacherCode"
                      value={form.teacherCode}
                      onChange={update}
                      required
                    />
                  </label>
                )}
              </>
            )}
            <ErrorBox error={error} />
            <button className="primary full" disabled={busy}>
              {busy ? "Please wait…" : register ? "Create account" : "Sign in"}{" "}
              <ArrowUpRight size={18} />
            </button>
          </form>
          <p className="auth-switch">
            {register ? "Already have an account?" : "New to GapMap?"}{" "}
            <button
              className="link-button"
              onClick={() => {
                setRegister(!register);
                setError("");
              }}
            >
              {register ? "Sign in" : "Create an account"}
            </button>
          </p>
          {demo && (
            <div className="demo-box">
              <strong>Explore the demo</strong>
              {demo && (
                <div className="form-actions">
                  <button
                    className="secondary"
                    disabled={busy}
                    onClick={() => demoSignIn("STUDENT")}
                  >
                    Student demo
                  </button>
                  <button
                    className="secondary"
                    disabled={busy}
                    onClick={() => demoSignIn("TEACHER")}
                  >
                    Teacher demo
                  </button>
                </div>
              )}
              <p>
                Student: student1@gapmap.test
                <br />
                Teacher: teacher@gapmap.test
                <br />
                Password: GapMap123!
              </p>
              <small>Available when the demo profile is enabled.</small>
            </div>
          )}
        </div>
      </main>
    </div>
  );
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
      for (const key of Object.keys(sessionStorage))
        if (key.startsWith("gapmap-")) sessionStorage.removeItem(key);
      setUser(null);
    } catch (e) {
      setError(errorMessage(e));
    }
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
          <span className="today">
            {new Date().toLocaleDateString(undefined, {
              weekday: "short",
              month: "short",
              day: "numeric",
            })}
          </span>
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
        if (e.response?.status === 401 && !e.config.url.includes("/auth/login"))
          setUser(null);
        return Promise.reject(e);
      },
    );
    return () => api.interceptors.response.eject(id);
  }, []);
  return (
    <AuthContext.Provider value={{ user, setUser, ready }}>
      <Routes>
        <Route path="/login" element={<AuthPage />} />
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
