# Caching Patterns & Performance Optimization

In a high-frequency trading signal system, database performance is critical. If every signal ingestion requires a database look-up, the system will eventually bottleneck.

## 1. The Cache-Aside Pattern (Lazy Loading)

Instead of querying PostgreSQL for every request, we use Redis as a high-speed middle layer.

### Workflow:
1.  **Request:** A signal arrives with a `botId`.
2.  **Cache Hit:** The system checks Redis for `bot:secret:{botId}`. If found, it returns the secret in <1ms.
3.  **Cache Miss:** If not found in Redis:
    *   Query PostgreSQL (expensive).
    *   Store the result in Redis with a **TTL** (Time To Live), e.g., 24 hours.
    *   Return the secret to the caller.

### Why this is better:
-   **Reduced DB Load:** Reduces SQL queries by ~99% for active bots.
-   **Low Latency:** RAM access is significantly faster than Disk I/O.
-   **Scalability:** Allows the system to handle thousands of signals per second.

## 2. The Cache Invalidation Trap

Caching introduces the risk of "Stale Data."

### The Problem:
If a user updates their `Bot_Secret` in the Web Dashboard, the Database is updated, but Redis still holds the **old** secret for the next 24 hours (until the TTL expires).
-   **Risk:** Legitimate bot requests will be rejected (Invalid Signature).
-   **Security Risk:** If a key was compromised and then changed, the hacker could still use the old key stored in the cache.

### The Solution:
Whenever an "Update Secret" event occurs in the system:
1.  Update the Database.
2.  **IMMEDIATELY** delete the corresponding key from Redis (`redis.delete("bot:secret:{botId}")`).
3.  The next request will trigger a "Cache Miss," pulling the fresh secret from the DB.

## 3. Implementation Blueprint (DDD Perspective)

The caching logic should be hidden inside the **Infrastructure Layer**. The **API** and **Application** layers should not care whether the data is coming from a cache or a database.

```java
public class BotSecretProviderImpl implements BotSecretProvider {
    // Hidden logic: checks Redis, then DB, then populates Redis.
}
```
