package at.fhooe.usmile.app.se_performance;



import at.fhooe.usmile.app.se_performance.connection.SEConnection;
import at.fhooe.usmile.app.se_performance.tests.CryptoTest;

/**
 * @author Local2013
 *
 */
public class TestPerformer2Activity extends TestPerformerActivity {

    final static int REPEAT = 20;
    
    @Override
	public void run() {
		cryptoTest = new CryptoTest(this, new SEConnection(null), 0);

        for (int i = 0; i < REPEAT; ++i) {
            cryptoTest.performSimpleEchoTest(0, 500);
            while (cryptoTest.simpleechoTestRunning()) {}
		}
	}
}
