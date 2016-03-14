/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Should use Log4j here, but this is a simple logger for standart output
 * printing only.
 * 
 * @author Wilson de Carvalho
 */
public class SimpleLogger {
	static private SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");
	public static boolean logInfo = false;
	public static boolean logError = false;
	public static boolean logWarn = false;

	public static void info(String msg) {
		if (logInfo)
			System.out.println(sdf.format(new Date()) + " - " + msg);
	}

	public static void error(String msg) {
		if (logError)
			System.out.println("[ERROR] " + sdf.format(new Date()) + " - "
					+ msg);
	}
	
	public static void warn(String msg) {
		if (logWarn)
			System.out.println("[WARN] " + sdf.format(new Date()) + " - "
					+ msg);
	}
}
