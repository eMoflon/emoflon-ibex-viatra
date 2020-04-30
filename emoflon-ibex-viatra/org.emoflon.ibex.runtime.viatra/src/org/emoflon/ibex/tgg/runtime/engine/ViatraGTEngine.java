package org.emoflon.ibex.tgg.runtime.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import org.eclipse.viatra.query.patternlanguage.emf.EMFPatternLanguageStandaloneSetup;
import org.eclipse.viatra.query.patternlanguage.emf.specification.SpecificationBuilder;
import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.IMatchUpdateListener;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.base.api.BaseIndexOptions;
import org.eclipse.viatra.query.runtime.emf.EMFScope;

import org.emoflon.ibex.common.operational.IContextPatternInterpreter;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.common.operational.IMatchObserver;
import IBeXLanguage.IBeXPatternSet;

public class ViatraGTEngine implements IContextPatternInterpreter {
	protected static final Logger logger = Logger.getLogger(ViatraGTEngine.class);
	
	/**
	 * The registry.
	 */
	protected Registry registry;
	
	protected SpecificationBuilder builder;
	
	protected AdvancedViatraQueryEngine engine;
	
	protected IBeXPatternSet ibexPatternSet;
	
	protected ArrayList<ViatraQueryMatcher<?>> matchers;
	
	protected Set<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>> specifications;
	
	/**
	 * The resourceset
	 */
	protected ResourceSet resourceSet;
	
	/**
	 * The match observer.
	 */
	protected IMatchObserver app;
	
	/**
	 * The path for debugging output.
	 */
	protected Optional<String> debugPath = Optional.empty();
	
	
	public ViatraGTEngine() {
		this.specifications = new HashSet<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>>();
	}
	
	@Override
	public ResourceSet createAndPrepareResourceSet(final String workspacePath) {
		resourceSet = createAndPrepareResourceSet_internal(workspacePath);
		return createAndPrepareResourceSet_internal(workspacePath);
	}
	
	private ResourceSet createAndPrepareResourceSet_internal(final String workspacePath) {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		try {
			rs.getURIConverter().getURIMap().put(URI.createPlatformResourceURI("/", true), URI.createFileURI(new File(workspacePath).getCanonicalPath() + File.separator));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return rs;
	}
	
	@Override
	public void initialise(Registry registry, IMatchObserver matchObserver) {
		//needs to be called only once per Java application otherwise strange behavior form the ViatraQueryEngine can be noticed
		new EMFPatternLanguageStandaloneSetup().createStandaloneInjector();
		this.builder = new SpecificationBuilder();
		this.registry = registry;
		this.app = matchObserver;
	}
	
	@Override
	public void initPatterns(IBeXPatternSet ibexPatternSet){
		this.ibexPatternSet = ibexPatternSet;
		//needs a builder to create correct IQuerySpecifications, the builder can not be created inside the IBeXToViatraPatternTransformation class
		//because of the EMFPatternLanguageStandaloneSetup().createStandaloneInjector() call
		IBeXToViatraPatternTransformation transformation = new IBeXToViatraPatternTransformation(builder);
		try {
			specifications.addAll(transformation.transformIBeXToViatra(ibexPatternSet));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public void monitor(final ResourceSet resourceSet) {
		for (Resource r : resourceSet.getResources()) {
			if ("ecore".equals(r.getURI().fileExtension())) {
				logger.warn("Are you sure your resourceSet should contain a resource for a metamodel?: " + r.getURI());
				logger.warn("You should probably initialise this metamodel and make sure your "
						+ "resourceSet only contains models to be monitored by the pattern matcher.");
			}
		}
		EcoreUtil.resolveAll(resourceSet);

		EcoreUtil.UnresolvedProxyCrossReferencer//
				.find(resourceSet)//
				.forEach((eob, settings) -> {
					logger.error("Problems resolving: " + eob);
					settings.forEach(setting -> {
						EObject o = setting.getEObject();
						EStructuralFeature f = setting.getEStructuralFeature();

						try {
							if (f.isMany()) {
								((Collection<Object>) o.eGet(f)).remove(eob);
								logger.warn(
										"Removed proxy from collection.  You should probably check why this cannot be resolved!");
							} else {
								o.eSet(f, null);
								logger.warn(
										"Removed proxy (set to null).  You should probably check why this cannot be resolved!");
							}
						} catch (Exception e) {
							logger.warn("Unable to remove proxy: " + e);
						}
					});
				});
		try {	
			BaseIndexOptions options = new BaseIndexOptions().withDanglingFreeAssumption(false).withDynamicEMFMode(true);
			engine = AdvancedViatraQueryEngine.createUnmanagedEngine(new EMFScope(resourceSet, options));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		setMatchers();
	}
	
	/**
	 * The ViatraQueryEngine returns for each IQuerySpecification a ViatraQueryMatcher
	 * This matcher contains all matches from a IQuerySpecification which is basically a Pattern
	 */
	protected void setMatchers() {
		matchers = new ArrayList<ViatraQueryMatcher<?>>();
		specifications.forEach(spec -> {
			matchers.add(engine.getMatcher(spec));
		});
//		System.out.println("SetMatchers");
		for(ViatraQueryMatcher<?> matcher : matchers) {
			//to get a notification when a IPatternMatch appearance i.e. disappearance ever matcher needs a IMatchUpdateListener
			//this listener will then add i.e. remove a iMatch from the IMatchObserver
			IMatchUpdateListener<IPatternMatch> listener = new IMatchUpdateListener<IPatternMatch>() {
				
				@Override
				public void notifyDisappearance(IPatternMatch match) {
					IMatch iMatch = createMatch(match);
//					System.out.println("Match DIS: " +  iMatch);
					app.removeMatch(iMatch);
				}
				
				@Override
				public void notifyAppearance(IPatternMatch match) {
					IMatch iMatch = createMatch(match);
//					System.out.println("Match APP: " +  iMatch);
					app.addMatch(iMatch);
				}
			};
			engine.addMatchUpdateListener(matcher, listener, true);
		}
	}

	@Override
	public void updateMatches() {
		//to trigger the IMatchUpdateListener every matcher needs to be called
		//not the best solution because the method updateMatches is called very often
		for(ViatraQueryMatcher<?> matcher : matchers) {
			//fastest call to trigger the IMatchUpdateListener
			matcher.countMatches();
		}
		app.notifySubscriptions();
	}

	@Override
	public void terminate() {
		engine.wipe();
		engine.dispose();
	}

	@Override
	public void setDebugPath(String debugPath) {
		this.debugPath = Optional.of(debugPath);
		}
	
	protected IMatch createMatch(final IPatternMatch match) {
		return new ViatraGTMatch(match);
	}
}
