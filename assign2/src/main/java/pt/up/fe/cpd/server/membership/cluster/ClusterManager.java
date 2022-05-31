package pt.up.fe.cpd.server.membership.cluster;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;

public interface ClusterManager {
    void registerJoinNode(NodeInfo info, int membershipCounter);
    void addLogEntry(MembershipLogEntry logEntry);
    void addNode(NodeInfo info);
    void registerLeaveNode(NodeInfo info, int membershipCounter);
}
