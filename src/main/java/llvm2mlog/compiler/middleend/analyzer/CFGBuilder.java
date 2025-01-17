package llvm2mlog.compiler.middleend.analyzer;

import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRFunction;
import llvm2mlog.compiler.middleend.llvmir.inst.IRBrInst;
import llvm2mlog.compiler.share.pass.IRFuncPass;

public class CFGBuilder implements IRFuncPass {

    private void init(IRFunction function) {
        for (IRBlock block : function.blocks) {
            block.prevs.clear();
            block.nexts.clear();
        }
    }

    @Override
    public void runOnFunc(IRFunction function) {
        init(function);

        for (IRBlock block : function.blocks) {
            var terminator = block.terminator();
            if (terminator instanceof IRBrInst) {
                if (((IRBrInst) terminator).isJump()) {
                    block.linkBlock(((IRBrInst) terminator).destBlock());
                } else {
                    block.linkBlock(((IRBrInst) terminator).ifTrueBlock());
                    block.linkBlock(((IRBrInst) terminator).ifFalseBlock());
                }
            }
        }
    }
}
