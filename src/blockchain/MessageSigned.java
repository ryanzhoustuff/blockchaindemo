package blockchain;

public abstract class MessageSigned extends Message
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * This method will be called to validate the message. In this version, we do not encrypt
	 * the message. Instead, the message is signed to make sure that it is not altered.
	 * @return
	 */
	public abstract boolean isValid();
}

