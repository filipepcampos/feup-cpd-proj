package pt.up.fe.cpd.server.tasks;

import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.net.*;
import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.store.KeyValueStore;

public class StoreOperationListener implements Runnable {
    private final KeyValueStore keyValueStore;
    private final TCPListener listener;
    private final ExecutorService executor;

    public StoreOperationListener(KeyValueStore keyValueStore, TCPListener listener, ExecutorService executor){
        this.keyValueStore = keyValueStore;
        this.executor = executor;
        this.listener = listener;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Socket socket = listener.accept();
                System.out.println("[debug, storeoperationlistener] accepted");
                executor.execute(new StoreOperationHandler(keyValueStore, socket));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
