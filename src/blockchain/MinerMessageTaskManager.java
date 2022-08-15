package blockchain;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MinerMessageTaskManager extends WalletMessageTaskManager implements Runnable
{
	private boolean miningAction = true;
	private ArrayList<Transaction> existingTransactions = new ArrayList<Transaction>();
	private WalletConnectionAgent agent;
	
	public MinerMessageTaskManager(WalletConnectionAgent agent, Miner miner, 
			ConcurrentLinkedQueue<Message> messageQueue)
	{
		super(agent, miner, messageQueue);
		this.agent = agent;
	}
	
	protected synchronized void resetMiningAction(){
		this.miningAction = true;
	}
	
	protected synchronized boolean getMiningAction(){
		return this.miningAction;
	}
	
	protected synchronized void raiseMiningAction(){
		this.miningAction = false;
	}
	
	protected void receiveQueryForBlockchainBroadcast(MessageAskForBlockchainBroadcast mabcb)
	{
		PublicKey receiver = mabcb.getSenderKey();
		Blockchain bc = myWallet().getLocalLedger().copy_NotDeepCopy();
		
		MessageBlockchainPrivate message = new MessageBlockchainPrivate(bc, 	
						myWallet().getPublicKey(), receiver);
		boolean b = this.agent.sendMessage(message);
		if(b){
			System.out.println(myWallet().getName()+": sent local blockchain to the requester, chain size="
								+message.getMessageBody().size() + "|"+message.getInfoSize());
		}else{
			System.out.println(myWallet().getName()+": failed to send local blockchain to the requester");
		}
	}
	
	
	protected void receiveMessageTransactionBroadcast(MessageTransactionBroadcast mtb)
	{
		Transaction ts = mtb.getMessageBody();
		//first, make sure that this transaction does not exist in the current pool
		for(int i=0; i<this.existingTransactions.size(); i++){
			if(ts.equals(this.existingTransactions.get(i))){
				return;
			}
		}
		
		//add this into the existing storage
		if(!myWallet().validateTransaction(ts)){
			System.out.println("Miner "+ myWallet().getName()+" found an invalid transaction. Should broadcast it though");
			return;
		}
		this.existingTransactions.add(ts);
		//check if it meets the requirement to build a block
		if(this.existingTransactions.size() >= Block.TRANSACTION_LOWER_LIMIT 
				&& this.getMiningAction() ){
			this.raiseMiningAction();
			System.out.println(myWallet().getName()+" has enough transactions to mine the block now, " 
					+"mining_action_block_size requirement meets. Start mining a new block");
			MinerTheWorker worker = new MinerTheWorker(myWallet(), this, this.agent, this.existingTransactions);
			Thread miningThread = new Thread(worker);
			miningThread.start();
			//once the mining starts, we need to be ready to take in new incoming Transactions
			this.existingTransactions = new ArrayList<Transaction>(); 		
		}
	}
	

	
	
	
	protected Miner myWallet(){
		return (Miner)(super.myWallet());
	}
}

