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

import com.google.common.util.concurrent.ListenableFuture;

import com.brianwolter.etc.util.Property;

/**
 * Implemented by configuration providers.
 */
public interface Provider {
  
  /**
   * Implemented by observable provdiers
   */
  public static interface Observable extends Provider {
    
    /**
     * Obtain a configuration value.
     */
    public Property get(final String key) throws IOException;
    
  }
  
  /**
   * Implemented by mutable provdiers
   */
  public static interface Mutable extends Provider {
    
    /**
     * Set a configuration value. Not all providers implement this method.
     */
    public Property set(final String key, final Object value) throws IOException;
    
  }
  
  /**
   * Implemented by mutable provdiers
   */
  public static interface Monitorable extends Mutable {
    
    /**
     * Watch a value for changes. Not all providers implement this method.
     */
    public ListenableFuture<Property> watch(final String key, final Property previous) throws IOException;
    
  }
  
}

