package org.bimserver.elasstic;

public class EarthQuakeService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		return "earthquake.csv";
	}

	@Override
	public String getDefaultName() {
		return "EarthQuake Simulator";
	}
}
