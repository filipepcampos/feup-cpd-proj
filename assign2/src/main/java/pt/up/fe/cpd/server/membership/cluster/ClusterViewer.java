package pt.up.fe.cpd.server.membership.cluster;

import pt.up.fe.cpd.server.membership.ConnectionStatus;

public interface ClusterViewer {
    ConnectionStatus getConnectionStatus();
    String getLogRepresentation();
    String getNodeRepresentation();
    int getNodeCount();
}
