package com.distributedlog.messages;

public class AppendEntries {
    public int term;
    public String leaderId;

    public AppendEntries(int term, String leaderId) {
        this.term = term;
        this.leaderId = leaderId;
    }

    public int getTerm() {
        return term;
    }
    public String getLeaderId() {
        return leaderId;
    }
}
