package Dbh2;

import Zeze.Serialize.ByteBuffer;

public class TmpMain {
	public static void main(String [] args) {
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0xFA, (byte)0xAE, (byte)0xE5 }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x71, (byte)0x7D, (byte)0x1F, (byte)0xAA }).ReadLong());
		System.out.println(ByteBuffer.Wrap(new byte[] { (byte)0x72, (byte)0xFC, (byte)0xA2, (byte)0x39 }).ReadLong());
	}
}