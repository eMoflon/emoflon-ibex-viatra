package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.emoflon.ibex.common.project.ManifestHelper;
import org.emoflon.ibex.tgg.codegen.TGGEngineBuilderExtension;
import org.emoflon.ibex.tgg.ide.admin.IbexTGGBuilder;
import org.moflon.core.plugins.manifest.ManifestFileUpdater;
import org.moflon.core.utilities.LogUtils;
import org.moflon.tgg.mosl.tgg.TripleGraphGrammarFile;

public class IbexViatraBuilderExtension implements TGGEngineBuilderExtension {

	private Logger logger = Logger.getLogger(IbexViatraBuilderExtension.class);
	
	@Override
	public void run(IProject project, TripleGraphGrammarFile editorModel, TripleGraphGrammarFile flattenedEditorModel) {
		updateManifest(project);
		
		try {
			IbexTGGBuilder.createDefaultDebugRunFile(project, "MODELGEN_Debug_App", (projectName, fileName) 
				-> ViatraFilesGenerator.generateModelGenDebugFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "MODELGEN_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateModelGenFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "SYNC_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateSyncAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "INITIAL_FWD_App", (projectName, fileName) 
				-> ViatraFilesGenerator.generateInitialFwdAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "INITIAL_BWD_App", (projectName, fileName) 
				-> ViatraFilesGenerator.generateInitialBwdAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "CC_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateCCAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "CO_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateCOAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "FWD_OPT_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateFWDOptAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "BWD_OPT_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateBWDOptAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultRunFile(project, "INTEGRATE_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateIntegrateAppFile(projectName, fileName));
			IbexTGGBuilder.createDefaultConfigFile(project, "_DefaultRegistrationHelper", (projectName, fileName) 
					-> ViatraFilesGenerator.generateDefaultRegHelperFile(projectName));
			IbexTGGBuilder.createDefaultConfigFile(project, "ViatraRegistrationHelper", (projectName, fileName)
					-> ViatraFilesGenerator.generateRegHelperFile(projectName, editorModel));
		} catch (CoreException e) {
			LogUtils.error(logger, e);
		}
		
	}
	
	private void updateManifest(IProject project) {
		try {
			IFile manifest = ManifestFileUpdater.getManifestFile(project);
			ManifestHelper helper = new ManifestHelper();
			helper.loadManifest(manifest);
			if(!helper.sectionContainsContent("Require-Bundle", "org.emoflon.ibex.tgg.runtime.viatra")) {
				helper.addContentToSection("Require-Bundle", "org.emoflon.ibex.tgg.runtime.viatra");
			}
			String projectPath = project.getLocation().toPortableString();
			File rawManifest = new File(projectPath+"/"+manifest.getFullPath().removeFirstSegments(1).toPortableString());
			
			helper.updateManifest(rawManifest);
			
		} catch (CoreException | IOException e) {
			LogUtils.error(logger, "Failed to update MANIFEST.MF \n"+e.getMessage());
		}
	}

	
}
