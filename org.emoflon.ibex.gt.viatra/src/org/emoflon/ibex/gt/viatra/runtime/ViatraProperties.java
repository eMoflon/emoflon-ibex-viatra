package org.emoflon.ibex.gt.viatra.runtime;

import org.emoflon.ibex.common.operational.IPatternInterpreterProperties;

public class ViatraProperties implements IPatternInterpreterProperties {
	@Override
	public boolean supports_dynamic_emf() {
		return true;
	}
	
	@Override
	public boolean supports_arithmetic_attr_constraints() {
		return false;
	}
	
	@Override
	public boolean supports_count_matches() {
		return false;
	}
	
	@Override
	public boolean supports_transitive_closure() {
		return false;
	}
	
	@Override
	public boolean uses_reactive_matching() {
		return true;
	}
	
	@Override
	public boolean uses_synchroneous_matching() {
		return true;
	}
}
