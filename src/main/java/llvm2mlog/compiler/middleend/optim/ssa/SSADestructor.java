package llvm2mlog.compiler.middleend.optim.ssa;

import llvm2mlog.compiler.middleend.llvmir.Value;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRFunction;
import llvm2mlog.compiler.middleend.llvmir.inst.IRBrInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRMoveInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRPhiInst;
import llvm2mlog.compiler.share.lang.LLVM;
import llvm2mlog.compiler.share.pass.IRFuncPass;
import llvm2mlog.debug.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class SSADestructor implements IRFuncPass {

    private final HashMap<IRBlock, CopyGraph> copyGraphMap = new HashMap<>();

    // critical path: fromBlock -> toBlock
    // notice: ConcurrentModificationException
    private void criticalEdgeSplit(IRFunction function) {
        ArrayList<IRBlock> midBlockLists = new ArrayList<>();
        HashMap<IRBlock, IRBlock> redirectPreLists = new HashMap<>();
        HashMap<IRBlock, IRBlock> redirectSucLists = new HashMap<>();
        for (IRBlock fromBlock : function.blocks) {
            var successors = fromBlock.nexts;
            if (successors.size() <= 1) continue;


            for (IRBlock toBlock : successors) {
                if (toBlock.prevs.size() <= 1) continue;
                // fromBlock -> midBlock -> toBlock
                IRBlock midBlock = new IRBlock(LLVM.MidBlockLabel, null);

                midBlock.parentFunction = function; // add manually

                midBlockLists.add(midBlock);
                redirectPreLists.put(toBlock, midBlock);
                new IRBrInst(toBlock, midBlock); // jump to toBlock

                midBlock.prevs.add(fromBlock);
                midBlock.nexts.add(toBlock);
                redirectSucLists.put(toBlock, midBlock);
                //toBlock.redirectSucBlock(fromBlock, midBlock);
            }

            redirectSucLists.forEach((toBlock, midBlock) -> toBlock.redirectSucBlock(fromBlock, midBlock));
            redirectSucLists.clear();
            // from block suc fix
            redirectPreLists.forEach(fromBlock::redirectPreBlock);
            redirectPreLists.clear();
        }
        function.blocks.addAll(midBlockLists);
    }

    // resolve phi and generate para copies
    private void buildCopyGraph(IRFunction function) {
        function.blocks.forEach(block -> copyGraphMap.put(block, new CopyGraph()));
        for (IRBlock block : function.blocks) {
            for (IRPhiInst phi : block.phiInsts) {
                for (int i = 0; i < phi.operandSize(); i += 2) {
                    // insert in the graph of prevs
                    copyGraphMap.get((IRBlock) phi.getOperand(i + 1)).
                            insert(new CopyGraph.CopyEdge(phi, phi.getOperand(i)));
                }
            }


        }
    }

    // copy -> move inst
    // notice: ConcurrentModificationException
    // first eliminate all free nodes (whose in_degree in the graph == 0)
    private void eliminateFreeNode(IRBlock block, CopyGraph copyGraph) {
        boolean hasFreeNode = true;
        while (hasFreeNode) {
            hasFreeNode = false;
            for (var it = copyGraph.edges.iterator(); it.hasNext(); ) {
                CopyGraph.CopyEdge nowCopy = it.next();
                if (copyGraph.isFree(nowCopy.dest)) {
                    var move = new IRMoveInst(nowCopy.dest, nowCopy.source, null);// terminated
                    block.tAddBeforeTerminator(move);
                    copyGraph.remove(nowCopy, it);
                    hasFreeNode = true;
                }
            }
        }
    }

    // turn copy to move inst
    // if there is a loop, use a mid dest to break the loop
    public void copyToMove(IRBlock block, CopyGraph copyGraph) {
        boolean loopBreak = true;

        while (loopBreak) {
            loopBreak = false;
            eliminateFreeNode(block, copyGraph);
            for (var it = copyGraph.edges.iterator(); it.hasNext(); ) {
                CopyGraph.CopyEdge nowCopy = it.next();
                if (nowCopy.isLoop()) continue; // move A A
                copyGraph.remove(nowCopy, it);
                Value midDest = new Value(LLVM.Anon, nowCopy.dest.type); // a new dest
                copyGraph.insert(new CopyGraph.CopyEdge(nowCopy.dest, midDest));

                var move = new IRMoveInst(midDest, nowCopy.source, block); // terminated
                block.tAddBeforeTerminator(move);

                loopBreak = true; // after breaking the loop, the graph should be processed again
                break;
            }
        }
    }

    @Override
    public void runOnFunc(IRFunction function) {
        Log.track("SSA Destructor", function.identifier());

        criticalEdgeSplit(function);
        buildCopyGraph(function);
        copyGraphMap.forEach(this::copyToMove);
    }
}
