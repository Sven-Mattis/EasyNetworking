package EasyServer.Client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public class Client {

    public enum Event {
        Message_Received;
    }

    private Socket s;

    private HashMap<String, Function<Event, Boolean>> eventListeners = new HashMap<>();

    private final ArrayList<String> messages = new ArrayList<>();
    
    public Client (InetAddress addr, int port) throws IOException {
        s = new Socket(addr, port);

        new Thread( () -> {
            char last = 0;
            String str = "";
            while (true) {
                try {
                    last = (char) s.getInputStream().read();
                    if(last == 0) {
                        this.messages.add(str);
                        str = "";
                        eventListeners.forEach( (strrr, f) -> f.apply(Event.Message_Received));
                    } else
                        str += last;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public ArrayList<String> getMessages() {
        return this.messages;
    }

    public String getLastMessageReceived () {
        if(messages.size() == 0)
            return null;

        return this.messages.get(this.messages.size()-1);
    }

    public void sendMessage (String str) throws IOException {
        str.chars().forEach((chr) -> {
            try {
                s.getOutputStream().write(chr);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        s.getOutputStream().write(0);
    }

    public void addListener(String key, Function<Event, Boolean> f) {
        eventListeners.put(key, f);
    }

    public void removeListener(String key) {
        eventListeners.remove(key);
    }

}
