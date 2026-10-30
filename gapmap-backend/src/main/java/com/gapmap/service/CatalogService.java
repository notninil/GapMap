package com.gapmap.service;

import static com.gapmap.service.AccessService.*;

import com.gapmap.dto.Dtos.*;
import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {
  final AccessService access;
  final TopicRepository topics;
  final ClassRoomRepository classes;
  final AssessmentRepository assessments;
  final AttemptRepository attempts;
  final LearningMaterialRepository materials;

  public CatalogService(
      AccessService a,
      TopicRepository t,
      ClassRoomRepository c,
      AssessmentRepository q,
      AttemptRepository at,
      LearningMaterialRepository m) {
    access = a;
    topics = t;
    classes = c;
    assessments = q;
    attempts = at;
    materials = m;
  }

  public List<ClassView> classes() {
    var u = access.current();
    return (u.role == Role.TEACHER ? classes.findByTeacherId(u.id) : classes.findByStudentsId(u.id))
        .stream().map(this::classView).toList();
  }

  public ClassView classView(ClassRoom c) {
    return new ClassView(
        c.id,
        c.name,
        access.current().role == Role.TEACHER ? c.joinCode : null,
        access.current().role == Role.TEACHER
            ? c.students.stream().map(AuthService::view).toList()
            : List.of());
  }

  public ClassView createClass(ClassInput d) {
    ClassRoom c = new ClassRoom();
    c.name = d.name();
    c.teacher = access.current();
    c.joinCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return classView(classes.save(c));
  }

  public ClassView join(JoinInput d) {
    ClassRoom c = found(classes.findByJoinCode(d.code().trim().toUpperCase()));
    c.students.add(access.current());
    return classView(c);
  }

  public List<Topic> visibleTopics() {
    var u = access.current();
    if (u.role == Role.TEACHER) return topics.findByTeacherIdOrderById(u.id);
    Set<Long> teachers = new HashSet<>();
    classes.findByStudentsId(u.id).forEach(c -> teachers.add(c.teacher.id));
    return topics.findAll().stream().filter(t -> teachers.contains(t.teacher.id)).toList();
  }

  public Topic ownedTopic(Long id) {
    Topic t = found(topics.findById(id));
    access.teacher(t.teacher);
    return t;
  }

  public List<TopicView> topics() {
    return visibleTopics().stream().map(CatalogService::topicView).toList();
  }

  public static TopicView topicView(Topic t) {
    return new TopicView(
        t.id, t.name, t.description, t.prerequisites.stream().map(p -> p.id).toList());
  }

  public TopicView saveTopic(Long id, TopicInput d) {
    access.lockCurrent();
    Topic t = id == null ? new Topic() : ownedTopic(id);
    t.name = d.name();
    t.description = d.description();
    t.teacher = access.current();
    var pre = new LinkedHashSet<Topic>();
    for (Long p : d.prerequisiteIds()) {
      Topic item = ownedTopic(p);
      require(
          !Objects.equals(id, p) && !reaches(item, id, new HashSet<>()),
          "Prerequisites cannot form a cycle");
      pre.add(item);
    }
    t.prerequisites = pre;
    return topicView(topics.save(t));
  }

  public static boolean reaches(Topic t, Long target, Set<Long> visited) {
    if (target == null) return false;
    if (t.id.equals(target)) return true;
    if (!visited.add(t.id)) return false;
    return t.prerequisites.stream().anyMatch(p -> reaches(p, target, visited));
  }

  public void deleteTopic(Long id) {
    access.lockCurrent();
    Topic t = ownedTopic(id);
    require(
        topics.findAll().stream().noneMatch(x -> x.prerequisites.contains(t)),
        "Remove this topic from prerequisite links first");
    topics.delete(t);
    topics.flush();
  }

  public List<AssessmentView> assessments() {
    var u = access.current();
    List<Assessment> list =
        u.role == Role.TEACHER
            ? assessments.findByCreatedByIdAndArchivedFalseOrderByIdDesc(u.id)
            : assessments.findAll().stream()
                .filter(
                    a ->
                        !a.archived
                            && a.published
                            && a.classRoom.students.stream().anyMatch(s -> s.id.equals(u.id)))
                .toList();
    return list.stream().map(a -> assessmentView(a, false)).toList();
  }

  public AssessmentView assessment(Long id) {
    Assessment a = found(assessments.findById(id));
    access.classroom(a.classRoom);
    require(
        !a.archived && (access.current().role == Role.TEACHER || a.published),
        "Assessment unavailable");
    return assessmentView(a, true);
  }

  public AssessmentView assessmentView(Assessment a, boolean details) {
    boolean teacher = access.current().role == Role.TEACHER;
    return new AssessmentView(
        a.id,
        a.title,
        a.classRoom.id,
        a.classRoom.name,
        a.miniQuiz,
        a.topic == null ? null : a.topic.id,
        a.published,
        attempts.existsByAssessmentId(a.id),
        a.questions.size(),
        details
            ? a.questions.stream()
                .map(
                    q ->
                        new QuestionView(
                            q.id,
                            q.topic.id,
                            q.topic.name,
                            q.difficulty,
                            q.text,
                            teacher ? q.explanation : null,
                            q.options.stream()
                                .map(o -> new OptionView(o.id, o.text, teacher ? o.correct : null))
                                .toList()))
                .toList()
            : List.of());
  }

  public AssessmentView saveAssessment(Long id, AssessmentInput d) {
    ClassRoom c = found(classes.findById(d.classRoomId()));
    access.teacher(c.teacher);
    Assessment a = id == null ? new Assessment() : found(assessments.lockById(id));
    if (id != null) {
      access.teacher(a.createdBy);
      require(
          !attempts.existsByAssessmentId(id),
          "This assessment has submissions. Create a new version to preserve results.");
    }
    a.title = d.title();
    a.classRoom = c;
    a.createdBy = access.current();
    a.miniQuiz = d.miniQuiz();
    a.published = d.published();
    a.topic = d.topicId() == null ? null : ownedTopic(d.topicId());
    require(!a.miniQuiz || a.topic != null, "Select a topic for a mini-quiz");
    a.questions.clear();
    for (QuestionInput x : d.questions()) {
      require(
          x.options().stream().filter(OptionInput::correct).count() == 1,
          "Each question needs exactly one correct answer");
      Question q = new Question();
      q.assessment = a;
      q.topic = ownedTopic(x.topicId());
      require(
          !a.miniQuiz || q.topic.id.equals(a.topic.id), "Mini-quiz questions must use its topic");
      q.difficulty = x.difficulty();
      q.text = x.text();
      q.explanation = x.explanation();
      for (OptionInput oi : x.options()) {
        QuestionOption o = new QuestionOption();
        o.question = q;
        o.text = oi.text();
        o.correct = oi.correct();
        q.options.add(o);
      }
      a.questions.add(q);
    }
    return assessmentView(assessments.saveAndFlush(a), true);
  }

  public void deleteAssessment(Long id) {
    Assessment a = found(assessments.lockById(id));
    access.teacher(a.createdBy);
    if (attempts.existsByAssessmentId(id)) {
      a.archived = true;
      a.published = false;
    } else assessments.delete(a);
  }

  public static MaterialView materialView(LearningMaterial m) {
    return new MaterialView(m.id, m.topic.id, m.topic.name, m.title, m.type, m.urlOrContent);
  }

  public List<MaterialView> materials() {
    Set<Long> ids = new HashSet<>();
    visibleTopics().forEach(t -> ids.add(t.id));
    return materials.findAll().stream()
        .filter(m -> !m.archived && ids.contains(m.topic.id))
        .map(CatalogService::materialView)
        .toList();
  }

  public MaterialView saveMaterial(Long id, MaterialInput d) {
    LearningMaterial m = id == null ? new LearningMaterial() : found(materials.findById(id));
    if (id != null) access.teacher(m.topic.teacher);
    m.topic = ownedTopic(d.topicId());
    m.title = d.title();
    m.type = d.type();
    m.urlOrContent = d.urlOrContent();
    if (m.type == MaterialType.LINK || m.type == MaterialType.VIDEO)
      require(
          m.urlOrContent.matches("https?://[^\\s]+"), "Links must start with http:// or https://");
    return materialView(materials.save(m));
  }

  public void deleteMaterial(Long id) {
    var m = found(materials.findById(id));
    access.teacher(m.topic.teacher);
    m.archived = true;
  }
}
