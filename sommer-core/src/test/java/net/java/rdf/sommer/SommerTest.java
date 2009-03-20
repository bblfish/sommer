package net.java.rdf.sommer;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

import net.java.rdf.sommer.Mapper;
import net.java.rdf.sommer.MapperManager;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Bruno Harbulot (Bruno.Harbulot@manchester.ac.uk)
 * 
 */
public class SommerTest {
	public final static String TEST1_NS = "http://sommer.dev.java.net/unit-tests/test1/#";
	public final static String BASE_REF = "http://sommer.dev.java.net/unit-tests/test1/things/";

	public final static URI USER1_ID;
	public final static URI USER2_ID;

	public final static URI CAR1_ID;
	public final static String CAR1_COLOUR = "blue";

	public final static URI CAR2_ID;
	public final static String CAR2_COLOUR = "red";

	public final static URI PLANE1_ID;
	public final static String PLANE1_ENGINETYPE = "propellers";

	static {
		try {
			CAR1_ID = new URI(BASE_REF + "car01");
			CAR2_ID = new URI(BASE_REF + "car02");
			PLANE1_ID = new URI(BASE_REF + "plane01");
			USER1_ID = new URI(BASE_REF + "user1");
			USER2_ID = new URI(BASE_REF + "user2");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testLoadAllCars() throws Exception {
		Mapper mapper = MapperManager.getMapperForGraph(TEST1_NS);
		InputStreamReader rdfReader = new InputStreamReader(SommerTest.class
				.getResourceAsStream("test1.n3"));
		mapper.importFrom(rdfReader, BASE_REF, "text/rdf+n3");
		rdfReader.close();

		mapper.preload(Vehicle.class);
		mapper.preload(Car.class);
		mapper.preload(Plane.class);

		Collection<Car> cars = mapper.getAllObjectsOfType(Car.class);

		assertEquals("There should be two cars? ", 2, cars.size());

		Iterator<Car> carIter = cars.iterator();
		Car car1 = carIter.next();
		Car car2 = carIter.next();

		if (car2.getId().equals(CAR1_ID)) {
			Car temp = car2;
			car2 = car1;
			car1 = temp;
		}

		assertEquals("car01 ID matches? ", CAR1_ID, car1.getId());
		assertEquals("car01 owner matches? ", USER1_ID, car1.getOwner());
		assertEquals("car01 colour matches? ", CAR1_COLOUR, car1.getColour());
		assertEquals("car02 ID matches? ", CAR2_ID, car2.getId());
		assertEquals("car02 owner matches? ", USER2_ID, car2.getOwner());
		assertEquals("car02 colour matches? ", CAR2_COLOUR, car2.getColour());
	}

	@Test
	public void testCarOwnedByUser1() throws Exception {
		Mapper mapper = MapperManager.getMapperForGraph(TEST1_NS);
		InputStreamReader rdfReader = new InputStreamReader(SommerTest.class
				.getResourceAsStream("test1.n3"));
		mapper.importFrom(rdfReader, BASE_REF, "text/rdf+n3");
		rdfReader.close();

		mapper.preload(Vehicle.class);
		mapper.preload(Car.class);
		mapper.preload(Plane.class);

		Car example = new Car();
		example.setOwner(USER1_ID);

		Collection<Car> cars = mapper.queryByExample(example, Car.class);

		assertEquals("User1 owns only one car? ", 1, cars.size());

		Car car = cars.iterator().next();

		assertEquals("car01 ID matches? ", CAR1_ID, car.getId());
		assertEquals("car01 owner matches? ", USER1_ID, car.getOwner());
		assertEquals("car01 colour matches? ", CAR1_COLOUR, car.getColour());
	}

	@Test
	public void testVehiclesOwnedByUser1() throws Exception {
		Mapper mapper = MapperManager.getMapperForGraph(TEST1_NS);
		InputStreamReader rdfReader = new InputStreamReader(SommerTest.class
				.getResourceAsStream("test1.n3"));
		mapper.importFrom(rdfReader, BASE_REF, "text/rdf+n3");
		rdfReader.close();

		mapper.preload(Vehicle.class);
		mapper.preload(Car.class);
		mapper.preload(Plane.class);

		Car example = new Car();
		example.setOwner(USER1_ID);

		Collection<Vehicle> vehicles = mapper.queryByExample(example,
				Vehicle.class);

		assertEquals("User1 owns two vehicles? ", 2, vehicles.size());

		Car car = null;
		Plane plane = null;
		for (Vehicle vehicle : vehicles) {
			if (vehicle instanceof Car) {
				car = (Car) vehicle;
			}
			if (vehicle instanceof Plane) {
				plane = (Plane) vehicle;
			}
		}

		assertEquals("car01 ID matches? ", CAR1_ID, car.getId());
		assertEquals("car01 owner matches? ", USER1_ID, car.getOwner());
		assertEquals("car01 colour matches? ", CAR1_COLOUR, car.getColour());

		assertEquals("plane01 ID matches? ", PLANE1_ID, plane.getId());
		assertEquals("plane01 owner matches? ", USER1_ID, plane.getOwner());
		assertEquals("plane01 engine type matches? ", PLANE1_ENGINETYPE, plane
				.getEngineType());
	}
}
