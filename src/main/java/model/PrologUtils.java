package model;

import org.apache.jena.base.Sys;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.jpl7.*;

import java.util.*;

public class PrologUtils {

    public static Query equalityQueryFrom(Set<List<NodeTerm>> equalities) {
        List<List<Term>> pairs = new ArrayList<>();

        for (List<NodeTerm> eq: equalities)
            pairs.add(Arrays.asList(
                    eq.get(0).asPrologTerm(),
                    eq.get(1).asPrologTerm()
            ));

        List<Term> unifies = new ArrayList<>();
        for (List<Term> pair: pairs)
            unifies.add(new Compound("=", new Term[]{pair.get(0), pair.get(1)}));

        //TODO DEBUG
//        System.out.println("NEW GOAL");
//        for (Term term: unifies) {
//            Query q = new Query(term);
//            if (! q.hasSolution()) {
//                System.out.println(term.arg(1));
//                System.out.println(term.arg(2));
//                System.out.println();
//            }
//        }

        return new Query(termListAsGoal(unifies));
    }

    private static Term termListAsGoal(List<Term> terms) {
        if (terms.size() == 1)
            return terms.get(0);
        return new Compound(",", new Term[] { terms.get(0), termListAsGoal(terms.subList(1, terms.size())) });
    }

    public static Term stringAsCharTermList(String str) {
        List<Term> termArray = new ArrayList<>();
        for (char c: str.toCharArray())
            termArray.add(new Atom(String.valueOf(c).toLowerCase()));
        return Term.termArrayToList(termArray.toArray(new Term[0]));
    }

    public static Term listtermToDifferenceList(Term term, Variable var) {
        if (term.listToTermArray().length == 0)
            return var;
        Term newterm = listtermToDifferenceList(term.arg(2), var);
        return new Compound("[|]", new Term[] { term.arg(1), newterm });
    }

}
