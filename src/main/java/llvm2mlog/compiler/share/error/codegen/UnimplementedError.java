package llvm2mlog.compiler.share.error.codegen;

import llvm2mlog.compiler.share.error.CodegenError;

public class UnimplementedError extends CodegenError {
    public static final String unimplemented = "This syntax is unimplemented. Related class: ";

    // public UnimplementedError(CodePos codePos, Object obj) {
    // super(codePos, unimplemented + obj.getClass().getName());
    // }
//
    public UnimplementedError(Object obj) {
        super(unimplemented + obj.getClass().getName());
    }
}
