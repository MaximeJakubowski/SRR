package model;

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

    public static List<Op> flattenJoin(Op op) {
        List<Op> flattenedJoin = new ArrayList<>();
        flattenJoin_recursive(op, flattenedJoin);
        return flattenedJoin;
    }

    private static void flattenJoin_recursive(Op op, List<Op> flattenedJoin) {
        if (!(op instanceof OpJoin))
            flattenedJoin.add(op);
        else {
            flattenJoin_recursive(((OpJoin) op).getLeft(), flattenedJoin);
            flattenJoin_recursive(((OpJoin) op).getRight(), flattenedJoin);
        }
    }

    public static List<Op> flattenUnion(Op op) {
        List<Op> flattenedUnion = new ArrayList<>();
        flattenUnion_recursive(op, flattenedUnion);
        return flattenedUnion;
    }

    private static void flattenUnion_recursive(Op op, List<Op> flattenedUnion) {
        if (!(op instanceof OpUnion))
            flattenedUnion.add(op);
        else {
            flattenUnion_recursive(((OpUnion) op).getLeft(), flattenedUnion);
            flattenUnion_recursive(((OpUnion) op).getRight(), flattenedUnion);
        }
    }

    public static Op emptyQuery() {
        return OpFilter.filterBy(new ExprList(new NodeValueBoolean(false)), OpTable.unit());
    }

    public static Op createBGP(Triple tp) {
        BasicPattern bp = new BasicPattern();
        bp.add(tp);
        return new OpBGP(bp);
    }
}
