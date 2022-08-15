package blockchain;

import java.security.PublicKey;

public class MessageBlockchainPrivate extends Message implements java.io.Serializable
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private Blockchain ledger = null;
	private PublicKey sender = null;
	private PublicKey receiver = null;
	private int initialSize = 0;
	
	public MessageBlockchainPrivate(Blockchain ledger, PublicKey sender, PublicKey receiver){
		this.ledger = ledger;
		this.receiver = receiver;
		this.sender = sender;
		this.initialSize = this.ledger.size();
	}
	
	public int getInfoSize(){
		return this.initialSize;
	}
	
	
	public int getMessageType(){
		return Message.BLOCKCHAIN_PRIVATE;
	}
	
	public PublicKey getReceiver(){
		return this.receiver;
	}
		
	public Blockchain getMessageBody(){
		return this.ledger;
	}
	
	public boolean isForBroadcast(){
		return false;
	}

	public PublicKey getSender(){
		return this.sender;
	}
}

