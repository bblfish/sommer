Note:
To help clarify this README the  $SOMMER environmental variable here refers to the root of the sommer subversion directory in which this file is placed.

This subversion repository contains the following sub projects.

 So(m)mer
 --------

   The Semantic Object (metadata) Mapper mapps @rdf annotated java objects graphs to semantic web relation graphs. The java source code for this is in the $SOMMER/src directory and the test cases in $SOMMER/test. Code that uses so(m)mer annotated objects should be run via the$SOMMER/bin/sommer.sh script. This will first invoke a classrewriter that will inspect every loaded class and rewrite all those that have @rdf annotations to use a triple store (currently Sesame). 

Typically one would use sommer like this

  cd $SOMMER
  bin/sommer.sh eg.test.MappedTest

where eg.test.MappedTest is a class that contains a public static void main(String[] args) mthod.

 So(m)mer Ant Task
 -----------------
 
  This is an ant task that can rewrite java byte code to incorporate the mapping to java objects. This allows one to rewrite classes before usage, thereby reducing the run time intialisation cost. Using this one should take great care to make sure that no @rdf fields get used by classes that are not rewritten. The best way to make sure this happens is to make all those fields private, or package protected (i.e. one has to compile all the classes in a package).

  The So(m)mer ant task is in $SOMMER/misc/SommerAntTask   
  Compile it from there using "ant jar"
  An example of this in action is the Beatnik Address Book.
  Also to run the junit tests on the sommer test cases it is better to first compile this ant task, then run 
  $ ant tests
      
 Beatnik Address Book
 --------------------
 
  Beatnik is an example application of so(m)mer. It is an Address Book that consumes foaf files and can write out foaf files.

  See the $SOMMER/misc/AddressBook/README.txt for more information

