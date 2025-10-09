package com.distributedlog.messages;

public class RequestVoteResponse {
    private int term;
    private boolean voteGranted;

    public RequestVoteResponse(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() { return term; }
    public boolean isVoteGranted() { return voteGranted; }
}
