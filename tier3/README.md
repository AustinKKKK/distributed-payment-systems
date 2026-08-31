# Distributed Payment Processing — Tier 3

Tier 2 solved the write path: accepting a payment no longer waits on slow processing. This tier is about the read path — `GET /accounts/{id}/balance` — which Tier 1/2 never had to deal with, since balance was never queried under load until now.

![Tier 3 architecture](./architecture.svg)

**Status: caching for the read path is done and measured. Read replicas, replication lag, and circuit breakers (the rest of the original Tier 3 scope) are not yet started.**

## Why this endpoint, and why it's a different kind of bottleneck

Every prior tier's bottleneck was about *concurrency* — how many requests can be in flight at once (connection pool size, consumer parallelism). This one is different: it's about *how much work a single query does*, and that work grows with data volume.

Balance was never stored as a column — the schema was designed from Tier 1 to derive it:

```sql
SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE account_id = ?
```

`COALESCE` guards the case where an account has no ledger history — `SUM` returns `NULL` on an empty set, not `0`.

An index on `account_id` makes finding the relevant rows fast, but it cannot make *summing* them free. Every row that matches has to be added, one at a time. Writes stay flat-rate regardless of table size (an `INSERT` is always an `INSERT`), but a read like this gets slower as more history accumulates for that account — the exact opposite scaling behavior from what Tier 1/2 optimized for.

## Measured: the uncached cost

100,000 payments were routed through a single account pair (`account_id=1 ↔ 2`) to build up ledger history, then `GET /accounts/1/balance` was hit with 200 concurrent VUs, 20,000 iterations — every request summing the same 100K rows.

| | Throughput | p50 | p95 | Failures |
|---|---|---|---|---|
| Uncached `SUM` over 100K rows | 347 req/s | 569ms | 709ms | 0% |

For comparison, a simple indexed point lookup (`Payment` status by ID, Tier 1/2) runs at 8,000–9,000 req/s with p95 under 150ms. This is a ~20–25x throughput drop purely from replacing "find one row" with "sum many rows" — no concurrency issue involved, nothing failed, it's just doing 100,000 additions on every single request.

## Fix: cache the result, evict on write

```java
@Cacheable(value = "balance", key = "#accountId")
@Transactional(readOnly = true)
public Long getBalance(Long accountId) {
    return ledgerRepository.getBalance(accountId);
}
```

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Caching(evict = {
        @CacheEvict(value = "balance", key = "#fromAccount"),
        @CacheEvict(value = "balance", key = "#toAccount")
})
public Payment processLedgerAndFinish(Payment payment, Long fromAccount, Long toAccount, Long amount) {
    // ledger writes, status transition — unchanged from Tier 2
}
```

**The write side only ever deletes, never writes a new value into the cache.** Recomputing the sum and pushing it into the cache from the write path would mean doing that expensive `SUM` on every payment regardless of whether anyone's about to read it — wasted work — and if two payments hit the same account close together, whichever finishes last would "win" and could leave a stale value in place, a subtler version of the same check-then-act race covered in Tier 1. Deleting is idempotent: it doesn't matter how many writers evict the same key or in what order, the outcome is always "not cached." The next reader recomputes it once, lazily, and refills the cache. This is the standard cache-aside pattern, and the reason it's evict-only rather than update-in-place.

**Caveat confirmed by accident, not by design:** clearing the underlying table with `TRUNCATE` during testing left the Redis-cached balance untouched — the API kept returning a stale, very wrong number until the cache was flushed manually (`FLUSHALL`). `@CacheEvict` only fires on the code path that goes through `processLedgerAndFinish`; anything that changes the ledger outside the application (manual SQL, a migration, another service writing directly to the table) leaves the cache unaware. This is a real caching risk, not a hypothetical one — it happened in this project within the first hour of adding the cache.

## Results

| | Throughput | p50 | p95 | Improvement |
|---|---|---|---|---|
| No cache | 347 req/s | 569ms | 709ms | — |
| Cached (`@Cacheable`) | **15,536 req/s** | **6.93ms** | **48.7ms** | **~45x throughput, ~82x p50** |

With 200 concurrent readers hammering the same account, only the first request pays the full `SUM` cost; the remaining 19,999 read a value already sitting in Redis. Postgres barely sees this traffic anymore — the read load that used to hit the database is now almost entirely absorbed by the cache.
