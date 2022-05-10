import RML.*;
import model.*;
import org.apache.jena.atlas.lib.NotImplemented;
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
import model.NodeTermPair;
import org.jpl7.Query;
import org.jpl7.Term;

import java.util.*;

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
        Set<NodeTermPair> bindings = new HashSet<>();
        Map<Triple, Set<NodeTermPair>> equalities = new HashMap<>();
        Map<Triple, Map<String, String>> renamings = new HashMap<>();

        for (Triple tp : candidate.keySet()) {
            Map<String, String> renaming = renameVariablesFresh(candidate.get(tp));
            renamings.put(tp, renaming); // remember renaming for every triple

            Set<NodeTermPair> equality = new HashSet<>();

            NodeTermPair subjectPair = new NodeTermPair(tp.getSubject(),
                    applyRenaming(renaming, new FlatPTerm(candidate.get(tp).getSubjectMap())));
            equality.add(subjectPair);
            if (tp.getSubject().isVariable())
                bindings.add(subjectPair);

            NodeTermPair predicatePair = new NodeTermPair(tp.getPredicate(),
                    applyRenaming(renaming, new FlatPTerm(candidate.get(tp)
                            .getPredicateObjectMap()
                            .getPredicate())));
            equality.add(predicatePair);
            if (tp.getPredicate().isVariable() && differentName(tp.getPredicate(), tp.getSubject()))
                bindings.add(predicatePair);

            NodeTermPair objectPair = new NodeTermPair(tp.getObject(),
                    applyRenaming(renaming, new FlatPTerm(candidate.get(tp)
                            .getPredicateObjectMap()
                            .getObject())));
            equality.add(objectPair);
            if (tp.getObject().isVariable() &&
                    differentName(tp.getObject(), tp.getSubject()) &&
                    differentName(tp.getObject(), tp.getPredicate()))
                bindings.add(objectPair);

            equalities.put(tp, equality);
        }
        if (!satisfiableMatching(unionOf(equalities.values())))
            return AlgebraUtils.emptyQuery();

        return buildQuery(equalities, bindings, candidate, renamings);
    }

    private Set<NodeTermPair> unionOf(Collection<Set<NodeTermPair>> pairsets) {
        Set<NodeTermPair> npairs = new HashSet<>();
        for (Set<NodeTermPair> pairs: pairsets)
            npairs.addAll(pairs);
        return npairs;
    }

    private Op buildQuery(Map<Triple, Set<NodeTermPair>> equalities, Set<NodeTermPair> bindings,
                          Map<Triple, UnrolledTriplesMap> match, Map<Triple, Map<String, String>> renamings) {

        Set<Op> tojoin = new HashSet<>();
        for (Triple tp : match.keySet())
            tojoin.add(
                    OpFilter.filterBy(createFilterEqualities(equalities.get(tp)),
                            applyRenaming(
                                    renamings.get(tp),
                                    ((SPARQLSource) match.get(tp).getSource()).getQuery().getQueryOp()
                            )
                    )
            );

        Op rewrittenBGP = AlgebraUtils.makeJoin(tojoin);
        // if no vars, select *; otherwise select vars(renaming)
        if (renamings.values().stream().allMatch(renaming -> renaming.keySet().isEmpty())) //there were no variables
            return rewrittenBGP;
        return createProjectionRenaming(bindings, rewrittenBGP);
    }

    private ExprList createFilterEqualities(Set<NodeTermPair> equalities) {
        // the filter equalities. they should be "optimized", i.e., in their most simple form (prolog)
        // TODO: this is too naive. sometimes there is an eqiality of the form "?x = ?rvar1, ?x = ?rvar2"
        //  In that case, the filter "?rvar1 = ?rvar2" must be added. At the moment it adds nothing
        ExprList exprList = new ExprList();
        for (NodeTermPair npair : equalities)
            if (! npair.getNode().isVariable() )
                exprList.add(new E_Equals(
                        nodeAsExpr(npair.getNode()),
                        npair.getTerm().flatPTermAsExpr())
                );
        return exprList;
    }

    private Expr nodeAsExpr(Node node) {
        // This node can be a variable, or an RDF term
        // output corresponding Expr object

        if (node.isVariable())
            return new ExprVar(Var.alloc(node));

        if (node.isBlank())
            throw new NotImplemented("no support for blank nodes");

        // node is URI or literal
        return NodeValue.makeNode(node);
    }

    private Op createProjectionRenaming(Set<NodeTermPair> bindings, Op op) {
        // the "AS" filter
        // the node in these nodetermpair must be variables
        VarExprList varExprList = new VarExprList();
        for (NodeTermPair npair : bindings) {
            Var targetvar = Var.alloc(npair.getNode());
            if (!varExprList.contains(targetvar)) // multiple equalities can occur (in join) but must not be renamings
                varExprList.add(targetvar, npair.getTerm().flatPTermAsExpr());
        }
        return OpExtend.create(op, varExprList);
    }

    private Set<String> getTripleVariables(Triple tp) {
        Set<String> vars = new HashSet<>();
        if (tp.getSubject().isVariable())
            vars.add(tp.getSubject().getName());
        if (tp.getPredicate().isVariable())
            vars.add(tp.getPredicate().getName());
        if (tp.getObject().isVariable())
            vars.add(tp.getObject().getName());
        return vars;
    }

    private Map<String, String> restrictRenaming(Map<String, String> renaming,
                                                 Set<String> restriction) {
        Map<String, String> restrictedRenaming = new HashMap<>();
        for (String nvar : renaming.keySet())
            if (restriction.contains(nvar))
                restrictedRenaming.put(nvar, renaming.get(nvar));
        return restrictedRenaming;
    }

    private boolean satisfiableMatching(Set<NodeTermPair> equalities) {
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

    private Set<NodeTermPair> optimizeEqualities(Set<NodeTermPair> equalities) {
        // BEWARE: This function assumes that the equalities !!are satisfiable!!
        // inject transitive closure program 'tc/2'
        Query program = new Query("""
                assert(eq(A,B) :- eq(B,A)).
                assert(tc(A,B) :- eq(A,B)),
                assert(tc(A,B) :- ','(eq(A,C), eq(C,B))).
                """
        );
        program.hasSolution();

        // inject equality facts
        int symbol = 0;
        Map<Node, Integer> nodeSymbols = new HashMap<>();
        Map<FlatPTerm, Integer> ptermSymbols = new HashMap<>();

        for (NodeTermPair npair: equalities) {
            if (! nodeSymbols.containsKey(npair.getNode()))
                nodeSymbols.put(npair.getNode(), symbol++);
            if (! ptermSymbols.containsKey(npair.getTerm()))
                ptermSymbols.put(npair.getTerm(), symbol++);
        }

        Set<List<Integer>> encodedEqualities = new HashSet<>();
        for (NodeTermPair npair: equalities)
            encodedEqualities.add(
                    Arrays.asList(
                            nodeSymbols.get(npair.getNode()),
                            ptermSymbols.get(npair.getTerm()))
            );

        for (List<Integer> inteq: encodedEqualities) {
            Query assertfact = new Query(String.format("assert(eq(%s,%s))", inteq.get(0), inteq.get(1)));
            assertfact.hasSolution();
        }

        // retrieve transitive closure of equalities

    }

    private Op applyRenaming(Map<String, String> renaming, Op op) {
        return Transformer.transform(new VariableRenamer(renaming), op);
    }

    public FlatPTerm applyRenaming(Map<String, String> renaming, FlatPTerm pterm) {
        // rename all according to renaming. If var not in dom(renaming), keep var
        List<Object> renamedStructure = new ArrayList<>();

        for (Object obj : pterm.getStructure())
            if (obj instanceof Var && renaming.containsKey(((Var) obj).getVarName()))
                renamedStructure.add(Var.alloc(renaming.get(((Var) obj).getVarName())));
            else
                renamedStructure.add(obj);

        return new FlatPTerm(renamedStructure, pterm.isIRI(), pterm.isLiteral(), pterm.isBlank());
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
