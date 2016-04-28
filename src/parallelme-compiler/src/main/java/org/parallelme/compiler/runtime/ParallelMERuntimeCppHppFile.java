/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.stringtemplate.v4.ST;

import org.parallelme.compiler.runtime.translation.PrimitiveTypes;
import org.parallelme.compiler.runtime.translation.data.InputBind;
import org.parallelme.compiler.runtime.translation.data.Iterator;
import org.parallelme.compiler.runtime.translation.data.OutputBind;
import org.parallelme.compiler.runtime.translation.data.Variable;
import org.parallelme.compiler.runtime.translation.data.Iterator.IteratorType;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.parallelme.compiler.util.Pair;

/**
 * Stores all actions and definitions used to create cpp and hpp files'
 * contents.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeCppHppFile extends ParallelMERuntimeCFileBaseImpl {
	private CommonDefinitions commonDefinitions = new CommonDefinitions();
	private static final String templateJNICppFile = "<introductoryMsg>\n<includes:{var|#include <var.value>\n}>"
			+ "\nJavaVM *gJvm;\n\n"
			+ "JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {\n"
			+ "\tgJvm = jvm;\n"
			+ "\treturn JNI_VERSION_1_6;\n"
			+ "}\n\n"
			+ "<functions:{var|<var.decl><var.LDLIM>\n<var.body>\n<var.RDLIM>\n\n}>";
	private static final String templateFunctionDeclJNI = "JNIEXPORT <return> JNICALL Java_<className>_<functionName>(JNIEnv *env, jobject a<params:{var|, <var.type> <var.name>}>)";
	private static final String templateFunctionBodyJNI = "\ttry {\n"
			+ "\t\tauto foo = reinterpret_cast<castContents>(parallelMERuntime);\n"
			+ "\t\tstd::list<ExtraArgumentType> extra_args;\n"
			+ "\t\t<params:{var|ExtraArgument ea_<var.name>;\n"
			+ "ea_<var.name>.argType = ArgType::<var.argType>;\n"
			+ "ea_<var.name>.value.<var.argAlias> = <var.name>;\n"
			+ "extra_args.push_back(ea_<var.name>);\n}>"
			+ "\t\tfoo->addKernel(input_array_id, \"<functionName>\", extra_args, worksize<kernelParams:{var|, <var.name>}>);\n"
			+ "\t} catch(std::runtime_error &e) {\n"
			+ "\t\tstop_if(true, \"Error on call to ParallelME kernel at <functionName>: %s\", e.what());\n"
			+ "\t}" + "<returnStatement>";
	private static final String templateJNIHFile = "<introductoryMsg>\n<includes:{var|#include <var.value>\n}>"
			+ "\n#ifndef _Included_<className>\n"
			+ "#define _Included_<className>\n"
			+ "#ifdef __cplusplus\n"
			+ "extern \"C\" {\n"
			+ "#endif\n\n"
			+ "JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *);\n\n"
			+ "<functions:{var|<var.decl>;\n\n}>"
			+ "#ifdef __cplusplus\n"
			+ "}\n" + "#endif\n" + "#endif\n";
	private static final String templateKernelDecl = "__kernel void <functionName>(<params:{var|<var.type> <var.name>}>)";
	private static final String toFloatKernel = "__kernel void toFloat(__global uchar4 *gIn, __global float4 *gOut) {\n"
			+ "\tint gid = get_global_id(0);\n"
			+ "\tuchar4 in = gIn[gid];\n"
			+ "\tfloat4 out;\n"
			+ "\tfloat f;\n"
			+ "\tif(in.s3 != 0) {\n"
			+ "\t\tf = ldexp(1.0f, (in.s3 & 0xFF) - (128 + 8));\n"
			+ "\t\tout.s0 = (in.s0 & 0xFF) * f;\n"
			+ "\t\tout.s1 = (in.s1 & 0xFF) * f;\n"
			+ "\t\tout.s2 = (in.s2 & 0xFF) * f;\n"
			+ "\t} else {\n"
			+ "\t\tout.s0 = 0.0f;\n"
			+ "\t\tout.s1 = 0.0f;\n"
			+ "\t\tout.s2 = 0.0f;\n" + "\t}\n" + "\tgOut[gid] = out;\n" + "}\n";
	private static final String toBitmapKernel = "__kernel void toBitmap(__global float4 *gIn, __global uchar4 *gOut) {\n"
			+ "\tint gid = get_global_id(0);\n"
			+ "\tfloat4 in = gIn[gid];\n"
			+ "\tuchar4 out;\n"
			+ "\tout.x = (uchar) (255.0f * in.s0);\n"
			+ "\tout.y = (uchar) (255.0f * in.s1);\n"
			+ "\tout.z = (uchar) (255.0f * in.s2);\n"
			+ "\tout.w = 255;\n"
			+ "\tgOut[gid] = out;\n" + "}\n";

	/**
	 * Creates a string with a C class that wraps all JNI calls
	 * 
	 * @param packageName
	 *            Name of the package of which the returned class will be part.
	 * @param className
	 *            Name of the JNI wrapper class.
	 */
	public String getCppJNIWrapperClass(String packageName, String className,
			List<Iterator> iterators) {
		String cClassName = this.commonDefinitions.getCClassName(packageName,
				className);
		ST st = new ST(templateJNICppFile);
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		// 1. Add all libraries
		st.addAggr("includes.{value}", "\"" + cClassName + ".hpp\"");
		st.addAggr("includes.{value}", "\"error.h\"");
		st.addAggr("includes.{value}", "<stdexcept>");
		// Set to null to avoid errors in case of non-existing iterators
		st.add("functions", null);
		HashSet<String> variableTypes = new HashSet<>();
		// 2. Add functions
		// Stores variable types to include proper methods to handle each
		// user library class
		Set<String> variablesType = new HashSet<>();
		for (Iterator iterator : iterators) {
			String typeName = iterator.getVariable().typeName;
			if (!variablesType.contains(typeName)) {
				variablesType.add(typeName);
				Map<String, String> functionDeclByName = this
						.getJNIFunctionDeclByUserLibrary(packageName,
								className, typeName);
				Map<String, String> functionBodyByName = this
						.getJNIFunctionBodyByUserLibrary(typeName);
				for (String function : functionDeclByName.keySet()) {
					st.addAggr("functions.{decl, body, LDLIM, RDLIM}",
							functionDeclByName.get(function),
							functionBodyByName.get(function), " {", "}");
				}
			}
		}
		for (Iterator iterator : iterators) {
			String decl = this.getJNIFunctionDecl(packageName, className,
					iterator);
			String body = this.getJNIFunctionBody(iterator);
			st.addAggr("functions.{decl, body, LDLIM, RDLIM}", decl, body,
					" {", "}");
			variableTypes.add(iterator.getVariable().typeName);
		}
		// 3. Add imports
		for (String variableType : variableTypes) {
			String importValue = this.getImportLibrary(variableType);
			if (importValue != null)
				st.addAggr("includes.{value}", importValue);
		}
		return st.render();
	}

	/**
	 * Creates a string with a C class that wraps all JNI calls
	 * 
	 * @param packageName
	 *            Name of the package of which the returned class will be part.
	 * @param className
	 *            Name of the JNI wrapper class.
	 */
	public String getHppJNIWrapperClass(String packageName, String className,
			List<Iterator> iterators) {
		ST st = new ST(templateJNIHFile);
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		String cClassName = this.commonDefinitions.getCClassName(packageName,
				className);
		st.addAggr("includes.{value}", "<jni.h>");
		st.add("className", cClassName);
		// Stores variable types to include proper methods to handle each
		// user library class
		Set<String> variablesType = new HashSet<>();
		for (Iterator iterator : iterators) {
			String typeName = iterator.getVariable().typeName;
			if (!variablesType.contains(typeName)) {
				variablesType.add(typeName);
				for (String decl : this.getJNIFunctionDeclByUserLibrary(
						packageName, className, typeName).values()) {
					st.addAggr("functions.{decl}", decl);
				}
			}
		}
		for (Iterator iterator : iterators) {
			st.addAggr("functions.{decl}",
					this.getJNIFunctionDecl(packageName, className, iterator));
		}
		return st.render();
	}

	/**
	 * Creates a JNI function declaration for a given iterator.
	 */
	private String getJNIFunctionDecl(String packageName, String className,
			Iterator iterator) {
		String cClassName = this.commonDefinitions.getCClassName(packageName,
				className);
		ST st = new ST(templateFunctionDeclJNI);
		st.add("return", "void");
		st.add("className", cClassName);
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.addAggr("params.{type, name}", "jlong", "parallelMERuntime");
		st.addAggr("params.{type, name}", "jint", "input_array_id");
		st.addAggr("params.{type, name}", "jint", "worksize");
		List<Pair<String, String>> params = this
				.getJNIFunctionDeclParams(iterator);
		for (int i = 0; i < params.size(); i++) {
			Pair<String, String> pair = params.get(i);
			st.addAggr("params.{type, name}", pair.left, pair.right);
		}
		return st.render();
	}

	/**
	 * Creates a JNI function body for a given iterator.
	 */
	private String getJNIFunctionBody(Iterator iterator) {
		ST st = new ST(templateFunctionBodyJNI);
		st.add("ExtraArgumentType", "<ExtraArgument>");
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.add("castContents", "<ParallelMERuntime *>");
		st.add("params", null);
		st.add("kernelParams", null);
		st.add("returnStatement", null);
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{name, argType, argAlias}", variable.name,
					PrimitiveTypes.getRuntimeArgType(variable.typeName),
					PrimitiveTypes.getRuntimeAlias(variable.typeName));
		}
		return st.render();
	}

	/**
	 * Returns a map of functions' where key is the function name and the value
	 * is its declaration for a given user library type.
	 */
	private Map<String, String> getJNIFunctionDeclByUserLibrary(
			String packageName, String className, String userLibraryType) {
		Map<String, String> ret = new HashMap<>();
		if (userLibraryType.equals(BitmapImage.getName())
				|| userLibraryType.equals(HDRImage.getName())) {
			String cClassName = this.commonDefinitions.getCClassName(
					packageName, className);
			// toBitmap
			ST st = new ST(templateFunctionDeclJNI);
			st.add("return", "jobject");
			st.add("className", cClassName);
			st.add("functionName", "toBitmap");
			st.addAggr("params.{type, name}", "jlong", "parallelMERuntime");
			st.addAggr("params.{type, name}", "jint", "input_array_id");
			st.addAggr("params.{type, name}", "jint", "worksize");
			st.addAggr("params.{type, name}", "jbyteArray", "input_array");
			st.addAggr("params.{type, name}", "jint", "input_buffer_size");
			st.addAggr("params.{type, name}", "jobject", "bitmap");
			st.addAggr("params.{type, name}", "jint", "bitmap_buffer_size");
			ret.put("toBitmap", st.render());
			// toFloat
			st = new ST(templateFunctionDeclJNI);
			st.add("return", "void");
			st.add("className", cClassName);
			st.add("functionName", "toFloat");
			st.addAggr("params.{type, name}", "jlong", "parallelMERuntime");
			st.addAggr("params.{type, name}", "jint", "input_array_id");
			st.addAggr("params.{type, name}", "jint", "worksize");
			ret.put("toFloat", st.render());
		}
		return ret;
	}

	/**
	 * Returns a map of functions' where key is the function name and the value
	 * is its body for a given user library type.
	 */
	private Map<String, String> getJNIFunctionBodyByUserLibrary(
			String userLibraryType) {
		Map<String, String> ret = new HashMap<>();
		if (userLibraryType.equals(BitmapImage.getName())
				|| userLibraryType.equals(HDRImage.getName())) {
			// toBitmap
			ST st = new ST(templateFunctionBodyJNI);
			st.add("ExtraArgumentType", "<ExtraArgument>");
			st.add("functionName", "toBitmap");
			st.add("castContents", "<ParallelMERuntime *>");
			st.add("params", null);
			st.addAggr("kernelParams.{name}", "input_array");
			st.addAggr("kernelParams.{name}", "input_buffer_size");
			st.addAggr("kernelParams.{name}", "bitmap");
			st.addAggr("kernelParams.{name}", "bitmap_buffer_size");
			st.add("returnStatement", "\n\treturn bitmap;");
			ret.put("toBitmap", st.render());
			// toFloat
			st = new ST(templateFunctionBodyJNI);
			st.add("ExtraArgumentType", "<ExtraArgument>");
			st.add("functionName", "toFloat");
			st.add("castContents", "<ParallelMERuntime *>");
			st.add("params", null);
			st.add("kernelParams", null);
			st.add("returnStatement", null);
			ret.put("toFloat", st.render());
		}
		return ret;
	}

	/**
	 * Analyzes a given iterator object, takes its external objects and creates
	 * a string for each parameter declaration according to JNI parameter types.
	 */
	private List<Pair<String, String>> getJNIFunctionDeclParams(
			Iterator iterator) {
		ArrayList<Pair<String, String>> ret = new ArrayList<>();
		for (Variable variable : iterator.getExternalVariables()) {
			// TODO Check if variable type is primitive. If it is not, show some
			// error or throw an exception (check what is the best option).
			ret.add(new Pair<String, String>(PrimitiveTypes
					.getJNIType(variable.typeName), variable.name));
		}
		return ret;
	}

	/**
	 * Creates a string with the contents of the kernels file.
	 * 
	 * @param packageName
	 *            Name of the package of which the returned class will be part.
	 * @param className
	 *            Name of the JNI wrapper class.
	 */
	public String getHKernelFile(String packageName, String className,
			List<InputBind> inputBinds, List<Iterator> iterators,
			List<OutputBind> outputBinds) {
		String templateKernelFile = "<introductoryMsg>\n"
				+ "#ifndef KERNELS_H\n" + "#define KERNELS_H\n\n"
				+ "const char kernels[] =\n"
				+ "\t<kernels:{var|\"<var.line>\"\n}>" + "#endif\n";
		ST st = new ST(templateKernelFile);
		// 1. Add header comment
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		// 2. Translate input binds
		Set<String> inputBindTypes = new HashSet<String>();
		for (InputBind inputBind : inputBinds) {
			if (!inputBindTypes.contains(inputBind.getVariable().typeName)) {
				inputBindTypes.add(inputBind.getVariable().typeName);
				this.translateInputBind(inputBind, st);
				st.addAggr("kernels.{line}", "\\n");
			}
		}
		// 3. Translate iterators
		for (Iterator iterator : iterators) {
			this.translateIterator(iterator, st);
			st.addAggr("kernels.{line}", "\\n");
		}
		// 4. Translate outputbinds
		Set<String> outputBindTypes = new HashSet<String>();
		for (OutputBind outputBind : outputBinds) {
			if (!outputBindTypes.contains(outputBind.getVariable().typeName)) {
				outputBindTypes.add(outputBind.getVariable().typeName);
				this.translateOutputBind(outputBind, st);
				st.addAggr("kernels.{line}", "\\n");
			}
		}
		return st.render();
	}

	/**
	 * Translates a given input bind.
	 * 
	 * @param inputBind
	 *            Object containing the necessary information to translate an
	 *            input bind.
	 * @param st
	 *            String template that will be filled with input bind's kernel
	 *            information.
	 */
	private void translateInputBind(InputBind inputBind, ST st) {
		if (inputBind.getVariable().typeName.equals(BitmapImage.getName())
				|| inputBind.getVariable().typeName.equals(HDRImage.getName())) {
			this.addKernelByLine(toFloatKernel, st);
		}
	}

	/**
	 * Translates a given iterator.
	 * 
	 * @param iterator
	 *            Object containing the necessary information to translate an
	 *            iterator.
	 * @param st
	 *            String template that will be filled with iterator's kernel
	 *            information.
	 */
	private void translateIterator(Iterator iterator, ST st) {
		st.addAggr("kernels.{line}", this.getKernelFunctionDecl(iterator));
		this.addKernelByLine(iterator.getUserFunctionData().Code, st);
	}

	/**
	 * Translates a given output bind.
	 * 
	 * @param outputBind
	 *            Object containing the necessary information to translate an
	 *            output bind.
	 * @param st
	 *            String template that will be filled with output bind's kernel
	 *            information.
	 */
	private void translateOutputBind(OutputBind outputBind, ST st) {
		if (outputBind.getVariable().typeName.equals(BitmapImage.getName())
				|| outputBind.getVariable().typeName.equals(HDRImage.getName())) {
			this.addKernelByLine(toBitmapKernel, st);
		}
	}

	/**
	 * Add a given kernel line-by-line to the string template informed.
	 * 
	 * @param kernel
	 *            Multi-line kernel function.
	 * @param st
	 *            String template with "kernes.line" parameter.
	 */
	private void addKernelByLine(String kernel, ST st) {
		String[] lines = kernel.split("\n");
		for (String line : lines) {
			st.addAggr("kernels.{line}", line + "\\n");
		}
	}

	/**
	 * Creates kernel declaration for a given iterator.
	 */
	private String getKernelFunctionDecl(Iterator iterator) {
		ST st = new ST(templateKernelDecl);
		st.add("return", "void");
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.addAggr("params.{type, name}", "float4", "*gData");
		if (iterator.getType() == IteratorType.Sequential) {
			st.addAggr("params.{type, name}", ", int", "height");
			st.addAggr("params.{type, name}", ", int", "width");
		}
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}",
					", " + PrimitiveTypes.getCType(variable.typeName),
					variable.name);
		}
		return st.render();
	}
}
