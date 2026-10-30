import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowRight,
  ArrowUpRight,
  Target,
  TrendingUp,
  ClipboardCheck,
  Route,
  CheckCircle2,
  BookOpen,
  Lightbulb,
  Download,
  Users,
  AlertTriangle,
  Check,
  Play,
} from "lucide-react";
import { useAuth } from "../App";
import { api, errorMessage } from "../api/client";
import {
  useData,
  Load,
  Heading,
  Metric,
  TopicBars,
  ProgressChart,
  RecentAttempts,
  CardTitle,
  Badge,
  Empty,
  ErrorBox,
  percent,
  date,
  label,
  downloadCSV,
} from "../components/UI";
export function StudentDashboard() {
  const state = useData("/dashboards/student");
  const { user } = useAuth();
  return (
    <Load state={state}>
      {(d) => {
        const strong = d.mastery.filter(
          (t) => t.masteryLevel === "STRONG",
        ).length;
        const pending = d.recommendations.filter((r) => r.status === "PENDING");
        const latest = d.attempts.at(-1);
        return (
          <>
            <Heading
              eyebrow="A CLEARER PICTURE OF YOUR LEARNING"
              title={`Let’s keep growing, ${user.name.split(" ")[0]}.`}
              description="Your strengths, your next steps, and the progress in between."
            >
              <Link className="primary" to="/assessments">
                Take an assessment <ArrowUpRight size={17} />
              </Link>
            </Heading>
            <div className="hero-card">
              <div>
                <span className="pill">YOUR NEXT CHAPTER</span>
                <h2>
                  {pending.length
                    ? "A small step toward a stronger foundation."
                    : "You’re ready for your next challenge."}
                </h2>
                <p>
                  {pending.length
                    ? `Start with ${pending[0].topic}. Your learning path puts the foundations first, so every next step feels a little easier.`
                    : "Take an assessment to discover your strengths and find the topics worth practicing."}
                </p>
                <Link
                  to={pending.length ? "/study-path" : "/assessments"}
                  className="hero-link"
                >
                  {pending.length
                    ? "Continue my learning path"
                    : "Explore assessments"}{" "}
                  <ArrowRight size={17} />
                </Link>
              </div>
              <div className="journey-art" aria-hidden="true">
                <div className="orbit one" />
                <div className="orbit two" />
                <div className="orbit three" />
                <span className="journey-node first">
                  <Check size={20} />
                </span>
                <span className="journey-node middle">
                  <BookOpen size={25} />
                </span>
                <span className="journey-node last">
                  <Target size={23} />
                </span>
                <span className="art-label">One step closer.</span>
              </div>
            </div>
            <div className="metrics">
              <Metric
                title="Latest assessment"
                value={latest ? percent(latest.totalScore) : "—"}
                note={latest ? latest.assessment : "Your starting point awaits"}
                icon={ClipboardCheck}
              />
              <Metric
                title="Strong topics"
                value={`${strong} / ${d.mastery.length}`}
                note="Keep building on what you know"
                icon={Target}
              />
              <Metric
                title="Topics to work on"
                value={new Set(d.gaps.map((g) => g.topicId)).size}
                note="Opportunities for your next step"
                icon={Route}
              />
              <Metric
                title="Activities completed"
                value={
                  d.recommendations.filter((r) => r.status === "DONE").length
                }
                note="In your current learning path"
                icon={CheckCircle2}
              />
            </div>
            <div className="dashboard-grid">
              <section className="card">
                <CardTitle
                  title="Your topic map"
                  subtitle="Based on your latest result in each topic"
                  to="/progress"
                  link="Explore"
                />
                <TopicBars topics={d.mastery} />
                <div className="legend">
                  <span className="green-dot" />
                  Strong
                  <span className="amber-dot" />
                  Developing
                  <span className="red-dot" />
                  Needs practice
                </div>
              </section>
              <section className="card">
                <CardTitle
                  title="Progress, one attempt at a time"
                  subtitle="Every attempt is another step forward"
                  to="/progress"
                  link="Details"
                />
                <ProgressChart attempts={d.attempts} topics={d.mastery} />
                <div className="insight">
                  <Lightbulb size={19} />
                  <p>
                    Progress is personal. Focus on understanding one topic at a
                    time.
                  </p>
                </div>
              </section>
            </div>
            <div className="dashboard-grid wide-left">
              <section className="card">
                <CardTitle
                  title="Recent assessments"
                  subtitle="A little reflection goes a long way"
                />
                <RecentAttempts attempts={d.attempts} />
              </section>
              <section className="card next-card">
                <div className="icon-box">
                  <Route />
                </div>
                <p className="eyebrow">UP NEXT FOR YOU</p>
                <h2>
                  {pending[0]?.material.title || "Find your starting point"}
                </h2>
                <p>
                  {pending[0]?.explanation ||
                    "Your first assessment will help us build a path around your needs."}
                </p>
                <Link
                  className="secondary"
                  to={pending.length ? "/study-path" : "/assessments"}
                >
                  Let’s get started <ArrowRight size={16} />
                </Link>
              </section>
            </div>
          </>
        );
      }}
    </Load>
  );
}
export function TeacherDashboard() {
  const state = useData("/dashboards/teacher");
  return (
    <Load state={state}>
      {(d) => {
        const support = d.students.filter((s) =>
          s.mastery.some((t) => t.masteryLevel === "NEEDS_PRACTICE"),
        );
        return (
          <>
            <Heading
              eyebrow="SEE THE PATTERNS. SUPPORT THE PERSON."
              title="A clearer view of your classroom."
              description="Turn learning insights into meaningful next steps."
            >
              <Link to="/assessments/new" className="primary">
                Create assessment <ArrowUpRight size={17} />
              </Link>
            </Heading>
            <div className="metrics">
              <Metric
                title="Students"
                value={d.students.length}
                note="Across your classes"
                icon={Users}
              />
              <Metric
                title="Assessments"
                value={d.assessmentCount}
                note="Including topic mini-quizzes"
                icon={ClipboardCheck}
              />
              <Metric
                title="May need support"
                value={support.length}
                note="At least one topic needs practice"
                icon={Target}
              />
              <Metric
                title="Topics assessed"
                value={d.topics.length}
                note="Based on latest student results"
                icon={TrendingUp}
              />
            </div>
            <section className="card">
              <CardTitle
                title="The classroom learning map"
                subtitle="Latest mastery by student and topic. Select a name to see their report."
              />
              {d.students.length ? (
                <div className="table-wrap">
                  <table className="heatmap">
                    <thead>
                      <tr>
                        <th>Student</th>
                        {d.topics.map((t) => (
                          <th key={t.topicId}>{t.topic}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {d.students.map((s) => (
                        <tr key={s.student.id}>
                          <td>
                            <Link
                              className="text-link"
                              to={"/students/" + s.student.id}
                            >
                              {s.student.name} <ArrowUpRight size={13} />
                            </Link>
                          </td>
                          {d.topics.map((t) => {
                            const result = s.mastery.find(
                              (m) => m.topicId === t.topicId,
                            );
                            return (
                              <td key={t.topicId}>
                                {result ? (
                                  <span
                                    title={label(result.masteryLevel)}
                                    className={
                                      "heat-cell " +
                                      result.masteryLevel.toLowerCase()
                                    }
                                  >
                                    {percent(result.scorePercent)}
                                  </span>
                                ) : (
                                  <span title="Not assessed" className="muted">
                                    —
                                  </span>
                                )}
                              </td>
                            );
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <Empty title="Invite your first students">
                  Create a class and share its join code.
                </Empty>
              )}
              <div className="legend">
                <span className="green-dot" />
                Strong
                <span className="amber-dot" />
                Developing
                <span className="red-dot" />
                Needs practice
              </div>
            </section>
            <div className="dashboard-grid">
              <section className="card">
                <CardTitle
                  title="Where support matters most"
                  subtitle="Topics ordered from lowest to highest class average"
                />
                {d.topics.length ? (
                  d.topics.map((t) => (
                    <div className="weak-topic" key={t.topicId}>
                      <div className="row">
                        <strong>{t.topic}</strong>
                        <b>{percent(t.average)}</b>
                      </div>
                      <div className="track">
                        <div
                          className="developing"
                          style={{ width: t.average + "%" }}
                        />
                      </div>
                      <small>
                        {t.needsSupport} need practice · {t.misconceptions}{" "}
                        possible misconceptions · {t.assessed} assessed
                      </small>
                    </div>
                  ))
                ) : (
                  <Empty title="No results yet">
                    Class insights appear after students submit.
                  </Empty>
                )}
              </section>
              <section className="card">
                <CardTitle
                  title="A little extra attention"
                  subtitle="Students with a topic currently below Developing"
                />
                {support.length ? (
                  support.map((s) => (
                    <Link
                      key={s.student.id}
                      to={"/students/" + s.student.id}
                      className="student-row"
                    >
                      <div className="avatar">{s.student.name[0]}</div>
                      <div>
                        <strong>{s.student.name}</strong>
                        <small>
                          {s.mastery
                            .filter((t) => t.masteryLevel === "NEEDS_PRACTICE")
                            .map((t) => t.topic)
                            .join(", ")}
                        </small>
                      </div>
                      <ArrowUpRight size={18} />
                    </Link>
                  ))
                ) : (
                  <Empty title="No support flags">
                    No assessed students currently have a topic marked Needs
                    practice.
                  </Empty>
                )}
              </section>
            </div>
          </>
        );
      }}
    </Load>
  );
}
export function Progress() {
  const state = useData("/progress");
  return (
    <Load state={state}>
      {(d) => (
        <>
          <Heading
            title="Look how far you’ve come."
            description="Your complete assessment history, topic by topic."
          />
          <section className="card">
            <CardTitle
              title="Your learning over time"
              subtitle="Gaps in a line mean that topic was not assessed in that attempt."
            />
            <ProgressChart
              attempts={d.attempts}
              topics={d.mastery}
              height={350}
            />
          </section>
          <div className="dashboard-grid">
            <section className="card">
              <CardTitle title="Current topic mastery" />
              <TopicBars topics={d.mastery} />
            </section>
            <section className="card">
              <CardTitle title="All attempts" />
              {d.attempts.length ? (
                <div className="attempt-list">
                  {[...d.attempts].reverse().map((a) => (
                    <Link
                      to={"/results/" + a.id}
                      key={a.id}
                      className="student-row"
                    >
                      <div>
                        <strong>{a.assessment}</strong>
                        <small>
                          {date(a.submittedAt)} ·{" "}
                          {a.miniQuiz ? "Mini-quiz" : "Assessment"}
                        </small>
                      </div>
                      <b>{percent(a.totalScore)}</b>
                      <ArrowUpRight size={16} />
                    </Link>
                  ))}
                </div>
              ) : (
                <Empty />
              )}
            </section>
          </div>
        </>
      )}
    </Load>
  );
}
export function StudentReport() {
  const { id } = useParams(),
    state = useData("/students/" + id + "/report");
  return (
    <Load state={state}>
      {(d) => (
        <>
          <Heading
            eyebrow="INDIVIDUAL LEARNING REPORT"
            title={d.student.name}
            description={d.student.email}
          >
            <button
              className="secondary"
              onClick={() =>
                downloadCSV("gapmap-student-report.csv", [
                  ["Student", "Topic", "Latest score", "Mastery"],
                  ...d.mastery.map((t) => [
                    d.student.name,
                    t.topic,
                    t.scorePercent,
                    t.masteryLevel,
                  ]),
                ])
              }
            >
              <Download size={17} /> Export CSV
            </button>
          </Heading>
          <div className="dashboard-grid">
            <section className="card">
              <CardTitle title="Latest topic mastery" />
              <TopicBars topics={d.mastery} />
            </section>
            <section className="card">
              <CardTitle title="Progress over time" />
              <ProgressChart attempts={d.attempts} topics={d.mastery} />
            </section>
          </div>
          <section className="card">
            <CardTitle
              title="Identified learning gaps"
              subtitle="Possible misconceptions are signals to investigate, not diagnoses."
            />
            {d.gaps.length ? (
              <div className="gap-chips">
                {d.gaps.map((g) => (
                  <div key={g.id}>
                    <strong>{g.topic}</strong>
                    <Badge value={g.gapType} />
                  </div>
                ))}
              </div>
            ) : (
              <Empty title="No current gaps" />
            )}
          </section>
          <section className="card">
            <CardTitle title="Assessment history" />
            <RecentAttempts attempts={d.attempts} />
          </section>
          <section className="card">
            <CardTitle title="Recommended next steps" />
            {d.recommendations.map((r) => (
              <div className="report-rec" key={r.id}>
                <span className="step-number">{r.orderIndex}</span>
                <div>
                  <strong>{r.material.title}</strong>
                  <p>{r.explanation}</p>
                </div>
                <Badge value={r.status} />
              </div>
            ))}
          </section>
        </>
      )}
    </Load>
  );
}
export function Results() {
  const { id } = useParams(),
    state = useData("/results/" + id);
  return (
    <Load state={state}>
      {(d) => (
        <>
          <Heading
            eyebrow="ASSESSMENT REFLECTION"
            title={d.assessment}
            description={`Completed ${new Date(d.submittedAt).toLocaleString()}`}
          >
            <span className="result-score">
              {percent(d.totalScore)}
              <small>Overall score</small>
            </span>
          </Heading>
          <section className="card">
            <CardTitle
              title="The story behind your score"
              subtitle="Each topic is a new opportunity to understand a little more."
            />
            <TopicBars topics={d.topics} />
          </section>
          <section className="card">
            <CardTitle
              title="Review your thinking"
              subtitle="Compare your answer, confidence, and the explanation."
            />
            {d.answers.map((a, i) => (
              <div
                className={
                  "answer-review " + (a.correct ? "correct" : "incorrect")
                }
                key={a.questionId}
              >
                <div className="row">
                  <span className="eyebrow">QUESTION {i + 1}</span>
                  <Badge value={a.correct ? "CORRECT" : "INCORRECT"} />
                </div>
                <h3>{a.question}</h3>
                <p>
                  Your answer: <strong>{a.selected}</strong>
                </p>
                {!a.correct && (
                  <p>
                    Correct answer: <strong>{a.correctAnswer}</strong>
                  </p>
                )}
                <small>Confidence: {label(a.confidence)}</small>
                {a.misconception && (
                  <div className="misconception">
                    <Lightbulb size={17} />
                    Possible misconception: you were very sure of this incorrect
                    answer.
                  </div>
                )}
                <div className="explanation">{a.explanation}</div>
              </div>
            ))}
          </section>
          <Link className="secondary" to="/">
            Back to overview <ArrowRight size={17} />
          </Link>
        </>
      )}
    </Load>
  );
}
export function StudyPath() {
  const state = useData("/dashboards/student"),
    quizzes = useData("/assessments");
  const [error, setError] = useState(""),
    [busy, setBusy] = useState(null),
    [expanded, setExpanded] = useState(null);
  async function toggle(r) {
    setBusy(r.id);
    setError("");
    try {
      await api.patch("/recommendations/" + r.id, {
        done: r.status !== "DONE",
      });
      state.reload();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  }
  return (
    <Load state={state}>
      {(d) => {
        const done = d.recommendations.filter(
          (r) => r.status === "DONE",
        ).length;
        return (
          <>
            <Heading
              eyebrow="YOUR PERSONALIZED STUDY PATH"
              title="A little direction goes a long way."
              description="Start with the foundations. Build toward the bigger picture."
            >
              <button
                className="secondary"
                disabled={busy === "refresh"}
                onClick={async () => {
                  setBusy("refresh");
                  setError("");
                  try {
                    await api.post("/recommendations/refresh");
                    state.reload();
                  } catch (e) {
                    setError(errorMessage(e));
                  } finally {
                    setBusy(null);
                  }
                }}
              >
                Refresh learning path
              </button>
            </Heading>
            <div className="path-summary">
              <div className="icon-box">
                <Route />
              </div>
              <div>
                <h2>
                  {done} of {d.recommendations.length} activities complete
                </h2>
                <p>
                  Root gaps first, then direct gaps, then possible
                  misconceptions.
                </p>
              </div>
              <span className="score-pill">
                {d.recommendations.length
                  ? Math.round((done / d.recommendations.length) * 100)
                  : 0}
                %
              </span>
            </div>
            <ErrorBox error={error} />
            {d.gaps.length > 0 && (
              <div className="gap-chips">
                {d.gaps.map((g) => (
                  <div key={g.id}>
                    <strong>{g.topic}</strong>
                    <Badge value={g.gapType} />
                  </div>
                ))}
              </div>
            )}
            {!d.recommendations.length && (
              <Empty
                title={
                  d.gaps.length
                    ? "Materials are being prepared"
                    : "Your next step starts with an assessment"
                }
              >
                {d.gaps.length
                  ? "Your teacher has not added materials for these gaps yet."
                  : "Complete an assessment to receive a study path."}
              </Empty>
            )}
            <div className="study-path">
              {d.recommendations.map((r, i) => {
                const mini = quizzes.data?.find(
                  (a) => a.miniQuiz && a.topicId === r.topicId,
                );
                return (
                  <article
                    key={r.id}
                    className={
                      "path-step " + (r.status === "DONE" ? "done" : "")
                    }
                  >
                    <div className="path-number">
                      {r.status === "DONE" ? <Check size={20} /> : i + 1}
                    </div>
                    <section className="card">
                      <div className="row">
                        <div className="badges">
                          <Badge value={r.gapType} />
                          <span className="muted">
                            {r.topic} · {label(r.material.type)}
                          </span>
                        </div>
                        <Badge value={r.status} />
                      </div>
                      <h2>{r.material.title}</h2>
                      <div className="why">
                        <Lightbulb size={17} />
                        <div>
                          <strong>Why am I seeing this?</strong>
                          <p>{r.explanation}</p>
                        </div>
                      </div>
                      <button
                        className="text-link"
                        onClick={() =>
                          setExpanded(expanded === r.id ? null : r.id)
                        }
                      >
                        {expanded === r.id ? "Close activity" : "Open activity"}{" "}
                        <ArrowRight size={16} />
                      </button>
                      {expanded === r.id && (
                        <div className="lesson-content">
                          {["LINK", "VIDEO"].includes(r.material.type) ? (
                            <a
                              href={r.material.urlOrContent}
                              target="_blank"
                              rel="noreferrer"
                              className="text-link"
                            >
                              Open learning resource <ArrowUpRight size={16} />
                            </a>
                          ) : (
                            <p>{r.material.urlOrContent}</p>
                          )}
                        </div>
                      )}
                      <div className="path-actions">
                        <button
                          disabled={busy === r.id}
                          onClick={() => toggle(r)}
                          className={
                            r.status === "DONE" ? "secondary" : "primary"
                          }
                        >
                          <CheckCircle2 size={16} />
                          {r.status === "DONE"
                            ? "Mark as incomplete"
                            : "Mark complete"}
                        </button>
                        {mini && (
                          <Link className="secondary" to={"/quiz/" + mini.id}>
                            <Play size={15} />
                            Check understanding
                          </Link>
                        )}
                      </div>
                    </section>
                  </article>
                );
              })}
            </div>
          </>
        );
      }}
    </Load>
  );
}
