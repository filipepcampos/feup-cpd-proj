package pt.up.fe.cpd;

public interface ClusterMembership {
    /**
     * Adds a node to the cluster
     * @param node node that will be added to the cluster
     */
    public void join(Node node);

    /**
     * Removes a node from the cluster
     * @param nodeId id of the node that will be removed
     */
    public  void leave(String nodeId);
}
