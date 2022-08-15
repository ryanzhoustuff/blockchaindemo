package blockchain;

import java.security.PrivateKey;
import java.security.PublicKey;

public class MessageTextPrivate extends MessageSigned
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private String info = null;
	private byte[] signature = null;
	private PublicKey senderKey = null;
	private String senderName;
	private PublicKey receiver = null;
	
	public MessageTextPrivate(String text, PrivateKey prikey, PublicKey senderKey, String senderName, PublicKey receiver)
	{
		this.info = text;
		signature = UtilityMethods.generateSignature(prikey, this.info);
		this.senderKey = senderKey;
		this.receiver = receiver;
		this.senderName = senderName;
	}
	
	public String getMessageBody(){
		return this.info;
	}

	public boolean isValid()
	{
		return UtilityMethods.verifySignature(senderKey, signature, this.info);
	}
	
	public int getMessageType(){
		return Message.TEXT_PRIVATE;
	}
	
	public PublicKey getReceiver(){
		return this.receiver;
	}
	
	public PublicKey getSenderKey(){
		return this.senderKey;
	}
	
	public String getSenderName(){
		return this.senderName;
	}
	
	public KeyNamePair getSenderKeyNamePair(){
		return new KeyNamePair(this.getSenderKey(), this.senderName);
	}
	
	public boolean isForBroadcast(){
		return false;
	}
}
