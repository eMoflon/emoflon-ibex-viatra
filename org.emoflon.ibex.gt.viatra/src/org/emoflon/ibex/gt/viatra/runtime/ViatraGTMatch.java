package org.emoflon.ibex.gt.viatra.runtime;

import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.emoflon.ibex.common.operational.SimpleMatch;

public class ViatraGTMatch extends SimpleMatch {
	/**
	 * Creates a new ViatraGTMatch with the given frame and pattern.
	 * 
	 * @param match
	 *            the Viatra match 
	 */
	public ViatraGTMatch(final IPatternMatch match) {
		super(match.patternName());
		for (String parameter : match.parameterNames()) {
			put(parameter, match.get(parameter));
		}
	}
}
