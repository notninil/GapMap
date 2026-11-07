import { useEffect, useState } from "react";
import {
  Link,
  Navigate,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import {
  ArrowUpRight,
  CheckCircle2,
  Eye,
  EyeOff,
  GraduationCap,
  KeyRound,
  MailCheck,
  ShieldCheck,
  Waypoints,
} from "lucide-react";
import { api, errorMessage, clearDrafts } from "../api/client";
import { useAuth } from "../auth";
import { ErrorBox } from "../components/UI";
import ThemeToggle from "../components/ThemeToggle";

function Notice({ children }) {
  return children ? (
    <div role="status" className="success">
      <CheckCircle2 size={17} />
      <span>{children}</span>
    </div>
  ) : null;
}

function PasswordField({
  label,
  name,
  value,
  onChange,
  autoComplete,
  placeholder = "Enter your password",
  hint,
  minLength,
}) {
  const [show, setShow] = useState(false);
  return (
    <label>
      {label}
      <span className="password-field">
        <input
          name={name}
          type={show ? "text" : "password"}
          value={value}
          onChange={onChange}
          required
          minLength={minLength}
          maxLength={72}
          autoComplete={autoComplete}
          placeholder={placeholder}
        />
        <button
          type="button"
          className="icon-button"
          onClick={() => setShow(!show)}
          aria-label={show ? "Hide password" : "Show password"}
          aria-pressed={show}
        >
          {show ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      </span>
      {hint && <small className="field-hint">{hint}</small>}
    </label>
  );
}

function AuthShell({ icon: Icon = GraduationCap, eyebrow, title, subtitle, children }) {
  return (
    <div className="auth-page">
      <ThemeToggle floating />
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
            <Icon />
          </div>
          <p className="eyebrow">{eyebrow}</p>
          <h1>{title}</h1>
          <p className="muted">{subtitle}</p>
          {children}
        </div>
      </main>
    </div>
  );
}

function useAuthForm(initial) {
  const [form, setForm] = useState(initial),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  return { form, update, error, setError, busy, setBusy };
}

export function Login() {
  const { user, setUser } = useAuth(),
    navigate = useNavigate(),
    location = useLocation();
  const { form, update, error, setError, busy, setBusy } = useAuthForm({
    email: "",
    password: "",
  });
  const [demo, setDemo] = useState(false);
  useEffect(() => {
    api
      .get("/health")
      .then((r) => setDemo(r.data.demoLogin))
      .catch(() => {});
  }, []);
  if (user) return <Navigate to="/" replace />;
  async function signIn(request) {
    setBusy(true);
    setError("");
    try {
      const { data } = await request();
      clearDrafts();
      setUser(data);
      navigate("/");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const submit = (e) => {
    e.preventDefault();
    signIn(() => api.post("/auth/login", form));
  };
  return (
    <AuthShell
      eyebrow="LET’S FIND YOUR NEXT STEP"
      title="Welcome back."
      subtitle="Your next breakthrough starts with a little insight."
    >
      <form onSubmit={submit}>
        <Notice>{location.state?.notice}</Notice>
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
        <PasswordField
          label="Password"
          name="password"
          value={form.password}
          onChange={update}
          autoComplete="current-password"
        />
        <div className="auth-aside">
          <Link className="text-link" to="/forgot-password">
            Forgot password?
          </Link>
        </div>
        <ErrorBox error={error} />
        <button className="primary full" disabled={busy}>
          {busy ? "Please wait…" : "Sign in"} <ArrowUpRight size={18} />
        </button>
      </form>
      <p className="auth-switch">
        New to GapMap?{" "}
        <Link className="text-link" to="/register">
          Create an account
        </Link>
      </p>
      {demo && (
        <div className="demo-box">
          <strong>Explore the demo</strong>
          <div className="form-actions">
            <button
              className="secondary"
              disabled={busy}
              onClick={() =>
                signIn(() => api.post("/auth/demo", { role: "STUDENT" }))
              }
            >
              Student demo
            </button>
            <button
              className="secondary"
              disabled={busy}
              onClick={() =>
                signIn(() => api.post("/auth/demo", { role: "TEACHER" }))
              }
            >
              Teacher demo
            </button>
          </div>
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
    </AuthShell>
  );
}

export function Register() {
  const { user, setUser } = useAuth(),
    navigate = useNavigate();
  const { form, update, error, setError, busy, setBusy } = useAuthForm({
    name: "",
    email: "",
    password: "",
    confirm: "",
    role: "STUDENT",
    teacherCode: "",
  });
  if (user) return <Navigate to="/" replace />;
  async function submit(e) {
    e.preventDefault();
    if (form.password !== form.confirm) {
      setError("The two passwords don’t match.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const { confirm, ...payload } = form;
      if (payload.role !== "TEACHER") delete payload.teacherCode;
      const { data } = await api.post("/auth/register", payload);
      clearDrafts();
      setUser(data);
      navigate("/");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <AuthShell
      eyebrow="LET’S FIND YOUR NEXT STEP"
      title="Start your learning journey"
      subtitle="Create an account to make learning more personal."
    >
      <form onSubmit={submit}>
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
        <label>
          Email address
          <input
            name="email"
            type="email"
            value={form.email}
            onChange={update}
            required
            maxLength={200}
            autoComplete="email"
            placeholder="you@example.com"
          />
        </label>
        <PasswordField
          label="Password"
          name="password"
          value={form.password}
          onChange={update}
          autoComplete="new-password"
          minLength={8}
          placeholder="At least 8 characters"
          hint="Use 8–72 characters."
        />
        <PasswordField
          label="Confirm password"
          name="confirm"
          value={form.confirm}
          onChange={update}
          autoComplete="new-password"
          minLength={8}
          placeholder="Re-enter your password"
        />
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
              autoComplete="off"
            />
          </label>
        )}
        <ErrorBox error={error} />
        <button className="primary full" disabled={busy}>
          {busy ? "Please wait…" : "Create account"} <ArrowUpRight size={18} />
        </button>
      </form>
      <p className="auth-switch">
        Already have an account?{" "}
        <Link className="text-link" to="/login">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}

export function ForgotPassword() {
  const { user } = useAuth();
  const { form, update, error, setError, busy, setBusy } = useAuthForm({
    email: "",
  });
  const [sent, setSent] = useState(null);
  if (user) return <Navigate to="/" replace />;
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const { data } = await api.post("/auth/forgot-password", form);
      setSent(data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  let demoLink = null;
  if (sent?.demoResetLink) {
    try {
      const u = new URL(sent.demoResetLink);
      demoLink = u.pathname + u.search;
    } catch {}
  }
  return (
    <AuthShell
      icon={sent ? MailCheck : KeyRound}
      eyebrow="NO WORRIES, IT HAPPENS"
      title={sent ? "Check your inbox" : "Reset your password"}
      subtitle={
        sent
          ? "If an account exists for that email, a reset link is on its way. It expires soon, so use it right away."
          : "Enter the email you signed up with and we’ll prepare a link to choose a new password."
      }
    >
      {sent ? (
        <>
          {demoLink && (
            <div className="demo-box">
              <strong>Demo mode</strong>
              <p>No mail server is connected, so here is your link:</p>
              <Link className="text-link" to={demoLink}>
                Open reset link <ArrowUpRight size={14} />
              </Link>
            </div>
          )}
          <p className="auth-switch">
            <Link className="text-link" to="/login">
              Back to sign in
            </Link>
          </p>
        </>
      ) : (
        <>
          <form onSubmit={submit}>
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
            <ErrorBox error={error} />
            <button className="primary full" disabled={busy}>
              {busy ? "Please wait…" : "Send reset link"}{" "}
              <ArrowUpRight size={18} />
            </button>
          </form>
          <p className="auth-switch">
            Remembered it?{" "}
            <Link className="text-link" to="/login">
              Sign in
            </Link>
          </p>
        </>
      )}
    </AuthShell>
  );
}

export function ResetPassword() {
  const { user } = useAuth(),
    navigate = useNavigate(),
    [params] = useSearchParams();
  const token = params.get("token") || "";
  const { form, update, error, setError, busy, setBusy } = useAuthForm({
    password: "",
    confirm: "",
  });
  if (user) return <Navigate to="/" replace />;
  async function submit(e) {
    e.preventDefault();
    if (form.password !== form.confirm) {
      setError("The two passwords don’t match.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await api.post("/auth/reset-password", {
        token,
        password: form.password,
      });
      navigate("/login", {
        replace: true,
        state: { notice: "Password updated. Sign in with your new password." },
      });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  if (!token)
    return (
      <AuthShell
        icon={ShieldCheck}
        eyebrow="LINK PROBLEM"
        title="This link isn’t valid"
        subtitle="The reset link is missing or incomplete. Request a fresh one to continue."
      >
        <Link className="primary full" to="/forgot-password">
          Request a new link <ArrowUpRight size={18} />
        </Link>
      </AuthShell>
    );
  return (
    <AuthShell
      icon={ShieldCheck}
      eyebrow="ALMOST THERE"
      title="Choose a new password"
      subtitle="Pick something you haven’t used here before. You’ll be signed out everywhere else."
    >
      <form onSubmit={submit}>
        <PasswordField
          label="New password"
          name="password"
          value={form.password}
          onChange={update}
          autoComplete="new-password"
          minLength={8}
          placeholder="At least 8 characters"
          hint="Use 8–72 characters."
        />
        <PasswordField
          label="Confirm new password"
          name="confirm"
          value={form.confirm}
          onChange={update}
          autoComplete="new-password"
          minLength={8}
          placeholder="Re-enter your password"
        />
        <ErrorBox error={error} />
        {error && error.includes("invalid or has expired") && (
          <p className="auth-switch">
            <Link className="text-link" to="/forgot-password">
              Request a new link
            </Link>
          </p>
        )}
        <button className="primary full" disabled={busy}>
          {busy ? "Please wait…" : "Update password"} <ArrowUpRight size={18} />
        </button>
      </form>
    </AuthShell>
  );
}
