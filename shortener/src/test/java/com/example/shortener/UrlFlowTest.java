package com.example.shortener;

import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlFlowTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @Container @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ---------- helpers ----------

    private String registerAndLogin() throws Exception {
        String body = json.writeValueAsString(Map.of(
            "email", UUID.randomUUID() + "@test.com", "password", "password123"));
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body))
           .andExpect(status().isCreated());
        String res = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(body))
           .andExpect(status().isOk())
           .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private String createLink(String token, String url) throws Exception {
        String res = mvc.perform(post("/api/urls")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("url", url))))
           .andExpect(status().isCreated())
           .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("code").asText();
    }

    // ---------- the happy path ----------

    @Test
    void createRedirectAndCountClick() throws Exception {
        String token = registerAndLogin();
        String code = createLink(token, "https://example.com/hello");

        mvc.perform(get("/" + code))
           .andExpect(status().isFound())
           .andExpect(header().string("Location", "https://example.com/hello"));

        // Analytics is asynchronous (Kafka), so wait for the count instead of asserting at once
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            mvc.perform(get("/api/urls/" + code + "/stats").header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalClicks").value(1)));
    }

    // ---------- the failure paths (these prove your security and validation work) ----------

    @Test
    void creatingALinkWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/urls").contentType(APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\"}"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void anotherUserCannotReadMyStats() throws Exception {
        String ownerToken = registerAndLogin();
        String intruderToken = registerAndLogin();
        String code = createLink(ownerToken, "https://example.com/private");

        mvc.perform(get("/api/urls/" + code + "/stats").header("Authorization", "Bearer " + intruderToken))
           .andExpect(status().isForbidden());
    }

    @Test
    void javascriptUrlsAreRejected() throws Exception {
        String token = registerAndLogin();
        mvc.perform(post("/api/urls")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("{\"url\":\"javascript:alert(1)\"}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mvc.perform(get("/doesNotExist")).andExpect(status().isNotFound());
    }
}