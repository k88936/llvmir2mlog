package Mindustack.compiler.middleend.analyzer;

import Mindustack.compiler.middleend.llvmir.User;
import Mindustack.compiler.middleend.llvmir.constant.GlobalValue;
import Mindustack.compiler.middleend.llvmir.hierarchy.IRModule;
import Mindustack.compiler.middleend.llvmir.inst.IRBaseInst;
import Mindustack.compiler.share.pass.IRModulePass;
import Mindustack.debug.Log;

import java.util.HashMap;

/**
 * this pass intends to find the most frequently used global pointer and mark it
 * in Asm Stage, it will use "gp" to store it
 */

public class GPMark implements IRModulePass {
    private final HashMap<GlobalValue, Integer> useCount = new HashMap<>();

    @Override
    public void runOnModule(IRModule module) {
        module.functions.forEach(func -> new LoopAnalyzer().runOnFunc(func));

        for (GlobalValue glo : module.globalVarSeg) {
            for (User user : glo.users) {
                assert user instanceof IRBaseInst;
                useCount.put(glo, (int) Math.pow(10, ((IRBaseInst) user).parentBlock.loopDepth));
            }
        }

        // string constant not marked

        GlobalValue marked = null;

        for (var entry : useCount.entrySet()) {
            if (marked == null) marked = entry.getKey();
            else {
                if (entry.getValue() > useCount.get(marked)) marked = entry.getKey();
            }
        }

        if (marked != null) {
            Log.info("gp mark: ", marked.identifier());
            marked.gpRegMark = true;
        }
    }
}