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


import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.rdf.sommer.Mapper.Init;

/**
 * MapperManager manages all the Java to RDF Maps.
 * Currently the Mapper Manager can only be tied to a specific repository via the Init.
 *
 * @author Henry Story
 */
public abstract class MapperManager  {
    protected static transient Logger log = Logger.getLogger(MapperManager.class.getName());

    /**
     * We keep a map of graph name to mapper objects
     */
    protected static HashMap<String, Mapping> graphMapper = new HashMap<String, Mapping>();

    //
    //Client available methods
    //

    static Init deflt;
    
    /**
     * warning don't call unless you want to get stuck with the SesameMemorySail...
     * call setInit first.
     * @return
     */
    public static Init getInit() {
       if (deflt == null) {
          deflt = (Init) new SesameMemorySailInit();
       }
       return deflt;
    }    
    
    /** 
     * set the default intialiser. This can only be set once, per JVM session. If this is to be changed
     * the we will have to create instances of MapperManager. If we do this then it won't be possible
     * to call the static methods on the class to get the Mapper. A different lookup mechansim will 
     * have to be developed.
     * @param defltInit
     */
    public static void setInit(Init defltInit) {
        if (deflt != null) {
           Error e = new Error("Init can only be called once per JVM setup");
           log.log(Level.SEVERE,"need to change the MapperManager",e);
           throw e;
        }
        deflt = defltInit;
    }
    
    /**
     * every Mapper specialises in mapping to a graph or union of graphs
     *
     * NOTE: it may be that one should relativise the mappers to the UIs. In which case
     * MainPanel.getStatusBar(FinderPanel.this); which iteraties through the JComponent's parents
     * until it finds the Main Panel. This would allow one to have numerous windows with different views
     * on the DB.
     *
     * In that case these methods would have to be moved to the MainPanel too.
     *
     * @param url the url for the graph(s) (should later also allow blank nodes. idea: if they start with '_:')
     *        the first url indicates the graph write operations are done in 
     * @return  A Mapper
     */
    public static Mapper getMapperForGraph(String... url) {
      return getMapperForGraph(getInit(),url);
    }

    /**
     * every Mapper specialises in mapping to a graph
     * @param init the Initialiser for the mapper.
     * @param url the url for the graph(s) (should later also allow blank nodes. idea: if they start with '_:')
     *        the first url indicates the graph write operations are done in 
     * @return  A Mapper
     */
    public static Mapper getMapperForGraph(Mapper.Init init, String... urls) {
        
        //graph mappers are not so simple anymore to keep track of once we have union graphs.
        //also it was wrong before to think that one could just map index the urls to the graphs...
        //after all could there not be different Inits for the same graph?
        //so there is more thinking that needs to be done here...
        
        //one thing is for sure mapping to the array of urls is not a good idea. A small change
        //in ordering of those urls will create different Mappers.
        
       //we will identify the graph by the first url.
       //later I should work on something better. perhaps we need a context in the database to keep track 
       //of which contexts belong together...
       //or perhaps that should be optional...
        
        Mapping m = graphMapper.get(urls[0]);
        if (m == null) {          
            m = (Mapping) init.create(urls[0]);
            graphMapper.put(urls[0], m);
        }
        return m;     
    }

    //
    // Method for rewriter
    //


    /**
     * Every object o if mapped belongs to a graph.
     * We may not need this later if we add information right into the object concerning which field the object
     * is mapped to. For the moment this is a call for the rewriter only.
     * @param o
     * @return  The RewriteMapper in which the object is mapped, or null
     *
     * Todo, add the mapper info to the objects directly
     */
    public static Mapper getMapperForObject(Object o) {
        for (Mapping m: graphMapper.values()) {
            if (m.isMapped(o)) {
                return m;
            }
        }
        return null;
    }



    /**
     *  Output the content of all the graphs
     */
    public static void outputAll() {
        System.out.println("in outputAll. "+ graphMapper.values().size()+ " graphs.");
        deflt.export(System.out);
    }




}
