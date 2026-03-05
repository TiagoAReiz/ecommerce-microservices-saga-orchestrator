---
name: hexagonal-architecture-folders
description: Creates a standard hexagonal architecture folder structure for a Java microservice.
---

# Hexagonal Architecture Folders

This skill helps you create a standard hexagonal architecture (Ports and Adapters) folder structure for Java/Spring Boot microservices.

## Directory Structure

The standard structure to be created inside the base package (`src/main/java/your/base/package/`) is:

- `application/`
  - `services/`: Implementation of the use cases.
  - `mappers/`: Objects to map between entities and DTOs/infrastructure boundaries.
  - `ports/`
    - `in/`
      - `usecases/`: Interfaces that define what the application does (inbound boundaries).
    - `out/`
      - `repositories/`: Interfaces that define what the application needs from the outside (outbound boundaries).
- `core/`
  - `entities/`: Domain models and business rules.
  - `exceptions/`: Domain-specific exceptions.
- `infrastructure/`
  - `adapters/`
    - `in/`
      - `controllers/`: REST controllers, gRPC endpoints, etc.
        - `dtos/`: Data Transfer Objects for requests/responses.
    - `out/`
      - `repositories/`: Implementations of the outbound port interfaces (e.g., JPA repositories, external API clients).
      - `entities/`: Infrastructure-specific entities (e.g., JPA entities, DTOs for external services).
  - `config/`
    - `security/`: Security configurations, filters, etc.

## Instructions
When the user asks to create or apply the hexagonal architecture structure to a microservice, you should:

1. Identify the base package path for the microservice (e.g., `src/main/java/com/example/service`).
2. Use the `run_command` tool (e.g., PowerShell `New-Item -ItemType Directory -Force ...`) or the appropriate filesystem tools to create the standard directories inside that base package.
3. Inform the user that the structure has been created according to the standard.
