package model;

import RML.RMLReference;
import RML.TermMap;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.atlas.test.Gen;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
* Represents a Node or a Term.
* Terms are often nodes,
* sometimes terms are not nodes (only in the case of a template).
* This class interfaces Terms and Nodes
* */
public class NodeTerm {
    private final List<Object> structure;
    private final Node node;

    private enum GenType {
        URI,
        LITERAL,
        BLANK,
        ANY
    }
    private final GenType type;

    private NodeTerm(List<Object> structure, boolean iri, boolean blank, boolean lit) {
        List<Object> newStructure = new ArrayList<>();
        // replace RMLReference with Var
        for (Object obj: structure)
            if (obj instanceof RMLReference)
                newStructure.add(Var.alloc(((RMLReference) obj).getReference()));
            else
                newStructure.add(obj);

        this.structure = new ArrayList<>(newStructure);
        this.node = null;

        if (iri) { this.type = GenType.URI; }
        else if (blank) { this.type = GenType.BLANK; }
        else if (lit) { this.type = GenType.LITERAL; }
        else { this.type = null; }
    }

    private NodeTerm(Node node) {
        this.node = node;
        this.structure = null;

        if (node.isVariable())
            this.type = GenType.ANY;
        else if (node.isURI())
            this.type = GenType.URI;
        else if (node.isLiteral())
            this.type = GenType.LITERAL;
        else
            this.type = GenType.BLANK;
    }

    public static NodeTerm create(TermMap termMap) {
        return new NodeTerm(
                termMap.getGenerator().getStructure(),
                termMap.isIri(), termMap.isBlank(), termMap.isLiteral()
        );
    }

    public static NodeTerm create(Node node) {
        return new NodeTerm(node);
    }

    public boolean isIRI() { return this.type == GenType.URI || this.type == GenType.ANY; }

    public boolean isLiteral() { return this.type == GenType.LITERAL || this.type == GenType.ANY; }

    public boolean isBlank() { return this.type == GenType.BLANK || this.type == GenType.ANY; }

    public boolean isAny() { return this.type == GenType.ANY; }

    public boolean isConcrete() {
        return (this.node != null && this.node.isConcrete()) ||
                (this.structure != null && this.structure.stream()
                        .noneMatch(obj -> obj instanceof Var)); // depends heavily on lazy evaluation
    }

    public boolean isVariable() {
        return (this.node != null && this.node.isVariable()) ||
                (this.structure != null &&
                        this.structure.size() == 1 &&
                        this.structure.get(0) instanceof Var); // depends heavily on lazy evaluation
    }

    public boolean isNode() {
        return isConcrete() || isVariable();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodeTerm) {
            NodeTerm other = (NodeTerm) obj;
            if (this.isVariable() && other.isVariable())
                return this.asNode().getName().equals(other.asNode().getName());
            if (this.isNode() && other.isNode())
                return this.asNode().equals(other.asNode());
            if (!this.isNode() && !other.isNode())
                return this.structureEquals(other);
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        if (this.isVariable())
            return this.asNode().getName().hashCode();
        return super.hashCode();
    }

    protected boolean structureEquals(NodeTerm other) {
        if (this.structure.size() != other.structure.size())
            return false;

        for (int i = 0; i < this.structure.size(); i++)
            if (! this.structure.get(i).equals(other.structure.get(i)))
                return false;

        return true;
    }

    /*
    In true Apache Jena fashion:
    if not a node, die horribly
     */
    public Node asNode() {
        return this.node != null ? this.node: structureAsNode();
    }


    private Node structureAsNode() {
        if (isVariable())
            return (Var) this.structure.get(0);
        if (isBlank())
            return NodeFactory.createBlankNode(new BlankNodeId(structureAsString()));
        if (isIRI())
            return NodeFactory.createURI(structureAsString());
        // it must be a literal
        return NodeFactory.createLiteral(structureAsString());
    }

    private String structureAsString() {
        assert isConcrete(); // otherwise die horribly

        String coll = "";
        for (Object obj: this.structure)
            coll = coll.concat((String) obj);

        return coll;
    }

    public NodeTerm renamed(Map<String, String> renaming) {
        if (this.node != null)
            if (this.node.isVariable() && renaming.containsKey(this.node.getName()))
                return new NodeTerm(Var.alloc(renaming.get(this.node.getName())));
            else
                return this;

        List<Object> newStructure = new ArrayList<>();
        for (Object obj: this.structure)
            if (obj instanceof Var && renaming.containsKey(((Var) obj).getVarName()))
                newStructure.add(Var.alloc(renaming.get(((Var) obj).getVarName())));
            else
                newStructure.add(obj);
        return new NodeTerm(newStructure, this.isIRI(), this.isBlank(), this.isLiteral());
    }

    public Term asPrologTerm() {
        if (this.node != null)
            return nodeToTerm();
        return structureToTerm();
    }

    public Expr asJenaExpr() {
        // If we can consider this a node
        if (this.isNode()) {
            Node node = this.asNode();
            if (node.isVariable())
                return new ExprVar(Var.alloc(node));

            if (node.isBlank())
                throw new NotImplemented("no support for blank nodes");

            // node is URI or literal
            return NodeValue.makeNode(node);
        }

        // This is a template (this.structure != null and cannot be converted  to a node)
        ExprList exprList = new ExprList();
        for (Object obj: this.structure)
            if (obj instanceof String)
                exprList.add(new NodeValueString((String) obj));
            else
                exprList.add(new ExprVar((Var) obj));

        E_StrConcat concat = new E_StrConcat(exprList);
        if (this.type == GenType.URI)
            return new E_IRI(concat);
        return concat;
    }

    private Term structureToTerm() {
        String type = "unknown";
        if (this.isBlank())
            type = "blank";
        if (this.isLiteral())
            type = "literal";
        if (this.isIRI())
            type = "iri";

        // TODO for now, only naive templates (ending on variable, only one variable)
        List<Term> termList = new ArrayList<>();
        for (Object obj : this.structure)
            if (obj instanceof String)
                for (char c: ((String) obj).toCharArray())
                    termList.add(new Atom(String.valueOf(c)));
            else if (obj instanceof Var)
                return new Compound(type, new Term[] {
                        PrologUtils.listtermToDifferenceList(
                                Term.termArrayToList(termList.toArray(new Term[0])),
                                new Variable(((Var) obj).getVarName()))
                });

        return new Compound(type, new Term[] {  Term.termArrayToList(termList.toArray(new Term[0])) });
    }

    private Term nodeToTerm() {
        // TODO check if this is a good enough approach: do we ever get in trouble?
        // especially relating to the handing of the case of a variable node
        if (this.node.isVariable())
            return new Variable(node.getName()); // important: node.getName() != node.toString()

        String type = "unknown";
        if (this.node.isBlank())
            type = "blank";
        if (this.node.isLiteral())
            return new Compound("literal", new Term[]{ PrologUtils.stringAsCharTermList(this.node.getLiteral().toString()) });
        if (this.node.isURI())
            type = "iri";
        return new Compound(type, new Term[]{ PrologUtils.stringAsCharTermList(this.node.toString()) });
        // TODO proper handling of unknown
    }
}
