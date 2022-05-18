import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Obj;
import model.CoreLogicSAT;
import org.apache.jena.base.Sys;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class CoreLogicTest {
    @Test
    void clojuretest() {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("clojure.core.logic"));

        IFn eval = Clojure.var("clojure.core", "eval");

        Object rt2 = Clojure.read("(clojure.core.logic/run 1 [q] (clojure.core.logic/== q 1))");

        IFn macroexpand = Clojure.var("clojure.core", "macroexpand-1");

        System.out.println(macroexpand.invoke(rt2));
        System.out.println(eval.invoke(rt2));

    }

    @Test
    void clojureListTest() {
        List<Object> l = Arrays.asList(1,2,3,4,5);
        System.out.println(CoreLogicSAT.toClojureList(l));
    }
}
