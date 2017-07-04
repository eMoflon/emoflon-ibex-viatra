package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.emoflon.ibex.tgg.compiler.TGGCompiler;
import org.emoflon.ibex.tgg.compiler.defaults.DefaultFilesGenerator;
import org.emoflon.ibex.tgg.compiler.pattern.IbexPattern;
import org.emoflon.ibex.tgg.core.transformation.EditorTGGtoInternalTGG;
import org.emoflon.ibex.tgg.core.transformation.TGGProject;
import org.emoflon.ibex.tgg.ide.admin.BuilderExtension;
import org.emoflon.ibex.tgg.ide.admin.IbexTGGBuilder;
import org.moflon.tgg.mosl.tgg.TripleGraphGrammarFile;
import org.moflon.util.LogUtils;

import language.TGGRule;

public class IbexViatraBuilderExtension implements BuilderExtension {

	private Logger logger = Logger.getLogger(IbexViatraBuilderExtension.class);

	private static final String ENGINE = "ViatraEngine";
	private static final String IMPORT = "import org.emoflon.ibex.tgg.runtime.engine.ViatraEngine;";

	@Override
	public void run(IbexTGGBuilder builder, TripleGraphGrammarFile editorModel, TripleGraphGrammarFile flattenedEditorModel) {
		try {
			builder.createDefaultRunFile("MODELGEN_App", (projectName, fileName) 
					-> DefaultFilesGenerator.generateModelGenFile(projectName, fileName, ENGINE, IMPORT));
			builder.createDefaultRunFile("SYNC_App", (projectName, fileName) 
					-> DefaultFilesGenerator.generateSyncAppFile(projectName, fileName, ENGINE, IMPORT));
			builder.createDefaultRunFile("CC_App", (projectName, fileName) 
					-> DefaultFilesGenerator.generateCCAppFile(projectName, fileName, ENGINE, IMPORT));
			
			Optional<TGGProject> internalModel = new EditorTGGtoInternalTGG().generateInternalModels(editorModel, flattenedEditorModel, builder.getProject());
			
			TGGCompiler compiler = new TGGCompiler(internalModel.get().getTggModel(), internalModel.get().getFlattenedTggModel());
			compiler.preparePatterns();
			Map<TGGRule, Collection<IbexPattern>> rulesToPatterns = compiler.getRuleToPatternMap();
			rulesToPatterns.keySet().forEach(r -> {
				try {
					builder.createDefaultFile("model/patterns/", r.getName(), ".vql", (p, f) -> generatePatternFile(rulesToPatterns.get(r)));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			});
			
			
			// TODO [Erhan-Unification]: Generate anything else you want in the project e.g.:
			builder.createDefaultFile("model/", "MyFile", ".foo", (p, f) -> "This can be supplied by an xtend template:" + p + "::" + f);
		} catch (CoreException e) {
			LogUtils.error(logger, e);
		}
	}

	private String generatePatternFile(Collection<IbexPattern> patterns) {
		// TODO Auto-generated method stub
		return "Hoochie Mama";
	}
}
