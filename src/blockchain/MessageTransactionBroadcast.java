package blockchain;

public class MessageTransactionBroadcast extends Message 
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private Transaction transaction = null;
	
	public MessageTransactionBroadcast(Transaction transaction)
	{
		this.transaction = transaction;
	}
	
	public int getMessageType(){
		return Message.TRANSACTION_BROADCAST;
	}
		
	//the message body
	public Transaction getMessageBody(){
		return this.transaction;
	}
	
	public boolean isForBroadcast(){
		return true;
	}
}

