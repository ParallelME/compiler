/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import br.ufmg.dcc.parallelme.compiler.SimpleLogger;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.BoxedTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.PrimitiveTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.Float32;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.HDRImage;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.Int16;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.Int32;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.Pixel;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.RGB;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.RGBA;

/**
 * Code useful for specfic runtime definition implementation.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RuntimeDefinitionImpl implements RuntimeDefinition {
	protected final CTranslator cCodeTranslator;
	protected final String outputDestinationFolder;
	protected final CommonDefinitions commonDefinitions = new CommonDefinitions();

	public RuntimeDefinitionImpl(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		this.cCodeTranslator = cCodeTranslator;
		this.outputDestinationFolder = outputDestinationFolder;
	}

	/**
	 * Exports a given resource folder and all its contents.
	 * 
	 * @throws IOException
	 */
	protected void exportResource(String resourceName, String destinationFolder)
			throws IOException {
		URL resource = ClassLoader.getSystemClassLoader().getResource(
				resourceName);
		if (resource == null) {
			String msg = resourceName
					+ " resource folder is missing in this JAR. Please recompile the project.";
			SimpleLogger.error(msg);
			throw new RuntimeException(msg);
		}
		File resourceDir = null;
		try {
			resourceDir = new File(resource.toURI());
		} catch (URISyntaxException e) {
			SimpleLogger
					.error(resource
							+ " does not appear to be a valid URL / URI, thus it won't be copied to '"
							+ destinationFolder + "'.");
			resourceDir = null;
		}
		if (resourceDir != null && resourceDir.exists()) {
			// Get the list of the files contained in the package
			String[] list = resourceDir.list();
			for (int i = 0; i < list.length; i++) {
				String fileOrDirName = list[i];
				File source = new File(resourceDir.getAbsolutePath()
						+ File.separator + fileOrDirName);
				File destiny = new File(destinationFolder + File.separator
						+ fileOrDirName);
				if (source.isDirectory()) {
					FileUtils.copyDirectory(source, destiny);
				} else if (source.isFile()) {
					FileUtils.copyFile(source, destiny);
				}
			}
		}
	}

	/**
	 * Return the list of necessary imports for user library classes.
	 * 
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found in a given class.
	 * 
	 * @return String with the necessary imports.
	 */
	protected String getUserLibraryImports(
			List<UserLibraryData> iteratorsAndBinds) {
		StringBuffer imports = new StringBuffer();
		boolean exportedHDR = false;
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (!exportedHDR
					&& userLibraryData.getVariable().typeName.equals(HDRImage
							.getName())) {
				imports.append("import br.ufmg.dcc.parallelme.userlibrary.RGBE;\n");
				exportedHDR = true;
			}
		}
		return imports.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateVariable(Variable variable, String code) {
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateType(String typeName) {
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
}
