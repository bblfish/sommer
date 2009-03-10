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
package net.java.rdf.annotations;


import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation to annotate classes, methods and arguments with a uri which is meant to be interpreted
 * as:<ul>
 * <li>for a class: the rdf:type of the class
 * <li>for a method: the relation between an instance of the class and the argument(s) of the method. If the
 * method takes a number of argument then it relates the ordered list of those arguments.
 * <li>for an argument: the name of a relation between an instance of a class and that value of the argument
 * <li>for a field: the field is thought of as a relation between an instance of the class and the value of the field
 * </ul>
 * The value must a full URI, by default it is owl:sameAs ie, the identity relation.
 * If it is the inverse of the relation that is intended, set inverse to true.
 *
 * Created by Henry Story
 * Date: Aug 28, 2005
 * Time: 11:51:43 PM
 */
@Retention(RUNTIME)
@Target({TYPE,CONSTRUCTOR, METHOD, FIELD, PARAMETER})
public @interface rdf {
    /** help compose xsd literal ranges */
    String xsd="http://www.w3.org/2001/XMLSchema#";
    String sameAs="http://www.w3.org/2002/07/owl#sameAs";
    String rdfs="http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /** 
     * the String must be a full URI
     * The URI is the name of the relation (or of the inverse of the relation in inverse() is true
     * The default value is the empty string, which indicates that one should use the urn:java:class.path.class uri . (note: this is experimental)
     * There has been discussion that if a namespace value was set on the
     * class or package then then no value could mean that ns+name_of_variable is the URL.
     * This would cause problems for refactoring. Someone changing the name of a variable would change the name
     * of the relation, which may not be what is expected.
     */
    String value() default "";

    /**
     * @return true if it is the inverse of the named relation (value()) that this relation represents
     */
    boolean inverse() default false;

    /**
     * @return the range uri (for literal types). Value is "" for default.
     */
    String range() default "";

}