package org.emoflon.ibex.gt.viatra.ide.codegen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.emoflon.ibex.gt.codegen.GTEngineExtension;

public class GTViatraExtension implements GTEngineExtension{

	@Override
	public Set<String> getDependencies() {
		return new HashSet<String>(Arrays.asList("org.emoflon.ibex.gt.viatra"));
	}

	@Override
	public Set<String> getImports() {
		return new HashSet<String>(Arrays.asList("org.emoflon.ibex.gt.viatra.runtime.ViatraGTEngine"));
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
