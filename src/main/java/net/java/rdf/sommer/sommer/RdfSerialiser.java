/*
 * Serialiser.java
 * 
 * Created on Sep 29, 2007, 7:28:28 PM
 * 
 New BSD license: http://opensource.org/licenses/bsd-license.php

 Copyright (c) 2003, 2004, 2005 Sun Microsystems, Inc.
 901 San Antonio Road, Palo Alto, CA 94303 USA. 
 All rights reserved.


 Redistribution and use in source and binary forms, with or without 
 modification, are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice, 
  this list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice, 
  this list of conditions and the following disclaimer in the documentation 
  and/or other materials provided with the distribution.
 - Neither the name of Sun Microsystems, Inc. nor the names of its contributors
  may be used to endorse or promote products derived from this software 
  without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 POSSIBILITY OF SUCH DAMAGE.
 */

package net.java.rdf.sommer;

import java.lang.reflect.Field;

/**
 * Implementations of this guide behavior for serialising java objects.
 * 
 * So(m)mer will add the following method to its mapped classes:
 * <code><pre>
 *    public void rdfSerialise(RdfSerialiser rdfserialiser) {
        Field afield[] = Agent.class.getDeclaredFields();
        Object o;
        for(int i = 0; i < afield.length; i++) {
            Field field = afield[i];
            try {
                if(rdfserialiser.isInterestingField(field))
                     o = rdfserialiser.addField(this, field, field.get(this));
                     if (o!=null) 
                        field.set(this,o );
            } catch(Exceptions...) { ... }
        }
 *   }
 * </pre></code>
 * 
 * running a serialiser can change the values of the fields.
 * It would be very useful if one could limit access to this method to Sommer packages only. 
 * In fact it would be a very useful if there were a way in java to declare certain packages to be
 * friendly (they could have full access to the code). The one would not even have to duplicate the 
 * code for every class.
 * 
 * @author hjs
 */
public abstract class RdfSerialiser {

   /** 
    * This is method will usually be called first. 
    * 
    * @return true if the field one that we want to work with? 
    */   
   public abstract boolean isInterestingField(Field fld);
 
   /**
    * This method will process the field. 
    * 
    * @return if the field value needs to be changed, this will do it, otherwise it should return null
    * Field values may need to be wrapped around another object.
    */ 
   public abstract Object processField(Object sourceObj, Field f, Object value);
   
   
   /** Sometimes one wants to be able to stop the processing. Perhaps one has found what was needed */
   public boolean continueProcessing() {
      return true;
   }
     
     //for Elmo, or later if I add method annotations....
     //void addMethod(Method m);
}
