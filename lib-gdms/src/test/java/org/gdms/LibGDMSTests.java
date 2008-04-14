package org.gdms;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.gdms.sql.function.spatial.ExtractTest;


public class LibGDMSTests extends TestSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for lib-gdms");
		// $JUnit-BEGIN$
		suite.addTestSuite(ExtractTest.class);
		// $JUnit-END$
		return suite;
	}
}