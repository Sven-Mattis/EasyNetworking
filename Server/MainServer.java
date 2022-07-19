package EasyServer.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MainServer extends ServerSocket {


    private final HashMap<String, Function<String, Boolean>> listeners = new HashMap<>();
    private final HashMap<String, BiFunction<Socket, String, Boolean>> commands = new HashMap<>();
    protected final HashMap<Socket, Boolean> clients = new HashMap<>();

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
                    clients.put(s, s.isConnected());
                    new MiniServer(s, this).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
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
        msg.chars().forEach(e -> clients.forEach((s, b) -> {
            try {
                s.getOutputStream().write(e);
            } catch (IOException ignored) {
                clients.replace(s, s.isConnected());
            }
        }));
    }

    /**
     * Send a message to a specific socket
     * @param s the socket to send to
     * @param msg the message as String
     */
    public void sendString(Socket s, String msg) throws IOException {
        listeners.forEach( (str, o) -> {
            o.apply(String.format("Message:sendTo[%s]",s));
        });

        msg += "\n";
        msg.chars().forEach((chr) ->{
            try {
                s.getOutputStream().write(chr);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        s.getOutputStream().write(0);
    }

    /**
     * Add a command to this server
     * @param s the name of the command, e.g. Test -true --> here Test
     * @param f the function that get executed by this command, requires a String and return a boolean
     */
    public void addCommand (String s, BiFunction<Socket, String, Boolean> f) {
        commands.put(s, f);
        listeners.forEach( (str, o) -> {
            o.apply(String.format("Command:add[Name:%s{%s}]",s,f));
        });
    }

    /**
     * Get all commands that this Server know
     * @return the HashMap with the Commands
     */
    public HashMap<String, BiFunction<Socket, String, Boolean>> getCommands() {
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
