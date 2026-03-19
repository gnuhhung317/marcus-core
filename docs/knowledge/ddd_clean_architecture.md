# Coding with DDD & Clean Architecture

Following the **"Inside-Out"** rule:

## 1. Domain (The Core)
*   **Entity:** `Signal.java`
*   **Port (Interface):** `SignalRepository.java`

## 2. Application (The Orchestrator)
*   **UseCase:** `CaptureSignalUseCase.java`

## 3. Infrastructure (The Implementation)
*   **Adapter:** `KafkaSignalProducer.java` or `InMemorySignalMessagingAdapter.java`

## 4. API (The Entry Point)
*   **Controller:** `SignalController.java`

---

## Dependency Inversion
The **Application** layer defines the interface it needs. The **Infrastructure** layer provides the implementation. This means if you change Kafka to RabbitMQ, you don't change your business logic.
