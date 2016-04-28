/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.stringtemplate.v4.ST;

import org.parallelme.compiler.runtime.translation.data.Iterator;
import org.parallelme.compiler.runtime.translation.data.Variable;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.parallelme.compiler.util.Pair;

/**
 * Stores all actions and definitions used to create Java files contents.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeJavaFile {
	private CommonDefinitions commonDefinitions = new CommonDefinitions();
	private static final String templateClass = "<introductoryMsg>\n"
			+ "package <packageName>;\n"
			+ "\n<imports:{var|import <var.libraryName>;\n}>"
			+ "\npublic final class <className> {\n"
			+ "\tprivate static final <className> instance = new <className>();\n\n"
			+ "\tpublic static <className> getInstance() {\n"
			+ "\t\treturn this.instance;\n" + "\t}\n\n"
			+ "\t<\tfunctionsDecl:{var|<var.decl>;\n}>"
			+ "\n\t<functionsImpl:{var|\n\n<var.impl>}>}\n";
	private static final String templateNativeFunctionDecl = "private native <return> <functionName>(long runtimePointer<params:{var|<var.type> <var.name>}>)";
	private static final String templateFunctionImpl = "public void <functionName>(<paramsDecl:{var|<var.type> <var.name>}>) {\n"
			+ "\t<functionName>(ParallelMERuntimeJNIWrapper.getInstance().runtimePointer<paramsBody:{var|, <var.name>}>);\n"
			+ "}\n";

	/**
	 * Creates a string with a Java class that wraps all JNI calls
	 * 
	 * @param packageName
	 *            Name of the package of which the returned class will be part.
	 * @param className
	 *            Name of the JNI wrapper class.
	 */
	public String getJavaJNIWrapperClass(String packageName, String className,
			List<Iterator> iterators) {
		ST stClass = new ST(templateClass);
		stClass.add("packageName", packageName);
		stClass.add("className", className);
		// To avoid erros in case of non-existing imports or functions
		stClass.add("imports", null);
		stClass.add("functionsDecl", null);
		stClass.add("functionsImpl", null);
		stClass.add("introductoryMsg",
				this.commonDefinitions.getHeaderComment());
		Set<String> iteratorVariableTypes = this
				.getIteratorVariableTypes(iterators);
		// 1. Add all libraries
		for (String iteratorClass : iteratorVariableTypes) {
			for (String importLibrary : this.getImportLibrary(iteratorClass)) {
				stClass.addAggr("imports.{libraryName}", importLibrary);
			}
		}
		// 2. Add native functions' declaration
		for (Iterator iterator : iterators) {
			stClass.addAggr("functionsDecl.{decl}",
					this.getNativeFunctionDecl(iterator));
		}
		// 3. Add functions implementation
		for (Iterator iterator : iterators) {
			stClass.addAggr("functionsImpl.{impl}",
					this.getFunctionImpl(iterator));
		}
		// 4. Add conversion functions that are necessary for each variable type
		for (Pair<String, String> declImpl : this
				.getFunctionDeclImplByType(iteratorVariableTypes)) {
			stClass.addAggr("functionsDecl.{decl}", declImpl.left);
			stClass.addAggr("functionsImpl.{impl}", declImpl.right);
		}
		stClass.add("libraryName", "Name_yet_to_be_defined");
		return stClass.render();
	}

	private List<Pair<String, String>> getFunctionDeclImplByType(
			Set<String> iteratorVariableTypes) {
		ArrayList<Pair<String, String>> ret = new ArrayList<>();
		if (iteratorVariableTypes.contains(HDRImage.getName())
				|| iteratorVariableTypes.contains(BitmapImage.getName())) {
			ret.add(this.getToFloatDeclImpl());
			ret.add(this.getToBitmapDeclImpl());
		}
		return ret;
	}

	/**
	 * Creates a pair of strings containg the jni declaration (left) and the
	 * method implementation (right) for toFloat operation.
	 */
	private Pair<String, String> getToFloatDeclImpl() {
		ST stDecl = new ST(templateNativeFunctionDecl);
		stDecl.add("return", "void");
		stDecl.add("functionName", "toFloat");
		stDecl.addAggr("params.{type, name}", ", int", "input_array_id");
		stDecl.addAggr("params.{type, name}", ", int", "worksize");
		ST stImpl = new ST(templateFunctionImpl);
		stImpl.add("functionName", "toFloat");
		stImpl.addAggr("paramsDecl.{type, name}", "int", "input_array_id, ");
		stImpl.addAggr("paramsDecl.{type, name}", "int", "worksize");
		stImpl.addAggr("paramsBody.{name}", "input_array_id");
		stImpl.addAggr("paramsBody.{name}", "worksize");
		return new Pair<String, String>(stDecl.render(), stImpl.render());
	}

	/**
	 * Creates a pair of strings containg the jni declaration (left) and the
	 * method implementation (right) for toBitmap operation.
	 */
	private Pair<String, String> getToBitmapDeclImpl() {
		ST stDecl = new ST(templateNativeFunctionDecl);
		stDecl.add("return", "void");
		stDecl.add("functionName", "toBitmap");
		stDecl.addAggr("params.{type, name}", ", int", "input_array_id");
		stDecl.addAggr("params.{type, name}", ", int", "worksize");
		stDecl.addAggr("params.{type, name}", ", byte[]", "input_array");
		stDecl.addAggr("params.{type, name}", ", int", "input_buffer_size");
		stDecl.addAggr("params.{type, name}", ", Bitmap", "output_bitmap");
		stDecl.addAggr("params.{type, name}", ", int", "output_buffer_size");
		ST stImpl = new ST(templateFunctionImpl);
		stImpl.add("functionName", "toBitmap");
		stImpl.addAggr("paramsDecl.{type, name}", "int", "input_array_id, ");
		stImpl.addAggr("paramsDecl.{type, name}", "int", "worksize, ");
		stImpl.addAggr("paramsDecl.{type, name}", "byte[]", "input_array, ");
		stImpl.addAggr("paramsDecl.{type, name}", "int", "input_buffer_size, ");
		stImpl.addAggr("paramsDecl.{type, name}", "Bitmap", "bitmap, ");
		stImpl.addAggr("paramsDecl.{type, name}", "int", "bitmap_buffer_size");
		stImpl.addAggr("paramsBody.{name}", "input_array_id");
		stImpl.addAggr("paramsBody.{name}", "worksize");
		stImpl.addAggr("paramsBody.{name}", "input_array");
		stImpl.addAggr("paramsBody.{name}", "input_buffer_size");
		stImpl.addAggr("paramsBody.{name}", "bitmap");
		stImpl.addAggr("paramsBody.{name}", "bitmap_buffer_size");
		return new Pair<String, String>(stDecl.render(), stImpl.render());
	}

	/**
	 * Lists distinct variable types from a list of iterators.
	 */
	private Set<String> getIteratorVariableTypes(List<Iterator> iterators) {
		HashSet<String> types = new HashSet<>();
		for (Iterator iterator : iterators) {
			types.add(iterator.getVariable().typeName);
		}
		return types;
	}

	private List<String> getImportLibrary(String userLibraryClassName) {
		List<String> ret = new ArrayList<>();
		ret.add("org.parallelme.runtime.ParallelMERuntimeJNIWrapper");
		if (userLibraryClassName.equals(HDRImage.getName())
				|| userLibraryClassName.equals(BitmapImage.getName())) {
			ret.add("android.graphics.Bitmap");
		}
		return ret;
	}

	/**
	 * Returns a string with function declaration for a given iterator.
	 */
	private String getNativeFunctionDecl(Iterator iterator) {
		ST st = new ST(templateNativeFunctionDecl);
		st.add("return", "void");
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.addAggr("params.{type, name}", ", int", "input_array_id");
		st.addAggr("params.{type, name}", ", int", "worksize");
		for (Variable variable : iterator.getExternalVariables()) {
			String varType = ", " + variable.typeName;
			st.addAggr("params.{type, name}", varType, variable.name);
		}
		return st.render();
	}

	/**
	 * Returns a string with function implementation for a given iterator.
	 */
	private String getFunctionImpl(Iterator iterator) {
		ST st = new ST(templateFunctionImpl);
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.addAggr("paramsDecl.{type, name}", "int", "input_array_id, ");
		st.addAggr("paramsDecl.{type, name}", "int", "worksize");
		st.addAggr("paramsBody.{name}", "input_array_id");
		st.addAggr("paramsBody.{name}", "worksize");
		for (Variable variable : iterator.getExternalVariables()) {
			String varType = ", " + variable.typeName;
			st.addAggr("paramsDecl.{type, name}", varType, variable.name);
			st.addAggr("paramsBody.{name}", variable.name);
		}
		return st.render();
	}
}
