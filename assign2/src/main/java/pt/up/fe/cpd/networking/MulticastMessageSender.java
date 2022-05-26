package pt.up.fe.cpd.networking;

import java.io.IOException;
import java.net.*;

public class MulticastMessageSender {
    protected InetAddress address;
    protected int port;

    public MulticastMessageSender(InetAddress address, int port){
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void send(byte[] data) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(address);

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        socket.close();
    }
}

