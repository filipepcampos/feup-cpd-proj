package pt.up.fe.cpd.server.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

import pt.up.fe.cpd.server.store.KeyValueStore;

public class StoreOperationHandler implements Runnable {
    final private KeyValueStore keyValueStore;
    final private Socket socket;
    
    public StoreOperationHandler(KeyValueStore keyValueStore, Socket socket) {
        this.keyValueStore = keyValueStore;
        this.socket = socket;
    }

    @Override
    public void run() {        
        try {
            System.out.println("[StoreOperationHandler] opened");
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            String header = inputStream.readLine();
            inputStream.close();
            String[] splitHeader = header.split(" ");
            String operation = splitHeader[0];
            String key = splitHeader[1];

            switch(operation){
                case "GET":
                    keyValueStore.get(key, new DataOutputStream(socket.getOutputStream()));
                    break;
                case "DELETE":
                    keyValueStore.delete(key);
                    break;
                case "PUT":
                    keyValueStore.put(key, new DataInputStream(socket.getInputStream()));
                    break;
            }
            
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
