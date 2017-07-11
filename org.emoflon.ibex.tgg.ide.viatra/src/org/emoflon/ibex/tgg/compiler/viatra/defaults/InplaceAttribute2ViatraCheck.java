package org.emoflon.ibex.tgg.compiler.viatra.defaults;

import org.eclipse.emf.ecore.EcorePackage;

import language.basic.expressions.TGGEnumExpression;
import language.basic.expressions.TGGLiteralExpression;
import language.inplaceAttributes.TGGAttributeConstraintOperators;
import language.inplaceAttributes.TGGInplaceAttributeExpression;

public class InplaceAttribute2ViatraCheck {

	public static String extractViatraCheck(String attributeName, TGGInplaceAttributeExpression expression) {
		return attributeName + " " + convertOperatorEnumToString(expression.getOperator()) + " " + extractViatraEqualCheck(expression);
	}

	public static String extractViatraEqualCheck(TGGInplaceAttributeExpression expression) {
		if (expression.getValueExpr() instanceof TGGLiteralExpression) {
			TGGLiteralExpression tle = (TGGLiteralExpression) expression.getValueExpr();
			return tle.getValue();
		}
		if (expression.getValueExpr() instanceof TGGEnumExpression) {
			TGGEnumExpression tee = (TGGEnumExpression) expression.getValueExpr();
			return tee.getEenum().getName() + "::" + tee.getLiteral().toString();
		}
		return null;
	}

	public static boolean simpleExpression(TGGInplaceAttributeExpression expression) {
		return expression.getOperator().equals(TGGAttributeConstraintOperators.EQUAL);
	}
	
	public static boolean isENUMExpr(TGGInplaceAttributeExpression expression) {
		return expression.getAttribute().getEType().eClass().equals(EcorePackage.eINSTANCE.getEEnum());
	}

	public static String convertOperatorEnumToString(TGGAttributeConstraintOperators operator) {
		switch (operator) {
		case EQUAL:
			return "==";
		case UNEQUAL:
			return "!=";
		case GR_EQUAL:
			return ">=";
		case LE_EQUAL:
			return "<=";
		case GREATER:
			return ">";
		case LESSER:
			return "<";
		default:
			return null;
		}
	}
}
