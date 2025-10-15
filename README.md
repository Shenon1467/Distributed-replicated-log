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
| ClientInterface | Allows the user to send commands and queries to the distributed log cluster. |
| AppendEntries | Defines the RPC message used by the leader to replicate log entries and maintain heartbeat with followers.|
| AppendEntriesResponse | Represents follower responses indicating success or failure of log replication requests. |
| RequestVote | Models the vote request message sent by candidates during leader election. |
| RequestVoteResponse |	Encapsulates the follower’s response to a vote request, granting or denying the vote. |
| MessageClient | Sends requests (log entries, heartbeats, votes) between nodes. |
| MessageServer | Handles incoming RPC requests (AppendEntries, RequestVote, etc.) for each node. |
| ElectionManager | Manages leader election timeouts and vote requests. |
| LogEntry |	Represents a single command entry in the replicated log, including term and command data. |
| NodeRole |	Enum defining possible node states — FOLLOWER, CANDIDATE, and LEADER. |
| NodeState | Maintains current term, log entries, commit index, and persistent data. |
| NodeTimers | Handles periodic heartbeat and election timers. |
| Main | Launches and initializes the Raft nodes, setting up networking and timers to start the cluster. |
| State.json | Stores critical node meta data (current term, voted for)
| Log.json | Stores the replicated entries)
| Manual_log.txt | For manual inspection or debugging of log entries

---

## File Structure
```
Distributed-Replicated-Log/
├── data/ 
│ ├── Node5001
│ │ ├── log.json
│ │ ├── manual_log.txt
│ │ ├── state.json 
│ ├── Node5002
│ │ ├── log.json
│ │ ├── manual_log.txt
│ │ ├── state.json 
│ ├── Node5003
│ │ ├── log.json
│ │ ├── manual_log.txt
│ │ └── state.json
├── src/
│ ├── main/java/com/distributedlog/
│ │ ├── client 
│ │ │ ├── ClientInterface.java
│ │ ├── messages
│ │ │ ├── AppendEntries.java
│ │ │ ├── AppendEntriesResponse.java
│ │ │ ├── RequestVote.java
│ │ │ ├── RequestVoteResponse.java
│ │ ├── network
│ │ │ ├── MessageServer.java
│ │ │ ├── MessageClient.java
│ │ ├── node
│ │ │ ├── ElectionManager.java
│ │ │ ├── LogEntry.java
│ │ │ ├── NodeRole.java
│ │ │ ├── NodeState.java
│ │ └── NodeTimers.java
│ │ ├── Main.java 
├── README.md 
├── pom.xml 
└── .gitignore

```
---

## How to Run

### Option 1 — Run with IntelliJ IDEA
1. Open the project in IntelliJ.
2. Build the project.
3. Run Main.java (it starts all nodes automatically on ports 5001–5003).
4. Run ClientInterface.java separately to send client commands.

### Option 2 — Run via Command Line (Maven)
```in bash
mvn clean package
java -jar target/DistributedReplicatedLog-jar-with-deps.jar
```
