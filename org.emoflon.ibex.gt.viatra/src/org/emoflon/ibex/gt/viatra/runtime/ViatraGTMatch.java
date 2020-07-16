package org.emoflon.ibex.gt.viatra.runtime;

import java.util.List;

import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.emoflon.ibex.common.operational.SimpleMatch;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.operational.matches.TGGMatchParameterOrderProvider;

public class ViatraGTMatch extends SimpleMatch {
	/**
	 * Creates a new ViatraGTMatch with the given frame and pattern.
	 * 
	 * @param match the Viatra match
	 */
	public ViatraGTMatch(final IPatternMatch match) {
		super(match.patternName());
		if (TGGMatchParameterOrderProvider.isInitialized())
			initWithOrderedParams(match);
		else
			init(match);
	}

	private void init(final IPatternMatch match) {
		for (String parameter : match.parameterNames()) {
			put(parameter, match.get(parameter));
		}
	}

	/**
	 * Inserts parameters in a predefined order for determined match hashing.
	 * 
	 * @param match
	 */
	private void initWithOrderedParams(final IPatternMatch match) {
		List<String> params = null;
		if (match.patternName() != null)
			params = TGGMatchParameterOrderProvider.getParams(PatternSuffixes.removeSuffix(match.patternName()));
		if (params != null) {
			for (String p : params) {
				Object v = match.get(p);
				if (v != null)
					put(p, v);
			}
		}
	}
}
