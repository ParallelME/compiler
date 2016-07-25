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
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.translation.ImageTranslatorTest;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.Pixel;
import org.stringtemplate.v4.ST;

/**
 * Base class for RenderScript image tests.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSImageTranslatorTest extends ImageTranslatorTest {
	protected String getRSType() {
		return "F32_4";
	}

	@Override
	protected String getParameterType() {
		return Pixel.getInstance().getClassName();
	}

	@Override
	protected String getMapType() {
		return Float32.getInstance().getClassName();
	}

	@Override
	protected String getTranslatedParameterType() {
		return "float4";
	}

	@Override
	protected String getTranslatedMapType() {
		return "float";
	}

	protected String getMapRSType() {
		return "F32";
	}

	abstract protected String getClassName();

	/**
	 * Tests foreach operation translation.
	 */
	@Test
	public void translateForeachOperation() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		String expectedTranslation = "static float4 foreach123_func(float4 param1, uint32_t x, uint32_t y) {\n"
				+ "\tparam1.s0 = 123;\n"
				+ "\treturn param1;\n"
				+ "}\n"
				+ "float4 __attribute__((kernel)) foreach123(float4 param1, uint32_t x, uint32_t y) {\n"
				+ "\treturn foreach123_func(param1, x, y);\n" + "}\n";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		ST st = new ST(
				"<finalVarType> PM_g<finalVarName>Foreach123;\n"
						+ "static float4 foreach123_func(float4 param1, uint32_t x, uint32_t y) {\n"
						+ "\tparam1.s0 = 123;\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "float4 __attribute__((kernel)) foreach123(float4 param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn foreach123_func(param1, x, y);\n" + "}\n");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Foreach123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Foreach123;\n"
						+ "static float4 foreach123_func(float4 param1, uint32_t x, uint32_t y) {\n"
						+ "\tparam1.s0 = 123;\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "for (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "for (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputForeach123); ++PM_y) {\n"
						+ "\trsSetElementAt_float4(PM_gInputForeach123, foreach123_func(rsGetElementAt_float4(PM_gInputForeach123,PM_x,PM_y)),PM_x,PM_y);\n"
						+ "}\n"
						+ "}\n"
						+ "rsSetElementAt_int(PM_gOutput<nonFinalVarName>Foreach123, PM_g<nonFinalVarName>Foreach123, 0);\n"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "static float4 foreach123_func(float4 param1, uint32_t x, uint32_t y) {\n"
						+ "\tparam1.s0 = 123;\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "for (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "for (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputForeach123); ++PM_y) {\n"
						+ "\trsSetElementAt_float4(PM_gInputForeach123, foreach123_func(rsGetElementAt_float4(PM_gInputForeach123,PM_x,PM_y)),PM_x,PM_y);\n"
						+ "}\n" + "}\n" + "}");
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputForeach123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Foreach123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Foreach123;\n"
						+ "<finalVarType> PM_g<finalVarName>Foreach123;\n"
						+ "static float4 foreach123_func(float4 param1, uint32_t x, uint32_t y) {\n"
						+ "\tparam1.s0 = 123;\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "for (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "for (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputForeach123); ++PM_y) {\n"
						+ "\trsSetElementAt_float4(PM_gInputForeach123, foreach123_func(rsGetElementAt_float4(PM_gInputForeach123,PM_x,PM_y)),PM_x,PM_y);\n"
						+ "}\n"
						+ "}\n"
						+ "rsSetElementAt_int(PM_gOutput<nonFinalVarName>Foreach123, PM_g<nonFinalVarName>Foreach123, 0);\n"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
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
						+ "static float4 foreach123_func(float4 param1, uint32_t x, uint32_t y) {\n"
						+ "\tparam1.s0 = 123;\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "void foreach123() {\n"
						+ "for (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputForeach123); ++PM_x) {\n"
						+ "for (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputForeach123); ++PM_y) {\n"
						+ "\trsSetElementAt_float4(PM_gInputForeach123, foreach123_func(rsGetElementAt_float4(PM_gInputForeach123,PM_x,PM_y)),PM_x,PM_y);\n"
						+ "}\n"
						+ "}\n"
						+ "rsSetElementAt_int(PM_gOutput<nonFinalVarName>Foreach123, PM_g<nonFinalVarName>Foreach123, 0);\n"
						+ "}");
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
		ST st = new ST("<kernel>.forEach_foreach123(<varOut>, <varOut>);");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
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
						+ "<kernel>.forEach_foreach123(<varOut>, <varOut>);");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST("<kernel>.set_PM_gInputForeach123(<varOut>);\n"
				+ "<kernel>.invoke_foreach123();");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
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
						+ "PM_gOutput<UCNonFinalVarName>Foreach123.copyTo(<nonFinalVarName>);");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("finalVarName", finalVar.name);
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
						+ "PM_gOutput<UCNonFinalVarName>Foreach123.copyTo(<nonFinalVarName>);");
		st.add("varOut",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
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
		String expectedTranslation = "rs_allocation PM_gInputReduce123;\n"
				+ "rs_allocation PM_gTileReduce123;\n"
				+ "rs_allocation PM_gOutputDestVarReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123; \n"
				+ "param2.s1 = 456; \n"
				+ "return param2;\n"
				+ "}\n"
				+ "float4 __attribute__((kernel)) reduce123_tile(uint32_t x) {\n"
				+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, x, 0);\n"
				+ "float4 param2;\n"
				+ "for (int PM_x=1; PM_x<rsAllocationGetDimX(PM_gTileReduce123); ++PM_x) {\n"
				+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, x, PM_x);\n"
				+ "param1 = reduce123_func(param1, param2);\n"
				+ "}\n"
				+ "return param1;\n"
				+ "}"
				+ "void reduce123() {\n"
				+ "float4 param1 = rsGetElementAt_float4(PM_gTileReduce123, 0);\n"
				+ "float4 param2;\n"
				+ "for (int PM_x=1; PM_x<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
				+ "param2 = rsGetElementAt_float4(PM_gTileReduce123, PM_x);\n"
				+ "param1 = reduce123_func(param1, param2);\n"
				+ "}\n"
				+ "rsSetElementAt_float4(PM_gOutputDestVarReduce123, param1, 0);\n"
				+ "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		ST st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gTileReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "<finalVarType> PM_g<finalVarName>Reduce123;\n"
						+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
						+ "param1.s0 = 123; \n"
						+ "param2.s1 = 456; \n"
						+ "return param2;\n"
						+ "}\n"
						+ "float4 __attribute__((kernel)) reduce123_tile(uint32_t x) {\n"
						+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, x, 0);\n"
						+ "float4 param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gTileReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, x, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "return param1;\n"
						+ "}"
						+ "void reduce123() {\n"
						+ "float4 param1 = rsGetElementAt_float4(PM_gTileReduce123, 0);\n"
						+ "float4 param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "param2 = rsGetElementAt_float4(PM_gTileReduce123, PM_x);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "rsSetElementAt_float4(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "}");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
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
						+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
						+ "param1.s0 = 123; \n"
						+ "param2.s1 = 456; \n"
						+ "return param2;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, 0);\n"
						+ "float4 param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "for (int PM_y=1; PM_y\\<rsAllocationGetDimY(PM_gInputReduce123); ++PM_y) {\n"
						+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, PM_x, PM_y);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "}\n"
						+ "rsSetElementAt_float4(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Reduce123, PM_g<nonFinalVarName>Reduce123, 0);\n"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputReduce123;\n"
				+ "rs_allocation PM_gOutputDestVarReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123; \n"
				+ "param2.s1 = 456; \n"
				+ "return param2;\n"
				+ "}\n"
				+ "void reduce123() {\n"
				+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, 0);\n"
				+ "float4 param2;\n"
				+ "for (int PM_x=1; PM_x<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
				+ "for (int PM_y=1; PM_y<rsAllocationGetDimY(PM_gInputReduce123); ++PM_y) {\n"
				+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, PM_x, PM_y);\n"
				+ "param1 = reduce123_func(param1, param2);\n"
				+ "}\n"
				+ "}\n"
				+ "rsSetElementAt_float4(PM_gOutputDestVarReduce123, param1, 0);\n"
				+ "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"rs_allocation PM_gInputReduce123;\n"
						+ "rs_allocation PM_gOutputDestVarReduce123;\n"
						+ "rs_allocation PM_gOutput<nonFinalVarName>Reduce123;\n"
						+ "<finalVarType> PM_g<finalVarName>Reduce123;\n"
						+ "<nonFinalVarType> PM_g<nonFinalVarName>Reduce123;\n"
						+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
						+ "param1.s0 = 123; \n"
						+ "param2.s1 = 456; \n"
						+ "return param2;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, 0);\n"
						+ "float4 param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "for (int PM_y=1; PM_y\\<rsAllocationGetDimY(PM_gInputReduce123); ++PM_y) {\n"
						+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, PM_x, PM_y);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "}\n"
						+ "rsSetElementAt_float4(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Reduce123, PM_g<nonFinalVarName>Reduce123, 0);\n"
						+ "}");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVarName", upperCaseFirstLetter(finalVar.name));
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
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
						+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
						+ "param1.s0 = 123; \n"
						+ "param2.s1 = 456; \n"
						+ "return param2;\n"
						+ "}\n"
						+ "void reduce123() {\n"
						+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, 0);\n"
						+ "float4 param2;\n"
						+ "for (int PM_x=1; PM_x\\<rsAllocationGetDimX(PM_gInputReduce123); ++PM_x) {\n"
						+ "for (int PM_y=1; PM_y\\<rsAllocationGetDimY(PM_gInputReduce123); ++PM_y) {\n"
						+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, PM_x, PM_y);\n"
						+ "param1 = reduce123_func(param1, param2);\n"
						+ "}\n"
						+ "}\n"
						+ "rsSetElementAt_float4(PM_gOutputDestVarReduce123, param1, 0);\n"
						+ "rsSetElementAt_<nonFinalVarType>(PM_gOutput<nonFinalVarName>Reduce123, PM_g<nonFinalVarName>Reduce123, 0);\n"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
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
				"int PM_gTileSizeReduce123 = PM_imageVar1Out.getType().getY();\n"
						+ "Type PM_gOutputDestVarReduce123Type = newType.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(1).create();\n"
						+ "Allocation PM_gOutputDestVarReduce123 = Allocation.createTyped(PM_mRS, PM_gOutputDestVarReduce123Type);\n"
						+ "Type PM_gTileReduce123Type = new Type.Builder(PM_mRS,Element.<rsType>(PM_mRS)).setX(PM_gTileSizeReduce123).create();\n"
						+ "Allocation PM_gTileReduce123 = Allocation.createTyped(PM_mRS, PM_gTileReduce123Type);\n"
						+ "<kernel>.set_PM_gOutputDestVarReduce123(PM_gOutputDestVarReduce123);\n"
						+ "<kernel>.set_PM_gInputReduce123(PM_imageVar1Out);\n"
						+ "<kernel>.set_PM_gTileReduce123(PM_gTileReduce123);\n"
						+ "<kernel>.forEach_reduce123_tile(PM_gTileReduce123);\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "float[] PM_gOutputDestVarReduce123Tmp = new float[4];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new Pixel(PM_gOutputDestVarReduce123Tmp[0],PM_gOutputDestVarReduce123Tmp[1],PM_gOutputDestVarReduce123Tmp[2],PM_gOutputDestVarReduce123Tmp[3],-1,-1);");
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("rsType", getRSType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"int PM_gTileSizeReduce123 = PM_imageVar1Out.getType().getY();\n"
						+ "Type PM_gOutputDestVarReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(1).create();\n"
						+ "Allocation PM_gOutputDestVarReduce123 = Allocation.createTyped(PM_mRS, PM_gOutputDestVarReduce123Type);\n"
						+ "Type PM_gTileReduce123Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS)).setX(PM_gTileSizeReduce123).create();\n"
						+ "Allocation PM_gTileReduce123 = Allocation.createTyped(PM_mRS, PM_gTileReduce123Type);\n"
						+ "<kernel>.set_PM_gOutputDestVarReduce123(PM_gOutputDestVarReduce123);\n"
						+ "<kernel>.set_PM_gInputReduce123(PM_imageVar1Out);\n"
						+ "<kernel>.set_PM_gTileReduce123(PM_gTileReduce123);\n"
						+ "<kernel>.set_PM_g<UCFinalVarName>Reduce123(<finalVarName>);\n"
						+ "<kernel>.forEach_reduce123_tile(PM_gTileReduce123);\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "float[] PM_gOutputDestVarReduce123Tmp = new float[4];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new Pixel(PM_gOutputDestVarReduce123Tmp[0],PM_gOutputDestVarReduce123Tmp[1],PM_gOutputDestVarReduce123Tmp[2],PM_gOutputDestVarReduce123Tmp[3],-1,-1);");
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
						+ "<kernel>.set_PM_gInputReduce123(PM_imageVar1Out);\n"
						+ "<kernel>.invoke_reduce123();\n"
						+ "float[] PM_gOutputDestVarReduce123Tmp = new float[4];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new Pixel(PM_gOutputDestVarReduce123Tmp[0],PM_gOutputDestVarReduce123Tmp[1],PM_gOutputDestVarReduce123Tmp[2],PM_gOutputDestVarReduce123Tmp[3],-1,-1);");
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
						+ "float[] PM_gOutputDestVarReduce123Tmp = new float[4];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new Pixel(PM_gOutputDestVarReduce123Tmp[0],PM_gOutputDestVarReduce123Tmp[1],PM_gOutputDestVarReduce123Tmp[2],PM_gOutputDestVarReduce123Tmp[3],-1,-1);");
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
						+ "float[] PM_gOutputDestVarReduce123Tmp = new float[4];\n"
						+ "PM_gOutputDestVarReduce123.copyTo(PM_gOutputDestVarReduce123Tmp);\n"
						+ "return new Pixel(PM_gOutputDestVarReduce123Tmp[0],PM_gOutputDestVarReduce123Tmp[1],PM_gOutputDestVarReduce123Tmp[2],PM_gOutputDestVarReduce123Tmp[3],-1,-1);");
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
				"static <mapType> map123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1.s2*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "<mapType> __attribute__((kernel)) map123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn map123_func(param1, x, y);\n" + "}\n");
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
						+ "static <mapType> map123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1.s2*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "<mapType> __attribute__((kernel)) map123(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn map123_func(param1, x, y);\n" + "}\n");
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
						+ "static <mapType> map123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1.s2*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputMap123); ++PM_y) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x, PM_y)), PM_x, PM_y);\n"
						+ "\t}\n"
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
						+ "static <mapType> map123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1.s2*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputMap123); ++PM_y) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x, PM_y)), PM_x, PM_y);\n"
						+ "\t}\n" + "\t}\n" + "}\n");
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
						+ "static <mapType> map123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1.s2*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputMap123); ++PM_y) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x, PM_y)), PM_x, PM_y);\n"
						+ "\t}\n"
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
						+ "static <mapType> map123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\t<mapType> ret;\n"
						+ "\tret = param1.s2*1.5f;\n"
						+ "\treturn ret;\n"
						+ "}\n"
						+ "void map123() {\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputMap123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputMap123); ++PM_y) {\n"
						+ "\t\trsSetElementAt_<mapType>(PM_gOutputMap123, map123_func(rsGetElementAt_<type>(PM_gInputMap123, PM_x, PM_y)), PM_x, PM_y);\n"
						+ "\t}\n"
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
						+ ".setX(<varIn>.getType().getX() * <varIn>.getType().getY())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "<kernel>.forEach_map123(<varIn>, <varOut>);");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
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
						+ ".setX(<varIn>.getType().getX() * <varIn>.getType().getY())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "<kernel>.set_PM_g<UCFinalVarName>Map123(<finalVarName>);"
						+ "<kernel>.forEach_map123(<varIn>, <varOut>);");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("finalVarName", finalVar.name);
		st.add("UCFinalVarName", upperCaseFirstLetter(finalVar.name));
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createMapOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))"
						+ ".setX(<varIn>.getType().getX() * <varIn>.getType().getY())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "<kernel>.set_PM_gInputMap123(<varIn>);"
						+ "<kernel>.set_PM_gOutputMap123(<varOut>);"
						+ "<kernel>.invoke_map123();");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
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
						+ ".setX(<varIn>.getType().getX() * <varIn>.getType().getY())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Map123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Map123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Map123(PM_gOutput<UCNonFinalVarName>Map123);\n"
						+ "<kernel>.set_PM_gInputMap123(<varIn>);"
						+ "<kernel>.set_PM_gOutputMap123(<varOut>);"
						+ "<kernel>.set_PM_g<UCFinalVarName>Map123(<finalVarName>);\n"
						+ "<kernel>.invoke_map123();"
						+ "PM_gOutput<UCNonFinalVarName>Map123.copyTo(<nonFinalVarName>);\n");
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
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type <varOut>Type = new Type.Builder(PM_mRS, Element.<rsType>(PM_mRS))"
						+ ".setX(<varIn>.getType().getX() * <varIn>.getType().getY())"
						+ ".create();"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varOut>Type);"
						+ "Allocation PM_gOutput<UCNonFinalVarName>Map123 = Allocation.createSized(PM_mRS, Element.I32(PM_mRS), 1);\n"
						+ "<kernel>.set_PM_g<UCNonFinalVarName>Map123(<nonFinalVarName>[0]);\n"
						+ "<kernel>.set_PM_gOutput<UCNonFinalVarName>Map123(PM_gOutput<UCNonFinalVarName>Map123);\n"
						+ "<kernel>.set_PM_gInputMap123(<varIn>);"
						+ "<kernel>.set_PM_gOutputMap123(<varOut>);"
						+ "<kernel>.invoke_map123();"
						+ "PM_gOutput<UCNonFinalVarName>Map123.copyTo(<nonFinalVarName>);\n");
		st.add("rsType", getMapRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("UCNonFinalVarName", upperCaseFirstLetter(nonFinalVar.name));
		st.add("nonFinalVarName", nonFinalVar.name);
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
						+ "static bool filter123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn param1.s2 > 2;\n"
						+ "}\n"
						+ "<type> __attribute__((kernel)) filter123_tile(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\tif (filter123_func(param1, x, y)) {\n"
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
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gOutputTileFilter123); ++PM_y) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x, PM_y);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" 
						+ "\t}\n" 
						+ "\t}\n" 
						+ "}");
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
						+ "static bool filter123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn param1.s2 > 2;\n"
						+ "}\n"
						+ "<type> __attribute__((kernel)) filter123_tile(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\tif (filter123_func(param1, x, y)) {\n"
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
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gOutputTileFilter123); ++PM_y) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x, PM_y);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" 
						+ "\t}\n" 
						+ "\t}\n" 
						+ "}");
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
						+ "static bool filter123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn param1.s2 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputFilter123); ++PM_y) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x, PM_y))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "\t}\n"
						+ "\trsSetElementAt_int(PM_gOutput<UCNonFinalVarName>Filter123, PM_g<UCNonFinalVarName>Filter123, 0);\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gOutputTileFilter123); ++PM_y) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x, PM_y);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" 
						+ "\t}\n" 
						+ "\t}\n" 
						+ "}");
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
						+ "static bool filter123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn param1.s2 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputFilter123); ++PM_y) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x, PM_y))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "\t}\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gOutputTileFilter123); ++PM_y) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x, PM_y);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" 
						+ "\t}\n" 
						+ "\t}\n" 
						+ "}");
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
						+ "static bool filter123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn param1.s2 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputFilter123); ++PM_y) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x, PM_y))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "\t}\n"
						+ "\trsSetElementAt_int(PM_gOutput<UCNonFinalVarName>Filter123, PM_g<UCNonFinalVarName>Filter123, 0);\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gOutputTileFilter123); ++PM_y) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x, PM_y);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" 
						+ "\t}\n" 
						+ "\t}\n" 
						+ "}");
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
						+ "static bool filter123_func(<type> param1, uint32_t x, uint32_t y) {\n"
						+ "\treturn param1.s2 > 2;\n"
						+ "}\n"
						+ "void filter123_tile() {\n"
						+ "\tint PM_tileX = 0;"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gInputFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gInputFilter123); ++PM_y) {\n"
						+ "\t\t\tif (filter123_func(rsGetElementAt_<type>(PM_gInputFilter123, PM_x, PM_y))) {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, PM_tileX, PM_tileX);\n"
						+ "\t\t\t\tPM_gOutputXSizeFilter123++;\n"
						+ "\t\t\t} else {\n"
						+ "\t\t\t\trsSetElementAt_int(PM_gOutputTileFilter123, -1, PM_tileX);\n"
						+ "\t\t\t}\n"
						+ "\t\t\tPM_tileX++;"
						+ "\t}\n"
						+ "\t}\n"
						+ "\trsSetElementAt_int(PM_gOutput<UCNonFinalVarName>Filter123, PM_g<UCNonFinalVarName>Filter123, 0);\n"
						+ "}\n"
						+ "void filter123_setAllocationSize() {\n"
						+ "\trsSetElementAt_int(PM_gOutputXSizeFilter123_Allocation, PM_gOutputXSizeFilter123, 0);\n"
						+ "}\n"
						+ "void filter123() {\n"
						+ "\tint PM_count = 0;\n"
						+ "\tfor (int PM_x=0; PM_x\\<rsAllocationGetDimX(PM_gOutputTileFilter123); ++PM_x) {\n"
						+ "\tfor (int PM_y=0; PM_y\\<rsAllocationGetDimY(PM_gOutputTileFilter123); ++PM_y) {\n"
						+ "\t\tint PM_value = rsGetElementAt_int(PM_gOutputTileFilter123, PM_x, PM_y);\n"
						+ "\t\tif (PM_value > 0) {\n"
						+ "\t\t\trsSetElementAt_<type>(PM_gOutputFilter123, rsGetElementAt_<type>(PM_gInputFilter123, PM_value), PM_count++);\n"
						+ "\t\t}\n" 
						+ "\t}\n" 
						+ "\t}\n" 
						+ "}");
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
						+ "\t.setX(<varIn>.getType().getX()*<varIn>.getType().getY())\n"
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
						+ "\t<kernel>.set_PM_gOutputFilter123(<varOut>);\n"
						+ "\t<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "\t<kernel>.invoke_filter123();\n" + "}");
		st.add("rsType", getRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
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
						+ "\t.setX(<varIn>.getType().getX()*<varIn>.getType().getY())\n"
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
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createFilterOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputTileFilter123Type = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(<varIn>.getType().getX()*<varIn>.getType().getY())\n"
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
						+ "\t<kernel>.set_PM_gOutputFilter123(<varOut>);\n"
						+ "\t<kernel>.set_PM_gInputFilter123(<varIn>);\n"
						+ "\t<kernel>.invoke_filter123();\n" + "}");
		st.add("rsType", getRSType());
		st.add("varIn",
				commonDefinitions.getVariableOutName(operation.variable));
		st.add("varOut", commonDefinitions
				.getVariableOutName(operation.destinationVariable));
		st.add("kernel", commonDefinitions.getKernelName(className));
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
						+ "\t\t.setX(<varIn>.getType().getX()*<varIn>.getType().getY())\n"
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
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createFilterOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST(
				"Type PM_gOutputTileFilter123Type = new Type.Builder(PM_mRS, Element.I32(PM_mRS))\n"
						+ "\t\t.setX(<varIn>.getType().getX()*<varIn>.getType().getY())\n"
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
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests method call translation.
	 */
	@Test
	public void translateMethodCall() throws Exception {
		// getHeight method
		MethodCall methodCall = this.createMethodCall(BitmapImage.getInstance()
				.getHeightMethodName());
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateMethodCall(className,
				methodCall);
		ST st = new ST("return <varIn>.getType().get<XY>();");
		st.add("varIn",
				commonDefinitions.getVariableInName(methodCall.variable));
		st.add("XY", "Y");
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		methodCall = this.createMethodCall(BitmapImage.getInstance()
				.getWidthMethodName());
		translatedFunction = translator.translateMethodCall(className,
				methodCall);
		st.remove("XY");
		st.add("XY", "X");
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests output bind call.
	 */
	@Test
	public void translateOutputBindCall() throws Exception {
		// imageVar.toBitmap(bitmapVar);
		OutputBind outputBind = this.createOutputBind(OutputBindType.None);
		String kernelName = this.commonDefinitions.getKernelName(className);
		String varInName = this.commonDefinitions
				.getVariableInName(outputBind.variable);
		String varOutName = this.commonDefinitions
				.getVariableOutName(outputBind.variable);
		ST st = new ST(
				"<kernel>.forEach_toBitmap<className>(<varOut>,<varIn>);\n"
						+ "<varIn>.copyTo(<varDest>);");
		st.add("kernel", kernelName);
		st.add("varIn", varInName);
		st.add("varOut", varOutName);
		st.add("varDest", outputBind.destinationObject);
		st.add("className", getClassName());
		String expectedTranslation = st.render();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// bitmapVar = imageVar.toBitmap();
		outputBind = this.createOutputBind(OutputBindType.Assignment);
		st = new ST(
				"if (<varIn> != null) {\n"
						+ "Bitmap <varDest> = Bitmap.createBitmap(<varIn>.getType().getX(), <varIn>.getType().getY(), Bitmap.Config.ARGB_8888);\n"
						+ "<kernel>.forEach_toBitmap<className>(<varOut>,<varIn>);\n"
						+ "<varIn>.copyTo(<varDest>);" + "return <varDest>;\n"
						+ "} else {\n" + "return null;\n" + "}");
		st.add("kernel", kernelName);
		st.add("varIn", varInName);
		st.add("varOut", varOutName);
		st.add("varDest", outputBind.destinationObject);
		st.add("className", getClassName());
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
}
