import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Allocator;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.tools.Builder;


class TestFunction extends FunctionPointer {
    public TestFunction(Pointer p) { super(p); }
    public TestFunction() {  }
    //public TestFunction() { allocate(); }
    //private native void allocate();
    public native int call(int a, int b);
    public native Pointer get();
    public native TestFunction put(Pointer address);
}
