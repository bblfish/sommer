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

import info.aduna.collections.iterators.CloseableIterator;
import org.openrdf.model.*;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import java.util.*;
import java.util.Map.Entry;

/**
 * This smushes Sesame (2.0beta2) graphs.
 *
 * @author Henry Story
 */
public class SesameSmushInferencer implements Inferencer {


    public void infer(Mapper m) {
        SesameMapper map = (SesameMapper)m;
        RepositoryResult<Statement> iter = null;
        TreeMap<Resource, HashSet<Resource>> smasMp = new TreeMap<Resource, HashSet<Resource>>(new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                //always keep uris
                if (o1 instanceof URI && !(o2 instanceof URI)) return 1;
                if (!(o1 instanceof URI) && o2 instanceof URI) return -1;
                return o1.toString().compareTo(o2.toString());
            }
        });
        try {
            iter = map.rep().getStatements(null, OWL.SAMEAS, null, false);
            while (iter.hasNext()) {
                Statement smas = iter.next();
                Resource subj = smas.getSubject();
                Value objVal = smas.getObject();
                if (objVal instanceof Literal) continue;
                Resource obj = (Resource) objVal;
                insertIntoMap(smasMp, subj, obj);
                insertIntoMap(smasMp, obj, subj);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        } finally {
            if (iter != null) try {
                iter.close();
            } catch (RepositoryException e) {
                e.printStackTrace();  //todo: decide what exception to throw
            }
        }
        HashSet<Resource> delKeys = new HashSet<Resource>();
        for (Entry<Resource, HashSet<Resource>> rel : smasMp.entrySet()) {
            if (delKeys.contains(rel.getKey())) continue; //we have allready dealt with this
            Collection<Resource> objs = rel.getValue();
            while (objs.size() > 0) {
                ArrayList<Resource> toAdd = new ArrayList<Resource>();
                for (Resource okey : objs) {
                    if (delKeys.contains(okey)) continue;
                    HashSet<Resource> objColl = smasMp.get(okey);
                    if (objColl == null) continue;
                    toAdd.addAll(objColl);
                    delKeys.add(okey);
                }
                toAdd.removeAll(objs);
                toAdd.remove(rel.getKey());
                objs.addAll(toAdd);
                objs = toAdd;
            }
        }

        for (Resource key : delKeys) {
            smasMp.remove(key);
        }

        for (Entry<Resource, HashSet<Resource>> rel : smasMp.entrySet()) {
            for (Resource obj : rel.getValue()) {
                smush(rel.getKey(), obj,map);
            }
        }

    }

    private void smush(Resource remove, Resource replace, SesameMapper map) {
        ArrayList<Statement> newStmts = new ArrayList<Statement>();
        ArrayList<Statement> delStmts = new ArrayList<Statement>();

        //find all statements that have the node as a subject
        RepositoryResult<Statement> iter = null;
        try {
            iter = map.rep.getStatements(remove, null, null,false);
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                if (!stmt.getPredicate().equals(OWL.SAMEAS)) {
                    newStmts.add(map.vf.createStatement(replace, stmt.getPredicate(), stmt.getObject()));
                } //since we are removing owl:sameAs
                delStmts.add(stmt);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        } finally {
            if (iter !=null) try {
                iter.close();
            } catch (RepositoryException e) {
                e.printStackTrace();  //todo: decide what exception to throw
            }
        }

        //find all the statements that have the node as an object
        try {
            iter = map.rep.getStatements(null, null, remove,false);
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                if (stmt.getObject() instanceof Literal) continue;
                if (!stmt.getPredicate().equals(OWL.SAMEAS)) {
                    newStmts.add(map.vf.createStatement((Resource) stmt.getSubject(), stmt.getPredicate(), replace));
                }
                delStmts.add(stmt);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        } finally {
              if (iter!=null) try {
                  iter.close();
              } catch (RepositoryException e) {
                  e.printStackTrace();  //todo: decide what exception to throw
              }
        }


        //remove en bloc and add en bloc
        try {
            map.rep().add(newStmts, map.graphs);
            map.rep().remove(delStmts, map.graphs);
        } catch (RepositoryException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        }
    }

    /**
     * could use apache collections classes, but don't want to bother just for this case
     */
    void insertIntoMap(TreeMap<Resource, HashSet<Resource>> smasMp, Resource subj, Resource obj) {
        HashSet<Resource> list = smasMp.get(subj);
        if (list == null) {
            list = new HashSet<Resource>();
            smasMp.put(subj, list);
        }
        list.add(obj);
    }


}
