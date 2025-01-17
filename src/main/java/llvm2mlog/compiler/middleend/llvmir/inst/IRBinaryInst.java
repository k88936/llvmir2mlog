package llvm2mlog.compiler.middleend.llvmir.inst;

import llvm2mlog.compiler.middleend.llvmir.Value;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.middleend.llvmir.type.IRBaseType;
import llvm2mlog.compiler.share.pass.InstVisitor;

public class IRBinaryInst extends IRBaseInst {
    public String op;

    public IRBinaryInst(String op, IRBaseType retType, Value lhs, Value rhs, IRBlock parentBlock) {
        super(op, retType, parentBlock);
        this.op = op;
        this.addOperand(lhs);
        this.addOperand(rhs);
    }

    public Value lhs() {
        return this.getOperand(0);
    }

    public Value rhs() {
        return this.getOperand(1);
    }

    @Override
    public IRBaseInst copy() {
        return new IRBinaryInst(op, type, lhs(), rhs(), null);
    }

    @Override
    public void accept(InstVisitor visitor) {
        visitor.visit(this);
    }
}
