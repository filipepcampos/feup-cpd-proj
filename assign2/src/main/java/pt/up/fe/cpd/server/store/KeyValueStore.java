package pt.up.fe.cpd.server.store;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface KeyValueStore {
    /**
     * Adds a key-value pair to the store
     * @param key key that will be used to generate the encoded key
     * @param data value that will be stored associated to the key
     * @return sha-256 encoded key
     */
    public void put(String key, DataInputStream data);

    /**
     * Retrieves the value bounded to the key
     * @param key sha-256 encoded key
     * @param data value that will be returned associated to the key
     * @return value associated to the given key
     */
    public void get(String key, DataOutputStream dat);

    /**
     * Deletes a key-value pair
     * @param key sha-256 encoded key
     */
    public void delete(String key);
}
