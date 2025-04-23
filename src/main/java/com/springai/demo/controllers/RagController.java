package com.springai.demo.controllers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
public class RagController {

    private final EmbeddingModel embeddingModel;
    private final Logger logger = Logger.getLogger(RagController.class.getName());
    private final VectorStore vectorStore;
    private final OllamaChatModel chatModel;
    private final List<String> people = List.of(
            "Rubén Hernández es un hombre que vive en una ciudad grande. Viaja mucho a otras ciudades por trabajo.",
            "Alberto Fuentes es un hombre de mediana edad con mucho dinero. Le gusta el lujo y todo lo que tenga que ver con estar a la moda",
            "Lucía Martínez es una mujer a la que le encanta la aventura. Siempre que puede hace escapadas a los sitios más recónditos e inexplorados.",
            "Celia González es una mujer muy preocupada por el medio ambiente. Siempre que puede intenta hacer algo por el planeta."
    );

    @PostConstruct
    public void initStore() {
        addPeopleContext();
        addVehiculesContext();
    }

    private void addVehiculesContext() {
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
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("embedding", embeddingModel.embed(paragraph));
                    Document doc = new Document(paragraph, metadata);
                    vectorStore.add(List.of(doc));
                }
            }
            logger.info("PDF loaded and indexed into vector store.");
        } catch (Exception e) {
            logger.severe("Failed to load PDF: " + e.getMessage());
        }
    }

    private void addPeopleContext() {
        for (String content : people) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("embedding", embeddingModel.embed(content));
            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));
        }
    }

    @GetMapping("/ai/rag/ask")
    public Map<String, String> ask(@RequestParam("question") String question) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(10)
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
                        .model("gemma3:4B")
                        .temperature(0.7)
                        .build()
        )).getResult().getOutput().getText();
        return Map.of("response", response);
    }
}
