package com.distributedlog.messages;

public class RequestVote {
    public int term;
    public String candidateId;

    public RequestVote(int term, String candidateId) {
        this.term = term;
        this.candidateId = candidateId;
    }

    public int getTerm() {
        return term;
    }
    public String getCandidateId() {
        return candidateId;
    }
}
