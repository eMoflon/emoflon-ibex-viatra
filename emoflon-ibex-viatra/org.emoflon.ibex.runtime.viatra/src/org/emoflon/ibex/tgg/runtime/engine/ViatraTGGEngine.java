package org.emoflon.ibex.tgg.runtime.engine;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.viatra.query.runtime.api.IMatchUpdateListener;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.common.operational.IMatchObserver;
import org.emoflon.ibex.tgg.compiler.transformations.patterns.ContextPatternTransformation;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.ibex.tgg.operational.strategies.modules.MatchDistributor;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;

import IBeXLanguage.IBeXPatternSet;

public class ViatraTGGEngine extends ViatraGTEngine implements IBlackInterpreter {
	
	
	private ArrayList<IMatch> notifyMatches;
	/**
	 * Creates a new DemoclesTGGEngine.
	 */
	public ViatraTGGEngine() {
		super();
		base = URI.createPlatformResourceURI("/", true);
	}
	
	/**
	 * The base uri
	 */
	protected URI base;
	private IBeXPatternSet ibexPatterns;
	IbexOptions options;
	private IbexExecutable executable;
	
	/**
	 * please refer to {@link HiPETGGEngine}
	 */
	@Override
	public void initialise(IbexExecutable executable, IbexOptions options, Registry registry,
			IMatchObserver matchObserver) {
		super.initialise(registry, matchObserver);
		
		this.options = options;
		this.executable = executable;
		ContextPatternTransformation compiler = new ContextPatternTransformation(options, (MatchDistributor) matchObserver);
		ibexPatterns = compiler.transform();
		initPatterns(ibexPatterns);
	}
	
	
	protected Resource loadResource(String path) throws Exception {
		Resource modelResource = resourceSet.getResource(URI.createURI(path).resolve(base), true);
		if(modelResource == null)
			throw new IOException("File did not contain a vaild model.");
		return modelResource;
	}
	
	@Override
	protected void setMatchers() {
		matchers = new ArrayList<ViatraQueryMatcher<?>>();
		notifyMatches = new ArrayList<IMatch>();

		specifications.forEach(spec -> {
			matchers.add(engine.getMatcher(spec));
		});
		System.out.println("SetMatchers");
		for(ViatraQueryMatcher<?> matcher : matchers) {
			//to get a notification when a IPatternMatch appearance i.e. disappearance ever matcher needs a IMatchUpdateListener
			//this listener will then add i.e. remove a iMatch from the IMatchObserver
			IMatchUpdateListener<IPatternMatch> listener = new IMatchUpdateListener<IPatternMatch>() {
				
				@Override
				public void notifyDisappearance(IPatternMatch match) {
					IMatch iMatch = createMatch(match);
//					System.out.println("Match DIS: " +  iMatch);
					if(executable instanceof SYNC) {
						if(iMatch instanceof ViatraTGGMatch)
							((ViatraTGGMatch) iMatch).setDisapperance(true);
						notifyMatches.add(iMatch);
					}
					else 
						app.removeMatch(iMatch);
					System.out.println("DIS");
				}
				
				@Override
				public void notifyAppearance(IPatternMatch match) {
					IMatch iMatch = createMatch(match);
//					System.out.println("Match APP: " +  iMatch);
					if(executable instanceof SYNC) {
						if(iMatch instanceof ViatraTGGMatch)
							((ViatraTGGMatch) iMatch).setDisapperance(false);
						notifyMatches.add(createMatch(match));
					}
					else 
						app.addMatch(iMatch);
					System.out.println("APP");
				}
			};
			engine.addMatchUpdateListener(matcher, listener, true);
		}
	}
	
	@Override
	public void updateMatches() {
		System.out.println("UpdateMatches");
		//to trigger the IMatchUpdateListener every matcher needs to be called
		if(!(executable instanceof SYNC))
		for(ViatraQueryMatcher<?> matcher : matchers) {
			matcher.getAllMatches();
		}
		
		for(IMatch match : notifyMatches) {
			if(match instanceof ViatraTGGMatch) {
				if(((ViatraTGGMatch) match).getDisapperance()) {
					app.removeMatch(match);
					System.out.println("Match DIS: " +  match);
				}
				else {
					app.addMatch(match);
					System.out.println("Match APP: " +  match);
				}
			}
		}
		notifyMatches.clear();
		app.notifySubscriptions();
	}
	@Override
	public void terminate() {
		if(executable instanceof SYNC)
			updateMatches();
		Logger.getRootLogger().setLevel(Level.ALL);
		engine.wipe();
		engine.dispose();
	}
	
	@Override
	protected IMatch createMatch(final IPatternMatch match) {
		return new ViatraTGGMatch(match);
	}

}
