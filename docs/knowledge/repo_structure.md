# Ideal Repository Structure

```text
signal-core-backend/
├── marcus-domain/      # Business logic, Entities, Interfaces (Ports)
├── marcus-application/ # Use Cases, Service logic, Orchestration
├── marcus-infrastructure/# Persistence (DB), Messaging (Kafka), Adapters
└── marcus-api/         # REST Controllers, Security Filters, WebSockets
```
