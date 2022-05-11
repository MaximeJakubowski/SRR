package model;

import RML.*;
import org.apache.jena.sparql.algebra.op.OpProject;
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
        // the mapping body are always project queries
        OpProject projectOp = (OpProject) ((SPARQLSource) source).getQuery().getQueryOp();
        return new HashSet<>(projectOp.getVars());
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
