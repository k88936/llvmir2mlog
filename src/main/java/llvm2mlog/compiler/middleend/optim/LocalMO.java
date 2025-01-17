package llvm2mlog.compiler.middleend.optim;

import llvm2mlog.compiler.middleend.analyzer.AliasAnalyzer;
import llvm2mlog.compiler.middleend.analyzer.CFGBuilder;
import llvm2mlog.compiler.middleend.analyzer.DomTreeBuilder;
import llvm2mlog.compiler.middleend.llvmir.Value;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRFunction;
import llvm2mlog.compiler.middleend.llvmir.inst.IRBaseInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRCallInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRLoadInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRStoreInst;
import llvm2mlog.compiler.share.pass.IRBlockPass;
import llvm2mlog.compiler.share.pass.IRFuncPass;
import llvm2mlog.debug.Log;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Local Memory-related Optimization Pass
 * <p>
 * this pass intends to modify instructions so that it is faster in BackEnd
 */

public class LocalMO implements IRFuncPass, IRBlockPass {

    AliasAnalyzer analyzer = new AliasAnalyzer();

    // load match load, store match store
    // invaildate:
    // if there is a store, invalidate store and load.
    // load: write after read, store: write after write
    HashSet<IRLoadInst> loadRecord = new HashSet<>();
    HashSet<IRStoreInst> storeRecord = new HashSet<>();

    private Value recordMatch(IRBaseInst inst) {
        if (inst instanceof IRLoadInst) {
            // storeRecord.forEach(st -> Log.info(st.format()));

            for (IRLoadInst load : loadRecord)
                if (load.loadPtr() == ((IRLoadInst) inst).loadPtr())
                    return load;
            for (IRStoreInst store : storeRecord) {
                // Log.info(store.format());
                if (store.storePtr() == ((IRLoadInst) inst).loadPtr())
                    return store.storeValue();
            }
        } else if (inst instanceof IRStoreInst) {
            for (IRStoreInst store : storeRecord)
                if (store.storePtr() == ((IRStoreInst) inst).storePtr() &&
                        store.storeValue().equals(((IRStoreInst) inst).storeValue()))
                    return store;
        }

        return null;
    }

    private void invalidate(IRBaseInst inst) {
        if (inst instanceof IRStoreInst) {
            HashSet<IRLoadInst> toRemoveL = new HashSet<>();
            HashSet<IRStoreInst> toRemoveS = new HashSet<>();
            for (IRLoadInst load : loadRecord)
                if (analyzer.mayAlias(((IRStoreInst) inst).storePtr(), load.loadPtr())) toRemoveL.add(load);
            for (IRStoreInst store : storeRecord)
                if (analyzer.mayAlias(store.storePtr(), ((IRStoreInst) inst).storePtr())) toRemoveS.add(store);
            loadRecord.removeAll(toRemoveL);
            storeRecord.removeAll(toRemoveS);
        } else if (inst instanceof IRCallInst) {
            loadRecord.clear();
            storeRecord.clear();
        }
    }

    @Override
    public void runOnFunc(IRFunction function) {
        Log.track("local mem opt", function.identifier());

        analyzer.runOnFunc(function);
        new CFGBuilder().runOnFunc(function);
        new DomTreeBuilder(false).runOnFunc(function);

        function.blocks.forEach(this::runOnBlock);
    }

    @Override
    public void runOnBlock(IRBlock block) {
        loadRecord.clear();
        storeRecord.clear();

        HashSet<IRBlock> domeds = new HashSet<>();
        HashSet<IRBlock> visited = new HashSet<>();
        Queue<IRBlock> workQueue = new LinkedList<>(block.nexts);

        while (!workQueue.isEmpty()) {
            var qHead = workQueue.poll();
            if (visited.contains(qHead)) continue;
            visited.add(qHead);
            if (qHead.dtNode.doms.contains(block.dtNode)) {
                domeds.add(qHead);
                qHead.nexts.forEach(workQueue::offer);
            }
        }

        if (block.prevs.size() == 1 && (block.dtNode.idom != null && block.dtNode.idom.fromBlock == block.prevs.get(0))) {
            // Log.info(block.identifier(), block.dtNode.idom.fromBlock.identifier());

            for (IRBaseInst inst : block.dtNode.idom.fromBlock.instructions) {
                invalidate(inst);
                if (inst instanceof IRLoadInst) loadRecord.add((IRLoadInst) inst);
                else if (inst instanceof IRStoreInst) storeRecord.add((IRStoreInst) inst);
            }
        }

        var it = block.instructions.listIterator();
        // Log.info(block.identifier());
        while (it.hasNext()) {
            IRBaseInst inst = it.next();

            if (inst instanceof IRLoadInst) {
                var replace = recordMatch(inst);
                if (replace != null) {
                    it.remove();
                    inst.replaceAllUsesWith(replace);
                } else {
                    loadRecord.add((IRLoadInst) inst);
                }
            } else if (inst instanceof IRStoreInst) {
                var replace = recordMatch(inst);
                if (replace != null) {
                    it.remove();
                } else {
                    invalidate(inst);
                    storeRecord.add((IRStoreInst) inst);
                }
            } else if (inst instanceof IRCallInst) {
                invalidate(inst);
            }
        }

        // very, very, conservative
        domeds.forEach(domed -> domed.instructions.forEach(
                inst -> {
                    if (inst instanceof IRStoreInst || inst instanceof IRCallInst) {
                        invalidate(inst);
                    }
                }
        ));

        for (IRBlock domed : domeds) {
            it = domed.instructions.listIterator();
            while (it.hasNext()) {
                IRBaseInst inst = it.next();
                if (inst instanceof IRLoadInst) {
                    var replace = recordMatch(inst);
                    if (replace != null) {
                        it.remove();
                        inst.replaceAllUsesWith(replace);
                    }
                } else if (inst instanceof IRStoreInst || inst instanceof IRCallInst) {
                    break;
                }
            }
        }
    }
}
