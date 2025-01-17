package llvm2mlog.compiler.share.pass;

import llvm2mlog.compiler.middleend.llvmir.inst.*;

public interface InstVisitor extends Pass {
    void visit(IRAllocaInst inst);

    void visit(IRBinaryInst inst);

    void visit(IRBitCastInst inst);

    void visit(IRBrInst inst);

    void visit(IRCallInst inst);

    void visit(IRGetElementPtrInst inst);

    void visit(IRICmpInst inst);

    void visit(IRLoadInst inst);

    void visit(IRPhiInst inst);

    void visit(IRRetInst inst);

    void visit(IRStoreInst inst);

    void visit(IRCastInst inst);

//    void visit(IRZextInst inst);

    void visit(IRMoveInst inst);
}
