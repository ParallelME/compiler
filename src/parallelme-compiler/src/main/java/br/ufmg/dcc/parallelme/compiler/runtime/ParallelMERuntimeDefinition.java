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
import java.util.List;

import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;

/**
 * Definitions for ParallelME runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class ParallelMERuntimeDefinition extends RuntimeDefinitionImpl {


	public ParallelMERuntimeDefinition(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		super(cCodeTranslator, outputDestinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitializationString(String packageName, String className,
			List<Iterator> iterators) {
		String ret = "";
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImports(List<UserLibraryData> iteratorsAndBinds) {
		String ret = "";
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createAllocation(String className, InputBind inputBind) {
		String ret = "";
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String declareAllocation(InputBind inputBind) {
		String ret = "";
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationData(String className, OutputBind outputBind) {
		String ret = "";
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
