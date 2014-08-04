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

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A configuration.
 */
public class Config {
  
  private static final Logger logger = Logger.getLogger(Config.class.getName());
  
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
   * Obtain a configuration value for the specified path. The value is not actually looked up
   * before the value is returned. The value itself manages interacting with the etcd service
   * by lazily fetching values as needed.
   * 
   * @param key the configuration value key
   * @return a configuration value representing the specified key
   */
  public Value get(String key) throws IOException {
    return this.new Value(key);
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
    
    private String _key;
    private V      _value;
    
    /**
     * Construct a configuration value with the specified key
     */
    protected Value(String key) throws IOException {
      _key = key;
    }
    
    /**
     * Obtain the current value
     */
    public V get() throws IOException {
      return (V)Config.this.__get(_key);
    }
    
    /**
     * Set the current value
     */
    public V set(V value) throws IOException {
      Config.this.__set(_key, value);
      return (_value = (V)value);
    }
    
    /**
     * Monitor the current value
     */
    public ListenableFuture<V> watch() throws IOException {
      return Config.this.__watch(_key);
    }
    
  }
  
}

