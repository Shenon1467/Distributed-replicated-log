## **CONCURRENCY_ANALYSIS.md**

```markdown
# Concurrency and Consistency Analysis

## 1. Shared State
Each node maintains a shared `NodeState` object containing:
- `currentTerm`
- `votedFor`
- `logEntries`
- `commitIndex`
- `leaderId`

This state is accessed by multiple threads:
- RPC handling threads (MessageServer)
- Election and heartbeat timers
- Client command handler

---

## 2. Synchronization Mechanisms
To ensure thread-safety:
- Methods that modify shared state (e.g., `appendEntry()`, `updateTerm()`, `persistState()`) are marked `synchronized`.
- Timers (Election and Heartbeat) run on independent threads but access shared data via synchronized calls.
- Persistence operations (writing to JSON files) occur atomically to prevent race conditions.

---

## 3. Consistency Properties
- **Leader Completeness:** Only the leader commits new log entries.
- **Log Matching:** If two entries share the same index and term, their preceding entries are identical.
- **State Persistence:** Logs and state are saved to disk before acknowledgment to clients.

---

## 4. Concurrency Risks
Potential race conditions may occur if:
- Two timers trigger elections simultaneously.
- A node crashes during a file write.
These are minimized via synchronization and exception handling.

---

## 5. Safety Guarantees
- No two leaders can exist simultaneously in the same term.
- Once an entry is committed, it remains committed.
- Restarting a node never violates previously committed state.
