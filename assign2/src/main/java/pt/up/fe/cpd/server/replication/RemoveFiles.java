package pt.up.fe.cpd.server.replication;

public class RemoveFiles implements Runnable {
    private final byte[] lowestKey;
    private final byte[] highestKey;

    public RemoveFiles(byte[] lowestKey, byte[] highestKey){
        this.lowestKey = lowestKey;
        this.highestKey = highestKey;
    }

    @Override
    public void run(){
        
    }
}
