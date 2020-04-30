package org.emoflon.ibex.tgg.runtime.engine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.emoflon.ibex.gt.codegen.GTEngineExtension;

public class GTViatraExtension implements GTEngineExtension{

	@Override
	public Set<String> getDependencies() {
		return new HashSet<String>(Arrays.asList("GameGraphTransformation"));
	}

	@Override
	public Set<String> getImports() {
		return new HashSet<String>(Arrays.asList("viatra.pattern.transformation.ViatraGTEngine"));
	}

	@Override
	public String getEngineName() {
		return "Viatra";
	}

	@Override
	public String getEngineClassName() {
		return "ViatraGTEngine";
	}

}
