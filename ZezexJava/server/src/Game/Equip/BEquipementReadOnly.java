package Game.Equip;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BEquipementReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getBagPos();
}