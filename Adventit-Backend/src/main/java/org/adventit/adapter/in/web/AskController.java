package org.adventit.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.adventit.application.port.in.AskQuestionUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AskController {
    private final AskQuestionUseCase askQuestion;

    public AskController(AskQuestionUseCase askQuestion) {
        this.askQuestion = askQuestion;
    }

    @PostMapping("/app/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return new AskResponse(askQuestion.ask(request.question()));
    }

    public record AskRequest(@NotBlank @Size(max = 4000) String question) {
    }

    public record AskResponse(String answer) {
    }
}
