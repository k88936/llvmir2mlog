package Mindustack.compiler.backend.optim;

import Mindustack.compiler.backend.rvasm.hierarchy.AsmBlock;
import Mindustack.compiler.backend.rvasm.hierarchy.AsmFunction;
import Mindustack.compiler.backend.rvasm.inst.AsmALUInst;
import Mindustack.compiler.backend.rvasm.inst.AsmBaseInst;
import Mindustack.compiler.backend.rvasm.operand.PhysicalReg;
import Mindustack.compiler.share.lang.MLOG;
import Mindustack.compiler.share.pass.AsmFuncPass;

public class ZeroInstPeephole implements AsmFuncPass {

    @Override
    public void runOnFunc(AsmFunction function) {

        for (AsmBlock block : function.blocks) {
            var it = block.instructions.iterator();

            while (it.hasNext()) {
                AsmBaseInst inst = it.next();

                if (inst instanceof AsmALUInst) {
                    String op = ((AsmALUInst) inst).op;

                    switch (op) {
                        case MLOG.AddOperation:
                        case MLOG.SubOperation:
                        case MLOG.OrOperation:
                        case MLOG.XorOperation:
                        case MLOG.ShiftLeftOperation:
                        case MLOG.ShiftRightOperation: {
                            if (inst.rd.color == inst.rs1.color) {
                                if (inst.imm != null && inst.imm.value == 0) it.remove();
                                if (inst.rs2 != null && inst.rs2.color == PhysicalReg.reg("zero")) it.remove();
                            }
                        }
                    }
                }
            }
        }

    }
}
