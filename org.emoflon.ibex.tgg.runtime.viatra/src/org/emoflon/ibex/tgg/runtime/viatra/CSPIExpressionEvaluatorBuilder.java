package org.emoflon.ibex.tgg.runtime.viatra;

import java.util.List;

import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;
import org.eclipse.xtext.xbase.lib.Pure;
import org.emoflon.ibex.gt.viatra.runtime.IExpressionEvaluatorBuilder;
import org.emoflon.ibex.tgg.operational.csp.RuntimeTGGAttributeConstraintVariable;

import IBeXLanguage.IBeXAttributeExpression;
import IBeXLanguage.IBeXAttributeValue;
import IBeXLanguage.IBeXCSP;
import IBeXLanguage.IBeXConstant;

public class CSPIExpressionEvaluatorBuilder extends IExpressionEvaluatorBuilder {
	
	protected IExpressionEvaluator iBeXCSPExpressionEvaluatorBuilder(IBeXCSP csp, List<String> inputParameterNames, int numberOfCsp) {
		return new IExpressionEvaluator() {
			
			@Override
			public String getShortDescription() {
				return "IExpressionEvaluator for IBeXCSP"; 
			}
			
			@Override
			public Iterable<String> getInputParameterNames() {
				return inputParameterNames;
			}
			
			@Override
			public Object evaluateExpression(IValueProvider provider) throws Exception {
				return iBeXCSPToCodeTransformation(csp, provider, numberOfCsp);
			}
		};
	}
	
	/**
	 *  Creates method calls out of informations from the IBeXCSP, these informations are saved in Strings, so the method is sensible for exceptions
	 *  Attention the IBeXCSP has to have a valid name and package, which needs to refer to an actual Class otherwise exceptions will be thrown
	 *  
	 *  @param csp The IBeXCSP
	 *  @param provider The IValueProvider
	 */
	@Pure
	@SuppressWarnings({ "deprecation", "unchecked" })
	private boolean iBeXCSPToCodeTransformation(IBeXCSP csp, IValueProvider provider, int numberOfCsp) throws Exception {
		Class<?> clazz = Class.forName(csp.getPackage() + "." + getCSPName(csp.getName()));
		Object csp_object = clazz.newInstance();
		List<RuntimeTGGAttributeConstraintVariable> cspVariables = (List<RuntimeTGGAttributeConstraintVariable>) clazz.getSuperclass().getDeclaredMethod("getVariables").invoke(csp_object, null);
		for(IBeXAttributeValue value : csp.getValues()) {
               if(value instanceof IBeXAttributeExpression ) {
            	   IBeXAttributeExpression iExpr = (IBeXAttributeExpression) value;
            	   cspVariables.add(new org.emoflon.ibex.tgg.operational.csp.RuntimeTGGAttributeConstraintVariable(true, provider.getValue(iExpr.getNode().getName() + "_CSP_" + numberOfCsp), (iExpr.getAttribute().getEType().getInstanceClassName())));

               }
               else if(value instanceof IBeXConstant) {
                   IBeXConstant iConst= (IBeXConstant) value;
                   //TODO fix me need to cut the first and last Char of the String because it's "
                   String iConstStringValue = iConst.getStringValue();
                   cspVariables.add(new org.emoflon.ibex.tgg.operational.csp.RuntimeTGGAttributeConstraintVariable(true, iConstStringValue.substring(1, iConstStringValue.length() - 1), (iConst.getValue().getClass().getName())));
               }
        }
		clazz.getDeclaredMethod("solve").invoke(csp_object, null);
		boolean satisfied = (boolean) clazz.getSuperclass().getDeclaredMethod("isSatisfied").invoke(csp_object, null);
        return satisfied;
	}
	
	private String getCSPName(String name) {
		if(name.startsWith("eq_"))
			return "Eq";
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}
}
