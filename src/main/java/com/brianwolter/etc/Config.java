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
import com.brianwolter.etc.util.Property;

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
  public Value get(String key) {
    return get(key, new NativeMarshaler());
  }
  
  /**
   * Obtain a configuration value for the specified path.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public <V> Value<V> get(String key, Class<V> clazz) {
    return get(key, new PrimitiveMarshaler<V>(clazz));
  }
  
  /**
   * Obtain a configuration value for the specified path.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public <V> Value<V> get(String key, Class<V> clazz, V ifnull) {
    return get(key, new PrimitiveMarshaler<V>(clazz), ifnull);
  }
  
  /**
   * Obtain a configuration value for the specified path.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public <V> Value<V> get(String key, Marshaler<V> marshaler) {
    return get(key, marshaler, null);
  }
  
  /**
   * Obtain a configuration value for the specified path.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public <V> Value<V> get(String key, Marshaler<V> marshaler, V ifnull) {
    return this.new Value<V>(key, marshaler, ifnull);
  }
  
  /**
   * Obtain the value for the specified key from the first provider which defines one.
   */
  protected Property __get(String key) throws IOException {
    Property property = null;
    for(Provider provider : _providers){
      if(provider instanceof Provider.Observable){
        if((property = ((Provider.Observable)provider).get(key)) != null){
          break;
        }
      }
    }
    return property;
  }
  
  /**
   * Set a value for the specified key in the first mutable provider.
   */
  protected Property __set(String key, Object value) throws IOException {
    for(Provider provider : _providers){
      if(provider instanceof Provider.Mutable){
        return ((Provider.Mutable)provider).set(key, value);
      }
    }
    return null;
  }
  
  /**
   * Watch the value for the specified key on the first monitorable provider.
   */
  protected ListenableFuture<Property> __watch(String key, Property previous) throws IOException {
    for(Provider provider : _providers){
      if(provider instanceof Provider.Monitorable){
        return ((Provider.Monitorable)provider).watch(key, previous);
      }
    }
    return null;
  }
  
  /**
   * String description
   */
  public String toString() {
    return String.format("<Config %s>", _providers);
  }
  
  /**
   * A configuration value
   */
  public class Value <V> {
    
    private String                      _key;
    private Marshaler<V>                _marshaler;
    private V                           _ifnull;
    private V                           _value;
    private boolean                     _autoupdate;
    private Property                    _previous;
    private ListenableFuture<Property>  _monitor;
    private SettableFuture<V>           _watcher;
    
    /**
     * Construct a configuration value with the specified key
     */
    protected Value(String key, Marshaler<V> marshaler, V ifnull) {
      if((_key = key) == null || _key.isEmpty()) throw new IllegalArgumentException("Key must not be null or empty");
      if((_marshaler = marshaler) == null) throw new IllegalArgumentException("Marshaler must not be null");
      _ifnull = ifnull;
    }
    
    /**
     * Obtain the current value
     */
    public synchronized V get() throws ConfigException {
      return get(null);
    }
    
    /**
     * Obtain the current value
     */
    public synchronized V get(V ifnull) throws ConfigException {
      try {
        if(_value == null){
          if((_previous = Config.this.__get(_key)) != null){
            _value = _marshaler.unmarshal(_previous.value());
          }else{
            _value = (_ifnull != null) ? _ifnull : ifnull;
          }
        }
        return _value;
      }catch(IOException e){
        throw new ConfigException("Could not get configuration value: "+ this, e);
      }
    }
    
    /**
     * Set the current value
     */
    public synchronized V set(V value) throws ConfigException {
      try {
        Property property;
        if((property = Config.this.__set(_key, _marshaler.marshal(value))) != null){
          _value = _marshaler.unmarshal(property.value());
          _previous = property;
        }else{
          _value = value;
        }
        return _value;
      }catch(IOException e){
        throw new ConfigException("Could not set configuration value: "+ this, e);
      }
    }
    
    /**
     * Begin auto-updating this value
     */
    public synchronized Value<V> auto() throws ConfigException {
      // mark as auto-updating
      _autoupdate = true;
      // begin monitoring
      monitor();
      // return this value, for chaining
      return this;
    }
    
    /**
     * Begin monitoring this value.
     */
    private synchronized void monitor() throws ConfigException {
      if(_monitor == null){
        try {
          // create our monitor future by watching our key
          _monitor = Config.this.__watch(_key, _previous);
          // process callbacks
          Futures.addCallback(_monitor, new FutureCallback<Property>() {
            public void onSuccess(Property mutation) {
              Value.this.update(mutation);
            }
            public void onFailure(Throwable thrown) {
              Value.this.failed(thrown);
            }
          }, Config.this.executor);
        }catch(IOException e){
          throw new ConfigException("Could not monitor configuration value: "+ this, e);
        }
      }
    }
    
    /**
     * Update the value.
     */
    private synchronized void update(Property mutation) throws ConfigException {
      
      try {
        _value = _marshaler.unmarshal(mutation.value());
      }catch(IOException e){
        throw new ConfigException("Could not unmarshal value", e);
      }
      
      // update the context mutation
      _previous = mutation;
      // clear this monitor, it just completed
      _monitor = null;
      // if we're auto-updating begin monitoring again
      if(_autoupdate) monitor();
      
      // process the watcher future if we have one
      SettableFuture watcher;
      if((watcher = _watcher) != null){
        // clear it first
        _watcher = null;
        // propagate the value
        watcher.set(_value);
      }
      
    }
    
    /**
     * Update failed
     */
    private synchronized void failed(Throwable thrown) throws ConfigException {
      
      // clear our value? it's invalid
      _value = null;
      // clear the monitor, it just completed
      _monitor = null;
      // if we're auto-updating begin monitoring again
      if(_autoupdate) monitor();
      
      // process the watcher future if we have one
      SettableFuture watcher;
      if((watcher = _watcher) != null){
        // clear it first
        _watcher = null;
        // propagate the exception
        watcher.setException(thrown);
      }
      
    }
    
    /**
     * Monitor the current value
     */
    public synchronized ListenableFuture<V> watch() throws ConfigException {
      if(_watcher == null){
        // create our watcher future, which is shared
        _watcher = SettableFuture.create();
        // begin monitoring
        monitor();
      }
      return _watcher;
    }
    
    /**
     * String description
     */
    public String toString() {
      return String.format("'%s' in %s", _key, Config.this);
    }
    
  }
  
}

