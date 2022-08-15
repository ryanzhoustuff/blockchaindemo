package blockchain;

import java.util.ArrayList;

/**
 * This is a thread dedicated to mine a block. As mining is a time consuming and very competitive  process
 * dedicating a thread to do it is reasonable, especially considering this scenario:
 * A is mining a block, while B has finished and broadcasted the same block. If there is no dedicated thread,
 * then A is blocked in his own mining and would not know that the block has already been finished by others.
 * @author hzhou
 *
 */
public class MinerTheWorker implements Runnable
{
	private Miner miner;
	private WalletConnectionAgent agent;
	private MinerMessageTaskManager manager;
	private boolean goon = true;
	private ArrayList<Transaction> existingTransactions = null;
	
	public MinerTheWorker(Miner miner, MinerMessageTaskManager manager, WalletConnectionAgent agent, 
													ArrayList<Transaction> existingTransactions)
	{
		this.miner = miner;
		this.agent = agent;
		this.existingTransactions = existingTransactions;
		this.manager = manager;
	}
	
	public void run()
	{
		final long breakTime = 2;
		System.out.println("Miner "+miner.getName()+" begins to mine a block. Competition starts.");
		Block block = miner.createNewBlock(miner.getLocalLedger(), Configuration.blockMiningDifficultyLevel());
		for(int i=0; i<this.existingTransactions.size(); i++){
			miner.addTransaction(this.existingTransactions.get(i), block);
		}
		//reward the miner
		miner.generateRewardTransaction(block);
		try{
			Thread.sleep(breakTime);
		}catch(Exception e1){
			
		}
		//check if we should continue
		if(!goon){
			manager.resetMiningAction();
			return;
		}
		//mine the block
		boolean b = miner.mineBlock(block);
		if(b){
			System.out.println(miner.getName()+" mined and signed the block, hashID is:");
			System.out.println(block.getHashID());
		}else{
			System.out.println(miner.getName()+" failed to mine the block, mission aborted");
			this.existingTransactions.clear();
			manager.resetMiningAction();
			return;
		}
		try{
			Thread.sleep(breakTime);
		}catch(Exception e2){
			
		}
		//check if we should continue
		if(!goon){
			manager.resetMiningAction();
			return;
		}
		//to make it fair, the miner needs to announce the block first.
		//the miner should not just go ahead to update his local ledger. He needs to send this block into 
		//the public pool and compete. He only updates his local ledger with this block 
		//if this block comes back first from the public pool.
		MessageBlockBroadcast mbbc = new MessageBlockBroadcast(block);
		this.agent.sendMessage(mbbc);
		manager.resetMiningAction();
	}
	
	
	protected void abort(){
		this.goon = false;
	}
}
