package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MainServer extends ServerSocket {


    private final HashMap<String, Function<String, Boolean>> listeners = new HashMap<>();
    private final HashMap<String, Function<String, Boolean>> commands = new HashMap<>();

    protected final HashMap<Integer, Boolean> connections = new HashMap<>();
    protected final HashMap<Integer, Socket> clients = new HashMap<>();

    /**
     * Creates a new Server
     * to add a command use obj.addCommand("Name", (str) -> {
     *     // Code here
     *     return true;
     * });
     *
     * to add a listener use obj.addListener("Name", (str) -> {
     *     // Code here
     *     return true;
     * }
     * Possible event names:
     *      Connection:established
     *      Message:sendAll
     *      Message:sendTo[%s]
     *      Command:add[Name:%s{%s}]
     *
     *
     *
     * @param Port the port on which this server should open
     * @param addr the InetAddress of this server
     * @throws IOException
     */
    public MainServer(int Port, InetAddress addr) throws IOException {
        super(Port, 50, addr);
        new Thread ( () -> {
            while (true) {
                Socket s = null;
                try {
                    s = this.accept();
                    listeners.forEach( (str, o) -> {
                        o.apply("Connection:established");
                    });
                    int port = getFreePort() | clients.size() + 1001;
                    clients.put(port, s);
                    connections.put(port, true);
                    new MiniServer(this, port).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    /**
     * Get a free port out of the list of all clients, if there is no port free create a new one
     * @return 0 | port
     */
    private int getFreePort() {
        AtomicInteger freePort = new AtomicInteger();
        clients.forEach((integer, socket) -> {
            if (!connections.get(integer)) {
                freePort.set(integer);
            }
        });
        return freePort.get();
    }

    /**
     * send a message to all connected clients
     * @param msg the message as String that should get send
     */
    public void sendToAll(String msg) {
        listeners.forEach( (str, o) -> {
            o.apply("Message:sendAll");
        });

        msg += "\n";
        msg.chars().forEach(e -> clients.forEach((i, c) -> {
            try {
                c.getOutputStream().write(e);
            } catch (IOException ignored) {
                connections.replace(i, false);
            }
        }));
    }

    /**
     * Send a message to a specific socket
     * @param s the socket to send to
     * @param msg the message as String
     */
    public void sendString(Socket s, String msg) {
        listeners.forEach( (str, o) -> {
            o.apply(String.format("Message:sendTo[%s]",s));
        });

        msg += "\n";
        msg.chars().forEach(e -> clients.forEach((i, c) -> {
            try {
                c.getOutputStream().write(e);
            } catch (IOException ignored) {
                connections.replace(i, false);
            }
        }));
    }

    /**
     * Add a command to this server
     * @param s the name of the command, e.g. Test -true --> here Test
     * @param f the function that get executed by this command, requires a String and return a boolean
     */
    public void addCommand (String s, Function<String, Boolean> f) {
        commands.put(s, f);
        listeners.forEach( (str, o) -> {
            o.apply(String.format("Command:add[Name:%s{%s}]",s,f));
        });
    }

    /**
     * Get all commands that this Server know
     * @return the HashMap with the Commands
     */
    public HashMap<String, Function<String, Boolean>> getCommands() {
        return commands;
    }

    /**
     * Add a listener to this server
     * @param name the name to access this listener
     * @param f the function the get executed when it fires any event
     */
    public void addListener(String name, Function<String, Boolean> f) {
        this.listeners.put(name, f);
    }

    /**
     * get a specific listener
     * @param name the access name of this listener
     * @return the requested listener
     */
    public Function<?,?> getListener(String name) {
        return this.listeners.get(name);
    }

    /**
     * get all available listener
     * @return the HashMap with all Listener
     */
    public HashMap<String, Function<String, Boolean>> getAllListeners() {
        return this.listeners;
    }
}
