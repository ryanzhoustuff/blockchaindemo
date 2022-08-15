package blockchain;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Container;
import java.awt.Color;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.security.PublicKey;
import java.awt.event.KeyEvent;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.util.ArrayList;
import java.util.Random;
import java.util.Calendar;

public class WalletSimulator extends JFrame
{
	protected static MessageFrame messageFrame = new MessageFrame();
	protected static FrameHelp help = new FrameHelp();
	private boolean balanceShowPublicKey = false;
	private JTextArea textInput;
	private JButton sentButton;
	private JTextArea displayArea;
    private GridBagLayout mgr = null;
    private GridBagConstraints gcr = null;
    private Wallet wallet = null;
    private WalletConnectionAgent connectionAgent = null;
    private WalletMessageTaskManager taskManager = null;
    private Calendar calendar = Calendar.getInstance();
    
	public WalletSimulator(Wallet wallet, WalletConnectionAgent agent, 
										WalletMessageTaskManager manager)
	{
		super(wallet.getName());
		this.wallet = wallet;
		this.connectionAgent = agent;
		this.taskManager = manager;
		setUpGUI();
		this.addWindowListener(new java.awt.event.WindowAdapter(){
			public void windowClosing(java.awt.event.WindowEvent e){
				//close all thread if here is one
				try{
					connectionAgent.sendMessage(new MessageTextPrivate(Message.TEXT_CLOSE, 
							wallet.getPrivateKey(), wallet.getPublicKey(),
							wallet.getName(), connectionAgent.getServerAddress()));
				}catch(Exception e1){}
				try{
					connectionAgent.activeClose();
					taskManager.close();
				}catch(Exception ee){}
				dispose();
				System.exit(2);
			}
			
		});
	}
	
	private void setUpGUI()
	{
		this.setSize(500, 600);
		//set the bar first
		setBar();
		
        Container c = getContentPane();
        mgr = new GridBagLayout();
        gcr = new GridBagConstraints();
        c.setLayout(mgr);      
        JLabel lblInput = new JLabel("                             Message Board");
        lblInput.setForeground(Color.GREEN);
        this.displayArea = new JTextArea(50, 100);
        this.textInput = new JTextArea(5, 100);
        this.sentButton = new JButton("Click me or hit enter to send the message below");
        this.sentButton.addActionListener(new java.awt.event.ActionListener(){
        	public void actionPerformed(java.awt.event.ActionEvent e){
        		try{
        			MessageTextBroadcast m = new MessageTextBroadcast(
        					textInput.getText(), wallet.getPrivateKey(), 
        					wallet.getPublicKey(), wallet.getName());
        			connectionAgent.sendMessage(m);
        		}catch(Exception e2){
        			System.out.println("Error: " + e2.getMessage());
        			throw new RuntimeException(e2);
        		}
        		textInput.setText("");
        	}
        });
        
        this.gcr.fill = GridBagConstraints.BOTH;
        this.gcr.weightx = 1;
        this.gcr.weighty = 0.0;
        this.gcr.gridx = 0;
        this.gcr.gridy = 0;
        this.gcr.gridwidth = 1;
        this.gcr.gridheight = 1;
        this.mgr.setConstraints(lblInput, this.gcr);
        c.add(lblInput);
        
        this.gcr.weighty = 0.9;
        this.gcr.gridx = 0;
        this.gcr.gridy = 1;
        this.gcr.gridheight = 9;
        JScrollPane scroll = new JScrollPane(this.displayArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.mgr.setConstraints(scroll, this.gcr);
        c.add(scroll);
        this.displayArea.setEditable(false);
        this.displayArea.setBackground(Color.LIGHT_GRAY);
        this.displayArea.setLineWrap(true);
        this.displayArea.setWrapStyleWord(true);
        
        this.gcr.weighty = 0.0;
        this.gcr.gridx = 0;
        this.gcr.gridy = 11;
        this.gcr.gridheight = 1;
        this.mgr.setConstraints(this.sentButton, this.gcr);
        c.add(this.sentButton);
        
        this.gcr.weighty = 0.1;
        this.gcr.gridx = 0;
        this.gcr.gridy = 12;
        this.gcr.gridheight = 2;
        JScrollPane scroll2 = new JScrollPane(this.textInput);
		scroll2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.mgr.setConstraints(scroll2, this.gcr);
        c.add(scroll2);
        this.textInput.setLineWrap(true);
        this.textInput.setWrapStyleWord(true);
        //add a key listener to the textarea
        this.textInput.addKeyListener(new KeyListener(){
        	public void keyTyped(KeyEvent e) {}
        		public void keyReleased(KeyEvent e) {}
        		public void keyPressed(KeyEvent e) {
        			int key = e.getKeyCode();
        			if (key == KeyEvent.VK_ENTER) {
        				//allow using shift+ENTER or control+ENTER to get an ENTER
        				if(e.isShiftDown() || e.isControlDown()){
        					textInput.append(System.getProperty("line.separator"));
        				}else{
        					try{
        	        			MessageTextBroadcast m = new MessageTextBroadcast(
        	        					textInput.getText(), wallet.getPrivateKey(), 
        	        					wallet.getPublicKey(), wallet.getName());
        	        			connectionAgent.sendMessage(m);
        	        		}catch(Exception e2){
        	        			System.out.println("Error: " + e2.getMessage());
        	        			throw new RuntimeException(e2);
        	        		}
	        				//consume the ENTER so that the cursor will stay at the beginning
	        				e.consume();
	                		textInput.setText("");
        				}
        			}
        	     }
        	}
        );        
		this.setVisible(true);
	}
	
	private void setBalanceShowPublicKey(boolean yesno){
		this.balanceShowPublicKey = yesno;
	}
	
	public boolean showPublicKeyInBalance(){
		return this.balanceShowPublicKey;
	}
	
	
    private void setBar()
    {
        JMenuBar bar = new JMenuBar();
        //set the menubar for this frame
        setJMenuBar(bar); 
        JMenu askMenu = new JMenu("Ask For");
        
        JMenuItem helpItem = new JMenuItem("Click me for help");
        helpItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		showHelpMessage("1. When you 'update blockchain', "
        				+ "a broadcast message is sent for the latest "
        				+ "blockchain so as to update the local copy. "
        				+ "This becomes necessary if your local copy "
        				+ "is out of date.\n"
        				+ "2. When you click 'update users', "
        				+ "the service provider will update your user list.\n"
        				+ "3. Clicking 'show balance' will display your "
        				+ "balance in the display board.\n"
        				+ "4. Clicking 'display blockchain' will display "
        				+ "your local blockchain in the display board.");
        	}
        });
        
        JMenuItem askBlockchainItem = new JMenuItem("update blockchain");
        askBlockchainItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		MessageAskForBlockchainBroadcast m = 
        				new MessageAskForBlockchainBroadcast("please", 
        					wallet.getPrivateKey(), wallet.getPublicKey(),
        					wallet.getName());
        		connectionAgent.sendMessage(m);
        	}
        });

        JMenuItem askAddressesItem = new JMenuItem("update users");
        askAddressesItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		MessageTextPrivate m = 
        				new MessageTextPrivate(Message.TEXT_ASK_ADDRESSES, 
        				wallet.getPrivateKey(), wallet.getPublicKey(), 
        				wallet.getName(), connectionAgent.getServerAddress());
        		connectionAgent.sendMessage(m);
        	}
        });
        
        JMenuItem askBalanceItem = new JMenuItem("show balance");
        askBalanceItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		displayBalance(wallet);
        	}
        });
        
        JMenuItem displayBlockchain = new JMenuItem("display blockchain");
        displayBlockchain.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		displayBlockchain(wallet);
        	}
        });

        askMenu.add(helpItem);
        askMenu.add(askBlockchainItem);
        askMenu.add(askAddressesItem);
        askMenu.add(askBalanceItem);
        askMenu.add(displayBlockchain);
        bar.add(askMenu);
        
        JMenu sendMenu = new JMenu("To Send");
        JMenuItem helpItem2 = new JMenuItem("Click me for help");
        helpItem2.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		showHelpMessage("1. When you start a transaction, "
        				+"you need to choose the recipient(s) and "
        				+"the amount to each recipient.\n"
        				+"2. The private message you send to a "
        				+"user will be displayed on the message "
        				+"board, but only the recipent will be "
        				+"able to see it.");
        	}
        });
        
        JMenuItem sendTransactionItem = new JMenuItem("start a transaction");
        sendTransactionItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		FrameTransaction ft = new FrameTransaction(
        				connectionAgent.getAllStoredAddresses(), connectionAgent);
        	}
        });
       
        JMenuItem sendPrivateMessageItem = new JMenuItem("to send a private message");
        sendPrivateMessageItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		FramePrivateMessage fpm = new FramePrivateMessage(
        				connectionAgent.getAllStoredAddresses(), 
        				connectionAgent, WalletSimulator.this);
        	}
        });
        
        sendMenu.add(helpItem2);
        sendMenu.add(sendTransactionItem);
        sendMenu.add(sendPrivateMessageItem);
        bar.add(sendMenu);
    }
    
    //This method automatically adds new line at the end
    protected void appendMessageLineOnBoard(String s)
    {
    	String time = calendar.getTime().toString();
		this.displayArea.append("("+time+") "+s+System.getProperty("line.separator"));
		this.displayArea.setCaretPosition(this.displayArea.getText().length());
    }
    
    protected void displayBlockchain(Wallet w)
    {
    	StringBuilder sb = new StringBuilder();
    	UtilityMethods.displayBlockchain(w.getLocalLedger(), sb, 0);
    	messageFrame.setMessage(sb.toString());
    }
    
	protected void displayBalance(Wallet w)
	{
		StringBuilder sb = new StringBuilder();
		Blockchain ledger = w.getLocalLedger();
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		ArrayList<Transaction> sentT = new ArrayList<Transaction>();
		ArrayList<UTXO> rewards = new ArrayList<UTXO>();
		double balance = ledger.findRelatedUTXOs(w.getPublicKey(), all, spent, unspent, sentT, rewards);
		ArrayList<UTXO> su = new ArrayList<UTXO>();
		for(int i=0; i<sentT.size(); i++){
			Transaction t = sentT.get(i);
			for(int j=0; j<t.getNumberOfOutputUTXOs(); j++){
				UTXO u1 = t.getOuputUTXO(j);
				if(!(u1.getReceiver().equals(w.getPublicKey())) ){
					su.add(u1);
				}
			}
		}
		
		int level = 0;
		displayTab(sb, level, w.getName()+"{");
		displayTab(sb, level+1, "All UTXOs:");
		displayUTXOs(sb, all, level+2);
		displayTab(sb, level+1, "Spent UTXOs:");
		displayUTXOs(sb, spent, level+2);
		displayTab(sb, level+1, "Unspent UTXOs:");
		displayUTXOs(sb, unspent, level+2);
		if(w instanceof Miner){
			displayTab(sb, level+1, "Mining Rewards:");
			displayUTXOs(sb, rewards, level+2);
		}
		displayTab(sb, level+1, "Paid UTXOs:");
		displayUTXOs(sb, su, level+2);
		displayTab(sb, level+1, "Paid Transaction Fee:");
		displayTab(sb, level+2, ""+(sentT.size() * Transaction.TRANSACTION_FEE));
		displayTab(sb, level+1, "Balance="+balance);
		displayTab(sb, level, "}");
		String s = sb.toString();
		messageFrame.setMessage(s);
	}
	

	private void displayUTXOs(StringBuilder sb, ArrayList<UTXO> uxs, int level)
	{
		for(int i=0; i<uxs.size(); i++){
			UTXO ux = uxs.get(i);
			if(showPublicKeyInBalance()){
				displayTab(sb, level, "fund: "
					+ ux.getFundTransferred() +", receiver: "
					+ UtilityMethods.getKeyString(ux.getReceiver())
					+ ", sender: " + UtilityMethods.getKeyString(ux.getSender()));
			}else{
				displayTab(sb, level, "fund: "+ ux.getFundTransferred() +", receiver: "
						+ connectionAgent.getNameFromAddress(ux.getReceiver())
						+ ", sender: " + connectionAgent.getNameFromAddress(ux.getSender()));
			}
		}
	}
	
	
	
	private void displayTab(StringBuilder sb, int level, String mesg)
	{
		for(int i=0; i<level; i++){
			sb.append("\t");
		}
		sb.append(mesg);
		sb.append(System.getProperty("line.separator"));
	}
	

	protected static void showHelpMessage(String message)
	{
		help.setMessage(message);
	}
	
	
	public static void main(String[] args) throws Exception
	{
		Random rand = new Random();
		//let's make the chance to be a miner is 3 out 4, while the chance to be a wallet is 1 out of 4
		int chance = rand.nextInt(4);
		//Please not close it.
		Scanner in = new Scanner(System.in);
		System.out.println("please provide a name:");
		String wname = in.nextLine();
		System.out.println("Please provide your password:");
		String wpassword = in.nextLine();
		System.out.println("When showing balance, "
							+ "by default the public key is not shown as the address.\n"
							+ "This is for simplicity. "
							+ "Do you like to show the public key as address (Yes/No)??");
		String yesno = in.nextLine();
		boolean show = false;
		if(yesno.toUpperCase().startsWith("Y")){
			show = true;
		}
		System.out.println("To join the blockchain network, "
							+ "please present the service provider IP address:");
		String ipAddress = in.nextLine();
		if(ipAddress.length()<5){
			ipAddress = "localhost";
		}
		if(chance == 0){
			System.out.println("");
			System.out.println("===== Congratulation, you are a wallet, i.e. a general user =====");
			Wallet wallet = new Wallet(wname, wpassword);
			System.out.println("");
			System.out.println("Welcome "+ wname +", blockchain wallet created for you.");
			System.out.println();
			WalletConnectionAgent agent = new WalletConnectionAgent(ipAddress, Configuration.networkPort(), wallet);
			Thread agentThread = new Thread(agent);
			WalletMessageTaskManager manager = new WalletMessageTaskManager(agent, wallet, agent.getMessageQueue());
			Thread managerThread = new Thread(manager);
			WalletSimulator simulator = new WalletSimulator(wallet, agent, manager);
			manager.setSimulator(simulator);
			agentThread.start();
			System.out.println("wallet connection agent started");
			managerThread.start();
			System.out.println("wallet task manager started");
			simulator.setBalanceShowPublicKey(show);
		}else{
			System.out.println("");
			System.out.println("===== Congratulation, you are a miner, "
						+ "i.e. a full-power user who can mine blocks =====");
			System.out.println("");
			Miner miner = new Miner(wname, wpassword);
			System.out.println("Welcome "+ wname +", blockchain miner created for you.");
			WalletConnectionAgent agent = new WalletConnectionAgent(ipAddress, Configuration.networkPort(), miner);
			Thread agentThread = new Thread(agent);
			MinerMessageTaskManager manager = new MinerMessageTaskManager(agent, miner, agent.getMessageQueue());
			Thread managerThread = new Thread(manager);
			WalletSimulator simulator = new WalletSimulator(miner, agent, manager);
			manager.setSimulator(simulator);
			agentThread.start();
			System.out.println("miner connection agent started");
			managerThread.start();
			System.out.println("miner task manager started");
			simulator.setBalanceShowPublicKey(show);
		}
	}
}



class MessageFrame extends javax.swing.JFrame
{
    Container c = this.getContentPane();
    javax.swing.JTextArea msg = new JTextArea();
    JScrollPane pane = new JScrollPane();
    public MessageFrame() {
        super("Information Board");
        this.setBounds(0, 0, 600, 450);
        JScrollPane pane = new JScrollPane(this.msg);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        c.add(pane);
        msg.setLineWrap(false);
        msg.setRows(100);
        msg.setColumns(80);

        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
            	//do nothing
            }
        });
    }

    public void setMessage(String message)
    {
        msg.setText(message);
        this.validate();
        this.setVisible(true);
    }

    public void appendMessage(String message)
    {
        msg.append(message);
        this.validate();
        this.setVisible(true);
    }
}

class FrameHelp extends javax.swing.JFrame{
    javax.swing.JTextPane msg = new JTextPane();
    public FrameHelp() {
        super("Help Message");
        Container c = this.getContentPane();
        this.setBounds(500, 500, 300, 220);
        msg.setBounds(0, 0, this.getWidth(), this.getHeight());
        c.add(msg);
    }
    public void setMessage(String message)
    {
    	msg.setText(message);
    	this.validate();
        this.setVisible(true);
    }
}

class FrameTransaction extends javax.swing.JFrame implements ActionListener
{
	private ArrayList<KeyNamePair> users = null;
	private WalletConnectionAgent agent = null;
	public FrameTransaction(ArrayList<KeyNamePair> users, WalletConnectionAgent agent)
	{
		super("Prepare Transaction");
		this.users = users;
		this.agent = agent;
		setUp();
	}
	
	private void setUp()
	{
		Container c = this.getContentPane();
		this.setSize(300, 120);
		GridLayout layout = new GridLayout(3, 2, 5, 5);
		JLabel je = new JLabel("Please select a user");
		JLabel jf = new JLabel("The transaction amount");
		JButton js = new JButton("Submit");
		JButton jc = new JButton("Cancel");
		c.setLayout(layout);
		c.add(je);
		c.add(jf);
		JComboBox<String> candidates = new JComboBox<String>();
		for(int i=0; i<users.size(); i++){
			candidates.addItem(users.get(i).getName());
		}
		c.add(candidates);
		JTextField input = new JTextField();
		c.add(input);
		c.add(js);
		c.add(jc);
		
		js.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int selectedIndex = candidates.getSelectedIndex();
				double amount = -1.0;;
				String text = input.getText();
				if(text != null && text.length() > 0){
					try{
						amount = Double.parseDouble(text);
					}catch(Exception pe){
						amount = -1;
					}
					if(amount <= 0.0){
						input.setText("must be a positive number");
						return;
					}
					boolean b = agent.sendTransaction(
						users.get(selectedIndex).getKey(), amount);
					if(!b){
						input.setText("Failed to send");
					}else{
						input.setText("Transaction sent");
						//js.setEnabled(false);
					}
				}
			}
			
		});
		jc.addActionListener(this);
		this.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e){
		this.dispose();
	}
}

class FramePrivateMessage extends javax.swing.JFrame implements ActionListener
{
	private ArrayList<KeyNamePair> users = null;
	private WalletConnectionAgent agent = null;
	private JTextArea board = null;
	private WalletSimulator simulator;
	public FramePrivateMessage(ArrayList<KeyNamePair> users, 
			WalletConnectionAgent agent, WalletSimulator simulator)
	{
		super("Send a private message");
		this.users = users;
		this.agent = agent;
		this.simulator = simulator;
		setUp();
	}
	
	private void setUp()
	{
        Container c = getContentPane();
		this.setSize(300, 200);
        GridBagLayout mgr = new GridBagLayout();
        GridBagConstraints gcr = new GridBagConstraints();
        c.setLayout(mgr);      
        
        JLabel ja = new JLabel("Please select:");
        gcr.fill = GridBagConstraints.BOTH;
        gcr.weightx = 0.5;
        gcr.weighty = 0.0;
        gcr.gridx = 0;
        gcr.gridy = 0;
        gcr.gridwidth = 1;
        gcr.gridheight = 1;
        mgr.setConstraints(ja, gcr);
        c.add(ja);
        
        JComboBox<String> candidates = new JComboBox<String>();
		for(int i=0; i<users.size(); i++){
			candidates.addItem(users.get(i).getName());
		}
		gcr.weightx = 0.5;
        gcr.weighty = 0.0;
        gcr.gridx = 1;
        gcr.gridy = 0;
        gcr.gridwidth = 1;
        gcr.gridheight = 1;
        mgr.setConstraints(candidates, gcr);
		c.add(candidates);
        
        gcr.weighty = 0.9;
        gcr.weightx = 1.0;
        gcr.gridx = 0;
        gcr.gridy = 1;
        gcr.gridheight = 2;
        gcr.gridwidth = 2;
        JTextArea input = new JTextArea(2, 30);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        mgr.setConstraints(input, gcr);
        c.add(input);
        
        gcr.weighty = 0.0;
        gcr.gridx = 0;
        gcr.gridy = 3;
        gcr.gridheight = 1;
        gcr.gridwidth = 1;
        JButton js = new JButton("Send");
        mgr.setConstraints(js, gcr);
        c.add(js);
        js.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int selectedIndex = candidates.getSelectedIndex();
				String text = input.getText();
				if(text != null && text.length() > 0){
					PublicKey key = users.get(selectedIndex).getKey();
					boolean b = agent.sendPrivateMessage(key, text);
					if(b){
						input.setText("message sent");
						//js.setEnabled(false);
						simulator.appendMessageLineOnBoard("private-->"+agent.getNameFromAddress(key)+"]: "+text);
					}else{
						input.setText("ERROR: message failed");
					}
				}
			}
		});
        
        gcr.weighty = 0.0;
        gcr.gridx = 1;
        gcr.gridy = 3;
        gcr.gridheight = 1;
        gcr.gridwidth = 1;
        JButton jc = new JButton("Cancel");
        mgr.setConstraints(jc, gcr);
        c.add(jc);
		jc.addActionListener(this);
		this.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e){
		this.dispose();
	}
}

