/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary;

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
				.add("org.parallelme.userlibrary.datatype");
		PackageDefinition.packages
				.add("org.parallelme.userlibrary.function");
		PackageDefinition.packages
				.add("org.parallelme.userlibrary.image");
		PackageDefinition.packages
				.add("org.parallelme.userlibrary.parallel");
	}

	public static String getBasePackage() {
		return "org.parallelme.userlibrary";
	}

	public static Set<String> getPackages() {
		if (PackageDefinition.packages.isEmpty())
			PackageDefinition.initPackages();
		return PackageDefinition.packages;
	}
}
