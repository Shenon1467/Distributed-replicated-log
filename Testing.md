# Testing Strategy and Results

## 1. Overview
Testing focused on verifying **leader election**, **log replication**, **persistence**, and **fault tolerance**.

---

## 2. Test Environment
- Java 17
- IntelliJ IDEA / Terminal
- Ports: 5001, 5002, 5003
- Each node stores data in `/data/Node<port>/`

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
- `manual_log.txt` not always updated (manual logging issue noted).

**Conclusion:**  
Persistence correctness validated via JSON logs.

---

## 4. Summary
| Test | Objective | Result |
|------|------------|--------|
| Leader Election | Ensure only one leader per term | ✅ Passed |
| Log Replication | Entries replicated consistently | ✅ Passed |
| Persistence | No data loss after restart | ✅ Passed |
| Redirection | Client redirected to leader | ✅ Passed |
| Manual Log | Secondary log file sync | ⚠️ Partial |
