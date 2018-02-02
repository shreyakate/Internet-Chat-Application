import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class client 
{
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream written to the socket
 	ObjectInputStream in;          //stream read from the socket
	String username, message, serverReply;                
	boolean registered=false;                
	int tries = 0;
	private String msgTokens[];
 

	Runnable r1 = new Runnable() 
	{
		public void run() 
		{
			try{
				InetAddress address = InetAddress.getByName("localhost");
				
				//create a socket to connect to the server
				requestSocket = new Socket(address.getHostAddress(), 8000);
				System.out.println("Connected to server in port 8000");
				//initialize inputStream and outputStream
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(requestSocket.getInputStream());
				
				//get Input from standard input
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
				do
				{
					if(tries == 0)
					{
						System.out.print("Hello, please enter a username: ");
					}
					else if(tries > 0)
					{
						System.out.print("Please try another username : ");
					}
					username = bufferedReader.readLine();
					sendToServer(username);
					registered = (boolean)in.readObject();
					if(registered == true)
					{
						//show the message to the user
						System.out.println("You are now registered with username : " + username);
					}
					else
					{
						tries++;
					}
				}while(registered != true);
				
				System.out.println("***********WELCOME!!!***********");
				System.out.println("UNICAST 		: @UserName Message");
				System.out.println("BROADCAST		: Message");
				System.out.println("BLOCKCAST		: !@UserName Message");
				System.out.println("FILE UNICAST		: fs@UserName PathToFile");
				System.out.println("FILE BROADCAST		: fs PathToFile");
				
				//Create a folder with the user's username
				new File(username).mkdir();
				
				//Start new thread to listen incoming messages and files
				Thread thr2 = new Thread(r2);
				thr2.start();
				
				while(true)
				{
					message = bufferedReader.readLine();
					
					msgTokens = message.split(" ");
					
					//If file is to be sent to another client
					if(message.substring(0, 2).equalsIgnoreCase("fs"))
					{	
						File file = new File(msgTokens[1]);
						long fileSize = file.length();
						sendToServer(message+" "+fileSize);
						
				        FileInputStream fis = new FileInputStream(file);
				        BufferedInputStream bis = new BufferedInputStream(fis);
				        
				        byte[] contents; 
				        long current = 0;
				        
				        //Read 10000 bytes at a time and write it in the output stream
				        while(current!=fileSize)
				        { 
				            int size = 10000;
				            if(fileSize - current >= size)
				                current += size;    
				            else{ 
				                size = (int)(fileSize - current); 
				                current = fileSize;
				            } 
				            contents = new byte[size]; 
				            bis.read(contents, 0, size); 
				            out.write(contents);
				        } 
				        System.out.println("File Sent");
				        out.flush();
				        bis.close();
					}
					//If a message is to be sent to another client
					else
						sendToServer(message);
				}
			}
			catch (ConnectException e) {
	    			System.err.println("Connection refused. You need to initiate a server first.");
			} 
			catch(UnknownHostException unknownHost){
				System.err.println("You are trying to connect to an unknown host!");
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
			catch (ClassNotFoundException e){
				e.printStackTrace();
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					requestSocket.close();
				}
				catch(IOException ioException){
					ioException.printStackTrace();
				}
			}
		}
	};
	
	Runnable r2 = new Runnable()
	{
		public void run() 
		{
			try
			{
				while(true)
				{
					message = (String) in.readObject();
					System.out.println(message);
					msgTokens = message.split(" ");
					if(msgTokens[0].equalsIgnoreCase("file"))
					{
						String fileName = msgTokens[msgTokens.length-2];
						long fileSize = Long.parseLong(msgTokens[msgTokens.length-1]);
						byte[] contents = new byte[(int) fileSize];
					
						File destinationFile = new File(username+"/"+fileName);
    					
    					FileOutputStream fos = new FileOutputStream(destinationFile);
    					BufferedOutputStream bos = new BufferedOutputStream(fos);
    			        
    			        in.readFully(contents); 
    			        fos.write(contents, 0, contents.length);
    			        
    			        fos.close();
    			        bos.close();
    			        System.out.println("File "+fileName+" from user "+msgTokens[2]+" saved successfully!");	
					}
					java.awt.Toolkit.getDefaultToolkit().beep();
				}
			}
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			} 
			catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			}
		}
	};
		
	void sendToServer(String str)
	{
		try{
			out.reset();
			//stream write the message
			out.writeObject(str);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	
	void startThreads()
	{
		Thread thr1 = new Thread(r1);
		thr1.start();
	}
	//main method
	public static void main(String args[])
	{
		client newClient = new client();
		newClient.startThreads();
	}
}	