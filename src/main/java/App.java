import RML.Parser;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;

public class App {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Error: too few arguments");
            System.exit(1);
        }

        String userqueryInput = args[2];
        String mappingfilename = args[1];

        Parser parser = new Parser(mappingfilename);
        Op userquery = Algebra.compile(QueryFactory.create(userqueryInput));

        Op rewrittenQuery = Transformer.transform(new Rewriter(parser.getSourceSet()), userquery);

        System.out.println(rewrittenQuery);
    }
}
