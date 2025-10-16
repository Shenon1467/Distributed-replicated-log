# Testing Strategy and Results

## 1. Overview
Testing focused on verifying **leader election**, **log replication**, **persistence**, and **fault tolerance**.

---

## 2. Test Environment
- Java 17
- IntelliJ IDEA / Terminal
- Ports: 5001, 5002, 5003
- Each node stores data in `/data/Node<port>/`(Eg:- data/Node5001/)

---

## 3. Test Cases

### Test 1: Leader Election
**Steps:**
- Start main server (starts all nodes).
- Observe leader election logs.

**Expected:**  
Exactly one node becomes leader; others become followers.

**Actual:**  
Node5001 became leader (term 14). Others followed.

---

### Test 2: Log Replication
**Steps:**
- Connect via `ClientInterface`.
- Send:
send x=5
send y=24
- Inspect `log.json` of all nodes.

**Expected:**  
All nodes contain identical entries.

**Actual:**  
[{"term":14,"command":"hi.."},{"term":14,"command":"hi..."}]

---

### Test 3: Persistence Recovery
**Steps:**
- Stop nodes.
- Restart program.
- Inspect `log.json`.

**Expected:**  
Previously committed logs restored.

**Actual:**  
Entries successfully reloaded. No data loss observed.

---

### Test 4: Client Redirection
**Steps:**
- Connect to follower node via ClientInterface.
- Issue a command.

**Expected:**  
Follower redirects to leader node.

**Actual:**  
Redirection message displayed correctly.

---

### Test 5: Manual Log Verification
**Observation:**
- `log.json` updates correctly.
- `manual_log.txt` always updates automatically as txt.

**Conclusion:**  
Persistence correctness validated via JSON logs.

---

### Test 6: Command input
**Observation:**
- If the client input any unknown commands or wrong commands.

**Expected:**
An error message should appear displaying unknown commad.

**Actual:**  
When client inputs unknown commands an error message is displayed.

---

### Test 7: Nodes disconnected or inactive
**Observation:**
- If the nodes are inactive or disconnected.

**Expected:**  
Display message node couldn't be connected.

**Actual:**
A message is displayed displaying could not connect to node.

## 4. Summary
| Test | Objective | Result |
|------|------------|--------|
| Leader Election | Ensure only one leader per term | Passed |
| Log Replication | Entries replicated consistently | Passed |
| Persistence | No data loss after restart | Passed |
| Redirection | Client redirected to leader | Passed |
| Manual Log | Secondary log file sync | Passed |
| Command input | Display error message for unknown commands | Passed |
| Node inactive | Display error message when nodea are disconnected | Passed |
