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

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import net.java.rdf.annotations.functional;
import net.java.rdf.annotations.rdf;
import java.io.*;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Henry Story
 */
public class JavassistClassRewriter {

   protected static transient Logger log = Logger.getLogger(JavassistClassRewriter.class.getName());

   public static void main(String[] args) throws NotFoundException, CannotCompileException,
           ClassNotFoundException {
      if (args.length == 0) {
         message();
      } else if ("--run".equals(args[0])) {
         try {
            getRewriteLoader().run(args);
         } catch (Throwable ex) {
            ex.printStackTrace();
         }
      } else if ("--path".equals(args[0]) && (args.length > 1)) {
         //we are given a list of classes that we rewrite and then save
         ClassPool pool = ClassPool.getDefault();
         try {
            pool.insertClassPath(args[1]);
         } catch (NotFoundException e) {
            message();
            log.severe("could not find classpath " + args[1]);
            e.printStackTrace();
            System.exit(1);
         }
         File dir = new File(args[1]);
         ArrayList<File> classfiles = new ArrayList<File>();
         descendThrough(dir, classfiles);
         SommerEditor edt = new SommerEditor();
         for (File cf : classfiles) {
            try {
               log.info("rewriting class file " + cf);
               CtClass clzz = pool.makeClass(new FileInputStream(cf));
               clzz.instrument(edt);
               clzz.toBytecode(new DataOutputStream(new FileOutputStream(cf))); //todo: should not necessarily output to the same file
            } catch (IOException e) {
               log.warning("could not read file" + cf);
               e.printStackTrace(); //todo: decide what exception to throw
            } catch (CannotCompileException e) {
               log.severe("could not javassist compile " + cf);
               e.printStackTrace(); //todo: decide what exception to throw
               System.exit(-1);
            }
         }
      } else if ("--cp".equals(args[0]) && (args.length > 2)) {
         //we are given a list of files to rewrite and then save
         ClassPool pool = ClassPool.getDefault();
         try {
            pool.insertClassPath(args[1]);
            pool.insertClassPath("/Users/hjs/Programming/sommer/sommer.jar");
         } catch (NotFoundException e) {
            message();
            log.severe("could not find classpath " + args[1]);
            e.printStackTrace();
            System.exit(1);
         }
         SommerEditor edt = new SommerEditor();
         for (int i = 2; i < args.length; i++) {
            File cf = new File(args[i]);
            try {
               log.info("rewriting class file " + cf);
               CtClass clzz = pool.makeClass(new FileInputStream(cf));
               clzz.instrument(edt);
               clzz.toBytecode(new DataOutputStream(new FileOutputStream(cf))); //todo: should not necessarily output to the same file
            } catch (IOException e) {
               log.warning("could not read file" + cf);
               e.printStackTrace(); //todo: decide what exception to throw
            } catch (CannotCompileException e) {
               log.severe("could not javassist compile " + cf);
               e.printStackTrace(); //todo: decide what exception to throw
               System.exit(-1);
            }
         }
      }
   }

   static HashSet<CtClass> changedClasses = new HashSet<CtClass>();

   /**
    * for some reason clazz.ismodified too often returns true, so we'll be more explicit here.
    * @param clazz
    */
   static void markChanged(CtClass clazz) {
      if (!changedClasses.contains(clazz))
          logInfo("changed class: "+clazz.toString());
      changedClasses.add(clazz);
   }
   /**
    * Transform a set of classes.
    * @param classpath  a classpath for finding relevant classes
    * @param classfiles the classes to transform
    * @param baseOutputDir the base directory in which the classes get saved (after creating the appropriate package dirs)
    */
   public static void transformFiles(List<String> classpath,
           List<String> classfiles, String baseOutputDir,
           String markerName, String markerValue) throws Exception {
      ClassPool pool = ClassPool.getDefault();
      try {
         for (String path : classpath) {
            pool.insertClassPath(path);
         }
      } catch (NotFoundException e) {
         throw e;
      }

      SommerEditor edt = new SommerEditor();
      FileInputStream fin = null;
      FileOutputStream fout = null;
      CtClass stringCl = pool.get("java.lang.String");

      //we need to do this in stages
      HashMap<CtClass, String> clzzes = new HashMap<CtClass, String>();  //all classes that have @rdf annotations (we will need to add methods to them


      //0. Remove classes that have allready been processed
      for (String file : classfiles) {
         fin = new FileInputStream(file);
         CtClass clzz = pool.makeClass(fin);

         if (markerName != null && markerValue != null) {
            try {
               clzz.getDeclaredField(markerName);
               log.info("Skipping. Allready processed file " + file);
               continue; //this has allready been processed
            } catch (NotFoundException e) {
               //ok
            }
         }
         clzzes.put(clzz, file);
      }

      CtClass serializableInt = ClassPool.getDefault().get("net.java.rdf.sommer.RdfSerialisable");
      CtClass serializerInt = ClassPool.getDefault().get("net.java.rdf.sommer.RdfSerialiser");
      CtClass sommerMapableInt = ClassPool.getDefault().get("net.java.rdf.sommer.SommerMapable");

//      String serialiserImplStr = "{ ";
//      CtMetod serialiserImpl = CtMethod.make(arg0, stringCl)
      HashSet<CtClass> annotatedClasses = new HashSet<CtClass>();

      //1. ADD abstract methods + implementation of RDFSerialiser and SommerMapable
      //see: the "Mutual recursive methods" section of http://www.csg.is.titech.ac.jp/~chiba/javassist/tutorial/tutorial2.html#add
      //arggh: this is an anoyning aspect of javassist
      HashMap<CtField, CtMethod> getFieldMethod = new HashMap<CtField, CtMethod>();
      HashMap<CtField, CtMethod> setFieldMethod = new HashMap<CtField, CtMethod>();
      for (CtClass clazz : clzzes.keySet()) {
         JavassistClassRewriter.logInfo("attempting to add methods and interfaces on class " +
                 clazz.getName());
         CtField[] flds = clazz.getDeclaredFields();

         if (SommerEditor.getRdf(clazz.getAnnotations()) != null) {
            annotatedClasses.add(clazz);
         }

         boolean rdffields = false;
         for (CtField fld : flds) {
            try {
               Object[] annotations = fld.getAnnotations();
               if (SommerEditor.getRdf(annotations) == null) {
                  continue;
               }
               if (Modifier.isStatic(fld.getModifiers())) {//TODO: change. Add setter and getter methods here too.
                  markChanged(clazz);
                  continue;
               }
               if (!rdffields) {
                  rdffields = true;
                  annotatedClasses.add(clazz);
               }
               String getterSig = EditorTranslator.getterSignatureForField(clazz, fld) +
                       ";";
               String setterSig = EditorTranslator.setterSignatureForField(clazz, fld) +
                       ";";

               CtMethod getter = CtMethod.make(getterSig, clazz);
               CtMethod setter = CtMethod.make(setterSig, clazz);

               clazz.addMethod(getter);
               clazz.addMethod(setter);
               markChanged(clazz);

               getFieldMethod.put(fld, getter);
               setFieldMethod.put(fld, setter);
            } catch (CannotCompileException e) {
               log.severe("could not javassist compile " + clazz.getName());
               e.printStackTrace(); //todo: decide what exception to throw
               throw e;
            }
         }
      }

      //add RDFSerialisable and SommerMapable interfaces 
      //add the implementation of SommerMapable, as there will only be one implementation
      //only add the interfaces to the topmost class that needs it
      for (CtClass clazz : annotatedClasses) {
         if (clazz.isInterface()) {
            continue;
         } //should never happen, but anyway
         CtClass lastClazz = clazz;

         for (CtClass spr = clazz.getSuperclass(); spr != null; spr = spr.getSuperclass()) {
            //check that this superclass is not in the list;
            JavassistClassRewriter.logInfo("looking for class=" + spr.getName());
            if (annotatedClasses.contains(spr)) {
               lastClazz = spr;
            }
         }
         if (!lastClazz.subtypeOf(serializableInt)) {
            JavassistClassRewriter.logInfo("adding RDFSerialisation inferface, sommerMapable and implementation to " +
                    lastClazz.getName());
            lastClazz.addInterface(serializableInt);
            lastClazz.addInterface(sommerMapableInt);
            EditorTranslator.addSetSommerMapperMethod(lastClazz);
            markChanged(lastClazz);
         } else {
            JavassistClassRewriter.logInfo("could not add RDFSerialisation inferface, sommerMapable and implementation to " +
                    lastClazz.getName());
         }
      }

//      ArrayList<CtClass> sorthem = new ArrayList<CtClass>(annotatedClasses);
//      Collections.sort(sorthem, new Comparator<CtClass>() {
////not a total order, but it's not a hash, so it should be ok?
//         public int compare(CtClass o1, CtClass o2) {
//            try {
//            if (o1.subtypeOf(o2)) return 1;
//            if (o2.subtypeOf(o1)) return -1;
//            else return 0;
//            } catch (NotFoundException e) {
//               return 0;
//            }
//         }
//      });

      {
         CtClass[] serialisableArgs = new CtClass[]{serializerInt};
         //5. Add the implementation for the Serialisable class   and the mapperma
         for (CtClass clazz : annotatedClasses) {
            for (CtClass spr = clazz; spr != null; spr = spr.getSuperclass()) {
               if (spr.subtypeOf(serializableInt) &&
                       annotatedClasses.contains(spr)) {
                  try {
                     CtMethod rdfSerialise = spr.getDeclaredMethod("rdfSerialise", serialisableArgs);
                  } catch (NotFoundException e) {
                     CtMethod m = CtNewMethod.make("public void rdfSerialise(net.java.rdf.sommer.RdfSerialiser s);",
                             spr);
                     spr.addMethod(m);
                  }
               }
            }
            EditorTranslator.addSerialiserMethod(clazz);
         }
      }


      //  replace calls to fields with calls to methods in all classes (that have not been markes as transformed!)
      //   and then add the so(m)mer marker to the class
      //note: This cannot be done after implementations of these methods has been added, or else the implementations will themselves be rewritten!
      for (CtClass clzz : clzzes.keySet()) {
         try {
            clzz.instrument(edt);
         } catch (CannotCompileException e) {
            log.severe("could not javassist compile " + clzz.getName());
            e.printStackTrace(); //todo: decide what exception to throw
            throw e;
         }
      }



      //3.1 ADD METHOD bodies: getters
      for (Iterator<Entry<CtField, CtMethod>> it = getFieldMethod.entrySet().iterator(); it.hasNext();) {
         Entry<CtField, CtMethod> entry = it.next();
         try {
            EditorTranslator.addGetterMethod(entry.getKey(), entry.getValue());
         } catch (NotFoundException e) {
            log.severe("could not javassist compile " + entry.getKey().getName());
            e.printStackTrace(); //todo: decide what exception to throw
            throw e;
         }
      }

      //3.2 ADD METHOD bodies: setter
      for (Iterator<Entry<CtField, CtMethod>> it = setFieldMethod.entrySet().iterator(); it.hasNext();) {
         Entry<CtField, CtMethod> entry = it.next();
         try {
            EditorTranslator.addSetterMethod(entry.getKey(), entry.getValue());
         } catch (NotFoundException e) {
            log.severe("could not javassist compile " + entry.getKey().getName());
            e.printStackTrace(); //todo: decide what exception to throw
            throw e;
         }
      }


      //4. Make those classes that have changed methods and that were not abstract non abstract
      for (CtMethod method : getFieldMethod.values()) {
         CtClass clazz = method.getDeclaringClass();
         clazz.setModifiers(clazz.getClassFile2().getAccessFlags() &
                 ~Modifier.ABSTRACT);
      }




      //6. Write the output
      for (CtClass clzz: changedClasses) {

         File outfile = null;
         try {
            if (baseOutputDir != null) {
               outfile = new File(baseOutputDir +
                       File.separator +
                       clzz.getName().replace('.', File.separatorChar) +
                       ".class");
            } else {
               outfile = new File(clzzes.get(clzz)); //note: clzz.getClassFile() may remove the need for this hash map
            }
            fout = new FileOutputStream(outfile);

            CtField marker = new CtField(stringCl, markerName, clzz);
            marker.setModifiers(Modifier.STATIC); //todo: should also be private?
            clzz.addField(marker, CtField.Initializer.constant(markerValue));

            clzz.toBytecode(new DataOutputStream(fout)); //todo: should not necessarily output to the same file
            
            log.info("Javassist rewrote file " + clzz.getClassFile2());
         } catch (IOException e) {
            log.warning("could not write file " + clzz.getClassFile2());
            e.printStackTrace(); //todo: decide what exception to throw
            throw e;
         } catch (CannotCompileException e) {
            log.severe("could not javassist compile " + clzz.getClassFile2());
            e.printStackTrace(); //todo: decide what exception to throw
            throw e;
         } finally {
            if (fin != null) {
               fin.close();
            }
            if (fout != null) {
               fout.close();
            } //todo: leakage possible due to exception thrown before
         }
      }
   }

   private static void descendThrough(File dir, List<File> files) {
      if (dir.isDirectory()) {
         File[] kids = dir.listFiles();
         for (int i = 0; i < kids.length; i++) {
            descendThrough(kids[i], files);
         }
      } else if (dir.isFile()) {
         if (dir.getName().endsWith(".class")) {
            files.add(dir);
         }
      }
   }

   private static void message() {
      System.err.println("Note: This useage has not been thought through carefully. Please improove.");
      System.err.println("Usage: JavassistClassRewriter [ --run main-class [args...]]");
      System.err.println("                              [ --path dir ]");
      System.err.println("                              [ --cp dir files...]");
      System.err.println("[ --run ... ]  option: To run a main class and transform classes on the fly.");
      System.err.println("[ --path dir ] the dir is the root directory to search for classes requiring transformation");
      System.err.println("[ --cp dir --files files...] a classpath and a list of files. The transform files method is more thought out. This needs improoving. Don't rely on it.");
      System.err.println("Warning: the second method will rewrite all the classes beneath directory dir. ");
      System.err.println("If classes outside that path call annotated fields directly during some runtime those calls will not be rewritten.");
      System.err.println("All fields therefore should be part of a package and be package protected.");
      System.err.println("todo: this program could partly verify that, and send a warning message if not.");
   }

   public static Loader getRewriteLoader() throws NotFoundException,
           CannotCompileException,
           ClassNotFoundException {
      // set up class loader with translator
      EditorTranslator xtor = new EditorTranslator(new SommerEditor());
      ClassPool pool = ClassPool.getDefault();
      Loader loader = new Loader(pool);
      loader.delegateLoadingOf("net.java.rdf.annotations."); //we use this here, and also a lot in the code,...
      loader.delegateLoadingOf("net.java.rdf.sommer.");
      loader.addTranslator(pool, xtor);
      return loader;
   }

   public static void logInfo(String info) {
      log.log(Level.INFO, info);
   }

   public static void log2(String info) {
      log.log(Level.SEVERE, info);
   }
}

class EditorTranslator implements Translator {

   static void addSerialiserMethod(CtClass clazz) {
      JavassistClassRewriter.logInfo("in addSerialiserMethod for " +
              clazz.getName());
      String body = "{\n" +
              "java.lang.reflect.Field[] flds = " + clazz.getName() +
              ".class.getDeclaredFields();" +
              "  for (int i =0; $1.continueProcessing() && i < flds.length; i++) {\n" +
              "     java.lang.reflect.Field fld = flds[i];\n" +
              "     java.lang.Object newval;\n" +
              "     try {\n" +
              "        if ($1.isInterestingField(fld)) {\n" +
              "           newval = $1.processField(this, fld, fld.get(this));\n" +
              "           if (newval !=null) {\n" +
              "              fld.set(this,newval);\n" +
              "           }\n" +
              "        }\n" +
              "      } catch (java.lang.IllegalArgumentException ex) {\n" +
              "        java.util.logging.Logger.getLogger(" + clazz.getName() +
              ".class.getName()).log(java.util.logging.Level.SEVERE, \"Illegal Argument: should never happen\", ex);\n" +
              "          ex.printStackTrace(System.err);" +
              "      } catch (java.lang.IllegalAccessException ex) {\n" +
              "        java.util.logging.Logger.getLogger(" + clazz.getName() +
              ".class.getName()).log(java.util.logging.Level.SEVERE, \"Illegal Access: should never happen\", ex);\n" +
              "          ex.printStackTrace(System.err);" +
              "      }\n" +
              "  }\n";
      try {
         CtClass serializerInt = ClassPool.getDefault().get("net.java.rdf.sommer.RdfSerialiser");
         CtClass serializableInt = ClassPool.getDefault().get("net.java.rdf.sommer.RdfSerialisable");
         CtClass[] serialisableArgs = new CtClass[]{serializerInt};
         if (clazz.getSuperclass().subtypeOf(serializableInt)) {
            body += " if ($1.continueProcessing()) super.rdfSerialise($1);\n";
         }
         body += "}\n";
         JavassistClassRewriter.logInfo("added serialiser body=" + body);
         CtMethod mtd = clazz.getDeclaredMethod("rdfSerialise", serialisableArgs);
         mtd.setBody(body);
         clazz.setModifiers(clazz.getModifiers() & ~Modifier.ABSTRACT); //WARNING WARNING: could it still be abstract for other reasons???
         JavassistClassRewriter.markChanged(clazz);
      } catch (CannotCompileException ex) {
         Logger.getLogger(EditorTranslator.class.getName()).
                 log(Level.SEVERE, "cannot compile code ", ex);
      } catch (NotFoundException ex) {
         Logger.getLogger(EditorTranslator.class.getName()).
                 log(Level.SEVERE, "should never happen", ex);
      }
   }

   /**
    * create the name of the getter method for a field on a given class
    */
   static String getterMethodForField(CtField fld, CtClass clas) {
      return "javassistGet" + clas.getName().replace('.', '_') + "_" +
              fld.getName() + "()";
   }

   /**
    * create a setter method on a class for a given field
    */
   static String setterMethodForField(CtClass clazz, CtField fld) {
      return "javassistSet" + clazz.getName().replace('.', '_') + "_" +
              fld.getName();
   }
   private ExprEditor m_editor;

   EditorTranslator(ExprEditor editor) {
      m_editor = editor;
   }

   public void start(ClassPool pool) {
      JavassistClassRewriter.logInfo("start pool");
   }

   public void onLoad(ClassPool pool, String cname) throws NotFoundException,
           CannotCompileException {
      JavassistClassRewriter.logInfo("cname(in Onload)=" + cname);
      CtClass clas = pool.get(cname);
      //    addGetterMethod(clas);
      clas.instrument(m_editor);
   }

   static void addSetSommerMapperMethod(CtClass clazz) throws CannotCompileException, NotFoundException {
      CtField nullhash = CtField.make("protected java.util.HashSet sommerNullFieldHash;", clazz);
      clazz.addField(nullhash);

      CtField fld = CtField.make("protected net.java.rdf.sommer.RewriteMapper sommerRewriteMapper = null;", clazz);
      clazz.addField(fld);

      CtMethod m = CtNewMethod.make(
              "public void setSommerRewriteMapper(net.java.rdf.sommer.RewriteMapper  map) { " +
              "sommerRewriteMapper = map;\n " +
              "if (map==null) { \n" +
              "  sommerNullFieldHash = null; \n" +
              "} else { \n" +
              "  sommerNullFieldHash = new java.util.HashSet(); " +
              "}\n" +
              "}\n",
              clazz);
      clazz.addMethod(m);

//      CtClass[] serialisableArgs = new CtClass[]{rewriteMapperClzz};
//
//     CtMethod mtd = clazz.getDeclaredMethod("setSommerRewriteMapper", serialisableArgs);
//      JavassistClassRewriter.log("setting method body for sommerRewriteMapper in "+clazz.getName());
//     mtd.setBody("{ sommerRewriteMapper = map; }");

      clazz.setModifiers(clazz.getModifiers() & ~Modifier.ABSTRACT); //WARNING WARNING: could it still be abstract for other reasons???

   }

   static void addSetterMethod(CtField fld, CtMethod method) throws NotFoundException {
      CtClass clazz = fld.getDeclaringClass();

      JavassistClassRewriter.logInfo("in addSetterMethod for " +
              method.getName());
      try {
         Object[] annotations = fld.getAnnotations();
         if (SommerEditor.getRdf(annotations) == null) {
            JavassistClassRewriter.logInfo("field=" + fld.getName() +
                    ") has no annotation!");
            throw new NotFoundException("this should have a @rdf annotation or else we should never have gotten here!");
         }
         StringBuffer stMtd = new StringBuffer();
         stMtd.append("{\n").
                 append("if (sommerRewriteMapper==null) { \n   " +
                 "this." + fld.getName() + "= $1;\n").
                 append("} else {\n");
         String line1 = "java.lang.reflect.Field field = " + clazz.getName() +
                 ".class.getDeclaredField( \"" + fld.getName() + "\" );";
         String line2 = null;
         if (SommerEditor.isCollection(fld.getType())) {
            if (!SommerEditor.containsFunctional(annotations)) {
               line2 = format("this." + fld.getName() +
                       "=  ( {0} ) sommerRewriteMapper.replaceVirtualCollection( " +
                       "   this,  " +
                       "  (net.java.rdf.annotations.rdf)field.getAnnotation(net.java.rdf.annotations.rdf.class)," +
                       "   {1}, " +
                       "   $1, " +
                       "   field.getGenericType() ); ",
                       fld.getType().getName(),
                       fld.getName());
            } else {
               line2 = format("this." + fld.getName() +
                       "=  ( {0} ) sommerRewriteMapper.setCollectionField( " +
                       "   this,  " +
                       "  (net.java.rdf.annotations.rdf)field.getAnnotation(net.java.rdf.annotations.rdf.class)," +
                       "   {1}, " +
                       "   field.getGenericType() ); ",
                       fld.getType().getName(),
                       fld.getName());
            }
            stMtd.append("try { \n" + line1 + "\n" + line2 +
                    "\n    } catch ( java.lang.NoSuchFieldException e) { e.printStackTrace(); }");
         } else {
            line2 = format("this." + fld.getName() +
                    "= ( {0} ) sommerRewriteMapper.setField(" + "{1}.class," +
                    "this," +
                    "(net.java.rdf.annotations.rdf)field.getAnnotation(net.java.rdf.annotations.rdf.class)," +
                    "$1); ", fld.getType().getName(), fld.getType().getName());
            line2 += "if ( this. " + fld.getName() +
                    "== null ) { sommerNullFieldHash.add(field); } " +
                    " else { sommerNullFieldHash.remove(field); }";

            stMtd.append("try { \n" + line1 + "\n" + line2 +
                    "\n    } catch ( java.lang.NoSuchFieldException e) { e.printStackTrace(); }");
         }
         stMtd.append("   }\n"); //end of else;
         stMtd.append("}"); //end of method body;
         JavassistClassRewriter.logInfo("to add method: " + stMtd);
         method.setBody(stMtd.toString());
         JavassistClassRewriter.markChanged(clazz);
      } catch (CannotCompileException ex) {
         Logger.getLogger(EditorTranslator.class.getName()).
                 log(Level.SEVERE, null, ex);
      } catch (ClassNotFoundException ex) {
         Logger.getLogger(EditorTranslator.class.getName()).
                 log(Level.SEVERE, null, ex);
      }
   }

   static void addGetterMethod(CtField fld, CtMethod method) throws NotFoundException {
      CtClass clazz = fld.getDeclaringClass();

      JavassistClassRewriter.logInfo("in addgetterMethod for " +
              method.getName());
      try {
         Object[] annotations = fld.getAnnotations();
         if (SommerEditor.getRdf(annotations) == null) {
            JavassistClassRewriter.logInfo("field=" + fld.getName() +
                    ") has no annotation!");
            throw new NotFoundException("this should have a @rdf annotation or else we should never have gotten here!");
         }
         StringBuffer gtMtd = new StringBuffer();
         gtMtd.append("{\n");

         String line0 = "if (sommerRewriteMapper==null) { \n" +
                 "    return " + fld.getName() + ";\n" +
                 "} else if (" + fld.getName() + " == null) {\n";

         String line1 = "java.lang.reflect.Field field = " + clazz.getName() +
                 ".class.getDeclaredField( \"" + fld.getName() + "\" );";
         if (SommerEditor.isCollection(fld.getType())) {
            String line2;
            if (!SommerEditor.containsFunctional(fld.getAnnotations())) {
               line0 = "if (sommerRewriteMapper==null || " + fld.getName() +
                       " instanceof net.java.rdf.sommer.util.RDFCollection ) { \n" +
                       "    return " + fld.getName() + ";\n" +
                       "} else {\n";
               line2 = fld.getName() +
                       " = sommerRewriteMapper.createVirtualCollection(" +
                       " this, " +
                       "(net.java.rdf.annotations.rdf)field.getAnnotation(net.java.rdf.annotations.rdf.class)," +
                       fld.getName() + ", " +
                       "field.getGenericType() );";
            } else {
               line2 = fld.getName() +
                       "= sommerRewriteMapper.getCollectionField(" +
                       " this, " +
                       "(net.java.rdf.annotations.rdf)field.getAnnotation(net.java.rdf.annotations.rdf.class)," +
                       fld.getName() + ", " +
                       "field.getGenericType() );";
            }
            gtMtd.append(line0 +
                    " try { \n" +
                    line1 + "\n" +
                    line2 + "\n" +
                    " } catch ( java.lang.NoSuchFieldException e ) { \n" +
                    "    e.printStackTrace(); \n" +
                    "    return null;\n" +
                    "}");
         } else {
            String line2 = "if (sommerNullFieldHash.contains(field)) { return null; }";

            line2 += fld.getName() + "= (" + fld.getType().getName() +
                    ") sommerRewriteMapper.getField( " +
                    fld.getType().getName() + ".class ," + "this, " +
                    "(net.java.rdf.annotations.rdf)field.getAnnotation(net.java.rdf.annotations.rdf.class)," +
                    fld.getName() + "); ";
            line2 += "if ( this. " + fld.getName() +
                    "== null ) { sommerNullFieldHash.add(field); } " +
                    " else { sommerNullFieldHash.remove(field); }";

            gtMtd.append(line0 +
                    " try { \n" +
                    line1 + "\n" +
                    line2 + "\n" +
                    " } catch ( java.lang.NoSuchFieldException e ) { \n" +
                    "    e.printStackTrace(); \n" +
                    "    return null;\n" +
                    " } ");
         }
         gtMtd.append("   }\n"); //end of else;
         gtMtd.append("return " + fld.getName() + ";\n" +
                 "}"); //end of method body;
         //        JavassistClassRewriter.log("to add method: " + gtMtd);
         method.setBody(gtMtd.toString());
         JavassistClassRewriter.markChanged(clazz);
      } catch (CannotCompileException ex) {
         Logger.getLogger(EditorTranslator.class.getName()).
                 log(Level.SEVERE, null, ex);
      } catch (ClassNotFoundException ex) {
         Logger.getLogger(EditorTranslator.class.getName()).
                 log(Level.SEVERE, null, ex);
      }
   }

   static String setterSignatureForField(CtClass clazz, CtField fld) throws ClassNotFoundException,
           NotFoundException {
      rdf ann = SommerEditor.getRdf(fld.getAnnotations());
      if (ann == null) {
         return null;
      }
      StringBuffer mthdGetter = new StringBuffer();
      //set the same access restrictions as on the field
      int mods = fld.getModifiers();
      if (Modifier.isPrivate(mods)) {
         mthdGetter.append("private ");
      }
      if (Modifier.isProtected(mods)) {
         mthdGetter.append("protected ");
      }
      if (Modifier.isPublic(mods)) {
         mthdGetter.append("public ");
      }
      mthdGetter.append(" void ").append(setterMethodForField(clazz, fld)).append("(").
              append(fld.getType().getName() + " setToVal").append(")");
      JavassistClassRewriter.logInfo("setter sig=" + mthdGetter.toString());
      return mthdGetter.toString(); //could return StringBuffer or take StringBuffer as input for efficiency
   }

   static String getterSignatureForField(CtClass clas, CtField fld) throws NotFoundException,
           ClassNotFoundException {
      rdf ann = SommerEditor.getRdf(fld.getAnnotations());
      if (ann == null) {
         return null;
      }
      StringBuffer mthdGetter = new StringBuffer();
      //set the same access restrictions as on the field
      int mods = fld.getModifiers();
      if (Modifier.isPrivate(mods)) {
         mthdGetter.append("private ");
      }
      if (Modifier.isProtected(mods)) {
         mthdGetter.append("protected ");
      }
      if (Modifier.isPublic(mods)) {
         mthdGetter.append("public ");
      }
      mthdGetter.append(fld.getType().getName() + " ").append(getterMethodForField(fld, clas));

      return mthdGetter.toString(); //could return StringBuffer or take StringBuffer as input for efficiency
   }
}

class SommerEditor extends ExprEditor {

   SommerEditor() {
   }

   /*
   No longer rewrite constructors
   public void edit(NewExpr arg) throws CannotCompileException {
   JavassistClassRewriter.log("in NewExpr");
   try {
   Object[] annotations = arg.getConstructor().getAnnotations();
   if (containsInverseFunctional(annotations)) {
   JavassistClassRewriter.log("found invfunctional New expr for " + arg.getClassName());
   CtClass[] parameterTypes = arg.getConstructor().getParameterTypes();
   Object[][] paramAnnotations = arg.getConstructor().getParameterAnnotations();
   if (parameterTypes.length == paramAnnotations.length) {//each parameter has an annoation
   for (Object[] ann : paramAnnotations) {
   rdf relation = getRdf(ann);
   if (relation == null) return; // not all parameters have rdf annotations
   }
   StringBuffer code = new StringBuffer();
   code.append("Object o = net.java.rdf.sommer.MapperManager.active().cifpObject($type,$sig,$args);");
   code.append("System.out.println(\"returned object=\"+o);");
   code.append("$_=(o==null)?$proceed($$):o;");
   JavassistClassRewriter.log("code=" + code.toString());
   arg.replace(code.toString());
   }
   }
   } catch (ClassNotFoundException e) {
   e.printStackTrace();  //todo: decide what exception to throw
   } catch (NotFoundException e) {
   e.printStackTrace();  //todo: decide what exception to throw
   }
   }
   public void edit(ConstructorCall arg) throws CannotCompileException {
   JavassistClassRewriter.log("in ConstructorCall");
   try {
   Object[] annotations = arg.getConstructor().getAnnotations();
   if (containsInverseFunctional(annotations)) {
   JavassistClassRewriter.log("found invfunctional ConstructorCall for " + arg.getClassName() + "." + arg.getConstructor().getName());
   CtClass[] parameterTypes = arg.getConstructor().getParameterTypes();
   Object[][] paramAnnotations = arg.getConstructor().getParameterAnnotations();
   JavassistClassRewriter.log("paraType.length=" + parameterTypes.length + "; paramAnnotations.length=" + paramAnnotations.length);
   if (parameterTypes.length == paramAnnotations.length) {//each parameter has an annoation
   log.warning("Warning: there is not the same number of parameters as annotations!");
   //                    for (Object[] ann : paramAnnotations) {
   //                        rdf relation = getRdf(ann);
   //                        if (relation == null) return; // not all parameters have rdf annotations
   //                    }
   }//continue anyway. javassist does not seem to be working correctly
   StringBuffer code = new StringBuffer();
   //first pre-allocate a name, if there is one that fits the cifp
   code.append("net.java.rdf.sommer.MapperManager.active().preAllocateCifpName($class,$sig,$args);");
   //this will call the preallocated name if an assignement is made
   code.append("$proceed($$);");
   //if the name was not used above, use it here, and clear the pre-allocated name.
   code.append("net.java.rdf.sommer.MapperManager.active().name(this);");
   JavassistClassRewriter.log("code=" + code.toString());
   arg.replace(code.toString());
   }
   } catch (ClassNotFoundException e) {
   e.printStackTrace();  //todo: decide what exception to throw
   } catch (NotFoundException e) {
   e.printStackTrace();  //todo: decide what exception to throw
   }
   }
    */
   @Override
   public void edit(FieldAccess arg) throws CannotCompileException {
      try {
         Object[] annotations = arg.getField().getAnnotations();
         if (getRdf(annotations) == null) {
            return;
         }
         if (Modifier.isStatic(arg.getField().getModifiers())) {//TODO: change. Add setter and getter methods here too.
            return;
         }

         StringBuilder code = new StringBuilder();

         if (arg.isWriter()) {
            code.append(" $0.").
                    append(EditorTranslator.setterMethodForField(arg.getField().getDeclaringClass(),
                    arg.getField())).append("($1);");
         } else {
            code.append(" $_ = ($r) $0.").
                    append(EditorTranslator.getterMethodForField(arg.getField(),
                    arg.getField().getDeclaringClass())).
                    append("; ");
         }

         JavassistClassRewriter.logInfo("to " +
                 arg.getEnclosingClass().getName() + " will add " + code);
         arg.replace(code.toString());
      } catch (ClassNotFoundException e) {
         e.printStackTrace(); //todo: decide what exception to throw
         throw new CannotCompileException(e);
      } catch (NotFoundException e) {
//         e.printStackTrace(); //todo: decide what exception to throw
         JavassistClassRewriter.logInfo("could not access a class reltated to field " +
                 arg.getFieldName() + " in class " + arg.getClassName() +
                 ". Skipping this rewrite.");
         return;
      } catch (CannotCompileException e) {
         JavassistClassRewriter.log.log(Level.SEVERE, "Could Not rewrite byte code!!!", e);
         throw e;
      }
   }

   static boolean containsFunctional(Object[] annotations) {
      for (Object o : annotations) {
         if (o instanceof functional) {
            return true;
         }
      }
      return false;
   }

   /* No longer needed
   private boolean containsInverseFunctional(Object[] annotations) {
   JavassistClassRewriter.log("in containsInverseFunctional:" + annotations.length);
   for (Object o : annotations) {
   JavassistClassRewriter.log("annotation:" + o);
   if (o instanceof inverseFunctional) return true;
   }
   return false;
   }
    */
   static boolean isCollection(CtClass clazz) {
      return "java.util.Collection".equals(clazz.getName());
   }

   /**
    * @param annotations
    * @return return the rdf annotation (should never be more than one)
    */
   static rdf getRdf(Object[] annotations) {
      for (Object o : annotations) {
         if (o instanceof rdf) {
            return (rdf) o;
         }
      }
      return null;
   }
}