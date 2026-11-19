package com.attendance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceTest {

    private static ConfigurableApplicationContext app;
    private static String baseUrl;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final AtomicInteger IDS = new AtomicInteger(61000);

    // Start a fresh application on an available random port for this test class.
    @BeforeAll
    static void startApplication() {
        app = new SpringApplication(AttendanceApplication.class)
                .run("--server.port=0", "--spring.main.banner-mode=off");
        int port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port",
                app.getEnvironment().getProperty("server.port")));
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    static void stopApplication() {
        if (app != null) {
            app.close();
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private int createStudent() throws Exception {
        int id = IDS.incrementAndGet();
        String body = "{\"id\":" + id
                + ",\"name\":\"Rahul\",\"department\":\"CSE\"}";
        HttpResponse<String> response = post("/api/students", body);
        assertEquals(200, response.statusCode(), "Could not create test student: " + response.body());
        return id;
    }

    @Test
    void TC01_addStudent() throws Exception {
        int id = createStudent();
        HttpResponse<String> response = get("/api/students/" + id);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"name\":\"Rahul\""));
    }

    @Test
    void TC02_viewAllStudents() throws Exception {
        createStudent();
        HttpResponse<String> response = get("/api/students");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
    }

    @Test
    void TC03_searchStudentById() throws Exception {
        int id = createStudent();
        HttpResponse<String> response = get("/api/students/" + id);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"id\":" + id));
    }

    @Test
    void TC04_markPresent() throws Exception {
        int id = createStudent();
        String body = "{\"studentId\":" + id
                + ",\"date\":\"2026-11-18\",\"status\":\"Present\"}";
        HttpResponse<String> response = post("/api/attendance", body);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"status\":\"Present\""));
    }

    @Test
    void TC05_markAbsent() throws Exception {
        int id = createStudent();
        String body = "{\"studentId\":" + id
                + ",\"date\":\"2026-11-18\",\"status\":\"Absent\"}";
        HttpResponse<String> response = post("/api/attendance", body);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"status\":\"Absent\""));
    }

    @Test
    void TC06_viewAttendance() throws Exception {
        HttpResponse<String> response = get("/api/attendance");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("["));
    }

    @Test
    void TC07_invalidStudentId() throws Exception {
        HttpResponse<String> response = get("/api/students/999999999");
        assertEquals(404, response.statusCode());
    }

    @Test
    void TC08_duplicateStudent() throws Exception {
        int id = createStudent();
        String body = "{\"id\":" + id
                + ",\"name\":\"Rahul\",\"department\":\"CSE\"}";
        HttpResponse<String> response = post("/api/students", body);
        assertEquals(409, response.statusCode());
    }

    @Test
    void TC09_invalidAttendanceStatus() throws Exception {
        int id = createStudent();
        String body = "{\"studentId\":" + id
                + ",\"date\":\"2026-11-18\",\"status\":\"Holiday\"}";
        HttpResponse<String> response = post("/api/attendance", body);
        assertEquals(400, response.statusCode());
    }
}
