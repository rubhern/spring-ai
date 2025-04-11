package com.springai.demo.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
public class BasicWithParamsAIController {

    private final Logger logger = Logger.getLogger(BasicWithParamsAIController.class.getName());
    private final OllamaChatModel chatModel;


    @GetMapping("/ai/videogames/info")
    public Map<String, String> generate(@RequestParam(value = "videogame", defaultValue = "The witcher") String message,
                                        @RequestParam(value = "model", defaultValue = "gemma3") String model,
                                        @RequestParam(value = "temperature", defaultValue = "0.4") double temperature) {
        ChatResponse chatResponse = this.chatModel.call(new Prompt(
                "Describe brevemente el argumento del videojuego " + message + " en menos de 100 palabras. El público objetivo es un jugador habitual que ya está familiarizado con mecánicas comunes de videojuegos. No expliques controles ni tutoriales. Enfócate en la historia principal, el conflicto central y el tono del juego (oscuro, épico, humorístico, etc.). Usa un estilo directo, atractivo y con gancho, como si fuera la sinopsis de la contraportada del juego.\n",
                OllamaOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build()
        ));
        logger.info(chatResponse.toString());
        return Map.of("response", chatResponse.getResult().getOutput().getText());
    }
}
