# Learning & Architectural Journal

This file tracks the "Why" behind the technical decisions made in this project. Use this for your Thesis (Chương 3 & 4).

## 2026-03-16: API Security & Signal Ingestion

### 1. Clean Architecture implementation
*   **Concept:** Applied "Dependency Inversion".
*   **Why:** To keep the `Application` and `Domain` pure. We created a `SignalRepository` interface in the Domain and implemented an `InMemory` version in Infrastructure. This allows testing the logic without Kafka.
*   **Keywords:** Ports and Adapters, Inward Dependency.

### 2. HMAC Security (The "Dumb Bot")
*   **Concept:** Used HMAC-SHA256 for request signing.
*   **Why:** Simple API Keys (like `X-Bot-Api-Key`) are vulnerable if intercepted. Signing a `Timestamp + Payload` ensures:
    *   **Integrity:** The price or botId cannot be changed in transit.
    *   **Authenticity:** Only the bot with the secret key can generate a valid signature.
    *   **Replay Protection:** The timestamp ensures an old request cannot be resent 10 minutes later.
*   **Keywords:** HMAC-SHA256, Request Signing, Digital Signature.

### 3. Redis Idempotency & Race Conditions
*   **Problem:** Sequential `GET` then `SET` leads to Race Conditions where duplicate signals can bypass security during high-frequency volatility.
*   **Solution:** Atomic `setIfAbsent` (SETNX). This combines check and store into a single operation at the Redis engine level.
*   **Architectural Choice:** "Fail Closed". If Redis is down, the system rejects signals. Integrity and Account Safety > System Availability in high-stakes trading.
*   **Keywords:** Race Condition, Atomic Operation, Idempotency, Fail Closed vs. Fail Open.

### 4. Modular Monolith Organization
*   **Decision:** Grouped code by **Functional Layers** (marcus-api, marcus-application, etc.) rather than technical layers.
*   **Benefit:** Easier to split into Microservices later if the number of bots grows too large.

### 5. Performance Optimization: Cache-Aside Pattern
*   **Problem:** Querying the DB for every Signal to retrieve the `BotSecret` is an I/O bottleneck.
*   **Solution:** Use Redis as a caching layer. The system checks Redis first (Cache Hit). If null, it queries PostgreSQL and populates Redis with a TTL (Cache Miss). 
*   **Trap:** Cache Invalidation. If a secret is updated in the DB, it MUST be evicted from Redis immediately to prevent using stale, compromised keys.
*   **Keywords:** Cache-Aside, TTL, Cache Invalidation, Lazy Loading.
