~~# Distributed Replicated Log System with simplified consensus

## Project overview
This projects implements a simplified version of the Raft consensus algorithm while focusing on leader election, fault tolerance and leader election across a cluster of nodes. The goal is also ensure that nodes maintain consistency and identical logs even when some nodes fail or restart.

---

## Key Features

- **Three Node roles** - implements the node roles leader, follower and candidate with state transitions.
- **Log replication** - the leader replicates the commands to the followers to ensure consistent log orders across all nodes.
- **Leader election** - nodes elect and chose a leader using a randomized election timeout and vote requests.
- **Fault tolerance** - the system maintains consistency even if one or more nodes fail temporarily.
- **Recovery and persistence** - every node saves logs in log.json file and recovers committed entries when restarted.
- **Client interface** - a simple client interface lets users send commands(logs), inquire node information and inquire leader information.

---

## System Components

| File                       | Description                                                                                                           |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------|
| ClientInterface. java      | Allows the client to send commands and log entries, check nodes information and check leader information dynamically. |
| AppendEntries.java         | Defines the structure for appendentries requests from the leader to followers for log replication.                    |
| AppendEntriesResponse.java | Shows the follower responses to appendentries RPCs by confirming success or failed.                                   |
| RequestVote.java           | This handles the vote requests from candidate nodes during the leader election.                                       |
| RequestVoteResponse.java   | Enapsulate the responses to vote requests with the granted or rejected status.                                        |
| MessageClient.java         | Sends the requests like log entries, heartbeats and votes between the nodes.                                          |
| MessageServer.java         | Handle the incoming RPC requests like appendentries, requestvote for each node.                                       |
| ElectionManager.java       | This manages the leader election timeout and vote requests.                                                           |
| LogEntry.java              | Represent a single command entry in the replicated log with its own and associated term.                              |
| NodeRole.java              | Defines the nodes and the states which are leader, follower and candidate.                                            |
| NodeState.java             | Maintain the current term, log entries, commit index, and persistent data.                                            |
| NodeTimers.java            | Handle the periodic heartbeats and the election timers.                                                               |
| Main.java                  | Starts all the nodes follower, leader and candidate while launching and initializing communication channels.          |
| State.json                 | Stores the nodes metadata like current term and voted for.                                                            
| Log.json                   | Stores the replicated entries of all nodes.                                                                                        
| Manual_log.txt             | For the manual inspection or debugging of log entries.                                                                     

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

## Architecture summary

Each node runs an independent process:
- Leader node - send heartbeats periodically, sends the logs to followers and maintain authority.
- Follower node - responds and replicate the logs and requests and participate in the election process to chose a leader.
- Candidate node - votes for itself or another node in order to chose a leader for the term.
- Nodes communicate over the sockets using json based RPC messages.
- Client interface can connect to any node and automatically redirects to the current leader if necessary.

The detailed architectural diagrams and explanations are available in the Architecture.md file.

---

## Concurrency and consistency

Multiple threads run on each node concurrently:
- Heartbeat timers
- Election timers
- RPC handling 
- Client request and command processing

Detailed concurrency analysis and race conditions are explained in Concurrency Analysis.md

---

## Testing summary

The project implements three nodes and was tested (ports 5001, 5002, 5003):
- Ensures log replication in all nodes.
- Stores log in log.json and uses data when restarting program.
- Verified leader election
- Simulates mode failure and recovery to confirm persistence and fault tolerance.
- Redirects client to leader if necessary.
- Commits client commands to leader.

Test scenarios and results are explained in detail in the Testing.md file.

---

## How to Run

## Option 1 — Run with IntelliJ IDEA
1. Open the project in IntelliJ.
2. Build the project.
3. Run Main.java (it starts all nodes automatically on ports 5001–5003 and initiates the program).
4. Run the ClientInterface.java separately to send client commands and for nodes query.

### Option 2 — Run project via Command Line (Terminal)

1. Compile the project: mvn clean compile
2. Run the main file which starts all nodes: mvn exec:java -Dexec.mainClass="com.distributedlog.Main"
3. Run client interface: java -cp "out/production/DistributedReplicatedLog" com.distributedlog.client.ClentInterface~~
