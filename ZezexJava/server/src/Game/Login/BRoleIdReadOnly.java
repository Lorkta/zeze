package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BRoleIdReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public long getId();
}