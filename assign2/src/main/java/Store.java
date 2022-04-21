public class Store extends Node implements KeyValueStore {

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
