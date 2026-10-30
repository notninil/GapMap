package com.gapmap;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.gapmap.entity.*;
import com.gapmap.repository.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:workflow;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "app.jwt-secret=integration-test-secret-32-characters-minimum",
      "app.teacher-code=TEST-TEACHER",
      "app.seed=true",
      "app.seed-password=GapMap123!"
    })
@AutoConfigureMockMvc
class WorkflowIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired AttemptRepository attempts;
  @Autowired UserRepository users;
  final String origin = "http://localhost:5173";

  Cookie login(String email) throws Exception {
    var r =
        mvc.perform(
                post("/api/auth/login")
                    .header("Origin", origin)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(Map.of("email", email, "password", "GapMap123!"))))
            .andExpect(status().isOk())
            .andReturn();
    String cookie = r.getResponse().getHeader("Set-Cookie");
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Strict"));
    return new Cookie("gapmap_session", cookie.split(";", 2)[0].split("=", 2)[1]);
  }

  JsonNode getJson(String url, Cookie c) throws Exception {
    return json.readTree(
        mvc.perform(get(url).cookie(c))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void authRolesAndOriginAreEnforced() throws Exception {
    mvc.perform(get("/api/topics")).andExpect(status().isUnauthorized());
    Cookie s = login("student1@gapmap.test");
    mvc.perform(get("/api/dashboards/teacher").cookie(s)).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/topics")
                .cookie(s)
                .header("Origin", origin)
                .contentType("application/json")
                .content("{\"name\":\"Bad\",\"prerequisiteIds\":[]}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/auth/logout").cookie(s).header("Origin", "https://evil.example"))
        .andExpect(status().isForbidden());
  }

  @Test
  void seedAndDashboardAreReady() throws Exception {
    JsonNode t = getJson("/api/dashboards/teacher", login("teacher@gapmap.test"));
    assertTrue(t.get("students").size() >= 10);
    assertEquals(6, t.get("topics").size());
    assertEquals(7, t.get("assessmentCount").asInt());
    JsonNode s = getJson("/api/dashboards/student", login("student2@gapmap.test"));
    assertTrue(s.get("attempts").size() >= 3);
    assertEquals(6, s.get("mastery").size());
  }

  @Test
  void completeSubmissionIsAtomicIdempotentAndPrivate() throws Exception {
    Cookie s = login("student3@gapmap.test"), teacher = login("teacher@gapmap.test");
    JsonNode quizzes = getJson("/api/assessments", s);
    long aid = quizzes.get(0).get("id").asLong();
    JsonNode quiz = getJson("/api/assessments/" + aid, s),
        key = getJson("/api/assessments/" + aid, teacher);
    List<Map<String, Object>> answers = new ArrayList<>();
    for (JsonNode q : key.get("questions")) {
      long oid = 0;
      for (JsonNode o : q.get("options"))
        if (o.get("correct").asBoolean()) oid = o.get("id").asLong();
      answers.add(
          Map.of("questionId", q.get("id").asLong(), "optionId", oid, "confidence", "VERY_SURE"));
    }
    assertTrue(quiz.get("questions").get(0).get("options").get(0).get("correct").isNull());
    assertTrue(quiz.get("questions").get(0).get("explanation").isNull());
    String payload =
        json.writeValueAsString(
            Map.of(
                "submissionKey",
                UUID.randomUUID().toString(),
                "assessmentId",
                aid,
                "answers",
                answers));
    long before = attempts.count();
    String response =
        mvc.perform(
                post("/api/attempts")
                    .cookie(s)
                    .header("Origin", origin)
                    .contentType("application/json")
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode result = json.readTree(response);
    assertEquals(100, result.get("totalScore").asDouble());
    assertEquals(before + 1, attempts.count());
    String retry =
        mvc.perform(
                post("/api/attempts")
                    .cookie(s)
                    .header("Origin", origin)
                    .contentType("application/json")
                    .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(result.get("id"), json.readTree(retry).get("id"));
    assertEquals(before + 1, attempts.count());
    mvc.perform(
            get("/api/results/" + result.get("id").asLong()).cookie(login("student4@gapmap.test")))
        .andExpect(status().isForbidden());
  }

  @Test
  void malformedSubmissionDoesNotPersistAttempt() throws Exception {
    Cookie s = login("student5@gapmap.test");
    long before = attempts.count();
    mvc.perform(
            post("/api/attempts")
                .cookie(s)
                .header("Origin", origin)
                .contentType("application/json")
                .content(
                    "{\"assessmentId\":1,\"submissionKey\":\"invalid-one\",\"answers\":[{\"questionId\":1,\"optionId\":999999,\"confidence\":\"VERY_SURE\"}]}"))
        .andExpect(status().isBadRequest());
    assertEquals(before, attempts.count());
  }

  @Test
  void prerequisiteCycleRejectedAndOwnershipChecked() throws Exception {
    Cookie t = login("teacher@gapmap.test");
    JsonNode list = getJson("/api/topics", t);
    long first = list.get(0).get("id").asLong(), last = list.get(5).get("id").asLong();
    mvc.perform(
            put("/api/topics/" + first)
                .cookie(t)
                .header("Origin", origin)
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "name",
                            "Variables",
                            "description",
                            "Foundations",
                            "prerequisiteIds",
                            List.of(last)))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registrationDoesNotAllowTeacherWithoutInvitation() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .header("Origin", origin)
                .contentType("application/json")
                .content(
                    "{\"name\":\"Test\",\"email\":\"invalid-teacher@test.com\",\"password\":\"LongPassword123\",\"role\":\"TEACHER\",\"teacherCode\":\"wrong\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void teacherCrudAndNewStudentEnrollmentWork() throws Exception {
    Cookie teacher = login("teacher@gapmap.test");
    String code = UUID.randomUUID().toString();
    JsonNode topic =
        json.readTree(
            mvc.perform(
                    post("/api/topics")
                        .cookie(teacher)
                        .header("Origin", origin)
                        .contentType("application/json")
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "name",
                                    "Test topic " + code,
                                    "description",
                                    "Test",
                                    "prerequisiteIds",
                                    List.of()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    long tid = topic.get("id").asLong();
    JsonNode cl =
        json.readTree(
            mvc.perform(
                    post("/api/classes")
                        .cookie(teacher)
                        .header("Origin", origin)
                        .contentType("application/json")
                        .content("{\"name\":\"Integration class\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var registration =
        mvc.perform(
                post("/api/auth/register")
                    .header("Origin", origin)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "name",
                                "New Student",
                                "email",
                                code + "@test.com",
                                "password",
                                "GapMap123!",
                                "role",
                                "STUDENT"))))
            .andExpect(status().isOk())
            .andReturn();
    String c = registration.getResponse().getHeader("Set-Cookie");
    Cookie student = new Cookie("gapmap_session", c.split(";", 2)[0].split("=", 2)[1]);
    mvc.perform(
            post("/api/classes/join")
                .cookie(student)
                .header("Origin", origin)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("code", cl.get("joinCode").asText()))))
        .andExpect(status().isOk());
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", "Mini assessment");
    payload.put("classRoomId", cl.get("id").asLong());
    payload.put("miniQuiz", true);
    payload.put("topicId", tid);
    payload.put("published", true);
    payload.put(
        "questions",
        List.of(
            Map.of(
                "topicId",
                tid,
                "difficulty",
                "EASY",
                "text",
                "Two plus two?",
                "explanation",
                "2 + 2 = 4",
                "options",
                List.of(
                    Map.of("text", "4", "correct", true), Map.of("text", "5", "correct", false)))));
    JsonNode quiz =
        json.readTree(
            mvc.perform(
                    post("/api/assessments")
                        .cookie(teacher)
                        .header("Origin", origin)
                        .contentType("application/json")
                        .content(json.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    long aid = quiz.get("id").asLong();
    payload.put("title", "Updated mini assessment");
    mvc.perform(
            put("/api/assessments/" + aid)
                .cookie(teacher)
                .header("Origin", origin)
                .contentType("application/json")
                .content(json.writeValueAsString(payload)))
        .andExpect(status().isOk());
    JsonNode updated = getJson("/api/assessments/" + aid, student);
    assertEquals("Updated mini assessment", updated.get("title").asText());
    mvc.perform(delete("/api/assessments/" + aid).cookie(teacher).header("Origin", origin))
        .andExpect(status().isOk());
    mvc.perform(get("/api/assessments/" + aid).cookie(student)).andExpect(status().isNotFound());
    mvc.perform(delete("/api/topics/" + tid).cookie(teacher).header("Origin", origin))
        .andExpect(status().isOk());
  }

  @Test
  void otherTeachersCannotReadOrEditOwnedData() throws Exception {
    String email = UUID.randomUUID() + "@teacher.test";
    var r =
        mvc.perform(
                post("/api/auth/register")
                    .header("Origin", origin)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "name",
                                "Other Teacher",
                                "email",
                                email,
                                "password",
                                "GapMap123!",
                                "role",
                                "TEACHER",
                                "teacherCode",
                                "TEST-TEACHER"))))
            .andExpect(status().isOk())
            .andReturn();
    String c = r.getResponse().getHeader("Set-Cookie");
    Cookie teacher = new Cookie("gapmap_session", c.split(";", 2)[0].split("=", 2)[1]);
    mvc.perform(get("/api/assessments/1").cookie(teacher)).andExpect(status().isForbidden());
    mvc.perform(
            put("/api/topics/1")
                .cookie(teacher)
                .header("Origin", origin)
                .contentType("application/json")
                .content("{\"name\":\"Hacked\",\"prerequisiteIds\":[]}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/students/2/report").cookie(teacher)).andExpect(status().isForbidden());
  }

  @Test
  void recommendationsCompleteAndMiniQuizAddsProgress() throws Exception {
    Cookie s = login("student6@gapmap.test");
    JsonNode report = getJson("/api/dashboards/student", s);
    assertTrue(report.get("recommendations").size() > 0);
    long rid = report.get("recommendations").get(0).get("id").asLong();
    mvc.perform(
            patch("/api/recommendations/" + rid)
                .cookie(s)
                .header("Origin", origin)
                .contentType("application/json")
                .content("{\"done\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"));
    mvc.perform(
            patch("/api/recommendations/" + rid)
                .cookie(login("student7@gapmap.test"))
                .header("Origin", origin)
                .contentType("application/json")
                .content("{\"done\":true}"))
        .andExpect(status().isForbidden());
    JsonNode list = getJson("/api/assessments", s);
    long aid = 0;
    for (JsonNode a : list)
      if (a.get("miniQuiz").asBoolean()) {
        aid = a.get("id").asLong();
        break;
      }
    assertTrue(aid > 0);
    JsonNode quiz = getJson("/api/assessments/" + aid, s);
    List<Map<String, Object>> answers = new ArrayList<>();
    for (JsonNode q : quiz.get("questions"))
      answers.add(
          Map.of(
              "questionId",
              q.get("id").asLong(),
              "optionId",
              q.get("options").get(0).get("id").asLong(),
              "confidence",
              "NOT_SURE"));
    mvc.perform(
            post("/api/attempts")
                .cookie(s)
                .header("Origin", origin)
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "assessmentId",
                            aid,
                            "submissionKey",
                            UUID.randomUUID().toString(),
                            "answers",
                            answers))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.miniQuiz").value(true));
    assertEquals(
        report.get("attempts").size() + 1, getJson("/api/progress", s).get("attempts").size());
  }

  @Test
  void concurrentRetriesPersistOnlyOneAttempt() throws Exception {
    Cookie s = login("student8@gapmap.test");
    JsonNode quiz = getJson("/api/assessments/1", s);
    List<Map<String, Object>> answers = new ArrayList<>();
    for (JsonNode q : quiz.get("questions"))
      answers.add(
          Map.of(
              "questionId",
              q.get("id").asLong(),
              "optionId",
              q.get("options").get(0).get("id").asLong(),
              "confidence",
              "FAIRLY_SURE"));
    Map<String, Object> body =
        new HashMap<>(
            Map.of(
                "assessmentId",
                1,
                "submissionKey",
                UUID.randomUUID().toString(),
                "answers",
                answers));
    String payload = json.writeValueAsString(body);
    long before = attempts.count();
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      java.util.concurrent.Callable<Long> call =
          () ->
              json.readTree(
                      mvc.perform(
                              post("/api/attempts")
                                  .cookie(s)
                                  .header("Origin", origin)
                                  .contentType("application/json")
                                  .content(payload))
                          .andExpect(status().isOk())
                          .andReturn()
                          .getResponse()
                          .getContentAsString())
                  .get("id")
                  .asLong();
      var f1 = executor.submit(call);
      var f2 = executor.submit(call);
      assertEquals(
          f1.get(20, java.util.concurrent.TimeUnit.SECONDS),
          f2.get(20, java.util.concurrent.TimeUnit.SECONDS));
      assertEquals(before + 1, attempts.count());
    } finally {
      executor.shutdownNow();
    }
    body.put("assessmentId", 2);
    mvc.perform(
            post("/api/attempts")
                .cookie(s)
                .header("Origin", origin)
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
  }
}
