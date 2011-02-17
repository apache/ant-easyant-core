package org.apache.easyant.plugins.coverage;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Simple jUnit 4 unit test that can be run with a coverage
 * tool to test code coverage plugins.
 */
public class HelloWorldTest {

	@Test
	public void testHello() {
		assertEquals("Bonjour, le monde.", new HelloWorld().hello());
	}
	
}

