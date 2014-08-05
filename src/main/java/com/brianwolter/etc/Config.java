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
//   * Neither the name of Brian William Wolter nor the names of the contributors
//     may be used to endorse or promote products derived from this software without
//     specific prior written permission.
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

package com.brianwolter.etc;

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.FutureCallback;

import com.brianwolter.etc.marshal.NativeMarshaler;
import com.brianwolter.etc.marshal.PrimitiveMarshaler;

/**
 * A configuration.
 */
public class Config {
  
  private static final Logger           logger    = Logger.getLogger(Config.class.getName());
  private static final ExecutorService  executor  = Executors.newSingleThreadExecutor();
  
  protected final List<Provider> _providers;
  
  /**
   * Construct with providers
   */
  public Config(Provider... providers) {
    this(Arrays.asList(providers));
  }
  
  /**
   * Construct with providers
   */
  public Config(Collection<Provider> providers) {
    if(providers == null || providers.isEmpty()) throw new IllegalArgumentException("Providers must not be null or empty");
    _providers = Collections.unmodifiableList(new ArrayList<Provider>(providers));
  }
  
  /**
   * Obtain a configuration value for the specified path.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public Value get(String key) throws IOException {
    return get(key, new NativeMarshaler());
  }
  
  /**
   * Obtain a configuration value for the specified path.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public <V> Value<V> get(String key, Class<V> clazz) throws IOException {
    return get(key, new PrimitiveMarshaler<V>(clazz));
  }
  
  /**
   * Obtain a configuration value for the specified path.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public <V> Value<V> get(String key, Marshaler<V> marshaler) throws IOException {
    return this.new Value<V>(key, marshaler);
  }
  
  /**
   * Obtain the value for the specified key from the first provider which defines one.
   */
  protected Object __get(String key) throws IOException {
    Object value = null;
    for(Provider provider : _providers){
      if(provider instanceof Provider.Observable){
        if((value = ((Provider.Observable)provider).get(key)) != null){
          break;
        }
      }
    }
    return value;
  }
  
  /**
   * Set a value for the specified key in all mutable providers.
   */
  protected void __set(String key, Object value) throws IOException {
    for(Provider provider : _providers){
      if(provider instanceof Provider.Mutable){
        ((Provider.Mutable)provider).set(key, value);
      }
    }
  }
  
  /**
   * Watch the value for the specified key on the first monitorable provider.
   */
  protected ListenableFuture __watch(String key) throws IOException {
    for(Provider provider : _providers){
      if(provider instanceof Provider.Monitorable){
        return ((Provider.Monitorable)provider).watch(key);
      }
    }
    return null;
  }
  
  /**
   * A configuration value
   */
  public class Value <V> {
    
    private String              _key;
    private Marshaler<V>        _marshaler;
    private V                   _value;
    private ListenableFuture<V> _future;
    
    /**
     * Construct a configuration value with the specified key
     */
    protected Value(String key, Marshaler<V> marshaler) throws IOException {
      if((_key = key) == null || _key.isEmpty()) throw new IllegalArgumentException("Key must not be null or empty");
      if((_marshaler = marshaler) == null) throw new IllegalArgumentException("Marshaler must not be null");
    }
    
    /**
     * Obtain the current value
     */
    public synchronized V get() throws IOException {
      if(_value == null) _value = _marshaler.unmarshal(Config.this.__get(_key));
      return _value;
    }
    
    /**
     * Set the current value
     */
    public synchronized V set(V value) throws IOException {
      Config.this.__set(_key, Value.this._marshaler.marshal(value));
      return (_value = value);
    }
    
    /**
     * Monitor the current value
     */
    public synchronized ListenableFuture<V> watch() throws IOException {
      if(_future == null){
        
        // the inner future produced by the provider
        ListenableFuture inner = Config.this.__watch(_key);
        // the outer future managed by this method
        final SettableFuture outer = SettableFuture.create();
        // the outer future is the future returned to callers
        _future = outer;
        
        // watch the inner future for completion; update and produce our result on the outer future
        Futures.addCallback(inner, new FutureCallback() {
          
          public void onSuccess(Object value) {
            try {
              
              // update the state of this value first
              synchronized(Value.this){
                Value.this._value = Value.this._marshaler.unmarshal(value);
                Value.this._future = null;
              }
              
              // the propagate the value to the caller
              outer.set(value);
              
            }catch(Exception e){
              outer.setException(e);
            }
          }
          
          public void onFailure(Throwable thrown) {
            
            // update the state of this value first
            synchronized(Value.this){
              Value.this._value = null;
              Value.this._future = null;
            }
            
            // the propagate the exception to the caller
            outer.setException(thrown);
            
          }
          
        }, Config.this.executor);
      }
      return _future;
    }
    
  }
  
}

