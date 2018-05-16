import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.lang.management.ManagementFactory;
/**
*Server class

*/

public class Server{
  /**
  *port number to connect over
  */
  private int portNumber = 0;
  /**
  *welcome message sent to client, prompting username input
  */
	private String welcome = "Please type your username.";
  /**
  *Sent after entering a unique username, triggers an event allowing the client to type
  */
	private  String accepted = "Username accepted.";
  /**
  *Socket
  */
  private ServerSocket ss;
  /**
  *hashset containing the client names
  */
  private HashSet<String> clientNames = new HashSet<String>();
  /**
  *hashset containing the client writer objects
  */
  private HashSet<PrintWriter> clientWriters = new HashSet<PrintWriter>();
  /**
  *main method, creates and starts the server object
  *@throws IOException From client inputs
  *@param args Program arguments
  */
  public static void main (String[] args) throws IOException {
		Server server = new Server();
		server.start();
	}
  /**
  *Method that creates the socket and handler thread for each client that connects
  *@throws IOException From client inputs
  *<p>
  *Calls: {@link Server#shutDown()}
  */
	void start() throws IOException {
    Scanner s = new Scanner(System.in);
    do {
      System.out.println("Enter server port:");
      try {
        portNumber = s.nextInt();
      }
      catch (InputMismatchException ex){
        System.out.println("Invalid");
      }
      s.nextLine();
    } while (portNumber > 65535 || portNumber < 1);
		ss = new ServerSocket(portNumber);
		System.out.println("Server at " + InetAddress.getLocalHost() + " is waiting for connections ...");
		Socket socket;
		Thread thread;
		try {
			while (true) {
				socket = ss.accept();
				thread = new Thread(new HandleSession(socket));
				thread.start();
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

  /**
  *Nested handler class for each client connection
  */
	class HandleSession implements Runnable {
    /**
    *client name
    */
    String name;
    /**
    *Client Socket
    */
		private Socket socket;
    /**
    *reader for incoming client messages
    */
		BufferedReader in = null;
    /**
    *writer for client
    */
		PrintWriter out = null;
    /**
    *connection IP, needed to display connection info
    */
    InetAddress ip;
    /**
    *time client thread starts
    */
    long startTime;

    /**
    *constructor
    *@param socket the socket to use for this client thread
    */
		HandleSession (Socket socket) {
			this.socket = socket;
		}
    /**
    *run method to execute when thread starts
    *creates the client stream, establishes the start time
    *, gets the client name and starts the listener method.
    *<p>
    *Calls: {@link HandleSession#createInOutStreams()}
    *{@link HandleSession#getClientUserName()}
    *{@link HandleSession#listenForClientMessages()}
    */
		public void run() {
			try {
				createInOutStreams();
        startTime = System.currentTimeMillis();
				getClientUserName();
				listenForClientMessages();
			}
			catch (IOException e) {
				System.out.println(e);
			}
		}
    /**
    *establish buffered reader and the printwriter, adds it to the HashSet
    */
		private void createInOutStreams() {
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				clientWriters.add(out);
				System.out.println("One connection is established");
			}
			catch (IOException e) {
				System.err.println("Exception in createInOutStreams(): " + e);
			}
		}
    /**
    *gets unique client username and adds it to the HashSet
    */
		private void getClientUserName() {
      while (true){
        out.println(welcome);
        out.flush();
  			try{
          name = in.readLine();
        }
  			catch (IOException e) {
  				System.err.println("Exception in getClientUserName: " + e);
  			}
  			if (!clientNames.contains(name)){
          clientNames.add(name);
          out.println(accepted);
          broadcast(name + " has joined the chat.");
          out.flush();
          return;
        }
        if (clientNames.contains(name)){
          out.println("Username unavailable, try another.");
  			  out.flush();
          continue;
        }
      }
		}

    /**
    *method that listens for incoming client messages
    *Calls: {@link HandleSession#broadcast(String)}
    *{@link HandleSession#processCommand(String)}
    *{@link HandleSession#closeClientConnection()}
    *@throws IOException Throws IOException should client input the wrong info
    */
		private void listenForClientMessages() throws IOException {
			String line;
        try {
          while (in != null) {
    				line = in.readLine();
    				if (line != null && !line.startsWith("\\") && !line.equals("")){
              broadcast(name + " says: " + line);
            }
            if(line.startsWith("\\")){
              processCommand(line);
            }
          }
        }
        catch (SocketException e) {
          if (clientNames.contains(name)) {
            System.out.println("User " + name + " lost connection.");
            closeClientConnection();
          }
        }
		}
    /**
    *broadcasts method sent by this client to all the other threads
    *iterates through the writer hashSet
    *@param message The string message to broadcast
    */
    private void broadcast(String message){
      for(PrintWriter writer : clientWriters){
        writer.println(message);
        writer.flush();
      }
      System.out.println(message);
    }

    /**
    *method run when commands are entered
    *Calls: {@link HandleSession#closeClientConnection()}
    *@param command The string command to process
    */
    void processCommand (String command) {
      if (command.equals("\\help")){
        out.println("--------------------------------------------------------------------------");
        out.println("List of server commands: ");
        out.println("\\quit disconnects");
        out.println("\\uptime gets the server uptime");
        out.println("\\clienttime gets the time you have been connected to the server");
        out.println("\\serverip gets the server's IP address");
        out.println("\\clients lists the number of connected clients and their names");
        out.println("--------------------------------------------------------------------------");
        out.flush();
      }
      else if (command.equals("\\quit")){
        closeClientConnection();
      }
      else if (command.equals("\\uptime")){
        double upTime = ManagementFactory.getRuntimeMXBean().getUptime();
        out.println("--------------------------------------------------------------------------");
        out.println("Server uptime: " + upTime/1000 + "s");
        out.println("--------------------------------------------------------------------------");
        out.flush();
      }
      else if (command.equals("\\serverip")){
        try {
        ip = InetAddress.getLocalHost();
        out.println("--------------------------------------------------------------------------");
        out.println("Server IP: " + ip.getHostAddress());
        out.println("--------------------------------------------------------------------------");
        out.flush();
        }
        catch (UnknownHostException e){
        }
      }
      else if (command.equals("\\clients")){
        out.println("--------------------------------------------------------------------------");
        out.println("Connected clients: " + clientNames.size());
        Object[] names = clientNames.toArray();
        for (int i = 0; i < clientNames.size(); i++){
          out.println(names[i]);
        }
        out.println("--------------------------------------------------------------------------");
        out.flush();
      }
      else if (command.equals("\\clienttime")){
        out.println("--------------------------------------------------------------------------");
        out.println("Client connected for: " + (System.currentTimeMillis() - startTime)/1000 + "s");
        out.println("--------------------------------------------------------------------------");
        out.flush();
      }
      else {
        out.println("--------------------------------------------------------------------------");
        out.println("Unrecognised command, type \\help for a list of valid commands.");
        out.println("--------------------------------------------------------------------------");
        out.flush();
      }
    }

    /**
    *closes the client connection, removes their name and writer from the hashSet of each
    *Calls: {@link HandleSession#broadcast(String)}
    */
		void closeClientConnection() {
      try{
        socket.close();
      }
      catch(IOException e){
        System.out.println("Error closing socket");
      }
			if(name != null) {
        broadcast(name + " has left the chat.");
        clientNames.remove(name);
      }
      if (out != null){
        clientWriters.remove(out);
      }
		}
	}

}
