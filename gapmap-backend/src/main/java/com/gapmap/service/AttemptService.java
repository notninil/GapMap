package com.gapmap.service;

import static com.gapmap.service.AccessService.*;

import com.gapmap.dto.Dtos.*;
import com.gapmap.entity.*;
import com.gapmap.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptService {
  final AttemptRepository attempts;
  final AssessmentRepository assessments;
  final UserRepository users;
  final AccessService access;
  final AnalysisService analysis;
  final StudyPathService paths;

  public AttemptService(
      AttemptRepository a,
      AssessmentRepository q,
      UserRepository u,
      AccessService ac,
      AnalysisService an,
      StudyPathService p) {
    attempts = a;
    assessments = q;
    users = u;
    access = ac;
    analysis = an;
    paths = p;
  }

  @Transactional
  public AttemptView submit(Submission d) {
    User student = found(users.lockById(access.current().id));
    require(student.role == Role.STUDENT, "Only students can submit");
    String hash = fingerprint(d);
    var previous = attempts.findByStudentIdAndSubmissionKey(student.id, d.submissionKey());
    if (previous.isPresent()) {
      require(
          previous.get().payloadHash.equals(hash),
          "This submission key was already used for different answers");
      return view(previous.get());
    }
    Assessment a = found(assessments.lockById(d.assessmentId()));
    access.classroom(a.classRoom);
    require(a.published && !a.archived, "Assessment unavailable");
    require(d.answers().size() == a.questions.size(), "Answer every question before submitting");
    Map<Long, Answer> answers = new HashMap<>();
    for (var answer : d.answers())
      require(answers.put(answer.questionId(), answer) == null, "Duplicate question response");
    Attempt attempt = new Attempt();
    attempt.student = student;
    attempt.assessment = a;
    attempt.submittedAt = Instant.now();
    attempt.submissionKey = d.submissionKey();
    attempt.payloadHash = hash;
    for (var q : a.questions) {
      Answer answer = answers.get(q.id);
      require(answer != null, "Missing question response");
      QuestionOption option =
          q.options.stream()
              .filter(o -> o.id.equals(answer.optionId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new org.springframework.web.server.ResponseStatusException(
                          org.springframework.http.HttpStatus.BAD_REQUEST,
                          "Option does not belong to question"));
      Response r = new Response();
      r.attempt = attempt;
      r.question = q;
      r.selectedOption = option;
      r.correct = option.correct;
      r.confidenceLevel = answer.confidence();
      attempt.responses.add(r);
    }
    attempts.saveAndFlush(attempt);
    analysis.computeTopicScores(attempt.id);
    attempts.flush();
    paths.generateStudyPath(student.id);
    return view(attempt);
  }

  public static String fingerprint(Submission d) {
    String payload =
        d.assessmentId()
            + ":"
            + d.answers().stream()
                .sorted(Comparator.comparing(Answer::questionId))
                .map(a -> a.questionId() + "/" + a.optionId() + "/" + a.confidence())
                .toList();
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional(readOnly = true)
  public AttemptView result(Long id) {
    Attempt a = found(attempts.findById(id));
    access.student(a.student, a.assessment.classRoom);
    return view(a);
  }

  public static TopicScore topicScore(TopicResult t) {
    return new TopicScore(t.topic.id, t.topic.name, t.scorePercent, t.masteryLevel);
  }

  public static AttemptView view(Attempt a) {
    return new AttemptView(
        a.id,
        a.assessment.id,
        a.assessment.title,
        a.assessment.miniQuiz,
        a.submittedAt,
        a.totalScore,
        a.topicResults.stream().map(AttemptService::topicScore).toList(),
        a.responses.stream()
            .map(
                r ->
                    new AnswerView(
                        r.question.id,
                        r.question.text,
                        r.selectedOption.text,
                        r.question.options.stream()
                            .filter(o -> o.correct)
                            .map(o -> o.text)
                            .findFirst()
                            .orElse(""),
                        r.correct,
                        r.confidenceLevel,
                        MisconceptionService.isMisconception(r),
                        r.question.explanation))
            .toList());
  }
}
