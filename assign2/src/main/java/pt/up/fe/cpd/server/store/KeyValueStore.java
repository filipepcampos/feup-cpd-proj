package pt.up.fe.cpd.server.store;

public interface KeyValueStore {
    /**
     * Adds a key-value pair to the store
     * @param key key that will be used to generate the encoded key
     * @param value value that will be stored associated to the key
     * @return sha-256 encoded key
     */
    public String put(String key, byte[] value);

    /**
     * Retrieves the value bounded to the key
     * @param key sha-256 encoded key
     * @return value associated to the given key
     */
    public byte[] get(String key);

    /**
     * Deletes a key-value pair
     * @param key sha-256 encoded key
     */
    public void delete(String key);
}
