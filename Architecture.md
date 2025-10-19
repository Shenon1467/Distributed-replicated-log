## **ARCHITECTURE.md**

markdown
# Architectural Design Document

## 1. System architecture overview

The project implements a simplified replicated log system based on RAFT consensus algorithm which is designed to maintain consistent history across multiple nodes. Each node run separately and communicates through socket based RPC messages using JSON.

---

## 2. Key Components

| Component | Responsibility |
|------------|----------------|
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

## 3. Node lifecycles and state transitions

Each node goes through cycle of three main roles:

1. Leader - This role manages the replication of log entries to followers and sends regular heartbeats to maintain authority. Also it accepts the clients commands and commits the commands once confirmed by majority of nodes.
2. Follower - This role responds to leader messages and vote requests while also acting as a passive role. It converts to a candidate within the election timeout if it does not receive heartbeats from the leader.
3. Candidate - This role initiates an election by sending vote requests and incrementing its term. If the role receives majority of votes it becomes and leader and if it receives less votes or another node becomes a leader it reverts back to a follower.

---

## Message flow between nodes
Below is a simplified messages exchange pattern during the RAFT operation

[Client] --> (Send Command) --> [Leader]

[Leader] --> (Appendentries RPC) --> [Followers]

[Followers] --> (Appendentries) --> [Leader]

[Leader] --> (Commit log+acknowledge) --> [Client]

---

### Leader election flow

[Follower] --> (Election timeout) --> Becomes and transitions [Candidate] --> Sends (Requesvote RPC) --> [All nodes]

[Other nodes] --> (Requestvoteresponse) 

If majority votes granted and received --> Become [Leader]

---

### Hertbeat flow

[Leader] --> (Appendentries with empty data periodically) --> [Follower]

If the follower does not receive logs or heartbeats --> Starts new election

---

## Component interaction diagram (conceptual diagram)

+-----------------------------------+

Client interface

- Send commands
- Get all nodes information
- Get leader information
- Exit

+------------------+----------------+

                    |
                    |
                    v
+-----------------------------------+

Leader node

- Handles the client commands
- Send Appendentries RPCs
- Maintains replicated log

+------------------+----------------+

                |
        +-------+--------+
        v                v

+--------------------+      +--------------------+

| Follower 1 |                    | Follower 2 |

|---------------------|      |--------------------|

|Receive logs|                  |Receive logs|

|RPCs|                          |RPCs|

|Send msg status|               |Send msg status|

|Response|                      |Response|

+--------------------+      +--------------------+

## Persistence and recovery mechanis,

1. Each node persist with three files:
- log.json - stores and maintains all log entries

Eg: [
  {"term": 14, "command": "hi.."},
  {"term": 14, "command": "hi..."}
  ]
- manual_log.txt - manual log text in human readable form for inspection
- state.json - shows the current term, voted for and commited index.

2. During the restart nodes read the last log entries from the log.json file and reads the known term, voted for and commited index from the state.json file.
3. After election candidate resumes as a follower with updated state to maintain consistency.

---

## Concurrency and synchronization

Node runs multiple threads:
- RPC listener threads handles incoming requests.
- Election timer thread triggers new election when timeouts.
- Heartbeat timer thread helps maintain the leader authority.

Synchronized methods are used to avoid the race conditions in critical situations like updating term, log entries or voting state.
Detailed handling of concurrency is explained in Concurrency_analysis.md

## Date flow summary

| Step | Action                                       | Component              |
|------|----------------------------------------------|------------------------|
| 1    | Client sends commands                        | Client interface       |
| 2    | The leader appends commands to the local log | Appendentries          |
| 3    | Leader replicates logs to followers          | Appendentries          |
| 4    | Followers respond back with acknowldgements  | Appendentriiesresponse |
| 5    | Entry commited by leader                     | Nodestate              |
| 6    | Confirmation of command sent to client       | Client interface       |


### Conclusion

In conclusion this architecture ensures:
- Consistency of replicated logs in all nodes.
- Availability during failure of nodes through leader election.
- Durability through persistency and recovery of files.
- Safety through synchronized shared state and handling of controlled messages.

The implemented design reflects on the essential principles of the RAFT algorithm while aligning to the project tasks and expectations.

