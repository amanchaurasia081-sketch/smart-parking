package com.smartparking.smart_parking;

import org.junit.jupiter.api.Test;
import com.smartparking.smart_parking.entity.User;
import com.smartparking.smart_parking.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SmartParkingApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TokenService tokenService;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Test
	void contextLoads() {
	}

	@Test
	void parkingAvailabilityIsPublic() throws Exception {
		assertEquals(200, get("/api/slots", null).statusCode());
	}

	@Test
	void bookingAdministrationRequiresAnAdminToken() throws Exception {
		assertEquals(401, get("/api/bookings", null).statusCode());

		User regularUser = new User();
		regularUser.setEmail("user@example.com");
		regularUser.setRole("USER");
		assertEquals(403, get("/api/bookings", tokenService.issue(regularUser)).statusCode());

		User admin = new User();
		admin.setEmail("admin@example.com");
		admin.setRole("ADMIN");
		assertEquals(200, get("/api/bookings", tokenService.issue(admin)).statusCode());
	}

	@Test
	void registrationCreatesAUserTokenAndCannotAssignAdminRole() throws Exception {
		HttpResponse<String> response = post("/api/auth/register", """
				{"name":"Test User","email":"new.user@example.com","password":"test-password","vehicle":"TEST-01","role":"ADMIN"}
				""", null);
		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("\"role\":\"USER\""));
		assertFalse(response.body().contains("\"password\""));

		Matcher tokenMatcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());
		assertTrue(tokenMatcher.find());
		String token = tokenMatcher.group(1);
		assertEquals(200, get("/api/bookings/user/new.user@example.com", token).statusCode());
		assertEquals(403, get("/api/bookings/user/other.user@example.com", token).statusCode());

		String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
		HttpResponse<String> bookingResponse = post("/api/bookings", """
				{"slotNumber":"A01","vehicle":"test-01","bookingDate":"%s","duration":1,"price":999,"status":"completed","userEmail":"other.user@example.com","paymentStatus":"Paid","entryTime":"spoofed","exitTime":"spoofed"}
				""".formatted(today), token);
		assertEquals(200, bookingResponse.statusCode());
		assertTrue(bookingResponse.body().contains("\"userEmail\":\"new.user@example.com\""));
		assertTrue(bookingResponse.body().contains("\"status\":\"active\""));
		assertTrue(bookingResponse.body().contains("\"paymentStatus\":\"Pending\""));
		assertTrue(bookingResponse.body().contains("\"price\":20.0"));
		assertTrue(bookingResponse.body().contains("\"entryTime\":null"));
		assertTrue(get("/api/slots", null).body().contains("\"slotNumber\":\"A01\",\"status\":\"OCCUPIED\""));
	}

	private HttpResponse<String> get(String path, String token) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.GET();
		if (token != null) {
			request.header("Authorization", "Bearer " + token);
		}
		return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path, String body, String token) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body));
		if (token != null) {
			request.header("Authorization", "Bearer " + token);
		}
		return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}
