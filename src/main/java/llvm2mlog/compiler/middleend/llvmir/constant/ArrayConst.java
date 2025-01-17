package llvm2mlog.compiler.middleend.llvmir.constant;

import llvm2mlog.compiler.middleend.llvmir.type.ArrayType;
import llvm2mlog.compiler.middleend.llvmir.type.IRBaseType;
import llvm2mlog.compiler.share.lang.LLVM;

import java.util.ArrayList;

public class ArrayConst extends BaseConst {
    public ArrayList<BaseConst> constData;

    public ArrayConst(IRBaseType type, int length) {
        super(LLVM.ConstAnon, new ArrayType(type, length));
        constData = new ArrayList<>();
    }

    @Override
    public int size() {
        return this.type.size();
    }
}
