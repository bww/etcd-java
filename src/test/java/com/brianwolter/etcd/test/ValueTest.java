// 
// Copyright (c) 2013 Brian William Wolter. All rights reserved.
// 
// @LICENSE@
// 
// Developed in New York City
// 

package com.brianwolter.etcd.test;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import com.brianwolter.etcd.Config;

/**
 * Tests
 */
public class ValueTest {
  
  @Test
  public void testValue() throws Exception {
    Config config = new Config("localhost");
    
    Config.Value value1 = config.get("test/1/1000");
    value1.set("Hello?");
    System.err.println(value1.get());
    
    Config.Value value2 = config.get("test/1");
    System.err.println(value2.get());
    
  }
  
}

