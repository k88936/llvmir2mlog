package llvm2mlog.compiler.middleend.optim;

import llvm2mlog.compiler.middleend.analyzer.CFGBuilder;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRFunction;
import llvm2mlog.compiler.middleend.llvmir.inst.IRBaseInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRBrInst;
import llvm2mlog.compiler.share.pass.IRFuncPass;
import llvm2mlog.debug.Log;

import java.util.HashSet;

/**
 * This pass simplifies CFG (control flow graph) by:
 * <p>
 * 1. remove unreachable block
 * 2. merge blocks
 *
 * @requirement: CFGBuilder
 */

public class CFGSimplifier implements IRFuncPass {
    private boolean removeUnreachableBlock(IRFunction function) {
        HashSet<IRBlock> toRemoveSet = new HashSet<>();

        boolean removed = true;
        while (removed) {
            removed = false;
            for (IRBlock block : function.blocks) {
                if (block == function.entryBlock) continue;

                if (toRemoveSet.contains(block)) continue;

                if (block.prevs.isEmpty()) {
                    toRemoveSet.add(block);
                    for (IRBlock suc : block.nexts) {
                        suc.prevs.remove(block);
                    }
                    removed = true;
                }
            }
        }

        for (IRBlock toRemove : toRemoveSet) {
            assert toRemove != function.exitBlock;

            // Log.info("removeUnreachable", toRemove.identifier());

            function.blocks.remove(toRemove);

            for (IRBlock suc : toRemove.nexts) {
                suc.prevs.remove(toRemove);
                suc.removePhiBranch(toRemove);
            }

            toRemove.prevs.clear();
            toRemove.nexts.clear();
        }

        return toRemoveSet.size() > 1;
    }

    private boolean mergeBlocks(IRFunction function) {
        HashSet<IRBlock> toMergeSet = new HashSet<>(); // merge with pre

        function.blocks.forEach(block -> {
            if (block.prevs.size() == 1 && block.prevs.get(0).nexts.size() == 1) {
                assert block.prevs.get(0).nexts.get(0) == block;
                toMergeSet.add(block);
            }
        });

        boolean changed = false;

        while (!toMergeSet.isEmpty()) {
            var it = toMergeSet.iterator();
            while (it.hasNext()) {
                var beMerged = it.next();
                IRBlock preBlock = beMerged.prevs.get(0);
                if (toMergeSet.contains(preBlock)) continue;

                // Log.info("merged", preBlock.identifier(), beMerged.identifier());
                // beMerged.nexts.forEach(suc -> Log.info(suc.identifier()));

                var preTerminator = preBlock.terminator();

                // 1 nexts, must be Jmp
                assert preTerminator instanceof IRBrInst && ((IRBrInst) preTerminator).isJump();

                preBlock.nexts.remove(beMerged);
                preBlock.instructions.remove(preTerminator);
                preBlock.isTerminated = false;

                preBlock.nexts.addAll(beMerged.nexts);
                for (IRBlock suc : beMerged.nexts) {
                    suc.redirectPreBlock(beMerged, preBlock);
                }

                // will it have phi?
                for (IRBaseInst phi : beMerged.phiInsts) phi.setParentBlock(preBlock);

                // terminate it also
                for (IRBaseInst inst : beMerged.instructions) inst.setParentBlock(preBlock);

                if (function.exitBlock == beMerged) function.exitBlock = preBlock;

                function.blocks.remove(beMerged);
                it.remove();

                changed = true;
            }
        }

        return changed;
    }

    @Override
    public void runOnFunc(IRFunction function) {
        Log.track("CFG Simplifier", function.identifier());
        boolean changed = true;

        while (changed) {
            new CFGBuilder().runOnFunc(function);
            changed = mergeBlocks(function);
            changed |= removeUnreachableBlock(function);
        }
    }
}
