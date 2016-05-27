package org.parallelme.samples;

import org.parallelme.userlibrary.datatypes.Int32;
import org.parallelme.userlibrary.function.Foreach;

public class ArrayTest {
	public void method() {
		int i = 0;
		int[] tmp = new int[4];
		for (int x = 0; x < tmp.length; x++) {
			tmp[x] = ++i;
		}
		Array<Int32> array = new Array<Int32>(tmp, Int32.class);
		int varTeste = 0;
		array.par().foreach(new Foreach<Int32>() {
			@Override
			public void function(Int32 element) {
				varTeste += 1;
				element.value = element.value + varTeste;
			}
		});
		array.toJavaArray(tmp);
		//Int32 result = array.reduce(new Reduce<Int32>() {
		//	@Override
		//	public Int32 function(Int32 element1, Int32 element2) {
		//		element1.value += 10;
		//		return element1;
		//	}
		//});
	}
}
