package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.emoflon.ibex.common.project.ManifestHelper;
import org.emoflon.ibex.tgg.ide.admin.BuilderExtension;
import org.emoflon.ibex.tgg.ide.admin.IbexTGGBuilder;
import org.moflon.core.plugins.manifest.ManifestFileUpdater;
import org.moflon.core.utilities.LogUtils;
import org.moflon.tgg.mosl.tgg.TripleGraphGrammarFile;

public class IbexViatraBuilderExtension implements BuilderExtension {

	private Logger logger = Logger.getLogger(IbexViatraBuilderExtension.class);
	
	@Override
	public void run(IbexTGGBuilder builder, TripleGraphGrammarFile editorModel, TripleGraphGrammarFile flattenedEditorModel) {
		try {
			builder.createDefaultDebugRunFile("MODELGEN_Debug_App", (projectName, fileName) 
				-> ViatraFilesGenerator.generateModelGenDebugFile(projectName, fileName));
			builder.createDefaultRunFile("MODELGEN_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateModelGenFile(projectName, fileName));
			builder.createDefaultRunFile("SYNC_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateSyncAppFile(projectName, fileName));
			builder.createDefaultRunFile("INITIAL_FWD_App", (projectName, fileName) 
				-> ViatraFilesGenerator.generateInitialFwdAppFile(projectName, fileName));
			builder.createDefaultRunFile("INITIAL_BWD_App", (projectName, fileName) 
				-> ViatraFilesGenerator.generateInitialBwdAppFile(projectName, fileName));
			builder.createDefaultRunFile("CC_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateCCAppFile(projectName, fileName));
			builder.createDefaultRunFile("CO_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateCOAppFile(projectName, fileName));
			builder.createDefaultRunFile("FWD_OPT_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateFWDOptAppFile(projectName, fileName));
			builder.createDefaultRunFile("BWD_OPT_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateBWDOptAppFile(projectName, fileName));
			builder.createDefaultRunFile("INTEGRATE_App", (projectName, fileName) 
					-> ViatraFilesGenerator.generateIntegrateAppFile(projectName, fileName));
			builder.createDefaultConfigFile("_DefaultRegistrationHelper", (projectName, fileName) 
					-> ViatraFilesGenerator.generateDefaultRegHelperFile(projectName));
			builder.createDefaultConfigFile("ViatraRegistrationHelper", (projectName, fileName)
					-> ViatraFilesGenerator.generateRegHelperFile(projectName, editorModel));
		} catch (CoreException e) {
			LogUtils.error(logger, e);
		}
		
		updateManifest(builder.getProject());
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
