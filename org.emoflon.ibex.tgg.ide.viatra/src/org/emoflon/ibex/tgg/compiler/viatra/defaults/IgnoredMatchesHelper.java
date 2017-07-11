package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.util.Collection;
import java.util.stream.Collectors;

import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;

import language.BindingType;
import language.DomainType;
import language.TGG;
import language.TGGRule;

public class IgnoredMatchesHelper {

	public static boolean ignoreMatches(TGGRule rule, String patternSuffix) {
		if (rule.isAbstract())
			return true;

		if (patternSuffix.equals(PatternSuffixes.FWD))
			return !anyGreenElement(rule, DomainType.SRC);
		
		if(patternSuffix.equals(PatternSuffixes.BWD))
			return !anyGreenElement(rule, DomainType.TRG);
		
		return false;
	}
	
	private static boolean anyGreenElement(TGGRule rule, DomainType domainType){
		return rule.getNodes().stream()
				.anyMatch(n -> n.getDomainType() == DomainType.SRC && n.getBindingType() == BindingType.CREATE)
				|| rule.getEdges().stream().anyMatch(
						e -> e.getDomainType() == DomainType.SRC && e.getBindingType() == BindingType.CREATE);
	}
	
	
	public static Collection<TGGRule> relevantRules(TGG tgg, String patternSuffix){
		return tgg.getRules().stream().filter(r -> !ignoreMatches(r, patternSuffix)).collect(Collectors.toSet());
	}
}
