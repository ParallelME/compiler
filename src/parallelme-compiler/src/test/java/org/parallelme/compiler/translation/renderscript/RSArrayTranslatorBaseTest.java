/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import java.util.List;

import org.junit.Test;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.ArrayTranslatorTest;
import org.parallelme.compiler.translation.SimpleTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.stringtemplate.v4.ST;

/**
 * Performs tests to validate RSArrayTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSArrayTranslatorBaseTest extends ArrayTranslatorTest {
	abstract protected String getParameterType();

	abstract protected String getTranslatedParameterType();

	abstract protected String getRSType();

	abstract protected String getMapRSType();

	@Override
	protected BaseUserLibraryTranslator getTranslator() {
		return new RSArrayTranslator(new SimpleTranslator());
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
		String varName = commonDefinitions
				.getVariableOutName(inputBind.variable);
		ST st = new ST(
				"<varName> = Allocation.createSized(PM_mRS, Element.<rsType>(PM_mRS), <dataVar>.length);\n"
						+ "<varName>.copyFrom(<dataVar>);");
		st.add("dataVar", inputBind.parameters.get(0));
		st.add("varName", varName);
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
		ST st = new ST("private Allocation <varName>;\nprivate boolean <fromImageVar> = false;");
		st.add("varName",
				commonDefinitions.getVariableOutName(inputBind.variable));
		st.add("fromImageVar",
				commonDefinitions.getFromImageBooleanName(inputBind.variable));
		String expectedTranslation = st.render();
		String translatedFunction = translator
				.translateObjDeclaration(inputBind);
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
		String varName = commonDefinitions
				.getVariableOutName(outputBind.variable);
		ST st = new ST("<varName>.copyTo(<varDest>);");
		st.add("varName", varName);
		st.add("varDest", outputBind.destinationObject);
		String expectedTranslation = st.render();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// bitmapVar = imageVar.toBitmap();
		outputBind = this.createOutputBind(OutputBindType.Assignment);
		st = new ST("<type>[] PM_javaArray;\n" + "if (<varDest> != null) {\n"
				+ "\tint PM_size = <varDest>.getType().getX();\n"
				+ "\tPM_size = <varFromImage> ? PM_size * 4 : PM_size;\n"
				+ "\tPM_javaArray = new <type>[PM_size];\n"
				+ "\t<varDest>.copyTo(PM_javaArray);\n" + "} else {\n"
				+ "\tPM_javaArray = new <type>[0];\n" + "}\n"
				+ "return PM_javaArray;");
		st.add("varName", varName);
		st.add("varDest",
				commonDefinitions.getVariableOutName(outputBind.variable));
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(outputBind.variable));
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
				"static <type> foreach123_func(<type> param1) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "<type> __attribute__((kernel)) foreach123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn foreach123_func(param1);\n" + "}\n");
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"<finalVarType> PM_g<finalVarName>Foreach123;\n"
						+ "static <type> foreach123_func(<type> param1) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "<type> __attribute__((kernel)) foreach123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn foreach123_func(param1);\n" + "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var1");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Foreach123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Foreach123;\n"
						+ "static <type> foreach123_func(<type> param1) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<type>(PM_gInputForeach123, foreach123_func(rsGetElementAt_<type>(PM_gInputForeach123, PM_x)), PM_x);\n"
						+ "}\n"
						+ "\trsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Foreach123, PM_g<nonFinalVarName>Foreach123, 0);\n"
						+ "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "static <type> foreach123_func(<type> param1) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<type>(PM_gInputForeach123, foreach123_func(rsGetElementAt_<type>(PM_gInputForeach123, PM_x)), PM_x);\n"
						+ "}\n" + "}\n");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Foreach123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Foreach123;\n"
						+ "<finalVarType> PM_g<finalVarName>Foreach123;\n"
						+ "static <type> foreach123_func(<type> param1) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<type>(PM_gInputForeach123, foreach123_func(rsGetElementAt_<type>(PM_gInputForeach123, PM_x)), PM_x);\n"
						+ "}\n"
						+ "\trsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Foreach123, PM_g<nonFinalVarName>Foreach123, 0);\n"
						+ "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Foreach123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Foreach123;\n"
						+ "static <type> foreach123_func(<type> param1) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<type>(PM_gInputForeach123, foreach123_func(rsGetElementAt_<type>(PM_gInputForeach123, PM_x)), PM_x);\n"
						+ "}\n"
						+ "\trsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Foreach123, PM_g<nonFinalVarName>Foreach123, 0);\n"
						+ "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests foreach operation call.
	 */
	@Test
	public void translateForeachOperationCall() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		ST st = new ST("<kernel>.forEach_foreach123(<varOut>, <varOut>);\n"
				+ "<varFromImage> = false;");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(operation.variable));
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"<kernel>.set_PM_g<UCFinalVarName>Foreach123(<finalVarName>);\n"
						+ "<kernel>.forEach_foreach123(<varOut>, <varOut>);\n"
						+ "<varFromImage> = false;");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(operation.variable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST("<kernel>.set_PM_gInputForeach123(<varOut>);\n"
				+ "<kernel>.invoke_foreach123();\n" + "<varFromImage> = false;");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(operation.variable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential non-final and final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Allocation PM_gOutput<UCNonFinalVarName>Foreach123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Foreach123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Foreach123(PM_gOutput<UCNonFinalVarName>Foreach123);"
						+ "<kernel>.set_PM_gInputForeach123(<varOut>);\n"
						+ "<kernel>.set_PM_g<UCFinalVarName>Foreach123(<finalVarName>);\n"
						+ "<kernel>.invoke_foreach123();\n"
						+ "PM_gOutput<UCNonFinalVarName>Foreach123.copyTo(<nonFinalVarName>);\n"
						+ "<varFromImage> = false;");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(operation.variable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential non-final
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Allocation PM_gOutput<UCNonFinalVarName>Foreach123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Foreach123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Foreach123(PM_gOutput<UCNonFinalVarName>Foreach123);"
						+ "<kernel>.set_PM_gInputForeach123(<varOut>);\n"
						+ "<kernel>.invoke_foreach123();\n"
						+ "PM_gOutput<UCNonFinalVarName>Foreach123.copyTo(<nonFinalVarName>);\n"
						+ "<varFromImage> = false;");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(operation.variable));
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
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;\n"
						+ "return param2;\n"
						+ "}\n"
						+ "<type> __attribute__ ((kernel)) reduce123_tile(uint32_t x) {\n"
						+ "int PM_base = x*rsAllocationGetDimX(PM_gTileReduce123);\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gTileReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base + PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gTileReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gTileReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gTileReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "for (int PM_x = (int) pow(floor(sqrt((float)rsAllocationGetDimX(PM_gInputReduce123))), 2); PM_x \\< rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
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
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gTileReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "<finalVarType> PM_g<finalVarName>Reduce123;\n"
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;\n"
						+ "return param2;\n"
						+ "}\n"
						+ "<type> __attribute__ ((kernel)) reduce123_tile(uint32_t x) {\n"
						+ "int PM_base = x*rsAllocationGetDimX(PM_gTileReduce123);\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gTileReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_base + PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "return param1;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gTileReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gTileReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gTileReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "for (int PM_x = (int) pow(floor(sqrt((float)rsAllocationGetDimX(PM_gInputReduce123))), 2); PM_x \\< rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}\n");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Reduce123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Reduce123;\n"
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;\n"
						+ "return param2;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Reduce123, PM_g<nonFinalVarName>Reduce123, 0);\n"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;\n"
						+ "return param2;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Reduce123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Reduce123;\n"
						+ "<finalVarType> PM_g<finalVarName>Reduce123;\n"
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;\n"
						+ "return param2;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Reduce123, PM_g<nonFinalVarName>Reduce123, 0);\n"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
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
						+ "rs_allocation PM_gOutput<nonFinalVarName>Reduce123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Reduce123;\n"
						+ "static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;\n"
						+ "return param2;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "<type> param1 = rsGetElementAt_<type>(PM_gInputReduce123, 0);\n"
						+ "<type> param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_<type>(PM_gInputReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_<type>(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Reduce123, PM_g<nonFinalVarName>Reduce123, 0);\n"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
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
		// Parallel with final variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"int PM_gTileSizeReduce123 = (int)Math.floor(Math.sqrt(PM_arrayVar1Out.getType().getX()));\n"
						+ "Type PM_gOutputDestVarReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(1).create();\n"
						+ "Allocation PM_gOutputDestVarReduce123 = Allocation.createTyped(PM_mRS, PM_gOutputDestVarReduce123Type);\n"
						+ "Type PM_gTileReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(PM_gTileSizeReduce123).create();\n"
						+ "Allocation PM_gTileReduce123 = Allocation.createTyped(PM_mRS, PM_gTileReduce123Type);\n"
						+ "<kernel>.set_PM_gOutputDestVarReduce123(PM_gOutputDestVarReduce123);\n"
						+ "<kernel>.set_PM_gInputReduce123(PM_arrayVar1Out);\n"
						+ "<kernel>.set_PM_gTileReduce123(PM_gTileReduce123);\n"
						+ "<kernel>.set_PM_g<UCFinalVarName>Reduce123(<finalVarName>);\n"
						+ "<kernel>.forEach_reduce123_tile(PM_gTileReduce123);\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "<type>[] PM_gOutputDestVarReduce123Tmp = new <type>[1];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new <userLibraryType>(PM_gOutputDestVarReduce123Tmp[0]);");
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("type", getTranslatedParameterType());
		st.add("rsType", getRSType());
		st.add("userLibraryType", getParameterType());
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
		expectedTranslation = st.render();
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
		// Sequential with non-final and final variable
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputDestVarReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(1).create();\n"
						+ "Allocation PM_gOutputDestVarReduce123 = Allocation.createTyped(PM_mRS, PM_gOutputDestVarReduce123Type);\n"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Reduce123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Reduce123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Reduce123(PM_gOutput<UCNonFinalVarName>Reduce123);\n"
						+ "<kernel>.set_PM_gOutputDestVarReduce123(PM_gOutputDestVarReduce123);\n"
						+ "<kernel>.set_PM_gInputReduce123(<varOut>);\n"
						+ "<kernel>.set_PM_g<UCFinalVarName>Reduce123(<finalVarName>);\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "PM_gOutput<UCNonFinalVarName>Reduce123.copyTo(<nonFinalVarName>);\n"
						+ "<type>[] PM_gOutputDestVarReduce123Tmp = new <type>[1];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new <userLibraryType>(PM_gOutputDestVarReduce123Tmp[0]);");
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("type", getTranslatedParameterType());
		st.add("rsType", getRSType());
		st.add("userLibraryType", getParameterType());
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		expectedTranslation = st.render();
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputDestVarReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(1).create();\n"
						+ "Allocation PM_gOutputDestVarReduce123 = Allocation.createTyped(PM_mRS, PM_gOutputDestVarReduce123Type);\n"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Reduce123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Reduce123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Reduce123(PM_gOutput<UCNonFinalVarName>Reduce123);\n"
						+ "<kernel>.set_PM_gOutputDestVarReduce123(PM_gOutputDestVarReduce123);\n"
						+ "<kernel>.set_PM_gInputReduce123(<varOut>);\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "PM_gOutput<UCNonFinalVarName>Reduce123.copyTo(<nonFinalVarName>);\n"
						+ "<type>[] PM_gOutputDestVarReduce123Tmp = new <type>[1];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new <userLibraryType>(PM_gOutputDestVarReduce123Tmp[0]);");
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("type", getTranslatedParameterType());
		st.add("rsType", getRSType());
		st.add("userLibraryType", getParameterType());
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests map operation translation.
	 */
	@Test
	public void translateMapOperation() throws Exception {
		// Parallel
		Operation operation = this.createMapOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		ST st = new ST(
				"static <mapType> map123_func(<type> param1) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "<mapType> __attribute__((kernel)) map123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn map123_func(param1);\n" + "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createMapOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"<finalVarType> PM_g<finalVarName>Map123;\n"
						+ "static <mapType> map123_func(<type> param1) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "<mapType> __attribute__((kernel)) map123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn map123_func(param1);\n" + "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createMapOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputMap123;\n"
						+ "rs_allocation PM_gOutputMap123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Map123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Map123;\n"
						+ "static <mapType> map123_func(<type> param1) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x)), PM_x);\n"
						+ "\t}\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Map123, PM_g<nonFinalVarName>Map123, 0);\n"
						+ "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarType", nonFinalVar.typeName);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createMapOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputMap123;\n"
						+ "rs_allocation PM_gOutputMap123;\n"
						+ "static <mapType> map123_func(<type> param1) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x)), PM_x);\n"
						+ "\t}\n" + "}\n");
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputMap123;\n"
						+ "rs_allocation PM_gOutputMap123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Map123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Map123;\n"
						+ "<finalVarType> PM_g<finalVarName>Map123;\n"
						+ "static <mapType> map123_func(<type> param1) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x)), PM_x);\n"
						+ "\t}\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Map123, PM_g<nonFinalVarName>Map123, 0);\n"
						+ "}\n");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputMap123;\n"
						+ "rs_allocation PM_gOutputMap123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Map123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Map123;\n"
						+ "static <mapType> map123_func(<type> param1) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x)), PM_x);\n"
						+ "\t}\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Map123, PM_g<nonFinalVarName>Map123, 0);\n"
						+ "}\n");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests map operation call.
	 */
	@Test
	public void translateMapOperationCall() throws Exception {
		// Parallel
		Operation operation = this.createMapOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		ST st = new ST(
				"Type <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))"
						+ ".setX(<varIn>.getType().getX())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "<kernel>.forEach_map123(<varIn>, <varOut>);\n"
						+ "<varFromImage> = false;");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(operation.destinationVariable));
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final variable
		operation = this.createMapOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))"
						+ ".setX(<varIn>.getType().getX())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "<kernel>.set_PM_g<UCFinalVarName>Map123(<finalVarName>);"
						+ "<kernel>.forEach_map123(<varIn>, <varOut>);\n"
						+ "<varFromImage> = false;");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("finalVarName", finalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createMapOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))"
						+ ".setX(<varIn>.getType().getX())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "<kernel>.set_PM_gInputMap123(<varIn>);"
						+ "<kernel>.set_PM_gOutputMap123(<varOut>);"
						+ "<kernel>.invoke_map123();\n"
						+ "<varFromImage> = false;");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))"
						+ ".setX(<varIn>.getType().getX())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Map123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Map123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Map123(PM_gOutput<UCNonFinalVarName>Map123);\n"
						+ "<kernel>.set_PM_gInputMap123(<varIn>);"
						+ "<kernel>.set_PM_gOutputMap123(<varOut>);"
						+ "<kernel>.set_PM_g<UCFinalVarName>Map123(<finalVarName>);\n"
						+ "<kernel>.invoke_map123();\n"
						+ "PM_gOutput<UCNonFinalVarName>Map123.copyTo(<nonFinalVarName>);\n"
						+ "<varFromImage> = false;");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))"
						+ ".setX(<varIn>.getType().getX())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Map123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Map123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Map123(PM_gOutput<UCNonFinalVarName>Map123);\n"
						+ "<kernel>.set_PM_gInputMap123(<varIn>);"
						+ "<kernel>.set_PM_gOutputMap123(<varOut>);"
						+ "<kernel>.invoke_map123();"
						+ "PM_gOutput<UCNonFinalVarName>Map123.copyTo(<nonFinalVarName>);\n"
						+ "<varFromImage> = false;");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests filter operation translation.
	 */
	@Test
	public void translateFilterOperation() throws Exception {
		// Parallel
		Operation operation = this
				.createFilterOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		ST st = new ST(
				"rs_allocation PM_gInputFilter123;\n"
						+ "rs_allocation PM_gOutputFilter123;\n"
						+ "rs_allocation PM_gOutputXSizeFilter123_Allocation;\n"
						+ "rs_allocation PM_gOutputTileFilter123;\n"
						+ "int PM_gOutputXSizeFilter123;\n"
						+ "static bool filter123_func(<type> param1) {\n"
						+ "\treturn param1 > 2;\n"
						+ "}\n"
						+ "<type> __attribute__((kernel)) filter123_tile(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\tif (filter123_func(param1)) {\n"
						+ "\t\trsAtomicInc(&PM_gOutputXSizeFilter123);\n"
						+ "\t\treturn x;\n"
						+ "\t} else {\n"
						+ "\t\treturn -1;\n"
						+ "\t}\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" + "\t}\n" + "}");
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createFilterOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputFilter123;\n"
						+ "rs_allocation PM_gOutputFilter123;\n"
						+ "rs_allocation PM_gOutputXSizeFilter123_Allocation;\n"
						+ "rs_allocation PM_gOutputTileFilter123;\n"
						+ "int PM_gOutputXSizeFilter123;\n"
						+ "<finalVarType> PM_g<finalVarName>Filter123;\n"
						+ "static bool filter123_func(<type> param1) {\n"
						+ "\treturn param1 > 2;\n"
						+ "}\n"
						+ "<type> __attribute__((kernel)) filter123_tile(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\tif (filter123_func(param1)) {\n"
						+ "\t\trsAtomicInc(&PM_gOutputXSizeFilter123);\n"
						+ "\t\treturn x;\n"
						+ "\t} else {\n"
						+ "\t\treturn -1;\n"
						+ "\t}\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" + "\t}\n" + "}");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createFilterOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputFilter123;\n"
						+ "rs_allocation PM_gOutputFilter123;\n"
						+ "rs_allocation PM_gOutputXSizeFilter123_Allocation;\n"
						+ "rs_allocation PM_gOutputTileFilter123;\n"
						+ "rs_allocation PM_gOutput<UCNonFinalVarName>Filter123;\n"
						+ "int PM_gOutputXSizeFilter123;\n"
						+ "int PM_g<UCNonFinalVarName>Filter123;\n"
						+ "static bool filter123_func(<type> param1) {\n"
						+ "\treturn param1 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "\trsSetElementAt_int(PM_gOutput<UCNonFinalVarName>Filter123, PM_g<UCNonFinalVarName>Filter123, 0);\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" + "\t}\n" + "}");
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createFilterOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputFilter123;\n"
						+ "rs_allocation PM_gOutputFilter123;\n"
						+ "rs_allocation PM_gOutputXSizeFilter123_Allocation;\n"
						+ "rs_allocation PM_gOutputTileFilter123;\n"
						+ "int PM_gOutputXSizeFilter123;\n"
						+ "static bool filter123_func(<type> param1) {\n"
						+ "\treturn param1 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" + "\t}\n" + "}");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createFilterOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputFilter123;\n"
						+ "rs_allocation PM_gOutputFilter123;\n"
						+ "rs_allocation PM_gOutputXSizeFilter123_Allocation;\n"
						+ "rs_allocation PM_gOutputTileFilter123;\n"
						+ "rs_allocation PM_gOutput<UCNonFinalVarName>Filter123;\n"
						+ "int PM_gOutputXSizeFilter123;\n"
						+ "int PM_g<UCNonFinalVarName>Filter123;\n"
						+ "<finalVarType> PM_g<finalVarName>Filter123;\n"
						+ "static bool filter123_func(<type> param1) {\n"
						+ "\treturn param1 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "\trsSetElementAt_int(PM_gOutput<UCNonFinalVarName>Filter123, PM_g<UCNonFinalVarName>Filter123, 0);\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" + "\t}\n" + "}");
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createFilterOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputFilter123;\n"
						+ "rs_allocation PM_gOutputFilter123;\n"
						+ "rs_allocation PM_gOutputXSizeFilter123_Allocation;\n"
						+ "rs_allocation PM_gOutputTileFilter123;\n"
						+ "rs_allocation PM_gOutput<UCNonFinalVarName>Filter123;\n"
						+ "int PM_gOutputXSizeFilter123;\n"
						+ "int PM_g<UCNonFinalVarName>Filter123;\n"
						+ "static bool filter123_func(<type> param1) {\n"
						+ "\treturn param1 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "\trsSetElementAt_int(PM_gOutput<UCNonFinalVarName>Filter123, PM_g<UCNonFinalVarName>Filter123, 0);\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" + "\t}\n" + "}");
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests map operation call.
	 */
	@Test
	public void translateFilterOperationCall() throws Exception {
		// Parallel
		Operation operation = this
				.createFilterOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		ST st = new ST(
				"Type PM_gOutputTileFilter123Type = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t.setX(<varIn>.getType().getX())\n"
						+ "\t.create();\n"
						+ "Allocation PM_gOutputTileFilter123 = Allocation.createTyped(PM_mRS, PM_gOutputTileFilter123Type);\n"
						+ "Type PM_gOutputXSizeFilter123_AllocationType = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t.setX(1)\n"
						+ "\t.create();\n"
						+ "Allocation PM_gOutputXSizeFilter123_Allocation = Allocation.createTyped(PM_mRS, PM_gOutputXSizeFilter123_AllocationType);\n"
						+ "<kernel>.set_PM_gOutputTileFilter123(PM_gOutputTileFilter123);\n"
						+ "<kernel>.set_PM_gOutputXSizeFilter123_Allocation(PM_gOutputXSizeFilter123_Allocation);\n"
						+ "<kernel>.forEach_filter123_tile(<varIn>, PM_gOutputTileFilter123);\n"
						+ "<kernel>.invoke_filter123_setAllocationSize();\n"
						+ "int PM_size[] = new int[1];\n"
						+ "<kernel>.get_PM_gOutputXSizeFilter123_Allocation().copyTo(PM_size);\n"
						+ "if (PM_size[0] > 0) {\n"
						+ "\tType <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))\n"
						+ "\t\t.setX(PM_size[0])\n"
						+ "\t\t.create();\n"
						+ "\t<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);\n"
						+ "\t<varFromImage> = false;\n"
						+ "\t<kernel>.set_PM_gOutputFilter123(<varOut>);\n"
						+ "\t<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "\t<kernel>.invoke_filter123();\n" + "}");
		st.add("rsType", getRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final variable
		operation = this.createFilterOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputTileFilter123Type = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t.setX(<varIn>.getType().getX())\n"
						+ "\t.create();\n"
						+ "Allocation PM_gOutputTileFilter123 = Allocation.createTyped(PM_mRS, PM_gOutputTileFilter123Type);\n"
						+ "Type PM_gOutputXSizeFilter123_AllocationType = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t.setX(1)\n"
						+ "\t.create();\n"
						+ "Allocation PM_gOutputXSizeFilter123_Allocation = Allocation.createTyped(PM_mRS, PM_gOutputXSizeFilter123_AllocationType);\n"
						+ "<kernel>.set_PM_gOutputTileFilter123(PM_gOutputTileFilter123);\n"
						+ "<kernel>.set_PM_gOutputXSizeFilter123_Allocation(PM_gOutputXSizeFilter123_Allocation);\n"
						+ "<kernel>.set_PM_g<UCFinalVarName>Filter123(<finalVarName>);"
						+ "<kernel>.forEach_filter123_tile(<varIn>, PM_gOutputTileFilter123);\n"
						+ "<kernel>.invoke_filter123_setAllocationSize();\n"
						+ "int PM_size[] = new int[1];\n"
						+ "<kernel>.get_PM_gOutputXSizeFilter123_Allocation().copyTo(PM_size);\n"
						+ "if (PM_size[0] > 0) {\n"
						+ "\tType <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))\n"
						+ "\t\t.setX(PM_size[0])\n"
						+ "\t\t.create();\n"
						+ "\t<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);\n"
						+ "\t<varFromImage> = false;\n"
						+ "\t<kernel>.set_PM_gOutputFilter123(<varOut>);\n"
						+ "\t<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "\t<kernel>.invoke_filter123();\n" + "}");
		st.add("rsType", getRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("finalVarName", finalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createFilterOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputTileFilter123Type = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(<varIn>.getType().getX())\n"
						+ "\t\t.create();\n"
						+ "Allocation PM_gOutputTileFilter123 = Allocation.createTyped(PM_mRS, PM_gOutputTileFilter123Type);\n"
						+ "Type PM_gOutputXSizeFilter123_AllocationType = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(1)\n"
						+ "\t\t.create();\n"
						+ "Allocation PM_gOutputXSizeFilter123_Allocation = Allocation.createTyped(PM_mRS, PM_gOutputXSizeFilter123_AllocationType);\n"
						+ "<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "<kernel>.set_PM_gOutputTileFilter123(PM_gOutputTileFilter123);\n"
						+ "<kernel>.set_PM_gOutputXSizeFilter123_Allocation(PM_gOutputXSizeFilter123_Allocation);\n"
						+ "<kernel>.invoke_filter123_tile();\n"
						+ "<kernel>.invoke_filter123_setAllocationSize();\n"
						+ "int PM_size[] = new int[1];\n"
						+ "<kernel>.get_PM_gOutputXSizeFilter123_Allocation().copyTo(PM_size);\n"
						+ "if (PM_size[0] > 0) {\n"
						+ "\tType <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))\n"
						+ "\t\t\t.setX(PM_size[0])\n"
						+ "\t\t\t.create();\n"
						+ "\t<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);\n"
						+ "\t<varFromImage> = false;\n"
						+ "\t<kernel>.set_PM_gOutputFilter123(<varOut>);\n"
						+ "\t<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "\t<kernel>.invoke_filter123();\n" + "}");
		st.add("rsType", getRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation = this.createFilterOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputTileFilter123Type = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(<varIn>.getType().getX())\n"
						+ "\t\t.create();\n"
						+ "Allocation PM_gOutputTileFilter123 = Allocation.createTyped(PM_mRS, PM_gOutputTileFilter123Type);\n"
						+ "Type PM_gOutputXSizeFilter123_AllocationType = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(1)\n"
						+ "\t\t.create();\n"
						+ "Allocation PM_gOutputXSizeFilter123_Allocation = Allocation.createTyped(PM_mRS, PM_gOutputXSizeFilter123_AllocationType);\n"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Filter123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Filter123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Filter123(PM_gOutput<UCNonFinalVarName>Filter123);\n"
						+ "<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "<kernel>.set_PM_gOutputTileFilter123(PM_gOutputTileFilter123);\n"
						+ "<kernel>.set_PM_gOutputXSizeFilter123_Allocation(PM_gOutputXSizeFilter123_Allocation);\n"
						+ "<kernel>.set_PM_g<UCFinalVarName>Filter123(<finalVarName>);"
						+ "<kernel>.invoke_filter123_tile();\n"
						+ "<kernel>.invoke_filter123_setAllocationSize();\n"
						+ "PM_gOutput<UCNonFinalVarName>Filter123.copyTo(<nonFinalVarName>);\n"
						+ "int PM_size[] = new int[1];\n"
						+ "<kernel>.get_PM_gOutputXSizeFilter123_Allocation().copyTo(PM_size);\n"
						+ "if (PM_size[0] > 0) {\n"
						+ "\tType <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))\n"
						+ "\t\t\t.setX(PM_size[0])\n"
						+ "\t\t\t.create();\n"
						+ "\t<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);\n"
						+ "\t<varFromImage> = false;\n"
						+ "\t<kernel>.set_PM_gOutputFilter123(<varOut>);\n"
						+ "\t<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "\t<kernel>.invoke_filter123();\n" + "}");
		st.add("rsType", getRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createFilterOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputTileFilter123Type = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(<varIn>.getType().getX())\n"
						+ "\t\t.create();\n"
						+ "Allocation PM_gOutputTileFilter123 = Allocation.createTyped(PM_mRS, PM_gOutputTileFilter123Type);\n"
						+ "Type PM_gOutputXSizeFilter123_AllocationType = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(1)\n"
						+ "\t\t.create();\n"
						+ "Allocation PM_gOutputXSizeFilter123_Allocation = Allocation.createTyped(PM_mRS, PM_gOutputXSizeFilter123_AllocationType);\n"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Filter123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Filter123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Filter123(PM_gOutput<UCNonFinalVarName>Filter123);\n"
						+ "<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "<kernel>.set_PM_gOutputTileFilter123(PM_gOutputTileFilter123);\n"
						+ "<kernel>.set_PM_gOutputXSizeFilter123_Allocation(PM_gOutputXSizeFilter123_Allocation);\n"
						+ "<kernel>.invoke_filter123_tile();\n"
						+ "<kernel>.invoke_filter123_setAllocationSize();\n"
						+ "PM_gOutput<UCNonFinalVarName>Filter123.copyTo(<nonFinalVarName>);\n"
						+ "int PM_size[] = new int[1];\n"
						+ "<kernel>.get_PM_gOutputXSizeFilter123_Allocation().copyTo(PM_size);\n"
						+ "if (PM_size[0] > 0) {\n"
						+ "\tType <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))\n"
						+ "\t\t\t.setX(PM_size[0])\n"
						+ "\t\t\t.create();\n"
						+ "\t<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);\n"
						+ "\t<varFromImage> = false;\n"
						+ "\t<kernel>.set_PM_gOutputFilter123(<varOut>);\n"
						+ "\t<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "\t<kernel>.invoke_filter123();\n" + "}");
		st.add("rsType", getRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
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
