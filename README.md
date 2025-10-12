# Distributed Replicated Log System (Raft-based)

This project implements a **simplified distributed replicated log** system inspired by the **Raft consensus algorithm**.  
It maintains consistency among multiple nodes by ensuring all nodes agree on the same sequence of log entries, even in the presence of node failures.

---

## Features

- Leader election among nodes using randomized election timeouts.
- Log replication from the leader to follower nodes.
- Commit acknowledgment and persistence to local storage.
- Persistent state recovery (`state.json`, `log.json`, `manual_log.txt`).
- Client redirection to the current leader when connected to a follower.
- Text-based client interface for appending and querying logs.

---

## System Components

| Component | Description |
|------------|-------------|
| MessageServer | Handles incoming RPC requests (AppendEntries, RequestVote, etc.) for each node. |
| MessageClient | Sends requests (log entries, heartbeats, votes) between nodes. |
| NodeState | Maintains current term, log entries, commit index, and persistent data. |
| ElectionManager | Manages leader election timeouts and vote requests. |
| NodeTimers | Handles periodic heartbeat and election timers. |
| ClientInterface | Allows the user to send commands and queries to the distributed log cluster. |

---

## 🗂️ File Structure


---

## How to Run

### Option 1 — Run with IntelliJ IDEA
1. Open the project in IntelliJ.
2. Build the project.
3. Run Main.java (it starts all nodes automatically on ports 5001–5003).
4. Run ClientInterface.java separately to send client commands.

### Option 2 — Run via Command Line (Maven)
-in bash
mvn clean package
java -jar target/DistributedReplicatedLog-jar-with-deps.jar
