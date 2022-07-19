package EasyServer.Server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

class MiniServer extends Thread {

    private final Socket s;
    private final MainServer mainServer;

    protected MiniServer (MainServer mainServer, int port) throws IOException {
        if(port == mainServer.getLocalPort())
            throw new BindException("Port not available!");

        this.mainServer = mainServer;

        ServerSocket server = new ServerSocket(port, 50, mainServer.getInetAddress());
        mainServer.sendString(mainServer.clients.get(port), ""+port+(char) 0);
        s = server.accept();
        while(!s.isConnected())
            continue;
        mainServer.clients.replace(port, s);
    }

    @Override
    public void run() {
        super.run();
        ArrayList<String> words = new ArrayList<>();
        // Listen to incoming messages
        new Thread( () -> {
            try {
                char lastIn;
                String word = "";
                do {
                    lastIn = (char) s.getInputStream().read();
                    if(lastIn == 0) {
                        words.add(word);
                        Function<String, Boolean> f = checkCommand(word.split("\s")[0], mainServer.getCommands());
                        if (f==null)
                            mainServer.sendString(s, "No Command found!");
                        else
                            if(f.apply(word))
                                mainServer.sendString(s, String.format("Successfully executed Task! --> %s",word));
                            else
                                mainServer.sendString(s, String.format("An Error occured while excuting Task! --> %s",word));
                        word = "";
                    } else {
                        word += lastIn;
                    }
                } while (true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /**
     * Get the Command matching the Input, if no command is found then return null
     * @param toSearch the String to search for
     * @param commands the list of all possible commands
     * @return a function that need a String and return a Boolean
     */
    private Function<String, Boolean> checkCommand(String toSearch, HashMap<String, Function<String, Boolean>> commands) {
        // Create reference
        AtomicReference<Function<String, Boolean>> finalF = new AtomicReference<>();

        // Check if command exists
        commands.forEach((str, f) -> {
            if(toSearch.equals(str))
                finalF.set(f);
        });

        return finalF.get();
    }
}
