package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.EPackage;
import org.emoflon.ibex.common.project.ManifestHelper;
import org.emoflon.ibex.tgg.compiler.viatra.defaults.ViatraFilesGenerator;
import org.emoflon.ibex.tgg.ide.admin.BuilderExtension;
import org.emoflon.ibex.tgg.ide.admin.IbexTGGBuilder;
import org.moflon.core.plugins.manifest.ManifestFileUpdater;
import org.moflon.core.utilities.LogUtils;
import org.moflon.tgg.mosl.tgg.TripleGraphGrammarFile;

public class IbexViatraBuilderExtension implements BuilderExtension {

	private static Logger logger = Logger.getLogger(IbexViatraBuilderExtension.class);
	
	@Override
	public void run(IbexTGGBuilder builder, TripleGraphGrammarFile editorModel, TripleGraphGrammarFile flattenedEditorModel) {
		
		// create the actual project path
				EPackage srcPkg = flattenedEditorModel.getSchema().getSourceTypes().get(0);
				EPackage trgPkg = flattenedEditorModel.getSchema().getTargetTypes().get(0);
				EPackage corrPkg = flattenedEditorModel.eClass().getEPackage();
				try {
					if(srcPkg == null || trgPkg == null || corrPkg == null) {
						throw new RuntimeException("Could not get flattened trg or src model from editor model.");
					}
				} catch (Exception e) {
					LogUtils.error(logger, e); 
					return;
				}
				
				String srcModel = srcPkg.getName();
				String trgModel = trgPkg.getName();
				
				IProject srcProject = getProjectInWorkspace(srcModel, builder.getProject().getWorkspace());
				IProject trgProject = getProjectInWorkspace(trgModel, builder.getProject().getWorkspace());
				
				String srcPkgName = null;
				String trgPkgName = null;
				
				if(srcProject != null) {
					srcPkgName = getRootPackageName(srcProject);
				}
				if(trgProject != null) {
					trgPkgName = getRootPackageName(trgProject);
				}
				
				LogUtils.info(logger, "Building missing app stubs...");
				try {
					generateRegHelper(builder, srcProject, trgProject, srcPkgName, trgPkgName);
					generateDefaultStubs(builder, editorModel, flattenedEditorModel);
				}catch(Exception e) {
					LogUtils.error(logger, e);
				}
		updateManifest(builder.getProject());
	}
	
	public void generateDefaultStubs(IbexTGGBuilder builder, TripleGraphGrammarFile editorModel, TripleGraphGrammarFile flattenedEditorModel) throws CoreException {
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
	}
	
	public void generateRegHelper(IbexTGGBuilder builder, IProject srcProject, IProject trgProject, String srcPkg, String trgPkg) throws Exception {
		String input_srcProject = srcProject == null ? "<<SRC_Project>>" : srcProject.getName();
		String input_trgProject = trgProject == null ? "<<TRG_Project>>" : trgProject.getName();
		String input_srcPackage = srcPkg == null ? "<<SRC_Package>>" : srcPkg;
		String input_trgPackage = trgPkg == null ? "<<TRG_Package>>" : trgPkg;
		
		builder.createDefaultConfigFile(ViatraFilesGenerator.REGISTRATION_HELPER, (projectName, fileName)
				-> ViatraFilesGenerator.generateRegHelperFile(projectName, input_srcProject, input_trgProject, input_srcPackage, input_trgPackage));
	}
	
	private static IProject getProjectInWorkspace(String modelName, IWorkspace workspace) {
		IProject[] projects = workspace.getRoot().getProjects();
		for(IProject project : projects) {
			if(project.getName().toLowerCase().equals(modelName.toLowerCase())) {
				return project;
			}
		}
		LogUtils.info(logger, "The project belonging to model "+modelName+" could not be found in the workspace.");
		return null;
	}
	
	private static String getRootPackageName(IProject project) {
		String upperPkgName = project.getName();
		String firstLower = project.getName().substring(0, 1).toLowerCase()+project.getName().substring(1);
		String lowerPkgName = project.getName().toLowerCase();
		
		IPath projectPath = project.getLocation().makeAbsolute();
		Path srcPath = Paths.get(projectPath.toPortableString()+"/src");
		File srcFolder = srcPath.toFile();
		if(srcFolder.exists() && srcFolder.isDirectory()) {
			for(String fName : srcFolder.list()) {
				if(fName.equals(upperPkgName)) {
					return upperPkgName;
				}
				if(fName.equals(firstLower)) {
					return firstLower;
				}
				if(fName.equals(lowerPkgName)) {
					return lowerPkgName;
				}
			}	
		}
		srcPath = Paths.get(projectPath.toPortableString()+"/src-gen");
		srcFolder = srcPath.toFile();
		if(srcFolder.exists() && srcFolder.isDirectory()) {
			for(String fName : srcFolder.list()) {
				if(fName.equals(upperPkgName)) {
					return upperPkgName;
				}
				if(fName.equals(firstLower)) {
					return firstLower;
				}
				if(fName.equals(lowerPkgName)) {
					return lowerPkgName;
				}
			}
		}
		
		srcPath = Paths.get(projectPath.toPortableString()+"/gen");
		srcFolder = srcPath.toFile();
		if(srcFolder.exists() && srcFolder.isDirectory()) {
			for(String fName : srcFolder.list()) {
				if(fName.equals(upperPkgName)) {
					return upperPkgName;
				}
				if(fName.equals(firstLower)) {
					return firstLower;
				}
				if(fName.equals(lowerPkgName)) {
					return lowerPkgName;
				}
			}
		}
		
		LogUtils.info(logger, "The project belonging to model "+project.getName()+" does not seem to have generated code.");
		return null;
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
