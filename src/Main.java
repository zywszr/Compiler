
import FrontEnd.*;

//import org.antlr.runtime.ANTLRInputStream;

import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;
//import org.antlr.v4.runtime.CommonTokenStream;

import java.io.*;

public class Main {
    public static void main (String[] args) throws IOException, Exception {
    //   InputStream in = new FileInputStream("test.txt");
        ANTLRInputStream input = new ANTLRInputStream(System.in);
        MxStarLexer lexer = new MxStarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MxStarParser parser = new MxStarParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());

        ParseTree tree = parser.program();
/*
        System.out.println("LISP:");
        System.out.println(tree.toStringTree(parser));
        System.out.println();
*/

        ASTBuilder buildAST = new ASTBuilder();

        //System.out.println("what");
        Node ASTroot = buildAST.visit(tree);

        ScopeBuilder BuildScope = new ScopeBuilder();

        BuildScope.programScopeBuild(ASTroot);
        //System.out.println("what");
        SemanticChecker checker = new SemanticChecker(BuildScope.rootScope);
        try {
            ASTroot.accept(checker);
        } catch (SyntaxError error) {
            System.out.println(error.toString() + " on Line: " + error.pos.line + ",  Column: " + error.pos.column);
            throw error;
        }
    }
}
