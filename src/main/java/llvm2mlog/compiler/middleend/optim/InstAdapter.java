package llvm2mlog.compiler.middleend.optim;

import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRFunction;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRModule;
import llvm2mlog.compiler.middleend.llvmir.inst.IRBaseInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRCallInst;
import llvm2mlog.compiler.share.pass.IRBlockPass;
import llvm2mlog.compiler.share.pass.IRFuncPass;

import java.util.HashSet;
import java.util.Objects;

/**
 * InstAdapter Pass
 * <p>
 * this pass intends to modify instructions so that it is faster in BackEnd
 * e.g. <= -> <, by let the rhs+1, because <= costs two instructions
 */

public class InstAdapter implements IRFuncPass, IRBlockPass {

    private IRModule module;

    @Override
    public void runOnFunc(IRFunction function) {
        module = function.parentModule;
        function.blocks.forEach(this::runOnBlock);
    }

    @Override
    public void runOnBlock(IRBlock block) {
        var it = block.instructions.listIterator();

        HashSet<IRBaseInst> toRemove = new HashSet<>();

        while (it.hasNext()) {
            var inst = it.next();
            /*
            if (inst instanceof IRICmpInst && (((IRICmpInst) inst).lhs() instanceof NumConst || ((IRICmpInst) inst).rhs() instanceof NumConst)) {
                if (((IRICmpInst) inst).op.equals(LLVM.LessEqualArg)) {
                    ((IRICmpInst) inst).op = LLVM.LessArg;
                    if (((IRICmpInst) inst).rhs() instanceof NumConst)
                        ((NumConst) ((IRICmpInst) inst).rhs()).constData += 1;
                    else
                        ((NumConst) ((IRICmpInst) inst).lhs()).constData -= 1;
                } else if (((IRICmpInst) inst).op.equals(LLVM.GreaterEqualArg)) {
                    ((IRICmpInst) inst).op = LLVM.GreaterArg;
                    if (((IRICmpInst) inst).rhs() instanceof NumConst)
                        ((NumConst) ((IRICmpInst) inst).rhs()).constData -= 1;
                    else
                        ((NumConst) ((IRICmpInst) inst).lhs()).constData += 1;
                }
            }*/

            if (inst instanceof IRCallInst && Objects.equals(((IRCallInst) inst).callFunc(), module.getBuiltinFunction("toString"))) {
                if (inst.users.size() == 1 && inst.users.get(0) instanceof IRCallInst) {
                    if (((IRCallInst) inst.users.get(0)).callFunc() == module.getBuiltinFunction("print")) {
                        inst.removedFromAllUsers();
                        block.tSetByIterator(new IRCallInst(module.getBuiltinFunction("printInt"), null, ((IRCallInst) inst).getArg(0)),
                                it);
                        toRemove.add((IRBaseInst) inst.users.get(0));
                    }
                    if (((IRCallInst) inst.users.get(0)).callFunc() == module.getBuiltinFunction("println")) {
                        inst.removedFromAllUsers();
                        block.tSetByIterator(new IRCallInst(module.getBuiltinFunction("printlnInt"), null, ((IRCallInst) inst).getArg(0)),
                                it);
                        toRemove.add((IRBaseInst) inst.users.get(0));
                    }
                }
            }
        }

        toRemove.forEach(inst -> {
            inst.removedFromAllUsers();
            block.instructions.remove(inst);
        });
    }
}
