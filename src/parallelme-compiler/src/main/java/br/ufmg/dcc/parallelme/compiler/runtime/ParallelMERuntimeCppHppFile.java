/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.stringtemplate.v4.ST;

import br.ufmg.dcc.parallelme.compiler.runtime.translation.PrimitiveTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Iterator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Variable;
import br.ufmg.dcc.parallelme.compiler.util.Pair;

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
			+ "\t\tfoo->addKernel(input_array_id, \"<functionName>\", extra_args, worksize);\n"
			+ "\t} catch(std::runtime_error &e) {\n"
			+ "\t\tstop_if(true, \"Error on call to ParallelME kernel at <functionName>: %s\", e.what());\n"
			+ "\t}";
	private static final String templateFunctionInitBodyJNI = "\ttry {\n"
			+ "\t\treturn reinterpret_cast<jlong>(new ParallelMERuntime(gJvm, env));\n"
			+ "\t} catch(std::runtime_error &e) {\n"
			+ "\t\tstop_if(true, \"Error on ParallelME runtime initialization: %s\", e.what());\n"
			+ "\t}";
	private static final String templateFunctionCreateBodyJNI = "\tauto foo = reinterpret_cast<castContents>(parallelMERuntime);\n"
			+ "\tdelete foo;";
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
		st.addAggr("functions.{decl, body, LDLIM, RDLIM}",
				this.getInitFunctionDecl(packageName, className),
				this.getInitFunctionImpl(), " {", "}");
		st.addAggr("functions.{decl, body, LDLIM, RDLIM}",
				this.getCleanUpFunctionDecl(packageName, className),
				this.getCleanUpFunctionImpl(), " {", "}");
		// stCPP.addAggr("functions.{decl, body}",
		// this.getCleanUpFunctionDecl());
		for (Iterator iterator : iterators) {
			Pair<String, String> declAndBody = this.getJNIFunctionDeclAndBody(
					packageName, className, iterator);
			st.addAggr("functions.{decl, body, LDLIM, RDLIM}",
					declAndBody.left, declAndBody.right, " {", "}");
			variableTypes.add(iterator.getVariable().typeName);
		}
		// 3. Add
		// Set to null to avoid errors in case of non-existing init functions
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
		st.addAggr("functions.{decl}",
				this.getInitFunctionDecl(packageName, className));
		st.addAggr("functions.{decl}",
				this.getCleanUpFunctionDecl(packageName, className));
		for (Iterator iterator : iterators) {
			Pair<String, String> declAndBody = this.getJNIFunctionDeclAndBody(
					packageName, className, iterator);
			st.addAggr("functions.{decl}", declAndBody.left);
		}
		return st.render();
	}

	/**
	 * Creates a JNI function declaration and body for a given iterator.
	 * 
	 * @return A pair where left is function declaration and right is its body.
	 */
	private Pair<String, String> getJNIFunctionDeclAndBody(String packageName,
			String className, Iterator iterator) {
		String cClassName = this.commonDefinitions.getCClassName(packageName,
				className);
		ST stDecl = new ST(templateFunctionDeclJNI);
		stDecl.add("return", "void");
		stDecl.add("className", cClassName);
		stDecl.add("functionName",
				this.commonDefinitions.getPrefixedIteratorName(iterator));
		stDecl.addAggr("params.{type, name}", "jlong", "parallelMERuntime");
		stDecl.addAggr("params.{type, name}", "int", "input_array_id");
		stDecl.addAggr("params.{type, name}", "int", "worksize");
		ST stBody = new ST(templateFunctionBodyJNI);
		stBody.add("ExtraArgumentType", "<ExtraArgument>");
		stBody.add("functionName",
				this.commonDefinitions.getPrefixedIteratorName(iterator));
		stBody.add("castContents", "<ParallelMERuntime *>");
		stBody.add("params", null);
		List<Pair<String, String>> params = this
				.getJNIFunctionDeclParams(iterator);
		for (int i = 0; i < params.size(); i++) {
			Pair<String, String> pair = params.get(i);
			stDecl.addAggr("params.{type, name}", pair.left, pair.right);
		}
		for (Variable variable : iterator.getExternalVariables()) {
			stBody.addAggr("params.{name, argType, argAlias}", variable.name,
					PrimitiveTypes.getRuntimeArgType(variable.typeName),
					PrimitiveTypes.getRuntimeAlias(variable.typeName));
		}
		return new Pair<String, String>(stDecl.render(), stBody.render());
	}

	/**
	 * Returns a string with init function declaration.
	 */
	private String getInitFunctionDecl(String packageName, String className) {
		ST st = new ST(templateFunctionDeclJNI);
		String cClassName = this.commonDefinitions.getCClassName(packageName,
				className);
		st.add("return", "jlong");
		st.add("className", cClassName);
		st.add("functionName", "init");
		st.add("params", null);
		return st.render();
	}

	/**
	 * Returns a string with int function implementation.
	 */
	private String getInitFunctionImpl() {
		ST st = new ST(templateFunctionInitBodyJNI);
		// Didn't find a better way to do this
		st.add("jlong", "<jlong>");
		return st.render();
	}

	/**
	 * Returns a string with the cleanup function declaration.
	 */
	private String getCleanUpFunctionDecl(String packageName, String className) {
		ST st = new ST(templateFunctionDeclJNI);
		String cClassName = this.commonDefinitions.getCClassName(packageName,
				className);
		st.add("return", "jlong");
		st.add("className", cClassName);
		st.add("functionName", "cleanUp");
		st.add("params", null);
		return st.render();
	}

	/**
	 * Returns a string with the cleanup function declaration.
	 */
	private String getCleanUpFunctionImpl() {
		ST st = new ST(templateFunctionCreateBodyJNI);
		st.add("castContents", "<ParallelMERuntime *>");
		st.addAggr("params.{type, name}", "jlong", "parallelMERuntimePtr");
		return st.render();
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
			List<Iterator> iterators) {
		String templateKernelFile = "<introductoryMsg>\n"
				+ "#ifndef KERNELS_H\n"
				+ "#define KERNELS_H\n\n"
				+ "const char kernels[] =\n"
				+ "\t<kernels:{var|\"<var.line>\"\n}>"
				+ "#endif\n";
		ST st = new ST(templateKernelFile);
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		for (Iterator iterator : iterators) {
			st.addAggr("kernels.{line}", this.getKernelFunctionDecl(iterator));
		}
		return st.render();
	}

	/**
	 * Creates kernel declaration for a given iterator.
	 */
	private  String getKernelFunctionDecl(Iterator iterator) {
		ST st = new ST(templateKernelDecl);
		st.add("return", "void");
		st.add("kernelName", "");// this.getClassKernelName(className));
		st.add("functionName",
				this.commonDefinitions.getPrefixedIteratorName(iterator));
		st.add("params", null);
		return st.render();
	}

	/**
	 * Analyzes a given iterator object, takes its external objects and creates
	 * a string for each parameter declaration according to C parameter types.
	 */
	private List<Pair<String, String>> getKernelFunctionDeclParams(
			Iterator iterator) {
		ArrayList<Pair<String, String>> ret = new ArrayList<>();
		for (Variable variable : iterator.getExternalVariables()) {
			// TODO Check if variable type is primitive. If it is not, show some
			// error or throw an exception (check what is the best option).
			ret.add(new Pair<String, String>(PrimitiveTypes
					.getCType(variable.typeName), variable.name));
		}
		return ret;
	}
}
