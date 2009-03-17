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

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryException;

import java.util.ArrayList;
import java.util.List;

/**
 * Do inverse functional inferencing for a set of inverse functional relations on a given graph.
 * This adds owl:sameAs statements to the graph, for later smushing.
 *
 * @author Henry Story
 */
public class SesameInvFuncInferencer implements Inferencer {
    SesameMapper map;          //warning: this makes this non thread safe!
    List<String> relations;

    public SesameInvFuncInferencer(List<String> relations) {
        this.relations = relations;
    }

    public void infer(Mapper map) {
        this.map = (SesameMapper)map;
        for (String relation: relations) {
            calculate(relation);
        }
    }

    private void calculate(String relation) {
       String seRQL = "SELECT DISTINCT s1, s2 " +
                      "FROM CONTEXT ";
       for (int i = 0; i < map.graphs.length; i++, seRQL +=",") {
         seRQL += "<" +map.graphs[i].toString() +">";        
       }       
       seRQL +=       "     {s1} <"+relation+"> {obj}, " +
                      "     {s2} <"+relation+"> {obj} " +
                      "WHERE s1 != s2 ";
        System.out.println("serql="+seRQL);
        ArrayList<Statement> answers = new ArrayList<Statement>();
        TupleQueryResult cit = null;
        try {
            cit = map.rep().prepareTupleQuery(QueryLanguage.SERQL, seRQL).evaluate();
            while (cit.hasNext()) {
                BindingSet v = cit.next();
                answers.add(map.vf.createStatement((Resource)v.getValue("s1"),OWL.SAMEAS,v.getValue("s2")));
            }
        } catch (MalformedQueryException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        } catch (RepositoryException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        } catch (QueryEvaluationException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        } finally {
            if (cit !=null) try {
                cit.close();
            } catch (QueryEvaluationException e) {
                e.printStackTrace();  //todo: decide what exception to throw
            }
        }
        System.out.println("RESULTS FROM QUERY = "+answers.size());
        for (Statement s: answers) {
            System.out.println("adding:"+s);
        }
        try {
            map.rep().add(answers);
        } catch (RepositoryException e) {
            e.printStackTrace();  //todo: decide what exception to throw
        }

    }


}
