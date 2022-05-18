package pt.up.fe.cpd.server;

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
}
