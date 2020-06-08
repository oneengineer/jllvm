import org.bytedeco.javacpp._
import org.bytedeco.llvm.LLVM._
import org.bytedeco.llvm.global.LLVM._

object LLVMS1 extends App{


  def dowork() = {
    val error = new BytePointer(null.asInstanceOf[Pointer]) // Used to retrieve messages from functions
    LLVMLinkInMCJIT()
    LLVMInitializeNativeAsmPrinter
    LLVMInitializeNativeAsmParser
    LLVMInitializeNativeDisassembler
    LLVMInitializeNativeTarget
    val mod = LLVMModuleCreateWithName("fac_module")
    val fac_args = Array(LLVMInt32Type)
    val fac = LLVMAddFunction(mod, "fac", LLVMFunctionType(LLVMInt32Type, fac_args(0), 1, 0))
    LLVMSetFunctionCallConv(fac, LLVMCCallConv)
    val n = LLVMGetParam(fac, 0)

    val entry = LLVMAppendBasicBlock(fac, "entry")
    val iftrue = LLVMAppendBasicBlock(fac, "iftrue")
    val iffalse = LLVMAppendBasicBlock(fac, "iffalse")
    val end = LLVMAppendBasicBlock(fac, "end")
    val builder = LLVMCreateBuilder

    LLVMPositionBuilderAtEnd(builder, entry)
    val If = LLVMBuildICmp(builder, LLVMIntEQ, n, LLVMConstInt(LLVMInt32Type, 0, 0), "n == 0")
    LLVMBuildCondBr(builder, If, iftrue, iffalse)

    LLVMPositionBuilderAtEnd(builder, iftrue)
    val res_iftrue = LLVMConstInt(LLVMInt32Type, 1, 0)
    LLVMBuildBr(builder, end)

    LLVMPositionBuilderAtEnd(builder, iffalse)
    val n_minus = LLVMBuildSub(builder, n, LLVMConstInt(LLVMInt32Type, 1, 0), "n - 1")
    val call_fac_args = Array(n_minus)
    val call_fac = LLVMBuildCall(builder, fac, new PointerPointer(call_fac_args:_*), 1, "fac(n - 1)")
    val res_iffalse = LLVMBuildMul(builder, n, call_fac, "n * fac(n - 1)")
    LLVMBuildBr(builder, end)

    LLVMPositionBuilderAtEnd(builder, end)
    val res = LLVMBuildPhi(builder, LLVMInt32Type, "result")
    val phi_vals = Array(res_iftrue, res_iffalse)
    val phi_blocks = Array(iftrue, iffalse)
    LLVMAddIncoming(res, new PointerPointer(phi_vals:_*), new PointerPointer(phi_blocks:_*), 2)
    LLVMBuildRet(builder, res)

    LLVMVerifyModule(mod, LLVMAbortProcessAction, error)
    LLVMDisposeMessage(error) // Handler == LLVMAbortProcessAction -> No need to check errors


    val engine = new LLVMExecutionEngineRef
    if (LLVMCreateJITCompilerForModule(engine, mod, 2, error) != 0) {
      System.err.println(error.getString)
      LLVMDisposeMessage(error)
      System.exit(-1)
    }

    val pass = LLVMCreatePassManager
    LLVMAddConstantPropagationPass(pass)
    LLVMAddInstructionCombiningPass(pass)
    LLVMAddPromoteMemoryToRegisterPass(pass)
    // LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
    LLVMAddGVNPass(pass)
    LLVMAddCFGSimplificationPass(pass)
    LLVMRunPassManager(pass, mod)
    LLVMDumpModule(mod)

    val exec_args = LLVMCreateGenericValueOfInt(LLVMInt32Type, 10, 0)
    val exec_res: LLVMGenericValueRef = LLVMRunFunction(engine, fac, 1, exec_args)
    System.err.println()
    System.err.println("; Running fac(10) with JIT...")
    System.err.println("; Result: " + LLVMGenericValueToInt(exec_res, 0))

    LLVMDisposePassManager(pass)
    LLVMDisposeBuilder(builder)
    LLVMDisposeExecutionEngine(engine)
  }

  dowork()

}
