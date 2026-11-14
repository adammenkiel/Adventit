package org.adventit.adapter.out.qwen;

import java.util.List;
import org.adventit.application.port.out.QuestionAnswerer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaQuestionAnswerer implements QuestionAnswerer {
    private final RestClient restClient;
    private final String endpoint;
    private final String model;

    public OllamaQuestionAnswerer(
            RestClient.Builder restClientBuilder,
            @Value("${app.qwen.ollama.endpoint:http://localhost:11434/api/chat}") String endpoint,
            @Value("${app.qwen.model:qwen2.5:latest}") String model) {
        this.restClient = restClientBuilder.build();
        this.endpoint = endpoint;
        this.model = model;
    }

    @Override
    public String answer(String question) {
        OllamaResponse response = restClient.post()
                .uri(endpoint)
                .body(new OllamaRequest(model, List.of(new OllamaMessage("user", question)), false))
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Ollama returned no answer");
        }
        return response.message().content();
    }

    private record OllamaRequest(String model, List<OllamaMessage> messages, boolean stream) {
    }

    private record OllamaMessage(String role, String content) {
    }

    private record OllamaResponse(OllamaMessage message) {
    }
}
