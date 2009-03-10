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

import java.util.ArrayList;
import net.java.rdf.sommer.SesameMapper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * An  unbuffered collection. Once the elements are added to the collection, all calls go straight to the rdf
 * database.
 * (easier to debug)
 * class should probably called SesameRDFCollection as it does depend on Sesame Resource...
 *
 * @author Henry Story
 */
public class RDFCollection<E> implements Collection<E> {
    Resource source;
    URI relation;
    boolean inverse = false;
    Class superType;
    SesameMapper map;
    ArrayList cache = null;

    /**
     * Map all of its elements into a Semantic web collection taking this as subject
     *
     * @param collection the collection of objects
     */
    public RDFCollection(SesameMapper map, Collection<E> collection, Class<E> supertype) {
        this.map = map;
        this.source = (Resource) map.map(this);
        relation = RDFS.MEMBER;
        map.addRelation(source, RDF.TYPE, false, RDFS.CONTAINER);
        addRelations(collection);
        this.superType = supertype;
    }


    /**
     * creates a parallel world in the database
     *
     * @param collection the java collection to map to an rdf collection
     * @param source     the source of the relations (usually the calling object)
     * @param relation   the name of the relation between the source and every member of the collection collection
     * @param inverse    is it the inverse of the relation that is desired?
     */
    public RDFCollection(SesameMapper map, Collection<E> collection, Class<E> supertype, Object source, URI relation, boolean inverse) {
        this.map = map;
        this.source = (Resource) map.map(source);
        this.relation = relation;
        this.inverse = inverse;
        this.superType = supertype;
        if (collection!=null) addRelations(collection);
    }

    /**
     * A pure rdf collection object, where we know the rdf name of the collection in advance.
     * Here we fill the collection with information from the database
     *
     * @param id        the id of the collection.
     * @param supertype the type of objects in the collection
     */
    public RDFCollection(SesameMapper map, Resource id, Class<E> supertype) {
        this.map = map;
        map.name(this, id);
        this.relation = RDFS.MEMBER;
        this.source = id;
    }


    private void addRelations(Collection<E> wrapped) {
        for (E item : wrapped) {
            map.addRelation(source, relation, inverse, item);
        }
    }
    
    Date updatedAt;
    private Collection<E> getCurrentCollection() {
        if (cache == null || map.isDirty(updatedAt)) {  
            //XXX This seems to get called very very frequently.
            //Removed logging since it was logged so much it was
            //making any other logging unusable. - Tim
            cache = map.getCollection(source, relation, inverse, superType);
            //clearly the wrong way to do it, but good enough for a test
            updatedAt = new Date();
        }
        return cache;
    }

    /**
     * replace all the members of this collection with those of the new collection
     * 
     * the algorithm could perhaps be improoved to work out if relations already exist before removing them...
     */ 
    public void replaceWith(Collection newColl) {
       clear();
       addAll(newColl);
    }
    
    public int size() {
        return getCurrentCollection().size();
    }

    public boolean isEmpty() {
        return getCurrentCollection().isEmpty();
    }

    public boolean contains(Object o) {
        return getCurrentCollection().contains(o);
    }

    public Iterator<E> iterator() {
        return new MRIterator();
    }

    public Object[] toArray() {
        return getCurrentCollection().toArray();
    }

    public <T> T[] toArray(T[] a) {
        return getCurrentCollection().toArray(a);
    }

    public boolean add(E o) {
        //todo: check: not sure if this fullfills the add contract concerning the return value
        if (map.addRelation(source, relation, inverse, o))  {
             ArrayList cacheofcache = cache;
            if (getCurrentCollection() == cacheofcache) {
                if (!cache.contains(o)) {
                    //we have to do this because we are dealing with a Collection and not a list.
                    //It may be better to use a HashSet for the cache
                    cache.add(o);
                }
            } // else the current collection is now up to date
            return true;
        }
        return false;
    }

    public boolean remove(Object o) {
       
        return map.removeRelation(source, relation, inverse, o);
    }

    public boolean containsAll(Collection<?> c) {
        return getCurrentCollection().containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean result = true;
        for (E o : c) {
            //todo: could work on faster implementation!
            //todo: also this should be done in a transactional way
            if (!map.addRelation(source, relation, inverse, o))
                result = false;
        }
        ArrayList cacheofcache = cache;
         if (getCurrentCollection() == cacheofcache) {
             for (E o: c) {
              if (!cache.contains(o)) {
                 //we have to do this because we are dealing with a Collection and not a list.
                 //It may be better to use a HashSet for the cache
                 cache.add(o);
              }
             }
         }
        return result;
    }

    //todo check if the return value really respects the contract
    public boolean removeAll(Collection<?> c) {
        boolean success = true;
        for (Object o : c) {//could work on faster implementation!
            if (!map.removeRelation(source, relation, inverse, o))
                success = false;
        }
        return success;
    }

    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : getCurrentCollection()) {
            if (!c.contains(o)) {
                remove(o);
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        map.removeAll(source, relation, inverse);
    }

    class MRIterator implements Iterator {
        Iterator<E> wit;
        E current;

        MRIterator() {
            wit = getCurrentCollection().iterator();
        }

        public boolean hasNext() {
            return wit.hasNext();
        }

        public E next() {
            current = wit.next();
            return current;
        }

        public void remove() {
            wit.remove();
            map.removeRelation(source, relation, inverse, current);
        }
    }
}
