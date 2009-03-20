package net.java.rdf.sommer;

import net.java.rdf.annotations.rdf;

/**
 * @author Bruno Harbulot.
 */
@rdf(Vehicle.v + "Car")
public class Car extends Vehicle {
	@rdf(v + "colour")
	private String colour;

	public String getColour() {
		return this.colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}
}
