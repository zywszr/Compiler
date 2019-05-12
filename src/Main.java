

import BackEnd.*;
import FrontEnd.*;

import IRClass.LineIR;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;

import java.io.*;
import java.util.HashMap;

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

        HashMap <String, Node> funcNode = new HashMap<>();
        SemanticChecker checker = new SemanticChecker(BuildScope.rootScope, funcNode);
        try {
            ASTroot.accept(checker);
        } catch (SyntaxError error) {
            System.out.println(error.toString() + " on Line: " + error.pos.line + ",  Column: " + error.pos.column);
            throw error;
        }

        RegisterSet.init();

        IRBuilder buildIR = new IRBuilder(BuildScope.rootScope, funcNode);
        LineIR lineIR = buildIR.buildLineIR(ASTroot);
        //lineIR.print();

        GlobalSolver solveGlobal = new GlobalSolver(lineIR);
        solveGlobal.work();

        IRCorrector correctIR = new IRCorrector(lineIR, buildIR.getTmpVarIdx());
        correctIR.work();

        // lineIR.printCode();

        DeadCode deadCode = new DeadCode(lineIR);
        deadCode.work_before_allocate();

        RegisterAllocater allocateReg = new RegisterAllocater(lineIR, correctIR.getTmpVarIdx());
        allocateReg.work();

        deadCode.work_after_allocate();

        CodeGen genCode = new CodeGen(lineIR);
        genCode.work();

        //PrintStream psOld = System.out;
        //System.setOut(new PrintStream(new File("../test_lyc.asm")));
        genCode.print();
        //System.setOut(psOld);
    }
}