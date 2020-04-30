package org.emoflon.ibex.tgg.runtime.engine;

import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.emoflon.ibex.tgg.compiler.patterns.PatternUtil;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.matches.SimpleTGGMatch;

public class ViatraTGGMatch extends ViatraGTMatch implements ITGGMatch {
	
	boolean disapperance;
	/**
	 * Creates a new ViatraTGGMatch with the given match.
	 * 
	 * @param match
	 *            the HiPE match
	 */
	public ViatraTGGMatch(IPatternMatch match) {
		super(match);
	}

	@Override
	public ITGGMatch copy() {
		SimpleTGGMatch copy = new SimpleTGGMatch(getPatternName());
		getParameterNames().forEach(n -> copy.put(n, get(n)));
		return copy;
	}

	@Override
	public PatternType getType() {
		return PatternUtil.resolve(getPatternName());
	}
	
	public void setDisapperance(boolean value) {
		disapperance = value;
	}
	
	public boolean getDisapperance() {
		return disapperance;
	}

}
