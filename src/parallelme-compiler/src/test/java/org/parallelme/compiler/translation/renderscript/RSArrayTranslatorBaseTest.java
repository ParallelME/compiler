/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Parameter;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.BaseTranslatorTest;
import org.parallelme.compiler.translation.SimpleTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.stringtemplate.v4.ST;

/**
 * Performs tests to validate RSArrayTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSArrayTranslatorBaseTest extends BaseTranslatorTest {
	abstract protected String getParameterType();

	abstract protected String getTranslatedParameterType();

	abstract protected String getRSType();

	private Variable arrayVar = new Variable("arrayVar", "Array",
			getParameterType(), "", 1);

	@Override
	protected Variable getUserLibraryVar() {
		return this.arrayVar;
	}

	@Override
	protected BaseUserLibraryTranslator getTranslator() {
		return new RSArrayTranslator(new SimpleTranslator());
	}

	private InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", getTranslatedParameterType()
				+ "[]", "", "", 2));
		parameters.add(new Variable(getParameterType() + ".class", "Class",
				getParameterType(), "", 3));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null,
				null);
	}

	private OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("arrayVar",
				getTranslatedParameterType() + "[]", "", "", 1);
		return new OutputBind(this.getUserLibraryVar(), destinationVar, 1,
				null, outputBindType);
	}

	private Operation createForeachOperation(ExecutionType executionType) {
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Foreach, null);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), "", "", 10));
		UserFunction userFunction = new UserFunction(
				" { param1.value = 123; }", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}

	protected Operation createReduceOperation(ExecutionType executionType) {
		Variable destVar = new Variable("destVar", getParameterType(), "", "",
				999);
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Reduce, destVar);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), "", "", 10));
		arguments.add(new Variable("param2", getParameterType(), "", "", 11));
		UserFunction userFunction = new UserFunction(
				" { param1.value += param2.value; " + "return param1;}",
				arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}

	/**
	 * Tests input bind translation.
	 */
	@Test
	public void translateInputBind() throws Exception {
		String expectedTranslation = "";
		InputBind inputBind = this.createInputBind();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateInputBind(className,
				inputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests input bind object creation.
	 */
	@Test
	public void translateInputBindObjCreation() throws Exception {
		InputBind inputBind = this.createInputBind();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String varInName = commonDefinitions
				.getVariableInName(inputBind.variable);
		ST st = new ST(
				"<varIn> = Allocation.createSized(PM_mRS, Element.<rsType>(PM_mRS), <dataVar>.length);\n"
						+ "<varIn>.copyFrom(<dataVar>);");
		st.add("dataVar", inputBind.parameters.get(0));
		st.add("varIn", varInName);
		st.add("rsType", getRSType());
		String expectedTranslation = st.render();
		String translatedFunction = translator.translateInputBindObjCreation(
				className, inputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests input bind object declaration.
	 */
	@Test
	public void translateInputBindObjDeclaration() throws Exception {
		InputBind inputBind = createInputBind();
		BaseUserLibraryTranslator translator = this.getTranslator();
		ST st = new ST(
				"<varIn> = Allocation.createSized(PM_mRS, Element.<rsType>(PM_mRS), <dataVar>.length);\n"
						+ "<varIn>.copyFrom(<dataVar>);");
		st.add("varIn", commonDefinitions.getVariableInName(inputBind.variable));
		st.add("rsType", getRSType());
		st.add("dataVar", inputBind.parameters.get(0));
		String expectedTranslation = st.render();
		String translatedFunction = translator.translateInputBindObjCreation(
				className, inputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests output bind translation.
	 */
	@Test
	public void translateOutputBind() throws Exception {
		String expectedTranslation = "";
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBind(className,
				null);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests output bind call.
	 */
	@Test
	public void translateOutputBindCall() throws Exception {
		// arrayVar.toArray(dataVar);
		OutputBind outputBind = createOutputBind(OutputBindType.None);
		String varInName = commonDefinitions
				.getVariableInName(outputBind.variable);
		ST st = new ST("<varIn>.copyTo(<varDest>);");
		st.add("varIn", varInName);
		st.add("varDest", outputBind.destinationObject);
		String expectedTranslation = st.render();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// bitmapVar = imageVar.toBitmap();
		outputBind = this.createOutputBind(OutputBindType.Assignment);
		st = new ST(
				"<varDest> = (<type>[]) java.lang.reflect.Array.newInstance(<type>.class, <varIn>.getType().getX());\n"
						+ "<varIn>.copyTo(<varDest>);");
		st.add("varIn", varInName);
		st.add("varDest", outputBind.destinationObject);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Bitmap bitmapVar = imageVar.toBitmap();
		outputBind = this
				.createOutputBind(OutputBindType.DeclarativeAssignment);
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests operation translation.
	 */
	@Test
	public void translateForeachOperation() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		ST st = new ST(
				"<type> __attribute__((kernel)) foreach123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "param1=123;\n" + "returnparam1;\n" + "}");
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"<finalVarType> PM_gExternalVarForeach123;"
						+ "<type> __attribute__((kernel)) foreach123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "param1=123;\n" + "returnparam1;\n" + "}");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "rs_allocation PM_gOutputExternalVarForeach123;\n"
						+ "int PM_gInputXSizeForeach123;\n"
						+ "<nonFinalVarType> PM_gExternalVarForeach123;\n"
						+ "void foreach123() {\n"
						+ "<type> param1;\n"
						+ "for (int PM_x=0; PM_x\\<PM_gInputXSizeForeach123; ++PM_x) {\n"
						+ "param1 = rsGetElementAt_<type>(PM_gInputForeach123, PM_x);\n"
						+ "param1 = 123;\n"
						+ "rsSetElementAt_<type>(PM_gInputForeach123, param1, PM_x);\n"
						+ "}\n"
						+ "rsSetElementAt_int(PM_gOutputExternalVarForeach123, PM_gExternalVarForeach123, 0);\n"
						+ "}");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "int PM_gInputXSizeForeach123;\n"
						+ "void foreach123() {\n"
						+ "<type> param1;\n"
						+ "for (int PM_x=0; PM_x\\<PM_gInputXSizeForeach123; ++PM_x) {\n"
						+ "param1 = rsGetElementAt_<type>(PM_gInputForeach123, PM_x);\n"
						+ "param1 = 123;\n"
						+ "rsSetElementAt_<type>(PM_gInputForeach123, param1, PM_x);\n"
						+ "}\n" + "}");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "int PM_gInputXSizeForeach123;\n"
						+ "<finalVarType> PM_gExternalVarForeach123;\n"
						+ "void foreach123() {\n"
						+ "<type> param1;\n"
						+ "for (int PM_x=0; PM_x\\<PM_gInputXSizeForeach123; ++PM_x) {\n"
						+ "param1 = rsGetElementAt_<type>(PM_gInputForeach123, PM_x);\n"
						+ "param1 = 123;\n"
						+ "rsSetElementAt_<type>(PM_gInputForeach123, param1, PM_x);\n"
						+ "}\n" + "}");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "rs_allocation PM_gOutputExternalVarForeach123;\n"
						+ "int PM_gInputXSizeForeach123;\n"
						+ "<nonFinalVarType> PM_gExternalVarForeach123;\n"
						+ "void foreach123() {\n"
						+ "<type> param1;\n"
						+ "for (int PM_x=0; PM_x\\<PM_gInputXSizeForeach123; ++PM_x) {\n"
						+ "param1 = rsGetElementAt_<type>(PM_gInputForeach123, PM_x);\n"
						+ "param1 = 123;\n"
						+ "rsSetElementAt_<type>(PM_gInputForeach123, param1, PM_x);\n"
						+ "}\n"
						+ "rsSetElementAt_int(PM_gOutputExternalVarForeach123, PM_gExternalVarForeach123, 0);\n"
						+ "}");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests operation call.
	 */
	@Test
	public void translateForeachOperationCall() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		ST st = new ST("<kernel>.forEach_foreach123(<varOut>, <varOut>);");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"<kernel>.set_PM_gInputForeach123(<varOut>);\n"
						+ "<kernel>.set_PM_gInputXSizeForeach123(<varOut>.getType().getX());\n"
						+ "<kernel>.invoke_foreach123(<varOut>, <varOut>);");
		st.add("rsType", getRSType());
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests reduce operation translation.
	 */
	@Test
	public void translateReduceOperation() throws Exception {
		// Parallel
		Operation operation = this
				.createReduceOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		ST st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gTileReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "int PM_gInputXSizeReduce123;\n"
						+ "int PM_gTileSizeReduce123;\n"
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param1 += param2;\n"
						+ "return param1;\n"
						+ "}\n"
						+ "<type> __attribute__ ((kernel)) reduce123_tile(uint32_t x) {\n"
						+ "int PM_base = x*PM_gTileSizeReduce123;\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gTileSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base + PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gTileReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gTileSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gTileReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "for (int PM_x = (int) pow(floor(sqrt((float)PM_gInputXSizeReduce123)), 2); PM_x \\< PM_gInputXSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}\n");
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gTileReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "int PM_gInputXSizeReduce123;\n"
						+ "int PM_gTileSizeReduce123;\n"
						+ "<finalVarType> PM_gExternalVarReduce123;\n"
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param1 += param2;\n"
						+ "return param1;\n"
						+ "}\n"
						+ "<type> __attribute__ ((kernel)) reduce123_tile(uint32_t x) {\n"
						+ "int PM_base = x*PM_gTileSizeReduce123;\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gTileSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base + PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gTileReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gTileSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gTileReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "for (int PM_x = (int) pow(floor(sqrt((float)PM_gInputXSizeReduce123)), 2); PM_x \\< PM_gInputXSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}\n");
		st.add("finalVarType", finalVar.typeName);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "rs_allocation PM_gOutputExternalVarReduce123;"
						+ "int PM_gInputXSizeReduce123;\n"
						+ "int PM_gExternalVarReduce123;\n"
						+ "<type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param1 += param2;\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gInputXSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}\n");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "int PM_gInputXSizeReduce123;\n"
						+ "<type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param1 += param2;\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gInputXSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}\n");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "int PM_gInputXSizeReduce123;\n"
						+ "<finalVarType> PM_gExternalVarReduce123;\n"
						+ "<type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param1 += param2;\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gInputXSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}\n");
		st.add("finalVarType", finalVar.typeName);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "rs_allocation PM_gOutputExternalVarReduce123;\n"
						+ "int PM_gInputXSizeReduce123;\n"
						+ "<nonFinalVarType> PM_gExternalVarReduce123;\n"
						+ "<type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param1 += param2;\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<PM_gInputXSizeReduce123; ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}\n");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests reduce operation call.
	 */
	@Test
	public void translateReduceOperationCall() throws Exception {
		// Parallel
		Operation operation = this
				.createReduceOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		ST st = new ST(
				"int PM_gTileSizeReduce123 = (int)Math.floor(Math.sqrt(PM_arrayVar1Out.getType().getX()));\n"
						+ "Type PM_gOutputDestVarReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(1).create();\n"
						+ "Allocation PM_gOutputDestVarReduce123 = Allocation.createTyped(PM_mRS, PM_gOutputDestVarReduce123Type);\n"
						+ "Type PM_gTileReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(PM_gTileSizeReduce123).create();\n"
						+ "Allocation PM_gTileReduce123 = Allocation.createTyped(PM_mRS, PM_gTileReduce123Type);\n"
						+ "<kernel>.set_PM_gOutputDestVarReduce123(PM_gOutputDestVarReduce123);\n"
						+ "<kernel>.set_PM_gInputReduce123(PM_arrayVar1Out);\n"
						+ "<kernel>.set_PM_gTileReduce123(PM_gTileReduce123);\n"
						+ "<kernel>.set_PM_gTileSizeReduce123(PM_gTileSizeReduce123);\n"
						+ "<kernel>.set_PM_gInputXSizeReduce123(PM_arrayVar1Out.getType().getX());\n"
						+ "<kernel>.forEach_reduce123_tile(PM_gTileReduce123);\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "<type>[] PM_gOutputDestVarReduce123Tmp = new <type>[1];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new <userLibraryType>(PM_gOutputDestVarReduce123Tmp[0]);");
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("type", getTranslatedParameterType());
		st.add("rsType", getRSType());
		st.add("userLibraryType", getParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputDestVarReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(1).create();\n"
						+ "Allocation PM_gOutputDestVarReduce123 = Allocation.createTyped(PM_mRS, PM_gOutputDestVarReduce123Type);\n"
						+ "<kernel>.set_PM_gOutputDestVarReduce123(PM_gOutputDestVarReduce123);\n"
						+ "<kernel>.set_PM_gInputReduce123(PM_arrayVar1Out);\n"
						+ "<kernel>.set_PM_gInputXSizeReduce123(PM_arrayVar1Out.getType().getX());\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "<type>[] PM_gOutputDestVarReduce123Tmp = new <type>[1];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new <userLibraryType>(PM_gOutputDestVarReduce123Tmp[0]);");
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("type", getTranslatedParameterType());
		st.add("rsType", getRSType());
		st.add("userLibraryType", getParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests method call translation.
	 */
	@Test
	public void translateMethodCall() throws Exception {
		// TODO Where is the getLength method in Array class?
	}
}
