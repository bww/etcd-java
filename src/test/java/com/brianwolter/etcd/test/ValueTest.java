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

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URLEncodedUtils;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.brianwolter.etc.Config;
import com.brianwolter.etc.util.Property;
import com.brianwolter.etc.provider.EtcdProvider;
import com.brianwolter.etc.provider.SystemProvider;

/**
 * Tests
 */
public class ValueTest {
  
  private static final Config           config    = new Config(new SystemProvider(), new EtcdProvider());
  private static final ExecutorService  executor  = Executors.newSingleThreadExecutor();
  
  
  @Test
  public void testAsyncHTTP() throws Exception {
    List<String> keys = new ArrayList<String>();
    int base = 11, count = 5;
    
    for(int i = 0; i < count; i++){
      keys.add(String.format("test.watch.%d", base + i));
    }
    
    RequestConfig requestConfig = RequestConfig.custom()
      .setConnectTimeout(1000)
      .setSocketTimeout(60 * 6 * 1000)
      .setConnectionRequestTimeout(60 * 6 * 1000)
      .build();
    
    final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
      .setDefaultRequestConfig(requestConfig)
      .build();
    
    httpclient.start();
    
    try {
      
      final CountDownLatch latch = new CountDownLatch(keys.size());
      
      for(String key : keys){
        // URI for our key
        URI uri = new URI("http", null, "localhost", 4001, String.format("/v2/keys/%s", EtcdProvider.keyToPath(key)), "wait=true", null);
        // setup our request
        final HttpGet get = new HttpGet(uri);
        // note it for debugging
        System.err.println(get);
        // perform our request
        httpclient.execute(get, new org.apache.http.concurrent.FutureCallback<HttpResponse>() {
          public void completed(HttpResponse response) {
            System.err.println(response.getStatusLine());
            latch.countDown();
          }
          public void failed(Exception e) {
            e.printStackTrace();
            latch.countDown();
          }
          public void cancelled() {
            System.err.println("Cancelled...");
            latch.countDown();
          }
        });
      }
      
      latch.await();
      
    }finally{
      httpclient.close();
    }
    
  }
  
  @Test
  public void testEtcd() throws Exception {
    EtcdProvider provider = new EtcdProvider();
    List<String> keys = new ArrayList<String>();
    int base = 11, count = 5;
    
    for(int i = 0; i < count; i++){
      keys.add(String.format("test.watch.%d", base + i));
    }
    
    final CountDownLatch latch = new CountDownLatch(keys.size());
    
    for(String key : keys){
      Futures.addCallback(provider.watch(key, null), new com.google.common.util.concurrent.FutureCallback<Property>() {
        public void onSuccess(Property property) {
          System.err.println("--> "+ property);
          latch.countDown();
        }
        public void onFailure(Throwable thrown) {
          thrown.printStackTrace();
          latch.countDown();
        }
      }, executor);
    }
    
    latch.await();
    
  }
  
  @Test
  public void testGetSet() throws Exception {
    Config.Value value = config.get("test.1");
    System.err.println("--> "+ value.get());
    System.err.println("--> "+ value.set("This is the new value now."));
  }
  
  @Test
  public void testMonitor() throws Exception {
    List<Config.Value<String>> values = new ArrayList<Config.Value<String>>();
    int base = 11, count = 5;
    
    for(int i = 0; i < count; i++){
      values.add(config.get(String.format("test.watch.%d", base + i), String.class));
    }
    
    final CountDownLatch latch = new CountDownLatch(values.size());
    
    for(Config.Value<String> value : values){
      Futures.addCallback(value.watch(), new com.google.common.util.concurrent.FutureCallback<String>() {
        public void onSuccess(String value) {
          System.err.println("--> "+ value);
          latch.countDown();
        }
        public void onFailure(Throwable thrown) {
          thrown.printStackTrace();
          latch.countDown();
        }
      }, executor);
    }
    
    latch.await();
    
  }
  
  @Test
  public void testNotFound() throws Exception {
    Config.Value value = config.get("test.invalid");
    System.err.println("--> "+ value.get());
  }
  
}

