import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class JettyDemo {

    public static void main(String[] args) throws Exception {

        Server server = new Server(8080);

        try (ServerConnector connector = new ServerConnector(server)) {
            connector.setPort(8080);
            server.setConnectors(new Connector[]{connector});
        }
        server.start();
        server.join();

    }
}
