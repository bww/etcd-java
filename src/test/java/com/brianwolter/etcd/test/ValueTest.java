// 
// Copyright (c) 2014 Brian William Wolter, All rights reserved.
// Etcd - an etcd SDK for Java
// 
// Redistribution and use in source and binary forms, with or without modification,
// are permitted provided that the following conditions are met:
// 
//   * Redistributions of source code must retain the above copyright notice, this
//     list of conditions and the following disclaimer.
// 
//   * Redistributions in binary form must reproduce the above copyright notice,
//     this list of conditions and the following disclaimer in the documentation
//     and/or other materials provided with the distribution.
//     
//   * Neither the names of Brian William Wolter, Wolter Group New York, nor the
//     names of its contributors may be used to endorse or promote products derived
//     from this software without specific prior written permission.
//     
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
// IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
// OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.
// 

package com.brianwolter.etc.test;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.FutureCallback;

import com.brianwolter.etc.Config;
import com.brianwolter.etc.provider.EtcdProvider;
import com.brianwolter.etc.provider.SystemProvider;

/**
 * Tests
 */
public class ValueTest {
  
  @Test
  public void testEtcdProvider() throws Exception {
    //ExecutorService executor = Executors.newFixedThreadPool(10);
    
    EtcdProvider provider = new EtcdProvider();
    System.err.println(provider.get("test/1"));
    System.err.println(provider.set("test/1", "Hello again..."));
    
    ListenableFuture future = provider.watch("test/1");
    System.err.println(future.get());
    
    /*
    Futures.addCallback(future, new FutureCallback() {
      public void onSuccess(Object value) {
        System.err.println("OK NOW: "+ value);
      }
      public void onFailure(Throwable thrown) {
        System.err.println("YARP: "+ thrown);
      }
    }, executor);
    */
    
  }
  
  @Test
  public void testValues() throws Exception {
    Config config = new Config(new SystemProvider(), new EtcdProvider());
    Config.Value value = config.get("test/1");
    System.err.println(config.get("test.property").get());
    System.err.println(value.set(123));
    System.err.println(value.get());
    System.err.println(value.watch().get());
  }
  
}

