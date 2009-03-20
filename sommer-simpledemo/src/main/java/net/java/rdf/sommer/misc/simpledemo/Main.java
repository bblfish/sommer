package net.java.rdf.sommer.misc.simpledemo;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import net.java.rdf.sommer.Mapper;
import net.java.rdf.sommer.MapperManager;
import net.java.rdf.sommer.util.GraphUpdateException;
import net.java.rdf.sommer.util.ParseException;

/** 
 * @author Stephanie Stroka
 * (stepahnie.stroka@salzburgresearch.at)
 */
public class Main {
        static final String ns = "http://simple.test/";

	@SuppressWarnings("deprecation")
	public Mapper addData(String file) {
		Mapper map = MapperManager.getMapperForGraph(ns);
		try {
			InputStream resource = ClassLoader.getSystemResourceAsStream(file);
						
			map.importFrom(new InputStreamReader(resource,"UTF-8"), Person.foaf+"Person", "application/rdf+xml");
			return map;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GraphUpdateException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) {
		Mapper mapper;
		try {
			Main m = new Main();
			System.out.println( "------Mapper-Output---------");
			mapper = m.addData("Test.rdf");
			mapper.preload( Person.class );
			mapper.output(System.out);

			System.out.println( "-----------Init--------------");
			Collection<Person> people_init = mapper.getAllObjectsOfType(Person.class);
			Iterator<Person> it_init = people_init.iterator();
			System.out.println("These people are stored:");
			while(it_init.hasNext()) {
				System.out.println("    " + it_init.next().getFirstName());
			}

			System.out.println( "----------Update-------------");
			Person p1 = new Person();
			p1.setFirstName("steffi");
			p1.setID(new URI(ns+"Steffi"));
			System.out.println("adding person: " + p1.getFirstName());
			Person p2 = new Person();
			p2.setFirstName("henry");
			p2.setID(new URI(ns+"Henry"));
			System.out.println("adding person: " + p2.getFirstName());
			mapper.addObjects(p1,p2);
			Collection<Person> people = mapper.getAllObjectsOfType(Person.class);
			Iterator<Person> it = people.iterator();
			System.out.println("These people are stored:");
			while(it.hasNext()) {
				System.out.println("    " + it.next().getFirstName());
			}

			System.out.println( "------Mapper-Output---------");
			mapper.output(System.out);
			mapper.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
