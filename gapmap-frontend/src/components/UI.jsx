import { useEffect, useState, lazy, Suspense } from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  ArrowRight,
  BookOpen,
  AlertCircle,
  LoaderCircle,
} from "lucide-react";
import { api, errorMessage } from "../api/client";
export const label = (x) =>
  String(x || "")
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (x) => x.toUpperCase());
export const percent = (x) => `${Math.round(x || 0)}%`;
export const date = (x) =>
  new Date(x).toLocaleDateString(undefined, { month: "short", day: "numeric" });
export function useData(url) {
  const [data, setData] = useState(null),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    api
      .get(url)
      .then((r) => {
        if (active) setData(r.data);
      })
      .catch((e) => {
        if (active) setError(errorMessage(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [url, version]);
  return { data, error, loading, reload: () => setVersion((v) => v + 1) };
}
export function Load({ state, children }) {
  if (state.loading)
    return (
      <div className="state">
        <LoaderCircle className="spin" /> Loading your learning space…
      </div>
    );
  if (state.error)
    return (
      <div className="state">
        <AlertCircle />
        <p>{state.error}</p>
        <button onClick={state.reload}>Try again</button>
      </div>
    );
  return children(state.data);
}
export function ErrorBox({ error }) {
  return error ? (
    <div role="alert" className="error">
      <AlertCircle size={17} />
      {error}
    </div>
  ) : null;
}
export function Empty({ title = "Nothing here yet", children }) {
  return (
    <div className="empty">
      <BookOpen />
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}
export function Heading({ eyebrow, title, description, children }) {
  return (
    <header className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow || "YOUR LEARNING, MAPPED"}</p>
        <h1>{title}</h1>
        {description && <p className="muted">{description}</p>}
      </div>
      {children}
    </header>
  );
}
export function Badge({ value }) {
  return (
    <span className={"badge " + String(value).toLowerCase()}>
      {label(value)}
    </span>
  );
}
export function Metric({ title, value, note, icon: Icon }) {
  return (
    <div className="metric">
      <div className="metric-label">
        {title}
        {Icon && <Icon size={18} />}
      </div>
      <strong>{value}</strong>
      <small>{note}</small>
    </div>
  );
}
export function TopicBars({ topics = [] }) {
  return topics.length ? (
    <div className="topic-bars">
      {topics.map((t) => (
        <div key={t.topicId}>
          <div className="row">
            <strong>{t.topic}</strong>
            <span>{percent(t.scorePercent)}</span>
          </div>
          <div className="track">
            <div
              className={t.masteryLevel.toLowerCase()}
              style={{ width: `${t.scorePercent}%` }}
            />
          </div>
          <small>{label(t.masteryLevel)}</small>
        </div>
      ))}
    </div>
  ) : (
    <Empty title="Your map starts here">
      Take an assessment to see your topic strengths.
    </Empty>
  );
}
const LazyProgressChart = lazy(() => import("./ProgressChart"));
export function ProgressChart(props) {
  return (
    <Suspense fallback={<div className="state">Loading chart…</div>}>
      <LazyProgressChart {...props} />
    </Suspense>
  );
}
export function RecentAttempts({ attempts = [] }) {
  return attempts.length ? (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Assessment</th>
            <th>Date</th>
            <th>Score</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {[...attempts]
            .reverse()
            .slice(0, 8)
            .map((a) => (
              <tr key={a.id}>
                <td>
                  <strong>{a.assessment}</strong>
                  <small>{a.miniQuiz ? "Mini-quiz" : "Assessment"}</small>
                </td>
                <td>{date(a.submittedAt)}</td>
                <td>
                  <span className="score-pill">{percent(a.totalScore)}</span>
                </td>
                <td>
                  <Link
                    className="text-link"
                    to={"/results/" + a.id}
                    aria-label={"View " + a.assessment}
                  >
                    View <ArrowUpRight size={14} />
                  </Link>
                </td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  ) : (
    <Empty title="Ready when you are">
      Complete an assessment to get a personalized learning map.
    </Empty>
  );
}
export function CardTitle({ title, subtitle, link, to }) {
  return (
    <div className="card-title">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {to && (
        <Link className="text-link" to={to}>
          {link || "View all"} <ArrowRight size={15} />
        </Link>
      )}
    </div>
  );
}
export function downloadCSV(name, rows) {
  const safe = (x) => {
    let s = String(x ?? "");
    if (/^[=+@-]/.test(s)) s = "'" + s;
    return '"' + s.replaceAll('"', '""') + '"';
  };
  const blob = new Blob(
    ["\ufeff" + rows.map((r) => r.map(safe).join(",")).join("\r\n")],
    { type: "text/csv;charset=utf-8;" },
  );
  const url = URL.createObjectURL(blob),
    a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
