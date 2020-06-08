import org.bytedeco.javacpp._
import org.bytedeco.javacpp.tools._

import org.bytedeco.llvm.LLVM._
import org.bytedeco.llvm.global.LLVM._

object try1 extends App {

  init()

  //val theContext = LLVMContextCreate()
  //val theModule = LLVMModuleCreateWithNameInContext("my jit", theContext)
  val theModule = LLVMModuleCreateWithName("my jit")
  val fpm = LLVMCreatePassManager()
  val builder = LLVMCreateBuilder()
  val error = new BytePointer(null.asInstanceOf[Pointer]) // Used to retrieve messages from functions
  val engine = new LLVMExecutionEngineRef()


  def fun1(): LLVMValueRef = {
    val args: Array[LLVMTypeRef] = Array( LLVMInt32Type() )
    val funtype = LLVMFunctionType( LLVMInt32Type(), new PointerPointer(args:_*), args.size, 0 );
    val fun: LLVMValueRef = LLVMAddFunction(theModule, "fun_a_plus_b", funtype)

    LLVMSetFunctionCallConv(fun, LLVMCCallConv) // TODO what is this

    val b1: LLVMBasicBlockRef = LLVMAppendBasicBlock(fun,"fun_bb")
    LLVMPositionBuilderAtEnd(builder, b1)

    val a: LLVMValueRef = LLVMGetParam(fun, 0)

    val constV = LLVMConstInt( LLVMInt32Type(), 3, 1 )

    val c = LLVMBuildAdd( builder, a, constV, "added"  )
    LLVMBuildRet( builder, c )
    fun
  }

  def fun2(): LLVMValueRef = {
    val args: Array[LLVMTypeRef] = Array( LLVMInt32Type() , LLVMInt32Type() )
    val funtype = LLVMFunctionType( LLVMInt32Type(), new PointerPointer(args:_*), args.size, 0 );
    val fun: LLVMValueRef = LLVMAddFunction(theModule, "fun_a_plus_b", funtype)

    LLVMSetFunctionCallConv(fun, LLVMCCallConv) // TODO what is this

    val b1: LLVMBasicBlockRef = LLVMAppendBasicBlock(fun,"fun_bb")
    LLVMPositionBuilderAtEnd(builder, b1)

    val a: LLVMValueRef = LLVMGetParam(fun, 0)
    val b: LLVMValueRef = LLVMGetParam(fun, 1)

    val c = LLVMBuildAdd( builder, a, b, "added"  )
    LLVMBuildRet( builder, c )
    fun
  }

  def init() = {
    LLVMLinkInMCJIT()

    LLVMInitializeNativeAsmPrinter()
    LLVMInitializeNativeAsmParser()
    LLVMInitializeNativeDisassembler()
    LLVMInitializeNativeTarget()
  }

  /**
   *
   * @return function of LLVMValueRef
   */
  def create_ast() = {
    val f1 = fun1()
    val s = LLVMPrintValueToString(f1).getString
    println(s)
    f1
  }


  def close() = {
    LLVMDisposePassManager(fpm)
    LLVMDisposeBuilder(builder)
    LLVMDisposeExecutionEngine(engine)
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
    LLVMAddCFGSimplificationPass(pass)

    println("before LLVMRunPassManager")
    val x: Int = LLVMRunPassManager(fpm, theModule)
    println("after LLVMRunPassManager")

    println(s"LLVMRunPassManager take effect $x")
    LLVMDumpModule(theModule)

  }



  def run_fun(f:LLVMValueRef) = {

    val a: LLVMGenericValueRef = LLVMCreateGenericValueOfInt(LLVMInt32Type, -3, 1)
    val b = LLVMCreateGenericValueOfInt(LLVMInt32Type, 5, 1)
    //val result = LLVMCreateGenericValueOfInt(LLVMInt32Type, 5, 1)

    //LLVMInitializeMCJITCompilerOptions()

    val arr = Seq(a,b).toArray
    //val result: LLVMGenericValueRef = LLVMRunFunction(engine, f, 2, new PointerPointer(arr:_*))
    val result: LLVMGenericValueRef = LLVMRunFunction(engine, f, 1, a)

    //val funAddr: Long = LLVMGetFunctionAddress(engine, "fun_a_plus_b")

    // int (*sum_func)(int, int) = (int (*)(int, int))LLVMGetFunctionAddress(engine, "sum");
    //val fn = new Fn( funAddr )

    //val address = new Pointer { address = funAddr }
    //val fn = new TestFunction().put( address )

    //val resultInt = fn.call(7, 9)

    val resultInt: Long = LLVMGenericValueToInt(result,1)
    println(resultInt)
  }

  /**
   *
   * manual create ir, and functions
   * create module an dit
   *
   * run ir to get address
   *
   */


  //init()
  val f1 = create_ast()
  make_module(f1)
  run_fun(f1)
  close()

}
