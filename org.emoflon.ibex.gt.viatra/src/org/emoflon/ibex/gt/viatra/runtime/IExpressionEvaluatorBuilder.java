package org.emoflon.ibex.gt.viatra.runtime;

import java.util.Arrays;
import java.util.List;

import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;
import org.eclipse.xtext.xbase.lib.Pure;
import org.emoflon.ibex.gt.viatra.runtime.IBeXToViatraPatternTransformation.FunctionType;
import org.emoflon.ibex.gt.viatra.runtime.IBeXToViatraPatternTransformation.Operator;

import IBeXLanguage.IBeXAttributeExpression;
import IBeXLanguage.IBeXAttributeValue;
import IBeXLanguage.IBeXCSP;
import IBeXLanguage.IBeXConstant;

/**
 * Separate class to create IExpressionEvaluator
 * @author Julian Barthel
 *
 */
public class IExpressionEvaluatorBuilder {

	protected IExpressionEvaluator expressionEvaluatorBuilder(Class<?> javaType, String[] parameters, Object[] values, FunctionType fType, Operator op) {
		return new IExpressionEvaluator() {
			
			@Override
			public String getShortDescription() {
				return "Expression evaluation";
			}
			
			@Override
			public Iterable<String> getInputParameterNames() {
				if(fType == FunctionType.PARAMETER_ONLY)
					return Arrays.asList(parameters);
				else if((fType == FunctionType.PARAMETER_AND_VALUE))
					return Arrays.asList(parameters[0]);
				else return Arrays.asList();
			}
			
			@Override
			public Object evaluateExpression(IValueProvider provider) throws Exception {
				if(fType.equals(FunctionType.PARAMETER_ONLY)) {
					if(javaType == String.class) {
						String _par0 = (String) provider.getValue(parameters[0]);
						String _par1 = (String) provider.getValue(parameters[1]);
						switch(op) {
						case EQUAL: return (_par0.compareTo(_par1) == 0);
						case UNEQUAL: return (_par0.compareTo(_par1) != 0);
						case SMALLER: return (_par0.compareTo(_par1) < 0); 
						case SMALLER_OR_EQUAL: return (_par0.compareTo(_par1) <= 0);
						case GREATER: return (_par0.compareTo(_par1) > 0);
						case GREATER_OR_EQUAL: return (_par0.compareTo(_par1) >= 0);
						}
					}
					else if(javaType == int.class) {
						Integer _par0 = (Integer) provider.getValue(parameters[0]);
						Integer _par1 = (Integer) provider.getValue(parameters[1]);
						switch(op) {
						case EQUAL: return (_par0 = _par1);
						case UNEQUAL: return (_par0 != _par1);
						case SMALLER: return (_par0 < _par1);
						case SMALLER_OR_EQUAL: return (_par0 <= _par1);
						case GREATER: return (_par0 > _par1);
						case GREATER_OR_EQUAL: return (_par0 >= _par1);
						}
					}
				} else if (fType.equals(FunctionType.VALUE_ONLY)) {
					if(Integer.class.isInstance(values[0]) && Integer.class.isInstance(values[1])) {
						switch(op) {
						case EQUAL: return ((Integer) values[0] == (Integer) values[1]);
						case UNEQUAL: return ((Integer) values[0] != (Integer) values[1]);
						case SMALLER: return ((Integer) values[0] < (Integer) values[1]);
						case SMALLER_OR_EQUAL: return ((Integer) values[0] <= (Integer) values[1]);
						case GREATER: return ((Integer) values[0] > (Integer) values[1]);
						case GREATER_OR_EQUAL: return ((Integer) values[0] >= (Integer) values[1]);
						}
					}
					if(values[0] instanceof String && values[1] instanceof String) {
						String _var0 = (String) values[0];
						String _var1 = (String) values[1];
						switch(op) {
						case EQUAL: return (_var0.compareTo(_var1) == 0);
						case UNEQUAL: return (_var0.compareTo(_var1) != 0);
						case SMALLER: return (_var0.compareTo(_var1) > 0);
						case SMALLER_OR_EQUAL: return (_var0.compareTo(_var1) >= 0);
						case GREATER: return (_var0.compareTo(_var1) < 0);
						case GREATER_OR_EQUAL: return (_var0.compareTo(_var1) <= 0);
						}
					}
					
				} else if(fType.equals(FunctionType.PARAMETER_AND_VALUE)) {
					if(javaType == int.class && Integer.class.isInstance(values[0])) {
						Integer _par0 = (Integer) provider.getValue(parameters[0].toString());
						switch(op) {
						case EQUAL: return (_par0 == (int) values[0]);
						case UNEQUAL: return (_par0 != (int) values[0]);
						case SMALLER: return (_par0 < (int) values[0]);
						case SMALLER_OR_EQUAL: return (_par0 <= (int) values[0]);
						case GREATER: return (_par0 > (int) values[0]);
						case GREATER_OR_EQUAL: return (_par0 >= (int) values[0]);
						}
					} else if(javaType == String.class && values[0] instanceof String) {
						String _par0 = (String) provider.getValue((String) parameters[0]);
						switch(op) {
						case EQUAL: return (_par0.compareTo((String)values[0]) == 0);
						case UNEQUAL: return (_par0.compareTo((String) values[0]) != 0);
						case SMALLER: return (_par0.compareTo((String) values[0]) < 0);
						case SMALLER_OR_EQUAL: return (_par0.compareTo((String) values[0]) <= 0);
						case GREATER: return (_par0.compareTo((String) values[0]) > 0);
						case GREATER_OR_EQUAL: return (_par0.compareTo((String) values[0]) >= 0);
						}
					}
				}
				return null;
			}
		};
	}
}
