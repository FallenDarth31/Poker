import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

public class Client {

    List<String> Servers = new ArrayList<>();


    public void start(String IP) {
        for (int i = 1; i < 255; i++) {
            try (Socket socket = new Socket() ) {
                String TestIP = IP + "."+ Integer.toString(i);
                socket.connect(new InetSocketAddress(TestIP, 9000), 50);
                System.out.println("Working " + TestIP);
                Servers.add(TestIP);

            }
            catch (Exception e) {
                System.out.println(IP +"." + Integer.toString(i)+ " is not working");
            }
        }

    }
}
