package model;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.jpl7.*;

import java.util.*;

public class PrologUtils {

    public static Query equalityQueryFrom(Set<NodeTermPair> equalities) {
        List<List<Term>> pairs = new ArrayList<>();

        for (NodeTermPair npair: equalities)
            pairs.add(
                    Arrays.asList(
                            nodeAsTerm(npair.getNode()),
                            fptermAsTerm(npair.getTerm()))
            );

        List<Term> unifies = new ArrayList<>();
        for (List<Term> pair: pairs)
            unifies.add(new Compound("=", new Term[]{ pair.get(0), pair.get(1) }));

        return new Query(termListAsGoal(unifies));
    }

    private static Term termListAsGoal(List<Term> terms) {
        if (terms.size() == 1)
            return terms.get(0);
        return new Compound(",", new Term[] { terms.get(0), termListAsGoal(terms.subList(1, terms.size())) });
    }

    public static Term nodeAsTerm(Node node) {
        // TODO check if this is a good enough approach: do we ever get in trouble?
        // especially relating to the handing of the case of a variable node
        if (node.isVariable())
            return new Variable(node.getName()); // important: node.getName() != node.toString()

        String type = "unknown";
        if (node.isBlank())
            type = "blank";
        if (node.isLiteral())
            return new Compound("literal", new Term[]{ stringAsTermList(node.getLiteral().toString()) });
        if (node.isURI())
            type = "iri";
        return new Compound(type, new Term[]{ stringAsTermList(node.toString()) });
        // TODO proper handling of unknown
    }

    private static Term stringAsTermList(String str) {
        List<Term> termArray = new ArrayList<>();
        for (char c: str.toCharArray())
            termArray.add(new Atom(String.valueOf(c)));
        return Term.termArrayToList(termArray.toArray(new Term[0]));
    }

    private static Term fptermAsTerm(FlatPTerm pterm) {
        String type = "unknown";
        if (pterm.isBlank())
            type = "blank";
        if (pterm.isLiteral())
            type = "literal";
        if (pterm.isIRI())
            type = "iri";

        return new Compound(type, new Term[] { ptermAsTermList(pterm) });
    }

    private static Term ptermAsTermList(FlatPTerm pterm) {
        // TODO for now, only naive templates (ending on variable, only one variable)
        List<Term> termList = new ArrayList<>();
        for (Object obj : pterm.getStructure())
            if (obj instanceof String)
                for (char c: ((String) obj).toCharArray())
                    termList.add(new Atom(String.valueOf(c)));
            else if (obj instanceof Var)
                return listtermToDifferenceList(
                        Term.termArrayToList(termList.toArray(new Term[0])),
                        new Variable(((Var) obj).getVarName()));
        return Term.termArrayToList(termList.toArray(new Term[0]));
    }

    private static Term listtermToDifferenceList(Term term, Variable var) {
        if (term.listToTermArray().length == 0)
            return var;
        Term newterm = listtermToDifferenceList(term.arg(2), var);
        return new Compound("[|]", new Term[] { term.arg(1), newterm });
    }

}
