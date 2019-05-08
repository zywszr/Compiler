package FrontEnd;

import OprandClass.*;
import IRClass.*;
import TypeDefition.*;
import IRClass.FuncFrame;

import static BackEnd.RegisterSet.args;
import static IRClass.Inst.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import static BackEnd.RegisterSet.getReg;

public class IRBuilder extends ASTVisitor {

    LineIR lineIR;
    FuncFrame curfunc;
    CFGNode curlabel;
    //HashMap <String, Long> curClassObjIdx;

    //HashMap <String, Long> curClassObjSiz;
    //Long curClassSize;
    int curVarKind; // 0: class 1: global 2:local
    int labelIdx;
    int tmpVarIdx;
    Stack <CFGNode> trueLabels, falseLabels;
    Stack <CFGNode> nextLabels, breakLabels, continLabels;
    ClassTypeDef curClassType;
    CFGNode curRetLabel;
    String inClass;

    public IRBuilder() {
        lineIR = new LineIR();
        curfunc = null;
        curVarKind = 1;
        labelIdx = 0;
        tmpVarIdx = -1;
        trueLabels = new Stack<>();
        falseLabels = new Stack<>();
        nextLabels = new Stack<>();
        breakLabels = new Stack<>();
        continLabels = new Stack<>();
        curClassType = null;
        curRetLabel = null;
        inClass = "";
    }

    public LineIR buildLineIR(Node ASTroot) throws Exception {
        ASTroot.accept(this);
        return lineIR;
    }

    public CFGNode createNewLabel() {
        CFGNode label = new CFGNode(labelIdx);
        labelIdx += 1;
        curfunc.pushLabel(label);
        return label;
    }

    public void createNewFunc(String funcName) {
        curfunc = new FuncFrame(funcName);
        curlabel = createNewLabel();
    }

    public void completeFunc() {
        curfunc.createCFG();
        lineIR.pushFunc(curfunc);
    }

    public void addEdge(CFGNode from, CFGNode to) {
        from.addTo(to);
        to.addFrom(from);
    }

    @Override
    public void visit(ProgramNode node) throws Exception {
        curVarKind = 1;
        createNewFunc("___init");
        curRetLabel = createNewLabel();
        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            if (child instanceof VarDefStateNode) visit(child);
        }
        addQuad(curlabel, new JumpQuad(JMP, curRetLabel));
        curfunc.setEnd(curRetLabel);
        curRetLabel = null;
        completeFunc();

        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            if (!(child instanceof VarDefStateNode)) visit(child);
        }
    }

    @Override public void visit(ClassDefNode node) throws Exception {
        curVarKind = 0;
        curClassType = (ClassTypeDef) node.type;
        inClass = node.id;
        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            if (child instanceof VarDefStateNode) visit(child);
        }

        boolean flag = false;
        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            if (!(child instanceof VarDefStateNode)) visit(child);
            if (child instanceof ConstructFuncNode) flag = true;
        }
        inClass = "";
        curClassType = null;
        if (!flag) {
            createNewFunc(node.id + "_" + node.id);
            completeFunc();
        }
        lineIR.pushClassSize(node.id, node.type.getSize());
    }

    RegOprand newTempVar(boolean isAddr) {
        tmpVarIdx += 1;
        return getReg((isAddr ? "A" : "V") + "_" + Integer.toString(tmpVarIdx), true);
    }

    public int getTmpVarIdx() {
        return tmpVarIdx;
    }

    void genFuncQuad(String funcName, ArrayList<Oprand> params, boolean isReturn, Oprand rdest) {
        for (int i = params.size() - 1 ; i >= 0 ; -- i) {
            addQuad(curlabel, new FuncQuad(PARAM, params.get(i), new ImmOprand((long) i)));
        }
        Oprand rd = null;
        if (isReturn) rd = rdest;
        addQuad(curlabel, new FuncQuad(CALL, rd, funcName, new ImmOprand((long)params.size())));
    }

    void genNewFunc(Oprand siz, Oprand rdest) {
        ArrayList <Oprand> params = new ArrayList<>();
        params.add(siz);
        genFuncQuad("malloc", params, true, rdest);
    }

    void genStrcpyFunc(Oprand to, Oprand from) {
        ArrayList <Oprand> params = new ArrayList<>();
        params.add(to);
        params.add(from);
        genFuncQuad("String_strcpy", params, false, null);
    }

    @Override public void visit(VarDefNode node) throws Exception {
        // node.reg = getReg(node.reName, false);
        switch (curVarKind) {
            case 0: // class
                // pushClassObj(node);
                break;
            case 1: // global
                node.reg = new GlobalMemOprand(getReg(node.reName, false));
                lineIR.pushGlobalVar(node.reName);
            case 2: // local
                if (node.reg == null) {
                    node.reg = getReg(node.reName, false);
                }
                curfunc.pushVar(node.reName);
                // string
                if (node.type instanceof StringTypeDef) {
                    genNewFunc(new ImmOprand(256L), node.reg);
                    if (node.childs.size() > 0) {
                        visitChild(node);
                        genStrcpyFunc(node.reg, node.childs.get(0).reg);
                    }
                    break;
                }
                // bool int class array
                if (node.childs.size() > 0) {
                    Node child = node.childs.get(0);
                    if (node.type instanceof BoolTypeDef) {
                        CFGNode newTrueLabel = createNewLabel(), newFalseLabel = createNewLabel(), nowLabel = createNewLabel();
                        addQuad(newTrueLabel, new ArthQuad(MOV, node.reg, new ImmOprand(1L)));
                        addQuad(newTrueLabel, new JumpQuad(JMP, nowLabel));

                        addQuad(newFalseLabel, new ArthQuad(MOV, node.reg, new ImmOprand(0L)));
                        addQuad(newFalseLabel, new JumpQuad(JMP, nowLabel));

                        pushTFLabel(newTrueLabel, newFalseLabel);
                        child.setNotUse();
                        visit(child);
                        popTFLabel();
                        curlabel = nowLabel;

                        break;
                    }
                    visitChild(node);
                    addQuad(curlabel, new ArthQuad(MOV, node.reg, child.reg));
                }
                break;
        }
    }

    @Override public void visit(FunctionDefNode node) throws Exception {
        if (!node.inClass.equals("")) createNewFunc(node.inClass + "_" + node.id);
        else createNewFunc(node.id);
        if (node.id.equals("main")) {
            addQuad(curlabel, new FuncQuad(CALL, null, "___init", new ImmOprand(0L)));
        }
        curRetLabel = createNewLabel();
        curVarKind = 2;
        int K;
        if (!inClass.equals("")) {
            addQuad(curlabel, new ArthQuad(MOV, getReg(inClass + "_this", true), args.get(0)));
            curfunc.parameters.add(getReg(inClass + "_this", true));
            K = 5;
        } else {
            K = 6;
        }
        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            visit(child);
            if (i < node.childs.size() - 1) {
                curfunc.parameters.add(child.reg);
                if (i < K) {
                    addQuad(curlabel, new ArthQuad(MOV, child.reg, args.get(i)));
                } else {
                    MemOprand stack = new StackSlot();
                    child.reg.setMemPos(stack);
                    addQuad(curlabel, new ArthQuad(MOV, child.reg, stack));
                }
            }
        }

        if (curlabel != null) {
           addQuad(curlabel, new JumpQuad(JMP, curRetLabel));
        }
        curfunc.setEnd(curRetLabel);
        curRetLabel = null;
        completeFunc();
    }

    @Override public void visit(ConstructFuncNode node) throws Exception {
        createNewFunc(node.id + "_" + node.id);
        curRetLabel = createNewLabel();
        curVarKind = 2;
        addQuad(curlabel, new ArthQuad(MOV, getReg(inClass + "_this", true), args.get(0)));
        curfunc.parameters.add(getReg(inClass + "_this", true));
        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            visit(child);
            if (i < node.childs.size() - 1) {
                curfunc.parameters.add(child.reg);
                if (i < 5) {
                    addQuad(curlabel, new ArthQuad(MOV, child.reg, args.get(i + 1)));
                } else {
                    addQuad(curlabel, new ArthQuad(MOV, child.reg, new StackSlot()));
                }
            }
        }

        addQuad(curlabel, new JumpQuad(JMP, curRetLabel));
        curfunc.setEnd(curRetLabel);
        curRetLabel = null;
        completeFunc();
    }

    @Override public void visit(BlockStateNode node) throws Exception {
        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            visit(child);
            if (curlabel == null) break;
        }
    }

    // @Override public void visit(ExprStateNode node) throws Exception { }

    @Override public void visit(CondStateNode node) throws Exception {
        CFGNode newTrueLabel = createNewLabel(), newFalseLabel, newNextLabel = createNewLabel();
        if (node.childs.size() > 2) newFalseLabel = createNewLabel();
        else newFalseLabel = newNextLabel;

        pushTFLabel(newTrueLabel, newFalseLabel);
        node.childs.get(0).setNotUse();
        visit(node.childs.get(0));
        popTFLabel();

        curlabel = newTrueLabel;
        visit(node.childs.get(1));
        if (curlabel != null) {
            addQuad(curlabel, new JumpQuad(JMP, newNextLabel));
        }
        if (node.childs.size() > 2) {
            curlabel = newFalseLabel;
            visit(node.childs.get(2));
            if (curlabel != null) {
                addQuad(curlabel, new JumpQuad(JMP, newNextLabel));
            }
        }
        if (!newNextLabel.hasFrom()) curlabel = null;
        else curlabel = newNextLabel;
    }

    void pushBCLabel(CFGNode breakLabel, CFGNode continLabel) {
        breakLabels.push(breakLabel);
        continLabels.push(continLabel);
    }

    void popBCLabel() {
        breakLabels.pop();
        continLabels.pop();
    }

    @Override public void visit(ForStateNode node) throws Exception {
        Node initExp = node.childs.get(0), condExp = node.childs.get(1), loopExp = node.childs.get(2);
        Node state = node.childs.get(3);
        CFGNode condLabel = createNewLabel(), stateLabel, loopLabel, nextLabel = createNewLabel();
        if (initExp instanceof EmptyExprNode) {
            addQuad(curlabel, new JumpQuad(JMP, condLabel));
        } else if (initExp.type instanceof BoolTypeDef) {
            pushTFLabel(condLabel, condLabel);
            initExp.setNotUse();
            visit(initExp);
            popTFLabel();
        } else {
            visit(initExp);
            addQuad(curlabel, new JumpQuad(JMP, condLabel));
        }

        curlabel = condLabel;
        if (condExp instanceof EmptyExprNode) {
            stateLabel = condLabel;
        } else {
            stateLabel = createNewLabel();
            pushTFLabel(stateLabel, nextLabel);
            condExp.setNotUse();
            visit(condExp);
            popTFLabel();
        }

        curlabel = stateLabel;
        if (loopExp instanceof EmptyExprNode) {
            loopLabel = condLabel;
        } else {
            loopLabel = createNewLabel();
        }
        pushBCLabel(nextLabel, loopLabel);
        visit(state);
        popBCLabel();
        if (curlabel != null) {
            addQuad(curlabel, new JumpQuad(JMP, loopLabel));
        }

        curlabel = loopLabel;
        if ((!(loopExp instanceof EmptyExprNode)) && loopLabel.hasFrom()) {
            if (loopExp.type instanceof BoolTypeDef) {
                pushTFLabel(condLabel, condLabel);
                loopExp.setNotUse();
                visit(loopExp);
                popTFLabel();
            } else {
                visit(loopExp);
                addQuad(curlabel, new JumpQuad(JMP, condLabel));
            }
        }

        if (nextLabel.hasFrom()) curlabel = nextLabel;
        else curlabel = null;
    }

    @Override public void visit(WhileStateNode node) throws Exception {
        Node cond = node.childs.get(0), state = node.childs.get(1);
        CFGNode condLabel = createNewLabel(), stateLabel = createNewLabel(), nextLabel = createNewLabel();
        addQuad(curlabel, new JumpQuad(JMP, condLabel));
        curlabel = condLabel;
        pushTFLabel(stateLabel, nextLabel);
        cond.setNotUse();
        visit(cond);
        popTFLabel();

        curlabel = stateLabel;
        pushBCLabel(nextLabel, condLabel);
        visit(state);
        popBCLabel();
        if (curlabel != null) {
            addQuad(curlabel, new JumpQuad(JMP, condLabel));
        }

        if (nextLabel.hasFrom()) {
            curlabel = nextLabel;
        } else {
            curlabel = null;
        }
    }

    @Override public void visit(ReturnStateNode node) throws Exception {
        if (node.childs.isEmpty()) {
            // addQuad(curlabel, new FuncQuad(RET, null));
            addQuad(curlabel, new JumpQuad(JMP, curRetLabel));
        } else {
            Node expr = node.childs.get(0);
            if (expr.type instanceof BoolTypeDef) {
                CFGNode newTrueLabel = createNewLabel(), newFalseLabel = createNewLabel();
                addQuad(newTrueLabel, new FuncQuad(RET, new ImmOprand(1L)));
                addQuad(newTrueLabel ,new JumpQuad(JMP, curRetLabel));
                addQuad(newFalseLabel, new FuncQuad(RET, new ImmOprand(0L)));
                addQuad(newFalseLabel, new JumpQuad(JMP, curRetLabel));

                pushTFLabel(newTrueLabel, newFalseLabel);
                expr.setNotUse();
                visit(expr);
                popTFLabel();
            } else {
                visit(expr);
                addQuad(curlabel, new FuncQuad(RET, expr.reg));
                addQuad(curlabel, new JumpQuad(JMP, curRetLabel));
            }
        }
        curlabel = null;
    }

    @Override public void visit(BreakStateNode node) throws Exception {
        addQuad(curlabel, new JumpQuad(JMP, breakLabels.peek()));
        curlabel = null;
    }

    @Override public void visit(ContinStateNode node) throws Exception {
        addQuad(curlabel, new JumpQuad(JMP, continLabels.peek()));
        curlabel = null;
    }

    // @Override public void visit(NullStateNode node) throws Exception { }

    // @Override public void visit(VarDefStateNode node) throws Exception { }

    // @Override public void visit(EmptyExprNode node) throws Exception { }

    void pushTFLabel(CFGNode trueLabel, CFGNode falseLabel) {
        trueLabels.push(trueLabel);
        falseLabels.push(falseLabel);
    }

    void popTFLabel() {
        trueLabels.pop();
        falseLabels.pop();
    }

    void MemToVar(CFGNode label, Oprand reg) {
        if (reg != null && reg instanceof AddrOprand) {
            Oprand base = ((AddrOprand) reg).getBase(), offset = ((AddrOprand) reg).getOffSet();
            if (base instanceof MemOprand) {
                Oprand tmp = newTempVar(true);
                label.insertQuad(new ArthQuad(MOV, tmp, base));
                ((AddrOprand) reg).setBase(tmp);
            }
            if (offset instanceof MemOprand) {
                Oprand tmp = newTempVar(false);
                label.insertQuad(new ArthQuad(MOV, tmp, offset));
                ((AddrOprand) reg).setOffSet(tmp);
            }
        }
    }

    void addQuad(CFGNode label, Quad quad) {
        MemToVar(label, quad.getR1());
        label.insertQuad(quad);
        if (quad instanceof JumpQuad) {
            ArrayList <CFGNode> labels = ((JumpQuad)quad).getLabel();
            for (CFGNode p : labels) {
                addEdge(label, p);
            }
        }
    }

    void genStrcatFunc(Oprand l, Oprand r) {
        ArrayList <Oprand> params = new ArrayList<>();
        params.add(l);
        params.add(r);
        genFuncQuad("String_strcat", params, false, null);
    }

    @Override public void visit(BinExprNode node) throws Exception {
        if (node.isUnique()) {
            if (node.type instanceof StringTypeDef) {
                node.reg = new StrOprand(lineIR.addStrLiter(node.strLiter));
            } else if (node.type instanceof BoolTypeDef) {
                if (((ImmOprand) node.reg).getVal() == 1L) {
                    addQuad(curlabel, new JumpQuad(JMP, trueLabels.peek()));
                } else {
                    addQuad(curlabel, new JumpQuad(JMP, falseLabels.peek()));
                }
            }
            return;
        }

        Node lson = node.childs.get(0), rson = node.childs.get(1);
        if (node.type instanceof BoolTypeDef) {
            if (node.id.equals("&&") || node.id.equals("||")) {
                CFGNode newTrueLabel = null, newFalseLabel = null;
                if (node.isWillUse()) {
                    node.reg = newTempVar(false);
                    newTrueLabel = createNewLabel();
                    addQuad(newTrueLabel, new ArthQuad(MOV, node.reg, new ImmOprand(1L)));
                    addQuad(newTrueLabel, new JumpQuad(JMP, trueLabels.peek()));
                    newFalseLabel = createNewLabel();
                    addQuad(newFalseLabel, new ArthQuad(MOV, node.reg, new ImmOprand(0L)));
                    addQuad(newFalseLabel, new JumpQuad(JMP, falseLabels.peek()));
                }

                if (node.id.equals("&&")) {
                    CFGNode rtlabel = createNewLabel();
                    trueLabels.push(rtlabel);
                    if (node.isWillUse()) {
                        falseLabels.push(newFalseLabel);
                    }
                    lson.setNotUse();
                    visit(lson);
                    trueLabels.pop();

                    curlabel = rtlabel;
                    if (node.isWillUse()) {
                        trueLabels.push(newTrueLabel);
                    }
                    rson.setNotUse();
                    visit(rson);
                    if (node.isWillUse()) {
                        popTFLabel();
                    }
                } else {
                    CFGNode rtlabel = createNewLabel();
                    falseLabels.push(rtlabel);
                    if (node.isWillUse()) {
                        trueLabels.push(newTrueLabel);
                    }
                    lson.setNotUse();
                    visit(lson);
                    falseLabels.pop();

                    curlabel = rtlabel;
                    if (node.isWillUse()) {
                        falseLabels.push(newFalseLabel);
                    }
                    rson.setNotUse();
                    visit(rson);
                    if (node.isWillUse()) {
                        popTFLabel();
                    }
                }
            } else {
                RegOprand tmp = null;
                if (lson.type instanceof StringTypeDef) {
                    visitChild(node);
                    ArrayList <Oprand> params = new ArrayList<>();
                    params.add(lson.reg);
                    params.add(rson.reg);
                    tmp = newTempVar(false);
                    genFuncQuad("strcmp", params, true, tmp);

                } else if (lson.type instanceof BoolTypeDef) {
                    CFGNode rtlabel = createNewLabel();
                    pushTFLabel(rtlabel, rtlabel);
                    visit(lson);
                    popTFLabel();

                    curlabel = rtlabel;
                    CFGNode nowlabel = createNewLabel();
                    pushTFLabel(nowlabel, nowlabel);
                    visit(rson);
                    popTFLabel();

                    curlabel = nowlabel;
                } else {
                    visitChild(node);
                }

                CFGNode newTrueLabel, newFalseLabel;
                if (node.isWillUse()) {
                    node.reg = newTempVar(false);
                    newTrueLabel = createNewLabel();
                    addQuad(newTrueLabel, new ArthQuad(MOV, node.reg, new ImmOprand(1L)));
                    addQuad(newTrueLabel, new JumpQuad(JMP, trueLabels.peek()));
                    newFalseLabel = createNewLabel();
                    addQuad(newFalseLabel, new ArthQuad(MOV, node.reg, new ImmOprand(0L)));
                    addQuad(newFalseLabel, new JumpQuad(JMP, falseLabels.peek()));
                } else {
                    newTrueLabel = trueLabels.peek();
                    newFalseLabel = falseLabels.peek();
                }
                String op = "";
                switch (node.id) {
                    case "==":
                        op = JE;
                        break;
                    case "!=":
                        op = JNE;
                        break;
                    case "<":
                        op = JL;
                        break;
                    case "<=":
                        op = JLE;
                        break;
                    case ">":
                        op = JG;
                        break;
                    case ">=":
                        op = JGE;
                        break;
                }
                if (lson.type instanceof StringTypeDef) {
                    addQuad(curlabel, new CompQuad(tmp, new ImmOprand(0L)));
                } else {
                    addQuad(curlabel, new CompQuad(lson.reg, rson.reg));
                }
                addQuad(curlabel, new JumpQuad(op, newTrueLabel, newFalseLabel));
            }
        } else if (node.type instanceof IntTypeDef){
            visitChild(node);
            node.reg = newTempVar(false);
            switch (node.id) {
                case "+":
                    addQuad(curlabel, new ArthQuad("add", node.reg, lson.reg, rson.reg));
                    break;
                case "-":
                    addQuad(curlabel, new ArthQuad("sub", node.reg, lson.reg, rson.reg));
                    break;
                case "*":
                    addQuad(curlabel, new ArthQuad("mul", node.reg, lson.reg, rson.reg));
                    break;
                case "/":
                    addQuad(curlabel, new ArthQuad("div", node.reg, lson.reg, rson.reg));
                    break;
                case "%":
                    addQuad(curlabel, new ArthQuad("mod", node.reg, lson.reg, rson.reg));
                    break;
                case "<<":
                    addQuad(curlabel, new ArthQuad("sal", node.reg, lson.reg, rson.reg));
                    break;
                case ">>":
                    addQuad(curlabel, new ArthQuad("sar", node.reg, lson.reg, rson.reg));
                    break;
                case "&":
                    addQuad(curlabel, new ArthQuad("and", node.reg, lson.reg, rson.reg));
                    break;
                case "|":
                    addQuad(curlabel, new ArthQuad("or", node.reg, lson.reg, rson.reg));
                    break;
                case "^":
                    addQuad(curlabel, new ArthQuad("xor", node.reg, lson.reg, rson.reg));
                    break;
            }
        } else if (node.type instanceof StringTypeDef) {
            if (node.reg != null) {
                node.reg = newTempVar(true);
                genNewFunc(new ImmOprand(256L), node.reg);
            }

            if (lson.id.equals("+")) lson.reg = node.reg;
            visit(lson);
            if (!lson.id.equals("+")) genStrcatFunc(node.reg, lson.reg);

            if (rson.id.equals("+")) rson.reg = node.reg;
            visit(rson);
            if (!rson.id.equals("+")) genStrcatFunc(node.reg, rson.reg);
        } else if (node.id.equals("=")){
            lson.setLeftVal();
            visit(lson);
            if (rson.type instanceof BoolTypeDef) {
                CFGNode nowLabel = createNewLabel();
                pushTFLabel(nowLabel, nowLabel);
                visit(rson);
                popTFLabel();
                curlabel = nowLabel;
            } else {
                visit(rson);
            }
            addQuad(curlabel, new ArthQuad(MOV, lson.reg, rson.reg));
            node.reg = null;
        }
    }

    boolean checkAddrType(TypeDef type) {
        return (type instanceof ArrayTypeDef) || (type instanceof SpecialTypeDef);
    }

    @Override public void visit(LUnaryExprNode node) throws Exception {
        if (node.isUnique()) {
            if (node.type instanceof BoolTypeDef) {
                if (((ImmOprand) node.reg).getVal() == 1L) {
                    addQuad(curlabel, new JumpQuad(JMP, trueLabels.peek()));
                } else {
                    addQuad(curlabel, new JumpQuad(JMP, falseLabels.peek()));
                }
            }
        }

        Node child = node.childs.get(0);
        if (node.id.equals("!")) {
            child.setNotUse();
            if (node.isWillUse()) {
                node.reg = newTempVar(checkAddrType(node.type));
                CFGNode newTrueLabel = createNewLabel();
                addQuad(newTrueLabel, new ArthQuad(MOV, node.reg, new ImmOprand(1L)));
                addQuad(newTrueLabel, new JumpQuad(JMP, trueLabels.peek()));

                CFGNode newFalseLabel = createNewLabel();
                addQuad(newFalseLabel, new ArthQuad(MOV, node.reg, new ImmOprand(0L)));
                addQuad(newFalseLabel, new JumpQuad(JMP, falseLabels.peek()));

                pushTFLabel(newFalseLabel, newTrueLabel);
                visit(child);
                popTFLabel();
            } else {
                Stack <CFGNode> tmp = trueLabels;
                trueLabels = falseLabels;
                falseLabels = tmp;
                visit(child);
                tmp = trueLabels;
                trueLabels = falseLabels;
                falseLabels = tmp;

            }
        } else {
            node.reg = newTempVar(checkAddrType(node.type));
            visit(child);
            switch (node.id) {
                case "++":
                    addQuad(curlabel, new ArthQuad("add", child.reg, child.reg, new ImmOprand(1L)));
                    node.reg = child.reg;
                    break;
                case "--":
                    addQuad(curlabel, new ArthQuad("sub", child.reg, child.reg, new ImmOprand(1L)));
                    node.reg = child.reg;
                    break;
                case "+":
                    node.reg = child.reg;
                    break;
                case "-":
                    addQuad(curlabel, new ArthQuad("neg", node.reg, child.reg));
                    break;
                case "~":
                    addQuad(curlabel, new ArthQuad("not", node.reg, child.reg));
                    break;
            }
        }
    }

    @Override public void visit(RUnaryExprNode node) throws Exception {
        node.reg = newTempVar(checkAddrType(node.type));
        Node child = node.childs.get(0);
        visit(child);
        if (node.id.equals("++")) {
            addQuad(curlabel, new ArthQuad(MOV, node.reg, child.reg));
            addQuad(curlabel, new ArthQuad("add", child.reg, child.reg, new ImmOprand(1L)));
        } else {
            addQuad(curlabel, new ArthQuad(MOV, node.reg, child.reg));
            addQuad(curlabel, new ArthQuad("sub", child.reg, child.reg, new ImmOprand(1L)));
        }
    }

    @Override public void visit(NewVarNode node) throws Exception {
        if (node.childs.isEmpty()) {
            node.reg = newTempVar(true);
            if (node.type instanceof StringTypeDef) {
                genNewFunc(new ImmOprand(256L), node.reg);
            } else {
                genNewFunc(new ImmOprand(((OtherTypeDef)node.type).getClassSize() * 8), node.reg);
                ArrayList <Oprand> params = new ArrayList<>();
                params.add(node.reg);
                genFuncQuad(((OtherTypeDef) node.type).getTypeId() + "_" + ((OtherTypeDef) node.type).getTypeId(), params, false, null);
            }
            return;
        }
        Node expr = node.childs.get(0), eleType = node.childs.get(1);
        // eleType.setUnique();
        node.reg = newTempVar(true);
        visit(expr);
        if (expr.isUnique()) {
            genNewFunc(new ImmOprand(((ImmOprand)expr.reg).getVal() * 8 + 8), node.reg);
        } else {
            RegOprand tmp = newTempVar(false);
            addQuad(curlabel, new ArthQuad("mul", tmp, expr.reg, new ImmOprand(8L)));
            addQuad(curlabel, new ArthQuad("add", tmp, tmp, new ImmOprand(8L)));
            genNewFunc(tmp, node.reg);
        }
        addQuad(curlabel, new ArthQuad(MOV, new MemOprand(node.reg, null, null), expr.reg));

        if (eleType.childs.isEmpty() && (!(eleType.type instanceof StringTypeDef))) return;

        Oprand i = newTempVar(false);
        addQuad(curlabel, new ArthQuad(MOV, i, new ImmOprand(0L)));
        CFGNode condLabel = createNewLabel(), stateLabel = createNewLabel(), nextLabel = createNewLabel();
        addQuad(curlabel, new JumpQuad(JMP, condLabel));
        curlabel = condLabel;
        addQuad(curlabel, new CompQuad(i, expr.reg));
        addQuad(curlabel, new JumpQuad("jl", stateLabel, nextLabel));
        curlabel = stateLabel;
        visit(eleType);
        Oprand addr = newTempVar(true);

        addQuad(curlabel, new ArthQuad(LEA, addr, new ArrayAddrOprand(node.reg, i, new ImmOprand(8L))));
        addQuad(curlabel, new ArthQuad(MOV, new MemOprand(addr, null, null), eleType.reg));
        addQuad(curlabel, new ArthQuad(ADD, i, i, new ImmOprand(1L)));
        addQuad(curlabel, new JumpQuad(JMP, condLabel));

        curlabel = nextLabel;
    }

    @Override public void visit(FunEleExprNode node) throws Exception {
        String funcName = node.id;
        ArrayList <Oprand> params = new ArrayList<>();
        if (!node.inClass.equals("")) {
            funcName = node.inClass + "_" + funcName;
            params.add(getReg(node.inClass + "_this", false));
        }

        for (int i = 0 ; i < node.childs.size() ; ++ i) {
            Node child = node.childs.get(i);
            if (child.type instanceof BoolTypeDef) {
                CFGNode nowLabel = createNewLabel();
                pushTFLabel(nowLabel, nowLabel);
                visit(child);
                popTFLabel();
                curlabel = nowLabel;
            } else {
                visit(child);
            }
            params.add(child.reg);
        }
        if (node.type instanceof VoidTypeDef) {
            genFuncQuad(funcName, params, false, null);
        } else {
            node.reg = newTempVar(checkAddrType(node.type));
            genFuncQuad(funcName, params, true, node.reg);
            checkBool(node);
        }
    }

    boolean checkGlobalVar(String var) {
        return !var.contains(".");
    }

    void checkBool(Node node) {
        if (node.type instanceof BoolTypeDef) {
            CFGNode newTrueLabel = trueLabels.peek(), newFalseLabel = falseLabels.peek();
            addQuad(curlabel, new CompQuad(node.reg, new ImmOprand(1L)));
            addQuad(curlabel, new JumpQuad("je", newTrueLabel, newFalseLabel));
        }
    }

    @Override public void visit(VarEleExprNode node) throws Exception {
        if (node.id.equals("this")) {
            node.reg = getReg(node.reName, false);
            return;
        }
        if (!node.inClass.equals("")) {
            Oprand tmp = newTempVar(true);
            Oprand base = getReg(node.inClass + "_this", false);
            Oprand offset = new ImmOprand(curClassType.getVarIdx(node.reName));

            node.reg = new MemOprand(base, offset, new ImmOprand(8L));
        } else if (checkGlobalVar(node.reName)) {
            node.reg = new GlobalMemOprand(getReg(node.reName, false));
        } else {
            node.reg = getReg(node.reName, false);
        }
        if (!node.isLeftVal()) {
            checkBool(node);
        }
    }

    @Override public void visit(PriArrExprNode node) throws Exception {
        visitChild(node);
        Node lson = node.childs.get(0), rson = node.childs.get(1);
        Oprand tmp = newTempVar(true);

        addQuad(curlabel, new ArthQuad(LEA, tmp, new ArrayAddrOprand(lson.reg, rson.reg, new ImmOprand(8L))));
        node.reg = new MemOprand(tmp, null, null);

        if (!node.isLeftVal()) {
            checkBool(node);
        }
    }

    @Override public void visit(PriPntExprNode node) throws Exception {
        if (!(node.type instanceof VoidTypeDef)) {
            node.reg = newTempVar(checkAddrType(node.type));
        }
        Node child = node.childs.get(0), obj = node.childs.get(1);
        if (child.type instanceof ArrayTypeDef) {
            addQuad(curlabel, new ArthQuad(MOV, node.reg, new MemOprand(child.reg, null, null)));
            return;
        }
        visit(child);
        if (obj instanceof FunEleExprNode) {
            ArrayList <Oprand> params = new ArrayList<>();
            params.add(child.reg);
            for (int i = 0 ; i < obj.childs.size() ; ++ i) {
                Node son = obj.childs.get(i);
                if (son.type instanceof BoolTypeDef) {
                    CFGNode nowLabel = createNewLabel();
                    pushTFLabel(nowLabel, nowLabel);
                    visit(son);
                    popTFLabel();
                    curlabel = nowLabel;
                } else {
                    visit(son);
                }
                params.add(son.reg);
            }
            if (node.reg != null) {
                genFuncQuad(obj.inClass + "_" + obj.id, params, true, node.reg);
                checkBool(node);
            } else  {
                genFuncQuad(obj.inClass + "_" + obj.id, params, false, null);
            }
        } else {
            visitChild(obj);
            Oprand tmp = newTempVar(true);
            addQuad(curlabel, new ArthQuad(LEA,
                                                tmp,
                                                new AddrOprand(child.reg,
                                                                new ImmOprand(((OtherTypeDef)child.type).getBelongClass().getVarIdx(obj.reName)),
                                                                new ImmOprand(8L))));
            node.reg = new MemOprand(tmp, null, null);
            if (!node.isLeftVal()) {
                checkBool(node);
            }
        }
    }

    @Override public void visit(LogLitNode node) throws Exception {
        if (((ImmOprand) node.reg).getVal() == 1L) {
            addQuad(curlabel, new JumpQuad(JMP, trueLabels.peek()));
        } else {
            addQuad(curlabel, new JumpQuad(JMP, falseLabels.peek()));
        }
    }

    @Override public void visit(StrLitNode node) throws Exception {
        node.reg = new StrOprand(lineIR.addStrLiter(node.strLiter));
    }
}