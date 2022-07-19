package EasyServer.Server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

class MiniServer extends Thread {

    private final Socket s;
    private final MainServer mainServer;

    protected MiniServer (Socket s, MainServer mainServer) throws IOException {
        this.s = s;
        this.mainServer = mainServer;
    }

    @Override
    public void run() {
        super.run();
        ArrayList<String> words = new ArrayList<>();
        // Listen to incoming messages
        try {
            char lastIn;
            String word = "";
            do {
                lastIn = (char) s.getInputStream().read();
                if(lastIn == 0) {
                    words.add(word);
                    BiFunction<Socket, String, Boolean> f = checkCommand(word.split("\s")[0], mainServer.getCommands());
                    if (f==null)
                        mainServer.sendString(s, "No Command found!");
                    else if(!f.apply(s, word))
                        mainServer.sendString(s, String.format("An Error occured while excuting Task! --> %s",word));
                    word = "";
                } else {
                    word += lastIn;
                }
            } while (true);
        } catch (IOException e) {
            try {
                Thread.sleep(1000);
                mainServer.clients.replace(s, s.isConnected());
                s.close();
            } catch (InterruptedException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Get the Command matching the Input, if no command is found then return null
     * @param toSearch the String to search for
     * @param commands the list of all possible commands
     * @return a function that need a String and return a Boolean
     */
    private BiFunction<Socket, String, Boolean> checkCommand(String toSearch, HashMap<String, BiFunction<Socket, String, Boolean>> commands) {
        // Create reference
        AtomicReference<BiFunction<Socket, String, Boolean>> finalF = new AtomicReference<>();

        // Check if command exists
        commands.forEach((str, f) -> {
            if(toSearch.equals(str))
                finalF.set(f);
        });

        return finalF.get();
    }
}
