package com.gapmap.service;

import com.gapmap.entity.*;
import com.gapmap.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessService {
  private final UserRepository users;

  public AccessService(UserRepository users) {
    this.users = users;
  }

  public User current() {
    return users
        .findById(Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName()))
        .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
  }

  public User lockCurrent() {
    return found(users.lockById(current().id));
  }

  public void teacher(User owner) {
    if (current().role != Role.TEACHER || !owner.id.equals(current().id))
      throw new AccessDeniedException("You do not own this resource");
  }

  public void classroom(ClassRoom c) {
    var u = current();
    if (u.role == Role.TEACHER) {
      teacher(c.teacher);
      return;
    }
    if (c.students.stream().noneMatch(s -> s.id.equals(u.id)))
      throw new AccessDeniedException("Join this class first");
  }

  public void student(User student, ClassRoom c) {
    var u = current();
    if (!u.id.equals(student.id)) {
      teacher(c.teacher);
    }
  }

  public static <T> T found(java.util.Optional<T> value) {
    return value.orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
  }

  public static void require(boolean ok, String message) {
    if (!ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
