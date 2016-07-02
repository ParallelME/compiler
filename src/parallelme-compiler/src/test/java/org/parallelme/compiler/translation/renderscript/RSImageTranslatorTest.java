/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.translation.ImageTranslatorTest;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for RenderScript image tests.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSImageTranslatorTest extends ImageTranslatorTest {
	/**
	 * Tests input bind object declaration.
	 */
	@Test
	public void translateInputBindObjDeclaration() throws Exception {
		BaseUserLibraryTranslator translator = this.getTranslator();
		assertEquals(translator.translateInputBindObjDeclaration(null), "");
	}

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
		String expectedTranslation = "float4 __attribute__ ((kernel)) foreach123(float4 param1, uint32_t x, uint32_t y) {\n"
				+ "param1.s0=123;\n" + "return param1;\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "int PM_gExternalVarForeach123;\n"
				+ "float4 __attribute__ ((kernel)) foreach123(float4 param1, uint32_t x, uint32_t y) {\n"
				+ "param1.s0=123;\n" + "return param1;\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputForeach123;\n"
				+ "rs_allocation PM_gOutputExternalVarForeach123;\n"
				+ "int PM_gInputXSizeForeach123;\n"
				+ "int PM_gInputYSizeForeach123;\n"
				+ "int PM_gExternalVarForeach123;\n"
				+ "void foreach123() {\n"
				+ "float4 param1;\n"
				+ "for (int PM_x=0; PM_x<PM_gInputXSizeForeach123; PM_x++) {\n"
				+ "for (int PM_y=0; PM_y<PM_gInputYSizeForeach123; PM_y++) {\n"
				+ "param1 = rsGetElementAt_float4(PM_gInputForeach123, PM_x, PM_y);\n"
				+ "param1.s0 = 123;\n"
				+ "rsSetElementAt_float4(PM_gInputForeach123, param1, PM_x, PM_y);\n"
				+ "}\n"
				+ "}\n"
				+ "rsSetElementAt_int(PM_gOutputExternalVarForeach123, PM_gExternalVarForeach123, 0);\n"
				+ "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputForeach123;\n"
				+ "int PM_gInputXSizeForeach123;\n"
				+ "int PM_gInputYSizeForeach123;\n"
				+ "void foreach123(){\n"
				+ "float4 param1;\n"
				+ "for (int PM_x=0; PM_x<PM_gInputXSizeForeach123; PM_x++) {\n"
				+ "for (int PM_y=0; PM_y<PM_gInputYSizeForeach123; PM_y++) {\n"
				+ "param1 = rsGetElementAt_float4(PM_gInputForeach123, PM_x, PM_y);\n"
				+ "param1.s0 = 123;\n"
				+ "rsSetElementAt_float4(PM_gInputForeach123, param1, PM_x, PM_y);\n"
				+ "}\n" + "}\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputForeach123;\n"
				+ "int PM_gInputXSizeForeach123;\n"
				+ "int PM_gInputYSizeForeach123;\n"
				+ "int PM_gExternalVarForeach123;\n"
				+ "void foreach123() {\n"
				+ "float4 param1;\n"
				+ "for (int PM_x=0; PM_x<PM_gInputXSizeForeach123; PM_x++) {\n"
				+ "for (int PM_y=0; PM_y<PM_gInputYSizeForeach123; PM_y++) {\n"
				+ "param1 = rsGetElementAt_float4(PM_gInputForeach123, PM_x, PM_y);\n"
				+ "param1.s0 = 123;\n"
				+ "rsSetElementAt_float4(PM_gInputForeach123, param1, PM_x, PM_y);\n"
				+ "}\n" + "}\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputForeach123;\n"
				+ "rs_allocation PM_gOutputExternalVarForeach123;\n"
				+ "int PM_gInputXSizeForeach123;\n"
				+ "int PM_gInputYSizeForeach123;\n"
				+ "int PM_gExternalVarForeach123;\n"
				+ "void foreach123() {\n"
				+ "float4 param1;\n"
				+ "for (int PM_x=0; PM_x<PM_gInputXSizeForeach123; PM_x++) {\n"
				+ "for (int PM_y=0; PM_y<PM_gInputYSizeForeach123; PM_y++) {\n"
				+ "param1 = rsGetElementAt_float4(PM_gInputForeach123, PM_x, PM_y);\n"
				+ "param1.s0 = 123;\n"
				+ "rsSetElementAt_float4(PM_gInputForeach123, param1, PM_x, PM_y);\n"
				+ "}\n"
				+ "}\n"
				+ "rsSetElementAt_int(PM_gOutputExternalVarForeach123, PM_gExternalVarForeach123,0);\n"
				+ "}";
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
		String kernelName = this.commonDefinitions.getKernelName(className);
		String varOutName = this.commonDefinitions
				.getVariableOutName(operation.variable);
		ST st = new ST("<kernel>.forEach_foreach123(<varOut>,<varOut>);");
		st.add("kernel", kernelName);
		st.add("varOut", varOutName);
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		st = new ST(
				"<kernel>.set_PM_gInputForeach123(<varOut>);\n"
						+ "<kernel>.set_PM_gInputXSizeForeach123(<varOut>.getType().getX());\n"
						+ "<kernel>.set_PM_gInputYSizeForeach123(<varOut>.getType().getY());\n"
						+ "<kernel>.invoke_foreach123(<varOut>,<varOut>);");
		operation = this.createForeachOperation(ExecutionType.Sequential);
		st.add("kernel", kernelName);
		st.add("varOut", varOutName);
		expectedTranslation = st.render();
		translatedFunction = translator.translateOperationCall(className,
				operation);
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
				+ "int PM_gInputXSizeReduce123;\n"
				+ "int PM_gInputYSizeReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123;\n"
				+ "param2.s1 = 456;\n"
				+ "return param2;\n"
				+ "}\n"
				+ "float4 __attribute__ ((kernel)) reduce123_tile(uint32_t x) {\n"
				+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, x, 0);\n"
				+ "float4 param2;\n"
				+ "for (int i=1; i<PM_gInputYSizeReduce123; ++i) {\n"
				+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, x, i);\n"
				+ "param1 = reduce123_func(param1, param2);\n"
				+ "}\n"
				+ "return param1;\n"
				+ "}\n"
				+ "float4 __attribute__ ((kernel)) reduce123(uint32_t x) {\n"
				+ "float4 param1 = rsGetElementAt_float4(PM_gTileReduce123, 0);\n"
				+ "float4 param2;\n"
				+ "for (int i=1; i<PM_gInputXSizeReduce123; ++i) {\n"
				+ "param2 = rsGetElementAt_float4(PM_gTileReduce123, i);\n"
				+ "param1 = reduce123_func(param1, param2);\n" + "}\n"
				+ "return param1;\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputReduce123;\n"
				+ "rs_allocation PM_gTileReduce123;\n"
				+ "int PM_gInputXSizeReduce123;\n"
				+ "int PM_gInputYSizeReduce123;\n"
				+ "int PM_gExternalVarReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123;\n"
				+ "param2.s1 = 456;\n"
				+ "return param2;\n"
				+ "}\n"
				+ "float4 __attribute__ ((kernel)) reduce123_tile(uint32_t x) {\n"
				+ "float4 param1 = rsGetElementAt_float4(PM_gInputReduce123, x, 0);\n"
				+ "float4 param2;\n"
				+ "for (int i=1; i<PM_gInputYSizeReduce123; ++i){\n"
				+ "param2 = rsGetElementAt_float4(PM_gInputReduce123, x, i);\n"
				+ "param1 = reduce123_func(param1, param2);\n"
				+ "}\n"
				+ "return param1;\n"
				+ "}\n"
				+ "float4 __attribute__ ((kernel)) reduce123(uint32_t x) {\n"
				+ "float4 param1 = rsGetElementAt_float4(PM_gTileReduce123, 0);\n"
				+ "float4 param2;\n"
				+ "for (int i=1; i<PM_gInputXSizeReduce123; ++i) {\n"
				+ "param2 = rsGetElementAt_float4(PM_gTileReduce123, i);\n"
				+ "param1 = reduce123_func(param1, param2);\n" + "}\n"
				+ "return param1;\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputReduce123;\n"
				+ "rs_allocation PM_gTileReduce123;\n"
				+ "rs_allocation PM_gOutputExternalVarReduce123;\n"
				+ "int PM_gInputXSizeReduce123;\n"
				+ "int PM_gInputYSizeReduce123;\n"
				+ "int PM_gExternalVarReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123;\n" + "param2.s1 = 456;\n"
				+ "return param2;\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputReduce123;\n"
				+ "rs_allocation PM_gTileReduce123;\n"
				+ "int PM_gInputXSizeReduce123;\n"
				+ "int PM_gInputYSizeReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123;\n" + "param2.s1 = 456;\n"
				+ "return param2;\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputReduce123;\n"
				+ "rs_allocation PM_gTileReduce123;\n"
				+ "int PM_gInputXSizeReduce123;\n"
				+ "int PM_gInputYSizeReduce123;\n"
				+ "int PM_gExternalVarReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123;\n" + "param2.s1 = 456;\n"
				+ "return param2;\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "rs_allocation PM_gInputReduce123;\n"
				+ "rs_allocation PM_gTileReduce123;\n"
				+ "rs_allocation PM_gOutputExternalVarReduce123;\n"
				+ "int PM_gInputXSizeReduce123;\n"
				+ "int PM_gInputYSizeReduce123;\n"
				+ "int PM_gExternalVarReduce123;\n"
				+ "static float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123;\n" + "param2.s1 = 456;\n"
				+ "return param2;\n" + "}";
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
		ST st = new ST("Type PM_gTileReduce123Type = newType.Builder(PM_mRS, Element.F32_3(PM_mRS))"
				+ ".setX(<varOut>.getType().getX())"
				+ ".create();\n"
				+ "AllocationPM_gTileReduce123 = Allocation.createTyped(PM_mRS, PM_gTileReduce123Type);\n"
				+ "Type PM_destVarType = new Type.Builder(PM_mRS, Element.F32_3(PM_mRS)).setX(1).create();\n"
				+ "Allocation PM_destVar = Allocation.createTyped(PM_mRS, PM_destVarType);\n"
				+ "<kernel>.set_PM_gInputReduce123(<varOut>);\n"
				+ "<kernel>.set_PM_gTileReduce123(PM_gTileReduce123);\n"
				+ "<kernel>.set_PM_gInputXSizeReduce123(<varOut>.getType().getX());\n"
				+ "<kernel>.set_PM_gInputYSizeReduce123(<varOut>.getType().getY());\n"
				+ "<kernel>.forEach_reduce123_tile(PM_gTileReduce123);\n"
				+ "<kernel>.forEach_reduce123(PM_destVar);\n"
				+ "float[] PM_destVarTmp = new float[4];\n"
				+ "PM_destVar.copyTo(PM_destVarTmp);\n"
				+ "return new Pixel(PM_destVarTmp[0], PM_destVarTmp[1], PM_destVarTmp[2], PM_destVarTmp[3], -1, -1);");
		st.add("varOut", commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		st = new ST("Type PM_gTileReduce123Type = newType.Builder(PM_mRS,Element.F32_3(PM_mRS))"
				+ ".setX(<varOut>.getType().getX()).create();\n"
				+ "Allocation PM_gTileReduce123 = Allocation.createTyped(PM_mRS,PM_gTileReduce123Type);\n"
				+ "Type PM_destVarType = new Type.Builder(PM_mRS,Element.F32_3(PM_mRS)).setX(1).create();\n"
				+ "Allocation PM_destVar = Allocation.createTyped(PM_mRS, PM_destVarType);\n"
				+ "<kernel>.set_PM_gInputReduce123(<varOut>);\n"
				+ "<kernel>.set_PM_gTileReduce123(PM_gTileReduce123);\n"
				+ "<kernel>.set_PM_gInputReduce123(<varOut>);\n"
				+ "<kernel>.set_PM_gInputXSizeReduce123(<varOut>.getType().getX());\n"
				+ "<kernel>.set_PM_gInputYSizeReduce123(<varOut>.getType().getY());\n"
				+ "<kernel>.set_PM_gInputXSizeReduce123(<varOut>.getType().getX());\n"
				+ "<kernel>.set_PM_gInputYSizeReduce123(<varOut>.getType().getY());\n"
				+ "<kernel>.invoke_reduce123_tile(PM_gTileReduce123);\n"
				+ "<kernel>.invoke_reduce123(PM_destVar);\n"
				+ "float[] PM_destVarTmp = new float[4];\n"
				+ "PM_destVar.copyTo(PM_destVarTmp);\n"
				+ "return new Pixel(PM_destVarTmp[0], PM_destVarTmp[1], PM_destVarTmp[2], PM_destVarTmp[3], -1, -1);");
		st.add("varOut", commonDefinitions.getVariableOutName(operation.variable));
		st.add("kernel", commonDefinitions.getKernelName(className));
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
}
