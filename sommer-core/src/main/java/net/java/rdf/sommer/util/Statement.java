/*
 New BSD license: http://opensource.org/licenses/bsd-license.php

 Copyright (c) 2007 Sun Microsystems, Inc.
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

import java.net.URI;

/**
 * This represents an rdf Statement.
 *
 * It is a more flexible class than the usual equivalents in pure RDF frameworks, such as Sesame or Jena, since the
 * subject and the object can be normal java objects, not just resources, blank nodes or literals.  
 *
 * Clearly this can't be functioning quite like other @rdf classes. For example what would changing the subject
 * really amount to? Deleting a triple from the triple store and adding a new triple? What about a Relation object that
 * has null fields (subject, relation or object)? Inevitably this will have to be true for at least a few milliseconds.
 * Relations would have to be objects that are updated in bulk in the triple store. As such, it may not be interesting
 * to map them. Perhaps therfore my mapper needs a way to just add annotated objects to the store, without mapping them!
 *
 * @author Henry Story
 */
@rdf(rdf.rdfs+"Statement")
public class Statement<S,O> {

    @rdf(rdf.rdfs+"subject") private S subject; //the subject of the relation is an object, probably mapped in some way
    @rdf(rdf.rdfs+"predicate") private URI relation; //the relation is a URI
    @rdf(rdf.rdfs+"object") private O object; //the object is a string, uri, or bnode

    public Statement() {}

    public Statement(S subject, URI relation, O object) {
        this.subject = subject;
        this.relation = relation;
        this.object = object;
    }

    public Statement(Statement<S,O> template) {
        if (template == null) return;
        subject = template.getSubject();
        relation = template.getRelation();
        object = template.getObject();
    }

    public S getSubject() {
        return subject;
    }

    public void setSubject(S subject) {
        this.subject = subject;
    }

    public URI getRelation() {
        return relation;
    }

    public void setRelation(URI relation) {
        this.relation = relation;
    }

    public O getObject() {
        return object;
    }

    public void setObject(O object) {
        this.object = object;
    }

    public boolean isComplete() {
        return subject != null && relation != null && object != null;
    }

    @Override
    public String toString() {
        return subject + " -- "+relation+" --> "+ object  +" .";
    }
}
