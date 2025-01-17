package llvm2mlog.compiler.middleend.llvmir.type;

public class VoidType extends IRBaseType {
    @Override
    public boolean match(IRBaseType other) {
        return other instanceof VoidType;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return "void";
    }
}
