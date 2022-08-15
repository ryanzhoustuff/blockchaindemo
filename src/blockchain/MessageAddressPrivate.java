package blockchain;

import java.util.ArrayList;

public class MessageAddressPrivate extends Message
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<KeyNamePair> addresses;
	
	public MessageAddressPrivate(ArrayList<KeyNamePair> addresses)
	{
		this.addresses = addresses;
	}
	
	public int getMessageType(){
		return Message.ADDRESS_PRIVATE;
	}
		
	//the message body
	public ArrayList<KeyNamePair> getMessageBody(){
		return this.addresses;
	}
	
	public boolean isForBroadcast(){
		return false;
	}
}
