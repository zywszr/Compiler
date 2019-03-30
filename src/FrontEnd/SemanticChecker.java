package FrontEnd;

import TypeDefition.*;
import ScopeClass.*;

import javafx.util.Pair;

import javax.crypto.spec.OAEPParameterSpec;
import javax.print.attribute.standard.MediaSize;
import java.util.Stack;

public class SemanticChecker extends ASTVisitor {
    GlobalScope<TypeDef> rootScope;
    ClassScope<TypeDef> curClassSCope;
    int loopNum;
    String curClass;
    //St
    TypeDef funcRetType;

    public SemanticChecker(GlobalScope<TypeDef> root) {
        rootScope = root;
        curClassSCope = null;
        loopNum = 0;
        curClass = "";
        funcRetType = null;
    }
    // without string
    boolean checkTypeExist(TypeDef type) {
        if (type instanceof ArrayTypeDef) type = ((ArrayTypeDef)type).getSingleType();
        if (type instanceof SpecialTypeDef) {
            ClassTypeDef belong = (ClassTypeDef) rootScope.findItem(((SpecialTypeDef) type).getTypeId());
            if (belong == null) return false;
            ((SpecialTypeDef) type).setBelongClass(belong);
        }
        return true;
    }

    boolean checkLeftValue(ExprNode node) {
        if (node instanceof VarEleExprNode) return true;
        if (node instanceof PriPntExprNode &&
            checkLeftValue((ExprNode) node.childs.get(1))) return true;
        if (node instanceof PriArrExprNode) return true;
        return false;
    }

    boolean matchFuncForm(FuncTypeDef func, FunEleExprNode node) {
        int num = func.getParaNum();
        if (num != node.childs.size()) return false;
        for (int i = 0 ; i < num ; ++ i) {
            Node child = node.childs.get(i);
            if (!AssignOpType.checkExpr(func.getPara(i), child.type)) return false;
        }
        return true;
    }

    TypeDef typeCheckObj(ClassTypeDef nodeclass, Node node) throws Exception {
        if (!nodeclass.contain(node.id)) throw new NoDefinedVarError(node.pos);
        TypeDef type = nodeclass.getEntity(node.id);
        if (node instanceof FunEleExprNode) {
            if (!(type instanceof FuncTypeDef)) throw new NoDefinedVarError(node.pos);
            if (!matchFuncForm((FuncTypeDef)type, (FunEleExprNode)node)) throw new NoDefinedVarError(node.pos);
            return ((FuncTypeDef)type).getRetType();
        } else {
            if (!(type instanceof VarTypeDef)) throw new NoDefinedVarError(node.pos);
            return type;
        }
    }

    @Override void visit(VarDefNode curNode) throws Exception {
        if (curNode.type instanceof VoidTypeDef
            || (curNode.type instanceof ArrayTypeDef
                && ((ArrayTypeDef)(curNode.type)).getSingleType() instanceof VoidTypeDef)) {
            throw new VoidDefVarError(curNode.pos);
        }
        if (!checkTypeExist(curNode.type)) throw new NoDefinedTypeError(curNode.pos);
        visitChild(curNode);
        if (curNode.childs.size() > 0) {
            ExprNode expr = (ExprNode)curNode.childs.get(0);
            if (!curNode.type.equals(expr.type)) {
                //OpType op = OpType.getOpType("=");
                if (!AssignOpType.checkExpr(curNode.type, expr.type)) {
                    throw new NoCastExpr(expr.pos);
                }
            }
        }
        Scope<TypeDef> curScope = curNode.belong;
        if (curScope instanceof ClassScope) curNode.inClass = curClass;
        else {
            if (!curScope.addItem(curNode.id, curNode.type)) {
                throw new ReDefinedError(curNode.pos);
            }
        }
    }

    @Override void visit(ClassDefNode curNode) throws Exception {
        curClass = curNode.id;
        curClassSCope = (ClassScope<TypeDef>)curNode.belong;
        /*for (int i = 0 ; i < curNode.childs.size() ; ++ i) {
            Node childNode = curNode.childs.get(i);
            if (!(childNode instanceof ))
        }*/
        visitChild(curNode);
        curNode.type = rootScope.findItem(curNode.id);
        curClass = "";
        curClassSCope = null;
    }

    @Override void visit(FunctionDefNode curNode) throws Exception {
        if(!checkTypeExist(curNode.type)) throw new NoDefinedTypeError(curNode.pos);
        funcRetType = curNode.type;
        visitChild(curNode);
        funcRetType = null;
        if (curNode.belong instanceof ClassScope) curNode.inClass = curClass;
    }

    @Override void visit(ConstructFuncNode curNode) throws Exception {
        if (!curClass.equals(curNode.id)) throw new WrongNameConsFunc(curNode.pos);
        funcRetType = curNode.type;
        visitChild(curNode);
        funcRetType = null;
        curNode.inClass = curClass;
    }

    @Override void visit(CondStateNode curNode) throws Exception {
        visitChild(curNode);
        ExprNode expr = (ExprNode)curNode.childs.get(0);
        if (!(expr.type instanceof BoolTypeDef)) throw new NotConditionExpr(expr.pos);
    }

    @Override void visit(ForStateNode curNode) throws Exception {
        ++ loopNum;
        visitChild(curNode);
        -- loopNum;
        ExprNode expr = (ExprNode)curNode.childs.get(1);
        if (!(expr instanceof EmptyExprNode)) {
            if (!(expr.type instanceof BoolTypeDef)) throw new NotConditionExpr(expr.pos);
        }
    }

    @Override void visit(WhileStateNode curNode) throws Exception {
        ++ loopNum;
        visitChild(curNode);
        -- loopNum;
        ExprNode expr = (ExprNode)curNode.childs.get(0);
        if (!(expr.type instanceof BoolTypeDef)) throw new NotConditionExpr(expr.pos);
    }

    @Override void visit(BreakStateNode curNode) throws Exception {
        if (loopNum == 0) throw new BrkOutIterStat(curNode.pos);
    }

    @Override void visit(ContinStateNode curNode) throws Exception {
        if (loopNum == 0) throw new CntOutIterStat(curNode.pos);
    }

    @Override void visit(ReturnStateNode curNode) throws Exception {
        if (funcRetType instanceof VoidTypeDef) {
            if (!curNode.childs.isEmpty()) throw new DisMatchedFuncReturn(curNode.pos);
        } else {
            if (curNode.childs.isEmpty()) throw new DisMatchedFuncReturn(curNode.pos);
            visitChild(curNode);
            ExprNode expr = (ExprNode) curNode.childs.get(0);
            if(!AssignOpType.checkExpr(funcRetType, expr.type)) {
                throw new DisMatchedFuncReturn(curNode.pos);
            }
        }
    }

    @Override void visit(VarDefStateNode curNode) throws Exception {
        visitChild(curNode);
    }

    @Override void visit(BinExprNode curNode) throws Exception {
        visitChild(curNode);
        ExprNode left = (ExprNode)curNode.childs.get(0);
        ExprNode right = (ExprNode)curNode.childs.get(1);
        OpType op = OpType.getOpType(curNode.id);
        if (!left.type.equals(right.type)) {
            boolean flag = false;
            if (op instanceof AssignOpType && AssignOpType.checkExpr(left.type, right.type)) flag = true;
            if (op instanceof CompOpType && ((CompOpType)op).checkExpr(left.type, right.type)) flag = true;
            if (!flag) throw new NoCastExpr(curNode.pos);
        }
        if (op instanceof AssignOpType) {
            if (!checkLeftValue(left)) throw new NotLeftValue(left.pos);
            if (left instanceof VarEleExprNode && left.id.equals("this")) throw new ThisBeAssigned(left.pos);
            curNode.type = TypeDef.build("void");
        } else if (op.containsType(left.type)) {
            if (op instanceof CompOpType) curNode.type = new BoolTypeDef();
            else curNode.type = left.type;
        } else throw new NoDefinedOpError(curNode.pos);
    }

    @Override void visit(LUnaryExprNode curNode) throws Exception {
        visitChild(curNode);
        ExprNode expr = (ExprNode)curNode.childs.get(0);
        OpType op = OpType.getOpType(curNode.id);
        if (op instanceof SelfOpType && !checkLeftValue(expr)) throw new NotLeftValue(expr.pos);
        if (!op.containsType(expr.type)) throw new NoCastExpr(curNode.pos);
        curNode.type = expr.type;
    }

    @Override void visit(RUnaryExprNode curNode) throws Exception {
        visitChild(curNode);
        ExprNode expr = (ExprNode)curNode.childs.get(0);
        OpType op = OpType.getOpType(curNode.id);
        if (op instanceof SelfOpType && !checkLeftValue(expr)) throw new NotLeftValue(expr.pos);
        if (!op.containsType(expr.type)) throw new NoCastExpr(curNode.pos);
        curNode.type = expr.type;
    }

    @Override void visit(NewVarNode curNode) throws Exception {
        if (!checkTypeExist(curNode.type)) throw new NoDefinedOpError(curNode.pos);
        visitChild(curNode);
        for (int i = 0 ; i < curNode.childs.size() ; ++ i) {
            ExprNode expr = (ExprNode)(curNode.childs.get(i));
            if (!(expr instanceof EmptyExprNode) && !(expr.type instanceof IntTypeDef)) {
                throw new NoCastExpr(expr.pos);
            }
        }
        if (curNode.childs.size() > 0) {
            curNode.type = new ArrayTypeDef((VarTypeDef) curNode.type, curNode.childs.size());
        }
    }

    @Override void visit(FunEleExprNode curNode) throws Exception {
        visitChild(curNode);
        Pair<Scope<TypeDef>, TypeDef> ret = curNode.belong.matchVarName(curNode.id);
        if (ret == null) throw new NoDefinedTypeError(curNode.pos);
        if (ret.getKey() instanceof ClassScope) curNode.inClass = curClass;
        TypeDef func = ret.getValue();
        if ((!(func instanceof FuncTypeDef)) || (!matchFuncForm((FuncTypeDef)func, curNode))) {
            throw new NoDefinedTypeError(curNode.pos);
        }
        curNode.type = ((FuncTypeDef)func).getRetType();
    }

    @Override void visit(PriArrExprNode curNode) throws Exception {
        visitChild(curNode);
        Node pointer = curNode.childs.get(0);
        if (!(pointer.type instanceof ArrayTypeDef)) throw new NullPointer(curNode.pos);
        curNode.type = ((ArrayTypeDef)pointer.type).getInnerType();
    }

    @Override void visit(PriPntExprNode curNode) throws Exception {
        Node child = curNode.childs.get(0);
        Node object = curNode.childs.get(1);
        visit(child);
        if (child.type instanceof ArrayTypeDef) {
            if (object.id.equals("size") && object.childs.size() == 0) curNode.type = new IntTypeDef();
            else throw new NoDefinedTypeError(object.pos);
            return;
        }
        if (!(child.type instanceof SpecialTypeDef)) throw new NoCastExpr(curNode.pos);
        ClassTypeDef nodeclass = (ClassTypeDef)rootScope.findItem(((SpecialTypeDef)child.type).getTypeId());

        visitChild(object);
        curNode.type = typeCheckObj(nodeclass, object);
        if (curNode.type instanceof OtherTypeDef) ((OtherTypeDef)curNode.type).setBelongClass(nodeclass);
        object.inClass = ((SpecialTypeDef)child.type).getTypeId();
        object.type = curNode.type;
    }

    @Override void visit(VarEleExprNode curNode) throws Exception {
        if (curNode.id.equals("this")) {
            if (curClass == "") throw new ThisOutOfClass(curNode.pos);
            curNode.type = new OtherTypeDef(curClass);
            ((OtherTypeDef)curNode.type).setBelongClass((ClassTypeDef)rootScope.findItem(curClass));
            return;
        }
        Pair<Scope<TypeDef>, TypeDef> ret = curNode.belong.matchVarName(curNode.id);
     //   System.out.println(curNode.id);
        if (ret == null) throw new NoDefinedVarError(curNode.pos);
        curNode.type = ret.getValue();
    }
}
