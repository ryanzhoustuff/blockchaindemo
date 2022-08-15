package blockchain;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
//import java.security.Signature;
//import javax.crypto.Cipher;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.io.*;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class UtilityMethods 
{
	private static long uniqueNumber = 0;
	
	public static long getUniqueNumber(){
		return UtilityMethods.uniqueNumber++;
	}
	
	public static KeyPair generateKeyPair()
	{
		try{
			//KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(Configuration.keyPairAlgorithm());
			kpg.initialize(2048);
			KeyPair pair = kpg.generateKeyPair();
			return pair;
		}catch(java.security.NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		}
	}	
	
	public static byte[] generateSignature(PrivateKey privateKey, String message)
	{
		try{
			Signature sig = Signature.getInstance(Configuration.signatureAlgorithm());
			//initializing the Signature instance to sign the digital binary data
			sig.initSign(privateKey);
			sig.update(message.getBytes());
			return sig.sign();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static boolean verifySignature(PublicKey publicKey, byte[] signature, String message)
	{
		try{
			Signature sig2 = Signature.getInstance(Configuration.signatureAlgorithm());
			sig2.initVerify(publicKey);
			sig2.update(message.getBytes());
			return sig2.verify(signature);
		}catch(Exception e){
			//throw new RuntimeException(e);
			e.printStackTrace();
			return false;
		}
	}
	
	public static String messageDigestSHA256_toString(String message)
	{
		return Base64.getEncoder().encodeToString(messageDigestSHA256_toBytes(message));
	}
	
	public static byte[] messageDigestSHA256_toBytes(String message)
	{
		try{
			//MessageDigest md = MessageDigest.getInstance("SHA-256");
			MessageDigest md = MessageDigest.getInstance(Configuration.hashAlgorithm());
			md.update(message.getBytes());
			return md.digest();
		}catch(java.security.NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		}
	}
	
	public static long getTimeStamp()
	{
		return java.util.Calendar.getInstance().getTimeInMillis();
	}
	
	public static String toBinaryString(byte[] hash)
	{
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<hash.length; i++){
			int x = ((int)hash[i])+128; //making it unsigned
			String s = Integer.toBinaryString(x);
			while(s.length() < 8){
				s = "0"+s;
			}
			sb.append(s);
		}
		return sb.toString();
	}
	
	public static boolean hashMeetsDifficultyLevel(String hash, int difficultyLevel)
	{
		char[] c = hash.toCharArray();
		for(int i=0; i<difficultyLevel; i++){
			if(c[i] != '0'){
				return false;
			}
		}
		return true;
	}
	
	public static String getKeyString(Key key)
	{
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}
	
	
	public static void displayTransaction(Transaction T, PrintStream out, int level)
	{
		displayTab(out, level, "Transaction{");
		displayTab(out, level+1, "ID: " + T.getHashID());
		displayTab(out, level+1, "sender: " + UtilityMethods.getKeyString(T.getSender()));
		displayTab(out, level+1, "fundToBeTransferred total: " + T.getTotalFundToTransfer());
		displayTab(out, level+1, "Input:");
		for(int i=0; i<T.getNumberOfInputUTXOs(); i++){
			UTXO ui = T.getInputUTXO(i);
			displayUTXO(ui, out, level + 2);
		}
		
		displayTab(out, level+1, "Output:");
		for(int i=0; i<T.getNumberOfOutputUTXOs()-1; i++){
			UTXO ut = T.getOuputUTXO(i);
			displayUTXO(ut, out, level+2);
		}
		UTXO change = T.getOuputUTXO(T.getNumberOfOutputUTXOs()-1);
		displayTab(out, level+2,"change: "+change.getFundTransferred());
		displayTab(out, level+1, "transaction fee: "+ Transaction.TRANSACTION_FEE);
		boolean b = T.verifySignature();
		displayTab(out, level+1, "signature verification: " + b);
		displayTab(out, level, "}");
	}
	
	public static void displayTab(PrintStream out, int level, String s)
	{
		for(int i=0; i<level; i++){
			out.print("\t");
		}
		out.println(s);
	}
	
	public static void displayUTXO(UTXO ux, PrintStream out, int level)
	{
		displayTab(out, level, "fund: "+ ux.getFundTransferred()+", receiver: "+UtilityMethods.getKeyString(ux.getReceiver()));
	}
	
	public static void displayBlockchain(Blockchain ledger, PrintStream out, int level)
	{
		displayTab(out, level, "Blockchain{ number of blocks: "+ledger.size());
		for(int i=0; i<ledger.size(); i++) {
			Block block = ledger.getBlock(i);
			displayBlock(block, out, level+1);
		}
		displayTab(out, level,"}");
	}
	
	public static void displayBlock(Block block, PrintStream out, int level)
	{
		displayTab(out, level, "Block{");
		displayTab(out, level, "\tID: " + block.getHashID());
		for(int i=0; i<block.getTotalNumberOfTransactions(); i++) {
			displayTransaction(block.getTransaction(i), out, level+1);
		}
		//display the reward transaction
		if(block.getRewardTransaction() != null) {
			displayTab(out, level, "\tReward Transaction:");
			displayTransaction(block.getRewardTransaction(), out, level+1);
		}
		displayTab(out, level, "}");
	}

	
	public static void displayTransaction(Transaction T, StringBuilder out, int level)
	{
		displayTab(out, level, "Transaction{");
		displayTab(out, level+1, "ID: " + T.getHashID());
		displayTab(out, level+1, "sender: " + UtilityMethods.getKeyString(T.getSender()));
		displayTab(out, level+1, "fundToBeTransferred total: " + T.getTotalFundToTransfer());
		displayTab(out, level+1, "Input:");
		for(int i=0; i<T.getNumberOfInputUTXOs(); i++){
			UTXO ui = T.getInputUTXO(i);
			displayUTXO(ui, out, level + 2);
		}
		
		displayTab(out, level+1, "Output:");
		for(int i=0; i<T.getNumberOfOutputUTXOs()-1; i++){
			UTXO ut = T.getOuputUTXO(i);
			displayUTXO(ut, out, level+2);
		}
		UTXO change = T.getOuputUTXO(T.getNumberOfOutputUTXOs()-1);
		displayTab(out, level+2,"change: "+change.getFundTransferred());
		displayTab(out, level+1, "transaction fee: "+ Transaction.TRANSACTION_FEE);
		boolean b = T.verifySignature();
		displayTab(out, level+1, "signature verification: " + b);
		displayTab(out, level, "}");
	}
	
	public static void displayTab(StringBuilder out, int level, String s)
	{
		for(int i=0; i<level; i++){
			out.append("\t");
		}
		out.append(s + System.getProperty("line.separator"));
	}
	
	public static void displayUTXO(UTXO ux, StringBuilder out, int level)
	{
		displayTab(out, level, "fund: "+ ux.getFundTransferred()+", receiver: "+UtilityMethods.getKeyString(ux.getReceiver()));
	}
	
	public static void displayBlockchain(Blockchain ledger, StringBuilder out, int level)
	{
		displayTab(out, level, "Blockchain{ number of blocks: "+ledger.size());
		for(int i=0; i<ledger.size(); i++) {
			Block block = ledger.getBlock(i);
			displayBlock(block, out, level+1);
		}
		displayTab(out, level,"}");
	}
	
	public static void displayBlock(Block block, StringBuilder out, int level)
	{
		displayTab(out, level, "Block{");
		displayTab(out, level, "\tID: " + block.getHashID());
		for(int i=0; i<block.getTotalNumberOfTransactions(); i++) {
			displayTransaction(block.getTransaction(i), out, level+1);
		}
		//display the reward transaction
		if(block.getRewardTransaction() != null) {
			displayTab(out, level, "\tReward Transaction:");
			displayTransaction(block.getRewardTransaction(), out, level+1);
		}
		displayTab(out, level, "}");
	}
	
	
	
	public static byte[] longToBytes(long v)
	{
		byte[] b = new byte[Long.BYTES];
		for(int i=b.length-1; i>=0; i--){
			b[i] = (byte)(v & 0xFFFF);
			v = v >> Byte.SIZE;
		}
		return b;
	}
	
	/**
	 * the byte array must be of size 8
	 * @param b
	 */
	public static long bytesToLong(byte[] b)
	{
		long v = 0L;
		for(int i=0; i<b.length; i++){
			v = v << Byte.SIZE;
			v = v | (b[i] & 0xFFFF);
		}
		return v;
	}
	
	public static byte[] intToBytes(int v)
	{
		byte[] b = new byte[Integer.BYTES];
		for(int i=b.length-1; i>=0; i--){
			b[i] = (byte)(v & 0xFF);
			v = v >> Byte.SIZE;
		}
		return b;
	}
	
	/**
	 * the byte array must be of size 4
	 * @param b
	 */
	public static int bytesToInt(byte[] b)
	{
		int v = 0;
		for(int i=0; i<b.length; i++){
			v = v << Byte.SIZE;
			v = v | (b[i] & 0xFF);
		}
		return v;
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public static byte[] encryptionByXOR(byte[] key, String password)
	{
		int more = 100;
		byte[] p = UtilityMethods.messageDigestSHA256_toBytes(password);
		byte[] pwds = new byte[p.length * more];
		for(int i=0,z=0; i<more; i++){
			for(int j=0; j<p.length; j++,z++){
				pwds[z] = p[j];
			}
		}
		byte[] result = new byte[key.length];
		int i = 0;
		for(i=0; i<key.length && i<pwds.length; i++){
			result[i] =(byte)((key[i] ^ pwds[i]) & 0xFF);
		}
		while(i < key.length){
			result[i] = key[i];
			i++;
		}
		return result;
	}
	
	/**
	 * used only for decrypting a key which should never be more than 1000 bytes
	 * @param keyIn
	 * @param password
	 * @return
	 */
	public static byte[] decryptionByXOR(FileInputStream keyIn, String password)
	{
		try{
			byte[] data = new byte[4096];
			int size = keyIn.read(data);
			byte[] result = new byte[size];
			for(int i=0; i<result.length; i++){
				result[i] = data[i];
			}
			return decryptionByXOR(result, password);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	
	
	public static byte[] decryptionByXOR(byte[] key, String password)
	{
		return encryptionByXOR(key, password);
	}
	
	
	public static byte[] encryptionByAES(byte[] key, String password)
	{
		try{
			byte[] salt = new byte[8];
			SecureRandom rand = new SecureRandom();
			rand.nextBytes(salt);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1024, 128);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			AlgorithmParameters params = cipher.getParameters();
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			byte[] output = cipher.doFinal(key);
			//using a variable to record the length of the output
			byte[] outputSizeBytes = intToBytes(output.length);
			byte[] ivSizeBytes = intToBytes(iv.length);
			byte[] data = new byte[Integer.BYTES * 2 + salt.length + iv.length + output.length];
			//the order of the data is arranged as the following:
			//int-forDataSize+int-forIVsize+8-byte-salt+iv-bytes+output-bytes
			int z = 0;
			for(int i=0; i<outputSizeBytes.length; i++, z++){
				data[z] = outputSizeBytes[i];
			}
			for(int i=0; i<ivSizeBytes.length; i++,z++){
				data[z] = ivSizeBytes[i];
			}
			for(int i=0; i<salt.length; i++,z++){
				data[z] = salt[i];
			}
			for(int i=0; i<iv.length; i++,z++){
				data[z] = iv[i];
			}
			for(int i=0; i<output.length; i++,z++){
				data[z] = output[i];
			}
			return data;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	
	public static byte[] decryptionByAES(byte[] key, String password)
	{
		try{
			//divide the input data key[] into proper values
			//remember the order of the data is:
			//int-forDataSize+int-forIVsize+8-byte-salt+iv-bytes+output-bytes
			int z = 0;
			byte[] lengthByte = new byte[Integer.BYTES];
			for(int i=0; i<lengthByte.length; i++, z++){
				lengthByte[i] = key[z];
			}
			int dataSize = bytesToInt(lengthByte);
			for(int i=0; i<lengthByte.length; i++, z++){
				lengthByte[i] = key[z];
			}
			int ivSize = bytesToInt(lengthByte);
			byte[] salt = new byte[8];
			for(int i=0; i<salt.length; i++,z++){
				salt[i] = key[z];
			}
			//iv bytes
			byte[] ivBytes = new byte[ivSize];
			for(int i=0; i<ivBytes.length; i++, z++){
				ivBytes[i] = key[z];
			}
			//real data bytes
			byte[] dataBytes = new byte[dataSize];
			for(int i=0; i<dataBytes.length; i++, z++){
				dataBytes[i] = key[z];
			}
			//now, once data are ready, reconstruct the key and cipher
			PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, 1024, 128);
			SecretKeyFactory secretKeyFactory = SecretKeyFactory
					.getInstance("PBKDF2WithHmacSHA1");
			SecretKey tmp = secretKeyFactory.generateSecret(pbeKeySpec);
			SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher cipher2 = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher2.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
			byte[] data = cipher2.doFinal(dataBytes);
			return data;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * The merkle tree could be a binary tree, or triple tree, or ...
	 * Here it is a binary tree model.
	 * <p>The input size could be of an even or odd number. So, I use a recursive algorithm to
	 * compute the merkle tree root hash
	 * @param hashes
	 * @return
	 */
	public static String computeMerkleTreeRootHash(String[] hashes)
	{
		return computeMerkleTreeRootHash(hashes, 0, hashes.length-1);
	}
	
	/**
	 * @param hashes
	 * @param from
	 * @param end
	 * @return
	 */
	private static String computeMerkleTreeRootHash(String[] hashes, int from, int end)
	{
		//there is only one string, then return this string
		if(end-from+1 == 1){
			return hashes[end];
		}else if(end-from+1 == 2){
			//compute the hashID from the two 
			return messageDigestSHA256_toString(hashes[from]+hashes[end]);
		}else{
			//we need continue divide the array into two parts
			int c = (from+end)/2;
			String mesg = computeMerkleTreeRootHash(hashes,from, c)+computeMerkleTreeRootHash(hashes, c+1,end);
			return messageDigestSHA256_toString(mesg);
		}
	}
	
	public static int guaranteeIntegerInputByScanner(java.util.Scanner in, int lowerBound, int upperBound) 
	{
		int x = -1;
		try{
			x = in.nextInt();
		}catch(java.util.InputMismatchException ee){
			x = lowerBound - 1;
		}
		while(x < lowerBound || x > upperBound){
			System.out.println("You selected " + x+", please only enter an integer betweeen " + lowerBound+" and " + upperBound+" inclusively");
			try{
				x = in.nextInt();
			}catch(java.util.InputMismatchException e){
				in.nextLine();
				x = lowerBound-1;
			}
		}		
		//digest the ENTER for later uses
		in.nextLine();
		return x;
	}
	
}

