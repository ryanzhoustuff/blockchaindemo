package blockchain;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain implements java.io.Serializable
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	public static final double MINING_REWARD = 100.0;
	private LedgerList<Block> blockchain; 
	
	public Blockchain(Block genesisBlock)
	{
		this.blockchain = new LedgerList<Block>();
		this.blockchain.add(genesisBlock);
	}

	
	public Block getGenesisBlock(){
		return this.blockchain.getFirst();
	}
	
	public Block getLastBlock()
	{
		return this.blockchain.getLast();
	}
	
	public int size(){
		return this.blockchain.size();
	}
	
	public Block getBlock(int index){
		return this.blockchain.findByIndex(index);
	}
	
	public double checkBalance(PublicKey key)
	{
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		return findRelatedUTXOs(key, all, spent, unspent);
	}
	
	
	public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all, ArrayList<UTXO> spent, 
			ArrayList<UTXO> unspent, ArrayList<Transaction> sentTransactions, ArrayList<UTXO> rewards)
	{
		double gain = 0.0, spending = 0.0;
		HashMap<String, UTXO> map = new HashMap<String, UTXO>();
		int limit = this.size();
		for(int a=0; a<limit; a++){
			Block block = this.blockchain.findByIndex(a);
			int size = block.getTotalNumberOfTransactions();
			for(int i=0; i<size; i++){
				Transaction T = block.getTransaction(i);
				int N;
				if(a != 0 && T.getSender().equals(key)){
					N = T.getNumberOfInputUTXOs();
					for(int x=0; x<N; x++){
						UTXO ut = T.getInputUTXO(x);
						spent.add(ut);
						map.put(ut.getHashID(), ut);
						spending += ut.getFundTransferred();
					}
					sentTransactions.add(T);
				}
				
				N = T.getNumberOfOutputUTXOs();
				for(int x=0; x<N; x++){
					UTXO ux = T.getOuputUTXO(x);
					if(ux.getReceiver().equals(key)){
						all.add(ux);
						gain += ux.getFundTransferred();
					}
				}
			}
			//add reward transactions. The reward might be null since a miner might underpay himself
			if(block.getCreator().equals(key)) {
				Transaction rt = block.getRewardTransaction();
				if(rt != null && rt.getNumberOfOutputUTXOs()>0){
					UTXO ux = rt.getOuputUTXO(0);
					//double check again, so a miner can only reward himself
					//if he rewards others, this reward is not counted
					if(ux.getReceiver().equals(key)){
						rewards.add(ux);
						all.add(ux);
						gain += ux.getFundTransferred();
					}
				}
			}
		}
		for(int i=0; i<all.size();i++){
			UTXO ut = all.get(i);
			if(!map.containsKey(ut.getHashID())){
				unspent.add(ut);
			}
		}
		return (gain - spending);
	}
	
	
	public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all, ArrayList<UTXO> spent, 
			ArrayList<UTXO> unspent, ArrayList<Transaction> sentTransactions) {
		ArrayList<UTXO> rewards = new ArrayList<UTXO>();
		return findRelatedUTXOs(key, all, spent, unspent, sentTransactions, rewards);
	}

	
	public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all, ArrayList<UTXO> spent, ArrayList<UTXO> unspent)
	{
		ArrayList<Transaction> sendingTransactions = new ArrayList<Transaction>();
		return findRelatedUTXOs(key, all, spent, unspent, sendingTransactions);
	}
	
	
	public ArrayList<UTXO> findUnspentUTXOs(PublicKey key)
	{
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		findRelatedUTXOs(key, all, spent, unspent);
		return unspent;
	}
	
	public double findUnspentUTXOs(PublicKey key, ArrayList<UTXO> unspent)
	{
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		return findRelatedUTXOs(key, all, spent, unspent);
	}
	
	
	/**
	 * Only check to the block before the genesis block
	 * @param T
	 * @return
	 */
	protected boolean isTransactionExist(Transaction T)
	{
		int size = this.blockchain.size();
		for(int i=size-1; i>0; i--){
			Block b = this.blockchain.findByIndex(i);
			int bs = b.getTotalNumberOfTransactions();
			for(int j=0; j<bs; j++){
				Transaction t2 = b.getTransaction(j);
				if(T.equals(t2)){
					return true;
				}
			}
		}
		return false;
	}
	
	public PublicKey getGenesisMiner(){
		return this.getGenesisBlock().getCreator();
	}
	

	public static boolean validateBlockchain(Blockchain ledger)
	{
		int size = ledger.size();
		for(int i = size-1; i>0; i--){
			Block currentBlock = ledger.getBlock(i);
			boolean b = currentBlock.verifySignature(currentBlock.getCreator());
			if(!b){
				System.out.println("validateBlockChain(): block "+(i+1)+"  signature is invalid.");
				return false;
			}
			b = UtilityMethods.hashMeetsDifficultyLevel(currentBlock.getHashID(), currentBlock.getDifficultyLevel())
					&& currentBlock.computeHashID().equals(currentBlock.getHashID());
			if(!b){
				System.out.println("validateBlockChain():  block  "+(i+1)+"  its hashing is bad");
				return false;
			}
			Block previousBlock = ledger.getBlock(i-1);
			b = currentBlock.getPreviousBlockHashID().equals(previousBlock.getHashID());
			if(!b){
				System.out.println("validateBlockChain():  block  "+(i+1)+"  invalid previous block hashID");
				return false;
			}
		}
		Block genesisBlock = ledger.getGenesisBlock();
		//confirm the genesis is signed
		boolean b2 = genesisBlock.verifySignature(genesisBlock.getCreator());
		if(!b2){
			System.out.println("validateBlockChain():  genesis block is tampered, signature bad");
			return false;
		}
		
		b2 = UtilityMethods.hashMeetsDifficultyLevel(genesisBlock.getHashID(), genesisBlock.getDifficultyLevel())
				&& genesisBlock.computeHashID().equals(genesisBlock.getHashID());
		if(!b2){
			System.out.println("validateBlockChain(): gensis block is hashing is bad");
			return false;
		}
		return true;
	}

	public synchronized boolean addBlock(Block block)
	{
		if(this.size() == 0){
			this.blockchain.add(block);
			return true;
		}else if(block.getPreviousBlockHashID().equals(this.getLastBlock().getHashID())){
			this.blockchain.add(block);
			return true;
		}else{
			return false;
		}
	}
	
	
	private Blockchain(LedgerList<Block> chain)
	{
		this.blockchain = new LedgerList<Block>();
		int size = chain.size();
		for(int i=0; i<size; i++){
			this.blockchain.add(chain.findByIndex(i));
		}
	}
	
	public synchronized Blockchain copy_NotDeepCopy(){
		return new Blockchain(this.blockchain);
	}
}
