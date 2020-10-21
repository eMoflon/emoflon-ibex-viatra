package org.emoflon.ibex.gt.viatra.runtime;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.viatra.query.patternlanguage.emf.specification.SpecificationBuilder;
import org.eclipse.viatra.query.patternlanguage.emf.vql.BoolValue;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ClassType;
import org.eclipse.viatra.query.patternlanguage.emf.vql.CompareConstraint;
import org.eclipse.viatra.query.patternlanguage.emf.vql.CompareFeature;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Constraint;
import org.eclipse.viatra.query.patternlanguage.emf.vql.EnumValue;
import org.eclipse.viatra.query.patternlanguage.emf.vql.LiteralValueReference;
import org.eclipse.viatra.query.patternlanguage.emf.vql.LocalVariable;
import org.eclipse.viatra.query.patternlanguage.emf.vql.NumberValue;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Parameter;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ParameterDirection;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ParameterRef;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PathExpressionConstraint;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternBody;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternCall;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternCompositionConstraint;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguageFactory;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ReferenceType;
import org.eclipse.viatra.query.patternlanguage.emf.vql.StringValue;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Type;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ValueReference;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Variable;
import org.eclipse.viatra.query.patternlanguage.emf.vql.VariableReference;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.ExpressionEvaluation;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery.PQueryStatus;
import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.impl.XbaseFactoryImpl;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXArithmeticValue;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXAttributeConstraint;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXAttributeExpression;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXAttributeParameter;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXAttributeValue;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXConstant;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContext;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextAlternatives;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXEdge;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXEnumLiteral;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXInjectivityConstraint;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXNode;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXPatternInvocation;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXPatternSet;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXRelation;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXStochasticAttributeValue;

/**
 * Class which is used to transform IBeX-Model into Viatra-Model
 * 
 * @author Julian Barthel
 * @version 1.0
 * @since 1.0
 */
@SuppressWarnings({ "restriction" })
public class IBeXToViatraPatternTransformation {

	protected PatternBody body;
	protected Collection<Variable> parameters;
	protected HashMap<String, Pattern> viatraPatterns;
	protected Set<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>> viatraSpecifications;
	protected HashMap<IBeXNode, Boolean> allNodes;
	protected SpecificationBuilder builder;
	protected HashMap<String, Variable> variables;
	protected Pattern patternToResolve;

	public IBeXToViatraPatternTransformation(SpecificationBuilder builder) {
		this.builder = builder;
	}

	public IBeXToViatraPatternTransformation() {
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

	protected CompareConstraint createCompareConstraint(CompareFeature operation, ValueReference leftOperand,
			ValueReference rightOperand) {
		CompareConstraint constr = PatternLanguageFactory.eINSTANCE.createCompareConstraint();
		constr.setFeature(operation);
		constr.setLeftOperand(leftOperand);
		constr.setRightOperand(rightOperand);
		return constr;
	}

	protected LiteralValueReference createLiteralValueReference(Object value) {
		if (value instanceof String) {
			StringValue stringVal = PatternLanguageFactory.eINSTANCE.createStringValue();
			stringVal.setValue((String) value);
			return stringVal;
		} else if (value instanceof Integer) {
			XNumberLiteral xnumlit = XbaseFactoryImpl.eINSTANCE.createXNumberLiteral();
			NumberValue intValue = PatternLanguageFactory.eINSTANCE.createNumberValue();
			if ((Integer) value < 0)
				intValue.setNegative(true);
			xnumlit.setValue(value.toString());
			intValue.setValue(xnumlit);
			return intValue;
		} else if (value instanceof java.lang.Boolean) {
			BoolValue boolValue = PatternLanguageFactory.eINSTANCE.createBoolValue();
			XBooleanLiteral xBoolLit = XbaseFactoryImpl.eINSTANCE.createXBooleanLiteral();
			xBoolLit.setIsTrue((boolean) value);
			boolValue.setValue(xBoolLit);
			return boolValue;
		} else
			throw new IllegalArgumentException(
					"IlleagalArgument: Parameter " + value + " has to be from Type String, Integer or Boolean");
	}

	/**
	 * Returns a Set with IQuerySpecification {@link IQuerySpecification}
	 * transformed out of a IBeXPatternSet, all ContextPatterns and
	 * ContextAlternatives will be transformed into Viatra Patterns and the from all
	 * Patterns the Specification will be created. A ViatraQueryEngine
	 * {@link ViatraQueryEngine} can get these Specifications to generate Matchers
	 * or rather Matches
	 *
	 * @param patternSet an IBeXPatternSet containing all Patterns which will be
	 *                   transformed to Viatra IQuerySpecification
	 * @return Set of all transformed Patterns into Viatra IQuerySpecification
	 */
	public Set<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>> transformIBeXToViatra(
			IBeXPatternSet patternSet) throws Exception {
		viatraPatterns = new HashMap<String, Pattern>();
		viatraSpecifications = new HashSet<IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>>();
		ArrayList<IBeXContextAlternatives> contextAlternatives = new ArrayList<IBeXContextAlternatives>();

		for (IBeXContext context : patternSet.getContextPatterns()) {
			if (context instanceof IBeXContextPattern) {
				// Creates a new ViatraPattern from the IBeX-Model and put it on a List with all
				// Patterns from the Resource
				iBexPatternToViatraPattern((IBeXContextPattern) context);
				parameters.clear();
				body = null;
			}
			if (context instanceof IBeXContextAlternatives) {
				contextAlternatives.add((IBeXContextAlternatives) context);
				parameters.clear();
			}
		}
		for (IBeXContextAlternatives alternative : contextAlternatives) {
			iBeXContextAlternativesToViatraSchema(alternative);
			parameters.clear();
		}
		return viatraSpecifications;
	}

	/**
	 * Returns a IQuerySpecification {@link IQuerySpecification} transformed out of
	 * a Pattern {@link Pattern} , it is possible to add additional
	 * IExpressionEvaluator {@link IExpressionEvaluator} to the IQuerySpecification
	 *
	 * @param pattern     Pattern to create a IQuerySpecification from
	 * @param expressions A Collection of additional IExpressionEvaluator which can
	 *                    be added to the IQuerySpecification, can be Null
	 * @return the created Viatra IQuerySpecification out of the Pattern
	 */
	public IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> buildSpecification(
			Pattern pattern, Collection<IExpressionEvaluator> expressions) {
		if (builder == null)
			return null;
		IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> specification = builder
				.getOrCreateSpecification(pattern, true);
		if (expressions != null && !expressions.isEmpty()) {
			specification.getInternalQueryRepresentation().getDisjunctBodies().getBodies().forEach(body -> {
				body.setStatus(PQueryStatus.UNINITIALIZED);
				expressions.forEach(exp -> new ExpressionEvaluation(body, exp, null));
				body.setStatus(PQueryStatus.OK);
			});
		}
		return specification;
	}

	/**
	 * For each IBeXPatternInvocation, Viatra needs a call to another pattern For
	 * this you need to create PatternCall and a PatternCompositionConstraint which
	 * references to the other pattern which you can get from the InvokedPattern
	 * from the IBeXPatternInvocation
	 * 
	 * @param invocations List with IBeXPatternInvocation
	 */
	protected void iBeXInvocationsToViatraPatternCall(EList<IBeXPatternInvocation> invocations,
			IBeXContextPattern patternToTransform) {
		invocations.forEach(invoc -> {
			if (!viatraPatterns.containsKey(invoc.getInvokedPattern().getName()))
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

			PatternCompositionConstraint pCompConstr = PatternLanguageFactory.eINSTANCE
					.createPatternCompositionConstraint();
			PatternCall pCall = PatternLanguageFactory.eINSTANCE.createPatternCall();

			pCall.setPatternRef(patternReference);

			// In Viatra a patternCall (PatternInvocation) acts like a method call
			// Therefore it is important that the order of parameters is correct
			patternReference.getParameters().forEach(para -> {
				String paraName = para.getName();
				// iterate throw all entry's of node mapping to find the node of the called
				// IBeX-pattern that equals the parameter of the called Viatra-Pattern
				for (Map.Entry<IBeXNode, IBeXNode> nodeEntry : invoc.getMapping()) {
					if (nodeEntry.getValue().getName().contentEquals(paraName)) {
						boolean callerNodeisLocal = false;
						String callerNodeName = nodeEntry.getKey().getName();
						// check if the node is a local node so that just a variable will be created
						for (IBeXNode localN : patternToTransform.getLocalNodes()) {
							if (localN.getName() == callerNodeName)
								;
							callerNodeisLocal = true;
						}
						if (callerNodeisLocal) {
							LocalVariable locVar = createLocalVariable(callerNodeName, createClassType(
									para.getType().getTypename(), ((ClassType) para.getType()).getClassname()));
							PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE
									.createPathExpressionConstraint();
							pathExp.setDst(createVariableReference(callerNodeName, locVar));
							pathExp.setSourceType(createClassType(para.getType().getTypename(),
									((ClassType) para.getType()).getClassname()));
							pathExp.setSrc(createVariableReference(callerNodeName, locVar));
							ValueReference ref = createVariableReference(callerNodeName, locVar);
							pCall.getParameters().add(ref);
							body.getConstraints().add(pathExp);
							body.getVariables().add(locVar);
						} else {
							Parameter locPar = createParameter(callerNodeName,
									createClassType(para.getType().getTypename(),
											((ClassType) para.getType()).getClassname()),
									ParameterDirection.get(0));
							parameters.add(locPar);
							ParameterRef parRef = PatternLanguageFactory.eINSTANCE.createParameterRef();
							parRef.setReferredParam(locPar);
							parRef.setName(locPar.getName());
							parRef.setType(createClassType(para.getType().getTypename(),
									((ClassType) para.getType()).getClassname()));
							body.getVariables().add(parRef);
							ValueReference ref = createVariableReference(callerNodeName, locPar);
							pCall.getParameters().add(ref);
							break;
						}
					}
				}
			});

			if (!invoc.isPositive()) {
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

	protected LocalVariable addNewAttributeVariable(final IBeXAttributeExpression ibexExpr) {
		PathExpressionConstraint attributePathExpression = PatternLanguageFactory.eINSTANCE
				.createPathExpressionConstraint();

		Variable nodeVariable = createLocalVariable(ibexExpr.getNode().getName(),
				createClassType(ibexExpr.getNode().getType().getName(), ibexExpr.getNode().getType()));
		body.getVariables().add(nodeVariable);
		attributePathExpression
				.setSourceType(createClassType(ibexExpr.getNode().getName(), ibexExpr.getNode().getType()));
		attributePathExpression.setSrc(createVariableReference(ibexExpr.getNode().getName(), nodeVariable));

		ReferenceType attributeType = createReferenceType(ibexExpr.getAttribute().getName(), ibexExpr.getAttribute());
		LocalVariable attributeVariable = createLocalVariable(
				ibexExpr.getNode().getName() + "_" + ibexExpr.getAttribute().getName(), attributeType);
		body.getVariables().add(attributeVariable);
		attributePathExpression.getEdgeTypes().add(attributeType);
		attributePathExpression.setDst(createVariableReference(
				ibexExpr.getNode().getName() + "_" + ibexExpr.getAttribute().getName(), attributeVariable));
		body.getConstraints().add(attributePathExpression);

		if (allNodes.containsKey(ibexExpr.getNode()) && allNodes.get(ibexExpr.getNode())) {
			Parameter parameter = createParameter(ibexExpr.getNode().getName(),
					createClassType(ibexExpr.getNode().getType().getName(), ibexExpr.getNode().getType()),
					ParameterDirection.INOUT);
			parameters.add(parameter);
			ParameterRef parameterRef = PatternLanguageFactory.eINSTANCE.createParameterRef();
			parameterRef.setReferredParam(parameter);
			parameterRef.setName(ibexExpr.getNode().getName());
			parameterRef.setType(createClassType(ibexExpr.getNode().getType().getName(), ibexExpr.getNode().getType()));
			body.getVariables().add(parameterRef);
		}

		return attributeVariable;
	}

	protected Collection<IExpressionEvaluator> IBeXConstraintToViatraConstraint(
			List<IBeXAttributeConstraint> constraints) {
		Set<IExpressionEvaluator> iExpressions = new LinkedHashSet<IExpressionEvaluator>();

		for (IBeXAttributeConstraint ibexConstraint : constraints) {
			IBeXAttributeValue lhs = ibexConstraint.getLhs();
			IBeXAttributeValue rhs = ibexConstraint.getRhs();
			if (lhs instanceof IBeXAttributeParameter || rhs instanceof IBeXAttributeParameter)
				continue;
			if (lhs instanceof IBeXStochasticAttributeValue || rhs instanceof IBeXStochasticAttributeValue)
				continue;
			if (lhs instanceof IBeXArithmeticValue || rhs instanceof IBeXArithmeticValue)
				continue;

			if (lhs instanceof IBeXConstant && rhs instanceof IBeXConstant) {
				iExpressions.add(IExpressionEvaluatorBuilder.expressionEvaluatorBuilder((IBeXConstant) lhs,
						(IBeXConstant) rhs, ibexConstraint.getRelation()));
			} else if (lhs instanceof IBeXEnumLiteral && rhs instanceof IBeXEnumLiteral) {
				iExpressions.add(IExpressionEvaluatorBuilder.expressionEvaluatorBuilder((IBeXEnumLiteral) lhs,
						(IBeXEnumLiteral) rhs, ibexConstraint.getRelation()));
			} else if (lhs instanceof IBeXAttributeExpression && rhs instanceof IBeXAttributeExpression) {
				IBeXAttributeExpression lhsExpr = (IBeXAttributeExpression) lhs;
				IBeXAttributeExpression rhsExpr = (IBeXAttributeExpression) rhs;

				LocalVariable lhsParam = addNewAttributeVariable(lhsExpr);
				LocalVariable rhsParam = addNewAttributeVariable(rhsExpr);

				iExpressions.add(IExpressionEvaluatorBuilder.expressionEvaluatorBuilder(lhsExpr, rhsExpr,
						lhsParam.getName(), rhsParam.getName(), ibexConstraint.getRelation()));
			} else if ((lhs instanceof IBeXAttributeExpression && !(rhs instanceof IBeXAttributeExpression))
					|| (!(lhs instanceof IBeXAttributeExpression) && rhs instanceof IBeXAttributeExpression)) {
				IBeXAttributeExpression lhsExpr = null;
				LocalVariable param = null;
				IBeXAttributeValue rhsExpr = null;
				IBeXRelation relation = null;

				if (lhs instanceof IBeXAttributeExpression) {
					lhsExpr = (IBeXAttributeExpression) lhs;
					rhsExpr = rhs;
					relation = ibexConstraint.getRelation();
				} else {
					lhsExpr = (IBeXAttributeExpression) rhs;
					rhsExpr = lhs;
					relation = invertRelation(ibexConstraint.getRelation());
				}
				param = addNewAttributeVariable(lhsExpr);

				if (rhsExpr instanceof IBeXConstant) {
					iExpressions.add(IExpressionEvaluatorBuilder.expressionEvaluatorBuilder(lhsExpr,
							(IBeXConstant) rhsExpr, param.getName(), relation));
				} else {
					iExpressions.add(IExpressionEvaluatorBuilder.expressionEvaluatorBuilder(lhsExpr,
							(IBeXEnumLiteral) rhsExpr, param.getName(), relation));
				}

			}
		}
		return iExpressions;
	}

	public IBeXRelation invertRelation(IBeXRelation op) {
		switch (op) {
		case EQUAL:
			return IBeXRelation.EQUAL;
		case UNEQUAL:
			return IBeXRelation.UNEQUAL;
		case SMALLER:
			return IBeXRelation.GREATER;
		case SMALLER_OR_EQUAL:
			return IBeXRelation.GREATER_OR_EQUAL;
		case GREATER:
			return IBeXRelation.SMALLER;
		case GREATER_OR_EQUAL:
			return IBeXRelation.SMALLER_OR_EQUAL;
		default:
			return null;
		}
	}

	protected void iBexEdgeToViatraSchema(EList<IBeXEdge> edges) {
		for (IBeXEdge edge : edges) {
			PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE.createPathExpressionConstraint();
			ReferenceType refType = createReferenceType(edge.getType().getName(), edge.getType());
			LocalVariable locVarDst = createLocalVariable(edge.getTargetNode().getName(), refType);
			LocalVariable locVarSrc = createLocalVariable(edge.getSourceNode().getName(), refType);
			pathExp.getEdgeTypes().add(refType);
			pathExp.setSourceType(createClassType(edge.getSourceNode().getName(), edge.getSourceNode().getType()));
			pathExp.setDst(createVariableReference(edge.getTargetNode().getName(), locVarDst));
			pathExp.setSrc(createVariableReference(edge.getSourceNode().getName(), locVarSrc));
			if (allNodes.containsKey(edge.getSourceNode())) {
				body.getVariables().add(locVarSrc);
			}
			if (allNodes.containsKey(edge.getTargetNode())) {
				body.getVariables().add(locVarDst);
			}
			body.getConstraints().add(pathExp);
		}
	}

	protected List<Constraint> iBeXInjectiveConstraintToViatra(EList<IBeXInjectivityConstraint> pairs) {
		ArrayList<Constraint> ret = new ArrayList<Constraint>();
		// Iteration through all NodePairs and create CompareConstraint for all possible
		// combinations (no duplicates)
		for (IBeXInjectivityConstraint pair : pairs) {
			for (int i = 0; i < pair.getValues().size(); i++) {
				for (int j = i + 1; j < pair.getValues().size(); j++) {
					IBeXNode tempLeft = pair.getValues().get(i);
					IBeXNode tempRight = pair.getValues().get(j);
					ValueReference leftOp = createVariableReference(tempLeft.getName(),
							createParameter(tempLeft.getName(),
									createClassType(tempLeft.getType().getName(), tempLeft.getType()),
									ParameterDirection.get(1)));
					ValueReference rightOp = createVariableReference(tempRight.getName(),
							createParameter(tempRight.getName(),
									createClassType(tempRight.getType().getName(), tempRight.getType()),
									ParameterDirection.get(1)));
					ret.add(createCompareConstraint(CompareFeature.get(1), leftOp, rightOp));
				}
			}
		}
		return ret;
	}

	/**
	 * Method to transform a IBeXPattern into the Viatra-VQL-schema-model
	 * 
	 * @param pattern The IBeXContextPattern which will be transformed
	 */
	public Pattern iBexPatternToViatraPattern(IBeXContextPattern pattern) throws Exception {
		if (viatraPatterns.containsKey(pattern.getName())) {
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
		if (!pattern.getInvocations().isEmpty()) {
			iBeXInvocationsToViatraPatternCall(pattern.getInvocations(), pattern);
		}
		expressions.addAll(IBeXConstraintToViatraConstraint(pattern.getAttributeConstraint()));

		body.getConstraints().addAll(iBeXInjectiveConstraintToViatra(pattern.getInjectivityConstraints()));

		iBexEdgeToViatraSchema(pattern.getLocalEdges());

//		expressions.addAll(iBeXCSPToViatra(pattern.getCsps()));

		if (!allNodes.isEmpty()) {
			for (Map.Entry<IBeXNode, Boolean> node : allNodes.entrySet()) {
				nodeName = node.getKey().getName();
				nodeType = node.getKey().getType();
				ClassType type = createClassType(nodeType.getName(), nodeType);
				Variable var = createLocalVariable(nodeName, type);
				if (node.getValue()) {
					Parameter par = createParameter(nodeName, createClassType(nodeType.getName(), nodeType),
							ParameterDirection.get(0));
					parameters.add(par);
					var = (ParameterRef) PatternLanguageFactory.eINSTANCE.createParameterRef();
					((ParameterRef) var).setReferredParam(par);
					var.setName(nodeName);
					var.setType(createClassType(nodeType.getName(), nodeType));
				} else {
					PathExpressionConstraint pathExp = PatternLanguageFactory.eINSTANCE
							.createPathExpressionConstraint();
					pathExp.setDst(createVariableReference(nodeName, var));
					pathExp.setSourceType(createClassType(nodeType.getName(), nodeType));
					pathExp.setSrc(createVariableReference(nodeName, var));
					body.getConstraints().add(pathExp);
				}
				body.getVariables().add(var);
			}
		}
		viatraSpecifications
				.add(buildSpecification(setViatraPattern(patternToResolve, pattern.getName()), expressions));
		viatraPatterns.put(pattern.getName(), setViatraPattern(patternToResolve, pattern.getName()));
		return setViatraPattern(patternToResolve, pattern.getName());
	}

	/**
	 * Removes duplicate variables by there name Special case there can be two equal
	 * variables
	 * 
	 * @param variables The Collection with variables
	 */
	protected Collection<Variable> removeDubsInVariables(Collection<Variable> variables) {
		variables = variables.stream().collect(
				collectingAndThen(toCollection(() -> new TreeSet<>(comparing(Variable::getName))), ArrayList::new));
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
		// Add Parameters, Bodies, Annotations to the ViatraPattern
		// (patternlanguage.emf.vql.Pattern)
		parameters = removeDubsInVariables(parameters);
		viatraPattern.getParameters().addAll(parameters);
		viatraPattern.getBodies().add(body);
		return viatraPattern;
	}

//	/**
//	 * The parameters: parameters and values needs to have a length of two and all additionally Elements will be ignored
//	 * only works for int, boolean and String as Types
//	 * 
//	 * @param javaType
//	 * @param parameters Array of the parameters i.e. name of the parameters, can contain up to 2 Strings
//	 * @param values Array of the values, can contain up to 2 Objects
//	 * @param fType 
//	 * @param op  
//	 */
//	protected IExpressionEvaluator expressionEvaluatorBuilder(Class<?> javaType, String[] parameters, Object[] values, FunctionType fType, Operator op) {
//		return new IExpressionEvaluatorBuilder().expressionEvaluatorBuilder(javaType, parameters, values, fType, op);
//	}
}