package org.parallelme.nestedloop;

import org.parallelme.userlibrary.Array;
import org.parallelme.userlibrary.datatypes.Int32;
import org.parallelme.userlibrary.function.ForeachFunction;

public class NestedLoops {
    int varTeste = 0;
    Array<Int32> array;
    public void method() {
        int i = 0;
        int[] tmp = new int[10];
        for (int x = 0; x < tmp.length; x++) {
            tmp[x] = ++i;
        }
        array = new Array<Int32>(tmp, Int32.class);

        array.par().foreach(new ForeachFunction<Int32>() {
            @Override
            public void function(final Int32 element) {
                //varTeste += 1;
                array.par().foreach(new ForeachFunction<Int32>() {
                    @Override
                    public void function(Int32 element2) {
                        varTeste += 2;
                        element2.value = element2.value + 10 + varTeste;
                    }
                });
                element.value = element.value + 10; //+ varTeste;
            }
        });
        System.out.println("=============================");
        int[] ret = (int[]) array.toJavaArray();
        for (int x = 0; x < tmp.length; x++) {
            System.out.println(x + " " + ret[x]);
        }
        System.out.println("varTeste: " + varTeste);
    }
}
