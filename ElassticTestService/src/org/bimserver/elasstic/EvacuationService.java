package org.bimserver.elasstic;

public class EvacuationService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		return "evacuation.json";
	}

	@Override
	public String getDefaultName() {
		return "Evacuation Simulator";
	}
}
