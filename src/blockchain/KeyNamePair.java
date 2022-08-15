package blockchain;

import java.security.PublicKey;

public class KeyNamePair implements java.io.Serializable
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private PublicKey key;
	private String name;
	public KeyNamePair(PublicKey key, String name){
		this.key = key;
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	public PublicKey getKey(){
		return this.key;
	}
}

