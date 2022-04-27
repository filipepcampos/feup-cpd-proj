package pt.up.fe.cpd;

public class Store extends Node implements KeyValueStore {

    public Store(String nodeId, String storagePort) {
        super(nodeId, storagePort);
    }

    @Override
    public String put(String key, byte[] value) {
        return null;
    }

    @Override
    public byte[] get(String key) {
        return new byte[0];
    }

    @Override
    public void delete(String key) {

    }
}
