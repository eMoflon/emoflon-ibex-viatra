package org.emoflon.ibex.tgg.compiler.viatra.defaults

import java.util.Collection
import java.util.LinkedHashMap
import language.TGGRuleElement
import language.TGGRuleNode
import org.eclipse.emf.ecore.EPackage
import org.emoflon.ibex.tgg.compiler.pattern.IbexPattern
import org.emoflon.ibex.tgg.compiler.pattern.rulepart.RulePartPattern

class PatternTemplate {

	private LinkedHashMap<EPackage, String> importAliases;

	new(LinkedHashMap<EPackage, String> importAliases) {
		this.importAliases = importAliases;
	}

	def generatePatternCode(Collection<IbexPattern> patterns) {
		return '''
			package org.emoflon.ibex.tgg.patterns
					
			«FOR p : importAliases.keySet»
				import "«p.nsURI»" as «importAliases.get(p)»
			«ENDFOR»
			
			«FOR p : patterns»
				«generatePatternCode(p)»
			«ENDFOR»
		'''
	}

	def generatePatternCode(IbexPattern pattern) {
		return '''
			pattern «pattern.getName»(«FOR e : pattern.signatureElements SEPARATOR ", "»«e.name»:«typeOf(e)»«ENDFOR»){
				
				check(true);
				
				«FOR pi : pattern.positiveInvocations»«getPatternInvocation(pattern, pi, true)»«ENDFOR»
				«FOR ni : pattern.negativeInvocations»«getPatternInvocation(pattern, ni, false)»«ENDFOR»
				
				«FOR e : pattern.signatureElements»
					«typeOf(e)»(«e.name»);
				«ENDFOR»
				
				«FOR edge : pattern.bodyEdges»
					«typeOf(edge.srcNode)».«edge.type.name»(«edge.srcNode.name», «edge.trgNode.name»);
				«ENDFOR»
				
				«FOR corr : pattern.bodyCorrNodes»
					«typeOf(corr)».source(«corr.name»,«corr.source.name»);
					«typeOf(corr)».target(«corr.name»,«corr.target.name»);
				«ENDFOR»
				
				«IF pattern instanceof RulePartPattern»
					«getAttributechecks(pattern)»
					«getInjectivityChecks(pattern as RulePartPattern)»
				«ENDIF»
			}
		'''
	}

	def getAttributechecks(IbexPattern pattern) {
		return '''
		«FOR node : pattern.bodySrcTrgNodes»
			«typeOf(node)»(«node.name»);
			«FOR attrExpr : node.attrExpr»
				«IF InplaceAttribute2ViatraCheck.simpleExpression(attrExpr)»
					«typeOf(node)».«attrExpr.attribute.name»(«node.name», «InplaceAttribute2ViatraCheck.extractViatraEqualCheck(attrExpr)»);
				«ELSE»
					«typeOf(node)».«attrExpr.attribute.name»(«node.name», «node.name»_«attrExpr.attribute.name»_emoflonAttr);
					«IF !InplaceAttribute2ViatraCheck.isENUMExpr(attrExpr)»
						check («InplaceAttribute2ViatraCheck.extractViatraCheck(node.name + "_" + attrExpr.attribute.name + "_emoflonAttr", attrExpr)»);
					«ELSE»
						«InplaceAttribute2ViatraCheck.extractViatraCheck(node.name + "_" + attrExpr.attribute.name + "_emoflonAttr", attrExpr)»;
					«ENDIF»
				«ENDIF»
			«ENDFOR»	
		«ENDFOR»'''
	}

	def typeOf(TGGRuleElement e) {
		return '''«importAliases.get((e as TGGRuleNode).type.EPackage)»::«(e as TGGRuleNode).type.name»'''
	}

	def getInjectivityChecks(RulePartPattern pattern) {
		return '''
			«FOR injectivityCheckPair : pattern.injectivityChecks»
				«injectivityCheckPair.left.name» != «injectivityCheckPair.right.name»;
			«ENDFOR»
		'''
	}

	def getPatternInvocation(IbexPattern root, IbexPattern inv, boolean positive) {
		var i = 0;
		var signaturElement = inv.getSignatureElements().stream().findFirst();
		var j = 1;
		if (signaturElement.present)
			j = root.getMappedRuleElement(inv, signaturElement.get()).size();

		var result = "";
		while (i < j) {
			result += getPatternInvocation(root, inv, positive, i);
			i++;
		}
		return '''
			«result»
		'''
	}

	def getPatternInvocation(IbexPattern root, IbexPattern inv, boolean positive,
		int index) {
		return '''
			«IF !positive»neg«ENDIF» find «inv.name»(«FOR e : inv.signatureElements SEPARATOR ", "»«root.getMappedRuleElement(inv, e).get(index).name»«ENDFOR»);
			
		'''
	}

}
