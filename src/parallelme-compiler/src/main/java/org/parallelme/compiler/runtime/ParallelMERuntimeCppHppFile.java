/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.stringtemplate.v4.ST;
import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;

/**
 * Stores all actions and definitions used to create cpp and hpp files'
 * contents.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeCppHppFile extends ParallelMERuntimeCFileBaseImpl {
	private RuntimeCommonDefinitions commonDefinitions = new RuntimeCommonDefinitions();
	private static final String templateJNICppFile = "<introductoryMsg>\n<includes:{var|#include <var.value>\n}>"
			+ "\nJavaVM *gJvm;\n\n"
			+ "JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {\n"
			+ "\tgJvm = jvm;\n"
			+ "\treturn JNI_VERSION_1_6;\n"
			+ "}\n\n"
			+ "<functions:{var|<var.decl><var.LDLIM>\n<var.body>\n<var.RDLIM>\n\n}>";
	private static final String templateFunctionDeclJNI = "JNIEXPORT <return> JNICALL Java_<className>_<functionName>(JNIEnv *env, jobject a<params:{var|, <var.type> <var.name>}>)";
	private static final String templateFunctionBodyJNI = "\ttry {\n"
			+ "\t\tauto $foo = reinterpret_cast\\<ParallelMERuntime *>($parallelMERuntime);\n"
			+ "\t\tstd::list\\<ExtraArgumentType> $extra_args;\n"
			+ "\t\t<paramsEA:{var|ExtraArgument $ea_<var.name>;\n"
			+ "$ea_<var.name>.argType = ArgType::<var.argType>;\n"
			+ "$ea_<var.name>.value.<var.argAlias> = $<var.name>;\n"
			+ "$extra_args.push_back($ea_<var.name>);\n}>"
			+ "\t\tstd::list\\<int> $input_args;\n"
			+ "\t\t$input_args.push_back($inputBufferId);\n"
			+ "\t\tstd::list\\<int> $output_args;\n"
			+ "\t\t$output_args.push_back($outputBufferId);\n"
			+ "\t\t<paramsOA:{var|$output_args.push_back(<var.name>);}>\n"
			+ "\t\t$foo->addKernel($input_args, $output_args, \"<functionName>\", $extra_args, $worksize);\n"
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
			String typeName = iterator.variable.typeName;
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
			variableTypes.add(iterator.variable.typeName);
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
			String typeName = iterator.variable.typeName;
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
		st.addAggr("params.{type, name}", "jlong",
				this.commonDefinitions.getPrefix() + "parallelMERuntime");
		st.addAggr("params.{type, name}", "jint",
				this.commonDefinitions.getPrefix() + "inputBufferId");
		st.addAggr("params.{type, name}", "jint",
				this.commonDefinitions.getPrefix() + "outputBufferId");
		st.addAggr("params.{type, name}", "jint",
				this.commonDefinitions.getPrefix() + "worksize");
		// Create output buffers for each external variable
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}", "jint",
					this.commonDefinitions.getVariableOutName(variable));
		}
		if (iterator.getType() == IteratorType.Sequential) {
			if (iterator.variable.typeName.equals(BitmapImage.getName())
					|| iterator.variable.typeName.equals(HDRImage.getName())) {
				st.addAggr("params.{type, name}", "jint",
						this.commonDefinitions.getPrefix() + "height");
				st.addAggr("params.{type, name}", "jint",
						this.commonDefinitions.getPrefix() + "width");
			}
		}
		// External variables are added with its original name
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}",
					PrimitiveTypes.getJNIType(variable.typeName), variable.name);
		}
		return st.render();
	}

	/**
	 * Creates a JNI function body for a given iterator.
	 */
	private String getJNIFunctionBody(Iterator iterator) {
		ST st = new ST(templateFunctionBodyJNI);
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.add("paramsEA", null);
		st.add("returnStatement", null);
		st.add("paramsOA", null);
		if (iterator.getType() == IteratorType.Sequential) {
			if (iterator.variable.typeName.equals(BitmapImage.getName())
					|| iterator.variable.typeName.equals(HDRImage.getName())) {
				st.addAggr("paramsEA.{name, argType, argAlias}", "height",
						PrimitiveTypes.getRuntimeArgType("int"),
						PrimitiveTypes.getRuntimeAlias("int"));
				st.addAggr("paramsEA.{name, argType, argAlias}", "width",
						PrimitiveTypes.getRuntimeArgType("int"),
						PrimitiveTypes.getRuntimeAlias("int"));
			}
		}
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("paramsOA.{name}",
					this.commonDefinitions.getVariableOutName(variable));
			st.addAggr("paramsEA.{name, argType, argAlias}", variable.name,
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
			st.add("return", "void");
			st.add("className", cClassName);
			st.add("functionName", "toBitmap");
			st.addAggr("params.{type, name}", "jlong",
					this.commonDefinitions.getPrefix() + "parallelMERuntime");
			st.addAggr("params.{type, name}", "jint",
					this.commonDefinitions.getPrefix() + "inputBufferId");
			st.addAggr("params.{type, name}", "jint",
					this.commonDefinitions.getPrefix() + "outputBufferId");
			st.addAggr("params.{type, name}", "jint",
					this.commonDefinitions.getPrefix() + "worksize");
			ret.put("toBitmap", st.render());
			// toFloat. Duplicated code because string template will always add
			// strings whenever you try to replace a previously filled
			// parameter, so to simply add a new "functionName" we must
			// re-create the ST object.
			st = new ST(templateFunctionDeclJNI);
			st.add("return", "void");
			st.add("className", cClassName);
			st.add("functionName", "toBitmap");
			st.addAggr("params.{type, name}", "jlong",
					this.commonDefinitions.getPrefix() + "parallelMERuntime");
			st.addAggr("params.{type, name}", "jint",
					this.commonDefinitions.getPrefix() + "inputBufferId");
			st.addAggr("params.{type, name}", "jint",
					this.commonDefinitions.getPrefix() + "outputBufferId");
			st.addAggr("params.{type, name}", "jint",
					this.commonDefinitions.getPrefix() + "worksize");
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
			st.add("paramsOA", null);
			st.add("functionName", "toBitmap");
			st.add("paramsEA", null);
			st.add("returnStatement", "\n\treturn bitmap;");
			ret.put("toBitmap", st.render());
			// toFloat
			st = new ST(templateFunctionBodyJNI);
			st.add("paramsOA", null);
			st.add("functionName", "toFloat");
			st.add("paramsEA", null);
			st.add("returnStatement", null);
			ret.put("toFloat", st.render());
		}
		return ret;
	}
}
