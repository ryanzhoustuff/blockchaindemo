package blockchain;

import java.security.PublicKey;
import java.util.ArrayList;

public class Block implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	//to limit the number of transactions inside a block
	public final static int TRANSACTION_UPPER_LIMIT = 100; 
	//to set a lower limit for demonstration purpose
	public final static int TRANSACTION_LOWER_LIMIT = 2;
	private int difficultyLevel = 20;
	private ArrayList<Transaction> transactions = new ArrayList<Transaction>();
	private long timestamp;
	private String previousBlockHashID;
	private int nonce = -1;
	private String hashID;	
	//to mark the miner who creates this block
	private PublicKey creator;
	//to mark if the block has been mined. Once a block has been mined, then no change is allowed
	private boolean mined = false;
	//the miner must sign the block
	private byte[] signature = null;
	//the transaction to reward the miner
	private Transaction rewardTransaction = null;
 
	public Block(String previousBlockHashID, int difficultyLevel, PublicKey creator){
		this.previousBlockHashID = previousBlockHashID;
		this.timestamp = UtilityMethods.getTimeStamp();
		this.difficultyLevel = difficultyLevel;
		this.creator = creator;
	}

	protected String computeHashID()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.previousBlockHashID + Long.toHexString(this.timestamp));
		sb.append(this.computeMerkleRoot());
		sb.append(""+nonce);
		byte[] b = UtilityMethods.messageDigestSHA256_toBytes(sb.toString());
		return UtilityMethods.toBinaryString(b);
	}

	public boolean addTransaction(Transaction t, PublicKey key)
	{
		if(this.getTotalNumberOfTransactions() >= Block.TRANSACTION_UPPER_LIMIT){
			return false;
		}
		if(key.equals(this.getCreator()) && !this.isMined() && !this.isSigned()){
			this.transactions.add(t);
			return true;
		}else{
			return false;
		}
	}

	public String getHashID(){
		return this.hashID;
	}
 
	public int getNonce(){
		return this.nonce;
	}

	public long getTimeStamp(){
		return this.timestamp;
	}
 
	public String getPreviousBlockHashID(){
		return this.previousBlockHashID;
	}

	public boolean mineTheBlock(PublicKey key)
	{
		if(!this.mined && key.equals(this.getCreator())){
			this.hashID = this.computeHashID();
			while(!UtilityMethods.hashMeetsDifficultyLevel(this.hashID, this.difficultyLevel)){
				nonce++;
				this.hashID = this.computeHashID();
			}
			this.mined = true;
		}
		return this.mined;
	}
	
	public int getDifficultyLevel(){
		return this.difficultyLevel;
	}
	
	public int getTotalNumberOfTransactions(){
		return this.transactions.size();
	}
	
	public Transaction getTransaction(int index)
	{
		return this.transactions.get(index);
	}
	
	public boolean generateRewardTransaction(PublicKey pubKey, Transaction rewardTransaction)
	{
		if(this.rewardTransaction == null && pubKey.equals(this.creator)){
			this.rewardTransaction = rewardTransaction;
			return true;
		}else{
			return false;
		}
	}
	
	public Transaction getRewardTransaction(){
		return this.rewardTransaction;
	}
	
	public double getTransactionFeeAmount(){
		return this.transactions.size() * Transaction.TRANSACTION_FEE;
	}
	
	public boolean verifySignature(PublicKey pubKey)
	{
		return UtilityMethods.verifySignature(pubKey, this.signature, this.getHashID());
	}
	
	public boolean signTheBlock(PublicKey pubKey, byte[] signature)
	{
		if(!isSigned()){
			if(pubKey.equals(this.creator)){
				if(UtilityMethods.verifySignature(pubKey, signature, this.getHashID())){
					this.signature = signature;
					return true;
				}
			}
		}
		return false;
	}
	
	public PublicKey getCreator(){
		return this.creator;
	}
	
	public boolean isMined(){
		return this.mined;
	}
	
	public boolean isSigned(){
		return this.signature != null;
	}
	
	private String computeMerkleRoot()
	{
		String[] hashes;
		if(this.rewardTransaction == null){
			hashes = new String[this.transactions.size()];
			for(int i=0; i<this.transactions.size(); i++){
				hashes[i] = this.transactions.get(i).getHashID();
			}
		}else{
			hashes = new String[this.transactions.size()+1];
			for(int i=0; i<this.transactions.size(); i++){
				hashes[i] = this.transactions.get(i).getHashID();
			}
			hashes[hashes.length-1] = this.rewardTransaction.getHashID();
		}
		return UtilityMethods.computeMerkleTreeRootHash(hashes);
	}
	
	public boolean deleteTransaction(Transaction ts, PublicKey key)
	{
		if(!this.mined && !this.isSigned() && key.equals(this.getCreator())){
			return this.transactions.remove(ts);
		}else{
			return false;
		}
	}
	
	public boolean deleteTransaction(int index, PublicKey key)
	{
		if(!this.mined && !this.isSigned() && key.equals(this.getCreator())){
			Transaction ts = this.transactions.remove(index);
			return (ts != null);
		}else{
			return false;
		}
	}
	
	public boolean equals(Block b){
		return this.getHashID().equals(b.getHashID());
	}
}


