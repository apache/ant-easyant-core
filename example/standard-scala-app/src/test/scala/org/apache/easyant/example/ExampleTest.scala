package org.apache.easyant.example;

import java.util.Properties;

import junit.Test
import junit.Assert.assertEquals

class ExampleTest {
  
	@Test def testExample() {
		assertEquals("Hello EasyAnt!", Example.sayHello("EasyAnt"));
	}
 
	@Test def testTestResources() {
		val props = new Properties();
		props.load(this.getClass.getResourceAsStream("/test.properties"));
		assertEquals("Hello Test", props.getProperty("test.example"));
	}
}