/*
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

import net.java.rdf.annotations.rdf;

import java.lang.reflect.Type;
import java.util.Collection;
import net.java.rdf.sommer.util.RDFCollection;

/**
 * These are the methods required by JavassistClassRewriter
 *
 * @author Henry Story
 */
public interface RewriteMapper {


    /**
     * Set a Collection on a given field.
     *
     * @param obj      the object on which the field is attached
     * @param relation rdf annotation on the field
     * @param newcoll     the value that the field should be set to
     * @param type     the generic type of the collection (if known)
     *
     * @return The object the field is set to
     */
    public  <T> Collection<T> setCollectionField(Object sourceObj,
                                                 rdf relation,
                                                 Collection<T> newColl,
                                                 Type typeT);
    
    

    /**
     * get the value of a collection field. 
     * These are collections that have the (at)functional annotation (or something with a better name, when I get round to it)
     * They are collections that really appear as collections in the rdf store. 
     *
     * @param fieldType  the generic type of the collection
     * @param obj        the object on which the field is attached
     * @param relation   annotation on this field
     * @param old        the old value of the field - the one currently referred to
     *
     * @return The object the field is set to
     */
    public <T> Collection<T> getCollectionField(Object thiz,
                                                rdf relation,
                                                Collection<T> old,
                                                Type fieldType);
    
    /**
     * get the value of a virtual collection field.
     * This will only be called once: on mapping of the object into the graph. Later the object
     * present in the field will be returned.
     *
     * should this be also called when the object gets unmapped, so that it can return an 
     * unmapped collection?
     * 
     * @param thiz       the object on which the field is attached
     * @param relation   annotation on this field
     * @param old        the old value of the field - the one currently referred to
     * @param fieldType  Gernics type info of the Collection
     * 
     * @return The object the field is set to
     */
    public <T> RDFCollection<T> createVirtualCollection(Object thiz, 
                                                 rdf relation, 
                                                 Collection<T> old, 
                                                 Type fieldType);
    
    
    /**
     * Replace the elements in the collection with the new elements.
     * 
     * @param thiz      the object on which the field is attached
     * @param relation  the annotation on the field
     * @param oldColl   the old collection that the field was set to
     * @param newColl   the new collection the it should be replaced by
     * @param genType    the type of the objects in the collection
     * @return the collection containing the objects related to thiz by relation. This may be the
     *         same object as oldColl
     */ 
    public <T> RDFCollection<T> replaceVirtualCollection(Object thiz, 
                                                 rdf relation, 
                                                 Collection<T> oldColl,
                                                 Collection<T> newColl,
                                                 Type genType);
    
    

    /**
     * Set a given field
     *
     * @param fieldClass The class of the field being set
     * @param obj        The object on which the field is attached
     * @param relation   The rdf annotation on the field
     * @param value      the value that the field should be set to
     * 
     * @return The object the field is set to
     */
    public Object setField(Class fieldClass,
                           Object obj,
                           rdf relation,
                           Object value);

    /**
     * Get the value of a field (from the database), and remove the old value
     *
     * @param fieldClass the class of the field
     * @param obj        the object on which the field is located
     * @param relation   the rdf annotation on the field
     * @param value      the current value of the field
     * @return the value of the field
     */
    public Object getField(Class fieldClass, Object obj,
                    rdf relation,
                    Object value);


    /**
     * Set the name for thiz to an existing one if one exists.
     * The relations together form a CIFP (see http://esw.w3.org/topic/CIFP)
     * The array size for relations and values should be the same
     *
     * @param thiz     the object to be named
     * @param clazz    the class of the constructor
     * @param argTypes the argument types identifying the particular constructor
     * @param values   the values
     * @deprecated No longer rewrite constructors
     */
    public void cifpName(Object thiz, Class clazz, Class[] argTypes, Object[] values);

    /**
     * Check to see if we don't allready have an object that corresponds to this cifp
     *
     * @param clazz    the class on which the constructor is being called
     * @param argTypes the types of the arguments of the constructor, to help find the constructor
     * @return the object that allready corresponded to that cifp, or null if none
     * @deprecated No longer rewrite constructors
     */
    public <T> T cifpObject(Class<T> clazz, Class[] argTypes, Object[] values);


}
