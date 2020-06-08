import org.bytedeco.javacpp._
import org.bytedeco.javacpp.tools._
import org.bytedeco.llvm.LLVM._
import org.bytedeco.llvm.global.LLVM._

object FacWithVar extends App{


  init()

  def init() = {
    LLVMLinkInMCJIT()

    LLVMInitializeNativeAsmPrinter()
    LLVMInitializeNativeAsmParser()
    LLVMInitializeNativeDisassembler()
    LLVMInitializeNativeTarget()
  }

  //val theContext = LLVMContextCreate()
  //val theModule = LLVMModuleCreateWithNameInContext("my jit", theContext)
  val theModule = LLVMModuleCreateWithName("my jit")
  val fpm = LLVMCreatePassManager()
  val builder = LLVMCreateBuilder()
  val error = new BytePointer(null.asInstanceOf[Pointer]) // Used to retrieve messages from functions
  val engine = new LLVMExecutionEngineRef()


  def close() = {
    LLVMDisposePassManager(fpm)
    LLVMDisposeBuilder(builder)
    LLVMDisposeExecutionEngine(engine)
  }

  def create_fac() = {
    val args: Array[LLVMTypeRef] = Array( LLVMInt32Type() )
    val funtype = LLVMFunctionType( LLVMInt32Type(), new PointerPointer(args:_*), args.size, 0 );
    val fun: LLVMValueRef = LLVMAddFunction(theModule, "myfac", funtype)
    // if else
    // use LLVMBuildCondBr and LLVMBuildFCmp, LLVMBuildICmp
    // factorial a:
    // if a == 0
    //   goto end
    // else
    //   temp = a * fac(a - 1)
    //   goto end
    // end:
    //   return 1 or temp

    val var_a = LLVMGetParam(fun, 0)
    val const0 = LLVMConstInt(LLVMInt32Type(), 0, 1 )
    val const1 = LLVMConstInt(LLVMInt32Type(), 1, 1 )


    val if_b = LLVMAppendBasicBlock(fun, "if_b")
    val then_b = LLVMAppendBasicBlock(fun, "then_b")
    val else_b = LLVMAppendBasicBlock(fun, "else_b")
    val end_b = LLVMAppendBasicBlock(fun, "end_b")


    //------ if block ------
    LLVMPositionBuilderAtEnd(builder, if_b)
    val if_ = LLVMBuildICmp(builder, LLVMIntEQ ,var_a, const0, "cmp" )
    LLVMBuildCondBr(builder, if_, then_b, else_b )

    //------- then block --------
    LLVMPositionBuilderAtEnd(builder, then_b)
    val then_phi: LLVMValueRef = LLVMBuildBr(builder, end_b)

    //------- else block --------
    LLVMPositionBuilderAtEnd(builder, else_b)
    val else_ : LLVMValueRef = {
      val subed = LLVMBuildSub(builder, var_a, const1, "sub")
      val params = new PointerPointer( Seq(subed).toArray:_* )
      val a2 = LLVMBuildCall( builder, fun, params, 1, "call_fun" )
      val mul1 = LLVMBuildMul( builder, var_a, a2, "muled" )
      mul1
    }
    val else_phi: LLVMValueRef = LLVMBuildBr(builder, end_b)

    //------- end block --------
    LLVMPositionBuilderAtEnd(builder, end_b)
    val result_phi: LLVMValueRef = LLVMBuildPhi(builder, LLVMInt32Type(), "result_phi")
    LLVMAddIncoming(result_phi, new PointerPointer( const1, else_), new PointerPointer(  then_b, else_b), 2)

    LLVMBuildRet(builder, result_phi)

    fun // return funciton
  }


  def make_module(f:LLVMValueRef) = {
    LLVMVerifyModule(theModule, LLVMAbortProcessAction, error)
    LLVMDisposeMessage(error) // Handler == LLVMAbortProcessAction -> No need to check errors

    if (LLVMCreateJITCompilerForModule(engine, theModule, 2, error) != 0) {
      System.err.println(error.getString)
      LLVMDisposeMessage(error)
      System.exit(-1)
    }
    val pass = fpm
    LLVMAddConstantPropagationPass(pass)
    LLVMAddInstructionCombiningPass(pass)
    LLVMAddPromoteMemoryToRegisterPass(pass)
    // LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
    LLVMAddGVNPass(pass)
    //LLVMAddCFGSimplificationPass(pass)

    println("before LLVMRunPassManager")
    val x: Int = LLVMRunPassManager(fpm, theModule)
    println("after LLVMRunPassManager")

    println(s"LLVMRunPassManager take effect $x")
    LLVMDumpModule(theModule)


  }

  val f: LLVMValueRef = create_fac()
  make_module(f)


  def runfun(f: LLVMValueRef) = {
    val args: LLVMGenericValueRef = LLVMCreateGenericValueOfInt(LLVMInt32Type(), 10, 0);
    val result = LLVMRunFunction(engine, f, 1, args)
    val v2 = LLVMGenericValueToInt(result, 0)
    println("result is ", v2)
  }

  runfun(f)
  close()

}
