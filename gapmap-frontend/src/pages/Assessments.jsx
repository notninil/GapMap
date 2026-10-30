import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowRight,
  ArrowLeft,
  Plus,
  Clock,
  ClipboardList,
  Search,
  Trash2,
  Pencil,
  Send,
  Check,
  Lock,
} from "lucide-react";
import { api, errorMessage } from "../api/client";
import { useAuth } from "../App";
import {
  useData,
  Load,
  Heading,
  Badge,
  Empty,
  ErrorBox,
} from "../components/UI";
export function Assessments() {
  const state = useData("/assessments"),
    { user } = useAuth();
  const teacher = user.role === "TEACHER";
  const [search, setSearch] = useState(""),
    [filter, setFilter] = useState("ALL"),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(null);
  async function remove(a) {
    if (
      !confirm(
        a.locked
          ? "Archive this assessment? Past results will be preserved."
          : "Delete this assessment and its questions?",
      )
    )
      return;
    setBusy(a.id);
    try {
      await api.delete("/assessments/" + a.id);
      state.reload();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  }
  return (
    <>
      <Heading
        title={
          teacher
            ? "Give learning a starting point."
            : "Find your next challenge."
        }
        description={
          teacher
            ? "Create assessments that reveal more than a score."
            : "Discover what you know, and what you can build on."
        }
      >
        {teacher && (
          <Link to="/assessments/new" className="primary">
            <Plus size={17} />
            Create assessment
          </Link>
        )}
      </Heading>
      <div className="toolbar">
        <div className="tabs">
          {[
            ["ALL", "All assessments"],
            ["MAIN", "Assessments"],
            ["MINI", "Mini-quizzes"],
          ].map(([v, l]) => (
            <button
              key={v}
              className={filter === v ? "active" : ""}
              onClick={() => setFilter(v)}
            >
              {l}
            </button>
          ))}
        </div>
        <label className="search">
          <Search size={17} />
          <input
            aria-label="Search assessments"
            placeholder="Search assessments…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
      </div>
      <ErrorBox error={error} />
      <Load state={state}>
        {(data) => {
          const list = data.filter(
            (a) =>
              a.title.toLowerCase().includes(search.toLowerCase()) &&
              (filter === "ALL" || a.miniQuiz === (filter === "MINI")),
          );
          return list.length ? (
            <div className="assessment-grid">
              {list.map((a) => (
                <article className="card assessment-card" key={a.id}>
                  <div className="row">
                    <div className="icon-box">
                      <ClipboardList />
                    </div>
                    <Badge
                      value={
                        a.miniQuiz
                          ? "MINI_QUIZ"
                          : teacher && !a.published
                            ? "DRAFT"
                            : "ASSESSMENT"
                      }
                    />
                  </div>
                  <h2>{a.title}</h2>
                  <p>{a.className}</p>
                  <div className="assessment-meta">
                    <span>
                      <ClipboardList size={14} />
                      {a.questionCount} questions
                    </span>
                    <span>
                      <Clock size={14} />
                      {Math.max(3, a.questionCount)} min approx.
                    </span>
                  </div>
                  {teacher ? (
                    <div className="row">
                      <Link
                        className="secondary"
                        to={"/assessments/" + a.id + "/edit"}
                      >
                        {a.locked ? <Lock size={15} /> : <Pencil size={15} />}{" "}
                        {a.locked ? "View / duplicate" : "Edit assessment"}
                      </Link>
                      <button
                        className="icon-button danger"
                        aria-label={"Delete " + a.title}
                        disabled={busy === a.id}
                        onClick={() => remove(a)}
                      >
                        <Trash2 size={17} />
                      </button>
                    </div>
                  ) : (
                    <Link className="secondary full" to={"/quiz/" + a.id}>
                      {a.miniQuiz ? "Check understanding" : "Start assessment"}{" "}
                      <ArrowRight size={17} />
                    </Link>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <Empty title="No assessments found">
              {teacher
                ? "Create an assessment to get started."
                : "Join a class using your teacher’s code, or try another search."}
            </Empty>
          );
        }}
      </Load>
    </>
  );
}
export function Quiz() {
  const { id } = useParams(),
    state = useData("/assessments/" + id);
  return (
    <Load state={state}>{(a) => <QuizSession key={id} assessment={a} />}</Load>
  );
}
function QuizSession({ assessment: a }) {
  const { user } = useAuth(),
    navigate = useNavigate();
  const key = `gapmap-draft-${user.id}-${a.id}`;
  const [draft, setDraft] = useState(() => {
    try {
      const old = JSON.parse(sessionStorage.getItem(key));
      if (old && old.questionIds === a.questions.map((q) => q.id).join(","))
        return old;
    } catch {}
    return {
      questionIds: a.questions.map((q) => q.id).join(","),
      submissionKey: crypto.randomUUID(),
      answers: {},
      index: 0,
    };
  });
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [storageError, setStorageError] = useState(""),
    [review, setReview] = useState(false);
  useEffect(() => {
    try {
      sessionStorage.setItem(key, JSON.stringify(draft));
    } catch {
      setStorageError(
        "This browser cannot save your draft. Keep this page open until you submit.",
      );
    }
  }, [draft, key]);
  const q = a.questions[draft.index],
    answer = draft.answers[q.id] || {},
    count = a.questions.filter(
      (q) => draft.answers[q.id]?.optionId && draft.answers[q.id]?.confidence,
    ).length;
  function choose(field, value) {
    setDraft((d) => ({
      ...d,
      answers: { ...d.answers, [q.id]: { ...d.answers[q.id], [field]: value } },
    }));
  }
  async function submit() {
    setBusy(true);
    setError("");
    try {
      const response = await api.post("/attempts", {
        assessmentId: a.id,
        submissionKey: draft.submissionKey,
        answers: a.questions.map((q) => ({
          questionId: q.id,
          ...draft.answers[q.id],
        })),
      });
      sessionStorage.removeItem(key);
      navigate("/results/" + response.data.id, { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <Heading
        eyebrow={a.miniQuiz ? "A QUICK CHECK-IN" : "ONE QUESTION AT A TIME"}
        title={a.title}
        description="Take your time. Your confidence helps us understand your thinking."
      />
      <div className="quiz-layout">
        <section className="card quiz-card">
          <div className="row">
            <Badge value={q.difficulty} />
            <span className="muted">
              Question {draft.index + 1} of {a.questions.length}
            </span>
          </div>
          <div className="quiz-progress">
            <div style={{ width: (100 * count) / a.questions.length + "%" }} />
          </div>
          {review ? (
            <>
              <h2>Ready to submit?</h2>
              <p>
                You have answered and rated confidence for {count} of{" "}
                {a.questions.length} questions.
              </p>
              <p>
                Your results and personalized study path will be saved together.
              </p>
              <div className="quiz-navigation">
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() => setReview(false)}
                >
                  Review my answers
                </button>
                <button
                  className="primary"
                  disabled={busy || count !== a.questions.length}
                  onClick={submit}
                >
                  <Send size={17} />
                  {busy ? "Saving your results…" : "Submit assessment"}
                </button>
              </div>
            </>
          ) : (
            <>
              <p className="eyebrow">{q.topic}</p>
              <h2 className="question-text">{q.text}</h2>
              <fieldset className="options">
                <legend className="sr-only">Choose your answer</legend>
                {q.options.map((o, i) => (
                  <label
                    key={o.id}
                    className={
                      "option " + (answer.optionId === o.id ? "selected" : "")
                    }
                  >
                    <input
                      type="radio"
                      name={"question-" + q.id}
                      checked={answer.optionId === o.id}
                      onChange={() => choose("optionId", o.id)}
                    />
                    <span className="option-letter">
                      {String.fromCharCode(65 + i)}
                    </span>
                    <span>{o.text}</span>
                    {answer.optionId === o.id && <Check size={18} />}
                  </label>
                ))}
              </fieldset>
              <fieldset className="confidence">
                <legend>How sure are you?</legend>
                <p>
                  There’s no penalty for being unsure. This helps personalize
                  your path.
                </p>
                <div>
                  {[
                    ["NOT_SURE", "Not sure"],
                    ["FAIRLY_SURE", "Fairly sure"],
                    ["VERY_SURE", "Very sure"],
                  ].map(([v, t]) => (
                    <label
                      key={v}
                      className={answer.confidence === v ? "selected" : ""}
                    >
                      <input
                        type="radio"
                        name={"confidence-" + q.id}
                        checked={answer.confidence === v}
                        onChange={() => choose("confidence", v)}
                      />
                      {t}
                    </label>
                  ))}
                </div>
              </fieldset>
              <div className="quiz-navigation">
                <button
                  className="secondary"
                  disabled={draft.index === 0}
                  onClick={() => setDraft({ ...draft, index: draft.index - 1 })}
                >
                  <ArrowLeft size={16} />
                  Previous
                </button>
                {draft.index < a.questions.length - 1 ? (
                  <button
                    className="primary"
                    disabled={!answer.optionId || !answer.confidence}
                    onClick={() =>
                      setDraft({ ...draft, index: draft.index + 1 })
                    }
                  >
                    Next question <ArrowRight size={16} />
                  </button>
                ) : (
                  <button
                    className="primary"
                    disabled={count !== a.questions.length}
                    onClick={() => setReview(true)}
                  >
                    Review & submit <ArrowRight size={16} />
                  </button>
                )}
              </div>
            </>
          )}
          <ErrorBox error={error} />
          <ErrorBox error={storageError} />
        </section>
        <aside className="card quiz-sidebar">
          <h3>Your progress</h3>
          <p>
            {count} of {a.questions.length} complete
          </p>
          <div className="question-dots">
            {a.questions.map((x, i) => (
              <button
                key={x.id}
                disabled={busy}
                className={
                  (draft.answers[x.id]?.confidence &&
                  draft.answers[x.id]?.optionId
                    ? "complete "
                    : "") + (draft.index === i ? "current" : "")
                }
                onClick={() => {
                  setDraft({ ...draft, index: i });
                  setReview(false);
                }}
                aria-label={"Go to question " + (i + 1)}
              >
                {i + 1}
              </button>
            ))}
          </div>
          <div className="insight">
            <p>
              Answers are saved in this tab as you go. A retry after a
              connection error will not create a duplicate submission.
            </p>
          </div>
        </aside>
      </div>
    </>
  );
}
