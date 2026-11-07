package com.gapmap.config;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import com.gapmap.service.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements CommandLineRunner {
  final UserRepository users;
  final ClassRoomRepository classes;
  final TopicRepository topics;
  final AssessmentRepository assessments;
  final LearningMaterialRepository materials;
  final AttemptRepository attempts;
  final AnalysisService analysis;
  final StudyPathService paths;
  final PasswordEncoder passwords;

  @Value("${app.seed}")
  boolean enabled;

  @Value("${app.seed-password}")
  String password;

  public SeedData(
      UserRepository u,
      ClassRoomRepository c,
      TopicRepository t,
      AssessmentRepository a,
      LearningMaterialRepository m,
      AttemptRepository at,
      AnalysisService an,
      StudyPathService p,
      PasswordEncoder pe) {
    users = u;
    classes = c;
    topics = t;
    assessments = a;
    materials = m;
    attempts = at;
    analysis = an;
    paths = p;
    passwords = pe;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled || users.count() > 0) return;
    AccessService.require(password.length() >= 8, "Set SEED_PASSWORD to at least 8 characters");
    User teacher = user("Alex Rivera", "teacher@gapmap.test", Role.TEACHER);
    ClassRoom c = new ClassRoom();
    c.name = "Programming Fundamentals · G1";
    c.teacher = teacher;
    c.joinCode = "GAPMAP-G1";
    List<User> students = new ArrayList<>();
    String[] names = {
      "Jamie Santos",
      "Mika Reyes",
      "Carlo Cruz",
      "Ana Garcia",
      "Leo Tan",
      "Bea Flores",
      "Nico Ramos",
      "Ella Lim",
      "Luis Mendoza",
      "Sam Torres"
    };
    for (int i = 0; i < 10; i++) {
      User s = user(names[i], "student" + (i + 1) + "@gapmap.test", Role.STUDENT);
      students.add(s);
      c.students.add(s);
    }
    classes.save(c);
    String[] titles = {"Variables", "Operators", "Conditionals", "Loops", "Arrays", "Methods"};
    String[] lessons = {
      "A variable gives a value a name. Example: int count = 3; count = 5; replaces 3 with 5."
          + " Primitive assignment copies the current value, not a live link.",
      "Operators combine values. Multiplication has higher precedence than addition. Integer"
          + " division truncates: 5 / 2 is 2. The remainder operator % gives 7 % 3 = 1. Use == to"
          + " compare and = to assign.",
      "An if statement runs when its boolean condition is true. Use && when both bounds must hold."
          + " Example: score >= 50 && score <= 100. An if / else if chain executes the first"
          + " matching branch.",
      "Trace loops using a table of counter and accumulator values. In for(int i=0;i<3;i++), the"
          + " body runs for 0, 1, 2. Break exits a loop; continue skips the rest of the iteration.",
      "Arrays are indexed from zero. For int[] a={2,4,6}, a[1] is 4. Valid indices are 0 through"
          + " a.length-1. Use i < a.length to avoid an out-of-bounds access.",
      "Methods receive arguments through parameters and may return a value. Example: int twice(int"
          + " x){ return x*2; }. A void method returns no value. Recursion requires a reachable"
          + " base case."
    };
    List<Topic> ts = new ArrayList<>();
    for (int i = 0; i < titles.length; i++) {
      Topic t = new Topic();
      t.name = titles[i];
      t.description = "Build confidence with " + titles[i].toLowerCase() + " in Java.";
      t.teacher = teacher;
      if (i > 0) t.prerequisites.add(ts.get(i - 1));
      topics.save(t);
      ts.add(t);
      material(t, "Understand " + titles[i], MaterialType.LESSON, lessons[i]);
      material(
          t,
          "Practice: " + titles[i],
          MaterialType.PRACTICE,
          "Read the lesson, write two small Java examples, and predict their output before running"
              + " them. Explain each step aloud. Then take the "
              + titles[i]
              + " mini-quiz and compare your new score with your previous attempt.");
    }
    Assessment main = new Assessment();
    main.title = "Programming foundations";
    main.classRoom = c;
    main.createdBy = teacher;
    main.published = true;
    addQuestion(
        main,
        ts.get(0),
        Difficulty.EASY,
        "What does a variable store?",
        "A value",
        "A compiler",
        "A loop",
        "A variable names a storage location for a value.");
    addQuestion(
        main,
        ts.get(0),
        Difficulty.EASY,
        "After int x = 4; x = 7; what is x?",
        "7",
        "4",
        "11",
        "Assignment replaces the previous value.");
    addQuestion(
        main,
        ts.get(0),
        Difficulty.MEDIUM,
        "Which Java type stores true or false?",
        "boolean",
        "int",
        "String",
        "The boolean type represents true or false.");
    addQuestion(
        main,
        ts.get(0),
        Difficulty.MEDIUM,
        "What is the value of int x = 3 + 2 * 4?",
        "11",
        "20",
        "14",
        "Multiplication is evaluated before addition.");
    addQuestion(
        main,
        ts.get(0),
        Difficulty.HARD,
        "After int a = 3; int b = a; a = 9; what is b?",
        "3",
        "9",
        "12",
        "Primitive assignment copies the value at that moment.");
    addQuestion(
        main,
        ts.get(1),
        Difficulty.EASY,
        "What is 7 % 3 in Java?",
        "1",
        "2",
        "3",
        "The remainder after dividing 7 by 3 is 1.");
    addQuestion(
        main,
        ts.get(1),
        Difficulty.EASY,
        "What is 5 / 2 using Java integers?",
        "2",
        "2.5",
        "3",
        "Integer division discards the fractional part.");
    addQuestion(
        main,
        ts.get(1),
        Difficulty.MEDIUM,
        "Which expression tests equality?",
        "x == 5",
        "x = 5",
        "x => 5",
        "The == operator compares values; = assigns them.");
    addQuestion(
        main,
        ts.get(1),
        Difficulty.MEDIUM,
        "What is true && false?",
        "false",
        "true",
        "null",
        "Logical AND requires both operands to be true.");
    addQuestion(
        main,
        ts.get(1),
        Difficulty.HARD,
        "What is !(3 > 5)?",
        "true",
        "false",
        "3",
        "3 > 5 is false, then ! negates it.");
    addQuestion(
        main,
        ts.get(2),
        Difficulty.EASY,
        "When does an if block execute?",
        "When its condition is true",
        "Always",
        "When its condition is false",
        "An if statement executes its body only for a true condition.");
    addQuestion(
        main,
        ts.get(2),
        Difficulty.EASY,
        "For x = 8, is x >= 8 true?",
        "Yes",
        "No",
        "Only for strings",
        "Greater than or equal includes the boundary value.");
    addQuestion(
        main,
        ts.get(2),
        Difficulty.MEDIUM,
        "What does else handle?",
        "The false condition case",
        "The next iteration",
        "A return value",
        "Else runs when the associated if condition is false.");
    addQuestion(
        main,
        ts.get(2),
        Difficulty.MEDIUM,
        "For x = 4, which condition checks 1 through 5 inclusive?",
        "x >= 1 && x <= 5",
        "x > 1 || x < 5",
        "x == 1 && x == 5",
        "Both lower and upper bounds must hold.");
    addQuestion(
        main,
        ts.get(2),
        Difficulty.HARD,
        "In if / else if / else, how many branches execute?",
        "At most one",
        "All true branches",
        "Exactly two",
        "The first matching branch executes and the chain ends.");
    addQuestion(
        main,
        ts.get(3),
        Difficulty.EASY,
        "How many times does for(int i=0; i<3; i++) run?",
        "3",
        "2",
        "4",
        "The body runs for i = 0, 1, and 2.");
    addQuestion(
        main,
        ts.get(3),
        Difficulty.EASY,
        "What does break do in a loop?",
        "Exits the loop",
        "Skips one statement",
        "Restarts the loop",
        "Break exits the nearest enclosing loop.");
    addQuestion(
        main,
        ts.get(3),
        Difficulty.MEDIUM,
        "What does continue do?",
        "Skips to the next iteration",
        "Exits the program",
        "Resets the counter",
        "Continue skips the rest of the current loop iteration.");
    addQuestion(
        main,
        ts.get(3),
        Difficulty.MEDIUM,
        "What is sum after sum=0; for(int i=1;i<=3;i++) sum+=i;?",
        "6",
        "3",
        "9",
        "The sum is 1 + 2 + 3 = 6.");
    addQuestion(
        main,
        ts.get(3),
        Difficulty.HARD,
        "How many prints occur in two nested loops, each running 3 times?",
        "9",
        "6",
        "3",
        "The inner loop runs 3 times for each of 3 outer iterations.");
    addQuestion(
        main,
        ts.get(4),
        Difficulty.EASY,
        "What is the first index of a Java array?",
        "0",
        "1",
        "-1",
        "Java arrays use zero-based indexing.");
    addQuestion(
        main,
        ts.get(4),
        Difficulty.EASY,
        "For int[] a={2,4,6}; what is a[1]?",
        "4",
        "2",
        "6",
        "Index 1 refers to the second element.");
    addQuestion(
        main,
        ts.get(4),
        Difficulty.MEDIUM,
        "For an array of length 5, what is the last valid index?",
        "4",
        "5",
        "6",
        "The last valid index is length minus one.");
    addQuestion(
        main,
        ts.get(4),
        Difficulty.MEDIUM,
        "Which condition safely loops through every index?",
        "i < a.length",
        "i <= a.length",
        "i > a.length",
        "The index must be strictly less than the array length.");
    addQuestion(
        main,
        ts.get(4),
        Difficulty.HARD,
        "What happens when accessing a[a.length]?",
        "An index exception",
        "The last value",
        "Zero",
        "a.length is one past the last valid index.");
    addQuestion(
        main,
        ts.get(5),
        Difficulty.EASY,
        "What does a method parameter receive?",
        "An argument value",
        "A class name only",
        "An exception only",
        "Parameters receive values passed by the caller.");
    addQuestion(
        main,
        ts.get(5),
        Difficulty.EASY,
        "What does return do?",
        "Exits the method with an optional value",
        "Restarts a loop",
        "Declares a variable",
        "Return transfers control back to the caller.");
    addQuestion(
        main,
        ts.get(5),
        Difficulty.MEDIUM,
        "If int twice(int x){return x*2;}, what is twice(4)?",
        "8",
        "4",
        "2",
        "The method multiplies its argument by two.");
    addQuestion(
        main,
        ts.get(5),
        Difficulty.MEDIUM,
        "Can a void method return a numeric value?",
        "No",
        "Yes",
        "Only zero",
        "Void means the method does not return a value.");
    addQuestion(
        main,
        ts.get(5),
        Difficulty.HARD,
        "What does a recursive method need to stop?",
        "A reachable base case",
        "A global array",
        "An infinite loop",
        "A base case ends recursion before another recursive call.");
    assessments.saveAndFlush(main);
    for (Topic t : ts) {
      Assessment mini = new Assessment();
      mini.title = t.name + " · knowledge check";
      mini.classRoom = c;
      mini.createdBy = teacher;
      mini.miniQuiz = true;
      mini.topic = t;
      mini.published = true;
      main.questions.stream()
          .filter(q -> q.topic.id.equals(t.id))
          .limit(3)
          .forEach(
              q -> {
                Question copy = new Question();
                copy.assessment = mini;
                copy.topic = t;
                copy.text = q.text;
                copy.explanation = q.explanation;
                copy.difficulty = q.difficulty;
                for (var o : q.options) {
                  QuestionOption z = new QuestionOption();
                  z.question = copy;
                  z.text = o.text;
                  z.correct = o.correct;
                  copy.options.add(z);
                }
                mini.questions.add(copy);
              });
      assessments.save(mini);
    }
    Random random = new Random(42);
    for (int s = 0; s < students.size(); s++) {
      for (int round = 0; round < 3; round++) {
        Attempt a = new Attempt();
        a.assessment = main;
        a.student = students.get(s);
        a.submittedAt = Instant.now().minusSeconds((21L - round * 7) * 86400 + s * 60);
        a.submissionKey = "seed-" + s + "-" + round;
        a.payloadHash = "seed";
        for (Question q : main.questions) {
          int ti = ts.indexOf(q.topic);
          double chance = Math.min(.95, .45 + round * .12 + (s % 4) * .08 - ti * .06);
          boolean correct = random.nextDouble() < chance;
          Response response = new Response();
          response.attempt = a;
          response.question = q;
          response.selectedOption =
              q.options.stream().filter(o -> o.correct == correct).findFirst().orElseThrow();
          response.correct = correct;
          response.confidenceLevel = ConfidenceLevel.values()[random.nextInt(3)];
          a.responses.add(response);
        }
        attempts.saveAndFlush(a);
        analysis.computeTopicScores(a.id);
        attempts.flush();
      }
      paths.generateStudyPath(students.get(s).id);
    }
  }

  User user(String n, String e, Role role) {
    User u = new User();
    u.name = n;
    u.email = e;
    u.role = role;
    u.passwordHash = passwords.encode(password);
    return users.save(u);
  }

  void material(Topic t, String title, MaterialType type, String content) {
    LearningMaterial m = new LearningMaterial();
    m.topic = t;
    m.title = title;
    m.type = type;
    m.urlOrContent = content;
    materials.save(m);
  }

  void addQuestion(
      Assessment a,
      Topic t,
      Difficulty d,
      String text,
      String correct,
      String wrong1,
      String wrong2,
      String explanation) {
    Question q = new Question();
    q.assessment = a;
    q.topic = t;
    q.difficulty = d;
    q.text = text;
    q.explanation = explanation;
    List<String> options = new ArrayList<>(List.of(correct, wrong1, wrong2));
    Collections.rotate(options, a.questions.size() % 3);
    for (String value : options) {
      QuestionOption o = new QuestionOption();
      o.question = q;
      o.text = value;
      o.correct = value.equals(correct);
      q.options.add(o);
    }
    a.questions.add(q);
  }
}
