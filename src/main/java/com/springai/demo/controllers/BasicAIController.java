package com.springai.demo.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class BasicAIController {

    private final OllamaChatModel chatModel;

    @GetMapping("/ai/ask")
    public String ask(@RequestParam(value = "question", defaultValue = "Cuéntame un chiste") String message) {
        return this.chatModel.call(message);
    }
}
