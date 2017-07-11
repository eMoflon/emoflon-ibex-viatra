package org.emoflon.ibex.tgg.runtime.engine;

import java.util.Collection;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.emoflon.ibex.tgg.operational.util.IMatch;

public class ViatraMatch implements IMatch {

	private IPatternMatch match;
	public ViatraMatch(IPatternMatch match){
		this.match= match;
	}
	
	@Override
	public EObject get(String name) {
		return (EObject) match.get(name);
	}

	@Override
	public Collection<String> parameterNames() {
		return match.parameterNames();
	}

	@Override
	public String patternName() {
		return match.patternName();
	}

}
