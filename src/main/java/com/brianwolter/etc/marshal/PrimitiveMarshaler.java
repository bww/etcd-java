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

package com.brianwolter.etc.marshal;

import java.io.IOException;

import com.brianwolter.etc.Marshaler;
import com.brianwolter.etc.util.Typecast;

/**
 * A primitive type marshaler
 */
public class PrimitiveMarshaler <V> implements Marshaler <V> {
  
  private Class<V> _clazz;
  
  /**
   * Construct with a target type
   */
  public PrimitiveMarshaler(Class<V> clazz) {
    if((_clazz = clazz) == null) throw new IllegalArgumentException("Class must not be null");
  }
  
  /**
   * Unmarshal an object from it's external representation to its internal representation
   */
  public V unmarshal(Object o) throws IOException {
    return Typecast.convert(o, _clazz);
  }
  
  /**
   * Marshal an object from it's internal representation to its external representation
   */
  public Object marshal(V o) throws IOException {
    return String.valueOf(o);
  }
  
}

