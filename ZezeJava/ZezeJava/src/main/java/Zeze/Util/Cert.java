package Zeze.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

// 生成KeyStore: keytool -genkeypair -keyalg RSA -keysize 2048 -keystore test.ks -storetype pkcs12 -storepass 123456 -alias test -validity 365 -dname "cn=CommonName, ou=OrgName, o=Org, c=Country"
// 查看KeyStore: keytool -list -keystore test.ks -storepass 123456 -v
// 导出PEM格式公钥证书: keytool -exportcert -keystore test.ks -storepass 123456 -alias test -rfc -file test.pem
// 导出DER格式公钥证书: keytool -exportcert -keystore test.ks -storepass 123456 -alias test -file test.der
// 以上两种格式都可以用.cer或.crt后缀名, PEM和DER两种编码格式可以用OpenSSL工具转换
// 参考: https://docs.oracle.com/en/java/javase/11/tools/keytool.html
// 参考: https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html
// 编译时需要: --add-exports java.base/sun.security.x509=ALL-UNNAMED
public final class Cert {
	static {
		try {
			var m = Module.class.getDeclaredMethod("implAddOpensToAllUnnamed", String.class);
			Json.setAccessible(m); // force accessible
			m.invoke(Certificate.class.getModule(), "sun.security.x509"); // --add-opens java.base/sun.security.x509=ALL-UNNAMED
			// m.invoke(Certificate.class.getModule(), "sun.security.rsa"); // --add-opens java.base/sun.security.rsa=ALL-UNNAMED
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	// 从输入流加载KeyStore(PKCS12格式的二进制密钥存储格式,有密码加密,包含私钥和公钥证书)
	public static KeyStore loadKeyStore(InputStream inputStream, String passwd)
			throws GeneralSecurityException, IOException {
		var keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(inputStream, passwd != null ? passwd.toCharArray() : null);
		return keyStore;
	}

	// 从KeyStore里获取公钥证书
	public static Certificate getCertificate(KeyStore keyStore, String alias) throws KeyStoreException {
		return keyStore.getCertificate(alias);
	}

	// 从KeyStore里获取公钥
	public static PublicKey getPublicKey(KeyStore keyStore, String alias) throws KeyStoreException {
		return keyStore.getCertificate(alias).getPublicKey();
	}

	// 从KeyStore里获取私钥
	public static PrivateKey getPrivateKey(KeyStore keyStore, String passwd, String alias)
			throws GeneralSecurityException {
		return (PrivateKey)keyStore.getKey(alias, passwd != null ? passwd.toCharArray() : null);
	}

	// 从二进制编码加载X509公钥证书(二进制编码即Certificate.getEncoded()的结果)
	public static X509CertImpl loadCertificate(byte[] encodedCertificate) throws GeneralSecurityException {
		return new X509CertImpl(encodedCertificate);
	}

	// 从二进制或PEM编码的输入流加载X509公钥证书
	public static X509CertImpl loadCertificate(InputStream encodedCertificate) throws GeneralSecurityException {
		return new X509CertImpl(encodedCertificate);
	}

	// 从二进制编码加载RSA公钥(二进制编码即PublicKey.getEncoded()的结果)
	public static PublicKey loadPublicKey(byte[] encodedPublicKey) throws GeneralSecurityException {
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encodedPublicKey));
	}

	// 从二进制编码加载RSA公钥(二进制编码即PublicKey.getEncoded()的结果)
	// 编译和运行时需要: --add-exports/--add-opens java.base/sun.security.rsa=ALL-UNNAMED
//	public static PublicKey loadPublicKey1(byte[] encodedPublicKey) throws InvalidKeyException {
//		return sun.security.rsa.RSAPublicKeyImpl.newKey(sun.security.rsa.RSAUtil.KeyType.RSA, "PKCS#1", encodedPublicKey);
//	}

	// 从二进制编码加载RSA私钥(二进制编码即PrivateKey.getEncoded()的结果)
	public static PrivateKey loadPrivateKey(byte[] encodedPrivateKey) throws GeneralSecurityException {
		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));
	}

	// 生成RSA密钥对(公钥+私钥)
	public static KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
		var keyPairGen = KeyPairGenerator.getInstance("RSA");
		keyPairGen.initialize(2048);
		return keyPairGen.generateKeyPair();
	}

	// 为RSA公钥和私钥生成自签名的公钥证书并连同私钥保存到用密码加密的KeyStore输出流
	public static X509Certificate generate(String ownerName, PublicKey publicKey, String issuer, PrivateKey privateKey, int validDays)
			throws GeneralSecurityException, IOException {
		var certInfo = new X509CertInfo();
		var owner = new X500Name("CN=" + ownerName);
		certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
		certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(160, new SecureRandom())));
		try {
			certInfo.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
		} catch (CertificateException ignore) {
			certInfo.set(X509CertInfo.SUBJECT, owner);
		}
		var issuerX = new X500Name("CN=", issuer);
		try {
			certInfo.set(X509CertInfo.ISSUER, new CertificateIssuerName(issuerX));
		} catch (CertificateException ignore) {
			certInfo.set(X509CertInfo.ISSUER, issuerX);
		}
		var now = System.currentTimeMillis();
		var endTime = now + validDays * 86400_000L;
		certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(new Date(now), new Date(endTime)));
		certInfo.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
		var algoId = AlgorithmId.get("1.2.840.113549.1.1.11"); // SHA256withRSA
		certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algoId));
		certInfo.set(CertificateAlgorithmId.NAME + '.' + CertificateAlgorithmId.ALGORITHM, algoId);

		var cert = new X509CertImpl(certInfo);
		cert.sign(privateKey, "SHA256withRSA"); // 自签名
		// cert.verify(publicKey); // 此行可选
		return cert;
	}

	public static void generate(OutputStream outputStream, String passwd, String alias, PublicKey publicKey,
								PrivateKey privateKey, String commonName, int validDays)
			throws GeneralSecurityException, IOException {
		var certInfo = new X509CertInfo();
		var owner = new X500Name("CN=" + commonName);
		certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
		certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));
		try {
			certInfo.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
		} catch (CertificateException ignore) {
			certInfo.set(X509CertInfo.SUBJECT, owner);
		}
		try {
			certInfo.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
		} catch (CertificateException ignore) {
			certInfo.set(X509CertInfo.ISSUER, owner);
		}
		var now = System.currentTimeMillis();
		var endTime = now + validDays * 86400_000L;
		certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(new Date(now), new Date(endTime)));
		certInfo.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
		var algoId = AlgorithmId.get("1.2.840.113549.1.1.11"); // SHA256withRSA
		certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algoId));
		certInfo.set(CertificateAlgorithmId.NAME + '.' + CertificateAlgorithmId.ALGORITHM, algoId);

		var cert = new X509CertImpl(certInfo);
		cert.sign(privateKey, "SHA256withRSA"); // 自签名
		cert.verify(publicKey); // 此行可选

		var keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(null, null);
		// keyStore.setCertificateEntry(alias, cert);
		keyStore.setKeyEntry(alias, privateKey, null, new Certificate[]{cert});
		keyStore.store(outputStream, passwd != null ? passwd.toCharArray() : null);
	}

	// 使用RSA私钥对数据签名
	public static byte[] sign(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
		return sign(privateKey, data, 0, data.length);
	}

	// 使用RSA私钥对数据签名
	public static byte[] sign(PrivateKey privateKey, byte[] data, int offset, int count)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initSign(privateKey);
		signer.update(data, offset, count);
		return signer.sign();
	}

	// 使用RSA公钥验证签名
	public static boolean verifySign(PublicKey publicKey, byte[] data, byte[] signature)
			throws GeneralSecurityException {
		return verifySign(publicKey, data, 0, data.length, signature);
	}

	// 使用RSA公钥验证签名
	public static boolean verifySign(PublicKey publicKey, byte[] data, int offset, int count, byte[] signature)
			throws GeneralSecurityException {
		var signer = Signature.getInstance("SHA256WithRSA");
		signer.initVerify(publicKey);
		signer.update(data, offset, count);
		return signer.verify(signature);
	}

	// 使用RSA公钥加密小块数据(data长度不超过:RSA位数/8-11)
	public static byte[] encryptRsa(PublicKey publicKey, byte[] data) throws GeneralSecurityException {
		return encryptRsa(publicKey, data, 0, data.length);
	}

	// 使用RSA公钥加密小块数据(size不超过:RSA位数/8-11)
	public static byte[] encryptRsa(PublicKey publicKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(data, offset, size);
	}

	// 使用RSA私钥解密小块数据
	public static byte[] decryptRsa(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
		return decryptRsa(privateKey, data, 0, data.length);
	}

	// 使用RSA私钥解密小块数据
	public static byte[] decryptRsa(PrivateKey privateKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data, offset, size);
	}

	// 使用RSA私钥解密小块数据(不处理padding的原始数据)
	public static byte[] decryptRsaNoPadding(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
		return decryptRsaNoPadding(privateKey, data, 0, data.length);
	}

	// 使用RSA私钥解密小块数据(不处理padding的原始数据)
	public static byte[] decryptRsaNoPadding(PrivateKey privateKey, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(data, offset, size);
	}

	// 创建安全随机的AES密钥(固定256位)
	public static SecretKey generateAesKey() throws NoSuchAlgorithmException {
		var keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(256);
		return keyGenerator.generateKey();
	}

	// 加载自定义的AES密钥
	public static SecretKey loadAesKey(byte[] key) {
		return new SecretKeySpec(key, "AES");
	}

	// 创建安全随机的IV(固定128位)
	public static byte[] generateAesIv() {
		var iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

	// 使用AES加密数据(CBC模式需要提供IV,带padding)
	public static byte[] encryptAes(SecretKey key, byte[] iv, byte[] data) throws GeneralSecurityException {
		return encryptAes(key, iv, data, 0, data.length);
	}

	// 使用AES加密数据(CBC模式需要提供IV,带padding)
	public static byte[] encryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	// 使用AES解密数据(CBC模式需要提供IV,带padding)
	public static byte[] decryptAes(SecretKey key, byte[] iv, byte[] data) throws GeneralSecurityException {
		return decryptAes(key, iv, data, 0, data.length);
	}

	// 使用AES解密数据(CBC模式需要提供IV,带padding)
	public static byte[] decryptAes(SecretKey key, byte[] iv, byte[] data, int offset, int size)
			throws GeneralSecurityException {
		var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		return cipher.doFinal(data, offset, size);
	}

	private Cert() {
	}
}
