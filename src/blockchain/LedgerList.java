package blockchain;

import java.util.ArrayList;

public class LedgerList<T> implements java.io.Serializable
{
	/**
	 * later, any modification of this class (different version) should update the serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<T> list;
	
	public LedgerList(){
		list = new ArrayList<T>();
	}
	
	public int size(){
		return this.list.size();
	}
	
	public T getLast(){
		return this.list.get(size()-1);
	}
	
	public T getFirst(){
		return this.list.get(0);
	}
	
	public boolean add(T e){
		return this.list.add(e);
	}
	
	/**
	 * If index is outside of the range, null is returned.
	 * @param index
	 * @return
	 */
	public T findByIndex(int index)
	{
		if(index < 0  || index >= size()){
			return null;
		}else{
			return this.list.get(index);
		}
	}
	
	
	
	/**
	 * The copy is not a deep copy. The ledger list is a new list, but the elements
	 * are the same elements and they are in the same order.
	 * In addition, if the order in the original
	 * LedgerList is changed, it does not impact this copy.
	 * @return
	 */
	public synchronized LedgerList<T> copy(){
		LedgerList<T> ledger = new LedgerList<T>();
		for(int i=0; i<this.list.size(); i++){
			ledger.add(this.list.get(i));
		}
		return ledger;
	}
}
