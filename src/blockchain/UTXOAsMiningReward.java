package blockchain;

import java.security.PublicKey;

public class UTXOAsMiningReward extends UTXO
{
	private static final long serialVersionUID = 1L;
	
	public UTXOAsMiningReward(String parentTransactionID, PublicKey sender, PublicKey receiver, double fundToTransfer)
	{
		super(parentTransactionID, sender, receiver, fundToTransfer);
	}
	
	public boolean isMiningReward(){
		return true;
	} 
}
