package basictest;

import org.openflexo.pamela.annotations.Getter;
import org.openflexo.pamela.annotations.ModelEntity;
import org.openflexo.pamela.annotations.Setter;

import static org.openflexo.pamela.annotations.Getter.Cardinality.SINGLE;

/**
 * Created by adria on 14/12/2016.
 */
@ModelEntity
interface BasicPamelaEntity1 {

    public static class Toto {
        public int tutu() {
            int a, b, c;
            b = 1;
            c = 2;
            a = b + c;
            return a;
        }
    }
    /**
     * This is a test comment. It should remain on top of the method setMyAttribute.
     *
     * @param myAtt
     */
    @Setter(MYATTRIBUTE)
    void setMyattribute(int myAtt);


    String MYATTRIBUTE="attribute1";
    // in line comment
    @Getter(value=MYATTRIBUTE, cardinality = SINGLE)
    int getMyAttribute();

    /* This is another test */
}
