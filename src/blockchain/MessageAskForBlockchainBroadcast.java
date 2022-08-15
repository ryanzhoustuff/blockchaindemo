package blockchain;

import java.security.PrivateKey;
import java.security.PublicKey;

public class MessageAskForBlockchainBroadcast extends MessageTextBroadcast
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	public MessageAskForBlockchainBroadcast(String text, PrivateKey prikey, PublicKey sender, String name)
	{
		super(text, prikey, sender, name);
	}
	
	public int getMessageType(){
		return Message.BLOCKCHAIN_ASK_BROADCAST;
	}
}
