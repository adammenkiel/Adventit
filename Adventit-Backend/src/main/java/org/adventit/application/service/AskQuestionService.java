package org.adventit.application.service;

import org.adventit.application.port.in.AskQuestionUseCase;
import org.adventit.application.port.out.QuestionAnswerer;
import org.springframework.stereotype.Service;

@Service
public class AskQuestionService implements AskQuestionUseCase {
    private final QuestionAnswerer questionAnswerer;

    public AskQuestionService(QuestionAnswerer questionAnswerer) {
        this.questionAnswerer = questionAnswerer;
    }

    @Override
    public String ask(String question) {
        return questionAnswerer.answer(question);
    }
}
