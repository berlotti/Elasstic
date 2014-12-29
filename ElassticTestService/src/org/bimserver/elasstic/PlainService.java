package org.bimserver.elasstic;

public class PlainService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		return "plain.csv";
	}

	@Override
	public String getDescription() {
		return "Generates a fake plain simulation result";
	}

	@Override
	public String getDefaultName() {
		return "Plain Simulator";
	}
}