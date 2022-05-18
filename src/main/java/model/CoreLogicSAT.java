package model;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.PersistentList;

import java.util.List;
import java.util.Set;

public class CoreLogicSAT implements EqualitySAT {
    @Override
    public boolean isSAT(Set<List<NodeTerm>> equalities) {

        return false;
    }

    public static clojure.lang.ASeq toClojureList(List<Object> list) {
        IFn cons = Clojure.var("clojure.core", "cons");
        boolean first = true;
        clojure.lang.ASeq ret = new PersistentList("");
        for (Object obj: list)
            if (first) {
                ret = (clojure.lang.ASeq) cons.invoke(obj, Clojure.read("()"));
                first = false;
            } else {
                ret = (clojure.lang.ASeq) cons.invoke(obj, ret);
            }

        return ret;
    }
}