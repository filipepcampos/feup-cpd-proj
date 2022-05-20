package pt.up.fe.cpd.server;

public class MembershipUtils {
    public static String parseNodeId(byte[] nodeId) {
        StringBuilder result = new StringBuilder();
        for (byte b : nodeId) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    public static byte[] parseNodeIdString(String nodeIdString){
        byte[] nodeId = new byte[32];
        for (int i = 0; i < nodeIdString.length(); i += 2) {
            nodeId[i/2] = (byte) ((Character.digit(nodeIdString.charAt(i), 16) << 4)
                                + Character.digit(nodeIdString.charAt(i+1), 16));
        }
        return nodeId;
    }
}
