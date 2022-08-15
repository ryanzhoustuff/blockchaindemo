package blockchain;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
public class Transaction implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;
	public static final double TRANSACTION_FEE = 1.0;
	private String hashID;
	private PublicKey sender;
	private PublicKey[] receivers;
	private double[] fundToTransfer; //the value to transfer to each receiver respectively
	private long timestamp;
	private ArrayList<UTXO> inputs = new ArrayList<UTXO>();
	private ArrayList<UTXO> outputs = new ArrayList<UTXO>(4);
	private byte[] signature = null;
	private boolean signed = false;
	private long mySequentialNumber;
	
	
	public Transaction(PublicKey sender, PublicKey receiver, double fundToTransfer, ArrayList<UTXO> inputs)
	{
		PublicKey[] pks = new PublicKey[1];
		pks[0] = receiver;
		double[] funds = new double[1];
		funds[0] = fundToTransfer;
		this.setUp(sender, pks, funds, inputs);
	}
	
	public Transaction(PublicKey sender, PublicKey[] receivers, double[] fundToTransfer, ArrayList<UTXO> inputs)
	{
		this.setUp(sender, receivers, fundToTransfer, inputs);
	}
	
	private void setUp(PublicKey sender, PublicKey[] receivers, double[] fundToTransfer, ArrayList<UTXO> inputs){
		this.mySequentialNumber = UtilityMethods.getUniqueNumber();
		this.sender = sender;
		this.receivers = new PublicKey[1];
		this.receivers = receivers;
		this.fundToTransfer = fundToTransfer;
		this.inputs = inputs;
		this.timestamp = java.util.Calendar.getInstance().getTimeInMillis();
		computeHashID();
	}
	
	
	public void signTheTransaction(PrivateKey privateKey)
	{
		if(this.signature == null && !signed){
			this.signature = UtilityMethods.generateSignature(privateKey, getMessageData());
			signed = true;
		}
	}
	
	public boolean verifySignature()
	{
		String message = getMessageData();
		return UtilityMethods.verifySignature(this.sender, this.signature, message);
	}
	
	private String getMessageData()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(UtilityMethods.getKeyString(sender)+Long.toHexString(this.timestamp)+Long.toString(this.mySequentialNumber));
		for(int i=0; i<this.receivers.length; i++){
			sb.append(UtilityMethods.getKeyString(this.receivers[i]) + Double.toHexString(this.fundToTransfer[i]));
		}
		//also need to include the input UTXOs
		for(int i=0; i<this.getNumberOfInputUTXOs(); i++){
			UTXO ut = this.getInputUTXO(i);
			sb.append(ut.getHashID());
		}
		return sb.toString();
	}
	
	protected void computeHashID()
	{
		String message = getMessageData();
		this.hashID = UtilityMethods.messageDigestSHA256_toString(message);
	}
	
	public String getHashID(){
		return this.hashID;
	}
	
	public PublicKey getSender(){
		return this.sender;
	}
	
	public long getTimeStamp(){
		return this.timestamp;
	}
	
	public long getSequentialNumber(){
		return this.mySequentialNumber;
	}
	
	public double getTotalFundToTransfer(){
		double f = 0;
		for(int i=0; i<this.fundToTransfer.length;i++){
			f += this.fundToTransfer[i];
		}
		return f;
	}
	
	protected void addOutputUTXO(UTXO ut){
		if(!signed){
			outputs.add(ut);
		}
	}
	
	public int getNumberOfOutputUTXOs(){
		return this.outputs.size();
	}
	
	/**
	 * The returned value might be null,  this happens when the i is out of range.
	 * @param i
	 * @return
	 */
	public UTXO getOuputUTXO(int i)
	{
		return this.outputs.get(i);	
	}
	

	public int getNumberOfInputUTXOs(){
		//for the genesis block
		if(this.inputs == null){
			return 0;
		}
		return this.inputs.size();
	}
	
	public UTXO getInputUTXO(int i){
		return this.inputs.get(i);
	}
	
	public boolean equals(Transaction T){
		return this.getHashID().equals(T.getHashID());
	}
	
	public boolean prepareOutputUTXOs()
	{
		if(this.receivers.length != this.fundToTransfer.length){
			return false;
		}
		double totalCost = this.getTotalFundToTransfer() + Transaction.TRANSACTION_FEE;
		double available = 0.0;
		for(int i=0; i<this.inputs.size(); i++){
			available += this.inputs.get(i).getFundTransferred();
		}
		if(available < totalCost){
			return false;
		}
		this.outputs.clear();
		for(int i=0; i<receivers.length; i++){
			UTXO ut = new UTXO(this.getHashID(), this.sender, receivers[i], this.fundToTransfer[i]);
			this.outputs.add(ut);
		}
		//generate the change as an UTXO to the sender
		UTXO change = new UTXO(this.getHashID(), this.sender, this.sender, available-totalCost);
		this.outputs.add(change);
		return true;
	}
}
