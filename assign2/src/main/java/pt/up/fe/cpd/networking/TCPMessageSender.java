package pt.up.fe.cpd.networking;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.net.*;

public abstract class TCPMessageSender {
    protected InetAddress address;
    protected int port;

    public TCPMessageSender(InetAddress address, int port){
        this.address = address;
        this.port = port;
    }

    protected void send(byte[] data) throws IOException {
        Socket socket = new Socket(address, port);
        OutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        outputStream.write(data);
        outputStream.flush();
        socket.close();
    }
}
