import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Handler {
    // This chat server can accept up to maxClientsCount clients' connections.
    public static final int maxClientsCount = 1000;
    public static final client[] threads = new client[maxClientsCount];

    public static HashMap<String, String> ID = new HashMap<>();
    public static HashMap<String, ArrayList<String>> Serial = new HashMap<>();

    private static ArrayList<String> killList = new ArrayList<>();

    Thread killer = null;

    public static boolean running = true;

    public Handler() {
        killer = new Thread() {
            @Override
            public void run() {
                while (running) {
                    //message("Killing bad clients.");
                    ArrayList<client> clients = new ArrayList<>(Arrays.asList(threads));
                    clients.forEach(x ->
                    {
                        try{
                            if(x != null) {
                                //System.out.println("Checking " + x.serial + ".");
                                    if(killList.contains(x.serial))
                                    {
                                        message("Sending kill command to " + x.serial + ".");
                                        x.sendMessage("switch");
                                    }
                            }
                        }
                        catch(NullPointerException e)
                        {
                            message("Client has not been initialized yet.");
                        }
                    });
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        killer.start();
    }

    public void listSerials() {
        message("Listing registered serials:");
        ID.keySet().forEach(x ->
        {
            message(x);
        });
    }

    public void listID() {
        message("Listing registered IDs:");
        ID.keySet().forEach(x ->
        {
            message(x + " -> " + ID.get(x));
        });
    }

    public void kill(String serial) {
        killList.add(serial);
    }

    public void revive(String serial) {
        killList.remove(serial);
    }

    public void register(client c, String serial) {
        String ip = c.clientSocket.getRemoteSocketAddress().toString();
        if (!ID.keySet().contains(serial)) {
            ID.put(serial, ip);
        }
        if (!Serial.containsKey(serial)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(ip);
            Serial.put(serial, list);
        }
        if (!Serial.get(serial).contains(ip)) {
            ArrayList<String> list = Serial.get(serial);
            list.add(ip);
            Serial.put(serial, list);
        }
        message("A new connection from " + ip + " with the ID of " + ID.get(serial) + " and serial of " + serial + " has been registered!");
        if (killList.contains(serial)) {
            message("This client has the serial " + serial + " which has been registered to be removed.");
            message("The client has been sent the kill switch command and has been removed from the network.");
        }
    }

    public void rename(String serial, String name) {
        if (!ID.keySet().contains(serial))
            ID.put(serial, name);
        message("Assigned new ID " + name + " to " + serial + ".");
    }

    public void remove(String serial) {
        if (!ID.keySet().contains(serial))
            ID.remove(serial);
        message("Removed " + serial + " from the list.");
    }

    public ArrayList<String> find(String name) {
        ArrayList<String> ips = new ArrayList<>();
        ID.keySet().forEach(x -> {
            String n = ID.get(x);
            if (n.equals(name)) {
                ips.add(x);
            }
        });
        message("Found " + ips.size() + " of matching serials with the ID of " + name + ".");
        return ips;
    }

    public void message(String msg) {
        System.out.println(msg);
    }

    private ArrayList<String> findSerial(String ip) {
        ArrayList<String> serials = new ArrayList<String>();
        Serial.keySet().forEach(x ->
        {
            if (Serial.get(x).contains(ip))
                serials.add(x);
        });
        return serials;
    }

    private ArrayList<String> findSerialFromID(String id) {
        ArrayList<String> serials = new ArrayList<String>();

        ID.keySet().forEach(x ->
        {
            if (Serial.get(x).equals(id))
                serials.add(x);
        });
        return serials;
    }

    public void command(String cmd) {
        if (cmd.startsWith("serialid")) {
            String[] args = cmd.split(" ");
            if (args.length > 1) {
                message("Finding id...");
                ArrayList<String> list = findSerialFromID(args[1]);
                list.forEach(x ->
                {
                    message(x);
                });
            } else {
                message("Please define an ID.");
            }
        } else if (cmd.startsWith("serial")) {
            message("Finding serial...");
            String[] args = cmd.split(" ");
            if (args.length > 1) {
                ArrayList<String> list = findSerial(args[1]);
                list.forEach(x ->
                {
                    message(x);
                });
            } else {
                message("Please define an IP.");
            }
        } else if (cmd.startsWith("find")) {
            String[] args = cmd.split(" ");
            if (args.length > 1) {
                message("Finding...");
                ArrayList<String> list = find(args[1]);
                list.forEach(x ->
                {
                    message(x);
                });
            } else {
                message("Please define an ID.");
            }
        } else if (cmd.startsWith("remove")) {
            String[] args = cmd.split(" ");
            if (args.length > 1) {
                message("Removing " + args[1] + ".");
                remove(args[1]);
//                list.forEach(x ->
//                {
//                    message(x);
//                });
            } else {
                message("Please define a serial.");
            }
        } else if (cmd.startsWith("rename")) {
            String[] args = cmd.split(" ");
            if (args.length > 2) {
                message("Renaming.");
                rename(args[1], args[2]);
//                list.forEach(x ->
//                {
//                    message(x);
//                });
            } else {
                message("Please define a serial and a name.");
            }
        } else if (cmd.startsWith("revive")) {
            String[] args = cmd.split(" ");
            if (args.length > 1) {
                message("Reviving the serial " + args[1] + ".");
                revive(args[1]);
//                list.forEach(x ->
//                {
//                    message(x);
//                });
            } else {
                message("Please define a serial to revive.");
            }
        } else if (cmd.startsWith("kill")) {
            String[] args = cmd.split(" ");
            System.out.println("Args Length: " + args.length);
            System.out.println("Args: " + args[0]);
            if (args.length > 1) {
                message("Killing the serial " + args[1] + ".");
                kill(args[1]);
//                list.forEach(x ->
//                {
//                    message(x);
//                });
            } else {
                message("Please define a serial to kill.");
            }
        } else if (cmd.startsWith("listid")) {
            String[] args = cmd.split(" ");

            listID();
//                list.forEach(x ->
//                {
//                    message(x);
//                });
        } else if (cmd.startsWith("listserial")) {
            String[] args = cmd.split(" ");

            listSerials();
//                list.forEach(x ->
//                {
//                    message(x);
//                });
        } else if (cmd.equals("help")) {
            message("List of available commands:");
            message("listserial");
            message("listid");
            message("kill <serial>");
            message("revive <serial>");
            message("rename <serial> <new name>");
            message("remove <serial>");
            message("find <name>");
            message("serial <ip>");
            message("serialid <name>");
        } else {
            message("Command not found.");
        }
    }

}
