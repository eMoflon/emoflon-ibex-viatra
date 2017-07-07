package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EPackage;
import org.emoflon.ibex.tgg.compiler.TGGCompiler;
import org.emoflon.ibex.tgg.compiler.defaults.DefaultFilesGenerator;
import org.emoflon.ibex.tgg.compiler.patterns.PatternFactory;
import org.emoflon.ibex.tgg.compiler.patterns.common.IbexPattern;
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

	private static final String RUN = "src/org/emoflon/ibex/tgg/run/";
	private static final String PATTERNS = "model/patterns/";
	private static final String VIATRA_TRANSFORMATION = "ViatraTransformation";
	private static final String XTEND = ".xtend";
	private static final String VQL = ".vql";
	
	@Override
	public void run(IbexTGGBuilder builder, TripleGraphGrammarFile editorModel,
			TripleGraphGrammarFile flattenedEditorModel) {
		try {
			performClean(builder);
			generateRunnableStubs(builder);

			Optional<TGGProject> internalModelOptional = builder.computeOrGetFromBlackboard(
					EditorTGGtoInternalTGG.INTERNAL_TGG_MODEL, () -> new EditorTGGtoInternalTGG()
							.generateInternalModels(editorModel, flattenedEditorModel, builder.getProject()));

			TGGProject internalModel = internalModelOptional.get();

			generatePatternFiles(builder, internalModel);

			generateViatraTransformation(builder, internalModel);

		} catch (CoreException e) {
			LogUtils.error(logger, e);
		}
	}

	private void generateViatraTransformation(IbexTGGBuilder builder, TGGProject internalModel) {
		String projectName = builder.getProject().getName();
		TGG tgg = internalModel.getFlattenedTggModel();
		try {
			builder.createIfNotExists(RUN, VIATRA_TRANSFORMATION, XTEND,
					(p, f) -> generateViatraTransformationFile(tgg, projectName));
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private String generateViatraTransformationFile(TGG tgg, String projectName) {
		ManipulationTemplate manipulationTemlate = new ManipulationTemplate();
		return manipulationTemlate.getManipulationCode(tgg, projectName);
	}

	private void generatePatternFiles(IbexTGGBuilder builder, TGGProject internalModel) throws CoreException {
		LinkedHashMap<EPackage, String> aliases = (new ImportAliasHelper(internalModel.getTggModel()))
				.getEpackageToAlias();

		IbexOptions options = new IbexOptions();
		options.useFlattenedTGG(true);
		options.projectPath(builder.getProject().getName());
		options.debug(false);
		options.tgg(internalModel.getTggModel());
		options.flattenedTgg(internalModel.getFlattenedTggModel());

		TGGCompiler compiler = new TGGCompiler(options);
		compiler.preparePatterns();

		Map<TGGRule, Collection<IbexPattern>> rulesToPatterns = compiler.getRuleToPatternMap();
		rulesToPatterns.keySet().forEach(r -> {
			try {
				builder.createIfNotExists(PATTERNS, r.getName(), VQL,
						(p, f) -> generatePatternFile(rulesToPatterns.get(r), aliases));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		});

		Collection<IbexPattern> markedPatterns = PatternFactory.getMarkedPatterns().stream().map(p -> (IbexPattern) p)
				.collect(Collectors.toSet());
		builder.createIfNotExists(PATTERNS, "EMoflon", VQL,
				(p, f) -> generatePatternFile(markedPatterns, aliases));
	}

	private void generateRunnableStubs(IbexTGGBuilder builder) throws CoreException {
		builder.createDefaultRunFile("MODELGEN_App", (projectName, fileName) -> DefaultFilesGenerator
				.generateModelGenFile(projectName, fileName, ENGINE, IMPORT));
		builder.createDefaultRunFile("SYNC_App", (projectName, fileName) -> DefaultFilesGenerator
				.generateSyncAppFile(projectName, fileName, ENGINE, IMPORT));
		builder.createDefaultRunFile("CC_App", (projectName, fileName) -> DefaultFilesGenerator
				.generateCCAppFile(projectName, fileName, ENGINE, IMPORT));
	}

	private String generatePatternFile(Collection<IbexPattern> patterns, LinkedHashMap<EPackage, String> aliases) {
		return (new PatternTemplate(aliases)).generatePatternCode(patterns);
	}

	@Override
	public void performClean(IbexTGGBuilder builder) {

		try {
			IFolder patterns = builder.getProject().getFolder("model/patterns");
			if (patterns.exists())
				patterns.delete(true, new NullProgressMonitor());
			IFile viatraTrafo = builder.getProject().getFile(RUN+VIATRA_TRANSFORMATION);
			if(viatraTrafo.exists())
				viatraTrafo.delete(true, new NullProgressMonitor());
		} catch (Exception e) {
			LogUtils.error(logger, e);
		}
	}
}
