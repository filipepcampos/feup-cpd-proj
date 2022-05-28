package pt.up.fe.cpd.server;

import java.util.TreeSet;
import pt.up.fe.cpd.utils.Pair;

public class NodeSearcher {
    final private TreeSet<NodeInfo> nodeSet;

    public NodeSearcher(TreeSet<NodeInfo> nodeSet){
        this.nodeSet = nodeSet;
    }

    public NodeInfo findNodeByKey(byte[] key){ // TODO: This name is horrible
        NodeInfo item =  nodeSet.higher(new NodeInfo(key));
        if(item == null){ // Circular node representation
            return nodeSet.first();
        }
        return item;
    }

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
}
