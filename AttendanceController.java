package com.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AttendanceController {
    private final Map<Integer, Student> students = new ConcurrentHashMap<>();
    private final List<Attendance> records =
            Collections.synchronizedList(new ArrayList<>());

    @PostMapping("/students")
    public Student addStudent(@RequestBody Student student) {
        if (student == null || student.getId() <= 0
                || student.getName() == null || student.getName().isBlank()
                || student.getDepartment() == null || student.getDepartment().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid student details");
        }
        if (students.putIfAbsent(student.getId(), student) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already exists");
        }
        return student;
    }

    @GetMapping("/students")
    public Collection<Student> getStudents() {
        return students.values();
    }

    @GetMapping("/students/{id}")
    public Student getStudent(@PathVariable int id) {
        Student student = students.get(id);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        return student;
    }

    @PostMapping("/attendance")
    public Attendance markAttendance(@RequestBody Attendance record) {
        if (record == null || !students.containsKey(record.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        if (!"Present".equals(record.getStatus()) && !"Absent".equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be Present or Absent");
        }
        try {
            LocalDate.parse(record.getDate());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must be yyyy-MM-dd");
        }
        records.add(record);
        return record;
    }

    @GetMapping("/attendance")
    public List<Attendance> getAttendance() {
        synchronized (records) {
            return new ArrayList<>(records);
        }
    }
}
