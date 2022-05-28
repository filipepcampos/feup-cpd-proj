package pt.up.fe.cpd.server.membership.cluster;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.Pair;

public class SearchableCluster extends Cluster implements ClusterSearcher{
    private final ActiveNodeInfo activeNode;

    public SearchableCluster(ActiveNodeInfo activeNode) {
        super();
        this.activeNode = activeNode;
    }

    @Override
    public NodeInfo findNodeByKey(byte[] key){ // TODO: This name is horrible
        NodeInfo item =  nodeSet.higher(new NodeInfo(key));
        if(item == null){ // Circular node representation
            return nodeSet.first();
        }
        return item;
    }

    @Override
    public Pair<NodeInfo, NodeInfo> findTwoClosestNodes(NodeInfo nodeInfo){
        NodeInfo lower = nodeSet.lower(nodeInfo);
        NodeInfo higher = nodeSet.higher(nodeInfo);
        if(lower == null){
            lower = nodeSet.last();
        }
        if(higher == null){
            higher = nodeSet.first();
        }
        return new Pair<>(lower, higher);
    }

    @Override
    public boolean isActiveNode(NodeInfo nodeInfo) {
        return nodeInfo.equals((NodeInfo) activeNode);
    }
}
