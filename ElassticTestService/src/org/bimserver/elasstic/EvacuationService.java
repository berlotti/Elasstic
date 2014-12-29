package org.bimserver.elasstic;

public class EvacuationService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		return "evacuation.csv";
	}

	@Override
	public String getDescription() {
		return "Generates a fake evacuation simulation result";
	}

	@Override
	public String getDefaultName() {
		return "Evacuation Simulator";
	}
}
