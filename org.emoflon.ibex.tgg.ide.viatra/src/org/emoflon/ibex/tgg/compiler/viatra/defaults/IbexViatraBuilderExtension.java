package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EPackage;
import org.emoflon.ibex.tgg.compiler.TGGCompiler;
import org.emoflon.ibex.tgg.compiler.defaults.DefaultFilesGenerator;
import org.emoflon.ibex.tgg.compiler.pattern.IbexPattern;
import org.emoflon.ibex.tgg.compiler.pattern.PatternFactory;
import org.emoflon.ibex.tgg.core.transformation.EditorTGGtoInternalTGG;
import org.emoflon.ibex.tgg.core.transformation.TGGProject;
import org.emoflon.ibex.tgg.ide.admin.BuilderExtension;
import org.emoflon.ibex.tgg.ide.admin.IbexTGGBuilder;
import org.emoflon.ibex.tgg.operational.util.IbexOptions;
import org.moflon.tgg.mosl.tgg.TripleGraphGrammarFile;
import org.moflon.util.LogUtils;

import language.TGG;
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

			Optional<TGGProject> internalModel = builder.computeOrGetFromBlackboard(
				    EditorTGGtoInternalTGG.INTERNAL_TGG_MODEL, 
				    () -> new EditorTGGtoInternalTGG().generateInternalModels(editorModel, flattenedEditorModel, builder.getProject()));			
			
			
			LinkedHashMap<EPackage, String> aliases = (new ImportAliasHelper(internalModel.get().getTggModel())).getEpackageToAlias();
			
			IbexOptions options = new IbexOptions();
			options.useFlattenedTGG(true);
			options.projectPath(builder.getProject().getName());
			options.debug(false);
			options.tgg(internalModel.get().getTggModel());
			options.flattenedTgg(internalModel.get().getFlattenedTggModel());
			
			TGGCompiler compiler = new TGGCompiler(options);
			compiler.preparePatterns();
			
			
			
			Map<TGGRule, Collection<IbexPattern>> rulesToPatterns = compiler.getRuleToPatternMap();
			rulesToPatterns.keySet().forEach(r -> {
				try {
					builder.createDefaultFile("model/patterns/", r.getName(), ".vql", (p, f) -> generatePatternFile(rulesToPatterns.get(r), aliases));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			});
			

           Collection<IbexPattern> markedPatterns = PatternFactory.getMarkedPatterns().stream().map(p -> (IbexPattern) p).collect(Collectors.toSet());
		   builder.createDefaultFile("model/patterns/", "EMoflon", ".vql", (p, f) -> generatePatternFile(markedPatterns, aliases));

		} catch (CoreException e) {
			LogUtils.error(logger, e);
		}
	}

	private String generatePatternFile(Collection<IbexPattern> patterns, LinkedHashMap<EPackage, String> aliases) {
		return (new PatternTemplate(aliases)).generatePatternCode(patterns);
	}
}
