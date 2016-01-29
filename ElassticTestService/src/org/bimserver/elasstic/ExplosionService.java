package org.bimserver.elasstic;

import java.util.Random;

public class ExplosionService extends ElassticTestService {

	@Override
	public String getCsvFileName() {
		int nextInt = new Random().nextInt(3);
		if (nextInt == 0) {
			return "small.json";
		} else if (nextInt == 1) {
			return "medium.json";
		} else if (nextInt == 2) {
			return "large.json";
		}
		return "small.json";
	}

	@Override
	public String getDefaultName() {
		return "Explosion Simulator";
	}
}
