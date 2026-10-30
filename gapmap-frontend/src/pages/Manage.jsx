import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  Plus,
  Trash2,
  Pencil,
  Save,
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Network,
  Users,
  Copy,
  X,
} from "lucide-react";
import { api, errorMessage } from "../api/client";
import { useAuth } from "../App";
import {
  useData,
  Load,
  Heading,
  Empty,
  ErrorBox,
  Badge,
} from "../components/UI";
export function Topics() {
  const state = useData("/topics");
  const initial = { name: "", description: "", prerequisiteIds: [] };
  const [form, setForm] = useState(initial),
    [id, setId] = useState(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api[id ? "put" : "post"]("/topics" + (id ? "/" + id : ""), form);
      setForm(initial);
      setId(null);
      state.reload();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function remove(t) {
    if (
      !confirm(
        "Delete " +
          t.name +
          "? Linked assessments and prerequisites must be removed first.",
      )
    )
      return;
    try {
      await api.delete("/topics/" + t.id);
      state.reload();
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  return (
    <>
      <Heading
        title="Build the foundations."
        description="Connect topics so learning follows a meaningful order."
      />
      <div className="manage-grid">
        <section className="card">
          <h2>{id ? "Edit topic" : "Create a topic"}</h2>
          <form onSubmit={save}>
            <label>
              Topic name
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
                maxLength={100}
              />
            </label>
            <label>
              Description
              <textarea
                value={form.description}
                onChange={(e) =>
                  setForm({ ...form, description: e.target.value })
                }
                maxLength={4000}
              />
            </label>
            <fieldset className="checkbox-list">
              <legend>Prerequisites</legend>
              <p>Select the topics students should understand first.</p>
              {state.data
                ?.filter((t) => t.id !== id)
                .map((t) => (
                  <label key={t.id}>
                    <input
                      type="checkbox"
                      checked={form.prerequisiteIds.includes(t.id)}
                      onChange={(e) =>
                        setForm({
                          ...form,
                          prerequisiteIds: e.target.checked
                            ? [...form.prerequisiteIds, t.id]
                            : form.prerequisiteIds.filter((x) => x !== t.id),
                        })
                      }
                    />
                    {t.name}
                  </label>
                ))}
            </fieldset>
            <ErrorBox error={error} />
            <div className="form-actions">
              <button className="primary" disabled={busy}>
                <Save size={16} />
                {busy ? "Saving…" : "Save topic"}
              </button>
              {id && (
                <button
                  type="button"
                  className="secondary"
                  onClick={() => {
                    setId(null);
                    setForm(initial);
                    setError("");
                  }}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </section>
        <Load state={state}>
          {(topics) => (
            <section className="card">
              <h2>Your topic connections</h2>
              <p className="muted">
                Prerequisites point toward the topics they support. Cycles are
                blocked automatically.
              </p>
              {topics.length ? (
                topics.map((t) => (
                  <article className="topic-item" key={t.id}>
                    <div className="row">
                      <h3>{t.name}</h3>
                      <div className="row compact">
                        <button
                          className="icon-button"
                          aria-label={"Edit " + t.name}
                          onClick={() => {
                            setId(t.id);
                            setForm({
                              name: t.name,
                              description: t.description || "",
                              prerequisiteIds: t.prerequisiteIds,
                            });
                            setError("");
                          }}
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          className="icon-button danger"
                          aria-label={"Delete " + t.name}
                          onClick={() => remove(t)}
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </div>
                    <p>{t.description}</p>
                    <div className="prereq-links">
                      {t.prerequisiteIds.length ? (
                        t.prerequisiteIds.map((id) => (
                          <span key={id}>
                            {topics.find((p) => p.id === id)?.name}{" "}
                            <ArrowRight size={12} /> {t.name}
                          </span>
                        ))
                      ) : (
                        <span>Foundation topic · no prerequisites</span>
                      )}
                    </div>
                  </article>
                ))
              ) : (
                <Empty title="Start with a foundation topic" />
              )}
            </section>
          )}
        </Load>
      </div>
    </>
  );
}
export function Materials() {
  const state = useData("/materials"),
    topics = useData("/topics");
  const initial = { topicId: "", title: "", type: "LESSON", urlOrContent: "" };
  const [form, setForm] = useState(initial),
    [id, setId] = useState(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api[id ? "put" : "post"]("/materials" + (id ? "/" + id : ""), {
        ...form,
        topicId: Number(form.topicId),
      });
      setForm(initial);
      setId(null);
      state.reload();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function remove(m) {
    if (!confirm("Archive this learning material?")) return;
    try {
      await api.delete("/materials/" + m.id);
      state.reload();
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  return (
    <>
      <Heading
        title="Resources that make it click."
        description="Add lessons and practice activities to support each learning gap."
      />
      <div className="manage-grid">
        <section className="card">
          <h2>{id ? "Edit material" : "Add a learning material"}</h2>
          <form onSubmit={save}>
            <label>
              Topic
              <select
                required
                value={form.topicId}
                onChange={(e) => setForm({ ...form, topicId: e.target.value })}
              >
                <option value="">Select topic</option>
                {topics.data?.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Title
              <input
                required
                maxLength={200}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </label>
            <label>
              Type
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
              >
                {["LESSON", "PRACTICE", "VIDEO", "LINK"].map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </select>
            </label>
            <label>
              {["VIDEO", "LINK"].includes(form.type)
                ? "Resource URL (https://…)"
                : "Lesson or practice instructions"}
              <textarea
                rows={8}
                required
                maxLength={12000}
                value={form.urlOrContent}
                onChange={(e) =>
                  setForm({ ...form, urlOrContent: e.target.value })
                }
              />
            </label>
            <ErrorBox error={error || topics.error} />
            <div className="form-actions">
              <button className="primary" disabled={busy}>
                <Save size={16} />
                {busy ? "Saving…" : "Save material"}
              </button>
              {id && (
                <button
                  type="button"
                  className="secondary"
                  onClick={() => {
                    setId(null);
                    setForm(initial);
                  }}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </section>
        <Load state={state}>
          {(data) => (
            <div>
              {data.length ? (
                data.map((m) => (
                  <article className="card material-card" key={m.id}>
                    <div className="row">
                      <Badge value={m.type} />
                      <span className="muted">{m.topic}</span>
                    </div>
                    <h2>{m.title}</h2>
                    <p className="material-preview">{m.urlOrContent}</p>
                    <div className="form-actions">
                      <button
                        className="secondary"
                        onClick={() => {
                          setId(m.id);
                          setForm({
                            topicId: m.topicId,
                            title: m.title,
                            type: m.type,
                            urlOrContent: m.urlOrContent,
                          });
                          setError("");
                          window.scrollTo({ top: 0, behavior: "smooth" });
                        }}
                      >
                        <Pencil size={15} />
                        Edit
                      </button>
                      <button
                        className="icon-button danger"
                        aria-label={"Archive " + m.title}
                        onClick={() => remove(m)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </article>
                ))
              ) : (
                <Empty title="Add your first resource" />
              )}
            </div>
          )}
        </Load>
      </div>
    </>
  );
}
export function Classes() {
  const state = useData("/classes"),
    { user } = useAuth(),
    teacher = user.role === "TEACHER";
  const [value, setValue] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api.post(
        teacher ? "/classes" : "/classes/join",
        teacher ? { name: value } : { code: value },
      );
      setValue("");
      state.reload();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <Heading
        title={
          teacher ? "A space to learn together." : "Your learning communities."
        }
        description={
          teacher
            ? "Create a class and share its code with your students."
            : "Use your teacher’s class code to access assessments."
        }
      />
      <section className="card">
        <form onSubmit={submit} className="inline-form">
          <label>
            {teacher ? "New class name" : "Class join code"}
            <input
              required
              value={value}
              onChange={(e) => setValue(e.target.value)}
              maxLength={100}
              placeholder={
                teacher
                  ? "e.g. Programming Fundamentals · G2"
                  : "e.g. GAPMAP-G1"
              }
            />
          </label>
          <button className="primary" disabled={busy}>
            <Plus size={17} />
            {busy ? "Saving…" : teacher ? "Create class" : "Join class"}
          </button>
        </form>
        <ErrorBox error={error} />
      </section>
      <Load state={state}>
        {(data) =>
          data.length ? (
            <div className="assessment-grid">
              {data.map((c) => (
                <section className="card" key={c.id}>
                  <div className="icon-box">
                    <Users />
                  </div>
                  <h2>{c.name}</h2>
                  {teacher ? (
                    <>
                      <p>Share this code with your students:</p>
                      <div className="join-code">{c.joinCode}</div>
                      <h3>{c.students.length} enrolled students</h3>
                      {c.students.map((s) => (
                        <Link
                          key={s.id}
                          className="student-row"
                          to={"/students/" + s.id}
                        >
                          <span>{s.name}</span>
                          <ArrowRight size={15} />
                        </Link>
                      ))}
                    </>
                  ) : (
                    <Link to="/assessments" className="text-link">
                      Browse assessments <ArrowRight size={16} />
                    </Link>
                  )}
                </section>
              ))}
            </div>
          ) : (
            <Empty title="No classes yet">
              {teacher
                ? "Create a class above to get started."
                : "Ask your teacher for a class code, then enter it above."}
            </Empty>
          )
        }
      </Load>
    </>
  );
}
const blankQuestion = () => ({
  topicId: "",
  difficulty: "EASY",
  text: "",
  explanation: "",
  options: [
    { text: "", correct: true },
    { text: "", correct: false },
    { text: "", correct: false },
    { text: "", correct: false },
  ],
});
export function AssessmentEditor() {
  const { id } = useParams(),
    navigate = useNavigate(),
    topics = useData("/topics"),
    classes = useData("/classes");
  const [form, setForm] = useState({
      title: "",
      classRoomId: "",
      miniQuiz: false,
      topicId: "",
      published: false,
      questions: [blankQuestion()],
    }),
    [loading, setLoading] = useState(!!id),
    [locked, setLocked] = useState(false),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!id) return;
    setLoading(true);
    api
      .get("/assessments/" + id)
      .then(({ data: a }) => {
        setLocked(a.locked);
        setForm({
          title: a.title,
          classRoomId: a.classRoomId,
          miniQuiz: a.miniQuiz,
          topicId: a.topicId || "",
          published: a.published,
          questions: a.questions.map((q) => ({
            topicId: q.topicId,
            difficulty: q.difficulty,
            text: q.text,
            explanation: q.explanation,
            options: q.options.map((o) => ({
              text: o.text,
              correct: o.correct,
            })),
          })),
        });
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [id]);
  function changeQuestion(i, change) {
    setForm((f) => ({
      ...f,
      questions: f.questions.map((q, j) => (j === i ? { ...q, ...change } : q)),
    }));
  }
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const payload = {
        ...form,
        classRoomId: Number(form.classRoomId),
        topicId: form.miniQuiz ? Number(form.topicId) : null,
        questions: form.questions.map((q) => ({
          ...q,
          topicId: Number(q.topicId),
        })),
      };
      await api[id && !locked ? "put" : "post"](
        "/assessments" + (id && !locked ? "/" + id : ""),
        payload,
      );
      navigate("/assessments");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  if (loading) return <div className="state">Loading assessment…</div>;
  return (
    <>
      <Link to="/assessments" className="text-link">
        <ArrowLeft size={15} />
        Back to assessments
      </Link>
      <Heading
        title={
          id
            ? "Shape the next learning moment."
            : "Create a meaningful assessment."
        }
        description="Give each question a topic, clear options, and a helpful explanation."
      />
      {locked && (
        <div className="notice">
          This assessment has saved attempts. Saving creates a new assessment so
          existing results remain accurate.
        </div>
      )}
      <ErrorBox error={error || topics.error || classes.error} />
      <form onSubmit={save} className="assessment-editor">
        <section className="card">
          <h2>The essentials</h2>
          <div className="form-grid">
            <label>
              Assessment title
              <input
                required
                maxLength={200}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </label>
            <label>
              Assign to class
              <select
                required
                value={form.classRoomId}
                onChange={(e) =>
                  setForm({ ...form, classRoomId: e.target.value })
                }
              >
                <option value="">Select class</option>
                {classes.data?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="form-grid">
            <label className="check">
              <input
                type="checkbox"
                checked={form.miniQuiz}
                onChange={(e) =>
                  setForm({ ...form, miniQuiz: e.target.checked })
                }
              />
              This is a topic mini-quiz
            </label>
            <label className="check">
              <input
                type="checkbox"
                checked={form.published}
                onChange={(e) =>
                  setForm({ ...form, published: e.target.checked })
                }
              />
              Publish for enrolled students
            </label>
          </div>
          {form.miniQuiz && (
            <label>
              Mini-quiz topic
              <select
                required
                value={form.topicId}
                onChange={(e) =>
                  setForm({
                    ...form,
                    topicId: e.target.value,
                    questions: form.questions.map((q) => ({
                      ...q,
                      topicId: e.target.value,
                    })),
                  })
                }
              >
                <option value="">Select topic</option>
                {topics.data?.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </label>
          )}
        </section>
        {form.questions.map((q, i) => (
          <section className="card question-editor" key={i}>
            <div className="row">
              <h2>Question {i + 1}</h2>
              <button
                type="button"
                className="icon-button danger"
                aria-label={"Remove question " + (i + 1)}
                disabled={form.questions.length === 1}
                onClick={() =>
                  setForm({
                    ...form,
                    questions: form.questions.filter((_, j) => j !== i),
                  })
                }
              >
                <Trash2 size={17} />
              </button>
            </div>
            <label>
              Question text
              <textarea
                required
                maxLength={4000}
                value={q.text}
                onChange={(e) => changeQuestion(i, { text: e.target.value })}
              />
            </label>
            <div className="form-grid">
              <label>
                Topic
                <select
                  required
                  value={q.topicId}
                  onChange={(e) =>
                    changeQuestion(i, { topicId: e.target.value })
                  }
                >
                  <option value="">Select topic</option>
                  {topics.data
                    ?.filter(
                      (t) => !form.miniQuiz || t.id === Number(form.topicId),
                    )
                    .map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                      </option>
                    ))}
                </select>
              </label>
              <label>
                Difficulty
                <select
                  value={q.difficulty}
                  onChange={(e) =>
                    changeQuestion(i, { difficulty: e.target.value })
                  }
                >
                  {["EASY", "MEDIUM", "HARD"].map((x) => (
                    <option key={x}>{x}</option>
                  ))}
                </select>
              </label>
            </div>
            <fieldset className="option-editor">
              <legend>
                Answer options <small>Choose exactly one correct answer.</small>
              </legend>
              {q.options.map((o, k) => (
                <div key={k}>
                  <input
                    type="radio"
                    name={"correct-" + i}
                    aria-label={"Option " + (k + 1) + " is correct"}
                    checked={o.correct}
                    onChange={() =>
                      changeQuestion(i, {
                        options: q.options.map((x, n) => ({
                          ...x,
                          correct: n === k,
                        })),
                      })
                    }
                  />
                  <input
                    required
                    maxLength={2000}
                    aria-label={"Question " + (i + 1) + " option " + (k + 1)}
                    value={o.text}
                    placeholder={"Option " + String.fromCharCode(65 + k)}
                    onChange={(e) =>
                      changeQuestion(i, {
                        options: q.options.map((x, n) =>
                          n === k ? { ...x, text: e.target.value } : x,
                        ),
                      })
                    }
                  />
                  <button
                    type="button"
                    className="icon-button"
                    aria-label={"Remove option " + (k + 1)}
                    disabled={q.options.length <= 2}
                    onClick={() => {
                      const list = q.options.filter((_, n) => n !== k);
                      if (!list.some((x) => x.correct))
                        list[0] = { ...list[0], correct: true };
                      changeQuestion(i, { options: list });
                    }}
                  >
                    <X size={15} />
                  </button>
                </div>
              ))}
              {q.options.length < 6 && (
                <button
                  className="text-link"
                  type="button"
                  onClick={() =>
                    changeQuestion(i, {
                      options: [...q.options, { text: "", correct: false }],
                    })
                  }
                >
                  <Plus size={14} />
                  Add option
                </button>
              )}
            </fieldset>
            <label>
              Answer explanation
              <textarea
                required
                maxLength={4000}
                value={q.explanation}
                onChange={(e) =>
                  changeQuestion(i, { explanation: e.target.value })
                }
                placeholder="Help the student understand why this answer is correct."
              />
            </label>
          </section>
        ))}
        <div className="editor-actions">
          <button
            type="button"
            className="secondary"
            disabled={form.questions.length >= 100}
            onClick={() =>
              setForm({
                ...form,
                questions: [
                  ...form.questions,
                  {
                    ...blankQuestion(),
                    topicId: form.miniQuiz ? form.topicId : "",
                  },
                ],
              })
            }
          >
            <Plus size={17} />
            Add question
          </button>
          <button
            className="primary"
            disabled={busy || !topics.data?.length || !classes.data?.length}
          >
            <Save size={17} />
            {busy
              ? "Saving…"
              : locked
                ? "Save as new assessment"
                : "Save assessment"}
          </button>
        </div>
      </form>
    </>
  );
}
