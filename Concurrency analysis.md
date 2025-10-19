## CONCURRENCY_ANALYSIS.md

```markdown
# Concurrency and Consistency Analysis

1. Shared State and consistency sources

Each node in this project runs multiple concurrent threads:
- RPC handling threads from MessageServer.java- handle incoming appenentries and requestvote requests.
- Electimer in Nodetimers.java- this periodically checks for leader timeouts.
- Heartbeat timers - send regular appendentries heartbeats to followers.
- Client Interface - forwards client commands to the current term leader.

All of these threads access and update the shared state variables such as:
- currentTerm
- votedFor
- logEntries
- commitIndex
- leaderId
-state (the node whether it is leader, follower or candidate)

These shared data is encapsulated in the NodeState.java object useed by each node.

---

2. Synchronization Mechanisms

To ensure thread-safety and prevent inconsistent updates Synchronized methods, atomic files writes and timer isolation is used.
- Synchronized methods:
Core operations such as appendentry, updateTerm, persistentState, and becomeLeader are declared as synchronized, ensuring that only one thread at a time can modify the shared nodestate.
Eg:- public synchronized void appendentry{
            logEntries.add;()
            persistLog();}

- Atomic File writes:
When updating log entries to log.json, the file is written in a single atominc operation using the buffered I/O while preventing partial write if a node crashes in mid opreations.

- Timer Isolation:
Election and the heartbeat timers run in seperate threads but interact with the shared state only through synchronized methods which avoids timing conflicts between heartbeats and election triggers.

---

3. Realtime concurrency scenarios and handling

Scenario 1: Two nodes start election simultaneously
The nodes mught increment their term and send RquestVoteRPCs at nearly the same time.

Mitigation: The updateterm() method is synchrinized and a node will not vote twice in the same term where the candidate who first collects a majority becomes the leader while the others revert back to follower upon receiving a higher term.

Scenario2: Log replication and client commands overlap
The leader might also recieve client command send<log> while its also replicationg the earlier entries to the followers

Mitigation: appenEntry() is synchronized ensuring the log updates are serialized and the commitIndex update happens only after the replication confirmed from a majority, guaranteeing consistency.

Scenario 3: Node crashes during the log writes
If a node crashes in the middle of the log write, it could leave an incomplete log.

Mitigation: the persistence logic writes to a temporary file first and then renames it automatically to log.json ensuring valid data on restart of the program.

---

4. Consistency properties

- Leader Completeness: 
    Only the current leader can commits new log entries and the follower nodes reject appendentries if the term is outdated.
    
- Log Matching: 
    If two entries share the same index and term, all preceding entries are guaranteed to match while it verifies appendentries consistency check.
    
- State Persistence, durability and recovery: 
    Logs and state are saved to disk before acknowledgment to clients and upon the restart nodes read the log.json to restore the committed state.

---

5. Safety guarantees summary

| Properties | Gurantee | Mechanism |
|------------|----------|-----------|
| Leader unique | Only one leader per tearm | Has synchronized voting and term based election |
| Commit sustainability | When once commited, never undone | Majority based commit rule |
| Log consistence | The matching entries remain identical | Appendentries consistency check |
| Thread Safety | No concurrent modifications | Java synchronized and controlled timer |
| Crash safety | No half writes | Atomic file persistence |

---

6. Conclusion

In conclusion the concurrency model in the Raft implementation carefully combines synchronized state management, isolated timers and persistence whil ensuring mutual exclusion at key point and validating terms and log indices across the RPC. The system mainatains strong consistency and fault tolerance even under any network delays or node failures.

