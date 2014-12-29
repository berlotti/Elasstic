package org.bimserver.elasstic;

public class EarthQuakeService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		return "earthquake.csv";
	}

	@Override
	public String getDescription() {
		return "Generates a fake earthquake simulation result";
	}

	@Override
	public String getDefaultName() {
		return "EarthQuake Simulator";
	}
}
