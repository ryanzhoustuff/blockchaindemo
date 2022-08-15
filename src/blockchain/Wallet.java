package blockchain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Wallet 
{
	private KeyPair keyPair;
	private String walletName;
	private Blockchain localLedger = null;
	
	public Wallet(String walletName, String password)
	{
		this.keyPair = UtilityMethods.generateKeyPair();
		this.walletName = walletName;
		try{
			populateExistingWallet(walletName, password);
			System.out.println("A wallet exists with the same name and password. Loaded the existing wallet");
		}catch(Exception ee){
			try{
				this.prepareWallet(password);
				System.out.println("Created a new wallet based on the name and password");
			}catch(IOException ioe){
				throw new RuntimeException(ioe);
			}
		}
	}
	
	public Wallet(String walletName)
	{
		this.keyPair = UtilityMethods.generateKeyPair();
		this.walletName = walletName;
	}
	
	public String getName()
	{
		return this.walletName;
	}

	public PublicKey getPublicKey()
	{
		return this.keyPair.getPublic();
	}
	

	protected PrivateKey getPrivateKey()
	{
		return this.keyPair.getPrivate();
	}	
	
	private void prepareWallet(String password) throws IOException,FileNotFoundException
	{
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bo);
		out.writeObject(this.keyPair);
		//let's use XOR encryption first
		byte[] keyBytes = UtilityMethods.encryptionByXOR(bo.toByteArray(), password);
		//byte[] keyBytes = UtilityMethods.encryptionByAES(bo.toByteArray(), password);
		//now, write this into a file
		File F = new File(Configuration.keyLocation());
		if(!F.exists()){
			F.mkdir();
		}
		FileOutputStream fout = new FileOutputStream(Configuration.keyLocation()+"/"+this.getName()+"_keys");
		fout.write(keyBytes);
		fout.close();
		bo.close();
	}
	
	

	private void populateExistingWallet(String walletName, String password) 
			throws IOException, FileNotFoundException, ClassNotFoundException
	{
		FileInputStream fin = new FileInputStream(Configuration.keyLocation()+"/"+walletName+"_keys");
		byte[] bb = new byte[4096];
		int size = fin.read(bb);
		fin.close();
		byte[] data = new byte[size];
		for(int i=0; i<data.length; i++){
			data[i] = bb[i];
		}
		//the data for the object KeyPair
		byte[] keyBytes = UtilityMethods.decryptionByXOR(data, password);
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(keyBytes));
		this.keyPair = (KeyPair)(in.readObject());
		this.walletName = walletName;
	}
	
	public synchronized Blockchain getLocalLedger()
	{
		return this.localLedger;
	}
	

	
	
	/**
	 * This method achieves the following:<br>
	 * <li>check the available fund (UTXO) of this wallet, collect enough to initiate the transfer
	 * <li>If no enough fund, return null
	 * <li>Any fund (UTXO) allocated to this transfer (transaction) will be marked unavailable
	 * for further uses. In blockchain/bitcoin white paper, it says that all addresses have access to 
	 * all transactions. Each address does not store individual balance, instead the balance is
	 * computed from the blocks (the longest block chain): how much spent, how much received. 
	 * Practically speaking, we know that if every transaction has to go through the block chain
	 * to compute the balance, it would be time consuming. In this version, however, we will do 
	 * so as we do not have many transactions and the block chain is not long at all.
	 * </li>
	 * @param receiver
	 * @param fundToTransfer
	 * @return Transaction   However, the returned could be null
	 */
	public Transaction transferFund(PublicKey receiver, double fundToTransfer)
	{
		PublicKey[] receivers = new PublicKey[1];
		double[] funds = new double[1];
		receivers[0] = receiver;
		funds[0] = fundToTransfer;
		return transferFund(receivers, funds);
	}
	
	/**
	 * This method has a serious loophole. 
	 */
	public Transaction transferFund(PublicKey[] receivers, double[] fundToTransfer)
	{
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		double available = this.getLocalLedger().findUnspentUTXOs(this.getPublicKey(), unspent);
		double totalNeeded = Transaction.TRANSACTION_FEE;
		for(int i=0; i<fundToTransfer.length; i++){
			totalNeeded += fundToTransfer[i];
		}
		if(available < totalNeeded){
			System.out.println(this.walletName+" balance="+available+", not enough to make the transfer of "+totalNeeded);
			return null;
		}
		//create input for the transaction
		ArrayList<UTXO> inputs = new ArrayList<UTXO>();
		available = 0;
		for(int i=0; i<unspent.size() && available < totalNeeded; i++){
			UTXO uxo = unspent.get(i);
			available += uxo.getFundTransferred();
			inputs.add(uxo);
		}
		
		//create the Transaction
		Transaction T = new Transaction(this.getPublicKey(), receivers, fundToTransfer, inputs);
		
		//sign the transaction
		boolean b = T.prepareOutputUTXOs();
		if(b){
			T.signTheTransaction(this.getPrivateKey());
			return T;
		}else{
			return null;
		}
	}
	
	public double getCurrentBalance(Blockchain ledger)
	{
		return ledger.checkBalance(this.getPublicKey());
	}
	
	public synchronized boolean setLocalLedger(Blockchain ledger){
		boolean b = Blockchain.validateBlockchain(ledger);
		if(!b){
			System.out.println();
			System.out.println(this.getName()+"] Warning: the incoming blockchain failed validation");
			System.out.println();
			return false;
		}
		if(this.localLedger == null){
			this.localLedger = ledger;
			return true;
		}else{
			if(ledger.size() > this.localLedger.size() && ledger.getGenesisMiner().equals(this.localLedger.getGenesisMiner())){
				this.localLedger = ledger;
				return true;
			}else if(ledger.size() <= this.localLedger.size()){
				System.out.println(this.getName()+"] Warning: the incoming blockchain is no longer than current local one"
						+", local size="+this.localLedger.size()+", incoming size="+ledger.size());
				return false;
			}else{
				//System.out.println();
				System.out.println(this.getName()+"] Warning: the incoming blockchain has a different genesis miner than current local one");
				//System.out.println();
				return false;
			}
		}
		
	}
	
	public synchronized  boolean updateLocalLedger(ArrayList<Blockchain> chains)
	{
		if(chains.size() == 0){
			return false;
		}
		if(this.localLedger != null){
			Blockchain max = this.localLedger;
			for(int i=0; i<chains.size(); i++){
				Blockchain bc = chains.get(i);
				if(bc.getGenesisMiner().equals(this.localLedger.getGenesisMiner()) 
						&& bc.size()>max.size() && Blockchain.validateBlockchain(bc)){
					max =  bc;
				}
			}
			this.localLedger = max; //it is possible that nothing changed
			return true;
		}else{
			Blockchain max = null;
			int currentLength = 0;
			for(int i=0; i<chains.size(); i++){
				Blockchain bc = chains.get(i);
				boolean b = Blockchain.validateBlockchain(bc);
				if(b && bc.size()>currentLength){
					max =  bc;
					currentLength = max.size();
				}
			}
			if(max != null){
				this.localLedger = max;
				return true;
			}else{
				return false;
			}
		}
		
	}

	public synchronized boolean updateLocalLedger(Block block)
	{
		if(verifyGuestBlock(block)){
			return this.localLedger.addBlock(block);
		}
		return false;
	}
	
	
	public boolean verifyGuestBlock(Block block, Blockchain ledger)
	{
	
		//checking the signature
		if(!block.verifySignature(block.getCreator())){
			System.out.println("\tWarning: block("+block.getHashID()+") signature tampered");
			return false;
		}
		//got to verify the proof of work, too
		if(!UtilityMethods.hashMeetsDifficultyLevel(block.getHashID(), block.getDifficultyLevel())
				|| !block.computeHashID().equals(block.getHashID())){
			System.out.println("\tWarning: block("+block.getHashID()+") mining is not successful!");
			return false;
		}
		
		//making sure that this block is build upon last block, i.e, verify its hashID
		if(!ledger.getLastBlock().getHashID().equals(block.getPreviousBlockHashID())){
			System.out.println("\tWarning: block("+block.getHashID()+") is not linked to last block");
			return false;
		}
		
		//checking all the transactions are valid
		int size = block.getTotalNumberOfTransactions();
		for(int i=0; i<size; i++){
			Transaction T = block.getTransaction(i);
			if(!validateTransaction(T)){
				System.out.println("\tWarning: block("+block.getHashID()+") transaction " + i+" is invalid either "
						+ "because of signature being tampered or already existing in the blockchain.");
				return false;
			}
		}
		//here, we do not examine if the transaction balance is good, however
		//check the rewarding transaction
		Transaction tr = block.getRewardTransaction();
		if(tr.getTotalFundToTransfer() > Blockchain.MINING_REWARD+block.getTransactionFeeAmount()){
			System.out.println("\tWarning: block("+block.getHashID()+") over rewarded");
			return false;
		}
		
		return true;
	}
	
	public boolean verifyGuestBlock(Block block){
		return this.verifyGuestBlock(block, this.getLocalLedger());
	}
	
	/**
	 * This method should be called before a transaction is added into a block
	 * @param ts
	 * @return
	 */
	public boolean validateTransaction(Transaction ts)
	{
		if(ts == null){
			return false;
		}
		
		if(!ts.verifySignature()){
			System.out.println("WARNING: transaction ID="+ts.getHashID()+" from "
					+UtilityMethods.getKeyString(ts.getSender())+" is invalid. It has been tampered.");
			//remove this transaction and should push it to somewhere
			return false;
		}
		//make sure that this transaction does not exist in the existing ledger
		//this is a time consuming process
		boolean exists;
		if(this.getLocalLedger() == null){
			exists = false;
		}else{
			exists = this.getLocalLedger().isTransactionExist(ts);
		}
		return !exists;
	}
	
	
	
}




