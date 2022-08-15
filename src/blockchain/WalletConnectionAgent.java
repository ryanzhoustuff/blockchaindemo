package blockchain;

import java.security.PublicKey;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.Socket;

public class WalletConnectionAgent implements Runnable
{
	private Wallet wallet;
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private PublicKey serverAddress;
	private ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<Message>();
    private Hashtable<String, KeyNamePair> allAddresses = new Hashtable<String, KeyNamePair>();
	private boolean forever = true;
	public final long sleepTime = 100;
	
	public WalletConnectionAgent(String host, int port, Wallet wallet)
	{
		this.wallet = wallet;
		System.out.println("Begin to create agent for network communication");
		try{
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            //the agent will get a MessageForID from the service provider
            MessageID fromServer = (MessageID)in.readObject();
            //make sure that the message is in good standing
            if(fromServer.isValid()){
            	this.serverAddress = fromServer.getPublicKey();
            }else{
            	throw new Exception("MessageID from service provider is invalid.");
            }
            //if everything works well, the agent sends the server a responding MessageForID
            //because the server is waiting for the client to send in a MessageForID
            System.out.println("obtained server address and stored it, now sending wallet public key to server");
            System.out.println("name="+this.wallet.getName());
            MessageID mid = new MessageID(this.wallet.getPrivateKey(), this.wallet.getPublicKey(), this.wallet.getName());
            out.writeObject(mid);
            //now, expecting for the genesis blockchain
            MessageBlockchainPrivate mbcp = (MessageBlockchainPrivate)in.readObject();
            this.wallet.setLocalLedger(mbcp.getMessageBody());
            System.out.println("The genesis block chain set, everything ready ...");
		}catch(Exception e){
			System.out.println("WalletConnectionAgent: creation failed because|"+e.getMessage());
			System.out.println("Please restart");
			System.exit(1);
		}
	}
	
	
	public void run()
	{
		try{
			Thread.sleep(this.sleepTime);
		}catch(Exception er){
			
		}
		while(forever){
			try{
				Message m = (Message)in.readObject();
				this.messageQueue.add(m);
				Thread.sleep(this.sleepTime);
			}catch(Exception e){
				forever = false;
			}
		}
	}
	
	public synchronized boolean sendMessage(Message m)
	{
		//double ensure that all null message is not sent
		if(m == null){
			System.out.println("message is null, cannot send");
			return false;
		}
		try{
			this.out.writeObject(m);
			return true;
		}catch(Exception e){
			System.out.println("failed to send message [" + e.getMessage());
			return false;
		}
	}
	
	
	public void activeClose()
	{
		MessageTextPrivate mc = new MessageTextPrivate(Message.TEXT_CLOSE, this.wallet.getPrivateKey(),
				this.wallet.getPublicKey(), this.wallet.getName(), this.getServerAddress());
		this.sendMessage(mc);
		try{
			Thread.sleep(this.sleepTime);
		}catch(Exception ee){
			
		}
		this.close();
	}
	
	public void close()
	{
		this.forever = false;
		try{
			this.in.close();
			this.out.close();
		}catch(Exception e){
			
		}
	} 
	
	public ArrayList<KeyNamePair> getAllStoredAddresses()
	{
		Iterator<KeyNamePair> E = this.allAddresses.values().iterator();
		ArrayList<KeyNamePair> A = new ArrayList<KeyNamePair>();
		while(E.hasNext()){
			A.add(E.next());
		}
		return A;
	}
	
	public void addAddress(KeyNamePair address){
		this.allAddresses.put(UtilityMethods.getKeyString(address.getKey()), address);
	}
	
	public String getNameFromAddress(PublicKey key)
	{
		//if the key is self, then return the wallet's name
		if(key.equals(this.wallet.getPublicKey())){
			return this.wallet.getName();
		}
		String address = UtilityMethods.getKeyString(key);
		KeyNamePair kp = this.allAddresses.get(address);
		if(kp != null){
			return kp.getName();
		}else{
			return address;
		}
	}
	
	public PublicKey getServerAddress(){
		return this.serverAddress;
	}
	
	protected ConcurrentLinkedQueue<Message> getMessageQueue(){
		return this.messageQueue;
	}
	
	protected boolean sendTransaction(PublicKey receiver, double fundToTransfer)
	{
		Transaction T = this.wallet.transferFund(receiver, fundToTransfer);
		if(T != null && T.verifySignature()){
			MessageTransactionBroadcast m = new MessageTransactionBroadcast(T);
			this.sendMessage(m);
			return true;
		}
		return false;
	}
	
	protected boolean sendPrivateMessage(PublicKey receiver, String text)
	{
		MessageTextPrivate m = new MessageTextPrivate(text, 
				this.wallet.getPrivateKey(), this.wallet.getPublicKey(),
				this.wallet.getName(), receiver);
		this.sendMessage(m);
		return true;
	}
}



