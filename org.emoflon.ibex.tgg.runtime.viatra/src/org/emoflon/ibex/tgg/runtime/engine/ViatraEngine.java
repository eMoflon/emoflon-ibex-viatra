package org.emoflon.ibex.tgg.runtime.engine;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.emoflon.ibex.tgg.operational.OperationalStrategy;
import org.emoflon.ibex.tgg.operational.PatternMatchingEngine;
import org.emoflon.ibex.tgg.operational.strategies.cc.CC;
import org.emoflon.ibex.tgg.operational.strategies.gen.MODELGEN;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;
import org.emoflon.ibex.tgg.operational.util.IbexOptions;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import language.TGG;

public class ViatraEngine implements PatternMatchingEngine {

	private OperationalStrategy strategy;
	
	private THashMap<IPatternMatch, ViatraMatch> matches = new THashMap<>();
	
	private THashMap<ViatraMatch, String> addedOperationalMatches = new THashMap<>();
	
	private THashSet<ViatraMatch> removedOperationalMatches = new THashSet<>();
	
	private THashSet<ViatraMatch> brokenConsistencyMatches = new THashSet<>();
	
	private ResourceSet rs = new ResourceSetImpl();
	
	@Override
	public void registerInternalMetamodels() {
		
	}

	@Override
	public ResourceSet createAndPrepareResourceSet(String workspacePath) {
		return rs;
	}
	
	public ResourceSet getResourceSet(){
		return rs;
	}

	@Override
	public void updateMatches() {
		addedOperationalMatches.keySet().forEach(m -> strategy.addOperationalRuleMatch(addedOperationalMatches.get(m), m));
		removedOperationalMatches.forEach(strategy::removeOperationalRuleMatch);
		brokenConsistencyMatches.forEach(strategy::addBrokenMatch);
		
		addedOperationalMatches = new THashMap<>();
		removedOperationalMatches = new THashSet<>();
		brokenConsistencyMatches = new THashSet<>();
	}

	@Override
	public void terminate() {
		
	}

	@Override
	public void initialise(ResourceSet rs, OperationalStrategy operationalStrategy, IbexOptions options) {
		this.strategy = operationalStrategy;
	}
	
	private ViatraMatch getViatraMatch(IPatternMatch match){
		if(!matches.containsKey(match))
			matches.put(match, new ViatraMatch(match));
		return matches.get(match);
	}
	
	// Methods for the communication with Viatra-Transformation
	
	public void addOperationalRuleMatch(String ruleName, IPatternMatch match){
		addedOperationalMatches.put(getViatraMatch(match), ruleName);
	}
	
	public void removeOperationalRuleMatch(IPatternMatch match){
		removedOperationalMatches.add(getViatraMatch(match));
	}
	
	public void addBrokenMatch(IPatternMatch match){
		brokenConsistencyMatches.add(getViatraMatch(match));
	}
	
	public OperationMode getMode(){
		if(strategy instanceof SYNC)
			return OperationMode.SYNCH;
		if(strategy instanceof CC)
			return OperationMode.CC;
		if(strategy instanceof MODELGEN)
			return OperationMode.MODELGEN;
		
		throw new RuntimeException("Operation mode unknown");
	}

	
}
