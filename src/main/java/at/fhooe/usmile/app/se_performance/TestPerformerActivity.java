package at.fhooe.usmile.app.se_performance;



import at.fhooe.usmile.app.se_performance.connection.SEConnection;
import at.fhooe.usmile.app.se_performance.tests.CryptoTest;
import at.mroland.logging.Logging;

/**
 * @author Local2013
 *
 */
public class TestPerformerActivity {

    final static int DATALENGTH[] = { 16, 23, 48, 64, 128 };
    final static int DATALENGTH_RSA1024[] = { 16, 23, 48, 64, 87 };
    final static int REPEAT = DATALENGTH.length;
    final static int ITERATIONS = 20;
	
	CryptoTest cryptoTest ;

	public void run() {
		cryptoTest = new CryptoTest(this, new SEConnection(null), 0);

        for (int i = 0; i < REPEAT; ++i) {
//            cryptoTest.performSimpleEchoTest(0, 1);
//            while (cryptoTest.simpleechoTestRunning()) {}
//            cryptoTest.performSimpleEchoTest(0, 500);
//            while (cryptoTest.simpleechoTestRunning()) {}
            
            cryptoTest.performAESTest(DATALENGTH[i], ITERATIONS, 128);
            while (cryptoTest.aesTestRunning()) {}
            cryptoTest.performAESTest(DATALENGTH[i], ITERATIONS, 256);
            while (cryptoTest.aesTestRunning()) {}
            cryptoTest.performSHAtest(DATALENGTH[i], ITERATIONS);
            while (cryptoTest.SHAtestRunning()) {}
            cryptoTest.perform3DEStest(DATALENGTH[i], ITERATIONS);
			while (cryptoTest.trippleDESTestRunning()) {}
            cryptoTest.performECDHTest(ITERATIONS);
            while (cryptoTest.ecdhTestRunning()) {}
            cryptoTest.performRoundTripTest(DATALENGTH[i], ITERATIONS);
            while (cryptoTest.roundtripTestRunning()) {}
            // RSA test
            cryptoTest.performRSA1024test(DATALENGTH_RSA1024[i], ITERATIONS);
            while (cryptoTest.RSA1024TestRunning()) {}
		}
	}

	public void logText(String msg) {
		Logging.info("SEPerformanceTester", msg);
	}
    
	public void logTextSpecial(String msg) {
		Logging.info("SEPerformanceTester", msg);
	}
}
