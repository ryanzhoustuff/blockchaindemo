package blockchain;

import java.util.Scanner;
import java.util.ArrayList;

public class BlockchainPlatform 
{
	protected static Scanner keyboard = null;
	public static void main(String[] args) 
	{		
		keyboard = new Scanner(System.in);
		MinerGenesisSimulator genesisSimulator = new MinerGenesisSimulator();
		Thread simulator = new Thread(genesisSimulator);
		simulator.start();
		System.out.println("Genesis simulator is up");
		System.out.println("Starting the blockchain message service provider ...");
		BlockchainMessageServiceProvider server = new BlockchainMessageServiceProvider();
		Blockchain ledger = genesisSimulator.getGenesisLedger();
		BlockchainMessageServiceProvider.updateGenesisBlock(ledger);
		//make sure that the genesisMiner's blockChain and the ledger here are the same
		Blockchain ledger2 = genesisSimulator.getGenesisMiner().getLocalLedger();
		if(ledger.size() != ledger2.size()){
			System.out.println("ERROR!!! the two genesis blockchains are different in size!, "+ ledger.size()+"|"+ledger2.size());
			System.exit(1);
		}
		if(!ledger.getLastBlock().getHashID().equals(ledger2.getLastBlock().getHashID())){
			System.out.println("Error!!! the two genesis blockchains have different hashcodes!");
			System.out.println(ledger.getLastBlock().getPreviousBlockHashID()+"\n"+ledger2.getLastBlock().getPreviousBlockHashID());
			System.exit(2);
		}
		System.out.println("******************************************************");
		System.out.println("Genesis blockchain is set  for service provider");
		System.out.println("Blockchain message service provider is now ready to work");
		System.out.println("******************************************************");
		server.startWorking();
		System.out.println("=========Blockchain platform shuts down=========");
	}

}


final class MinerGenesisSimulator implements Runnable
{
	private Blockchain genesisLedger = null;
	private Miner genesisMiner;
	/**
	 * It is critical to make it synchronized here
	 * @return
	 */
	protected synchronized Blockchain getGenesisLedger()
	{
		if(genesisLedger == null){
			System.out.println("Blockchain platform starts ...");
			System.out.println("creating genesis miner, genesis transaction and genesis block");
			//create a genesis miner to start a blockchain
			genesisMiner = getGenesisMiner();
			//create genesis block
			Block genesisBlock = new Block("0", Configuration.blockMiningDifficultyLevel(), genesisMiner.getPublicKey());
			UTXO u1 = new UTXO("0", genesisMiner.getPublicKey(), genesisMiner.getPublicKey(), 1000001.0);
			UTXO u2 = new UTXO("0", genesisMiner.getPublicKey(), genesisMiner.getPublicKey(), 1000000.0);
			ArrayList<UTXO> inputs = new ArrayList<UTXO>();
			inputs.add(u1);
			inputs.add(u2);
			Transaction gt = new Transaction(genesisMiner.getPublicKey(), genesisMiner.getPublicKey(), 1000000.0, inputs);
			boolean b = gt.prepareOutputUTXOs();
			if(!b){
				System.out.println("genesis transaction failed.");
				System.exit(1);
			}
			gt.signTheTransaction(genesisMiner.getPrivateKey());
			b = genesisBlock.addTransaction(gt, genesisMiner.getPublicKey());
			if(!b){
				System.out.println("failed to add the genesis transaction to the genesis block. System quit");
				System.exit(2);
			}
			//the genesis miner mines the genesis block
			System.out.println("genesis miner is mining the genesis block");
			b = genesisMiner.mineBlock(genesisBlock);
			if(b){
				System.out.println("genesis block is successfully mined. HashID:");
				System.out.println(genesisBlock.getHashID());
			}else{
				System.out.println("failed to mine genesis block. System exit");
				System.exit(3);
			}
			Blockchain ledger = new Blockchain(genesisBlock);
			System.out.println("block chain genesis successful");
			//genesisMiner copies the blockchain to his local ledger
			genesisMiner.setLocalLedger(ledger);
			//set up the genesisLedger
			this.genesisLedger = ledger.copy_NotDeepCopy();
			System.out.println("genesis miner balance: " + genesisMiner.getCurrentBalance(genesisMiner.getLocalLedger()));
		}
		return this.genesisLedger;
	}
	
	/**
	 * It is critical to make it synchronized here
	 * @return
	 */
	protected synchronized Miner getGenesisMiner()
	{
		if(genesisMiner == null){
			genesisMiner = new Miner("genesis", "genesis");
		}
		return genesisMiner;
	}
	
	
	public void run()
	{
		System.out.println("Important!  You are the genesis miner, you must start before any other miners or wallet!");
		System.out.println("With great ability comes the great responsibility ...");
		System.out.println("");
		Miner miner = getGenesisMiner();
		getGenesisLedger();
		System.out.println("Your name="+miner.getName());
		System.out.println("===== Important!  Has the ServiceRelayProvider started?===== (1=yes, 0= no)");
		int yesno = UtilityMethods.guaranteeIntegerInputByScanner(BlockchainPlatform.keyboard, 0, 1);
		while(yesno == 0){
			System.out.println("===== Important!  Has the ServiceRelayProvider started?===== (1=yes, 0= no)");
			yesno = UtilityMethods.guaranteeIntegerInputByScanner(BlockchainPlatform.keyboard, 0, 1);
		}
		double balance = miner.getCurrentBalance(miner.getLocalLedger());
		System.out.println("checking genesis miner balance: " + balance);
		System.out.println("To join the blockchain network, please enter the service provider IP address:");
		System.out.println("If this simulator is on the same computer as the Service Provider, please enter 127.0.0.1");
		String ipAddress = BlockchainPlatform.keyboard.nextLine();
		if(ipAddress== null || ipAddress.length()<5){
			ipAddress = "localhost";
		}
		WalletConnectionAgent agent = new WalletConnectionAgent(ipAddress, 
					Configuration.networkPort(), miner);
		Thread athread = new Thread(agent);
		athread.start();
		MinerGenesisMessageTaskManager taskManager = new MinerGenesisMessageTaskManager(agent, 
				miner, agent.getMessageQueue());
		Thread tThread = new Thread(taskManager);
		tThread.start();
	}
}

