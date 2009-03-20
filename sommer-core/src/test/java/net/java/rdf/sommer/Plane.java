package net.java.rdf.sommer;

import net.java.rdf.annotations.rdf;

/**
 * @author Bruno Harbulot.
 */
@rdf(Vehicle.v + "Plane")
public class Plane extends Vehicle {
	@rdf(Vehicle.v + "engineType")
	private String engineType;

	public String getEngineType() {
		return this.engineType;
	}

	public void setEngineType(String engineType) {
		this.engineType = engineType;
	}
}
