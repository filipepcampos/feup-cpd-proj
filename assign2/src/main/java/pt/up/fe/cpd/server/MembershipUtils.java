package pt.up.fe.cpd.server;

public class MembershipUtils {
    public static String parseNodeId(byte[] nodeId) {
        StringBuilder result = new StringBuilder();
        for (byte b : nodeId) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
