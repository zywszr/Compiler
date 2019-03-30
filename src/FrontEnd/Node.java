
package FrontEnd;

import ScopeClass.Scope;
import TypeDefition.TypeDef;

import java.util.List;
import java.util.ArrayList;

public abstract class Node {
    String id;
    TypeDef type;
    String inClass;
    PositionDef pos;
    List<Node> childs;
    Scope<TypeDef> belong;
    Node() {
        id = "";
        inClass = "";
        type = TypeDef.build("void");
        pos  = null;
        childs = new ArrayList<Node>();
        belong = null;
    }
    public void accept(ASTVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}

class ProgramNode extends Node {
    ProgramNode() { super(); }
}

class ClassDefNode extends Node {
    ClassDefNode() { super(); }
}
/*
class NoAssignVarDecNode extends Node {
    NoAssignVarDecNode() { super(); }
}
*/
class ConstructFuncNode extends Node {
    ConstructFuncNode() { super(); }
}

class FunctionDefNode extends Node {
    FunctionDefNode() { super(); }
}
/*
class ParaDecNode extends Node {
    ParaDecNode() { super(); }
}
*/
abstract class StateNode extends Node {
    StateNode() { super(); }
}

class VarDefStateNode extends StateNode {
    VarDefStateNode() { super(); }
}

class VarDefNode extends Node {
    VarDefNode() { super(); }
}

class BlockStateNode extends StateNode {
    BlockStateNode() { super(); }
}

class ExprStateNode extends StateNode {
    ExprStateNode() { super(); }
}

class CondStateNode extends StateNode {
    CondStateNode() { super(); }
}

class WhileStateNode extends StateNode {
    WhileStateNode() { super(); }
}

class ForStateNode extends StateNode {
    ForStateNode() { super(); }
}

class ReturnStateNode extends StateNode {
    ReturnStateNode() { super(); }
}

class BreakStateNode extends StateNode {
    BreakStateNode() { super(); }
}

class ContinStateNode extends StateNode {
    ContinStateNode() { super(); }
}

class NullStateNode extends StateNode {
    NullStateNode() { super(); }
}

abstract class ExprNode extends Node {
    ExprNode() { super(); }
}

class BinExprNode extends ExprNode {
    BinExprNode() { super(); }
}

class EmptyExprNode extends ExprNode {
    EmptyExprNode() { super(); }
}

abstract class UnaryExprNode extends ExprNode {
    UnaryExprNode() { super(); }
}

class LUnaryExprNode extends UnaryExprNode {
    LUnaryExprNode() { super(); }
}

class RUnaryExprNode extends UnaryExprNode {
    RUnaryExprNode() { super(); }
}

class NewVarNode extends ExprNode {
    NewVarNode() { super(); }
}

class PriArrExprNode extends ExprNode {
    PriArrExprNode() { super(); }
}

class PriPntExprNode extends ExprNode {
    PriPntExprNode() { super(); }
}
/*
class ElementExprNode extends ExprNode {
    ElementExprNode() { super(); }
}
*/
class VarEleExprNode extends ExprNode {
    VarEleExprNode() { super(); }
}

class FunEleExprNode extends ExprNode {
    FunEleExprNode() { super(); }
}

abstract class LiteralNode extends ExprNode {
    LiteralNode() { super(); }
}

class LogLitNode extends LiteralNode {
    LogLitNode() { super(); }
}

class IntLitNode extends LiteralNode {
    IntLitNode() { super(); }
}

class StrLitNode extends LiteralNode {
    StrLitNode() { super(); }
}

class NullLitNode extends LiteralNode {
    NullLitNode() { super(); }
}