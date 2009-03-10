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
package net.java.rdf.sommer.util;

import net.java.rdf.annotations.rdf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Methods for analysing class elements
 *
 * @author Henry Story
 */
public class ClassAnalysis {

    /**
     * Given a Type take from a collection, find it's class
     *
     * @param type
     * @return
     */
    public static Class collectionType(Type type) {
        Class result = Object.class;
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            Type[] targs = ptype.getActualTypeArguments();
            for (int j = 0; j < targs.length; j++) {
                result = (Class) targs[j];
            }
        }
        return result;

    }


    /**
     * retrun all rdf annotations from the array of annotation arrays
     *
     * @param annots
     * @return an array of rdf annotations, in the same order. Array of some length
     */
    public static rdf[] filterRdfAnnotations(Annotation[][] annots) {
        rdf[] answer = new rdf[annots.length];
        for (int i = 0; i < annots.length; i++) {
            for (int j = 0; j < annots[i].length; j++) {
                if (annots[i][j]instanceof rdf) answer[i] = (rdf) annots[i][j];
            }
            if (answer[i] == null) return null;
        }
        return answer;
    }

    public static ArrayList<Field> filterFields(Field[] fields, Class<? extends Annotation> ann) {
        ArrayList<Field> result = new ArrayList<Field>();
        for (Field f : fields) {
            if (f.getAnnotation(ann) != null) {
                result.add(f);
            }
        }
        return result;
    }

}
