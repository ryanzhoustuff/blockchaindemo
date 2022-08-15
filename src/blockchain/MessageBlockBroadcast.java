package blockchain;

public class MessageBlockBroadcast extends Message
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private Block block = null;
	
	public MessageBlockBroadcast(Block block){
		this.block = block;
	}
	
	public int getMessageType(){
		return Message.BLOCK_BROADCAST;
	}
		
	//the message body
	public Block getMessageBody(){
		return this.block;
	}
	
	public boolean isForBroadcast(){
		return true;
	}
}


