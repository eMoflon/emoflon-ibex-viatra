package org.emoflon.ibex.tgg.runtime.viatra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.viatra.query.patternlanguage.emf.specification.SpecificationBuilder;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ClassType;
import org.eclipse.viatra.query.patternlanguage.emf.vql.LocalVariable;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Parameter;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ParameterDirection;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ParameterRef;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PathExpressionConstraint;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguageFactory;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ReferenceType;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Variable;
import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.emoflon.ibex.gt.viatra.runtime.IBeXToViatraPatternTransformation;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXAttributeExpression;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXAttributeValue;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXCSP;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXNode;

@SuppressWarnings("restriction")
public class TGGIBeXToViatraPatternTransformation extends IBeXToViatraPatternTransformation {
	
	public TGGIBeXToViatraPatternTransformation(SpecificationBuilder builder) {
		super(builder);
	}

	@SuppressWarnings("restriction")
	private List<IExpressionEvaluator> iBeXCSPToViatra(EList<IBeXCSP> csps) {
		List<IExpressionEvaluator> cspExpressions = new ArrayList<IExpressionEvaluator>();
		int i = 0;
		for(IBeXCSP csp : csps) {
			List<String> inputParameterNames = new ArrayList<String>(); 
			for(IBeXAttributeValue value : csp.getValues()) {
				if(value instanceof IBeXAttributeExpression) {
					String nodeTypeName = ((IBeXAttributeExpression) value).getNode().getType().getName();
					String nodeName = ((IBeXAttributeExpression) value).getNode().getName();
					EAttribute eAttValue = ((IBeXAttributeExpression) value).getAttribute();
					PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
					LocalVariable locVar = createLocalVariable(nodeName + "_CSP_"+ i, createReferenceType(eAttValue.getName(), eAttValue));
					Variable var_PathExp = createLocalVariable(nodeName, createClassType(nodeTypeName, ((IBeXAttributeExpression) value).getNode().getType()));
					ReferenceType refType_PathExp = createReferenceType(eAttValue.getName(), eAttValue);
					pathExp.setDst(createVariableReference(nodeName + "_CSP_" + i, locVar));
					pathExp.getEdgeTypes().add(refType_PathExp);
					pathExp.setSourceType(createClassType(nodeName, ((IBeXAttributeExpression) value).getNode().getType()));
					pathExp.setSrc(createVariableReference(nodeName, var_PathExp));
				 	inputParameterNames.add(locVar.getName());
				 	body.getConstraints().add(pathExp);
				 	body.getVariables().add(locVar);
				 	body.getVariables().add(var_PathExp);
			 	}
			}
			cspExpressions.add(iBeXCSPExpressionEvaluatorBuilder(csp, inputParameterNames, i));
			i++;
		}
		return cspExpressions;
	}
	
	@SuppressWarnings("restriction")
	@Override
	public Pattern iBexPatternToViatraPattern(IBeXContextPattern pattern) throws Exception {
		if(viatraPatterns.containsKey(pattern.getName())) {
			return null;
		}
		variables = new HashMap<String, Variable>();
		ArrayList<IExpressionEvaluator> expressions = new ArrayList<IExpressionEvaluator>();
		allNodes = new HashMap<IBeXNode, Boolean>();
		Pattern patternToResolve = PatternLanguageFactory.eINSTANCE.createPattern();
		body = PatternLanguageFactory.eINSTANCE.createPatternBody();
		parameters = new ArrayList<Variable>();
		String nodeName;
		EClass nodeType;
		pattern.getSignatureNodes().forEach(x -> allNodes.put(x, true));
		pattern.getLocalNodes().forEach(x -> {
			x.setName("LOCAL" + x.getName());
			allNodes.put(x, false);
		});
		if(!pattern.getInvocations().isEmpty()){
			iBeXInvocationsToViatraPatternCall(pattern.getInvocations());
		}
		expressions.addAll(IBeXConstraintToViatraConstraint(pattern.getAttributeConstraint()));
		
		body.getConstraints().addAll(iBeXInjectiveConstraintToViatra(pattern.getInjectivityConstraints()));
		
		iBexEdgeToViatraSchema(pattern.getLocalEdges());
		
		expressions.addAll(iBeXCSPToViatra(pattern.getCsps()));
		
		if(!allNodes.isEmpty()) {
		for(Map.Entry<IBeXNode, Boolean> node : allNodes.entrySet()) {
			nodeName = node.getKey().getName();
			nodeType = node.getKey().getType();
			ClassType type = createClassType(nodeType.getName(), nodeType);
			Variable var = createLocalVariable(nodeName, type);
			if(node.getValue()) {
				Parameter par = createParameter(nodeName, createClassType(nodeType.getName(), nodeType), ParameterDirection.get(0));
				parameters.add(par);
				var = (ParameterRef) PatternLanguageFactory.eINSTANCE.createParameterRef();
				((ParameterRef) var).setReferredParam(par);
				var.setName(nodeName);
				var.setType(createClassType(nodeType.getName(), nodeType));
			}  
			else {
				PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
				pathExp.setDst(createVariableReference(nodeName, var));
				pathExp.setSourceType(createClassType(nodeType.getName(), nodeType));
				pathExp.setSrc(createVariableReference(nodeName, var));
				body.getConstraints().add(pathExp);
			}
			body.getVariables().add(var);
		}
		}
		viatraSpecifications.add(buildSpecification(setViatraPattern(patternToResolve, pattern.getName()), expressions));
		viatraPatterns.put(pattern.getName() ,setViatraPattern(patternToResolve, pattern.getName()));
		return setViatraPattern(patternToResolve, pattern.getName());
		
	}
	
	private IExpressionEvaluator iBeXCSPExpressionEvaluatorBuilder(IBeXCSP csp, List<String> inputParameterNames, int numberOfCsp) {
		return new CSPIExpressionEvaluatorBuilder().iBeXCSPExpressionEvaluatorBuilder(csp, inputParameterNames, numberOfCsp);
	}
}
