package blockchain;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;

public class WalletMessageTaskManager implements Runnable
{
	private boolean forever = true;
	private WalletConnectionAgent agent;
	private Wallet wallet;
	private ConcurrentLinkedQueue<Message> messageQueue;
	private HashMap<String, String> thankYouTransactions = new HashMap<String, String>();
	private WalletSimulator simulator = null;
	
	public WalletMessageTaskManager(WalletConnectionAgent agent, Wallet wallet, 
			ConcurrentLinkedQueue<Message> messageQueue)
	{
		this.agent = agent;
		this.wallet = wallet;
		this.messageQueue = messageQueue;
	}
	
	public void setSimulator(WalletSimulator simulator){
		this.simulator = simulator;
	}
	
	protected void askForLatestBlockchain()
	{
		//The first job to do is to update the local ledger
		MessageAskForBlockchainBroadcast forLedger = new MessageAskForBlockchainBroadcast("Thank you", 
				this.wallet.getPrivateKey(), this.wallet.getPublicKey(), this.wallet.getName());
		boolean b = this.agent.sendMessage(forLedger);
		if(b){
			System.out.println("sent a message for latest blockchain");
		}else{
			System.out.println("Error!!!! failed to send a message for latest blockchain");
		}
	}
	
	
	public void whatToDo()
	{
		//do nothing
	}
	
	public void run()
	{
		try{
			//sleep a while to wait for the agent to complete
			Thread.sleep(agent.sleepTime*2);
		}catch(Exception ee){}
		
		askForLatestBlockchain();
		
		while(forever){
			if(this.messageQueue.isEmpty()){
				try{
					Thread.sleep(this.agent.sleepTime);
					whatToDo();
				}catch(Exception e){
					System.out.println("Error in sleep");
					e.printStackTrace();
					this.close();
					this.agent.activeClose();
				}
			}else{
				Message m = this.messageQueue.poll();
				if(m == null){
					System.out.println("message  is null, impossible!");
				}else{
					try{
						processMessage(m);
					}catch(Exception e){
						System.out.println("Error when processing message");
						e.printStackTrace();
						this.close();
						this.agent.activeClose();
					}
				}
			}
		}
	}
	
	protected void processMessage(Message message) //throws Exception
	{
    	if(message == null){
    		return;
    	}
		if(!message.isForBroadcast()){
			//this is a private message for this wallet
			//it can be a text message
			if(message.getMessageType() == Message.TEXT_PRIVATE){
				//got to confirm the message
				MessageTextPrivate m = (MessageTextPrivate)message;
				if(!m.isValid()){
					System.out.println("text private message tampered");
					return;
				}
				//check if the message is really for me
				if(!m.getReceiver().equals(this.wallet.getPublicKey())){
					System.out.println("text private is not for me, ignore it.");
					return;
				}
				//check if it is a CLOSE connection message
				//String text = UtilityMethods.decryptMessage(wallet.getPrivateKey(), m.getMessageBody());
				String text = m.getMessageBody();
				if(m.getSenderKey().equals(agent.getServerAddress()) && text.equals(Message.TEXT_CLOSE)){
					System.out.println("Server is asking to close the connection. Closing now ...");
					//close everything
					this.close();
					agent.close();
				}else{
					receivePrivateChatMessage(m);
				}
			}else if(message.getMessageType() == Message.ADDRESS_PRIVATE){
				MessageAddressPrivate mp = (MessageAddressPrivate)message;
				receiveMessageAddressPrivate(mp);
			}else if(message.getMessageType() == Message.BLOCKCHAIN_PRIVATE){
				MessageBlockchainPrivate mbcb = (MessageBlockchainPrivate)message;
				receiveMessagaeBlockchainPrivate(mbcb);
			}else{
				System.out.println("");
				System.out.println("....weird private message, not supported, please check ......");
				System.out.println("");
			}
		//now, about those broad messages
		}else if(message.getMessageType() == Message.BLOCK_BROADCAST){
			//when a wallet gets a block, he will validate it and then try to update it
			System.out.println("it is a block broadcast message, check if it is necessary to update it");
			MessageBlockBroadcast mbb = (MessageBlockBroadcast)message;
			this.receiveMessageBlockBroadcast(mbb);
		}else if(message.getMessageType() == Message.BLOCKCHAIN_BROADCAST){
			System.out.println("It is a blockchain broadcast message, check if it is necessary to update the blockchain");
			MessageBlockchainBroadcast mbcb = (MessageBlockchainBroadcast)message;
			boolean b = this.wallet.setLocalLedger(mbcb.getMessageBody());
			if(b){
				System.out.println("blockchain is updated!");
			}else{
				System.out.println("rejected the new blockchain");
			}
		}else if(message.getMessageType() == Message.TRANSACTION_BROADCAST){
			//as a wallet does not collect transaction or mine a block, a wallet will just pay attention to the
			//transaction that has payment to herself/himself
			System.out.println("It is a transaction broadcast message");
			MessageTransactionBroadcast mtb = (MessageTransactionBroadcast)message;
			this.receiveMessageTransactionBroadcast(mtb);
		}else if(message.getMessageType() == Message.BLOCKCHAIN_ASK_BROADCAST){
			MessageAskForBlockchainBroadcast mabcb = (MessageAskForBlockchainBroadcast)message;
			if(!(mabcb.getSenderKey().equals(myWallet().getPublicKey())) && mabcb.isValid()){
				receiveQueryForBlockchainBroadcast(mabcb);
			}
		}else if(message.getMessageType() == Message.TEXT_BROADCAST){
			MessageTextBroadcast mtb = (MessageTextBroadcast)message;
			receiveMessageTextBroadcast(mtb);
		}
	}
	
	protected void receiveMessageTextBroadcast(MessageTextBroadcast mtb)
	{
		String text = mtb.getMessageBody();
		String name = mtb.getSenderName();
		this.simulator.appendMessageLineOnBoard(name+"]: "+text);
		//Automatically store the user information (can be self)
		agent.addAddress(new KeyNamePair(mtb.getSenderKey(), mtb.getSenderName()));
	}
	
	protected void receiveMessageAddressPrivate(MessageAddressPrivate mp)
	{
		ArrayList<KeyNamePair> all = mp.getMessageBody();
		System.out.println("There are these many addresses (users) available (in addition to yourself): ");
		for(int z=0; z<all.size(); z++){
			KeyNamePair pk = all.get(z);
			if(!pk.getKey().equals(wallet.getPublicKey())){
				agent.addAddress(pk);
				System.out.println(pk.getName()+"| key="+UtilityMethods.getKeyString(pk.getKey()));
			}
		}
	}
	
	protected void receivePrivateChatMessage(MessageTextPrivate m)
	{
		String text = m.getMessageBody();
		String name = m.getSenderName();
		this.simulator.appendMessageLineOnBoard("private<--"+name+"]: "+text);
		//Automatically store the user information
		agent.addAddress(new KeyNamePair(m.getSenderKey(), m.getSenderName()));
	}
	
	
	/**
	 * For a wallet, it ignore such a message
	 * @param mabcb
	 */
	protected void receiveQueryForBlockchainBroadcast(MessageAskForBlockchainBroadcast mabcb)
	{
		System.out.println("I am just a wallet, ignore query for block chain");
	}
	
	
	/**
	 * This is the version for Wallet only. The wallet can either ignore it or 
	 * send a THNAK YOU message to the transaction publisher.
	 * @param mtb
	 */
	protected void receiveMessageTransactionBroadcast(MessageTransactionBroadcast mtb)
	{
		Transaction ts = mtb.getMessageBody();
		if(!this.thankYouTransactions.containsKey(ts.getHashID())){
			int n = ts.getNumberOfOutputUTXOs();
			int total = 0;
			for(int i=0; i<n; i++){
				UTXO ut = ts.getOuputUTXO(i);
				if(ut.getReceiver().equals(this.wallet.getPublicKey())){
					total += ut.getFundTransferred();
				}
			}
			//if the UTXO sender is self, do not display this message
			if(total > 0 && !ts.getSender().equals(myWallet().getPublicKey())){
				this.thankYouTransactions.put(ts.getHashID(), ts.getHashID());
				System.out.println("in the transaction, there is payment of " + total+" to me. Sending THANK YOU to the payer");
				MessageTextPrivate mtp = new MessageTextPrivate("Thank you for the fund of "+total+", waiting for its publishing.",
						this.wallet.getPrivateKey(), this.wallet.getPublicKey(), this.wallet.getName(), ts.getSender());
				this.agent.sendMessage(mtp);
				
			}
		}
	}
	
	protected void receiveMessageBlockBroadcast(MessageBlockBroadcast mbb)
	{
		Block block = mbb.getMessageBody();
		boolean b = this.wallet.updateLocalLedger(block);
		if(b){
			System.out.println("new block is added to the local blockchain");
		}else{
			int size = block.getTotalNumberOfTransactions();
			int counter = 0;
			for(int i=0; i<size; i++){
				Transaction T = block.getTransaction(i);
				if(!myWallet().getLocalLedger().isTransactionExist(T)){
					MessageTransactionBroadcast mt = new MessageTransactionBroadcast(T);
					this.agent.sendMessage(mt);
					counter++;
				}
			}
			System.out.println("new block is rejected, released "+ counter+" unpublished transactions into the pool");
		}
	}
	
	protected void receiveMessagaeBlockchainPrivate(MessageBlockchainPrivate mbcb)
	{
		System.out.println("It is a blockchain private message, check if it is for me and if necessary to update the blockchain");
		if(mbcb.getReceiver().equals(myWallet().getPublicKey())){
			boolean b = this.myWallet().setLocalLedger(mbcb.getMessageBody());
			if(b){
				System.out.println("blockchain is updated!");
			}else{
				System.out.println("rejected the new blockchain");
			}
		}else{
			System.out.println("ERROR!!! weired, it is a blockchain private message, but it is sent to me!");
		}
	}
	
	protected Wallet myWallet(){
		return this.wallet;
	}
	
	public void close()
	{
		forever = false;
	}
}
