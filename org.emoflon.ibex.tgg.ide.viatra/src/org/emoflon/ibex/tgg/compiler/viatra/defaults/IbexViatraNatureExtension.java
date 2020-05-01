package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.emoflon.ibex.tgg.ide.admin.NatureExtension;
import org.moflon.core.plugins.manifest.ManifestFileUpdater;
import org.moflon.core.utilities.LogUtils;

public class IbexViatraNatureExtension implements NatureExtension {

	private Logger logger = Logger.getLogger(IbexViatraNatureExtension.class);

	@Override
	public void setUpProject(IProject project) {
		try {
			new ManifestFileUpdater().processManifest(project, manifest -> {
				boolean changed = false;
				changed |= ManifestFileUpdater.updateDependencies(
						manifest,
						Arrays.asList(
								// Viatra deps
								"org.eclipse.viatra.query.patternlanguage.emf",
								"org.eclipse.viatra.query.runtime",
								"org.eclipse.viatra.query.runtime.base",
								"org.eclipse.viatra.query.runtime.matchers",
								"org.eclipse.viatra.query.runtime.rete",
								"org.eclipse.viatra.query.runtime.base.itc",

								// Ibex Viatra deps
								"org.emoflon.ibex.tgg.runtime.viatra"
						));
				return changed;
			});
		} catch (CoreException e) {
			LogUtils.error(logger, e);
		}	
	}
}
