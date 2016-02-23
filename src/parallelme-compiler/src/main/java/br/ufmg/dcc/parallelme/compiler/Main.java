package br.ufmg.dcc.parallelme.compiler;

import java.io.IOException;

import br.ufmg.dcc.parallelme.compiler.runtime.RenderScriptRuntimeDefinition;

public class Main {

	public static void main(String[] args) {

		try {

			(new Compiler(new RenderScriptRuntimeDefinition())).compile(args, "X:\\UFMG\\ParallelME\\compiler\\src\\samples\\output\\");
				

			/*HashSet<Class> converted = new HashSet<Class>();
			for (ArgumentClass class_to_convert : classes_to_convert) {
				if (class_to_convert.getTypeArgument() == null) {
					// TODO: Add support to well know classes like RGB
					System.out.println("Class "
							+ class_to_convert.getClassName() + " not found!");
				} else if (!class_to_convert.getTypeArgument().isConvertible()) {
					System.out.println("Can't convert class "
							+ class_to_convert.getClassName());
				} else if (!converted.contains(class_to_convert
						.getTypeArgument())) {
					System.out.println("Class to convert: "
							+ class_to_convert.getTypeArgument().getName());
					for (PrimitiveField f : class_to_convert.getTypeArgument()
							.getPrimitiveFields()) {
						System.out.println("\t" + f.getPrimitiveType() + " "
								+ f.getName());
					}
					converted.add(class_to_convert.getTypeArgument());
				}
			}*/

		} catch (IOException ioexception) {
			System.out.println("IOException!!");
		}

	}
}