package org.emoflon.ibex.gt.viatra.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EStructuralFeature;

import org.eclipse.viatra.query.patternlanguage.emf.specification.SpecificationBuilder;
import org.eclipse.viatra.query.patternlanguage.emf.vql.*;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.ExpressionEvaluation;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery.PQueryStatus;

import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.impl.XbaseFactoryImpl;

import IBeXLanguage.IBeXNode;
import IBeXLanguage.IBeXNodePair;
import IBeXLanguage.IBeXPatternInvocation;
import IBeXLanguage.IBeXPatternSet;
import IBeXLanguage.IBeXAttributeConstraint;
import IBeXLanguage.IBeXAttributeExpression;
import IBeXLanguage.IBeXAttributeParameter;
import IBeXLanguage.IBeXAttributeValue;
import IBeXLanguage.IBeXCSP;
import IBeXLanguage.IBeXConstant;
import IBeXLanguage.IBeXContext;
import IBeXLanguage.IBeXContextAlternatives;
import IBeXLanguage.IBeXContextPattern;
import IBeXLanguage.IBeXEdge;
import IBeXLanguage.IBeXEnumLiteral;

/**
 * Class which is used to transform IBeX-Model into Viatra-Model
 * @author Julian Barthel
 * @version 1.0
 * @since 1.0
 */
@SuppressWarnings({ "restriction"})
public class IBeXToViatraPatternTransformation{
	
	protected PatternBody body;
	protected Collection<Variable> parameters;
	protected HashMap<String, Pattern> viatraPatterns;
	protected Set<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>> viatraSpecifications;
	protected HashMap<IBeXNode, Boolean> allNodes;
	protected SpecificationBuilder builder;
	protected HashMap<String, Variable> variables;
	protected Pattern patternToResolve;
	
	protected enum FunctionType {
		PARAMETER_ONLY,
		PARAMETER_AND_VALUE,
		VALUE_ONLY,
	}
	protected enum Operator {
		EQUAL,
		GREATER_OR_EQUAL,
		GREATER,
		SMALLER,
		SMALLER_OR_EQUAL,
		UNEQUAL
	}

	public IBeXToViatraPatternTransformation(SpecificationBuilder builder){
		this.builder = builder;
	}
	public IBeXToViatraPatternTransformation(){
		}
	
	protected ClassType createClassType(String typeName, EClassifier className) {
		ClassType type = PatternLanguageFactory.eINSTANCE.createClassType();
		type.setTypename(typeName);
		type.setClassname(className);
		return type;
	}	
	protected LocalVariable createLocalVariable(String name, Type type) {
		LocalVariable var = PatternLanguageFactory.eINSTANCE.createLocalVariable();
		var.setName(name);
		var.setType(type);
		return var;
		}
	
	protected VariableReference createVariableReference(String var, Variable variable) {
		VariableReference ref = PatternLanguageFactory.eINSTANCE.createVariableReference();
		ref.setVar(var);
		ref.setVariable(variable);
		return ref;
	}
	
	protected Parameter createParameter(String name, Type type, ParameterDirection direction) {
		Parameter par = PatternLanguageFactory.eINSTANCE.createParameter();
		par.setName(name);
		par.setType(type);
		par.setDirection(direction);
		return par;
	}
	
	protected ReferenceType createReferenceType(String typename, EStructuralFeature refname) {
		ReferenceType refType = PatternLanguageFactory.eINSTANCE.createReferenceType();
		refType.setTypename(typename);
		refType.setRefname(refname);
		return refType;
	}
	
	protected CompareConstraint createCompareConstraint(CompareFeature operation, ValueReference leftOperand, ValueReference rightOperand ) {
		CompareConstraint constr = PatternLanguageFactory.eINSTANCE.createCompareConstraint();
		constr.setFeature(operation);
		constr.setLeftOperand(leftOperand);
		constr.setRightOperand(rightOperand);
		return constr;
	}
	
	protected LiteralValueReference createLiteralValueReference(Object value) {
		if(value instanceof String) {
			StringValue stringVal = PatternLanguageFactory.eINSTANCE.createStringValue();
			stringVal.setValue((String) value);
			return stringVal;
		}
		else if(value instanceof Integer) {
			XNumberLiteral xnumlit = XbaseFactoryImpl.eINSTANCE.createXNumberLiteral();
			NumberValue intValue = PatternLanguageFactory.eINSTANCE.createNumberValue();
			if((Integer) value < 0)
				intValue.setNegative(true);
			xnumlit.setValue(value.toString());
			intValue.setValue(xnumlit);
			return intValue;
			}
		else if(value instanceof java.lang.Boolean) {
			BoolValue boolValue = PatternLanguageFactory.eINSTANCE.createBoolValue();
			XBooleanLiteral xBoolLit = XbaseFactoryImpl.eINSTANCE.createXBooleanLiteral();
			xBoolLit.setIsTrue((boolean) value);
			boolValue.setValue(xBoolLit);
			return boolValue;
		}
		else throw new IllegalArgumentException("IlleagalArgument: Parameter " + value + " has to be from Type String, Integer or Boolean");
	}
	
	/**
	 *  Returns a Set with IQuerySpecification {@link IQuerySpecification} transformed out of a IBeXPatternSet, all ContextPatterns and ContextAlternatives will be transformed into
	 *  Viatra Patterns and the from all Patterns the Specification will be created. 
	 *  A ViatraQueryEngine {@link ViatraQueryEngine} can get these Specifications to generate Matchers or rather Matches
	 *
	 * @param  patternSet  an IBeXPatternSet containing all Patterns which will be transformed to Viatra IQuerySpecification
	 * @return      Set of all transformed Patterns into Viatra IQuerySpecification
	 */
	public Set<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>> transformIBeXToViatra(IBeXPatternSet patternSet) throws Exception {
		viatraPatterns = new HashMap<String, Pattern>();
		viatraSpecifications = new HashSet<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>>();
		ArrayList<IBeXContextAlternatives> contextAlternatives = new ArrayList<IBeXContextAlternatives>();
		
		for(IBeXContext context : patternSet.getContextPatterns()) {
			if(context instanceof IBeXContextPattern) {
				//Creates a new ViatraPattern from the IBeX-Model and put it on a List with all Patterns from the Resource
					iBexPatternToViatraPattern((IBeXContextPattern) context);
					parameters.clear();
					body = null;
			}
			if(context instanceof IBeXContextAlternatives) {
				contextAlternatives.add((IBeXContextAlternatives) context);
				parameters.clear();
			}
		}
		for(IBeXContextAlternatives alternative : contextAlternatives) {
				iBeXContextAlternativesToViatraSchema(alternative);
				parameters.clear();
		}
		return viatraSpecifications;
	}
	
	/**
	 *  Returns a IQuerySpecification {@link IQuerySpecification} transformed out of a Pattern {@link Pattern} , 
	 *  it is possible to add additional IExpressionEvaluator {@link IExpressionEvaluator} to the IQuerySpecification
	 *
	 * @param  pattern Pattern to create a IQuerySpecification from 
	 * @param  expressions A Collection of additional IExpressionEvaluator which can be added to the IQuerySpecification, can be Null
	 * @return      the created Viatra IQuerySpecification out of the Pattern
	 */
	public IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> buildSpecification(Pattern pattern, Collection<IExpressionEvaluator> expressions) {
		if(builder == null)
			return null;
		IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> specification = builder.getOrCreateSpecification(pattern, true);
		if(expressions != null && !expressions.isEmpty()) {
			specification.getInternalQueryRepresentation().getDisjunctBodies().getBodies().forEach(body -> {
	    		body.setStatus(PQueryStatus.UNINITIALIZED);
	    		expressions.forEach(exp -> new ExpressionEvaluation(body, exp, null));
	            body.setStatus(PQueryStatus.OK);
	    	});
		}
		return specification;
	}
	
	/**
	 * For each IBeXPatternInvocation, Viatra needs a call to another pattern
	 * For this you need to create PatternCall and a PatternCompositionConstraint which references to the other pattern
	 * which you can get from the InvokedPattern from the IBeXPatternInvocation
	 * 
	 * @param invocations List with IBeXPatternInvocation
	 */
	protected void iBeXInvocationsToViatraPatternCall(EList<IBeXPatternInvocation>  invocations) {
		invocations.forEach(invoc -> {
			if(!viatraPatterns.containsKey(invoc.getInvokedPattern().getName()))
				try {
					HashMap<IBeXNode, Boolean> temp_allNodes = allNodes;
					PatternBody temp_body = body;
					Collection<Variable> temp_parameters = parameters; 
					iBexPatternToViatraPattern(invoc.getInvokedPattern());
					allNodes = temp_allNodes;
					body = temp_body;
					parameters = temp_parameters;
				} catch (Exception e) {
					e.printStackTrace();
				}
			Pattern patternReference = viatraPatterns.get(invoc.getInvokedPattern().getName());

			PatternCompositionConstraint pCompConstr = PatternLanguageFactory.eINSTANCE.createPatternCompositionConstraint();
			PatternCall pCall = PatternLanguageFactory.eINSTANCE.createPatternCall();
			

			pCall.setPatternRef(patternReference);
			patternReference.getParameters().forEach(para -> {
				ValueReference ref = createVariableReference(para.getName(), para);
				pCall.getParameters().add(ref);
				Parameter locPar = createParameter(para.getName(), createClassType(para.getType().getTypename(), ((ClassType) para.getType()).getClassname()), ParameterDirection.get(0));
				parameters.add(locPar);
				ParameterRef parRef = PatternLanguageFactory.eINSTANCE.createParameterRef();
				parRef.setReferredParam(locPar);
				parRef.setName(locPar.getName());
				parRef.setType(createClassType(para.getType().getTypename(), ((ClassType) para.getType()).getClassname()));
				body.getVariables().add(parRef);
			});
			if(!invoc.isPositive()) {
				pCompConstr.setNegative(true);
			}
			pCompConstr.setCall(pCall);
			body.getConstraints().add(pCompConstr); 
		});
	}
	
	public void iBeXContextAlternativesToViatraSchema(IBeXContextAlternatives alternative) {	
		alternative.getAlternativePatterns().forEach(p -> {
			try {
				iBexPatternToViatraPattern(p);
				parameters.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	protected ArrayList<IExpressionEvaluator> IBeXConstraintToViatraConstraint(EList<IBeXAttributeConstraint> constraints) {
		ArrayList<String> pathExpressions = new ArrayList<String>();
		ArrayList<IExpressionEvaluator> iExpressions = new ArrayList<IExpressionEvaluator>();
		IBeXAttributeValue attValue;
		EClass nodeType_Constr;
		String nodeName_Constr;
		String pathExpression;
		int i = 0;
		for(IBeXAttributeConstraint ibexConstr : constraints) {
			nodeType_Constr = (EClass) ibexConstr.getNode().getType();
			nodeName_Constr = ibexConstr.getNode().getName();
			attValue = ibexConstr.getValue();
			ClassType type_Constr = createClassType(nodeType_Constr.getName(), nodeType_Constr);
			Variable var_Constr = createLocalVariable(nodeName_Constr, type_Constr);
			VariableReference varRef_Constr = createVariableReference(nodeName_Constr, var_Constr);
			String individualVarName;
			String varName = "var_" + i;
			if(attValue instanceof IBeXAttributeExpression) {
				IBeXAttributeExpression exp = (IBeXAttributeExpression) attValue;
				EAttribute eAttValue = exp.getAttribute();
				boolean checkConstrNeeded = true;
				String literal = ibexConstr.getRelation().getLiteral();
				if(literal == "EQUAL" || literal == "UNEQUAL")
					checkConstrNeeded = false;
			
				nodeType_Constr = (EClass) ibexConstr.getNode().getType();
				nodeName_Constr = ibexConstr.getNode().getName();
				String nodeName_Exp = exp.getNode().getName();
				EClass nodeType_Exp = exp.getNode().getType();
				
				PathExpressionConstraint pathExpCONSTR = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
				PathExpressionConstraint pathExpEXP = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
				
				ReferenceType refType_PathExpConstr = createReferenceType(nodeType_Constr.getName(), ibexConstr.getType());
//				ReferenceType refType_PathExpEXP = createReferenceType(nodeType_Exp.getName(), ibexConstr.getType());
				ReferenceType refType_PathExpEXP = createReferenceType(eAttValue.getName(), eAttValue);
				Variable var_PathExpEXP = createLocalVariable(nodeName_Exp, createClassType(nodeType_Exp.getName(), nodeType_Exp));
				LocalVariable locVarDst_PathExpConstr;
				LocalVariable locVarDst_PathExpEXP;
				if(checkConstrNeeded) {
					individualVarName = nodeName_Constr + "_" + i;
					locVarDst_PathExpConstr = createLocalVariable(individualVarName, createReferenceType(nodeType_Constr.getName(), ibexConstr.getType()));
					pathExpCONSTR.setDst(createVariableReference(individualVarName, locVarDst_PathExpConstr));
					
					individualVarName = nodeName_Exp + "_" + i;
//					locVarDst_PathExpEXP = createLocalVariable(individualVarName, createReferenceType(nodeType_Exp.getName(), ibexConstr.getType()));
					locVarDst_PathExpEXP = createLocalVariable(individualVarName, createReferenceType(eAttValue.getName(), eAttValue));
					pathExpEXP.setDst(createVariableReference(individualVarName, locVarDst_PathExpEXP));
				}
				else {
					locVarDst_PathExpConstr = createLocalVariable(varName, createReferenceType(nodeType_Constr.getName(), ibexConstr.getType()));
					pathExpCONSTR.setDst(createVariableReference(varName, locVarDst_PathExpConstr));
					
//					locVarDst_PathExpEXP = createLocalVariable("s", createReferenceType(nodeType_Exp.getName(), ibexConstr.getType()));
					locVarDst_PathExpEXP = createLocalVariable(varName, createReferenceType(eAttValue.getName(), eAttValue));
					pathExpEXP.setDst(createVariableReference(varName, locVarDst_PathExpEXP));
				}
				pathExpCONSTR.getEdgeTypes().add(refType_PathExpConstr);
				pathExpCONSTR.setSourceType(createClassType(nodeName_Constr, nodeType_Constr));
				pathExpCONSTR.setSrc(createVariableReference(nodeName_Constr, var_Constr));
				
				pathExpEXP.getEdgeTypes().add(refType_PathExpEXP);
				pathExpEXP.setSourceType(createClassType(nodeName_Exp, nodeType_Exp));
				pathExpEXP.setSrc(createVariableReference(nodeName_Exp, var_PathExpEXP));
				
				pathExpression = pathExpCONSTR.toString();
				pathExpressions.add(pathExpression);
				pathExpression = pathExpEXP.toString();
				pathExpressions.add(pathExpression);
				
				ClassType typ = createClassType(nodeType_Constr.getName(), nodeType_Constr);
				ValueReference leftOp = createVariableReference(nodeName_Constr, createParameter(nodeName_Constr, typ, ParameterDirection.get(0)));
				ValueReference rightOp = createVariableReference(nodeName_Exp, 
						createParameter(nodeName_Exp, 
								createClassType(nodeName_Exp, nodeType_Exp), ParameterDirection.get(0)));
				CompareConstraint constr = createCompareConstraint(CompareFeature.get(1), leftOp, rightOp);
				
				switch(literal) {
				case "UNEQUAL":
					constr.setFeature(CompareFeature.get(0));
					body.getConstraints().add(constr);
					break;
				case "EQUAL":
					body.getConstraints().add(constr);
					break;
				case "SMALLER" :
					iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {locVarDst_PathExpConstr.getName(), locVarDst_PathExpEXP.getName()} , null, FunctionType.PARAMETER_ONLY, Operator.SMALLER));
					break;
				case "SMALLER_OR_EQUAL" :
					iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {locVarDst_PathExpConstr.getName(), locVarDst_PathExpEXP.getName()} , null, FunctionType.PARAMETER_ONLY, Operator.SMALLER_OR_EQUAL));
					break;
				case "GREATER" :
					iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {locVarDst_PathExpConstr.getName(), locVarDst_PathExpEXP.getName()} , null, FunctionType.PARAMETER_ONLY, Operator.GREATER));
					break;
				case "GREATER_OR_EQUAL" :
					iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {locVarDst_PathExpConstr.getName(), locVarDst_PathExpEXP.getName()} , null, FunctionType.PARAMETER_ONLY, Operator.GREATER_OR_EQUAL));
					break;
				}
				body.getConstraints().add(pathExpCONSTR);
				
				if(allNodes.get(ibexConstr.getNode())) {
					Parameter par_Constr = createParameter(nodeName_Constr, createClassType(nodeType_Constr.getName(), nodeType_Constr), ParameterDirection.get(0));
					parameters.add(par_Constr);
					var_Constr = (ParameterRef) PatternLanguageFactory.eINSTANCE.createParameterRef();
					((ParameterRef) var_Constr).setReferredParam(par_Constr);
					var_Constr.setName(nodeName_Constr);
					var_Constr.setType(createClassType(nodeType_Constr.getName(), nodeType_Constr));
				}
				if(allNodes.containsKey(exp.getNode()) && allNodes.get(exp.getNode())) {
					Parameter par_Exp = createParameter(nodeName_Exp, createClassType(nodeType_Exp.getName(), nodeType_Exp), ParameterDirection.get(0));
					parameters.add(par_Exp);
					var_PathExpEXP = (ParameterRef) PatternLanguageFactory.eINSTANCE.createParameterRef();
					((ParameterRef) var_PathExpEXP).setReferredParam(par_Exp);
					var_PathExpEXP.setName(nodeName_Exp);
					var_PathExpEXP.setType(createClassType(nodeType_Exp.getName(), nodeType_Exp));
				}
				body.getVariables().add(var_Constr);
				body.getVariables().add(locVarDst_PathExpConstr);
				body.getVariables().add(var_PathExpEXP);
				body.getVariables().add(locVarDst_PathExpEXP);
				if(allNodes.containsKey(exp.getNode())) {
					body.getConstraints().add(pathExpEXP);
				}
			}
			else if(attValue instanceof IBeXConstant) {
			IBeXConstant con = (IBeXConstant) attValue;
			PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
			ReferenceType refType = createReferenceType(nodeType_Constr.getName(), ibexConstr.getType());
			LocalVariable locVarDst = createLocalVariable(varName, refType);
			
			pathExp.setDst(createVariableReference(varName, locVarDst));
			pathExp.getEdgeTypes().add(createReferenceType(ibexConstr.getType().getName(), ibexConstr.getType()));
			pathExp.setSourceType(createClassType(nodeName_Constr, nodeType_Constr));
			pathExp.setSrc(varRef_Constr);
			pathExpressions.add(pathExp.toString());
			
			switch(ibexConstr.getRelation().getLiteral()) {
			case "EQUAL" :
				pathExp.setDst(createLiteralValueReference(con.getValue()));
				break;
			case "UNEQUAL" :
				iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {varName}, new Object[] {con.getValue()}, FunctionType.PARAMETER_AND_VALUE, Operator.UNEQUAL));
				break;
			case "SMALLER" :
				iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {varName}, new Object[] {con.getValue()}, FunctionType.PARAMETER_AND_VALUE, Operator.SMALLER));
				break;
			case "SMALLER_OR_EQUAL" :
				iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {varName}, new Object[] {con.getValue()}, FunctionType.PARAMETER_AND_VALUE, Operator.SMALLER_OR_EQUAL));
				break;
			case "GREATER_OR_EQUAL" :
				iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {varName}, new Object[] {con.getValue()}, FunctionType.PARAMETER_AND_VALUE, Operator.GREATER_OR_EQUAL));
				break;
			case "GREATER" :
				iExpressions.add(expressionEvaluatorBuilder(ibexConstr.getType().getEType().getInstanceClass(), new String[] {varName}, new Object[] {con.getValue()}, FunctionType.PARAMETER_AND_VALUE, Operator.GREATER));
				break;
			}
			if(allNodes.get(ibexConstr.getNode())) {
				Parameter par_Constr = createParameter(nodeName_Constr, createClassType(nodeType_Constr.getName(), nodeType_Constr), ParameterDirection.get(0));
				parameters.add(par_Constr);
				var_Constr = (ParameterRef) PatternLanguageFactory.eINSTANCE.createParameterRef();
				((ParameterRef) var_Constr).setReferredParam(par_Constr);
				var_Constr.setName(nodeName_Constr);
				var_Constr.setType(createClassType(nodeType_Constr.getName(), nodeType_Constr));
			}
			body.getConstraints().add(pathExp);
			body.getVariables().add(locVarDst);
			body.getVariables().add(var_Constr);
			}
			
			else if(attValue instanceof IBeXAttributeParameter) {
			body.getVariables().add(var_Constr);	
			}
			
			else if(attValue instanceof IBeXEnumLiteral) {
				EEnumLiteral enumLiteral = ((IBeXEnumLiteral) attValue).getLiteral();
				PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
				EnumValue eValue = PatternLanguageFactory.eINSTANCE.createEnumValue();
				eValue.setLiteral(enumLiteral);
				eValue.setEnumeration(enumLiteral.getEEnum());
				pathExp.setSourceType(createClassType(nodeName_Constr, nodeType_Constr));
				pathExp.setSrc(varRef_Constr);
				pathExp.setDst(eValue);
				pathExp.getEdgeTypes().add(createReferenceType(ibexConstr.getType().getName(), ibexConstr.getType()));
				body.getConstraints().add(pathExp);
				}
			i++;
		}
		return iExpressions;
	}
	
	protected void iBexEdgeToViatraSchema(EList<IBeXEdge> edges) {
		for(IBeXEdge edge : edges) {
			PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
			ReferenceType refType = createReferenceType(edge.getType().getName(), edge.getType());
			LocalVariable locVarDst = createLocalVariable(edge.getTargetNode().getName(), refType);
			LocalVariable locVarSrc = createLocalVariable(edge.getSourceNode().getName(), refType);
			pathExp.getEdgeTypes().add(refType);
			pathExp.setSourceType(createClassType(edge.getSourceNode().getName(), edge.getSourceNode().getType()));
			pathExp.setDst(createVariableReference(edge.getTargetNode().getName(), locVarDst));
			pathExp.setSrc(createVariableReference(edge.getSourceNode().getName(), locVarSrc));
			if(allNodes.containsKey(edge.getSourceNode())) {
				body.getVariables().add(locVarSrc);
			}
			if(allNodes.containsKey(edge.getTargetNode())) {
				body.getVariables().add(locVarDst);
			}
			body.getConstraints().add(pathExp);
		}
	}
	
	protected List<Constraint> iBeXInjectiveConstraintToViatra(EList<IBeXNodePair> pairs) {
		ArrayList<Constraint> ret = new ArrayList<Constraint>();
		//Iteration through all NodePairs and create CompareConstraint for all possible combinations (no duplicates)
		for(IBeXNodePair pair : pairs) {
			for(int i = 0; i < pair.getValues().size(); i++) {
				for(int j = i+1; j < pair.getValues().size() ; j++) {
					IBeXNode tempLeft = pair.getValues().get(i);
					IBeXNode tempRight = pair.getValues().get(j);
					ValueReference leftOp = createVariableReference(tempLeft.getName(), createParameter(tempLeft.getName(), createClassType(tempLeft.getType().getName(), tempLeft.getType()), ParameterDirection.get(1)));
					ValueReference rightOp = createVariableReference(tempRight.getName(), createParameter(tempRight.getName(), createClassType(tempRight.getType().getName(), tempRight.getType()), ParameterDirection.get(1)));
					ret.add(createCompareConstraint(CompareFeature.get(1), leftOp, rightOp));
				}
			}
		}
		return ret;
	}
	
	/**
	 * Method to transform a IBeXPattern into the Viatra-VQL-schema-model
	 * @param pattern The IBeXContextPattern which will be transformed
	 */
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
		
//		expressions.addAll(iBeXCSPToViatra(pattern.getCsps()));
		
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
	
	/**
	 * Removes duplicate variables by there name
	 * Special case there can be two equal variables
	 * 
	 * @param variables The Collection with variables 
	 */
	protected Collection<Variable> removeDubsInVariables(Collection<Variable> variables) {
		variables = variables.stream()
                .collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(Variable::getName))),
                                           ArrayList::new));
		return variables;
	}
	
	/**
	 * building/transforming a ViatraPattern out of IBeX informations
	 * 
	 * @param viatraPattern
	 * @param patternName
	 */
	public Pattern setViatraPattern(Pattern viatraPattern, String patternName) {
		viatraPattern.setName(patternName);
		//Add Parameters, Bodies, Annotations to the ViatraPattern (patternlanguage.emf.vql.Pattern)
		parameters = removeDubsInVariables(parameters);
		viatraPattern.getParameters().addAll(parameters);
		viatraPattern.getBodies().add(body);
		return viatraPattern;
	}
	
	/**
	 * The parameters: parameters and values needs to have a length of two and all additionally Elements will be ignored
	 * only works for int, boolean and String as Types
	 * 
	 * @param javaType
	 * @param parameters Array of the parameters i.e. name of the parameters, can contain up to 2 Strings
	 * @param values Array of the values, can contain up to 2 Objects
	 * @param fType 
	 * @param op  
	 */
	protected IExpressionEvaluator expressionEvaluatorBuilder(Class<?> javaType, String[] parameters, Object[] values, FunctionType fType, Operator op) {
		return new IExpressionEvaluatorBuilder().expressionEvaluatorBuilder(javaType, parameters, values, fType, op);
	}
}