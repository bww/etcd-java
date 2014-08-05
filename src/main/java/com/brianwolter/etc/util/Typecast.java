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

package com.brianwolter.etc.util;

/**
 * Typecast obejcts
 */
public class Typecast {
  
  /**
   * Convert an object to the specified type, if possible.
   */
  public static <V> V convert(Object o, Class<V> type) {
    if(o == null){
      return null;
    }else if(type.isAssignableFrom(o.getClass())){
      return (V)o;
    }else if(o instanceof Number){
      return (V)__convert((Number)o, type);
    }else if(o instanceof String){
      return (V)__convert((String)o, type);
    }else if(o instanceof Boolean){
      return (V)__convert((Boolean)o, type);
    }else{
      throw new IllegalArgumentException(String.format("Cannot convert %s to %s", o.getClass().getName(), type));
    }
  }
  
  /**
   * Convert a numeric object to the specified type, if possible.
   */
  private static Object __convert(Number o, Class type) {
    if(type.equals(Byte.class) || type.equals(byte.class)){
      return o.byteValue();
    }else if(type.equals(Short.class) || type.equals(short.class)){
      return o.shortValue();
    }else if(type.equals(Integer.class) || type.equals(int.class)){
      return o.intValue();
    }else if(type.equals(Long.class) || type.equals(long.class)){
      return o.longValue();
    }else if(type.equals(Float.class) || type.equals(float.class)){
      return o.floatValue();
    }else if(type.equals(Double.class) || type.equals(double.class)){
      return o.doubleValue();
    }else if(type.equals(String.class)){
      return String.valueOf(o);
    }else if(type.equals(Boolean.class) || type.equals(boolean.class)){
      return o.byteValue() != 0;
    }else{
      throw new IllegalArgumentException(String.format("Cannot convert %s to %s", o.getClass().getName(), type));
    }
  }
  
  /**
   * Convert a string object to the specified type, if possible.
   */
  private static Object __convert(String o, Class type) {
    if(type.equals(String.class)){
      return o;
    }else if(type.equals(Byte.class) || type.equals(byte.class)){
      return Byte.valueOf(o);
    }else if(type.equals(Short.class) || type.equals(short.class)){
      return Short.valueOf(o);
    }else if(type.equals(Integer.class) || type.equals(int.class)){
      return Integer.valueOf(o);
    }else if(type.equals(Long.class) || type.equals(long.class)){
      return Long.valueOf(o);
    }else if(type.equals(Float.class) || type.equals(float.class)){
      return Float.valueOf(o);
    }else if(type.equals(Double.class) || type.equals(double.class)){
      return Double.valueOf(o);
    }else if(type.equals(Boolean.class) || type.equals(boolean.class)){
      return o.equalsIgnoreCase("true") || o.equalsIgnoreCase("t");
    }else{
      throw new IllegalArgumentException(String.format("Cannot convert %s to %s", o.getClass().getName(), type));
    }
  }
  
  /**
   * Convert a boolean object to the specified type, if possible.
   */
  private static Object __convert(Boolean o, Class type) {
    if(type.equals(Boolean.class)){
      return o;
    }else if(type.equals(String.class)){
      return o.booleanValue() ? "true" : "false";
    }else if(type.equals(Byte.class) || type.equals(byte.class)){
      return o.booleanValue() ? (byte)1 : (byte)0;
    }else if(type.equals(Short.class) || type.equals(short.class)){
      return o.booleanValue() ? (short)1 : (short)0;
    }else if(type.equals(Integer.class) || type.equals(int.class)){
      return o.booleanValue() ? 1 : 0;
    }else if(type.equals(Long.class) || type.equals(long.class)){
      return o.booleanValue() ? 1L : 0L;
    }else if(type.equals(Float.class) || type.equals(float.class)){
      return o.booleanValue() ? 1f : 0f;
    }else if(type.equals(Double.class) || type.equals(double.class)){
      return o.booleanValue() ? 1d : 0d;
    }else{
      throw new IllegalArgumentException(String.format("Cannot convert %s to %s", o.getClass().getName(), type));
    }
  }
  
}

