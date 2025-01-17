package llvm2mlog.compiler.backend.rvasm;

import llvm2mlog.compiler.backend.rvasm.hierarchy.AsmBlock;
import llvm2mlog.compiler.backend.rvasm.hierarchy.AsmFunction;
import llvm2mlog.compiler.backend.rvasm.inst.AsmLiInst;
import llvm2mlog.compiler.backend.rvasm.inst.AsmLoadInst;
import llvm2mlog.compiler.backend.rvasm.operand.*;
import llvm2mlog.compiler.middleend.llvmir.Value;
import llvm2mlog.compiler.middleend.llvmir.constant.BoolConst;
import llvm2mlog.compiler.middleend.llvmir.constant.NullptrConst;
import llvm2mlog.compiler.middleend.llvmir.constant.NumConst;
import llvm2mlog.compiler.share.error.codegen.UnimplementedError;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AsmCurrent {

    public AsmBlock block;
    public AsmFunction func;

    public Map<Integer, Register> recordLi = new HashMap<>();

    public Immediate toImm(Value value) {
        if (value.asmOperand instanceof RawStackOffset || value.asmOperand instanceof RawMemOffset)
            return (Immediate) value.asmOperand;

        if (value instanceof NumConst) return new Immediate(((NumConst) value).getConstData());
        if (value instanceof BoolConst) return new Immediate(((BoolConst) value).constData ? 1 : 0);
        throw new UnimplementedError(this);
    }

    public Immediate toImm(int value) {
      //  if (value < -1 * RV32I.ImmBound || value > RV32I.ImmBound)
        //    throw new InternalError("invalid immediate detected");
        return new Immediate(value);
    }

    public double toRawData(Value value) {
        // if (value.asmOperand instanceof RawStackOffset) return (Immediate) value.asmOperand;
        if (value instanceof NumConst) return ((NumConst) value).getConstData();
        if (value instanceof BoolConst) return ((BoolConst) value).constData ? 1 : 0;
        throw new UnimplementedError(this);
    }

    public Register toReg(@NotNull Value value) {
        if (value.asmOperand != null) {

//            if (value.asmOperand instanceof RawStackOffset) {
//
//                var virtualReg = new VirtualReg();
//
//                var virtualReg1 = new VirtualReg();
//                new AsmALUInst(MLOG.SubOperation, virtualReg, PhysicalReg.reg("fp"),
//                        ((RawStackOffset) value.asmOperand), this.block);
//                new AsmLoadInst(1, virtualReg1, virtualReg
//                        , new Immediate(0), this.block);
//                return virtualReg1;
//            }
            if (value.asmOperand instanceof RawMemOffset) {
//
                var virtualReg = new VirtualReg();
//
//                var virtualReg1 = new VirtualReg();
//                new AsmALUInst(MLOG.SubOperation, virtualReg, PhysicalReg.reg("fp"),
//                        ((RawStackOffset) value.asmOperand), this.block);
                new AsmLoadInst(1, virtualReg, PhysicalReg.reg("zero")
                        , toImm(value), this.block);
                return virtualReg;
            }
            return (Register) value.asmOperand;
        }
        Integer intValue = null;
        if (value instanceof NumConst) intValue = ((NumConst) value).getConstData();
        else if (value instanceof BoolConst) intValue = ((BoolConst) value).constData ? 1 : 0;
        else if (value instanceof NullptrConst) intValue = 0;

        if (intValue != null && intValue == 0) {
            value.asmOperand = PhysicalReg.reg("zero");
            return PhysicalReg.reg("zero");
        }

        Register ret;

        if (recordLi.containsKey(intValue)) {
            ret = recordLi.get(intValue);
        } else {
            ret = new VirtualReg(value.type.size());
            if (intValue != null) {
                new AsmLiInst(ret, new Immediate(intValue), this.block);
                recordLi.put(intValue, ret);
            }
        }

        // const info is memorized by Li
        if (!(value instanceof NumConst || value instanceof BoolConst)) value.asmOperand = ret;
        return ret;
    }
}
