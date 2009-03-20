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

import java.io.PrintStream;
import net.java.rdf.sommer.util.GraphUpdateException;
import net.java.rdf.sommer.util.ParseException;
import net.java.rdf.sommer.util.Statement;

import java.util.Collection;
import java.util.List;
import java.io.Reader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * This is the interface the clients use to Map objects to a graph,
 * (as well as remove them).
 *
 * @author Henry Story
 */
public interface Mapper {

   /**
    * Different RDF stores usually require very different  initialisations.
    * The Init interface will be implemented for each of the stores, and extended to
    * give access to objects specific to that store.
    * I have placed this in a seperate class so that people don't have to subclass what will 
    * usually be a very large Mapper class.
    * But this might not be the right solution. Feel free to be critical.
    */
   interface Init {

      /**
       * export all the triples of this repository 
       * (in TriG format currently, would prefer N3, or this to be settable)\
       * FIXME: there is no reason this should be in the INIT really! put it back to Mapper.
       * 
       * @param out
       */
      public void export(PrintStream out);

      /** 
       * create a mapper instance that ranges over the union of the given contexts
       * @param uris a number of uris naming the graphs. May also be blank nodes if they start with '_:'
       * @return a subclass of Mapper
       **/
       Mapper create(String... uris);
    }
   
   public void enableInferencing(boolean on);


   /**
    * cast the object to the given class.
    * In RDF an object can have multiple superclasses. Sometimes the two java classes that
    * implement their behavior are part of different hierarchies. This will try to find an object
    * of the right class with the same id, or it will create a new such object in the mapper.
    * @param <T>
    * @param o
    * @param clazz
    * @return
    */
   public <T> T cast(Object o, Class<T> clazz);


   /** 
    * This returns the underlying mapped object.  
    * This is useful. Sometimes it is just a lot easier to work at the triple level.
    * 
    * thought: In a very well designed system, this could in fact be the object itself.
    *
    * Questions:
    * - If the object is such an rdf object, should this method just return it back?
    * - If the object is a literal, should this return the rdf literal associated with it?
    * 
    * @return an object in the rdf hierarchy. Eg a sesame.Resource object. 
    * subclasses of Mapper can be more specific in what gets returned. 
    */
   public Object getId(Object sommermaped);
   
   /**
    * get the objects related via relation to the subject. 
    * This is one of the methods that in a more dynamic language would be placed on the object itself.
    * In Java this requires bytecode rewriting, and that does not work well with many IDEs
    * 
    * @param subject the object that is the subject of the relations 
    * @param relation the relation as a URI
    * @param inverse is it the inverse of the relation that is desired?
    * @param type the type of the objects to return (could this be one of the underlying RDF objects such as Sesame's Resource?)
    * @return
    */
   public <T> Collection<T> getRelatedObject(Object subject, String relationURI, boolean inverse, Class<T> type);
   
   
   public boolean isInferencingEnabled();

    /**
     *
     * @param r a Reader on the rdf
     * @param baseUri the base to resolve relative uri from
     * @param mimeType of the rdf serialisation: rdfxml, n3, turtle, ntriples, ...
     */
    public void importFrom(Reader r, String baseUri, String mimeType) throws IOException, GraphUpdateException, ParseException;

    /**
     *  import graph serialisation into the given context.
     *  Add the context to the list of contexts we are working with     *
     *
     * @param r  the reader containing the serialisation of the graph
     * @param baseUri the base uri to xxx relative uris
     * @param mimeType format of the serialisation ( currently the mime type)
     * @param context the context to drop the information into
     * @throws IOException
     * @throws GraphUpdateException
     * @throws ParseException
     */
    public void importInto(Reader r, String baseUri, String mimeType, String context) throws IOException, GraphUpdateException, ParseException;

    /**
     *  Delete this graph, and all objects associated to it.
     */
    public boolean clear();

    /**
     *
     * note: We should be able to ask the object directly now that it implements SommerMapable
     *       except of course that we are adding this directly
     * @param obj a potentially mapped object
     * @return true if the object is mapped, false otherwise
     * 
     */
    public boolean isMapped(Object obj);

    
    /**
     * find the classes of the object with id uri
     * @param uri the id of the object looked for
     * @return a list of classes
     */
    public List<Class> getClassesOf(String uri);
    
    
    /**
     * Fetch an object of type with a given uri.
     * 
     * Note. There may be a good case for having a similar method that can also take
     * a literal.
     * 
     * @param uri  the id of the object
     * @param clazz the type of the object looked for
     * @return null if none available in the database or an object mapped 
     *   (if there is already an object mapped to that id, it should return that one)
     */
    public <T> T getObjectById(String uri, Class<T> clazz);


    /**
     * Create an object of type with a given uri
     * 
     * @param uri  the id of the object
     * @param clazz the type of the object looked for
     * @return an object (null only if the class has no @rdf annotation?)
     */
    public <T> T createObjectWithId(String uri, Class<T> clazz);
    
    public <T> Collection<T> getAllObjectsOfType(Class<T> clazz);


    /**
     * Insert the object and all its dependent objects into the graph
     * @param object the object whose relations should be added to the graph
     * @return  the same object, for method chaining
     */
    public <T> T addObject(T object);


    /**
     * Insert all objects
     * @param objects
     * @return
     */
    public boolean addObjects(Object... objects);


    /**
     * Add the following statements to the writeable graph.
     * The Subject and Object of the Statement can be mapped objects, in which case their id will be used.
     * If they are not mapped objects, what should one do? Map them first and then add the statement?
     * @param statements
     */
    public void addStatements(Collection<Statement> statements);


    /**
     * remove the following statements from the graphs. (this may require making diffs of non writeable graphs)
     * @param statements
     */
    public void removeStatements(Collection<Statement> statements);


    /**
     * remove all statements concerning the resource this object is mapped to.
     * The object will no longer appear in the rdf graph after this.
     * note: On inferencing graphs this may not lead to the resource being removed
     *   if the resource is implied by other facts in the database.
     * todo: this needs to be looked at a lot more carefully
     * todo: is it a good thing to be able to remove an object? It could be quite a problem, as it could invalidate a number
     * of other objects...
     * @param obj
     * @return false if it never was part of the graph
     */
    public boolean remove(Object obj );

    /**
     * This will remove the object from the map, but the relations will remain in the database.
     * todo: understand what this would mean for all the objects in points to or all objects pointed to it
     * return true if unmapped, false if object was never mapped
     */
    public boolean unmap(Object obj);

    /**
    * Output the contents of this graph
     * todo: add type of output (N3,rdf/xml)
    */
    void output(OutputStream out);
    
    /**
    * Output the contents of this graph
     * todo: add type of output (N3,rdf/xml)
    */
    void output(Writer out);


    /**
     * The Size in triples of the graph, mapped to.
     * 
     * @return the size of the graph, or -1 if there was a problem finding out
     */
    long  size(); 

    String graphId();

    /**
     * Equality of mapped objects.
     * As far as the mapper knows these two objects are equals. Inferencing graphs will do a
     * better job of finding object equality than non inferencing graphs.
     * We can't use Object.equals(o) because that is too closely tied to the hash signature. In
     * the graph equality between objects can be discovered over time, as new relations are added to
     * the database. Equality between object may possibly also be broken, as relations are altered.
     *
     * if either object is not mapped, then this returns false
     * if both objects are mapped and == then this is true
     * if there is a owl:sameAs relation between them then it is true
     * else it is false.
     *
     * @param mapped1 a mapped object
     * @param mapped2 another mapped objects
     * @return as far as this mapper knows mapped1 and mapped2 are the same
     */
    boolean equals(Object mapped1, Object mapped2);

    /**
     * Query the map by giving an example object, and return all objects that fit the example.
     * Here the example is an object with @rdf annotated fields. Only looks at fields with values.
     * Note: objects with simple literal values such as char, int, etc... are always set. So beware.
     *
     * @param eg example object. Should not be a mapped object
     * or an object with a id specified by @rdf URI id since otherwise the only object returneable would be
     * the one given. The object can have fields that are set either to mapped or  unmapped objects.
     * @param clazz of objects to return  (should this not just be the same as the class of o?
     * @return a Collection of objects of the given that fit the example.
     */
    <T> Collection<T> queryByExample(Object eg, Class<T> clazz);


    /**
     * Sometimes one needs to preload a bunch of annotated classes in order to take account of hierarchies.
     * We want to be able to have the most specific java class possible instantiate an rdf object. But we cannot walk
     * down a java class hierarchy.
     * see Mapping.mostSpecific(clazz,uri); 
     * TODO: this should be on the MapperManager class. It should not be specific to different graphs (otherwise it is going to get
     * tedious as one used different mappers)
     */
    public void preload(Class... classes);

    /** run the inferencers on this graph one after the other */
    void cogitate(List<Inferencer> inferenceList);
    
    
    /** A Mapper for relations identified by relationURI */
 // I would like something like this, but will generalise from some specialised use cases that I will try out first   
 //   <F,T> RDFMap<F,T> mapperForRelation(String relationURI, Class<F> from, Class<T> to);


    /**
     * Initialise the static fields of given class with relations from this map.
     * Warning: obviously changing static fields in a class will affect all uses of it across the application
     * for a same class loader.  Great care should be taken with this method.
     * To start off with I won't implement write functionality, that would require byte code rewriting
     * Other question: should this happen automatically when a an object is mapped? Answer: no, because
     * one may want static info to be mapped in one graph and non static info in another.
     * Classes with no @rdf annotations will be given the URI urn:java:fullclassname (good idea?)
     *
     * @param c the class to map
     */
    public void mapStatic(Class c);

}
