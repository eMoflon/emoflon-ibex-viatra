package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import java.util.LinkedHashMap;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;

import language.TGG;
import runtime.RuntimePackage;

public class ImportAliasHelper {

	private LinkedHashMap<EPackage, String> epackageToAlias = new LinkedHashMap<>();

	private int packageCounter = 0;
	
	public ImportAliasHelper(TGG tgg){
		fillImportAliasTables(tgg);
	}
	
	private void fillImportAliasTables(TGG tgg) {
		epackageToAlias.put(RuntimePackage.eINSTANCE, "dep_ibex");
		epackageToAlias.put(EcorePackage.eINSTANCE, "dep_ecore");

		tgg.getRules().stream().flatMap(r -> r.getNodes().stream()).map(n -> n.getType().getEPackage())
				.forEachOrdered(this::createAlias);
	}
	
	private void createAlias(EPackage p){
		if (!epackageToAlias.containsKey(p)) {
			String alias = "dep_" + ++packageCounter;
			epackageToAlias.put(p, alias);
		}
	}
	
	public LinkedHashMap<EPackage, String> getEpackageToAlias() {
		return epackageToAlias;
	}
	
}
