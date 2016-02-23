/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.userlibrary;

import java.util.HashSet;
import java.util.Set;

/**
 * Package definitions for user library.
 * 
 * @author Wilson de Carvalho
 */
public class PackageDefinition {
	private static HashSet<String> packages = new HashSet<String>();

	private static void initPackages() {
		PackageDefinition.packages.add(PackageDefinition.getBasePackage());
		PackageDefinition.packages
				.add("br.ufmg.dcc.parallelme.userlibrary.function");
		PackageDefinition.packages
				.add("br.ufmg.dcc.parallelme.userlibrary.image");
		PackageDefinition.packages
				.add("br.ufmg.dcc.parallelme.userlibrary.parallel");
	}
	
	public static String getBasePackage() {
		return "br.ufmg.dcc.parallelme.userlibrary";
	}

	public static Set<String> getPackages() {
		if (PackageDefinition.packages.isEmpty())
			PackageDefinition.initPackages();
		return PackageDefinition.packages;
	}
}
