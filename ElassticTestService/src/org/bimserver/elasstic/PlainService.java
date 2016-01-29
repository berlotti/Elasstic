package org.bimserver.elasstic;

public class PlainService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		return "plain.csv";
	}

	@Override
	public String getDefaultName() {
		return "Plain Simulator";
	}
}