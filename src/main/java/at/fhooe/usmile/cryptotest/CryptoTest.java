package at.fhooe.usmile.cryptotest;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.ECPrivateKey;
import javacard.security.ECPublicKey;
import javacard.security.KeyAgreement;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.MessageDigest;
import javacardx.crypto.Cipher;

public class CryptoTest extends Applet {

	ECPrivateKey ecPrivate;
	ECPublicKey ecPublic;
	byte[] extPublic;

	KeyPair keyPair;
	KeyAgreement keyAgreement;

	final static byte SEND_TEST = 0x51;
	final static byte RECIEVE_TEST = 0x52;
	final static byte ECHO_TEST = 0x53;

	final static byte GEN_KEY_PAIR = 0x10;
	final static byte GET_OWN_PUBLIC = 0x11;
	final static byte SET_EXT_PUBLIC = 0x12;
	final static byte DERIVE_SHARED_SECRET = 0x13;
	final static byte COMPLETE_ECDH = 0x14;

	final static short LENGTH_PUPLIC_PARAM = (short) 0x31;

	byte[] sharedSecret;

	short sharedSecretLen;

	/**
	 *  for AES
	 */
	final static byte ENCRYPT_AES128 = (byte) 0x41;
	final static byte DECRYPT_AES128 = (byte) 0x42;

	final static byte ENCRYPT_AES192 = (byte) 0x43;
	final static byte DECRYPT_AES192 = (byte) 0x44;

	final static byte ENCRYPT_AES256 = (byte) 0x45;
	final static byte DECRYPT_AES256 = (byte) 0x46;

	final static byte DIGEST_256 = (byte) 0x26;
	 

	AESKey aesKey128;
	AESKey aesKey256;

	final static short KEY_SIZE_128 = 0x0080;
	final static short IV_SIZE_128 = 0x0080;

	final static short KEY_SIZE_256 = 0x0100;

	final static short DIGEST_SIZE = 0x0020;

	Cipher encryptionCipher;

	MessageDigest msgDigest_SHA256;
 

	byte[] temp;

	byte[] encryptedBuffer;

	byte[] IV;


	public CryptoTest() {

		ecPrivate = (ECPrivateKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_EC_FP_PRIVATE, KeyBuilder.LENGTH_EC_FP_192,
				false);
		ecPublic = (ECPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_192,
				false);
		ecPublic = (ECPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_192,
				false);
		sharedSecret = JCSystem.makeTransientByteArray((short) 0x20,
				JCSystem.CLEAR_ON_DESELECT);
		extPublic = JCSystem.makeTransientByteArray((short) 0x31,
				JCSystem.CLEAR_ON_DESELECT);

		keyPair = new KeyPair(KeyPair.ALG_EC_FP, KeyBuilder.LENGTH_EC_FP_192);
		
		keyAgreement = KeyAgreement.getInstance(KeyAgreement.ALG_EC_SVDP_DH,
				false);

		// AES Init
		// initialize key AES 128
		aesKey128 = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES,
				KeyBuilder.LENGTH_AES_128, false);

		temp = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x06,
				0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x00,
				0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
				0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
		aesKey128.setKey(temp, (short) 16);

		// initialize Key 256
		aesKey256 = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES,
				KeyBuilder.LENGTH_AES_256, false);
		aesKey256.setKey(temp, (short) 0);

		Util.arrayFillNonAtomic(temp, (short) 0, (short) temp.length, (byte) 0);

		encryptionCipher = Cipher.getInstance(
				Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		
		IV = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x06, 0x07,
				0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

		msgDigest_SHA256 = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
 
		
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration

		new CryptoTest().register();
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] incomingBuf = apdu.getBuffer();
		short len = (short) 0;
		switch (incomingBuf[ISO7816.OFFSET_INS]) {
		case GEN_KEY_PAIR:
			// key pair generation

			keyPair.genKeyPair();
			ecPrivate = (ECPrivateKey) keyPair.getPrivate();
			ecPublic = (ECPublicKey) keyPair.getPublic();

			break;
		case GET_OWN_PUBLIC:
			// send public part
			ecPublic.getW(incomingBuf, ISO7816.OFFSET_CDATA);
			// ecPublic.get
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, LENGTH_PUPLIC_PARAM);
			break;
		case SET_EXT_PUBLIC:
			apdu.setIncomingAndReceive();
			Util.arrayCopyNonAtomic(incomingBuf, ISO7816.OFFSET_CDATA,
					extPublic, (short) 0x00, LENGTH_PUPLIC_PARAM);
			break;
		case DERIVE_SHARED_SECRET:

			keyAgreement.init(ecPrivate);
			sharedSecretLen = keyAgreement.generateSecret(extPublic,
					(short) 0x00, LENGTH_PUPLIC_PARAM, sharedSecret, (short) 0);

			break;
		case COMPLETE_ECDH:

			// initialize key agreement with private key ... then generate
			// session key using incoming public
			apdu.setIncomingAndReceive();
			Util.arrayCopyNonAtomic(incomingBuf, ISO7816.OFFSET_CDATA,
					extPublic, (short) 0x00, LENGTH_PUPLIC_PARAM);
			// key pair generation

			keyPair.genKeyPair();
			ecPrivate = (ECPrivateKey) keyPair.getPrivate();
			ecPublic = (ECPublicKey) keyPair.getPublic();

			// send public part
			ecPublic.getW(incomingBuf, ISO7816.OFFSET_CDATA);
			// ecPublic.get
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, LENGTH_PUPLIC_PARAM);

			keyAgreement.init(ecPrivate);
			sharedSecretLen = keyAgreement.generateSecret(extPublic,
					(short) 0x00, LENGTH_PUPLIC_PARAM, sharedSecret, (short) 0);

			break;

		case ENCRYPT_AES128:
			encryptAES(apdu, (short)128);
			break;
		case DECRYPT_AES128:
			decryptAES(apdu, (short)128);
			break;
		case ENCRYPT_AES256:
			encryptAES(apdu, (short)256);
			break;
		case DECRYPT_AES256:
			decryptAES(apdu, (short)256);
			break;
		case DIGEST_256:
			messageDigest(apdu);
			break; 
		case RECIEVE_TEST:
			len = apdu.setOutgoing();
			apdu.setOutgoingLength(len);
			apdu.sendBytes((short) 0, len);
			break;
		case SEND_TEST:
			len = apdu.setIncomingAndReceive();
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 0);
			break;
		case ECHO_TEST:
			len = apdu.setIncomingAndReceive();
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, len);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}

	}

	public void encryptAES(APDU apdu, short keyLen) {

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		// since there is no padding implementation check for data size
		if ((short) (length % 16) != 0) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
 		}

		// get IV value
		// ivParam.getKey(IV, (short)0x00);
		if(keyLen == (short)128){
			encryptionCipher.init(aesKey128, Cipher.MODE_ENCRYPT, IV, (short) 0x00,(short) 0x10);
		}else{
			encryptionCipher.init(aesKey256, Cipher.MODE_ENCRYPT, IV, (short) 0x00,(short) 0x10);

		}
		

		encryptionCipher.doFinal(incomingBuf, ISO7816.OFFSET_CDATA, length,
				incomingBuf, ISO7816.OFFSET_CDATA);

		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
	}

	public void decryptAES(APDU apdu, short keyLen) {

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		// since there is no padding implementation check for data size
		if ((short) (length % 16) != 0) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		// get IV value
		// ivParam.getKey(IV, (short)0x00);
		if(keyLen == (short)128){
			encryptionCipher.init(aesKey128, Cipher.MODE_DECRYPT, IV, (short) 0x00,(short) 0x10);
		}else{
			encryptionCipher.init(aesKey256, Cipher.MODE_DECRYPT, IV, (short) 0x00,(short) 0x10);

		}
		 
		encryptionCipher.doFinal(incomingBuf, ISO7816.OFFSET_CDATA, length,
				incomingBuf, ISO7816.OFFSET_CDATA);

		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
	}

	public void messageDigest(APDU apdu ) {

		msgDigest_SHA256.reset();
		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		msgDigest_SHA256.doFinal(incomingBuf, ISO7816.OFFSET_CDATA, length,
					incomingBuf, ISO7816.OFFSET_CDATA);

		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 0x20);

	}
}