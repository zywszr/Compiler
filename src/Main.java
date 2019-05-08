

import BackEnd.*;
import FrontEnd.*;

import IRClass.LineIR;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;

import java.io.*;

public class Main {
    public static void main (String[] args) throws IOException, Exception {
        // InputStream in = new FileInputStream("test.txt");
        ANTLRInputStream input = new ANTLRInputStream(System.in);
        MxStarLexer lexer = new MxStarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MxStarParser parser = new MxStarParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());

        ParseTree tree = parser.program();

        ASTBuilder buildAST = new ASTBuilder();

        Node ASTroot = buildAST.visit(tree);

        ScopeBuilder BuildScope = new ScopeBuilder();

        BuildScope.programScopeBuild(ASTroot);

        SemanticChecker checker = new SemanticChecker(BuildScope.rootScope);
        try {
            ASTroot.accept(checker);
        } catch (SyntaxError error) {
            System.out.println(error.toString() + " on Line: " + error.pos.line + ",  Column: " + error.pos.column);
            throw error;
        }

        RegisterSet.init();

        IRBuilder buildIR = new IRBuilder();
        LineIR lineIR = buildIR.buildLineIR(ASTroot);
        // lineIR.print();

        IRCorrector correctIR = new IRCorrector(lineIR, buildIR.getTmpVarIdx());
        correctIR.work();

        // lineIR.printCode();

        RegisterAllocater allocateReg = new RegisterAllocater(lineIR, correctIR.getTmpVarIdx());
        allocateReg.work();

        CodeGen genCode = new CodeGen(lineIR);
        genCode.work();

    /*    PrintStream psOld = System.out;
        System.setOut(new PrintStream(new File("test_lyc.asm")));
     */   genCode.print();
        //   System.setOut(psOld);
    }
}