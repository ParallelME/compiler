package br.ufmg.dcc.parallelme.test;

import br.ufmg.dcc.parallelme.userlibrary.datatypes.Int32;
import br.ufmg.dcc.parallelme.userlibrary.function.ForeachFunction;

public class ArrayTest {
	public void method() {
		int i = 0;
		int[] tmp = new int[10];
		for (int x = 0; x < tmp.length; x++) {
			tmp[x] = ++i;
		}
		Array<Int32> array = new Array<Int32>(tmp, Int32.class, tmp.length);
		int varTeste = 0;
		array.par().foreach(new ForeachFunction<Int32>() {
			@Override
			public void function(Int32 element) {
				varTeste += 1;
				element.value = element.value + 10;
			}
		});
		System.out.println("=============================");
		int[] ret = (int[]) array.toJavaArray();
		for (int x = 0; x < tmp.length; x++) {
			System.out.println(x + " " + ret[x]);
		}
	}
}
