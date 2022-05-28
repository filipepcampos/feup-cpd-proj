package pt.up.fe.cpd.server.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

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
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            
            Scanner scanner = new Scanner(dataInputStream);            
            String header = scanner.nextLine();

            String[] splitHeader = header.split(" ");
            String operation = splitHeader[0];
            String key = splitHeader[1];
            
            switch(operation){
                case "GET":
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    keyValueStore.get(key, dataOutputStream);
                    dataOutputStream.close();
                    break;
                case "DELETE":
                    keyValueStore.delete(key);
                    break;
                case "PUT":
                    keyValueStore.put(key, dataInputStream);
                    break;
            }
            
            scanner.close();
            dataInputStream.close();
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
