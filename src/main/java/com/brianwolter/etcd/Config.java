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

package com.brianwolter.etcd;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URLEncodedUtils;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * A configuration backed by an etcd service.
 */
public class Config {
  
  private static final Logger logger = Logger.getLogger(Config.class.getName());
  
  private static final String ENCODING            = "UTF-8";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  private static final String CONTENT_TYPE_JSON   = "application/json";
  private static final String CONTENT_TYPE_FORM   = "application/x-www-form-urlencoded";
  private static final Gson   GSON                = new Gson();
  
  private String  _host;
  private int     _port;
  
  /**
   * Construct a configuration with the specified host for the etcd server
   * it will interact with.
   */
  public Config(String host) {
    this(host, 0);
  }
  
  /**
   * Construct a configuration with the specified host and port for the etcd server
   * it will interact with.
   */
  public Config(String host, int port) {
    if((_host = host) == null || _host.isEmpty()) throw new IllegalArgumentException("Etcd server host is invalid");
    if((_port = port) <= 0) _port = 4001;
  }
  
  /**
   * Obtain the etcd server host that backs this configuration
   */
  public String getServiceHost() {
    return _host;
  }
  
  /**
   * Obtain the etcd server port that backs this configuration
   */
  public int getServicePort() {
    return _port;
  }
  
  /**
   * Obtain a configuration value for the specified path. The value is not actually looked up
   * before the value is returned. The value itself manages interacting with the etcd service
   * by lazily fetching values as needed.
   * 
   * @param path the configuration value path. This is the path to the value excluding the implied '/v2/keys' prefix.
   * @return a configuration value representing the specified path
   */
  public Value get(String path) throws IOException {
    return this.new Value(path);
  }
  
  /**
   * A configuration value
   */
  public class Value <V> {
    
    private String  _path;
    private URI     _valueURL;
    private URI     _watchURL;
    private V       _value;
    
    /**
     * Construct a configuration value with the specified path
     */
    protected Value(String path) throws IOException {
      
      path = trimLeadingSlash(path);
      if((_path = path) == null || _path.isEmpty()) throw new IllegalArgumentException("Configuration value path is invalid");
      
      try {
        _valueURL = new URI("http", null, Config.this.getServiceHost(), Config.this.getServicePort(), String.format("/v2/keys/%s", path), null, null);
        _watchURL = new URI("http", null, Config.this.getServiceHost(), Config.this.getServicePort(), String.format("/v2/keys/%s", path), "wait=true", null);
      }catch(URISyntaxException e){
        throw new IOException("Could not build endpoint URLs", e);
      }
      
    }
    
    /**
     * Obtain the current value
     */
    public V get() throws IOException {
      return __get();//(_value == null) ? __get() : _value;
    }
    
    /**
     * Update the current value
     */
    protected V __get() throws IOException {
      CloseableHttpClient client = null;
      HttpGet get;
      
      // setup our get request
      get = new HttpGet(_valueURL);
      logger.debug(get);
      
      // send our request and process the response
      try {
        
        // request timeout
        int timeout = 3 * 1000;
        // setup our client configuration
        RequestConfig requestConfig = RequestConfig.custom()
          .setSocketTimeout(timeout)
          .setConnectTimeout(timeout)
          .setConnectionRequestTimeout(timeout)
          .build();
        
        // create our client
        client = HttpClientBuilder.create()
          .setDefaultRequestConfig(requestConfig)
          .build();
        
        // send our request
        HttpResponse response = client.execute(get);
        
        // check out status code
        int status;
        if((status = response.getStatusLine().getStatusCode()) != 200){
          invalidStatus(status);
        }
        
        // obtain our response entity
        HttpEntity entity;
        if((entity = response.getEntity()) == null){
          throw new IOException("Etcd response contains no data");
        }
        
        valueForEntity(entity);
        
      }catch(IOException e){
        throw e;
      }catch(Exception e){
        throw new IOException("Etcd request failed: "+ get, e);
      }finally{
        // release our connection
        get.releaseConnection();
        // close our client
        client.close();
      }
      
      return null;
    }
    
    /**
     * Set the current value
     */
    public void set(V value) throws IOException {
      _value = value;
      __set(value);
    }
    
    /**
     * Update the current value
     */
    protected void __set(V value) throws IOException {
      CloseableHttpClient client = null;
      HttpPut put;
      
      // our value
      List<NameValuePair> params = new ArrayList<NameValuePair>();
      params.add(new BasicNameValuePair("value", String.valueOf(value)));
      String update = URLEncodedUtils.format(params, ENCODING);
      
      // setup our put request
      put = new HttpPut(_valueURL);
      put.setHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM);
      put.setEntity(new StringEntity(GSON.toJson(update)));
      logger.debug(String.valueOf(put));
      
      // send our request and process the response
      try {
        
        // request timeout
        int timeout = 3 * 1000;
        // setup our client configuration
        RequestConfig requestConfig = RequestConfig.custom()
          .setSocketTimeout(timeout)
          .setConnectTimeout(timeout)
          .setConnectionRequestTimeout(timeout)
          .build();
        
        // create our client
        client = HttpClientBuilder.create()
          .setDefaultRequestConfig(requestConfig)
          .build();
        
        // send our request
        HttpResponse response = client.execute(put);
        
        // check out status code
        int status;
        if((status = response.getStatusLine().getStatusCode()) != 200 && status != 201){
          invalidStatus(status);
        }
        
        // obtain our response entity
        HttpEntity entity;
        if((entity = response.getEntity()) == null){
          throw new IOException("Etcd response contains no data");
        }
        
        valueForEntity(entity);
        
      }catch(IOException e){
        throw e;
      }catch(Exception e){
        throw new IOException("Etcd request failed: "+ put, e);
      }finally{
        // release our connection
        put.releaseConnection();
        // close our client
        client.close();
      }
      
    }
    
    /**
     * Obtain a value from the specified entity
     */
    private V valueForEntity(HttpEntity entity) throws IOException {
      Type type = new TypeToken<Map<String, Object>>(){}.getType();
      Map<String, Object> content = GSON.fromJson(new InputStreamReader(entity.getContent(), ENCODING), type);
      System.err.println("OK: "+ content);
      return null;
    }
    
    /**
     * Report an invalid status
     */
    private void invalidStatus(int status) throws IOException {
      switch(status){
        case 400:
          throw new IOException(String.format("[%s] Invalid request", _path));
        case 401:
          throw new IOException(String.format("[%s] Unauthorized", _path));
        case 403:
          throw new IOException(String.format("[%s] Forbidden", _path));
        case 404:
          throw new IOException(String.format("[%s] No such key", _path));
        case 422:
          throw new IOException(String.format("[%s] Invalid request", _path));
        case 500:
          throw new IOException(String.format("[%s] Etcd service error", _path));
        default:
          throw new IOException(String.format("[%s] Etcd responded with an unexpected response code: %d", _path, status));
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
  
}

