package model;

import RML.PredicateObjectMap;
import RML.RMLReference;
import RML.Source;
import RML.TermMap;
import org.apache.jena.sparql.core.Var;

import java.util.HashSet;
import java.util.Set;

// Represents "a single mapping"
public class UnrolledTriplesMap {
    private TermMap subjectMap;
    private PredicateObjectMap predicateObjectMap;
    private Source source;

    public UnrolledTriplesMap(TermMap subjectMap, PredicateObjectMap predicateObjectMap, Source source) {
        this.subjectMap = subjectMap;
        this.predicateObjectMap = predicateObjectMap;
        this.source = source;
    }

    public Set<Var> getAllVariables() {
        Set<Var> ret = new HashSet<>();
        for (RMLReference ref: subjectMap.getReferences())
            ret.add(Var.alloc(ref.getReference()));
        for (RMLReference ref: predicateObjectMap.getPredicate().getReferences())
            ret.add(Var.alloc(ref.getReference()));
        for (RMLReference ref: predicateObjectMap.getObject().getReferences())
            ret.add(Var.alloc(ref.getReference()));
        return ret;
    }

    public TermMap getSubjectMap() {
        return subjectMap;
    }

    public PredicateObjectMap getPredicateObjectMap() {
        return predicateObjectMap;
    }

    public Source getSource() {
        return source;
    }
}
