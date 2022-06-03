package pt.up.fe.cpd.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileTransfer {
    public static boolean transfer(DataInputStream input, DataOutputStream output){
        int count;
        byte[] buffer = new byte[4096];
        boolean written = false;

        try {
            while((count = input.read(buffer)) > 0){
                output.write(buffer, 0, count);
                written = true;
            }
        } catch(IOException e){
            return false;
        }
        return written;
    }
}
