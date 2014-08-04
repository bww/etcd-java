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

package com.brianwolter.etc.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

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

import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ListenableFuture;

import com.brianwolter.etc.Provider;

/**
 * Etcd provider.
 */
public class EtcdProvider implements Provider {
  
  private static final Logger logger = Logger.getLogger(EtcdProvider.class.getName());
  
  private static final String ENCODING                = "UTF-8";
  private static final String HEADER_CONTENT_TYPE     = "Content-Type";
  private static final String CONTENT_TYPE_JSON       = "application/json";
  private static final String CONTENT_TYPE_FORM       = "application/x-www-form-urlencoded";
  private static final Gson   GSON                    = new Gson();
  
  private final CloseableHttpAsyncClient httpclient;
  
  /**
   * Construcuct
   */
  public EtcdProvider() {
    int requestTimeout = 3 * 1000;
    
    RequestConfig requestConfig = RequestConfig.custom()
      .setSocketTimeout(requestTimeout)
      .setConnectTimeout(requestTimeout)
      .setConnectionRequestTimeout(requestTimeout)
      .build();
    
    httpclient = HttpAsyncClients.custom()
      .setDefaultRequestConfig(requestConfig)
      .build();
    
    httpclient.start();
    
  }
  
  /**
   * Close the HTTP client
   */
  protected void finalize() throws Throwable {
    httpclient.close();
  }
  
  /**
   * Obtain a configuration value.
   */
  public Object get(final String key) throws IOException {
    HttpGet get;
    
    try {
      get = new HttpGet(new URI("http", null, "localhost", 4001, String.format("/v2/keys/%s", trimLeadingSlash(key)), null, null));
    }catch(URISyntaxException e){
      throw new IOException(e);
    }
    
    try {
      
      // note it for debugging
      logger.debug(get);
      // send our request synchronously
      HttpResponse response = executeRequest(get).get();
      
      // check out status code
      int status;
      if((status = response.getStatusLine().getStatusCode()) != 200){
        invalidStatus(key.toString(), status);
      }
      
      // obtain our response entity
      HttpEntity entity;
      if((entity = response.getEntity()) == null){
        throw new IOException("Etcd response contains no data");
      }
      
      // return the canonical value
      return valueForEntity(entity);
      
    }catch(IOException e){
      throw e;
    }catch(Exception e){
      throw new IOException("Etcd request failed: "+ get, e);
    }finally{
      get.releaseConnection();
    }
    
  }
  
  /**
   * Set a configuration value. Not all providers must implement this method.
   */
  public Object set(final String key, final Object value) throws IOException {
    HttpPut put;
    
    // our value
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("value", String.valueOf(value)));
    String update = URLEncodedUtils.format(params, ENCODING);
    
    try {
      put = new HttpPut(new URI("http", null, "localhost", 4001, String.format("/v2/keys/%s", trimLeadingSlash(key)), null, null));
    }catch(URISyntaxException e){
      throw new IOException(e);
    }
    
    // setup our put request
    put.setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM);
    put.setEntity(new StringEntity(update));
    
    try {
      
      // note it for debugging
      logger.debug(put);
      // send our request
      HttpResponse response = executeRequest(put).get();
      
      // check out status code
      int status;
      if((status = response.getStatusLine().getStatusCode()) != 200 && status != 201){
        invalidStatus(key, status);
      }
      
      // obtain our response entity
      HttpEntity entity;
      if((entity = response.getEntity()) == null){
        throw new IOException("Etcd response contains no data");
      }
      
      // return the canonical value
      return valueForEntity(entity);
      
    }catch(IOException e){
      throw e;
    }catch(Exception e){
      throw new IOException("Etcd request failed: "+ put, e);
    }finally{
      put.releaseConnection();
    }
    
  }
  
  /**
   * Watch a value for changes.
   */
  public ListenableFuture watch(final String key) throws IOException {
    try {
      final SettableFuture future = SettableFuture.create();
      watch(key, new URI("http", null, "localhost", 4001, String.format("/v2/keys/%s", trimLeadingSlash(key)), "wait=true", null), null, future);
      return future;
    }catch(URISyntaxException e){
      throw new IOException(e);
    }
  }
  
  /**
   * Watch a value for changes.
   */
  protected void watch(final String key, final URI uri, final Date until, final SettableFuture future) {
    
    // setup our request
    HttpGet get = new HttpGet(uri);
    // note it for debugging
    logger.debug(get);
    
    // send our request asynchronously
    httpclient.execute(get, new FutureCallback<HttpResponse>() {
      
      public void completed(HttpResponse response) {
        try {
          
          // check out status code
          int status;
          if((status = response.getStatusLine().getStatusCode()) != 200){
            invalidStatus(key.toString(), status);
          }
          
          // obtain our response entity
          HttpEntity entity;
          if((entity = response.getEntity()) == null){
            throw new IOException("Etcd response contains no data");
          }
          
          future.set(valueForEntity(entity));
        }catch(Exception e){
          future.setException(e);
        }
      }
      
      public void failed(Exception e) {
        if(e instanceof java.net.SocketTimeoutException){
          logger.error("TIMEOUT", e);
          EtcdProvider.this.watch(key, uri, until, future);
        }else{
          future.setException(e);
        }
      }
      
      public void cancelled() {
        future.setException(new InterruptedException());
      }
      
    });
    
  }
  
  /**
   * Execute a request
   */
  private ListenableFuture<HttpResponse> executeRequest(HttpUriRequest request) throws IOException {
    final SettableFuture<HttpResponse> future = SettableFuture.create();
    httpclient.execute(request, new FutureCallback<HttpResponse>() {
      public void completed(HttpResponse result) {
        future.set(result);
      }
      public void failed(Exception e) {
        future.setException(e);
      }
      public void cancelled() {
        future.setException(new InterruptedException());
      }
    });
    return future;
  }
  
  /**
   * Obtain a value from the specified entity
   */
  private Object valueForEntity(HttpEntity entity) throws IOException {
    Type type = new TypeToken<Map<String, Object>>(){}.getType();
    Map<String, Object> content = GSON.fromJson(new InputStreamReader(entity.getContent(), ENCODING), type);
    List<Map<String, Object>> subnodes;
    Map<String, Object> node;
    
    if((subnodes = (List<Map<String, Object>>)content.get("nodes")) != null){
      throw new IOException("Directory nodes are not supported");
    }else if((node = (Map<String, Object>)content.get("node")) != null){
      return node.get("value");
    }else{
      throw new IOException("Invalid node");
    }
    
  }
  
  /**
   * Report an invalid status
   */
  private void invalidStatus(String key, int status) throws IOException {
    switch(status){
      case 400:
        throw new IOException(String.format("[%s] Invalid request", key));
      case 401:
        throw new IOException(String.format("[%s] Unauthorized", key));
      case 403:
        throw new IOException(String.format("[%s] Forbidden", key));
      case 404:
        throw new IOException(String.format("[%s] No such key", key));
      case 422:
        throw new IOException(String.format("[%s] Invalid request", key));
      case 500:
        throw new IOException(String.format("[%s] Etcd service error", key));
      default:
        throw new IOException(String.format("[%s] Etcd responded with an unexpected response code: %d", key, status));
    }
  }
  
  /**
   * Strip off leading '/' from a path
   */
  private String trimLeadingSlash(String path) {
    for(int i = 0; i < path.length(); i++){
      if(path.charAt(i) != '/'){
        return path.substring(i);
      }
    }
    return new String();
  }
  
}

