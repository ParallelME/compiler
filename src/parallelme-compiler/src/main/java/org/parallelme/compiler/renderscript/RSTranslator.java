/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.misc.Interval;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseTranslator;
import org.parallelme.compiler.userlibrary.classes.Array;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for RenderScript translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSTranslator extends BaseTranslator {
	private static final String templateIteratorParallelCall = "<externalVariables:{var|<var.kernelName>.set_<var.gVariableName>(<var.variableName>);\n}>"
			+ "<kernelName>.forEach_<functionName>(<allocationName>, <allocationName>);\n";
	private static final String templateIteratorSequentialCall = "<externalVariables:{var|<var.type>[] <var.arrName> = new <var.type>[1];\n"
			+ "Allocation <var.allName> = Allocation.createSized($mRS, Element.<var.elementType>($mRS), 1);\n"
			+ "<kernelName>.set_<var.gName>(<var.name>);\n"
			+ "<kernelName>.set_<var.outputData>(<var.allName>);\n}>"
			+ "<kernelName>.set_<inputData>(<allocationName>);\n"
			+ "<inputSize:{var|<kernelName>.set_<var.name>(<allocationName>.getType().get<var.XYZ>());\n}>"
			+ "<kernelName>.invoke_<functionName>();\n"
			+ "<externalVariables:{var|<var.allName>.copyTo(<var.arrName>);\n"
			+ "<var.name> = <var.arrName>[0];\n}>";
	private static final String templateIteratorSequentialFunction = "rs_allocation <inputData>;\n"
			+ "<outVariable:{var|rs_allocation <var.name>;\n}>"
			+ "int gInputXSize<iteratorName>;\n"
			+ "int gInputYSize<iteratorName>;\n"
			+ "<externalVariables:{var|<var.variableType> <var.variableName>;\n}>"
			+ "\n<functionSignature>\n {\n"
			+ "\t<userFunctionVarType> <userFunctionVarName>;\n"
			+ "\t<forLoop>"
			+ "\t<externalVariables:{var|rsSetElementAt_<var.variableType>(<var.outVariableName>, <var.variableName>, 0);\n}>"
			+ "}";

	private static final String templateForLoop = "for (int <varName> = 0; <varName> \\< <varMaxVal>; <varName>++) {\n\t<body>}\n";
	private static final String templateForLoopSequentialBody = "<userFunctionVarName> = rsGetElementAt_<userFunctionVarType>(<inputData>, x<param:{var|, <var.name>}>);\n"
			+ "<userCode>\n"
			+ "rsSetElementAt_<userFunctionVarType>(<inputData>, <userFunctionVarName>, x<param:{var|, <var.name>}>);\n";
	private static final String templateIteratorParallelFunctionSignature = "<parameterTypeTranslated> __attribute__((kernel)) <userFunctionName>(<parameterTypeTranslated> <parameterName>, uint32_t x<params:{var|, <var.type> <var.name>}>)";
	private static final String templateIteratorSequentialFunctionSignature = "void <functionName>()";

	// Keeps a key-value map of equivalent types from Java to RenderScript
	// allocation.
	private static Map<String, String> java2RSAllocationTypes = null;
	protected CTranslator cCodeTranslator;

	public RSTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
		if (java2RSAllocationTypes == null)
			this.initJava2RSAllocationTypes();
	}

	private void initJava2RSAllocationTypes() {
		java2RSAllocationTypes = new HashMap<>();
		java2RSAllocationTypes.put("short", "I16");
		java2RSAllocationTypes.put("int", "I32");
		java2RSAllocationTypes.put("float", "F32");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateIteratorCall(String className, Iterator iterator) {
		String functionName = this.commonDefinitions.getIteratorName(iterator);
		String kernelName = this.commonDefinitions.getKernelName(className);
		ST st;
		if (iterator.getType() == IteratorType.Parallel) {
			st = new ST(templateIteratorParallelCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("externalVariables", null);
			if (!iterator.getExternalVariables().isEmpty()) {
				for (Variable variable : iterator.getExternalVariables()) {
					String gVariable = this.getGlobalVariableName(
							variable.name, iterator);
					st.addAggr(
							"externalVariables.{kernelName, gVariableName, variableName}",
							kernelName, gVariable, variable.name);
				}
			}
		} else {
			String inputData = this
					.getGlobalVariableName(
							"input"
									+ this.upperCaseFirstLetter(iterator
											.getVariable().name), iterator);
			String iteratorName = this.upperCaseFirstLetter(functionName);
			st = new ST(templateIteratorSequentialCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("inputData", inputData);
			st.add("iteratorName", iteratorName);
			for (Variable variable : iterator.getExternalVariables()) {
				String gName = this.getGlobalVariableName(variable.name,
						iterator);
				String allocationName = this.commonDefinitions.getPrefix()
						+ gName + "_Allocation";
				String outputData = this.getGlobalVariableName(
						"output" + this.upperCaseFirstLetter(variable.name),
						iterator);
				st.addAggr(
						"externalVariables.{type, arrName, name, gName, allName, elementType, outputData}",
						variable.typeName, this.commonDefinitions.getPrefix()
								+ variable.name, variable.name,
						this.getGlobalVariableName(variable.name, iterator),
						allocationName,
						java2RSAllocationTypes.get(variable.typeName),
						outputData);
			}
			st.addAggr("inputSize.{name, XYZ}", "gInputXSize" + iteratorName,
					"X");
			if (iterator.getVariable().typeName.equals(BitmapImage.getName())
					|| iterator.getVariable().typeName.equals(HDRImage
							.getName())) {
				st.addAggr("inputSize.{name, XYZ}", "gInputYSize"
						+ iteratorName, "Y");
			}
		}
		if (iterator.getVariable().typeName.equals(Array.getName()))
			st.add("allocationName", this.commonDefinitions
					.getVariableInName(iterator.getVariable()));
		else
			st.add("allocationName", this.commonDefinitions
					.getVariableOutName(iterator.getVariable()));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelIterator(Iterator iterator) {
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		//String code2Translate = iterator.getUserFunctionData().Code.trim(); TODO: Code should return what I've wrote below
		
		TokenStreamRewriter tokenStreamRewriter = new TokenStreamRewriter(iterator.getTokenStream());
		ArrayList<Variable> variables = iterator.getExternalVariables();
		
		
		for(Iterator nested : iterator.getNestedIterators()){
			String iteratorName = this.upperCaseFirstLetter(this.commonDefinitions
					.getIteratorName(nested));
			
			//TODO: If it's an image should be two max values
			//TODO: Remove this "magic string" "gInputXSize". This string should be a constant on this class because it's used in another place too
			//TODO: "int" should be a enumerator (Another case of Magic String)
			variables.add(new Variable("gInputXSize" + iteratorName, "int", "", "" )); //Add max value of each nested loop as a external variable
			
			String variableName = this
					.upperCaseFirstLetter(nested.getVariable().name);
			
			String gNameIn = this.getGlobalVariableName("input" + variableName,
					nested);				
			
			variables.add(new Variable(gNameIn, "rs_allocation", "", "" ));
			
			Variable nestedUserFunctionVariable = nested.getUserFunctionData().variableArgument;
			
			String userFunctionVarDeclaration = "\t"+this
					.translateType(nestedUserFunctionVariable.typeName)+" "+nestedUserFunctionVariable.name+";\n";
			
			tokenStreamRewriter.replace(nested.getStatementAddress().start,nested.getStatementAddress().stop, userFunctionVarDeclaration + this.translateNestedIterator(nested)); //TODO: Insert Nested Loop Generated code here			
		}
		
		String code2Translate = tokenStreamRewriter.getText(Interval.of(iterator.getUserFunctionData().getTokenAddress().start.getTokenIndex(), iterator.getUserFunctionData().getTokenAddress().stop.getTokenIndex()));				
		
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		String ret;
		String returnString = "\treturn " + userFunctionVariable.name + ";";
		code2Translate = code2Translate + "\n" + returnString + "\n}";
		// Insert external variables as global variables
		StringBuffer externalVariables = new StringBuffer();
				
		
		for (Variable variable : iterator.getExternalVariables()) {
			String gVariableName = this.getGlobalVariableName(variable.name,
					iterator);
			externalVariables.append(variable.typeName + " " + gVariableName
					+ ";\n");
			code2Translate = this.replaceAndEscapePrefix(code2Translate,
					gVariableName, variable.name);
		}
		externalVariables.append("\n");
		ret = externalVariables.toString()
				+ this.getIteratorFunctionSignature(iterator)
				+ this.translateVariable(userFunctionVariable,
						this.cCodeTranslator.translate(code2Translate));
		return ret;
	}
	
	protected String translateNestedIterator(Iterator iterator){
		
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		
		String userFunctionVarType = this
				.translateType(userFunctionVariable.typeName);
		
		String iteratorName = this.upperCaseFirstLetter(this.commonDefinitions
				.getIteratorName(iterator));
		
		String variableName = this
				.upperCaseFirstLetter(iterator.getVariable().name);
		
		String gNameIn = this.getGlobalVariableName("input" + variableName,
				iterator);		
		
		ST stFor = new ST(templateForLoop);
		stFor.add("varName", "x");
		stFor.add("varMaxVal", "gInputXSize" + iteratorName);
		ST stForBody = new ST(templateForLoopSequentialBody);
		stForBody.add("inputData", gNameIn);
		stForBody.add("userFunctionVarName", userFunctionVariable.name);
		stForBody.add("userFunctionVarType", userFunctionVarType);
		stForBody.add("userCode", iterator.getUserFunctionData().Code.trim()); //TODO: This code need to be translate
		stForBody.add("param", null);
		// BitmapImage and HDRImage types contains two for loops
		if (iterator.getVariable().typeName.equals(BitmapImage.getName())
				|| iterator.getVariable().typeName.equals(HDRImage.getName())) {
			stForBody.addAggr("param.{name}", "y");
			ST stFor2 = new ST(templateForLoop);
			stFor2.add("varName", "y");
			stFor2.add("varMaxVal", "gInputYSize" + iteratorName);
			stFor2.add("body", stForBody.render());
			stFor.add("body", stFor2.render());
		} else {
			// Array types
			stFor.add("body", stForBody.render());
		}

		return stFor.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateSequentialIterator(Iterator iterator) {
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		String code2Translate = iterator.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		// Remove the first curly brace
		code2Translate = code2Translate.substring(
				code2Translate.indexOf("{") + 1, code2Translate.length());
		ST st = new ST(templateIteratorSequentialFunction);
		String variableName = this
				.upperCaseFirstLetter(iterator.getVariable().name);
		String gNameIn = this.getGlobalVariableName("input" + variableName,
				iterator);
		String iteratorName = this.upperCaseFirstLetter(this.commonDefinitions
				.getIteratorName(iterator));
		st.add("inputData", gNameIn);
		st.add("functionSignature", this.getIteratorFunctionSignature(iterator));
		st.add("iteratorName", iteratorName);
		String userFunctionVarType = this
				.translateType(userFunctionVariable.typeName);
		st.add("userFunctionVarName", userFunctionVariable.name);
		st.add("userFunctionVarType", userFunctionVarType);
		
		
		String cCode = this.translateVariable(userFunctionVariable,
				this.cCodeTranslator.translate(code2Translate)).trim();
		
		
		
		for (Variable variable : iterator.getExternalVariables()) {
			String gNameOut = this.getGlobalVariableName(
					"output" + this.upperCaseFirstLetter(variable.name),
					iterator);
			st.addAggr("outVariable.{name}", gNameOut);
			String gNameVar = this.getGlobalVariableName(variable.name,
					iterator);
			st.addAggr(
					"externalVariables.{ variableType, outVariableName, variableName }",
					variable.typeName, gNameOut, gNameVar);
			cCode = this.replaceAndEscapePrefix(cCode, gNameVar, variable.name);
		}
		ST stFor = new ST(templateForLoop);
		stFor.add("varName", "x");
		stFor.add("varMaxVal", "gInputXSize" + iteratorName);
		ST stForBody = new ST(templateForLoopSequentialBody);
		stForBody.add("inputData", gNameIn);
		stForBody.add("userFunctionVarName", userFunctionVariable.name);
		stForBody.add("userFunctionVarType", userFunctionVarType);
		stForBody.add("userCode", cCode);
		stForBody.add("param", null);
		// BitmapImage and HDRImage types contains two for loops
		if (iterator.getVariable().typeName.equals(BitmapImage.getName())
				|| iterator.getVariable().typeName.equals(HDRImage.getName())) {
			stForBody.addAggr("param.{name}", "y");
			ST stFor2 = new ST(templateForLoop);
			stFor2.add("varName", "y");
			stFor2.add("varMaxVal", "gInputYSize" + iteratorName);
			stFor2.add("body", stForBody.render());
			stFor.add("body", stFor2.render());
		} else {
			// Array types
			stFor.add("body", stForBody.render());
		}
		st.add("forLoop", stFor.render());
		return st.render();
	}

	/**
	 * Create the function signature for a given iterator.
	 * 
	 * @param iterator
	 *            Iterator that must be analyzed in order to create a function
	 *            signature.
	 * 
	 * @return Function signature.
	 */
	protected String getIteratorFunctionSignature(Iterator iterator) {
		String functionSignature = "";
		String parameterTypeTranslated = this.translateType(iterator
				.getUserFunctionData().variableArgument.typeName);
		if (iterator.getType() == IteratorType.Parallel) {
			ST st = new ST(templateIteratorParallelFunctionSignature);
			st.add("parameterTypeTranslated", parameterTypeTranslated);
			st.add("parameterName",
					iterator.getUserFunctionData().variableArgument.name);
			st.add("userFunctionName",
					this.commonDefinitions.getIteratorName(iterator));
			st.add("params", null);
			if (iterator.getVariable().typeName.equals(BitmapImage.getName())
					|| iterator.getVariable().typeName.equals(HDRImage
							.getName())) {
				st.addAggr("params.{type, name}", "uint32_t", "y");
			}
			functionSignature = st.render();
		} else {
			ST st = new ST(templateIteratorSequentialFunctionSignature);
			st.add("functionName",
					this.commonDefinitions.getIteratorName(iterator));
			functionSignature = st.render();
		}
		return functionSignature + " ";
	}
}
