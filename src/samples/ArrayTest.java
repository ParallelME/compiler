package org.parallelme.samples;

import org.parallelme.userlibrary.datatypes.Int16;
import org.parallelme.userlibrary.function.ForeachFunction;

public class ArrayTest {
	public void method() {
		short i = 0;
		short[] tmp = new short[10];
		for (int x = 0; x < tmp.length; x++) {
			tmp[x] = ++i;
		}
		Array<Int16> array = new Array<Int16>(tmp, Int16.class, tmp.length);
		int varTeste = 0;
		array.par().foreach(new ForeachFunction<Int16>() {
			@Override
			public void function(Int16 element) {
				varTeste += 1;
				element.value = element.value + 10 + varTeste;
			}
		});
		System.out.println("=============================");
		short[] ret = (short[]) array.toJavaArray();
		for (int x = 0; x < tmp.length; x++) {
			System.out.println(x + " " + ret[x]);
		}
	}
}
