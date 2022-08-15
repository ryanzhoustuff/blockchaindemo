package blockchain;

import java.security.PublicKey;

public class UTXO implements java.io.Serializable
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	private String hashID;
	private String parentTransactionID;
	private PublicKey receiver;
	private PublicKey sender;
	private long timestamp;
	private double fundTransferred;
	private long sequentialNumber = 0;
	
	public UTXO(String parentTransactionID, PublicKey sender, PublicKey receiver, double fundToTransfer)
	{
		this.sequentialNumber = UtilityMethods.getUniqueNumber();
		this.parentTransactionID = parentTransactionID;
		this.receiver = receiver;
		this.sender = sender;
		this.fundTransferred = fundToTransfer;
		this.timestamp = UtilityMethods.getTimeStamp();
		this.hashID = computeHashID();
	}
	
	protected String computeHashID()
	{
		String message = this.parentTransactionID + UtilityMethods.getKeyString(this.sender) + UtilityMethods.getKeyString(receiver) 
						+ Double.toHexString(this.fundTransferred) + Long.toHexString(this.timestamp)+Long.toHexString(this.sequentialNumber);
		return UtilityMethods.messageDigestSHA256_toString(message);
	}
	
	public String getHashID(){
		return this.hashID;
	}
	
	public String getParentTransactionID(){
		return this.parentTransactionID;
	}
	
	public PublicKey getReceiver(){
		return this.receiver;
	}
	
	public PublicKey getSender(){
		return this.sender;
	}
	
	public long getTimeStamp(){
		return this.timestamp;
	}
	
	public long getSequentialNumber(){
		return this.sequentialNumber;
	}
	
	public double getFundTransferred(){
		return this.fundTransferred;
	}
	
	public boolean equals(UTXO uxo){
		return this.getHashID().equals(uxo.getHashID());
	}
	
	public boolean isMiningReward(){
		return false;
	}
}
