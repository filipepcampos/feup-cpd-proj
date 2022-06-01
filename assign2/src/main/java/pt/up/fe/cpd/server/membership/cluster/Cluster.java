package pt.up.fe.cpd.server.membership.cluster;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.Connection;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.log.MembershipLog;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;

import java.util.TreeSet;
import java.util.stream.Collectors;

public class Cluster implements ClusterManager, ClusterViewer{
    protected final Connection connection;
    protected final TreeSet<NodeInfo> nodeSet;
    protected final MembershipLog log;

    public Cluster(){
        this.nodeSet = new TreeSet<>();
        this.log = new MembershipLog();
        this.connection = new Connection();
    }

    public Connection getConnection(){
        return this.connection;
    }

    @Override
    public void registerJoinNode(NodeInfo info, int membershipCounter){
        log.addEntry(new MembershipLogEntry(info.getAddress(), info.getPort(), membershipCounter));
        nodeSet.add(info);
    }

    @Override
    public boolean addLogEntry(MembershipLogEntry logEntry){
        return log.addEntry(logEntry);
    }

    @Override
    public void addNode(NodeInfo info) {
        nodeSet.add(info);
    }

    @Override
    public void registerLeaveNode(NodeInfo info, int membershipCounter){
        log.addEntry(new MembershipLogEntry(info.getAddress(), info.getPort(), membershipCounter));
        nodeSet.remove(info);
    }

    @Override
    public ConnectionStatus getConnectionStatus(){
        return this.connection.getStatus();
    }

    @Override
    public String getLogRepresentation(){
        return log.toString();
    }

    @Override
    public String getNodeRepresentation(){
        return nodeSet.stream().map(NodeInfo::toString).collect(Collectors.joining(", "));
    }

    @Override
    public int getNodeCount() {
        return nodeSet.size();
    }
}
