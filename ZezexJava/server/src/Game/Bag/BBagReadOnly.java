package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BBagReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getMoney();
	public int getCapacity();
	 public System.Collections.Generic.IReadOnlyDictionary<Integer,Game.Bag.BItemReadOnly> getItems();
}