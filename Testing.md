_# Testing Strategy and Results

## 1. Testing overview
The testing overview mainly focus on verifying the log consistency, leader election, client command handling, log replication and fault tolerance under different conditions. This also helps to ensure the system works even when some nodes fail or restart.
The testing was done using Intellij IDE.

---

## 2. Test setup and environment
- Testing done in Intellij IDEA with Maven build system.
- The three nodes running on separate ports in local host sockets where each node listens on different ports.
- Client interface is used to send commands and check the leader status and nodes information.
- Persistence check where the log entries stored in log.json and manual_log.txt files and recording status in state.json file.

---

## 3. Functional test cases

### Test 1: Leader Election
- Start the Main.java where all nodes start and initiate leader election and one becomes leader within few seconds.

Expected outcome: Exactly one node becomes leader; others become followers.

Actual output: Node5001 became leader (term 14). Others followed.

---

### Test 2: Log Replication and client commands
- Sends commands by client while leader accepts it and replicates to all followers. Commands will be committed to all nodes.

Expected outcome: All nodes contain identical entries.

Actual output:  
Eg:- {"term":14,"command":"hi.."},{"term":14,"command":"hi..."}

---

### Test 3: Persistence Recovery
- Stop all the nodes and restart the program.

Expected outcome: Previously committed logs restored.

Actual outcome: Entries successfully reloaded. No data loss is observed.

---

### Test 4: Client Redirection
- The client connects to one of the nodes and sends a command to the node.

Expected outcome: Follower redirects to leader node if node is not leader.

Actual output: Redirection message displayed correctly if the node connected is not the leader.

---

### Test 5: Manual Log Verification
- The logs are restored and stored in the log.json and manual_log.txt file for inspection.

Actual outcome: Persistence correctness validated via JSON logs.

---

### Test 6: Invalid command input
- If the client inputs any unknown commands or wrong commands in the client interface.

Expected outcome: An error message should appear displaying unknown commad.

Actual output: When client inputs unknown commands an error message is displayed.

---

### Test 7: Nodes disconnected or inactive
- If the nodes are inactive or disconnected.

Expected outcome: Display message node couldn't be connected.

Actual output: A message is displayed saying could not connect to node.

## 4. Functional test case summary
| Test                               | Objective                                            | Result |
|------------------------------------|------------------------------------------------------|--------|
| Leader Election                    | Ensurse only one leader per term                     | Passed |
| Log Replication and client commands| Replicates entries consistently                      | Passed |
| Persistence recovery               | No loss of data after restart                        | Passed |
| Client redirection                 | Client's command is redirected back to leader        | Passed |
| Manual Log verification            | Manul_log.txt file is synced                         | Passed |
| Invalid command input              | Displays an error message for unknown commands       | Passed |
| Nodes disconnected or inactive     | Display an error message when nodea are disconnected | Passed |

---

## Reliability and recovery test cases

Leader node failure simulation:
- Identifies leader node failure and starts the election process where another node becomes a leader.

Restart of node:
- Restarted a node after several commands and checked the log entries for automatic syncing from the leader.

Message delay simulation:
- Small sleep delays are added in message handling method to simulate the slow networks and confirmed that the system still maintained consistency.

---

## Log consistency and verification

Tested the logs by opening the log.json file in all nodes and compared the logs. The specific term and the commands were matched of all nodes.
All the nodes showed identical entries after replication and recovery.
Eg: "term":14,"command":"hi..."

## Conclusion
In conclusion with the testing that was done it has confirmed that the system maintains data consistency across all nodes, smooth leader election and recovery and the project meets the intended functional requirement of a replicated log system using a simplified RAFT logic._  