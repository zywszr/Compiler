
package FrontEnd;

import OprandClass.Oprand;
import ScopeClass.Scope;
import TypeDefition.TypeDef;

import java.util.List;
import java.util.ArrayList;

public abstract class Node {
    String id;
    String reName;
    TypeDef type;
    String inClass; // any use?
    String strLiter;
    Oprand reg;
    PositionDef pos;
    List<Node> childs;
    Scope<TypeDef> belong;
    boolean unique;
    boolean willUse;
    boolean leftVal;
    Node() {
        id = "";
        reName = "";
        type = TypeDef.build("void");
        inClass = "";
        strLiter = "";
        reg = null;
        pos  = null;
        childs = new ArrayList<>();
        belong = null;
        unique = false;
        willUse = true;
        leftVal = false;
    }
    public void accept(ASTVisitor visitor) throws Exception {
        visitor.visit(this);
    }
    public void setUnique() {
        unique = true;
    }
    public boolean isUnique() {
        return unique;
    }
    public void setNotUse() {
        willUse = false;
    }
    public boolean isWillUse() {
        return willUse;
    }
    public void setLeftVal() {
        leftVal = true;
    }
    public boolean isLeftVal() {
        return leftVal;
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