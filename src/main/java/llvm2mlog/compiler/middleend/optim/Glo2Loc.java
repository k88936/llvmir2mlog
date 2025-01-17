package llvm2mlog.compiler.middleend.optim;

import llvm2mlog.compiler.middleend.llvmir.User;
import llvm2mlog.compiler.middleend.llvmir.Value;
import llvm2mlog.compiler.middleend.llvmir.constant.GlobalVariable;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRBlock;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRFunction;
import llvm2mlog.compiler.middleend.llvmir.inst.IRAllocaInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRBaseInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRLoadInst;
import llvm2mlog.compiler.middleend.llvmir.inst.IRStoreInst;
import llvm2mlog.compiler.middleend.llvmir.type.PointerType;
import llvm2mlog.compiler.share.lang.LLVM;
import llvm2mlog.compiler.share.pass.IRFuncPass;
import llvm2mlog.debug.Log;

import java.util.*;

/**
 * Glo2Loc Pass:
 * <p>
 * if a GlobalVariable is used many times in a function, localize it.
 * also, if a GlobalVariable will not be modified in the program, replace it with a constant
 * localize can be beneficial to optimization
 * <p>
 * works well in the following case:
 * int N;
 * void func() {
 * for (int i = 1; i < N; i++)
 * ...
 * }
 *
 * @requirement: CallGraphAnalyzer. Run before Mem2Reg because it introduces alloc
 */

public class Glo2Loc implements IRFuncPass {

    // if a global variable isn't used many times, not localize it because not worthy
    public static final int UsageThreshold = 1;

    private final Map<GlobalVariable, Integer> refTimes = new HashMap<>();
    private final Set<GlobalVariable> ableSet = new HashSet<>();
    private final Set<GlobalVariable> constAbleSet = new HashSet<>();

    private boolean constantDetect(GlobalVariable global) {
        if (global.initValue == null) return false;
        for (User use : global.users) {
            if (use instanceof IRStoreInst &&
                    !Objects.equals(((IRStoreInst) use).parentBlock.parentFunction.name, LLVM.InitFuncName)) {
                return false;
            }
        }
        return true;
    }

    private void collectAbleSet(IRFunction function) {

        for (IRBlock block : function.blocks)
            for (IRBaseInst inst : block.instructions) {

                Value global;

                if (inst instanceof IRLoadInst) {
                    global = ((IRLoadInst) inst).loadPtr();
                } else if (inst instanceof IRStoreInst) {
                    global = ((IRStoreInst) inst).storePtr();
                } else {
                    continue;
                }

                if (global instanceof GlobalVariable) {
                    refTimes.putIfAbsent((GlobalVariable) global, 1);
                    refTimes.put((GlobalVariable) global, refTimes.get(global) + 1);
                }
            }

        //  If a global variable is used in the callee function, it is likely it will be modified,
        //  and that causes side effects.

        for (var global : refTimes.keySet()) {
            // Log.info("ref times", global.identifier(), refTimes.get(global));

            if (constantDetect(global)) constAbleSet.add(global);
            else if (refTimes.get(global) >= UsageThreshold) {
                boolean check = true;
                for (IRFunction callee : function.node.callee) {
                    if (callee.node.glbUses.contains(global)) {
                        Log.info("can not", global.identifier(), callee.identifier());
                        check = false;
                        break;
                    }
                }
                if (check) ableSet.add(global);
            }
        }
    }

    @Override
    public void runOnFunc(IRFunction function) {
        if (function.node.cyclic) return;
        if (function.name.equals(LLVM.InitFuncName)) return;

        collectAbleSet(function);

        Log.mark("glo2loc able set: " + function.identifier());
        ableSet.forEach(glb -> Log.info(glb.identifier()));
        constAbleSet.forEach(glb -> Log.info(glb.identifier()));

        for (GlobalVariable global : ableSet) {
            IRBaseInst initLoad = new IRLoadInst(global, null),
                    initAlloc = new IRAllocaInst(LLVM.LocalPrefix + global.name, ((PointerType) global.type).pointedType, null),
                    initStore = new IRStoreInst(initAlloc, initLoad, null);

            // Notice that "tAddFirst" method is like the stack
            // Therefore the correct order of these three insts is:
            // 1. alloc space for local
            // 2. load value from global
            // 3. store value to local

            function.entryBlock.tAddFirst(initStore);
            function.entryBlock.tAddFirst(initLoad);
            function.entryBlock.tAddFirst(initAlloc);

            IRBaseInst writeBackLoad = null, writeBackStore = null;
            // if the function doesn't modify it, there is no need to store back
            if (function.node.glbDefs.contains(global)) {
                writeBackLoad = new IRLoadInst(initAlloc, null);
                writeBackStore = new IRStoreInst(global, writeBackLoad, null);
                function.exitBlock.tAddFirst(writeBackStore);
                function.exitBlock.tAddFirst(writeBackLoad);
            }

            for (IRBlock block : function.blocks)
                for (IRBaseInst inst : block.instructions) {
                    if (inst instanceof IRLoadInst && global == ((IRLoadInst) inst).loadPtr() &&
                            inst != initLoad && inst != writeBackLoad) {
                        ((IRLoadInst) inst).replacePtr(initAlloc);
                    } else if (inst instanceof IRStoreInst && global == ((IRStoreInst) inst).storePtr() &&
                            inst != initStore && inst != writeBackStore) {
                        ((IRStoreInst) inst).replacePtr(initAlloc);
                    }
                }
        }

        for (GlobalVariable global : constAbleSet) {
            // replace all load with initValue, remove all loads
            // remove the only store

            for (User use : global.users) {
                assert use instanceof IRBaseInst;
                if (use instanceof IRLoadInst) use.replaceAllUsesWith(global.initValue);
                ((IRBaseInst) use).parentBlock.instructions.remove(use);
            }
        }
    }
}
