package org.emoflon.ibex.gt.viatra.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.viatra.query.runtime.matchers.psystem.IExpressionEvaluator;
import org.eclipse.viatra.query.runtime.matchers.psystem.IValueProvider;
import org.emoflon.ibex.gt.editor.utils.GTEditorAttributeUtils;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXAttributeExpression;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXConstant;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXEnumLiteral;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXRelation;

/**
 * Separate class to create IExpressionEvaluator
 * @author Julian Barthel
 *
 */
public class IExpressionEvaluatorBuilder {

	public static BiFunction<Object, Object, Boolean> compare(final EClassifier type1, final  EClassifier type2, IBeXRelation op) {
		if(type1 == EcorePackage.Literals.ESTRING && type2 == EcorePackage.Literals.ESTRING) {
			switch(op) {
				case EQUAL: return (val1, val2) -> ((String)val1).compareTo((String)val2) == 0;
				case UNEQUAL: return (val1, val2) -> ((String)val1).compareTo((String)val2) != 0;
				case SMALLER: return (val1, val2) -> ((String)val1).compareTo((String)val2) < 0;
				case SMALLER_OR_EQUAL: return (val1, val2) -> ((String)val1).compareTo((String)val2) <= 0;
				case GREATER: return (val1, val2) -> ((String)val1).compareTo((String)val2) > 0;
				case GREATER_OR_EQUAL: return (val1, val2) -> ((String)val1).compareTo((String)val2) >= 0;
				default: return null;
			}
			
		} else if(type1 == EcorePackage.Literals.EINT && type2 == type1) {
			switch(op) {
				case EQUAL: return (val1, val2) -> ((Integer)val1) == ((Integer)val2);
				case UNEQUAL: return (val1, val2) -> ((Integer)val1) != ((Number)val2);
				case SMALLER: return (val1, val2) -> ((Integer)val1) < ((Integer)val2);
				case SMALLER_OR_EQUAL: return (val1, val2) -> ((Integer)val1) <= ((Integer)val2);
				case GREATER: return (val1, val2) -> ((Integer)val1) > ((Integer)val2);
				case GREATER_OR_EQUAL: return (val1, val2) -> ((Integer)val1) >= ((Integer)val2);
				default: return null;
			}
		} else if(type1 == EcorePackage.Literals.EENUM && type2 == type1) {
			switch(op) {
				case EQUAL: return (val1, val2) -> val1.toString().equals(val2.toString());
				case UNEQUAL: return (val1, val2) -> !val1.toString().equals(val2.toString());
				default: return null;
			}
		}else if(type1 == EcorePackage.Literals.EDOUBLE && type2 == type1) {
			switch(op) {
				case EQUAL: return (val1, val2) -> ((Double)val1) == ((Double)val2);
				case UNEQUAL: return (val1, val2) -> ((Double)val1) != ((Double)val2);
				case SMALLER: return (val1, val2) -> ((Double)val1) < ((Double)val2);
				case SMALLER_OR_EQUAL: return (val1, val2) -> ((Double)val1) <= ((Double)val2);
				case GREATER: return (val1, val2) -> ((Double)val1) > ((Double)val2);
				case GREATER_OR_EQUAL: return (val1, val2) -> ((Double)val1) >= ((Double)val2);
				default: return null;
			}
		} else if(type1 == EcorePackage.Literals.EBOOLEAN && type2 == type1) {
			switch(op) {
				case EQUAL: return (val1, val2) -> ((Boolean)val1) == ((Boolean)val2);
				case UNEQUAL: return (val1, val2) -> ((Boolean)val1) != ((Boolean)val2);
				default: return null;
			}
		} else if((type1 == EcorePackage.Literals.EINT && type2 == EcorePackage.Literals.EDOUBLE) || 
				(type1 == EcorePackage.Literals.EDOUBLE && type2 == EcorePackage.Literals.EINT)) {
			switch(op) {
				case EQUAL: return (val1, val2) -> ((Double)val1) == ((Double)val2);
				case UNEQUAL: return (val1, val2) -> ((Double)val1) != ((Double)val2);
				case SMALLER: return (val1, val2) -> ((Double)val1) < ((Double)val2);
				case SMALLER_OR_EQUAL: return (val1, val2) -> ((Double)val1) <= ((Double)val2);
				case GREATER: return (val1, val2) -> ((Double)val1) > ((Double)val2);
				case GREATER_OR_EQUAL: return (val1, val2) -> ((Double)val1) >= ((Double)val2);
				default: return null;
			}
		} else {
			return null;
		}
	
	}
	
	public static IExpressionEvaluator expressionEvaluatorBuilder(IBeXConstant constant1, IBeXConstant constant2, IBeXRelation op) {
		return new IExpressionEvaluator() {
			
			BiFunction<Object, Object, Boolean> comp = compare(GTEditorAttributeUtils.convertStringToEDataType(constant1.getStringValue()), 
					GTEditorAttributeUtils.convertStringToEDataType(constant2.getStringValue()), op);
			Object val1 = GTEditorAttributeUtils.convertEDataTypeStringToObject(constant1.getStringValue()).get();
			Object val2 = GTEditorAttributeUtils.convertEDataTypeStringToObject(constant2.getStringValue()).get();

			@Override
			public String getShortDescription() {
				return "Expression evaluation for two numeric constants.";
			}

			@Override
			public Iterable<String> getInputParameterNames() {
				return Arrays.asList();
			}

			@Override
			public Object evaluateExpression(IValueProvider provider) throws Exception {
				return comp.apply(val1, val2);
			}
			
		};
	}
	
	public static IExpressionEvaluator expressionEvaluatorBuilder(IBeXEnumLiteral enum1, IBeXEnumLiteral enum2, IBeXRelation op) {
		return new IExpressionEvaluator() {

			BiFunction<Object, Object, Boolean> comp = compare(EcorePackage.Literals.EENUM, EcorePackage.Literals.EENUM, op);
			Object val1 = enum1.getLiteral();
			Object val2 = enum2.getLiteral();
			
			@Override
			public String getShortDescription() {
				return "Expression evaluation for two enum constants.";
			}

			@Override
			public Iterable<String> getInputParameterNames() {
				return Arrays.asList();
			}

			@Override
			public Object evaluateExpression(IValueProvider provider) throws Exception {
				return comp.apply(val1, val2);
			}
			
		};
	}
	
	public static IExpressionEvaluator expressionEvaluatorBuilder(IBeXAttributeExpression expr, IBeXConstant constant1, String exprName, IBeXRelation op) {
		return new IExpressionEvaluator() {
			
			List<String> params = new ArrayList<>(Arrays.asList(new String[]{exprName}));
			
			BiFunction<Object, Object, Boolean> comp = compare(expr.getAttribute().getEType(), 
					GTEditorAttributeUtils.convertStringToEDataType(constant1.getStringValue()), op);
			Object val2 = GTEditorAttributeUtils.convertEDataTypeStringToObject(constant1.getStringValue()).get();

			@Override
			public String getShortDescription() {
				return "Expression evaluation for an attribute value and a constant value.";
			}

			@Override
			public Iterable<String> getInputParameterNames() {
				return params;
			}

			@Override
			public Object evaluateExpression(IValueProvider provider) throws Exception {
				Object val1 = provider.getValue(params.get(0));
				return comp.apply(val1, val2);
			}
			
		};
	}
	
	public static IExpressionEvaluator expressionEvaluatorBuilder(IBeXConstant constant1, IBeXAttributeExpression expr, String exprName, IBeXRelation op) {
		return expressionEvaluatorBuilder(expr, constant1, exprName, op);
	}
	
	public static IExpressionEvaluator expressionEvaluatorBuilder(IBeXAttributeExpression expr, IBeXEnumLiteral enum1, String exprName, IBeXRelation op) {
		return new IExpressionEvaluator() {
			
			List<String> params = new ArrayList<>(Arrays.asList(new String[]{exprName}));
			BiFunction<Object, Object, Boolean> comp = compare(EcorePackage.Literals.EENUM, 
					EcorePackage.Literals.EENUM, op);
			Object val2 = enum1.getLiteral();

			@Override
			public String getShortDescription() {
				return "Expression evaluation for an attribute value and an enum literal.";
			}

			@Override
			public Iterable<String> getInputParameterNames() {
				return params;
			}

			@Override
			public Object evaluateExpression(IValueProvider provider) throws Exception {
				Object val1 = provider.getValue(params.get(0));
				return comp.apply(val1, val2);
			}
			
		};
	}
	
	public static IExpressionEvaluator expressionEvaluatorBuilder(IBeXEnumLiteral enum1, IBeXAttributeExpression expr, String exprName, IBeXRelation op) {
		return expressionEvaluatorBuilder(expr, enum1, exprName, op);
	}
	
	public static IExpressionEvaluator expressionEvaluatorBuilder(IBeXAttributeExpression expr1, IBeXAttributeExpression expr2, String expr1Name, String expr2Name, IBeXRelation op) {
		return new IExpressionEvaluator() {
			
			List<String> params = null;
			String param1 = expr1Name;
			String param2 = expr2Name;
					
			BiFunction<Object, Object, Boolean> comp = compare(expr1.getAttribute().getEType(), 
					expr2.getAttribute().getEType(), op);

			@Override
			public String getShortDescription() {
				return "Expression evaluation for two attribute values.";
			}

			@Override
			public Iterable<String> getInputParameterNames() {
				if(params == null) {
					params = new ArrayList<>(2);
					params.add(param1);
					params.add(param2);
				}
				return params;
			}

			@Override
			public Object evaluateExpression(IValueProvider provider) throws Exception {
				Object val1 = provider.getValue(param1);
				Object val2 = provider.getValue(param2);
				return comp.apply(val1, val2);
			}
			
		};
	}
	
//	protected IExpressionEvaluator expressionEvaluatorBuilder(Class<?> javaType, String[] parameters, Object[] values, FunctionType fType, Operator op) {
//		return new IExpressionEvaluator() {
//			
//			@Override
//			public String getShortDescription() {
//				return "Expression evaluation";
//			}
//			
//			@Override
//			public Iterable<String> getInputParameterNames() {
//				if(fType == FunctionType.PARAMETER_ONLY)
//					return Arrays.asList(parameters);
//				else if((fType == FunctionType.PARAMETER_AND_VALUE))
//					return Arrays.asList(parameters[0]);
//				else return Arrays.asList();
//			}
//			
//			@Override
//			public Object evaluateExpression(IValueProvider provider) throws Exception {
//				if(fType.equals(FunctionType.PARAMETER_ONLY)) {
//					if(javaType == String.class) {
//						String _par0 = (String) provider.getValue(parameters[0]);
//						String _par1 = (String) provider.getValue(parameters[1]);
//						switch(op) {
//						case EQUAL: return (_par0.compareTo(_par1) == 0);
//						case UNEQUAL: return (_par0.compareTo(_par1) != 0);
//						case SMALLER: return (_par0.compareTo(_par1) < 0); 
//						case SMALLER_OR_EQUAL: return (_par0.compareTo(_par1) <= 0);
//						case GREATER: return (_par0.compareTo(_par1) > 0);
//						case GREATER_OR_EQUAL: return (_par0.compareTo(_par1) >= 0);
//						}
//					}
//					else if(javaType == int.class) {
//						Integer _par0 = (Integer) provider.getValue(parameters[0]);
//						Integer _par1 = (Integer) provider.getValue(parameters[1]);
//						switch(op) {
//						case EQUAL: return (_par0 = _par1);
//						case UNEQUAL: return (_par0 != _par1);
//						case SMALLER: return (_par0 < _par1);
//						case SMALLER_OR_EQUAL: return (_par0 <= _par1);
//						case GREATER: return (_par0 > _par1);
//						case GREATER_OR_EQUAL: return (_par0 >= _par1);
//						}
//					}
//				} else if (fType.equals(FunctionType.VALUE_ONLY)) {
//					if(Integer.class.isInstance(values[0]) && Integer.class.isInstance(values[1])) {
//						switch(op) {
//						case EQUAL: return ((Integer) values[0] == (Integer) values[1]);
//						case UNEQUAL: return ((Integer) values[0] != (Integer) values[1]);
//						case SMALLER: return ((Integer) values[0] < (Integer) values[1]);
//						case SMALLER_OR_EQUAL: return ((Integer) values[0] <= (Integer) values[1]);
//						case GREATER: return ((Integer) values[0] > (Integer) values[1]);
//						case GREATER_OR_EQUAL: return ((Integer) values[0] >= (Integer) values[1]);
//						}
//					}
//					if(values[0] instanceof String && values[1] instanceof String) {
//						String _var0 = (String) values[0];
//						String _var1 = (String) values[1];
//						switch(op) {
//						case EQUAL: return (_par0 == (int) values[0]);
//						case UNEQUAL: return (_par0 != (int) values[0]);
//						case SMALLER: return (_par0 < (int) values[0]);
//						case SMALLER_OR_EQUAL: return (_par0 <= (int) values[0]);
//						case GREATER: return (_par0 > (int) values[0]);
//						case GREATER_OR_EQUAL: return (_par0 >= (int) values[0]);
//						}
//					}
//					
//				} else if(fType.equals(FunctionType.PARAMETER_AND_VALUE)) {
//					if(javaType == int.class && Integer.class.isInstance(values[0])) {
//						Integer _par0 = (Integer) provider.getValue(parameters[0].toString());
//						switch(op) {
//						case EQUAL: return (_par0 == (int) values[0]);
//						case UNEQUAL: return (_par0 != (int) values[0]);
//						case SMALLER: return (_par0 < (int) values[0]);
//						case SMALLER_OR_EQUAL: return (_par0 <= (int) values[0]);
//						case GREATER: return (_par0 > (int) values[0]);
//						case GREATER_OR_EQUAL: return (_par0 >= (int) values[0]);
//						}
//					} else if(javaType == String.class && values[0] instanceof String) {
//						String _par0 = (String) provider.getValue((String) parameters[0]);
//						switch(op) {
//						case EQUAL: return (_par0.compareTo((String)values[0]) == 0);
//						case UNEQUAL: return (_par0.compareTo((String) values[0]) != 0);
//						case SMALLER: return (_par0.compareTo((String) values[0]) < 0);
//						case SMALLER_OR_EQUAL: return (_par0.compareTo((String) values[0]) <= 0);
//						case GREATER: return (_par0.compareTo((String) values[0]) > 0);
//						case GREATER_OR_EQUAL: return (_par0.compareTo((String) values[0]) >= 0);
//						}
//					}
//				}
//				return null;
//			}
//		};
//	}
}
