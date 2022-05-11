import RML.*;
import model.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jpl7.Term;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Rewriter extends TransformCopy {
    private final Set<Source> sources;
    private int variableCounter = 0;

    public Rewriter(Set<Source> sources) {
        this.sources = sources;
    }

    @Override
    public Op transform(OpBGP opBGP) {
        Set<Op> unionSubqueries = new HashSet<>();
        List<Map<Triple, UnrolledTriplesMap>> candidates = generateMappingCandidates(opBGP.getPattern());
        for (Map<Triple, UnrolledTriplesMap> candidate : candidates)
            unionSubqueries.add(createRewrittenBGP(candidate));
        return AlgebraUtils.makeUnion(unionSubqueries);
    }

    private Op createRewrittenBGP(Map<Triple, UnrolledTriplesMap> candidate) {
        Set<List<NodeTerm>> equalityConstraints = new HashSet<>();

        Map<Triple, Map<String, String>> renamings = new HashMap<>();

        for (Triple tp : candidate.keySet()) {
            Map<String, String> renaming = renameVariablesFresh(candidate.get(tp));
            renamings.put(tp, renaming); // remember renaming for every triple

            equalityConstraints.add(Arrays.asList(
                    NodeTerm.create(tp.getSubject()),
                    NodeTerm.create(candidate.get(tp).getSubjectMap()).renamed(renaming))
            );

            equalityConstraints.add(Arrays.asList(
                    NodeTerm.create(tp.getPredicate()),
                    NodeTerm.create(candidate.get(tp).getPredicateObjectMap().getPredicate()).renamed(renaming))
            );

            equalityConstraints.add(Arrays.asList(
                    NodeTerm.create(tp.getObject()),
                    NodeTerm.create(candidate.get(tp).getPredicateObjectMap().getObject()).renamed(renaming))
            );
        }

        if (!satisfiableMatching(equalityConstraints))
            return AlgebraUtils.emptyQuery();

        return buildQuery(equalityConstraints, candidate, renamings);
    }


    private Op buildQuery(Set<List<NodeTerm>> equalityConstraints,
                          Map<Triple, UnrolledTriplesMap> match, Map<Triple, Map<String, String>> renamings) {
        List<Set<List<NodeTerm>>> filterBindingEq = getFilterAndBindEqualities(equalityConstraints);

        Set<List<NodeTerm>> filterEqualities = filterBindingEq.get(0);
        Set<List<NodeTerm>> bindingEqualities = filterBindingEq.get(1);

        Set<Op> tojoin = new HashSet<>();
        for (Triple tp : match.keySet())
            tojoin.add(applyRenaming(renamings.get(tp),
                    ((SPARQLSource) match.get(tp).getSource()).getQuery().getQueryOp()
            ));

        Op rewrittenBGP = OpFilter.filterBy(
                createFilterEqualities(filterEqualities),
                AlgebraUtils.makeJoin(tojoin));
        // if no vars, select *; otherwise select vars(renaming)
        if (renamings.values().stream().allMatch(renaming -> renaming.keySet().isEmpty())) //there were no variables
            return rewrittenBGP;

        return createProjectionRenaming(bindingEqualities, rewrittenBGP);
    }

    private List<Set<List<NodeTerm>>> getFilterAndBindEqualities(Set<List<NodeTerm>> equalities) {
        // We assume equalities is SAT
        // element 1: returns a subset of the equalities that represent the filter statements
        // element 2: returns a subset of the equalities that represent the bind statements
        // convention: the first element of the list is always a user query NodeTerm
        //             the second element of the list is always a mapping body NodeTerm
        Graph<NodeTerm, DefaultEdge> eqGraph = new SimpleGraph<>(DefaultEdge.class);
        Set<NodeTerm> uqNodeTerms = new HashSet<>();
        for (List<NodeTerm> eq: equalities) {
            // ignore self-loops: equality on identical elements
            if (eq.get(0).equals(eq.get(1)))
                continue;
            uqNodeTerms.add(eq.get(0));
            eqGraph.addVertex(eq.get(0));
            eqGraph.addVertex(eq.get(1));
            eqGraph.addEdge(eq.get(0), eq.get(1));
        }

        Set<List<NodeTerm>> filterEqualities = new HashSet<>();
        Set<List<NodeTerm>> bindingEqualities = new HashSet<>();
        for (Set<NodeTerm> connectedComponent:
                (new ConnectivityInspector<>(eqGraph)).connectedSets()) {
            List<NodeTerm> ccuqNodeTerms = connectedComponent.stream()
                    .filter(uqNodeTerms::contains)
                    .collect(Collectors.toList());
            List<NodeTerm> bodyNodeTerms = connectedComponent.stream()
                    .filter(Predicate.not(uqNodeTerms::contains))
                    .collect(Collectors.toList());

            // All the NodeTerms the body of the mapping + all the non-var nodes from the uq
            // need to be equal for one connected component
            List<NodeTerm> equalNodeTerms = new ArrayList<>(bodyNodeTerms);
            for (NodeTerm nodeTerm: ccuqNodeTerms)
                if (nodeTerm.isVariable())
                    bindingEqualities.add(Arrays.asList(nodeTerm, bodyNodeTerms.get(0)));
                else
                    equalNodeTerms.add(nodeTerm);

            for (int i = 1; i < equalNodeTerms.size(); i++) // only filter if uq variable acts as intermediary
                filterEqualities.add(Arrays.asList(equalNodeTerms.get(i - 1), equalNodeTerms.get(i)));
        }

        return Arrays.asList(filterEqualities, bindingEqualities);
    }

    private ExprList createFilterEqualities(Set<List<NodeTerm>> equalities) {
        ExprList exprList = new ExprList();
        for (List<NodeTerm> eq : equalities)
            exprList.add(new E_Equals(
                    eq.get(0).asJenaExpr(),
                    eq.get(1).asJenaExpr()
            ));
        return exprList;
    }

    private Op createProjectionRenaming(Set<List<NodeTerm>> bindings, Op op) {
        // the "AS" filter
        // convention: the first element of List<NodeTerm> is a user query NodeTerm and a variable
        //             the second element is whatever
        VarExprList varExprList = new VarExprList();
        for (List<NodeTerm> binding: bindings)
            if (binding.get(0).isVariable())
                varExprList.add(
                        (Var) binding.get(0).asNode(),
                        binding.get(1).asJenaExpr()
                );

        return OpExtend.create(op, varExprList);
    }

    private boolean satisfiableMatching(Set<List<NodeTerm>> equalities) {
        // test with prolog if the equalities can hold
        org.jpl7.Query goal = PrologUtils.equalityQueryFrom(equalities);
        Map<String, Term> solution = goal.oneSolution();
        return solution != null;
    }

    private boolean differentName(Node node1, Node node2) {
        if (!(node1.isVariable() && node2.isVariable()))
            return true;
        return node1.getName().equals(node2.getName());
    }

    private Op applyRenaming(Map<String, String> renaming, Op op) {
        return Transformer.transform(new VariableRenamer(renaming), op);
    }


    private Map<String, String> renameVariablesFresh(UnrolledTriplesMap tmap) {
        Map<String, String> renaming = new HashMap<>();
        for (Var var: tmap.getAllVariables())
            renaming.put(var.getVarName(), generateFreshVariable());
        return renaming;
    }

    private String generateFreshVariable() {
        return "rvar".concat(String.valueOf(this.variableCounter++));
    }

    private List<Map<Triple, UnrolledTriplesMap>> generateMappingCandidates(BasicPattern bp) {
        List<UnrolledTriplesMap> unrolledTriplesMaps = new ArrayList<>();
        for (Source source : this.sources)
            for (TriplesMap triplesMap : source.getTriplesMaps())
                for (PredicateObjectMap predicateObjectMap : triplesMap.getPredicateObjectMaps())
                    unrolledTriplesMaps.add(new UnrolledTriplesMap(
                            triplesMap.getSubject(),
                            predicateObjectMap,
                            source)
                    );

        List<Map<Triple, UnrolledTriplesMap>> candidates = new ArrayList<>();
        for (List<Integer> assignmentVector: generateAssignmentVectors(bp.size(), unrolledTriplesMaps.size()-1)) // minus one because the maxval will function as an index
            candidates.add(createMappingCandidate(bp.getList(), unrolledTriplesMaps, assignmentVector));

        return candidates;
    }

    private Map<Triple, UnrolledTriplesMap> createMappingCandidate(List<Triple> triples, List<UnrolledTriplesMap> maps, List<Integer> vector) {
        Map<Triple, UnrolledTriplesMap> candidate = new HashMap<>();
        for (int i = 0; i < triples.size(); i++)
            candidate.put(triples.get(i), maps.get(vector.get(i)));
        return candidate;
    }

    private Set<List<Integer>> generateAssignmentVectors(Integer length, Integer maxVal) {
        // Just a generalized counter: generates maxVal^length amount of vectors counting
        // from the zero vector to the maxVal vector

        Set<List<Integer>> vectors = new HashSet<>();

        List<Integer> assignmentVector = new ArrayList<>();
        for (int i = 0; i < length; i++)
            assignmentVector.add(0);

        vectors.add(assignmentVector);
        for (int i = 0; i < Math.pow(maxVal,length); i++)
            vectors.add(addOneAV(maxVal, assignmentVector));

        return vectors;
    }

    private List<Integer> addOneAV(Integer maxVal, List<Integer> vector) {
        List<Integer> ret = new ArrayList<>(vector);
        for (int index = 0; index < ret.size(); index++)
            if (index < maxVal) {
                ret.set(index, ret.get(index) + 1);
                break;
            }
        return ret;
    }
}
