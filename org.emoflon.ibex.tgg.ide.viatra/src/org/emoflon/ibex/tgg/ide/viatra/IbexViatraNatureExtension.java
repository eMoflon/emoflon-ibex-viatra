package org.emoflon.ibex.tgg.ide.viatra;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.emoflon.ibex.tgg.ide.admin.NatureExtension;
import org.emoflon.ibex.tgg.ui.ide.admin.plugins.ManifestFileUpdater;
import org.moflon.util.LogUtils;
import org.moflon.util.WorkspaceHelper;

public class IbexViatraNatureExtension implements NatureExtension {

	private static final Logger logger = Logger.getLogger(IbexViatraNatureExtension.class);
	
	@Override
	public void setUpProject(IProject project) {
		// TODO [Erhan-Unification]:  Add new natures, dependencies, whatever you want to do to the project
		// see IbexTGGNature for how to add natures, deps, classpath containers, etc to the project, e.g.:
		try {
			new ManifestFileUpdater().processManifest(project, manifest -> {
				boolean changed = false;
				
				changed |= ManifestFileUpdater.updateDependencies(manifest, Arrays.asList(
						// Ibex Viatra deps
						"org.emoflon.ibex.tgg.runtime.viatra",
						"org.eclipse.viatra.query.runtime",
						"org.eclipse.viatra.transformation.runtime.emf",
						"org.eclipse.viatra.transformation.evm",
						"org.eclipse.viatra.transformation.evm.transactions",
						"org.eclipse.viatra.query.runtime.base.itc",
						"com.google.guava",
						"org.eclipse.xtend",
						"org.eclipse.xtext.xbase.lib",
						"org.eclipse.xtend.lib",
						"org.eclipse.xtend.lib.macro"
						
						));
				
				return changed;
			});
			
			WorkspaceHelper.addNature(project, "org.eclipse.viatra.query.projectnature", new NullProgressMonitor());

			
		} catch (CoreException | IOException e) {
			LogUtils.error(logger, e);
		}
		
	}

}
