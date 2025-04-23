# Spring AI Demo Application

This project demonstrates the capabilities of Spring AI, focusing on integrating Large Language Models (LLMs) through Ollama with Spring Boot applications. It showcases different approaches to AI integration, from basic prompts to more advanced Retrieval Augmented Generation (RAG) techniques.

## Overview

The Spring AI Demo Application provides several REST endpoints that demonstrate different ways to interact with AI models:

1. **Basic AI Interaction**: Simple question-answering with default parameters
2. **Parameterized AI Interaction**: Customizable AI responses with adjustable parameters
3. **Retrieval Augmented Generation (RAG)**: Context-aware responses using vector embeddings

## Prerequisites

- Java 21 or higher
- Maven 3.6+ for building the project
- [Ollama](https://ollama.ai/) running locally (default: http://localhost:11434)
- The following Ollama models:
  - `mistral` (default model)
  - `gemma3:4B` (used for RAG)
  - `nomic-embed-text` (used for embeddings)

## Setup and Installation

1. Clone the repository
2. Install and start Ollama on your local machine
3. Pull the required models in Ollama:
   ```
   ollama pull gemma3
   ollama pull gemma3:4B
   ollama pull nomic-embed-text
   ```
4. Build the application:
   ```
   mvn clean install
   ```
5. Run the application:
   ```
   mvn spring-boot:run
   ```

## API Endpoints

### Basic AI Interaction

```
GET /ai/ask?question=<your-question>
```

Parameters:
- `question` (optional): The question to ask the AI (default: "Cuéntame un chiste")

Example:
```
GET /ai/ask?question=What is Spring Boot?
```

### Parameterized AI Interaction

```
GET /ai/videogames/info?videogame=<game-name>&model=<model-name>&temperature=<temperature>
```

Parameters:
- `videogame` (optional): The name of the videogame to describe (default: "The witcher")
- `model` (optional): The AI model to use (default: "gemma3")
- `temperature` (optional): The creativity level (0.0-1.0) (default: 0.4)

Example:
```
GET /ai/videogames/info?videogame=Elden Ring&temperature=0.7
```

### Retrieval Augmented Generation (RAG)

```
GET /ai/rag/ask?question=<your-question>
```

Parameters:
- `question` (required): The question to ask, which will be answered based on the context in the vector store

Example:
```
GET /ai/rag/ask?question=What car has the best fuel efficiency?
```

## Configuration

The application is configured through `application.yaml`:

```yaml
spring:
  application:
    name: demo
  ai:
    ollama:
      base-url: http://localhost:11434
      init:
        pull-model-strategy: always
        timeout: 60s
        max-retries: 1
      embedding:
        model: nomic-embed-text
```

Key configuration options:
- `spring.ai.ollama.base-url`: URL of your Ollama instance
- `spring.ai.ollama.init.pull-model-strategy`: Strategy for pulling models (always, if-not-present, never)
- `spring.ai.ollama.embedding.model`: Model used for generating embeddings

## Vector Store

The application uses an in-memory vector store that is initialized with:
1. Information about fictional people
2. Content extracted from a PDF file about car specifications

This data is used by the RAG endpoint to provide context-aware responses.

## Additional Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/index.html)
- [Ollama Documentation](https://ollama.ai/docs)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

## License

This project is licensed under the terms of the license included in the repository.