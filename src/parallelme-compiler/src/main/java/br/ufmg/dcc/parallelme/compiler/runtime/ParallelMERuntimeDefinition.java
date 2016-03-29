/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.stringtemplate.v4.ST;

import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.PrimitiveTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClass;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.BitmapImage;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.HDRImage;
import br.ufmg.dcc.parallelme.compiler.util.FileWriter;
import br.ufmg.dcc.parallelme.compiler.util.Pair;

/**
 * Definitions for ParallelME runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class ParallelMERuntimeDefinition extends RuntimeDefinitionImpl {
	private static final String templatePointer = "long <pointerName>;\n";
	private static final String templateNativeFunctionsJava = "\tprivate native void <functionName>(long $pointer<params:{var|, <var.type> <var.name>}>);\n";
	private static final String templateCreateAllocationJavaHDRImage = "RGBE.ResourceData <resourceData> = RGBE.loadFromResource(<params>);\n"
			+ "\t<pointerName> = <initializeFunction>(<resourceData>, <resourceData>.data.width, <resourceData>.data.height);";
	private static final String templateInitFunctionJava = "\tprivate native long <functionName>(byte[] data<params:{var|, <var.type> <var.name>}>);\n";
	private static final String templateCleanUpFunctionJava = "\tprivate native void <functionName>(long $pointer);\n";
	private static final String templateFunctionDeclJNI = "JNIEXPORT <return> JNICALL Java_<className>_<functionName>(JNIEnv *env, jobject a<params:{var|, <var.type> <var.name>}>)";
	private static final String templateFunctionInitBodyJNI = "\ttry {\n"
			+ "\tauto $pointer = new <className>Kernel(gJvm, env<params:{var|, <var.name>}>);\n"
			+ "\t\treturn reinterpret_cast<jlong>($pointer);\n"
			+ "\t} catch(std::runtime_error &e) {\n"
			+ "\t\tstop_if(true, \"Error on ParallelME runtime initialization: %s\", e.what());\n"
			+ "\t}\n}";
	private static final String templateFunctionBodyJNI = "\tauto foo = reinterpret_cast<castContents>($pointer);\n"
			+ "\ttry {\n"
			+ "\t\tfoo-><functionName>(<params:{var|<var.name>}>);\n"
			+ "\t} catch(std::runtime_error &e) {\n"
			+ "\t\tstop_if(true, \"Error on call to ParallelME kernel: %s\", e.what());\n"
			+ "\t}";
	private static final String templateJNICppFile = "<introductoryMsg>\n<includes:{var|#include <var.value>\n}>"
			+ "\nJavaVM *gJvm;\n\n"
			+ "JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {\n"
			+ "\tgJvm = jvm;\n"
			+ "\treturn JNI_VERSION_1_6;\n"
			+ "}\n\n"
			+ "<initFunctions:{var|<var.declaration> {\n<var.body>\n}>"
			+ "<functions:{var|<var.declaration><var.body>\n\n}>";

	public ParallelMERuntimeDefinition(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		super(cCodeTranslator, outputDestinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitializationString(String packageName, String className,
			List<InputBind> inputBinds, List<Iterator> iterators,
			List<OutputBind> outputBinds) {
		String ret = this.getInitializationStringJava(inputBinds, iterators,
				outputBinds);
		this.createJNIFile(packageName, className, inputBinds, iterators,
				outputBinds);
		return ret;
	}

	/**
	 * Creates the initialization string that will be used to change the
	 * original Java file written by the user.
	 */
	private String getInitializationStringJava(List<InputBind> inputBinds,
			List<Iterator> iterators, List<OutputBind> outputBinds) {
		StringBuilder ret = new StringBuilder();
		HashSet<Variable> variables = new HashSet<>();
		for (Iterator iterator : iterators) {
			ST st = new ST(templateNativeFunctionsJava);
			st.add("functionName", this.getPrefixedIteratorName(iterator));
			st.add("params", null);
			for (Variable variable : iterator.getExternalVariables()) {
				st.addAggr("params.{type, name}", variable.typeName,
						variable.name);
			}
			ret.append(st.render());
			variables.add(iterator.getVariable());
		}
		for (InputBind inputBind : inputBinds) {
			ret.append(this.getInitializationFunctionDecl(inputBind));
		}
		for (OutputBind outputBind : outputBinds) {
			ret.append(this.getCleanUpFunctionDecl(outputBind));
		}
		return ret.toString();
	}

	/**
	 * Create the JNI file that will be used to connect the Java environment
	 * with ParallelME runtime.
	 */
	private void createJNIFile(String packageName, String className,
			List<InputBind> inputBinds, List<Iterator> iterators,
			List<OutputBind> outputBinds) {
		ST st = new ST(templateJNICppFile);
		st.add("introductoryMsg", this.getHeaderComment());
		for (Iterator iterator : iterators) {
			Pair<String, String> declAndBody = this.getJNIFunctionDeclAndBody(
					packageName, className, iterator);
			st.addAggr("functions.{declaration, body}", declAndBody.left,
					declAndBody.right);
		}
		String cClassName = this.getCClassName(packageName, className);
		st.addAggr("includes.{value}", "\"" + cClassName + ".h\"");
		st.addAggr("includes.{value}", "\"error.h\"");
		st.addAggr("includes.{value}", "<stdexcept>");
		st.add("className", className);
		st.add("initFunctions", null);
		HashSet<String> variableTypes = new HashSet<>();
		for (InputBind inputBind : inputBinds) {
			variableTypes.add(inputBind.getVariable().typeName);
			Pair<String, String> declAndBody = this.getJNIFunctionDeclAndBody(
					packageName, className, inputBind);
			st.addAggr("initFunctions.{declaration, body}", declAndBody.left,
					declAndBody.right);
		}
		for (String variableType : variableTypes) {
			String importValue = this.getImportLibrary(variableType);
			if (importValue != null)
				st.addAggr("includes.{value}", importValue);
		}
		for (OutputBind outputBind : outputBinds) {
			Pair<String, String> declAndBody = this.getJNIFunctionDeclAndBody(
					packageName, className, outputBind);
			st.addAggr("functions.{declaration, body}", declAndBody.left,
					declAndBody.right);
		}
		String fileName = cClassName + ".cpp";
		FileWriter.writeFile(fileName, this.outputDestinationFolder,
				st.render());
	}

	private String getPointerName(Variable variable) {
		return this.getPrefix() + variable.name + "Ptr";
	}

	/**
	 * Creates a JNI function declaration and body for a given iterator.
	 * 
	 * @return A pair where left is function declaration and right is its body.
	 */
	private Pair<String, String> getJNIFunctionDeclAndBody(String packageName,
			String className, Iterator iterator) {
		String cClassName = this.getCClassName(packageName, className);
		ST stFunction = new ST(templateFunctionDeclJNI);
		stFunction.add("return", "void");
		stFunction.add("className", cClassName);
		stFunction.add("functionName", this.getPrefixedIteratorName(iterator));
		stFunction.addAggr("params.{type, name}", "jlong", "$pointer");
		ST stBody = new ST(templateFunctionBodyJNI);
		stBody.add("functionName", this.getPrefixedIteratorName(iterator));
		stBody.add("castContents", "<" + className + "Kernel *>");
		stBody.add("params", null);
		List<Pair<String, String>> params = this
				.getFunctionDeclParams(iterator);
		for (int i = 0; i < params.size(); i++) {
			Pair<String, String> pair = params.get(i);
			stFunction.addAggr("params.{type, name}", pair.left, pair.right);
			String param = pair.right;
			if (i != params.size() - 1) {
				param += ", ";
			}
			stBody.addAggr("params.{name}", param);
		}
		String iteratorBody = " {\n" + stBody.render() + "\n}";
		return new Pair<String, String>(stFunction.render(), iteratorBody);
	}

	/**
	 * Creates a JNI function declaration and body for a given input bind.
	 * 
	 * @return A pair where left is function declaration and right is its body.
	 */
	private Pair<String, String> getJNIFunctionDeclAndBody(String packageName,
			String className, InputBind inputBind) {
		String cClassName = this.getCClassName(packageName, className);
		ST stFunction = new ST(templateFunctionDeclJNI);
		ST stBody = new ST(templateFunctionInitBodyJNI);
		stFunction.add("return", "jlong");
		stFunction.add("className", cClassName);
		stFunction
				.add("functionName", this.getPrefixedInputBindName(inputBind));
		stBody.add("className", className);
		// Didn't find a better way to do this
		stBody.add("jlong", "<jlong>");
		for (Pair<String, String> param : this
				.getInitializationFunctionDeclParams(inputBind)) {
			stFunction.addAggr("params.{type, name}", param.left, param.right);
			stBody.addAggr("params.{name}", param.right);
		}
		return new Pair<String, String>(stFunction.render(), stBody.render());
	}

	/**
	 * Creates a JNI function declaration and body for a given output bind.
	 * 
	 * @return A pair where left is function declaration and right is its body.
	 */
	private Pair<String, String> getJNIFunctionDeclAndBody(String packageName,
			String className, OutputBind outputBind) {
		String cClassName = this.getCClassName(packageName, className);
		ST stFunction = new ST(templateFunctionDeclJNI);
		stFunction.add("return", "void");
		stFunction.add("className", cClassName);
		stFunction.add("functionName", this.getPrefixedOutputBindName(outputBind));
		stFunction.addAggr("params.{type, name}", "jlong", "$pointer");
		String outputBindBody = " {\n" + "\tauto foo = reinterpret_cast<"
				+ className + "Kernel *>($pointer)" + "\n\tdelete foo;" + "\n}";
		return new Pair<String, String>(stFunction.render(), outputBindBody);
	}

	/**
	 * Returns a string with the initialization function declaration to be used
	 * in Java code.
	 */
	private String getInitializationFunctionDecl(InputBind inputBind) {
		ST st = new ST(templateInitFunctionJava);
		st.add("functionName", this.getPrefixedInputBindName(inputBind));
		st.add("params", null);
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(inputBind.getVariable().typeName);
		if (userLibraryClass instanceof BitmapImage) {

		} else if (userLibraryClass instanceof HDRImage) {
			st.addAggr("params.{type, name}", "int", "width");
			st.addAggr("params.{type, name}", "int", "height");
		}
		return st.render();
	}

	/**
	 * Analyzes a given input bind object and creates a string for each
	 * parameter declaration that is necessary to perform an input bind at JNI
	 * level.
	 */
	private List<Pair<String, String>> getInitializationFunctionDeclParams(
			InputBind inputBind) {
		ArrayList<Pair<String, String>> ret = new ArrayList<>();
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(inputBind.getVariable().typeName);
		if (userLibraryClass instanceof BitmapImage) {

		} else if (userLibraryClass instanceof HDRImage) {
			ret.add(new Pair<String, String>("jbyteArray", "image"));
			ret.add(new Pair<String, String>("jint", "width"));
			ret.add(new Pair<String, String>("jint", "height"));
		}
		return ret;
	}

	/**
	 * Analyzes a given iterator object, takes its external objects and creates
	 * a string for each parameter declaration according to JNI parameter types.
	 */
	private List<Pair<String, String>> getFunctionDeclParams(Iterator iterator) {
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
	 * Returns a string with the cleanup function declaration to be used in Java
	 * code.
	 */
	private String getCleanUpFunctionDecl(OutputBind outputBind) {
		ST st = new ST(templateCleanUpFunctionJava);
		st.add("functionName", this.getPrefixedOutputBindName(outputBind));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImports(List<UserLibraryData> iteratorsAndBinds) {
		StringBuffer ret = new StringBuffer();
		ret.append(this.getUserLibraryImports(iteratorsAndBinds));
		ret.append("\n");
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createAllocation(String className, InputBind inputBind) {
		String ret = "";
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(inputBind.getVariable().typeName);
		if (userLibraryClass instanceof BitmapImage) {

		} else if (userLibraryClass instanceof HDRImage) {
			String resourceData = this.getPrefix() + inputBind.getVariable()
					+ "ResourceData";
			ST st = new ST(templateCreateAllocationJavaHDRImage);
			st.add("resourceData", resourceData);
			st.add("params",
					this.toCommaSeparatedString(inputBind.getParameters()));
			st.add("pointerName", this.getPointerName(inputBind.getVariable()));
			st.add("initializeFunction", this.getPrefixedInputBindName(inputBind));
			ret = st.render();
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String declareAllocation(InputBind inputBind) {
		StringBuilder ret = new StringBuilder();
		ST st = new ST(templatePointer);
		st.add("pointerName", this.getPointerName(inputBind.getVariable()));
		ret.append(st.render());
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationData(String className, OutputBind outputBind) {
		String ret = "teste";
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIteratorCall(String className, Iterator iterator) {
		String ret = "";
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean translateIteratorsAndBinds(String packageName,
			String className, List<Iterator> iterators,
			List<InputBind> inputBinds, List<OutputBind> outputBinds) {

		return false;
	}

	private String getCClassName(String packageName, String className) {
		return packageName.replaceAll("\\.", "_") + "_" + className;
	}

	/**
	 * Return a library name for a given type.
	 * 
	 * @param typeName
	 *            Type name.
	 * @return Library name for specified type.
	 */
	private String getImportLibrary(String typeName) {
		if (typeName.equals(HDRImage.getName())
				|| typeName.equals(BitmapImage.getName())) {
			return "<android/bitmap.h>";
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateVariable(Variable variable, String code) {
		String translatedCode = "";
		return translatedCode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateType(String typeName) {
		String translatedType = "";
		return translatedType;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exportInternalLibrary(String packageName,
			String destinationFolder) throws IOException {
		// Copy all files and directories under ParallelME resource folder to
		// the destination folder.
		this.exportResource("ParallelME", destinationFolder);
		this.exportResource("Common", destinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(MethodCall methodCall) {
		String ret = "";
		return ret;
	}
}
