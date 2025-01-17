package llvm2mlog.compiler.middleend.llvmir.inst;

import llvm2mlog.compiler.middleend.llvmir.Value;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.share.lang.LLVM;
import llvm2mlog.compiler.share.pass.InstVisitor;
import org.jetbrains.annotations.NotNull;

public class IRStoreInst extends IRBaseInst {

    public IRStoreInst(@NotNull Value storePtr, @NotNull Value storeValue, IRBlock parentBlock) {
        super(LLVM.StoreInst, storeValue.type, parentBlock);
        this.addOperand(storeValue);
        this.addOperand(storePtr);
    }

    public Value storeValue() {
        return this.getOperand(0);
    }

    public Value storePtr() {
        return this.getOperand(1);
    }

    public void replacePtr(Value value) {
        this.resetOperand(1, value);
    }

    @Override
    public boolean mayHaveSideEffects() {
        return true;
    }

    @Override
    public boolean isValueSelf() {
        return false;
    }

    @Override
    public IRBaseInst copy() {
        return new IRStoreInst(storePtr(), storeValue(), null);
    }

    @Override
    public void accept(InstVisitor visitor) {
        visitor.visit(this);
    }
}
