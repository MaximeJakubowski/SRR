import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.*;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VariableRenamer extends TransformCopy {
    private final Map<String, String> renaming;

    public VariableRenamer(Map<String, String> renaming) {
        this.renaming = renaming;
    }

    @Override
    public Op transform(OpBGP opBGP) {
        return new OpBGP(renameVars(opBGP.getPattern()));
    }

    @Override
    public Op transform(OpTriple opTriple) {
        return new OpTriple(renameVars(opTriple.getTriple()));
    }

    @Override
    public Op transform(OpService opService, Op subOp) {
        // only first constructor (no ElementService)
        return new OpService(
                renameVars(opService.getService()),
                subOp,
                opService.getSilent()
        );
    }

    @Override
    public Op transform(OpQuad opQuad) {
        return new OpQuad(renameVars(opQuad.getQuad()));
    }

    @Override
    public Op transform(OpQuadPattern opQuadPattern) {
        return new OpQuadPattern(
                renameVars(opQuadPattern.getGraphNode()),
                renameVars(opQuadPattern.getBasicPattern())
        );
    }

    @Override
    public Op transform(OpQuadBlock opQuadBlock) {
        return new OpQuadBlock(renameVars(opQuadBlock.getPattern()));
    }

    @Override
    public Op transform(OpTable opTable) {
        return OpTable.create(renameVars(opTable.getTable()));
    }

    @Override
    public Op transform(OpFilter opFilter, Op subOp) {
        return OpFilter.filterAlways(renameVars(opFilter.getExprs()), subOp);
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        return new OpProject(subOp, renameVars(opProject.getVars()));
    }

    @Override
    public Op transform(OpExtend opExtend, Op subOp) {
        return OpExtend.extend(subOp, renameVars(opExtend.getVarExprList()));
    }

    @Override
    public Op transform(OpGroup opGroup, Op subOp) {
        List<Var> renamedVarExpr = renameVars(opGroup.getGroupVars().getVars());

        List<ExprAggregator> exprAggs = new ArrayList<>();
        for (ExprAggregator ea: opGroup.getAggregators())
            exprAggs.add(renameVars(ea));

        return new OpGroup(subOp, new VarExprList(renamedVarExpr), exprAggs);
    }

    @Override
    public Op transform(OpOrder opOrder, Op subOp) {
        List<SortCondition> sortConditions = new ArrayList<>();
        for (SortCondition sc: opOrder.getConditions())
            sortConditions.add(new SortCondition(
                    sc.getExpression().copySubstitute(asBinding()),
                    sc.getDirection()));
        return new OpOrder(subOp, sortConditions);
    }

    @Override
    public Op transform(OpGraph opGraph, Op subOp) {
        return new OpGraph(renameVars(opGraph.getNode()), subOp);
    }

    @Override
    public Op transform(OpAssign opAssign, Op subOp) {
        return OpAssign.create(subOp, renameVars(opAssign.getVarExprList()));
    }

    private ExprAggregator renameVars(ExprAggregator agg) {
        return new ExprAggregator(
                renameVars(agg.getVar()),
                agg.getAggregator().copy(renameVars(agg.getAggregator().getExprList()))
        );
    }

    private BasicPattern renameVars(BasicPattern bp) {
        BasicPattern basicPattern = new BasicPattern();
        for (Triple tp : bp)
            basicPattern.add(renameVars(tp));
        return basicPattern;
    }

    private Binding renameVars(Binding b) {
        BindingBuilder bbuild = Binding.builder();
        b.forEach((v,n) -> bbuild.add(renameVars(v), renameVars(n)));
        return bbuild.build();
    }

    private Table renameVars(Table table) {
        Table ret = TableFactory.create();
        while (table.rows().hasNext())
            ret.addBinding(renameVars(table.rows().next()));
        return ret;
    }

    private Var renameVars(Var v) {
        return this.renaming.containsKey(v.getVarName()) ?
                Var.alloc(this.renaming.get(v.getVarName())) :
                v;
    }

    private Node renameVars(Node n) {
        return n.isVariable() && this.renaming.containsKey(n.getName()) ?
                Var.alloc(this.renaming.get(n.getName())) :
                n;
    }

    private Quad renameVars(Quad qd) {
        return new Quad(
                renameVars(qd.getGraph()),
                renameVars(qd.getSubject()),
                renameVars(qd.getPredicate()),
                renameVars(qd.getObject())
        );
    }

    private QuadPattern renameVars(QuadPattern qp) {
        QuadPattern ret = new QuadPattern();
        for (Quad q: qp.getList())
            ret.add(renameVars(q));
        return ret;
    }
    
    private Triple renameVars(Triple tp) {
        return new Triple(
                renameVars(tp.getSubject()),
                renameVars(tp.getPredicate()),
                renameVars(tp.getObject())
        );
    }

    private List<Var> renameVars(List<Var> variables) {
        List<Var> renamedVars = new ArrayList<>();
        for (Var var: variables)
            renamedVars.add(renameVars(var));
        return renamedVars;
    }

    private ExprList renameVars(ExprList exprlist) {
        List<Expr> ret = new ArrayList<>();
        for (Expr e: exprlist)
            ret.add(e.copySubstitute(asBinding()));
        return new ExprList(ret);
    }

    private VarExprList renameVars(VarExprList exprlist) {
        VarExprList ret = new VarExprList();
        for (Var var: exprlist.getExprs().keySet())
            ret.add(renameVars(var),
                    exprlist.getExpr(var).copySubstitute(asBinding()));
        return ret;
    }

    private Binding asBinding() {
        BindingBuilder bbuild = Binding.builder();
        for (String key: this.renaming.keySet())
            bbuild.add(Var.alloc(key), Var.alloc(this.renaming.get(key)));
        return bbuild.build();
    }
}
