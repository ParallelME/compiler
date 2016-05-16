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
import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;

/**
 * Stores all actions and definitions used to create Java files contents.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeJavaFile {
	private RuntimeCommonDefinitions commonDefinitions = new RuntimeCommonDefinitions();
	private static final String templateClass = "<introductoryMsg>\n"
			+ "package <packageName>;\n"
			+ "\n<imports:{var|import <var.libraryName>;\n}>"
			+ "\npublic final class <className> {\n"
			+ "\tprivate static final <className> instance = new <className>();\n\n"
			+ "\tpublic static <className> getInstance() {\n"
			+ "\t\treturn this.instance;\n" + "\t}\n\n"
			+ "\t<\tfunctionsDecl:{var|<var.decl>;\n}>"
			+ "\n\t<functionsImpl:{var|\n\n<var.impl>}>}\n";
	private static final String templateNativeFunctionDecl = "private native <return> <functionName>(long runtimePointer<params:{var|, <var.type> <var.name>}>)";
	private static final String templateFunctionImpl = "public void <functionName>(<paramsDecl:{var|<var.type> <var.name>}; separator=\", \">) {\n"
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
		return stClass.render();
	}

	/**
	 * Lists distinct variable types from a list of iterators.
	 */
	private Set<String> getIteratorVariableTypes(List<Iterator> iterators) {
		HashSet<String> types = new HashSet<>();
		for (Iterator iterator : iterators) {
			types.add(iterator.variable.typeName);
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
		st.addAggr("params.{type, name}", "int", "inputBufferId");
		st.addAggr("params.{type, name}", "int", "outputBufferId");
		st.addAggr("params.{type, name}", "int", "worksize");
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}", variable.typeName, variable.name);
		}
		return st.render();
	}

	/**
	 * Returns a string with function implementation for a given iterator.
	 */
	private String getFunctionImpl(Iterator iterator) {
		ST st = new ST(templateFunctionImpl);
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.addAggr("paramsDecl.{type, name}", "int", "inputBufferId");
		st.addAggr("paramsDecl.{type, name}", "int", "outputBufferId");
		st.addAggr("paramsDecl.{type, name}", "int", "worksize");
		st.addAggr("paramsBody.{name}", "outputBufferId");
		st.addAggr("paramsBody.{name}", "inputBufferId");
		st.addAggr("paramsBody.{name}", "worksize");
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("paramsDecl.{type, name}", variable.typeName,
					variable.name);
			st.addAggr("paramsBody.{name}", variable.name);
		}
		return st.render();
	}
}
