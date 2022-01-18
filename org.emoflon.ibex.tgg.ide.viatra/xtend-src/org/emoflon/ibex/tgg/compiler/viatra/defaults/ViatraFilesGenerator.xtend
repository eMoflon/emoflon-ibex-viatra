package org.emoflon.ibex.tgg.compiler.viatra.defaults
 
import org.moflon.core.utilities.MoflonUtil
import org.moflon.tgg.mosl.tgg.TripleGraphGrammarFile
import org.emoflon.ibex.tgg.codegen.DefaultFilesGenerator

class ViatraFilesGenerator extends DefaultFilesGenerator {

	def static String generateDefaultRegHelperFile(String projectName) {
		'''
			package org.emoflon.ibex.tgg.run.«MoflonUtil.lastCapitalizedSegmentOf(projectName).toLowerCase».config;
			
			import java.io.IOException;
				
			import org.eclipse.emf.ecore.resource.ResourceSet;
			import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
			import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
			import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
			
			public class _DefaultRegistrationHelper implements IRegistrationHelper{
			
				/** Load and register source and target metamodels */
				public void registerMetamodels(ResourceSet rs, IbexExecutable executable) throws IOException {
					// Replace to register generated code or handle other URI-related requirements
					new ViatraRegistrationHelper().registerMetamodels(rs, executable);
				}
			
				/** Create default options **/
				public IbexOptions createIbexOptions() {
					return new ViatraRegistrationHelper().createIbexOptions();
				}
			}
		'''
	}

	def static String generateRegHelperFile(String projectName, TripleGraphGrammarFile tgg) {
		'''
			package org.emoflon.ibex.tgg.run.«MoflonUtil.lastCapitalizedSegmentOf(projectName).toLowerCase».config;
			
			import java.io.IOException;
			
			import org.eclipse.emf.ecore.resource.ResourceSet;
			import org.emoflon.ibex.tgg.operational.csp.constraints.factories.«MoflonUtil.lastCapitalizedSegmentOf(projectName).toLowerCase».UserDefinedRuntimeTGGAttrConstraintFactory;
			import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
			import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
			import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
			import org.emoflon.ibex.tgg.runtime.viatra.ViatraTGGEngine;
			
			public class ViatraRegistrationHelper implements IRegistrationHelper {
			
				/** Load and register source and target metamodels */
				public void registerMetamodels(ResourceSet rs, IbexExecutable executable) throws IOException {
					// Replace to register generated code or handle other URI-related requirements
					«FOR imp : tgg.imports»
						executable.getResourceHandler().loadAndRegisterMetamodel("«imp.name»");
					«ENDFOR»
				}
			
				/** Create default options **/
				public IbexOptions createIbexOptions() {
					IbexOptions options = new IbexOptions();
					options.blackInterpreter(new ViatraTGGEngine());
					options.project.name("«MoflonUtil.lastCapitalizedSegmentOf(projectName)»");
					options.project.path("«projectName»");
					options.debug.ibexDebug(false);
					options.csp.userDefinedConstraints(new UserDefinedRuntimeTGGAttrConstraintFactory());
					options.registrationHelper(this);
					return options;
				}
			}
		'''
	}
}
