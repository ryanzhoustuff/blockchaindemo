package blockchain;

public class Miner extends Wallet
{
	public Miner(String minerName, String password) 
	{
		super(minerName, password);
	}
	
	public Miner(String minerName)
	{ 
		super(minerName);
	}
	
	public boolean mineBlock(Block block)
	{
		if((block.mineTheBlock(this.getPublicKey()))){
			//now the miner needs to sign the block
			byte[] signature = UtilityMethods.generateSignature(this.getPrivateKey(), block.getHashID());
			return block.signTheBlock(this.getPublicKey(), signature);
		}else{
			return false;
		}
	}
	
	public boolean addTransaction(Transaction ts, Block block)
	{
		if(this.validateTransaction(ts)){
			return block.addTransaction(ts, this.getPublicKey());
		}else{
			return false;
		}
	}

	
	public boolean deleteTransaction(Transaction ts, Block block)
	{
		return block.deleteTransaction(ts, this.getPublicKey());
	}
	
	
	public boolean generateRewardTransaction(Block block)
	{
		double amount = Blockchain.MINING_REWARD+block.getTransactionFeeAmount();
		Transaction T = new Transaction(this.getPublicKey(), this.getPublicKey(), amount, null);
		UTXO ut = new UTXOAsMiningReward(T.getHashID(), T.getSender(), this.getPublicKey(), amount);
		T.addOutputUTXO(ut);
		T.signTheTransaction(this.getPrivateKey());
		return block.generateRewardTransaction(this.getPublicKey(), T);
	}
	

	
	public Block createNewBlock(Blockchain ledger, int difficultLevel)
	{
		Block b = new Block(ledger.getLastBlock().getHashID(), difficultLevel, this.getPublicKey());
		return b;
	}	
	
}
