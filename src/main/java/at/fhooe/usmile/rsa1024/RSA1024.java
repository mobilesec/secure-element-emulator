/**
 * 
 */
package at.fhooe.usmile.rsa1024;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.DESKey;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RSAPrivateCrtKey;
import javacard.security.RSAPublicKey;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

/**
 * @author Local2013
 * 
 */
public class RSA1024 extends Applet {

	// class byte
	final static byte CLA = (byte) 0x80;

	// instruction bytes
	final static byte PIN_VERIFY = (byte) 0x20;
	final static byte ENCRYPT_RSA = (byte) 0x41;
	final static byte DECRYPT_RSA = (byte) 0x42;
	final static byte SIGN = (byte) 0x43;
	final static byte UPDATE_TOBE_VERIFIED = (byte) 0x44;
	final static byte VERIFY = (byte) 0x45;
	final static byte GET_MODULUS = (byte) 0x46;
	final static byte GET_EXPONENT = (byte) 0x47;
	final static byte SET_MODULUS = (byte) 0x48;
	final static byte SET_EXPONENT = (byte) 0x49;
	
	final static byte ENCRYPT_3DES = (byte) 0x51;
	final static byte DECRYPT_3DES = (byte) 0x52;

	final static byte PIN_LENGTH = (byte) 0x06;
	final static byte PIN_TRY = (byte) 0x03;

	final static short PIN_LENGTH_ERROR = 0x0010;
	final static short PIN_VERIFICATION_FAILED = 0x6300;
	final static short PIN_REQUIRED = 0x0014;
	final static short PIN_BLOCKED = 0x0012;
	final static short SIGNATURE_VERIFICATION_FAILED = 0x0013;
	final static short SIGNATURE_LENGTH_INVALID = 0x0015;
	final static short MODULUS_LENGTH_ERROR = 0x0016;
	final static short EXPONENT_LENGTH_ERROR = 0x0017;
	
	final static byte GEN_KEYPAIR = (byte)0x21;
	
	

	// RSAPrivateKey rsaPrivateKey;
	RSAPrivateCrtKey rsaCrtPrivateKey;
	RSAPublicKey rsaOwnPublicKey;
	// another party's public key ... used for decryption and signature
	// verification
	RSAPublicKey rsaExtPublicKey;

	Cipher rsaCipher;

	KeyPair keyPair;

	Signature signature;
	//byte[] signatureTobeVerified;
	byte[] signatureBuffer;
	
	byte[] defaultExp;
	
	DESKey desKey;
	Cipher cipher;
	byte[] IV;
	
	private RSA1024() {
		
		/*
		 * initialize RSA private and public key variables
		 * initialize signature variable
		 * generate key pair
		 */
		
		// key encryption set to false have a look later ... CRT private key is supportted by most cards including JCOP NXP cards due to computational efficiency
			rsaCrtPrivateKey = (RSAPrivateCrtKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_CRT_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);

			rsaOwnPublicKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		
			rsaExtPublicKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
			
			// init external public exponent to default value
			defaultExp = new byte[]{0x01, 0x00, 0x01};
			
			
			
			/*	keyPair = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_1024);

			 
		 * For the RSA algorithm, if the exponent value in the public key object is pre-initialized, it will be retained. 
		 * Otherwise, a default value of 65537 will be used.
		 
		keyPair.genKeyPair();
				
		rsaCrtPrivateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
		rsaOwnPublicKey = (RSAPublicKey) keyPair.getPublic();*/
		
		// initialize signature variable
		/*
		 *  externalAccess - true indicates that the instance will be shared among multiple applet instances and that the Signature instance will also be accessed
		 *  (via a Shareable interface) when the owner of the Signature instance is not the currently selected applet. 
		 *  If true the implementation must not allocate CLEAR_ON_DESELECT transient space for internal data.
		 */
		signature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
		
		//cipher RSA for encryption and decryption
		rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		
		signatureBuffer = JCSystem.makeTransientByteArray((short) 0x80,
				JCSystem.CLEAR_ON_DESELECT);
	//	signatureTobeVerified = JCSystem.makeTransientByteArray((short)0x80,JCSystem.CLEAR_ON_DESELECT);
		
		
		// des key and cipher initialization
		desKey = (DESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_DES,KeyBuilder.LENGTH_DES3_3KEY, false);
		cipher = Cipher.getInstance(Cipher.ALG_DES_CBC_NOPAD, false);
		IV = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x06, 0x07};
		byte[] dummykey = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x06, 0x07, 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x06, 0x07, 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x06, 0x07};

		desKey.setKey(dummykey, (short)0);
		
	}

	public static void install(byte bArray[], short bOffset, byte bLength)
			throws ISOException {
		new RSA1024().register();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javacard.framework.Applet#process(javacard.framework.APDU)
	 */
	public void process(APDU apdu) throws ISOException {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();

		switch (buf[ISO7816.OFFSET_INS]) {
		case GEN_KEYPAIR:
			generateKeyPair();
			break;
		case SIGN:
			sign(apdu);
			break;
		/*case UPDATE_TOBE_VERIFIED:
			updateToBeVerified(apdu);
			break;*/
		case VERIFY:
			verify(apdu);
			break;
		case ENCRYPT_RSA:
			encrypt_RSA(apdu);
			break;
		case DECRYPT_RSA:
			decrypt_RSA(apdu);
			break;
		case GET_MODULUS:
			getPublicModulus(apdu);
			break;
		case GET_EXPONENT:
			getPublicExponent(apdu);
			break;
		case SET_MODULUS:
			setModulus(apdu);
			break;
		case SET_EXPONENT:
			setExponent(apdu);
			break;
		case ENCRYPT_3DES:
			encrypt3DES(apdu);
			break;
		case DECRYPT_3DES:
			decrypt3DES(apdu);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	public void encrypt3DES(APDU apdu){
		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		// since cbc nopadding is used
		if ((short) (length % 8) != 0) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
 		}

		 cipher.init(desKey, Cipher.MODE_ENCRYPT, IV, (short) 0x00,(short) 0x08);
		 cipher.doFinal(incomingBuf, ISO7816.OFFSET_CDATA,length, incomingBuf, ISO7816.OFFSET_CDATA );
		 apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
	}
	
	public void decrypt3DES(APDU apdu){
		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		// since cbc nopadding is used
		if ((short) (length % 8) != 0) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
 		}

		 cipher.init(desKey, Cipher.MODE_DECRYPT, IV, (short) 0x00,(short) 0x08);
		 cipher.doFinal(incomingBuf, ISO7816.OFFSET_CDATA,length, incomingBuf, ISO7816.OFFSET_CDATA );
		 apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
	}
	
	
	public void generateKeyPair(){
		keyPair = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_1024);
		/* 
		 * For the RSA algorithm, if the exponent value in the public key object is pre-initialized, it will be retained. 
		 * Otherwise, a default value of 65537 will be used.
		 */
		keyPair.genKeyPair();
		rsaCrtPrivateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
		rsaOwnPublicKey = (RSAPublicKey) keyPair.getPublic();
	}

	public void encrypt_RSA(APDU apdu) {
		/* 
		 * max length of data to be encrypted For OAEP padding, recommended for all new applications, it must be less than the size of the key modulus � 41 (all in bytes).
		 * */
		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		rsaCipher.init(rsaExtPublicKey, Cipher.MODE_ENCRYPT);
		// incoming buff used to store cipher
		short cipherLen = rsaCipher.doFinal(incomingBuf, ISO7816.OFFSET_CDATA,
				(short)length, incomingBuf, ISO7816.OFFSET_CDATA);
		// set to apdu to outgoing and send
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, cipherLen);
	}

	public void getPublicModulus(APDU apdu) {

		byte[] incomingBuf = apdu.getBuffer();
		short modLen = rsaOwnPublicKey.getModulus(incomingBuf,
				ISO7816.OFFSET_CDATA);
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, modLen);
	}

	public void getPublicExponent(APDU apdu) {

		byte[] incomingBuf = apdu.getBuffer();
		short expLen = rsaOwnPublicKey.getExponent(incomingBuf,
				ISO7816.OFFSET_CDATA);
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, expLen);
	}

	public void setModulus(APDU apdu) {
		
		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		if (length < (short) 0x80) {
			ISOException.throwIt(MODULUS_LENGTH_ERROR);
		}
		rsaExtPublicKey.setModulus(incomingBuf, ISO7816.OFFSET_CDATA, length);
		rsaExtPublicKey.setExponent(defaultExp, (short)0, (short)3);
	}

	public void setExponent(APDU apdu) {

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		if (length < (short) 0x01) {
			ISOException.throwIt(EXPONENT_LENGTH_ERROR);
		}
		rsaExtPublicKey.setExponent(incomingBuf, ISO7816.OFFSET_CDATA, length);
	}

	public void decrypt_RSA(APDU apdu) {

		rsaCipher.init(rsaCrtPrivateKey, Cipher.MODE_DECRYPT);

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		// incoming buf used to store cipher
		short cipherLen = rsaCipher.doFinal(incomingBuf, ISO7816.OFFSET_CDATA,
				length, incomingBuf, ISO7816.OFFSET_CDATA);
		// set to apdu to outgoing and send
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, cipherLen);
	}

	public void sign(APDU apdu) {

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		signature.init(rsaCrtPrivateKey, Signature.MODE_SIGN);

		

		signature.sign(incomingBuf, ISO7816.OFFSET_CDATA, length,
				signatureBuffer, (byte) 0x00);

		// copy signature to incoming buffer
		Util.arrayCopyNonAtomic(signatureBuffer, (short) 0x00, incomingBuf,
				ISO7816.OFFSET_CDATA, (short) 0x80);
		// clear signature
		// Util.arrayFillNonAtomic(signatureBuffer, (short) 0x00, (short)0x80,
		// (byte) 0x00);
		// set out going and send
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 0x80);

	}

	/*public void updateToBeVerified(APDU apdu) {

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		if (length != (short) 0x80) {
			ISOException.throwIt(SIGNATURE_LENGTH_INVALID);
		}
		
		Util.arrayCopy(incomingBuf, ISO7816.OFFSET_CDATA,
				signatureTobeVerified, (short) 0x00, length);

	}*/

	/*public void verify(APDU apdu) {

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		// initialize signature variable for verification
		signature.init(rsaExtPublicKey, Signature.MODE_VERIFY);

		// verify incoming buffer with to be verified signature
		// verification is done in two steps .... first the signature to be
		// verified has to be filled in using updatateTobeVeried method
		// in this method the raw data (an encrypted) is signed and verified
		// against signatureTobeVerified
		boolean verified = signature.verify(incomingBuf, ISO7816.OFFSET_CDATA,
				length, signatureTobeVerified, (short) 0x00, (short) 0x80);

		// clear to be verified signature
		Util.arrayFillNonAtomic(signatureTobeVerified, (short) 0x00,
				(short) 0x80, (byte) 0x00);

		if (verified) {
			return;
		} else {
			ISOException.throwIt(SIGNATURE_VERIFICATION_FAILED);
		}
	}*/
	
	public void verify(APDU apdu) {

		byte[] incomingBuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();

		// initialize signature variable for verification
		signature.init(rsaExtPublicKey, Signature.MODE_VERIFY);

		/**
		 * the apdu received contains 128 bytes signature value followed by x bytes of original data for verification 
		 * 
		 */
		
		boolean verified = signature.verify(incomingBuf,(short) (ISO7816.OFFSET_CDATA + (short)0x80), (short) (length - (short)0x80),
				  incomingBuf, (short)ISO7816.OFFSET_CDATA , (short) 0x80 );

		if (verified) {
			return;
		} else {
			ISOException.throwIt(SIGNATURE_VERIFICATION_FAILED);
		}
	}

}
