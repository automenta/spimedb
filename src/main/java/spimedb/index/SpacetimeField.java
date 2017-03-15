package spimedb.index;

import org.apache.lucene.document.DoubleRangeField;
import org.apache.lucene.document.FieldType;
import spimedb.NObject;

/**
 * Created by me on 3/15/17.
 */
public class SpacetimeField extends DoubleRangeField {
    public static final FieldType SPACETIME_FIELD_TYPE;

    static {
        FieldType ft = new FieldType();
        ft.setDimensions(4 /* dims */ * 2, 8);
        ft.setStored(true);
        ft.freeze();
        SPACETIME_FIELD_TYPE = ft;
    }

    public SpacetimeField(double[] min, double[] max) {
        super(NObject.BOUND, min, max);
    }

    @Override
    public FieldType fieldType() {
        return SPACETIME_FIELD_TYPE;
    }
}
