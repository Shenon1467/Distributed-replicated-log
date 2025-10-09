package com.distributedlog.messages;

public class AppendEntriesResponse {
    private int term;
    private boolean success;
    private int matchIndex; // index of last log entry matched on follower

    public AppendEntriesResponse(int term, boolean success, int matchIndex) {
        this.term = term;
        this.success = success;
        this.matchIndex = matchIndex;
    }

    public int getTerm() { return term; }
    public boolean isSuccess() { return success; }
    public int getMatchIndex() { return matchIndex; }
}
