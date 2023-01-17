package Zeze.Net;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Digest {
	public static byte[] md5(byte[] message) {
		return md5(message, 0, message.length);
	}

	public static byte[] md5(byte[] message, int offset, int len) {
		try {
			var md5 = MessageDigest.getInstance("MD5");
			md5.update(message, offset, len);
			return md5.digest();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static byte[] hmacMd5(byte[] key, byte[] data, int offset, int length) {
		try {
			var mac = Mac.getInstance("HmacMD5");
			mac.init(new SecretKeySpec(key, 0, key.length, "HmacMD5"));
			mac.update(data, offset, length);
			return mac.doFinal();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private Digest() {
	}
}
