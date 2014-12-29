package org.bimserver.elasstic;

public class ExplosionService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		return "explosion.csv";
	}

	@Override
	public String getDescription() {
		return "Generates a fake explosion simulation result";
	}

	@Override
	public String getDefaultName() {
		return "Explosion Simulator";
	}
}
