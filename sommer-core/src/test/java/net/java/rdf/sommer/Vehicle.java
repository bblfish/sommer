package net.java.rdf.sommer;

import java.net.URI;

import net.java.rdf.annotations.rdf;

/**
 * @author Bruno Harbulot.
 */
@rdf(Vehicle.v + "Vehicle")
public class Vehicle {
	public static final String v = "http://sommer.dev.java.net/unit-tests/test1/#";

	@rdf(rdf.sameAs)
	private URI id;

	@rdf(v + "owner")
	private URI owner;

	public URI getId() {
		return this.id;
	}

	public void setId(URI id) {
		this.id = id;
	}

	public URI getOwner() {
		return this.owner;
	}

	public void setOwner(URI owner) {
		this.owner = owner;
	}
}
