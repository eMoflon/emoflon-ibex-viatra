package org.emoflon.ibex.tgg.compiler.viatra.defaults
 
import java.util.Collection
import language.TGGAttributeConstraintDefinition
import org.moflon.core.utilities.MoflonUtil
import org.moflon.tgg.mosl.tgg.TripleGraphGrammarFile
import org.emoflon.ibex.tgg.compiler.defaults.DefaultFilesGenerator

class ViatraFilesGenerator extends DefaultFilesGenerator {
	
	public static final String DEFAULT_REGISTRATION_HELPER = "_DefaultRegistrationHelper";
	public static final String MODELGEN_APP = "MODELGEN_App"; 
	public static final String SYNC_APP = "SYNC_App";
	public static final String INITIAL_FWD_APP = "INITIAL_FWD_App";
	public static final String INITIAL_BWD_APP = "INITIAL_BWD_App";
	public static final String CC_APP = "CC_App";
	public static final String CO_APP = "CO_App";
	public static final String FWD_OPT_APP = "FWD_OPT_App";
	public static final String BWD_OPT_APP = "BWD_OPT_App";
	public static final String REGISTRATION_HELPER = "ViatraRegistrationHelper";

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

	def static String generateRegHelperFile(String projectName, String srcPkg, String trgPkg, String srcProject, String trgProject) {
		'''
			package org.emoflon.ibex.tgg.run.«MoflonUtil.lastCapitalizedSegmentOf(projectName).toLowerCase».config;
			
			import java.io.IOException;
			
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.resource.Resource;
			import org.eclipse.emf.ecore.resource.ResourceSet;
			import org.emoflon.ibex.tgg.operational.csp.constraints.factories.«MoflonUtil.lastCapitalizedSegmentOf(projectName).toLowerCase».UserDefinedRuntimeTGGAttrConstraintFactory;
			import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
			import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
			import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
			import org.emoflon.ibex.tgg.operational.strategies.opt.BWD_OPT;
			import org.emoflon.ibex.tgg.operational.strategies.opt.FWD_OPT;
			import org.emoflon.ibex.tgg.runtime.viatra.ViatraTGGEngine;
			
			import «srcPkg».impl.«srcPkg.toFirstUpper»PackageImpl;
			import «trgPkg».impl.«trgPkg.toFirstUpper»PackageImpl;
			
			public class ViatraRegistrationHelper implements IRegistrationHelper {
			
				/** Load and register source and target metamodels */
				public void registerMetamodels(ResourceSet rs, IbexExecutable executable) throws IOException {
					// Load and register source and target metamodels
					«srcPkg.toFirstUpper»PackageImpl.init();
					«trgPkg.toFirstUpper»PackageImpl.init();
				
				if(executable instanceof FWD_OPT) {
					Resource res = executable.getResourceHandler().loadResource("platform:/resource/«trgProject»/model/«trgProject».ecore");
					EPackage pack = (EPackage) res.getContents().get(0);
					rs.getPackageRegistry().put("platform:/resource/«trgProject»/model/«trgProject».ecore", pack);
					rs.getPackageRegistry().put("platform:/plugin/«trgProject»/model/«trgProject».ecore", pack);
					}
											
				if(executable instanceof BWD_OPT) {
					Resource res = executable.getResourceHandler().loadResource("platform:/resource/«srcProject»/model/«srcProject».ecore");
					EPackage pack = (EPackage) res.getContents().get(0);
					rs.getPackageRegistry().put("platform:/resource/«srcProject»/model/«srcProject».ecore", pack);
					rs.getPackageRegistry().put("platform:/plugin/«srcProject»/model/«srcProject».ecore", pack);
					}
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
