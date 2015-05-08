package org.bimserver.elasstic;

public class Space {
	private String department;
	private String classification;
	private String guid;
	private String name;
	private double area;
	private String function;
	private String filename;
	private float lowestLevel;
	
	public Space() {
		
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getArea() {
		return area;
	}

	public void setArea(double area) {
		this.area = area;
	}

	public void setFunction(String function) {
		this.function = function;
	}
	
	public String getFunction() {
		return function;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getFilename() {
		return filename;
	}

	public float getLowestLevel() {
		return lowestLevel;
	}
	
	public void setLowestLevel(float lowestLevel) {
		this.lowestLevel = lowestLevel;
	}
}
