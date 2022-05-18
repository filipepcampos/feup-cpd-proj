package pt.up.fe.cpd.server;

import java.util.Arrays;

public class MembershipLogEntry {
    byte[] nodeId;
    int membershipCounter;

    public MembershipLogEntry(byte[] nodeId, int membershipCounter){
        this.nodeId = nodeId;
        this.membershipCounter = membershipCounter;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public int getMembershipCounter() {
        return membershipCounter;
    }

    @Override
    public String toString(){
        return MembershipUtils.parseNodeId(this.nodeId) + " " + this.membershipCounter;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MembershipLogEntry)) return false;
        MembershipLogEntry entry = (MembershipLogEntry) obj;
        return Arrays.equals(this.nodeId, entry.getNodeId()) && entry.getMembershipCounter() == this.membershipCounter;
    }
}
