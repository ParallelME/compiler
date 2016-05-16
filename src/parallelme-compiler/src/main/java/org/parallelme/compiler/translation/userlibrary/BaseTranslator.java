/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.userlibrary;

import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.BoxedTypes;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.Int16;
import org.parallelme.compiler.userlibrary.classes.Int32;
import org.parallelme.compiler.userlibrary.classes.Pixel;
import org.parallelme.compiler.userlibrary.classes.RGB;
import org.parallelme.compiler.userlibrary.classes.RGBA;

/**
 * Base class for translators containing code that is shared between different
 * target runtimes.
 * 
 * @author Wilson de Carvalho
 */
public abstract class BaseTranslator implements UserLibraryTranslatorDefinition {
	protected RuntimeCommonDefinitions commonDefinitions = new RuntimeCommonDefinitions();

	/**
	 * Translates a given type to an equivalent runtime type. Example: translate
	 * RGB type to float3 on RenderScript.
	 * 
	 * @param typeName
	 *            Type that must be translated.
	 * @return A string with the equivalent type for this runtime.
	 */
	protected String translateType(String typeName) {
		String translatedType = "";
		if (typeName.equals(RGB.getName())) {
			translatedType = "float3";
		} else if (typeName.equals(RGBA.getName())) {
			translatedType = "float4";
		} else if (typeName.equals(Pixel.getName())) {
			translatedType = "float4";
		} else if (typeName.equals(Int16.getName())) {
			translatedType = "short";
		} else if (typeName.equals(Int32.getName())) {
			translatedType = "int";
		} else if (typeName.equals(Float32.getName())) {
			translatedType = "float";
		} else if (PrimitiveTypes.isPrimitive(typeName)) {
			translatedType = PrimitiveTypes.getCType(typeName);
		} else if (BoxedTypes.isBoxed(typeName)) {
			translatedType = BoxedTypes.getCType(typeName);
		}
		return translatedType;
	}

	/**
	 * Translates variables on the give code to a correspondent runtime-specific
	 * type. Example: replaces all RGB objects by float3 on RenderScript.
	 * 
	 * @param variable
	 *            Variable that must be translated.
	 * @param code
	 *            Original code that must have the reference replaced.
	 * @return A string with the new code with the variable replaced.
	 */
	protected String translateVariable(Variable variable, String code) {
		String translatedCode = "";
		if (variable.typeName.equals(RGB.getName())) {
			translatedCode = this.translateRGBVariable(variable, code);
		} else if (variable.typeName.equals(RGBA.getName())) {
			translatedCode = this.translateRGBAVariable(variable, code);
		} else if (variable.typeName.equals(Pixel.getName())) {
			translatedCode = this.translatePixelVariable(variable, code);
		} else if (variable.typeName.equals(Int16.getName())
				|| variable.typeName.equals(Int32.getName())
				|| variable.typeName.equals(Float32.getName())) {
			translatedCode = this.translateNumericVariable(variable, code);
		} else if (PrimitiveTypes.isPrimitive(variable.typeName)) {
			translatedCode = code.replaceAll(variable.typeName,
					PrimitiveTypes.getCType(variable.typeName));
		} else if (BoxedTypes.isBoxed(variable.typeName)) {
			translatedCode = code.replaceAll(variable.typeName,
					BoxedTypes.getCType(variable.typeName));
		}
		return translatedCode;
	}

	protected String translateRGBVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".red", variable.name + ".s0");
		ret = ret.replaceAll(variable.name + ".green", variable.name + ".s1");
		ret = ret.replaceAll(variable.name + ".blue", variable.name + ".s2");
		return ret;
	}

	protected String translateRGBAVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".red", variable.name + ".s0");
		ret = ret.replaceAll(variable.name + ".green", variable.name + ".s1");
		ret = ret.replaceAll(variable.name + ".blue", variable.name + ".s2");
		ret = ret.replaceAll(variable.name + ".alpha", variable.name + ".s3");
		return ret;
	}

	protected String translatePixelVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".x", "x");
		ret = ret.replaceAll(variable.name + ".y", "y");
		ret = ret
				.replaceAll(variable.name + ".rgba.red", variable.name + ".s0");
		ret = ret.replaceAll(variable.name + ".rgba.green", variable.name
				+ ".s1");
		ret = ret.replaceAll(variable.name + ".rgba.blue", variable.name
				+ ".s2");
		ret = ret.replaceAll(variable.name + ".rgba.alpha", variable.name
				+ ".s3");
		return ret;
	}

	protected String translateNumericVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".value", variable.name);
		return ret;
	}

	/**
	 * Create a global variable name for the given variable following some
	 * standards. Global variables will be prefixed with "g" followed by an
	 * upper case letter and sufixed by the iterator name, so "max" from
	 * iterator 2 becomes "gMax_Iterator2"
	 */
	protected String getGlobalVariableName(String variable, Iterator iterator) {
		String iteratorName = this.commonDefinitions.getIteratorName(iterator);
		String variableName = this.upperCaseFirstLetter(variable);
		return "g" + variableName + this.upperCaseFirstLetter(iteratorName);
	}

	/**
	 * Change the first letter of the informed string to upper case.
	 */
	protected String upperCaseFirstLetter(String string) {
		return string.substring(0, 1).toUpperCase()
				+ string.substring(1, string.length());
	}

	/**
	 * Replace all instances of a given oldName by a newName, checking if this
	 * newName contains a $ sign, which is reserved in regex. In case a $
	 * exists, it will be escaped during replacement.
	 */
	protected String replaceAndEscapePrefix(String string, String newName,
			String oldName) {
		if (newName.contains("$")) {
			int idx = newName.indexOf("$");
			newName = newName.substring(0, idx) + "\\$"
					+ newName.substring(idx + 1, newName.length());
			return string.replaceAll(oldName, newName);
		} else {
			return string.replaceAll(oldName, newName);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateIterator(String className, Iterator iterator) {
		String ret;
		// Translate parallel iterators
		if (iterator.getType() == IteratorType.Parallel) {
			ret = this.translateParallelIterator(iterator);
		} else {
			ret = this.translateSequentialIterator(iterator);
		}
		return ret;
	}

	/**
	 * Translates a parallel iterator returning a C code compatible with this
	 * runtime.
	 * 
	 * @param iterator
	 *            Iterator that must be translated.
	 * @return C code with iterator's user code compatible with this runtime.
	 */
	abstract protected String translateParallelIterator(Iterator iterator);

	/**
	 * Translates a sequential iterator returning a C code compatible with this
	 * runtime.
	 * 
	 * @param iterator
	 *            Iterator that must be translated.
	 * @return C code with iterator's user code compatible with this runtime.
	 */
	abstract protected String translateSequentialIterator(Iterator iterator);
}
