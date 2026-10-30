package com.gapmap.dto;

import com.gapmap.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public class Dtos {
  public record Login(@Email @NotBlank String email, @NotBlank String password) {}

  public record Register(
      @NotBlank @Size(max = 100) String name,
      @Email @NotBlank @Size(max = 200) String email,
      @Size(min = 8, max = 72) @NotBlank String password,
      @NotNull Role role,
      String teacherCode) {}

  public record UserView(Long id, String name, String email, Role role) {}

  public record ClassInput(@NotBlank @Size(max = 100) String name) {}

  public record JoinInput(@NotBlank String code) {}

  public record ClassView(Long id, String name, String joinCode, List<UserView> students) {}

  public record TopicInput(
      @NotBlank @Size(max = 100) String name,
      @Size(max = 4000) String description,
      @NotNull Set<Long> prerequisiteIds) {}

  public record TopicView(Long id, String name, String description, List<Long> prerequisiteIds) {}

  public record OptionInput(@NotBlank @Size(max = 2000) String text, boolean correct) {}

  public record QuestionInput(
      @NotNull Long topicId,
      @NotNull Difficulty difficulty,
      @NotBlank @Size(max = 4000) String text,
      @NotBlank @Size(max = 4000) String explanation,
      @Valid @Size(min = 2, max = 6) @NotNull List<OptionInput> options) {}

  public record AssessmentInput(
      @NotBlank @Size(max = 200) String title,
      @NotNull Long classRoomId,
      boolean miniQuiz,
      Long topicId,
      boolean published,
      @Valid @Size(min = 1, max = 100) @NotNull List<QuestionInput> questions) {}

  public record OptionView(Long id, String text, Boolean correct) {}

  public record QuestionView(
      Long id,
      Long topicId,
      String topic,
      Difficulty difficulty,
      String text,
      String explanation,
      List<OptionView> options) {}

  public record AssessmentView(
      Long id,
      String title,
      Long classRoomId,
      String className,
      boolean miniQuiz,
      Long topicId,
      boolean published,
      boolean locked,
      int questionCount,
      List<QuestionView> questions) {}

  public record Answer(
      @NotNull Long questionId, @NotNull Long optionId, @NotNull ConfidenceLevel confidence) {}

  public record Submission(
      @NotBlank @Size(max = 80) String submissionKey,
      @NotNull Long assessmentId,
      @NotNull @Size(min = 1, max = 100) @Valid List<Answer> answers) {}

  public record TopicScore(
      Long topicId, String topic, double scorePercent, MasteryLevel masteryLevel) {}

  public record AnswerView(
      Long questionId,
      String question,
      String selected,
      String correctAnswer,
      boolean correct,
      ConfidenceLevel confidence,
      boolean misconception,
      String explanation) {}

  public record AttemptView(
      Long id,
      Long assessmentId,
      String assessment,
      boolean miniQuiz,
      Instant submittedAt,
      double totalScore,
      List<TopicScore> topics,
      List<AnswerView> answers) {}

  public record GapView(
      Long id, Long topicId, String topic, GapType gapType, Status status, Long attemptId) {}

  public record MaterialInput(
      @NotNull Long topicId,
      @NotBlank @Size(max = 200) String title,
      @NotNull MaterialType type,
      @NotBlank @Size(max = 12000) String urlOrContent) {}

  public record MaterialView(
      Long id, Long topicId, String topic, String title, MaterialType type, String urlOrContent) {}

  public record RecommendationView(
      Long id,
      Long topicId,
      String topic,
      GapType gapType,
      MaterialView material,
      String explanation,
      int orderIndex,
      Status status) {}

  public record StudentReport(
      UserView student,
      List<AttemptView> attempts,
      List<TopicScore> mastery,
      List<GapView> gaps,
      List<RecommendationView> recommendations) {}

  public record ClassTopic(
      Long topicId,
      String topic,
      double average,
      int assessed,
      int needsSupport,
      int misconceptions) {}

  public record TeacherDashboard(
      List<StudentReport> students, List<ClassTopic> topics, int assessmentCount) {}
}
