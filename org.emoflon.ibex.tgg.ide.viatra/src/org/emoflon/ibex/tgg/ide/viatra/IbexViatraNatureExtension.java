package org.emoflon.ibex.tgg.ide.viatra;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.emoflon.ibex.tgg.ide.admin.NatureExtension;
import org.emoflon.ibex.tgg.ui.ide.admin.plugins.ManifestFileUpdater;
import org.moflon.util.LogUtils;

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
						"org.emoflon.ibex.tgg.runtime.viatra"
						));
				
				return changed;
			});
		} catch (CoreException | IOException e) {
			LogUtils.error(logger, e);
		}
	}

}
