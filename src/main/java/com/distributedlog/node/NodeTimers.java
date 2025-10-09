package com.distributedlog.node;

import java.util.Timer;
import java.util.TimerTask;

public class NodeTimers {
    private final NodeState nodeState;
    private final ElectionManager electionManager;
    private Timer electionTimer;

    public NodeTimers(NodeState nodeState, ElectionManager electionManager) {
        this.nodeState = nodeState;
        this.electionManager = electionManager;
    }

    public void startElectionTimer() {
        electionTimer = new Timer(true);
        resetElectionTimeout();
    }

    public void resetElectionTimeout() {
        if (electionTimer != null) {
            electionTimer.cancel();
            electionTimer = new Timer(true);
        }

        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Election timeout! Node becomes CANDIDATE.");
                electionManager.startElection();
            }
        }, 5000 + (int)(Math.random() * 5000)); // random 5–10s
    }
}
