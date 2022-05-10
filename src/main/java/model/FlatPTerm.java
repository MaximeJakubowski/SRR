package model;

import RML.RMLReference;
import RML.TermMap;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

import java.util.ArrayList;
import java.util.List;

public class FlatPTerm {
    private List<Object> structure = new ArrayList<>();
    private boolean isIRI = false;
    private boolean isLiteral = false;
    private boolean isBlank = false;

    public FlatPTerm(TermMap tm) {
        List<Object> structure = tm.getGenerator().getStructure();
        for (Object obj: structure)
            if (obj instanceof String)
                this.structure.add(obj);
            else
                this.structure.add(Var.alloc(((RMLReference) obj).getReference()));

        if (tm.isBlank())
            this.isBlank = true;
        if (tm.isLiteral())
            this.isLiteral = true;
        if (tm.isIri())
            this.isIRI = true;
    }

    public boolean isIRI() { return this.isIRI; }
    public boolean isLiteral() { return this.isLiteral; }
    public boolean isBlank() { return this.isBlank; }

    public FlatPTerm(List<Object> structure, boolean iri, boolean lit, boolean blank) {
        assert iri ^ lit ^ blank;
        this.isIRI = iri;
        this.isLiteral = lit;
        this.isBlank = blank;
        this.structure = structure;
    }

    public List<Object> getStructure() {
        return structure;
    }

    public boolean isVariable() {
        return this.structure.size() == 1 && this.structure.get(0) instanceof Var;
    }

    public ExprList asExprList() {
        ExprList exprList = new ExprList();
        for (Object obj: this.structure)
            if (obj instanceof String)
                exprList.add(new NodeValueString((String) obj));
            else
                exprList.add(new ExprVar((Var) obj));
        return exprList;
    }

    public Expr flatPTermAsExpr() {
        if (isVariable())
            return asExprList().get(0);
        E_StrConcat concat = new E_StrConcat(asExprList());
        if (this.isIRI)
            return new E_IRI(concat);
        return concat;
    }
}
