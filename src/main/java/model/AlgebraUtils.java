package model;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AlgebraUtils {
    //makes nested join of >=1 operators
    public static Op makeJoin(Collection<Op> operators) {
        int count = 0;
        Op joinedOp = null;
        for (Op operator : operators) {
            if (count == 0)
                joinedOp = operator;
            else
                joinedOp = OpJoin.create(joinedOp, operator);
            count++;
        }
        return joinedOp;
    }

    //makes nested union of >=1 operators
    public static Op makeUnion(Collection<Op> operators) {
        if (operators.isEmpty()) {
            return AlgebraUtils.emptyQuery();
        }
        int count = 0;
        Op unionedOp = null;
        for (Op operator : operators) {
            if (count == 0)
                unionedOp = operator;
            else
                unionedOp = OpUnion.create(unionedOp, operator);
            count++;
        }
        return unionedOp;
    }

    public static Op emptyQuery() {
        return OpFilter.filterBy(new ExprList(new NodeValueBoolean(false)), OpTable.unit());
    }

    public static boolean isJenaVariable(Node n) {
        return n.isVariable() && !isJenaBlank(n);
    }

    public static boolean isJenaBlank(Node n) {
        // .isBlank() is bronken in jena ARQ. Blank nodes in a SPARQL query are considered variables
        if (!n.isVariable())
            return false;
        return n.getName().startsWith("?");
    }
}
