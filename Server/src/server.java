import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class server
{

	private static final int sPort = 8000;   //The server will be listening on this port number
	private static Map connMap = new HashMap(); 
	private static Map userMap = new HashMap(); 
	private static Map ipMap = new HashMap(); 
	
	public static void main(String[] args) throws Exception 
	{
		System.out.println("The server is running."); 
        ServerSocket listener = new ServerSocket(sPort);
        new File("filesToForward").mkdir();
		int clientNum = 1;
        try 
        {
        	while(true) 
        	{
        		new Handler(listener.accept(),clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
            }
        } finally 
        {
        	listener.close();
        } 
    }
	/**
	* A handler thread class.  Handlers are spawned from the listening
	* loop and are responsible for dealing with a single client's requests.
	*/
	private static class Handler extends Thread 
	{
		private String username, ipString; 
		private String message, receiver;  
		private Socket connection;
	    private ObjectInputStream in, tempIn;	//stream read from the socket
	    private ObjectOutputStream out, recConn, tempOut;    //stream write to the socket
		private int no;		//The index number of the client
		private boolean isRegistered = false, exists = false;
		private String msgTokens[];
	
	    public Handler(Socket connection, int no) 
	    {
	    	this.connection = connection;
	    	this.no = no;
	    }
	
	    public void run() 
	    {
	    	try
	    	{
	    		//initialize Input and Output streams
	    		out = new ObjectOutputStream(connection.getOutputStream());
	    		out.flush();
	    		in = new ObjectInputStream(connection.getInputStream());
	    		try
	    		{
	    			do
	    			{
	    				username = (String)in.readObject();
	    				System.out.println("Received request to register username : " + username);
	    				isRegistered = userMap.containsKey(username);
	    				if(!isRegistered)
	    				{
	    					userMap.put(username, out);
	    				}
	    				sendMessage(!isRegistered);
	    			}while(isRegistered);
	    			
	    			while(true)
	    			{
	    				message = (String)in.readObject();
	    				msgTokens = message.split(" ");
	    				//If a text message is to be sent
	    				if(!(msgTokens[0].substring(0, 2).equalsIgnoreCase("fs")))
	    				{
	    					//To send a unicast message
		    				if(msgTokens[0].charAt(0) == '@')
		    				{
		    					message = message.replaceFirst(msgTokens[0]+" ", "");
		    					receiver = msgTokens[0].replaceFirst("@", "");
		    					recConn = (ObjectOutputStream) userMap.get(receiver);
		    					if(recConn == null)
		    					{
		    						sendMessage("Username "+receiver+" does not exist.");
		    					}
		    					else
		    					{
		    						System.out.println("Message from "+username+" to "+receiver);
		    						sendMessage(recConn, "Message from "+username+" : "+message);
		    					}
		    				}
		    				//To send a blockcast message
		    				else if(msgTokens[0].charAt(0) == '!' && msgTokens[0].charAt(1) == '@')
		    				{
		    					message = message.replaceFirst(msgTokens[0]+" ", "");
		    					receiver = msgTokens[0].replaceFirst("!@", "");
		    					for (Object entry : userMap.keySet()) 
		    					{
		    					    String user = entry.toString();
		    					    if(!(user.equalsIgnoreCase(receiver)) && !(user.equalsIgnoreCase(username)))
		    					    {
		    					    	recConn = (ObjectOutputStream) userMap.get(user);
		    					    	System.out.println("Message from "+username+" to "+user);
		    					    	sendMessage(recConn, "Message from "+username+" : "+message);
		    					    }
		    					}
		    				}
		    				//To send a broadcast message
		    				else
		    				{
		    					for (Object entry : userMap.keySet()) 
		    					{
		    					    String user = entry.toString();
		    					    if(!(user.equalsIgnoreCase(username)))
		    					    {
		    					    	System.out.println("Message from "+username+" to "+user);
		    					    	recConn = (ObjectOutputStream) userMap.get(user);
		    					    	sendMessage(recConn, "Message from "+username+" : "+message);
		    					    }
		    					}
		    				}
	    				}
	    				//If a file is to be sent
	    				else
	    				{
	    					String filePath[] = msgTokens[1].split("/");
	    					String fileName = filePath[filePath.length-1];
	    					long fileSize = Long.parseLong(msgTokens[2]);
	    					
	    					byte[] contents = new byte[(int) fileSize];
	    					//Store the file to be sent in the folder "filesToForward"
	    					File destinationFile = new File("filesToForward/"+fileName);
	    					
	    					FileOutputStream fos = new FileOutputStream(destinationFile);
	    					BufferedOutputStream bos = new BufferedOutputStream(fos);
	    			        
	    			        in.readFully(contents);
	    			        fos.write(contents, 0, contents.length);
	    			        
	    			        fos.close();
	    			        bos.close();
	    			        
	    			        //To unicast a file
		    				if(msgTokens[0].length() > 2)
		    				{
		    					receiver = msgTokens[0].replaceFirst("fs@", "");
		    					recConn = (ObjectOutputStream) userMap.get(receiver);
		    					if(recConn == null)
		    					{
		    						sendMessage("Username "+receiver+" does not exist.");
		    					}
		    					else
		    					{
		    						System.out.println("File from "+username+" to "+receiver);
		    						sendFile(recConn, "File from "+username, fileName, fileSize, contents);
		    					}
		    				}
		    				//To broadcast a file
		    				else
		    				{
		    					for (Object entry : userMap.keySet()) 
		    					{
		    					    String user = entry.toString();
		    					    if(!(user.equalsIgnoreCase(username)))
		    					    {
		    					    	System.out.println("File from "+username+" to "+user);
		    					    	recConn = (ObjectOutputStream) userMap.get(user);
		    					    	sendFile(recConn, "File from "+username, fileName, fileSize, contents);
		    					    }
		    					}
		    				}
	    				}
	    			}
	    		}
	    		catch(ClassNotFoundException classnot)
	    		{
					System.err.println("Data received in unknown format");
				}
	    	}
	    	catch(IOException ioException)
	    	{
	    		userMap.remove(username);
	    		System.out.println("Disconnected with Client " + username);
	    	}
	    	finally
	    	{
	    		//Close connections
	    		try
	    		{
					in.close();
					out.close();
					connection.close();
					userMap.remove(username);
					connMap.remove(connection);
				}
	    		catch(IOException ioException)
	    		{
	    			userMap.remove(username);
	    			System.out.println("Disconnected with Client " + username);
	    		}
	    	}
	    }
		//send registration confirmation to the client
		public void sendMessage(boolean status)
		{
			try
			{
				out.writeObject(status);
				out.flush();
				if(!isRegistered)
				{
					System.out.println("Username "+username+" is now registered.");
				}
			}
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			}
		}
		//Reply back to the client if the client at the destination does not exist
		public void sendMessage(String status)
		{
			try
			{
				out.writeObject(status);
				out.flush();
			}
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			}
		}
		//Send message to the client at the destination
		public void sendMessage(ObjectOutputStream oos, String msg)
		{
			try
			{
				oos.writeObject(msg);
				oos.flush();
			}
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			}
		}
		//Send file to the destined client
		public void sendFile(ObjectOutputStream oos, String msg, String fname, long fileSize, byte[] contents)
		{
			try
			{
				oos.writeObject(msg+" "+fname+" "+fileSize);
				oos.flush();
				oos.write(contents);
				oos.flush();
	        }
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			}
		}
	}
}