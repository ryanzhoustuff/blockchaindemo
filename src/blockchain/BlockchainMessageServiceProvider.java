package blockchain;

import java.security.PublicKey;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.Socket;
import java.net.ServerSocket;
import java.security.KeyPair;


public class BlockchainMessageServiceProvider
{
	private ServerSocket serverSocket = null;
    private boolean forever = true;
    private Hashtable<String, ConnectionChannelTaskManager> connections = null;
    private ConcurrentLinkedQueue<Message> messageQueue = null;
    private Hashtable<String, KeyNamePair> allAddresses = null;
    /**
     * genesis blockchain should become a public asset in this system
     */
    private static Blockchain genesisBlockchain = null;

	public BlockchainMessageServiceProvider()
	{
		System.out.println("BlockchainMessageServiceProvider is starting");
		connections = new Hashtable<String, ConnectionChannelTaskManager>();
		this.messageQueue = new ConcurrentLinkedQueue<Message>();
		this.allAddresses = new Hashtable<String, KeyNamePair>();
		try{
			serverSocket = new ServerSocket(Configuration.networkPort());
		}catch(Exception e){
			System.out.println("BlockchainMessageServiceProvider failed to create server socket. Failed");
			System.exit(1);
		}
	}
	
	
	protected void startWorking()
	{
		System.out.println("BlockchainMessageServiceProvider is ready");
		KeyPair keypair = UtilityMethods.generateKeyPair();
		//start the message checking thread
		MessageCheckingTaskManager checkingAgent = new MessageCheckingTaskManager(this, messageQueue, keypair);
		Thread agent = new Thread(checkingAgent);
		agent.start();
		System.out.println("BlockchainMessageServiceProvider generated MessageCheckingTaskManager, thread working");
		
		 // the loop to get connections
        while(forever){
            try{
                Socket socket = serverSocket.accept();
                System.out.println("BlockchainMessageServiceProvider accepts one connection");
                ConnectionChannelTaskManager st = new ConnectionChannelTaskManager(this, socket, keypair);
                Thread tt = new Thread(st);
                tt.start();
            }catch(Exception e){
            	System.out.println("BlockchainMessageServiceProvider runs into a problem: "+ e.getMessage()+" --> exit now");
                System.exit(2);
            }
        }
	}
	
	
	protected PublicKey findAddress(String ID)
	{
		KeyNamePair kp = this.allAddresses.get(ID);
		if(kp != null){
			return kp.getKey();
		}else{
			return null;
		}
	}
	
	protected synchronized KeyNamePair removeAddress(String ID)
	{
		return this.allAddresses.remove(ID);
	}
	
	protected synchronized ArrayList<KeyNamePair> getAllAddresses()
	{
		ArrayList<KeyNamePair> A = new ArrayList<KeyNamePair>();
		Iterator<KeyNamePair> it = this.allAddresses.values().iterator();
		while(it.hasNext()){
			A.add(it.next());
		}
		return A;
	}
	
	protected synchronized ConnectionChannelTaskManager findConnectionChannelTaskManager(String connectionID)
	{
		return this.connections.get(connectionID);
	}
	
	protected synchronized ArrayList<ConnectionChannelTaskManager> getAllConnectionChannelTaskManager()
	{
		ArrayList<ConnectionChannelTaskManager> A = new ArrayList<ConnectionChannelTaskManager>();
		Iterator<ConnectionChannelTaskManager> V = this.connections.values().iterator();
		while(V.hasNext()){
			A.add(V.next());
		}
		return A;
	}
	
	protected synchronized void addPublicKeyAddress(KeyNamePair knp)
	{
		this.allAddresses.put(UtilityMethods.getKeyString(knp.getKey()), knp);
	}
	
	protected synchronized void addConnectionChannel(ConnectionChannelTaskManager channel)
	{
		this.connections.put(channel.getConnectionChannelID(), channel);
	}
	
	protected synchronized KeyNamePair removeConnectionChannel(String channelID)
	{
		this.connections.remove(channelID);
		KeyNamePair kp = this.removeAddress(channelID);
		return kp;
	}
	
	public void addMessageIntoQueue(Message m)
	{
		this.messageQueue.add(m);
	}
	
    public static void updateGenesisBlock(Blockchain genesisBlock)
    {
    	if(BlockchainMessageServiceProvider.genesisBlockchain == null){
    		BlockchainMessageServiceProvider.genesisBlockchain = genesisBlock;
    	}
    }
    
    public static Blockchain getGenesisBlockchain(){
    	return BlockchainMessageServiceProvider.genesisBlockchain;
    }
}



//----------------- Inner class ---------------------------------
class ConnectionChannelTaskManager implements Runnable
{
    private Socket socket;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    boolean forever = true;
    // the connection clientID that marks which connection this
    // thread is working for
    private String ConnectionID = null;

    private BlockchainMessageServiceProvider server;
    private KeyPair keypair;
    private PublicKey delegatePublicKey;
    private String name = null;
 
    protected ConnectionChannelTaskManager(BlockchainMessageServiceProvider server, Socket s, KeyPair keypair)
    {
        this.server = server;
        this.socket = s;
        this.keypair = keypair;
       
        try{
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            //the server will first send the client its public key
            MessageID toClient = new MessageID(this.keypair.getPrivate(), this.keypair.getPublic(), "ServiceProvider");
            out.writeObject(toClient);
            out.flush();
            //the server will then wait for the client to send in a MessageForID
            MessageID mid = (MessageID)in.readObject();
            //now server examine if the connection is securely constructed
            if(!mid.isValid()){
            	throw new Exception("messageID is invalid. Something wrong.");
            }
			//store this connection ID
            this.delegatePublicKey = mid.getPublicKey();
			this.ConnectionID = UtilityMethods.getKeyString(mid.getPublicKey());
			this.name = mid.getName();
			System.out.println("connection successfully established for "+this.getDelegateName()+"|"+this.ConnectionID);
			server.addConnectionChannel(this);
			this.server.addPublicKeyAddress(mid.getKeyNamePair());
			System.out.println("adding address for " + mid.getKeyNamePair().getName()+", now send the genesis blockchain");
			//let the new user get the genesis block as its blockchain			
			MessageBlockchainPrivate mchain = new MessageBlockchainPrivate(BlockchainMessageServiceProvider.getGenesisBlockchain(),
				BlockchainMessageServiceProvider.getGenesisBlockchain().getGenesisMiner(), this.delegatePublicKey);
			out.writeObject(mchain);
        }catch(Exception e){
        	System.out.println("ConnectionChannelTaskManager exception: "+ e.getMessage());
        	System.out.println("This ConnectionChannelTaskManager connection failed");
        	System.out.println("aborting this connection now");
            this.activeClose();
        }
    }
    
    public String getDelegateName(){
    	return this.name;
    }
    
    public PublicKey getDelegateAddress(){
    	return this.delegatePublicKey;
    }

    protected synchronized boolean sendMessage(Message m)
    {
        try{
            out.writeObject(m);
            out.flush();
            return true;
        }catch(Exception ee){
        	return false;
        }
    }


    public void run()
    {
        int count = 0;
        while(forever){
            try{
              	Message m = (Message)in.readObject();
              	this.server.addMessageIntoQueue(m);
            }catch(Exception ie){
                //ie.printStackTrace();
                count++;
                // if the exception happened too many times,
                // I would like to close this thread
                if(count >= 3){
                	this.activeClose();
                }
            }   
        }
    }

    
    protected String getConnectionChannelID()
    {
        return this.ConnectionID;
    }


    private void activeClose()
    {
    	this.forever = false;
        try{
        	this.server.removeConnectionChannel(this.getConnectionChannelID());
            System.out.println("ConnectionChannelTaskManager: preparing to close connection: " + this.getDelegateName()+"|"+ this.getConnectionChannelID());
            MessageTextPrivate mc = new MessageTextPrivate(Message.TEXT_CLOSE, this.keypair.getPrivate(),
            								this.keypair.getPublic(), this.getDelegateName(), this.delegatePublicKey);
            this.sendMessage(mc);
            Thread.sleep(1000);      
            System.out.println("ConnectionChannelTaskManager "+this.getDelegateName() +" closed actively (" + this.getConnectionChannelID()+")");
            in.close();
            out.close();
            socket.close();
        }catch(Exception e){
            //e.printStackTrace();
        }
    }

    /**
     * This close action is initiated by the client side
     */
    protected void passiveClose()
    {
    	this.forever = false;
    	try{
    		this.server.removeConnectionChannel(this.getConnectionChannelID());
            in.close();
            out.close();
            socket.close();
            System.out.println("ConnectionChannelTaskManager closed passively (" + this.getConnectionChannelID()+")");
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

}


//-------------- Another Thread -------------------------------


class MessageCheckingTaskManager implements Runnable
{
    private boolean forever = true;
    private long sleepTime = 100;
    private BlockchainMessageServiceProvider server;
    private ConcurrentLinkedQueue<Message> messageQueue;
    private KeyPair keypair;

    protected MessageCheckingTaskManager(BlockchainMessageServiceProvider server, ConcurrentLinkedQueue<Message> messageQueue, KeyPair keypair)
    {
    	this.server = server;
    	this.messageQueue = messageQueue;
    	this.keypair = keypair;
    }

    public void run()
    {
        while(forever){
            try{
            	//checking if there is any message in the queue
            	if(this.messageQueue.isEmpty()){
            		Thread.sleep(this.sleepTime);
            	}else{
            		while(!this.messageQueue.isEmpty()){
            			Message m = this.messageQueue.poll();
            			processMessage(m);
            		}
            	}
            }catch(Exception e){
            	e.printStackTrace();
            }
        }
        
    }

    private void processMessage(Message m) throws Exception
    {
    	if(m == null){
    		return;
    	}
    	if(m.isForBroadcast()){
    		ArrayList<ConnectionChannelTaskManager> all = this.server.getAllConnectionChannelTaskManager();
    		for(int i=0; i<all.size(); i++){
    			all.get(i).sendMessage(m);
    		}
    		//}
    	}else if(m.getMessageType()==Message.TEXT_PRIVATE){
    		MessageTextPrivate mt = (MessageTextPrivate)m;
    		if(!mt.isValid()){
    			return;
    		}
    		
    		String text = null;
    		//checking the receiver. First of all, check if it is for the service provider
    		if(mt.getReceiver().equals(this.keypair.getPublic())){
    			text = mt.getMessageBody();
    			//there are only two types of service message for the service provider
    			if(text.equals(Message.TEXT_CLOSE)){
    				System.out.println(mt.getSenderName()+" left the system.");
        			ConnectionChannelTaskManager thread = this.server.findConnectionChannelTaskManager(UtilityMethods.getKeyString(mt.getSenderKey()));
        			if(thread != null){
        				thread.passiveClose();
        			}
    			}else if(text.equals(Message.TEXT_ASK_ADDRESSES)){
    				if(!mt.getSenderKey().equals(BlockchainMessageServiceProvider.getGenesisBlockchain().getGenesisMiner())){
    					System.out.println(mt.getSenderName()+" is asking for a list of users.");
    				}
        			ArrayList<KeyNamePair> addresses = this.server.getAllAddresses();
        			if(addresses.size() == 0){
        				return;
        			}
        			if(addresses.size() == 1){
        				KeyNamePair kp = addresses.get(0);
        				if(kp.getKey().equals(mt.getSenderKey())){
        					return;
        				}
        			}
        			ConnectionChannelTaskManager thread = this.server.findConnectionChannelTaskManager(UtilityMethods.getKeyString(mt.getSenderKey()));
        			if(thread != null){
        				MessageAddressPrivate mp = new MessageAddressPrivate(addresses);
        				thread.sendMessage(mp);
        			}
        		}else{
        			System.out.println("Garbage message for service provider found: "+ text);
        		}
    		}else{
    			ConnectionChannelTaskManager thread = this.server.findConnectionChannelTaskManager(UtilityMethods.getKeyString(mt.getReceiver()));
    			try{
    				thread.sendMessage(mt);
    			}catch(Exception et){
    				//do nothing
    			}
    		}
    	}else if(m.getMessageType() == Message.BLOCKCHAIN_PRIVATE){
    		//try to deliver this message to the proper receiver
    		System.out.println("forwarding a blockchain private message.");
    		MessageBlockchainPrivate mcp = (MessageBlockchainPrivate)m;
    		ConnectionChannelTaskManager thread = this.server.findConnectionChannelTaskManager(UtilityMethods.getKeyString(mcp.getReceiver()));
    		if(thread != null){
    			thread.sendMessage(mcp);
    		}
    	}else{
    		System.out.println("message type not supported currently, type="+m.getMessageType()+", object="+m.getMessageBody());
    	}
    }
    
    
    public void close()
    {
        forever = false;
    }


}




