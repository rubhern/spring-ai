package com.example.demo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
public class ChatController {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final Logger logger = Logger.getLogger(ChatController.class.getName());
    private final OllamaChatModel chatModel;
    private final List<String> baseVideogames = List.of(
            "The Witcher 3 es un RPG de mundo abierto en el que Geralt de Rivia busca a su hija adoptiva en un continente asolado por la guerra.",
            "Hollow Knight es una aventura de exploración en un mundo subteráneo, con combate preciso y atmósfera oscura.",
            "Portal 2 es un juego de puzles en primera persona donde el jugador usa portales para resolver desafíos en una instalación científica abandonada.",
            "Celeste es un desafiante juego de plataformas donde una joven escala una montaña mientras lucha contra sus propios demonios internos.",
            "Red Dead Redemption 2 cuenta la historia de Arthur Morgan, un forajido atrapado entre su lealtad a su banda y sus principios morales."
    );

    @PostConstruct
    public void initStore() {
        //vectorDBHardcode();
        vectorFromPDF();
    }

    private void vectorFromPDF() {
        try {
            Path pdfPath = Path.of(getClass().getClassLoader().getResource("Confidential_Tech_Cars_Specs.pdf").toURI());
            String text;
            try (PDDocument document = PDDocument.load(Files.newInputStream(pdfPath))) {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(document);
            }

            String[] paragraphs = text.split("\n\n");
            for (String paragraph : paragraphs) {
                paragraph = paragraph.trim();
                if (!paragraph.isEmpty()) {
                    float[] embeddingArray = embeddingModel.embed(paragraph);
                    List<Float> embedding = new ArrayList<>(embeddingArray.length);
                    for (float value : embeddingArray) {
                        embedding.add(value);
                    }
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("embedding", embedding);
                    Document doc = new Document(paragraph, metadata);
                    vectorStore.add(List.of(doc));
                }
            }
            logger.info("PDF loaded and indexed into vector store.");
        } catch (Exception e) {
            logger.severe("Failed to load PDF: " + e.getMessage());
        }
    }

    private void vectorDBHardcode() {
        for (String content : baseVideogames) {
            float[] embeddingArray = embeddingModel.embed(content);
            List<Float> embedding = new ArrayList<>(embeddingArray.length);
            for (float value : embeddingArray) {
                embedding.add(value);
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("embedding", embedding);
            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));
        }
    }


    @GetMapping("/ai/videogame/info")
    public Map<String, String> generate(@RequestParam(value = "videogame", defaultValue = "Tell me a joke") String message,
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

    @GetMapping("/ai/cars")
    public String generate(@RequestParam(value = "question", defaultValue = "Tell me a joke") String message) {
        return this.chatModel.call(message);
    }

    @GetMapping("/ai/cars/rag")
    public Map<String, String> ask(@RequestParam("question") String question) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(3)
                .build();

        List<String> context = vectorStore.similaritySearch(searchRequest).stream()
                .map(Document::getText)
                .collect(Collectors.toList());

        String prompt = """
                Contexto:
                %s

                Pregunta:
                %s

                Responde basándote únicamente en el contexto. Si no tienes suficiente información, indícalo.
                """.formatted(String.join("\n\n", context), question);

        String response = chatModel.call(new Prompt(prompt,
                OllamaOptions.builder()
                        .model("gemma3")
                        .temperature(0.7)
                        .build())).getResult().getOutput().getText();
        return Map.of("response", response);
    }
}
