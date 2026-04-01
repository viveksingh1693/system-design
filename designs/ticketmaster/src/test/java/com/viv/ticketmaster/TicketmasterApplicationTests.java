package com.viv.ticketmaster;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


@SpringBootTest
@AutoConfigureMockMvc
class TicketmasterApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldCreateAndConfirmBookingFlow() throws Exception {
		String eventPayload = """
				{
				  "title": "Coldplay Music of the Spheres",
				  "performer": "Coldplay",
				  "city": "Bengaluru",
				  "venueName": "Chinnaswamy Stadium",
				  "category": "Concert"
				}
				""";

		String eventResponse = mockMvc.perform(post("/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Coldplay Music of the Spheres"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long eventId = readId(eventResponse);

		String showPayload = """
				{
				  "startTime": "%s",
				  "price": 4999.00,
				  "totalSeats": 100
				}
				""".formatted(LocalDateTime.now().plusDays(10).format(DateTimeFormatter.ISO_DATE_TIME));

		String showResponse = mockMvc.perform(post("/events/{eventId}/shows", eventId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(showPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableSeats").value(100))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long showId = readId(showResponse);

		String holdPayload = """
				{
				  "showId": %d,
				  "customerEmail": "fan@example.com",
				  "seatCount": 4
				}
				""".formatted(showId);

		String bookingResponse = mockMvc.perform(post("/bookings/hold")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("HELD"))
				.andExpect(jsonPath("$.seatCount").value(4))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long bookingId = readId(bookingResponse);

		String confirmPayload = """
				{
				  "paymentToken": "tok-success"
				}
				""";

		mockMvc.perform(post("/bookings/{bookingId}/confirm", bookingId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(confirmPayload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.paymentReference").isNotEmpty());

		mockMvc.perform(get("/shows/{showId}", showId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableSeats").value(96));
	}

	@Test
	void shouldRejectHoldWhenInventoryIsInsufficient() throws Exception {
		String eventPayload = """
				{
				  "title": "IPL Finals",
				  "performer": "Cricket League",
				  "city": "Mumbai",
				  "venueName": "Wankhede Stadium",
				  "category": "Sports"
				}
				""";

		String eventResponse = mockMvc.perform(post("/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long eventId = readId(eventResponse);

		String showPayload = """
				{
				  "startTime": "%s",
				  "price": 999.00,
				  "totalSeats": 2
				}
				""".formatted(LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ISO_DATE_TIME));

		String showResponse = mockMvc.perform(post("/events/{eventId}/shows", eventId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(showPayload))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long showId = readId(showResponse);

		String holdPayload = """
				{
				  "showId": %d,
				  "customerEmail": "fan@example.com",
				  "seatCount": 5
				}
				""".formatted(showId);

		mockMvc.perform(post("/bookings/hold")
						.contentType(MediaType.APPLICATION_JSON)
						.content(holdPayload))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_SEATS"));
	}

	private Long readId(String json) throws Exception {
		Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("Could not extract id from response: " + json);
		}
		return Long.valueOf(matcher.group(1));
	}
}
