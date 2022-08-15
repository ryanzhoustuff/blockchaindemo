package blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MinerGenesisMessageTaskManager extends MinerMessageTaskManager implements Runnable
{
	public static final int SELF_BLOCKS_TO_MINE_LIMIT = 2;
	private int blocksMined = 0;
	public static final int SIGN_IN_BONUS_USERS_LIMIT = 1000;
	private HashMap<String, KeyNamePair> users = new HashMap<String, KeyNamePair>();
	private WalletConnectionAgent agent;
	private final int signInBonus = 1000;
	private ArrayList<KeyNamePair> waitingListForSignInBonus = new ArrayList<KeyNamePair>();
	
	
	public MinerGenesisMessageTaskManager(WalletConnectionAgent agent, 
			Miner miner, ConcurrentLinkedQueue<Message> messageQueue)
	{
		super(agent, miner, messageQueue);
		this.agent = agent;
	}
	
	
	/**
	 * The genesis miner will poll the service relay provider for new users time to time
	 */
	public void whatToDo()
	{
		try{
			Thread.sleep(agent.sleepTime*10);
			if(waitingListForSignInBonus.size() == 0 && users.size()<SIGN_IN_BONUS_USERS_LIMIT){
				//For the genesis miner, it is the best practice to use wait-notify mechanism instead of polling
				//for the new coming users. For this version, maybe it is Ok to do so
				MessageTextPrivate mp = new MessageTextPrivate(Message.TEXT_ASK_ADDRESSES, 
						myWallet().getPrivateKey(), myWallet().getPublicKey(), myWallet().getName(),this.agent.getServerAddress());
				agent.sendMessage(mp);
				Thread.sleep(agent.sleepTime*10);
			}else{
				sendSignInBonus();
			}
		}catch(Exception e){}
	}
	
	private void sendSignInBonus()
	{
		if(waitingListForSignInBonus.size() <= 0){
			return;
		}
		
		KeyNamePair pk = waitingListForSignInBonus.remove(0);	
		Transaction T = myWallet().transferFund(pk.getKey(), signInBonus);
		if(T != null && T.verifySignature()){
			System.out.println(myWallet().getName()+" is sending "+ pk.getName() +" sign-in bonus of "+signInBonus);
			if(blocksMined < SELF_BLOCKS_TO_MINE_LIMIT && this.getMiningAction()){
				blocksMined++;
				this.raiseMiningAction();
				System.out.println(myWallet().getName()+" is mining the sign-in bonus block for " + pk.getName()+" by himself");
				ArrayList<Transaction> tss = new ArrayList<Transaction>();
				tss.add(T);
				MinerTheWorker worker = new MinerTheWorker(myWallet(), this, this.agent, tss);
				Thread miningThread = new Thread(worker);
				miningThread.start();
			}else{
				//broadcast this transaction
				System.out.println(myWallet().getName()+" is broadcasting the transaction of sign-in bonus for " + pk.getName());
				MessageTransactionBroadcast mtb = new MessageTransactionBroadcast(T);
				this.agent.sendMessage(mtb);
			}
		}else{
			//got to redo :(  this is impossible to happen
			waitingListForSignInBonus.add(0, pk);
		}
		
	}
	
	
	/**
	 * This is how a genesis Miner acts when receiving a Block
	 * @param mbb
	 */
	protected void receiveMessageBlockBroadcast(MessageBlockBroadcast mbb)
	{
		Block block = mbb.getMessageBody();
		boolean b = myWallet().verifyGuestBlock(block, myWallet().getLocalLedger());
		boolean c = false;
		if(b){
			c = this.myWallet().updateLocalLedger(block);
		}
		if(b && c){
			System.out.println("new block is added to the local blockchain, blockchain size = " 
							+ this.myWallet().getLocalLedger().size());
			displayWallet_MinerBalance(myWallet());
		}else{
			System.out.println("new block is rejected");
			//got to check if this block is a sign-in bonus block, if it is, then needs to 
			//re-mine it
			if(block.getCreator().equals(myWallet().getPublicKey())){
				System.out.println("genesis miner] needs to re-mine a sign-in bonus block");
				//got to redo :(  this is likely to happen if mining competition becomes tight
				String id = UtilityMethods.getKeyString(block.getTransaction(0).getOuputUTXO(0).getReceiver());
				KeyNamePair pk = users.get(id);
				if(pk != null){
					//add at the beginning, do not update blocksMined or miningAction
					//if over limit, let other miners to mine the sign-in bonus for the genesis miner
					waitingListForSignInBonus.add(0, pk);
				}else{
					System.out.println();
					System.out.println("ERROR: an existing user for sign-in bonus is not found. Program error");
					System.out.println();
				}
			}
		}
	}
	
	
	protected void receiveMessageTransactionBroadcast(MessageTransactionBroadcast mtb)
	{
		//ignore such message
	}
	
	protected void receiveMessageAddressPrivate(MessageAddressPrivate mp)
	{
		ArrayList<KeyNamePair> all = mp.getMessageBody();
		for(int z=0; z<all.size(); z++){
			KeyNamePair pk = all.get(z);
			String ID = UtilityMethods.getKeyString(pk.getKey());
			if(!pk.getKey().equals(myWallet().getPublicKey()) && !users.containsKey(ID)){
				users.put(ID, pk);
				if(users.size() <= SIGN_IN_BONUS_USERS_LIMIT){
					this.waitingListForSignInBonus.add(pk);
				}
				
			}
		}
	}
	
	protected void receivePrivateChatMessage(MessageTextPrivate m)
	{
		//do nothing for the genesis miner
	}
	
	protected void receiveMessageTextBroadcast(MessageTextBroadcast mtb)
	{
		//do nothing for the genesis miner
	}
	
	
	/**
	 * Overwrite it so that the genesis miner does not bother with such action
	 */
	protected void askForLatestBlockChain()
	{
		//
	}
	
	public static final void displayWallet_MinerBalance(Wallet miner)
	{
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		ArrayList<Transaction> ts = new ArrayList<Transaction>();
		double b = miner.getLocalLedger().findRelatedUTXOs(miner.getPublicKey(), all, spent, unspent, ts);
		System.out.println("{");
		System.out.println("\t"+miner.getName()+": balance="+b+",   local blockchain size="+miner.getLocalLedger().size());
		
		double income = 0;
		
		System.out.println("\tAll UTXOs:");
		for(int i=0; i<all.size(); i++){
			UTXO ux = all.get(i);
			
			System.out.println("\t\t"+ux.getFundTransferred()+"|"+ux.getHashID()
				+"|from="+UtilityMethods.getKeyString(ux.getSender())+"|to="+UtilityMethods.getKeyString(ux.getReceiver()));
			
			income += ux.getFundTransferred();
		}
		System.out.println("\t---- total income = " + income+" ----------");
		
		System.out.println("\tSpent UTXOs:");
		income = 0;
		for(int i=0; i<spent.size(); i++){
			UTXO ux = spent.get(i);
			
			System.out.println("\t\t"+ux.getFundTransferred()+"|"+ux.getHashID()
				+"|from="+UtilityMethods.getKeyString(ux.getSender())+"|to="+UtilityMethods.getKeyString(ux.getReceiver()));
			
			income += ux.getFundTransferred();
		}
		System.out.println("\t---- total spending = " + income+" ----------");
		double tsFee = ts.size() * Transaction.TRANSACTION_FEE;
		if(tsFee > 0){
			System.out.println("\t\tTransaction Fee "+tsFee+" is automatically deducted. Please not include it in the calculation");
		}
		System.out.println("\tUnspent UTXOs:");
		income = 0;		
		for(int i=0; i<unspent.size(); i++){
			UTXO ux = unspent.get(i);
			
			System.out.println("\t\t"+ux.getFundTransferred()+"|"+ux.getHashID()
				+"|from="+UtilityMethods.getKeyString(ux.getSender())+"|to="+UtilityMethods.getKeyString(ux.getReceiver()));
			
			income += ux.getFundTransferred();
		}
		System.out.println("\t---- total unspent = " + income+" ----------");
		
		System.out.println("}");
	}
}
