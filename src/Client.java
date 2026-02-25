import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

// Scans the local network to find machines running the game server on port 9000
public class Client {

    // Stores the IP addresses of any servers found during the scan
    List<String> Servers = new ArrayList<>();

    // Loops through all 254 possible host addresses in the given subnet,
    // attempting a short TCP connection to port 9000 on each one
    public void start(String IP) {
        for (int i = 1; i < 255; i++) {
            try (Socket socket = new Socket() ) {
                String TestIP = IP + "."+ Integer.toString(i);
                socket.connect(new InetSocketAddress(TestIP, 9000), 10); // 10ms timeout
                System.out.println("Working " + TestIP);
                Servers.add(TestIP); // Server found — add to the list
            }
            catch (Exception e) {
                // No server at this address — continue scanning
                System.out.println(IP +"." + Integer.toString(i)+ " is not working");
            }
        }
    }
}