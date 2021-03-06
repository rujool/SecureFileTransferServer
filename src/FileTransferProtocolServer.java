import java.nio.ByteBuffer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class FileTransferProtocolServer {
	
	private PrivateKey serverPrivateKey;
	
	public PrivateKey getServerPrivateKey() {
		return serverPrivateKey;
	}

	public void setServerPrivateKey(PrivateKey serverPrivateKey) {
		this.serverPrivateKey = serverPrivateKey;
	}

	public long generateRandomNonce() {
		
		SecureRandom random = new SecureRandom();
		return random.nextLong();
	}
	
	public void receiveFileFromClient(Socket socket, DataInputStream dis, String fileName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		// Client uploads file to server
		
		int IVlength = dis.readInt();
		byte[] encryptedIV = new byte[IVlength];
		
		if(IVlength>0){
			dis.read(encryptedIV, 0, encryptedIV.length);
			//dis.close();

			//String encrypted = new String(data, 0, data.length);
			//System.out.println(encrypted);
			long decryptedNonce = this.decrypted(this.getServerPrivateKey(), encryptedIV);
			FileServer.showMessage("The decrypted Nonce is: " + decryptedNonce + "\n\n");
		}
		
		FileOutputStream fos = new FileOutputStream(fileName);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		byte[] fileByte = new byte[64];
		int bytesRead = 0;
		while(bytesRead != -1) {
			bytesRead = dis.read(fileByte,0,fileByte.length);
			if(bytesRead > 0) {
				bos.write(fileByte,0,bytesRead);
			}
		}
		bos.close();
		fos.close();
	}
	
	public long decrypted(PrivateKey key, byte[] encrypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		//Decrypt the nonce
		Cipher ci = Cipher.getInstance("RSA");	
		ci.init(Cipher.DECRYPT_MODE, key);
		byte[] decrypted = ci.doFinal(encrypted);
		long ldecrypted = bytesToLong(decrypted);
		//System.err.println(decrypted);
		//System.out.println("The decrypted random nonce is: " + ldecrypted);
		return ldecrypted;
		} 
	
	
	
	
	public byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(x);
	    return buffer.array();
	}
	
	public long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}
	
	public long getEncryptionKey(long sessionKey) {
		if(sessionKey > 0) {
			return sessionKey - 1;
		}
		return sessionKey + 1;
	}
	public long getIntegrityKey(long sessionKey) {
		if(sessionKey > 0) {
			return sessionKey - 2;
		}
		return sessionKey + 2;
	}
}
