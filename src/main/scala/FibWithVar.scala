import org.bytedeco.javacpp._
import org.bytedeco.javacpp.tools._
import org.bytedeco.llvm.LLVM._
import org.bytedeco.llvm.global.LLVM._


object FibWithVar extends App{

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

  val llvm_printf = {
    val char_star = LLVMPointerType(LLVMInt8Type(), 0)
    val args: Array[LLVMTypeRef] = Array( char_star )
    val funtype = LLVMFunctionType( LLVMInt32Type(), new PointerPointer(args:_*), args.size, 1 );
    val fun: LLVMValueRef = LLVMAddFunction(theModule, "printf", funtype)
    fun
  }

  def create_fac() = {
    val args: Array[LLVMTypeRef] = Array( LLVMInt32Type() )
    val funtype = LLVMFunctionType( LLVMInt32Type(), new PointerPointer(args:_*), args.size, 0 );
    val fun: LLVMValueRef = LLVMAddFunction(theModule, "myfac", funtype)

    /**
     *    seq [a,b,c]
     * if left <=2
     *    return 1
     * left = n - 2
     * while (left > 0){
     * c = a + b
     * a = b
     * b = c
     * left -= 1
     * }
     * return c
     *
     *
     * //translate while
     *
     * while_condition:
     *    x = ???
     *    branch x end_of_while
     *    do content in while
     * end_of_while:
     *
     */

    val var_a = LLVMGetParam(fun, 0)
    val const0 = LLVMConstInt(LLVMInt32Type(), 0, 1 )
    val const1 = LLVMConstInt(LLVMInt32Type(), 1, 1 )
    val const2 = LLVMConstInt(LLVMInt32Type(), 2, 1 )
    val const3 = LLVMConstInt(LLVMInt32Type(), 3, 1 )
    val const0_64 = LLVMConstInt(LLVMInt64Type(), 0, 1 )

    val if_b = LLVMAppendBasicBlock(fun, "if_b")
    val then_b = LLVMAppendBasicBlock(fun, "then_b")
    val else_b = LLVMAppendBasicBlock(fun, "else_b")

    val while_b = LLVMAppendBasicBlock(fun, "while_b")
    val while_content_b = LLVMAppendBasicBlock(fun, "while_content_b")
    //val while_end_b = LLVMAppendBasicBlock(fun, "while_end_b")

    val end_b = LLVMAppendBasicBlock(fun, "end_b")

    //---------- beginning block -----------
    LLVMPositionBuilderAtEnd( builder, if_b )

    // init const global
    val str = "hello world\n\0"
    val str2 = "hello: %d !\n\0"

    val ptrType = LLVMArrayType( LLVMInt8Type(),str.length )

    val ptrType2 = LLVMArrayType( LLVMInt8Type(),str2.length )

    //val const_text = LLVMAddGlobal(theModule, LLVMInt8Type(), "global_text")
    val const_text = LLVMAddGlobal(theModule, ptrType, "global_text")
    val constant_init = LLVMConstString(str, str.length, 1)
    LLVMSetInitializer(const_text, constant_init)

    val const_text2 = LLVMAddGlobal(theModule, ptrType2, "global_text")
    val constant_init2 = LLVMConstString(str2, str2.length, 1)
    LLVMSetInitializer(const_text2, constant_init2)


    def call_helloworld() = {
      val indicies = new PointerPointer(const0_64, const0_64) //64 bit ints
      val converted_p = LLVMConstInBoundsGEP(const_text, indicies, 2) //TODO
      val params3 = new PointerPointer(Seq(converted_p).toArray: _*)
      LLVMBuildCall(builder, llvm_printf, params3, 1, "called") // debug
    }

    def call_helloworld2(ref: LLVMValueRef) = {
      val indicies = new PointerPointer(const0_64, const0_64) //64 bit ints
      val converted_p = LLVMConstInBoundsGEP(const_text2, indicies, 2) //TODO
      val params3 = new PointerPointer(Seq(converted_p, ref).toArray: _*)
      LLVMBuildCall(builder, llvm_printf, params3, 2, "called") // debug
    }

    //------------------------

    //call_helloworld()

    val a = LLVMBuildAlloca( builder, LLVMInt32Type(), "a" )
    val b = LLVMBuildAlloca( builder, LLVMInt32Type(), "b" )
    val c = LLVMBuildAlloca( builder, LLVMInt32Type(), "c" )
    val steps = LLVMBuildAlloca( builder, LLVMInt32Type(), "steps" )


    LLVMBuildStore(builder, const1, b)
    LLVMBuildStore(builder, const1, a)

    val if_ = LLVMBuildICmp(builder, LLVMIntSLT ,var_a, const3, "cmp" )
    LLVMBuildCondBr(builder, if_, then_b, else_b )

    // ---------- then block ------
    LLVMPositionBuilderAtEnd( builder, then_b )

    LLVMBuildRet(builder, const3)



    // ---------- else block ------
    LLVMPositionBuilderAtEnd( builder, else_b )

    val temp = LLVMBuildSub( builder, var_a, const2, "temp")
    LLVMBuildStore(builder, temp, steps)

    LLVMBuildBr(builder, while_b)

    // -------- while block --------
    LLVMPositionBuilderAtEnd( builder, while_b )

    val temp_step = LLVMBuildLoad(builder, steps, "temp2")
    call_helloworld2( temp_step )

    val if2_ = LLVMBuildICmp(builder, LLVMIntSGT ,temp_step, const0, "cmp" )
    LLVMBuildCondBr(builder, if2_, while_content_b, end_b )

    // -------- while content block --------
    LLVMPositionBuilderAtEnd( builder, while_content_b )
    val temp_a = LLVMBuildLoad(builder, a, "tempa")
    val temp_b = LLVMBuildLoad(builder, b, "tempb")
    val temp_c = LLVMBuildAdd(builder, temp_a, temp_b, "tempc")
    call_helloworld2( temp_c )

    LLVMBuildStore(builder, temp_c, b)
    LLVMBuildStore(builder, temp_b, a)
    val step2 = LLVMBuildSub(builder, temp_step, const1, "step2")

    val params = new PointerPointer( Seq(const_text).toArray:_* )
    val params2 = new PointerPointer( Seq(constant_init).toArray:_* )

    //LLVMBuildCall(builder, llvm_printf, params,1, "called")// debug
    // convert global text to pointer debug

    call_helloworld()

    LLVMBuildStore(builder, step2, steps)
    LLVMBuildBr(builder, while_b)// jmp back

    // ------- end block --------
    LLVMPositionBuilderAtEnd( builder, end_b )
    val temp_c2 = LLVMBuildLoad(builder, b,"c2")
    LLVMBuildRet(builder, temp_c2)

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
    //LLVMAddInstructionCombiningPass(pass)
    //LLVMAddPromoteMemoryToRegisterPass(pass)
    // LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
   // LLVMAddGVNPass(pass)
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
    val args: LLVMGenericValueRef = LLVMCreateGenericValueOfInt(LLVMInt32Type(), 8, 0);
    val result = LLVMRunFunction(engine, f, 1, args)
    val v2 = LLVMGenericValueToInt(result, 0)
    println("result is ", v2)
  }

  runfun(f)
  close()
}
