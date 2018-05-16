import java.io.*;
import java.net.*;

/**
*Client class
*/
public class ClientInstance {
	/**
	*welcome message recieved from server prompting username input
	*/
	private String welcome = "Please type your username.";
	/**
	*String recieved from server to trigger event allowing client to sender
	*and recieve messages
	*/
	private String accepted = "Username accepted.";
	/**
	*Socket to connect with
	*/
	private Socket socket = null;
	/**
	*Reader to read in messages sent from the server
	*/
	private BufferedReader in;
	/**
	*Writer used to output to server
	*/
	private PrintWriter out;
	/**
	*boolean preventing client from sending messages to others until made true
	*/
	private boolean isAllowedToChat = false;
	/**
	*Self explanatory
	*/
	private boolean isServerConnected = false;
	/**
	*name of the client
	*/
	private String clientName;

	/**
	*Method used to execute everything else
	*<p>
	*Calls: {@link ClientInstance#establishConnection()}
	*{@link ClientInstance#handleOutgoingMessages()}
	*{@link ClientInstance#handleIncomingMessages()}
	*<p>
	*/
	public void start() {
		while (!establishConnection()){}
		handleOutgoingMessages();
		handleIncomingMessages();
	}

	/**
	*method which sets up the socket connection to the server and reader and writer objects
	*<p>
	*Calls: {@link ClientInstance#getClientInput(String)}
	*{@link ClientInstance#handleProfileSetUp()}
	*/
	private boolean establishConnection() {
			String serverAddress = getClientInput("What is the address of the server that you wish to connect to?");
			int serverPort = 0;
			do {
				try {
					serverPort = Integer.parseInt(getClientInput("What is the port to connect through?"));
				}
				catch (NumberFormatException ex){
					System.out.println("Invalid");
				}
			} while (serverPort > 65535 || serverPort < 1);
			try {
				socket = new Socket(serverAddress, serverPort);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				isServerConnected = true;
			}
			catch (IOException e) {
				System.out.println("Could not connect to specified address " + e);
				isServerConnected = false;
			}
			if (!isServerConnected) {
				return false;
			}
			else {
				handleProfileSetUp();
				return true;
			}
		}

	/**
	*Method to get the client username and send it to the server,
	* the client is allowed to chat if the server replies with the accepted string
	*<p>
	*Calls: {@link ClientInstance#refreshClientTerminal()}
	*{@link ClientInstance#getClientInput(String)}
	*<p>
	*/
	private void handleProfileSetUp() {
		String line = null;
		while (!isAllowedToChat) {
			try {
				line = in.readLine();
			}
			catch (IOException e) {
				System.err.println("Exception in handleProfileSetUp:" + e);
			}
			if (line.startsWith(welcome)) {
				out.println(getClientInput(welcome));
			}
			else if (line.startsWith(accepted)) {
				isAllowedToChat = true;
				refreshClientTerminal();
				System.out.println("Username accepted, you can now type messages.");
				System.out.println("To see a list of commands, type \\help.");
			}
			else System.out.println(line);
		}
	}

	/**
	*Method to send messages, creates a thread via an anonymous class so that incoming and outgoing messages
	*are handled concurrently
	*<p>
	*Calls: {@link ClientInstance#getClientInput(String)}
	*<p>
	*/
	private void handleOutgoingMessages() {
		Thread senderThread = new Thread(new Runnable(){
			/**
			*Message to output via the client's PrintWriter object
			*/
			String output;
			/**
			*Run method, waits for the client input and sends it if it is not null
			*/
			public void run() {
				while (isServerConnected){
					output = getClientInput(null);
					if (output != null){
						out.println(output);
					}
				}
			}
		});
		senderThread.start();
	}

  /**
	*reads in and returns a string that is not null
	*@param hint prints a hint to prompt the client on what to input, allowing this to be used for more than just getting message inputs
	*/
	private String getClientInput (String hint) {
		String message = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			if (hint != null) {
				System.out.println(hint);
			}
			message = reader.readLine();
			if (!isAllowedToChat) {
				clientName = message;
			}
		}
		catch (IOException e) {
			System.err.println("Exception in getClientInput(): " + e);
		}
		return message;
	}
	/**
	*Processes and prints out messages recieved from server that are not null
	*<p>
	*Uses an anonymous class and creates a thread so that this is run concurrently with the handler
	*for outgoing messages
	*<p>
	*/
	private void handleIncomingMessages() {
		Thread listenerThread = new Thread( new Runnable() {
			/**
			*Run method for the thread, waits for input and prints if it is not null
			*Calls {@link ClientInstance#closeConnection()} if connection to the server is lost.
			*/
			public void run() {
				while (isServerConnected) {
					String line = null;
					try {
						line = in.readLine();
						if (line == null) {
							isServerConnected = false;
							System.err.println("Disconnected from the server");
							closeConnection();
							break;
						}
						else if (line != null){
							 System.out.println(line);
						}
					}
					catch (IOException e) {
						isServerConnected = false;
						System.err.println("IOE in handleIncomingMessages()");
						closeConnection();
					}
				}
			}
		});
		listenerThread.start();
	}

	/**
	*Closes the server socket and exits the program
	*/
	void closeConnection() {
		try {
			socket.close();
			System.exit(0);
		}
		catch (IOException e) {
			System.err.println("Exception when closing the socket");
			System.err.println(e.getMessage());
		}
	}
	/**
	*Method to clear the terminal to improve readability
	*/
	private void refreshClientTerminal(){
		final String os = System.getProperty("os.name");
		if (os.contains("Windows")) {
			try {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			}
			catch(Exception o){}
		}
		else {
			try{
				//Runtime.getRuntime().exec("clear");
				System.out.print("\033[H\033[2J");
				System.out.flush();
			}
			catch(Exception l){
				//
			}
		}
	}
}
