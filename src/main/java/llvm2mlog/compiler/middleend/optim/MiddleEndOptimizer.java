package llvm2mlog.compiler.middleend.optim;

import llvm2mlog.compiler.middleend.analyzer.CallGraphAnalyzer;
import llvm2mlog.compiler.middleend.analyzer.LoopAnalyzer;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRFunction;
import llvm2mlog.compiler.middleend.llvmir.hierarchy.IRModule;
import llvm2mlog.compiler.middleend.optim.ssa.Mem2Reg;
import llvm2mlog.compiler.middleend.optim.ssa.SSADestructor;
import llvm2mlog.compiler.share.pass.IRModulePass;


// Mem2Reg eliminates allocate
// SSADestructor is necessary to eliminate phi
// CFGSimplifier (merge block) must be ahead of SSADestructor for the correct insertion of move

public class MiddleEndOptimizer implements IRModulePass {

    @Override
    public void runOnModule(IRModule module) {

        new CallGraphAnalyzer().runOnModule(module);

        for (IRFunction function : module.functions) {
            new Glo2Loc().runOnFunc(function);
            new Mem2Reg().runOnFunc(function);
        }

        for (int i = 1; i <= 7; i++) {

//          new FuncInliner(false).runOnModule(module);

            for (IRFunction function : module.functions) {
                new CFGSimplifier().runOnFunc(function);
                new GVN().runOnFunc(function);
                new SCCP().runOnFunc(function);
                new ADCE().runOnFunc(function);
                new CFGSimplifier().runOnFunc(function);
                new IVTrans().runOnFunc(function);
                new LICM().runOnFunc(function);
                new LocalMO().runOnFunc(function);
                new CFGSimplifier().runOnFunc(function);
            }
        }

//        new FuncInliner(true).runOnModule(module);

        for (IRFunction function : module.functions) {
            new GVN().runOnFunc(function);
            new CFGSimplifier().runOnFunc(function);
            new ADCE().runOnFunc(function);
            new CFGSimplifier().runOnFunc(function);
            new LICM().runOnFunc(function);
            new CFGSimplifier().runOnFunc(function);
        }

        // re-analyze info for asm
        for (IRFunction function : module.functions) {
            new SSADestructor().runOnFunc(function);
            new CFGSimplifier().runOnFunc(function);
            new LocalMO().runOnFunc(function);
            new TRO().runOnFunc(function);
            new LoopAnalyzer().runOnFunc(function);
//            new InstAdapter().runOnFunc(function);
        }
    }
}