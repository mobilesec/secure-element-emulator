package at.fhooe.usmile.app.se_performance.tests;

import java.util.Random;

import at.fhooe.usmile.app.se_performance.TestPerformerActivity;
import at.fhooe.usmile.app.se_performance.connection.SEConnection;

/**
 * @author Local2013
 *
 */
public class CryptoTest {

	AEStest aesTest;
	ECDHTest ecdhTest;
	SHAtest shaTest;
	RoundTripTest roundtripTest;
	SimpleEchoTest simpleechoTest;
	RSA1024Test rsa1024Test;
	TrippleDEStest trippleDES;

	TestPerformerActivity testerInstance;
	int readerIndex = 0;
	private final static byte[] AID = new byte[] { 0x01, 0x02, 0x03, 0x04,
			0x05, 0x02, 0x00, 0x01 };
	private final static byte[] AIDRSA = new byte[] { 0x01, 0x02, 0x03, 0x04,
		0x05, 0x04, 0x00, 0x01 };
	SEConnection seConn;

	public CryptoTest(TestPerformerActivity _testerInstance,
			SEConnection _seConn, int _readerIndex) {
		testerInstance = _testerInstance;
		seConn = _seConn;
		readerIndex = _readerIndex;
	}

	public boolean performAESTest(int datalen, int loop, int keyLen) {
		if (seConn.selectApplet(AID, readerIndex)) {
			aesTest = new AEStest(testerInstance, seConn, keyLen);
			byte[] testData = new byte[datalen];
			new Random().nextBytes(testData);
			aesTest.startTest(loop, testData);
			return true;
		}
		return false;
	}

	public void stopAESTest() {
		if (aesTest != null) {
			aesTest.stopTest();
		}
	}

	public boolean aesTestRunning() {
		if (aesTest != null) {
			return aesTest.testRunning();
		} else {
			return false;
		}
	}

	public boolean performECDHTest(int loop) {
		if (seConn.selectApplet(AID, readerIndex)) {
			ecdhTest = new ECDHTest(testerInstance, seConn);
			ecdhTest.startTest(loop);
			return true;
		}
		return false;
	}

	public void stopECDHTest() {
		if (ecdhTest != null) {
			ecdhTest.stopTest();
		}
	}

	public boolean ecdhTestRunning() {
		if (ecdhTest != null) {
			return ecdhTest.testRunning();
		} else {
			return false;
		}
	}

	public boolean performRoundTripTest(int datalen, int loop) {
		if (seConn.selectApplet(AID, readerIndex)) {
			roundtripTest = new RoundTripTest(testerInstance, seConn);
			byte[] testData = new byte[datalen];
			new Random().nextBytes(testData);
			roundtripTest.startTest(loop, testData);
			return true;
		}
		return false;
	}

	public void stopRoundTripTest() {
		if (roundtripTest != null) {
			roundtripTest.stopTest();
		}
	}

	public boolean roundtripTestRunning() {
		if (roundtripTest != null) {
			return roundtripTest.testRunning();
		} else {
			return false;
		}
	}

	public boolean performSimpleEchoTest(int datalen, int loop) {
		if (seConn.selectApplet(AID, readerIndex)) {
			simpleechoTest = new SimpleEchoTest(testerInstance, seConn);
            byte[] data = new byte[datalen];
            for (int i = 0; i < data.length; ++i) { data[i] = (byte)((i * 2 + 1) & 0x0ff); }
			simpleechoTest.startTest(loop, data);
			return true;
		}
		return false;
	}

	public void stopSimpleEchoTest() {
		if (simpleechoTest != null) {
			simpleechoTest.stopTest();
		}
	}

	public boolean simpleechoTestRunning() {
		if (simpleechoTest != null) {
			return simpleechoTest.testRunning();
		} else {
			return false;
		}
	}

	public boolean performSHAtest(int datalen, int loop) {
		if (seConn.selectApplet(AID, readerIndex)) {
			shaTest = new SHAtest(testerInstance, seConn);
			byte[] testData = new byte[datalen];
			new Random().nextBytes(testData);
			shaTest.startTest(loop, testData);
			return false;
		}
		return true;
	}

	public void stopSHATest() {
		if (shaTest != null) {
			shaTest.stopTest();
		}
	}

	public boolean SHAtestRunning() {
		if (shaTest != null) {
			return shaTest.testRunning();
		} else {
			return false;
		}
	}
	
	public boolean performRSA1024test(int datalen, int loop) {
		if (seConn.selectApplet(AIDRSA, readerIndex)) {
			rsa1024Test = new RSA1024Test(testerInstance, seConn);
			byte[] testData = new byte[datalen];
			new Random().nextBytes(testData);
			rsa1024Test.startTest(loop, testData);
			return false;
		}
		return true;
	}

	public void stopRSA1024Test() {
		if (rsa1024Test != null) {
			rsa1024Test.stopTest();
		}
	}

	public boolean RSA1024TestRunning() {
		if (rsa1024Test != null) {
			return rsa1024Test.testRunning();
		} else {
			return false;
		}
	}
	
	public boolean perform3DEStest(int datalen, int loop) {
		if (seConn.selectApplet(AIDRSA, readerIndex)) {
			trippleDES = new TrippleDEStest(testerInstance, seConn);
			byte[] testData = new byte[datalen];
			new Random().nextBytes(testData);
			trippleDES.startTest(loop, testData);
			return false;
		}
		return true;
	}

	public void stop3DESTest() {
		if (trippleDES != null) {
			trippleDES.stopTest();
		}
	}

	public boolean trippleDESTestRunning() {
		if (trippleDES != null) {
			return trippleDES.testRunning();
		} else {
			return false;
		}
	}


}
