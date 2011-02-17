package org.apache.easyant.example;

import java.io.IOException
import java.util.Properties

object Example extends Application {

  @throws(classOf[IOException]) 
  def sayHello(who : String) = {
    
    val props = new Properties()
    props.load(this.getClass().getResourceAsStream("/main.properties"))
    props.getProperty("example") + " " + who + "!";
  }
  
  println(sayHello("EasyAnt"))

}