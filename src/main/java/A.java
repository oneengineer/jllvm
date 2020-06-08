public class A {

    int x = 3;

    static void hello(){
        int a =3 ;
        int b= 6;
        b += a = 7;
        System.out.println(a + "  " + b);
    }

    static A geta(){
        A t = new A();
        t.x = 5;
        return t;
    }

//    public static void main(String[] args) {
//        hello();
//        geta() = new A();
//    }

}
