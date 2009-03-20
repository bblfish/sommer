/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.rdf.sommer;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.memory.model.MemValueFactory;

/**
 *
 * @author hjs
 */
public class SesameMemorySailInit extends SesameMapper.SesameInit {
   
   
      ValueFactory vf =   new MemValueFactory();
      SailRepositoryConnection rep;
      boolean inferencing = false;


      public ValueFactory getValueFactory() {
         return vf;
      }
      
      public RepositoryConnection getConnection() {
         return rep;
      }
      
      public boolean hasInferencing() {
         return inferencing;
      }
   
      public SesameMemorySailInit() {
         try {
            MemoryStore mem = new org.openrdf.sail.memory.MemoryStore();
            mem.initialize();
            org.openrdf.repository.sail.SailRepository sail = new org.openrdf.repository.sail.SailRepository(mem);
            rep = sail.getConnection();
            vf = sail.getValueFactory();
         } catch (SailException e) {
            e.printStackTrace(); //todo: decide what exception to throw
            throw new Error("todo something better", e);
         } catch (RepositoryException e) {
            e.printStackTrace(); //todo: decide what exception to throw
            throw new Error("todo something better", e);
         }
 
      }


}
