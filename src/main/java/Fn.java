import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.llvm.LLVM.LLVMGenericValueRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class Fn extends FunctionPointer {
        /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
        public    Fn(Pointer p) { super(p); }
        public    Fn(Long addr) {
            this.address = addr;
        }

        //protected Fn() { allocate(); }
        //private native void allocate();
        public native int call( int a,
                                 int b);

    }

//    public class LLVMDiagnosticHandler extends FunctionPointer {
//        static { Loader.load(); }
//        /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
//        public    LLVMDiagnosticHandler(Pointer p) { super(p); }
//        protected LLVMDiagnosticHandler() { allocate(); }
//        private native void allocate();
//        public native void call(LLVMDiagnosticInfoRef arg0, Pointer arg1);
//    }