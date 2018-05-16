import java.io.*;
import java.net.*;
/**
*Simple class to set up and execute the client program
*/
public class ClientMain {
	/**
	*Main method to create the client object and run methods
	*Calls: {@link ClientInstance#start()}
	*/
	public static void main(String[] args) throws Exception {
		ClientInstance client = new ClientInstance();
		client.start();
	}
}
