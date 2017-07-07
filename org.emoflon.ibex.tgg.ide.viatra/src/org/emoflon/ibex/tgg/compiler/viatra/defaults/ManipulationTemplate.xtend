package org.emoflon.ibex.tgg.compiler.viatra.defaults

import language.TGG
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes
import language.TGGRule

class ManipulationTemplate {

	def getManipulationCode(TGG tgg, String projectName) {

		val suffixes = #{PatternSuffixes.CONSISTENCY, PatternSuffixes.FWD, PatternSuffixes.BWD, PatternSuffixes.MODELGEN,
			PatternSuffixes.CC}

		return '''
			
				package org.emoflon.ibex.tgg.run
				
				import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine
				import org.eclipse.viatra.query.runtime.emf.EMFScope
				import org.eclipse.viatra.transformation.runtime.emf.rules.eventdriven.EventDrivenTransformationRuleFactory
				import org.eclipse.viatra.transformation.runtime.emf.transformation.eventdriven.EventDrivenTransformation
				import org.eclipse.viatra.transformation.evm.specific.Lifecycles
				import org.eclipse.viatra.transformation.evm.specific.crud.CRUDActivationStateEnum
				import org.eclipse.viatra.transformation.runtime.emf.rules.EventDrivenTransformationRuleGroup
				import org.emoflon.ibex.tgg.runtime.engine.ViatraEngine
				import org.emoflon.ibex.tgg.runtime.engine.OperationMode
				import org.apache.log4j.Logger
				import org.apache.log4j.LogManager
				import java.util.Collections
				import org.apache.log4j.Level
				import org.emoflon.ibex.tgg.patterns.*
				
				class ViatraTransformation{	
					
					
					/* Transformation-related extensions */
					extension EventDrivenTransformation transformation
				
					/* Transformation rule-related extensions */
					extension EventDrivenTransformationRuleFactory = new EventDrivenTransformationRuleFactory
				
					protected ViatraQueryEngine engine
				
					private ViatraEngine ibexViatraEngine;
					
					// protected EventDrivenTransformationRule<?,?> exampleRule
					new(ViatraEngine ibexViatraEngine) {
						this.ibexViatraEngine = ibexViatraEngine
						
						// Create EMF scope and EMF IncQuery engine based on the resource
						val scope = new EMFScope(ibexViatraEngine.getResourceSet())
						engine = ViatraQueryEngine.on(scope);
						
						val loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
						loggers.add(LogManager.getRootLogger());
						for ( Logger logger : loggers ) {
						   logger.setLevel(Level.OFF);
						}
					
						createTransformation
					
					}
					
					public def execute() {
						transformation.executionSchema.startUnscheduledExecution
					}
					
					def dispose() {
						if (transformation != null) {
							transformation.dispose
						}
						transformation = null
						return
					}
					
					private def createTransformation() {
						// Initialize event-driven transformation
						transformation = EventDrivenTransformation.forEngine(engine).addRules(getTransformationRuleGroup).build
					}
					
					private def getTransformationRuleGroup() {
						if (ibexViatraEngine.mode == OperationMode.SYNCH)
							return getSynch
						else if (ibexViatraEngine.mode == OperationMode.MODELGEN)
							return get«PatternSuffixes.MODELGEN»
						else if (ibexViatraEngine.mode == OperationMode.CC)
							return get«PatternSuffixes.CC»
					}
					
					private def getSynch() {
						new EventDrivenTransformationRuleGroup(
							«FOR rule : tgg.rules SEPARATOR ", "»
								get«rule.name»«PatternSuffixes.FWD»(),
								get«rule.name»«PatternSuffixes.BWD»(),
								get«rule.name»«PatternSuffixes.CONSISTENCY»()
							«ENDFOR»
							)
					}
					
					private def get«PatternSuffixes.MODELGEN»(){
						new EventDrivenTransformationRuleGroup(
							«FOR rule : tgg.rules SEPARATOR ", "»
								get«rule.name»«PatternSuffixes.MODELGEN»()
							«ENDFOR»
							)
					}
					
					private def get«PatternSuffixes.CC»(){
						new EventDrivenTransformationRuleGroup(
							«FOR rule : tgg.rules SEPARATOR ", "»
								get«rule.name»«PatternSuffixes.CC»()
							«ENDFOR»
						)
					}
						
					
					«FOR suffix : suffixes»
						«FOR rule : tgg.rules»
							private def get«rule.name»«suffix»() {
								createRule.name("«rule.name»«suffix»").precondition(«rule.name»«suffix»Matcher.querySpecification).action(
									«IF !IgnoredMatchesHelper.ignoreMatches(rule, suffix)»
										«IF suffix.equals(PatternSuffixes.CONSISTENCY)»
											CRUDActivationStateEnum.CREATED) []
											.action(CRUDActivationStateEnum.DELETED)[
											ibexViatraEngine.addBrokenMatch(it)]
										«ELSE»
											
											CRUDActivationStateEnum.CREATED) [
											ibexViatraEngine.addOperationalRuleMatch("«rule.name»", it)
											].action(CRUDActivationStateEnum.DELETED)[
											ibexViatraEngine.removeOperationalRuleMatch(it)]	
										«ENDIF»
									«ENDIF»
								.addLifeCycle(				
									Lifecycles.getDefault(false, true)
									).build
							}
						«ENDFOR»
						
					«ENDFOR»
				}		
				
		'''
	}

}