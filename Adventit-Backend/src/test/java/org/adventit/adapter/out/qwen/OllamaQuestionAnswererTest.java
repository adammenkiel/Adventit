package org.adventit.adapter.out.qwen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaQuestionAnswererTest {
    private static final String ENDPOINT = "http://localhost:11434/api/chat";

    @Test
    void sendsNonStreamingQwenChatRequestAndReturnsAssistantMessage() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "model": "qwen2.5:latest",
                          "messages": [{"role": "user", "content": "How are you?"}],
                          "stream": false
                        }
                        """))
                .andRespond(withSuccess("""
                        {"message":{"role":"assistant","content":"I am doing well."}}
                        """, MediaType.APPLICATION_JSON));
        OllamaQuestionAnswerer answerer =
                new OllamaQuestionAnswerer(restClientBuilder, ENDPOINT, "qwen2.5:latest");

        assertEquals("I am doing well.", answerer.answer("How are you?"));
        server.verify();
    }

    @Test
    void rejectsOllamaResponseWithoutAnAnswer() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        OllamaQuestionAnswerer answerer =
                new OllamaQuestionAnswerer(restClientBuilder, ENDPOINT, "qwen2.5:latest");

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> answerer.answer("How are you?"));

        assertEquals("Ollama returned no answer", exception.getMessage());
        server.verify();
    }
}
