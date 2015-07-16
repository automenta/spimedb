package automenta.netention.db;

import java.io.Serializable;
import java.util.Set;

/**
 * A container for vertex edges.
 *
 * <p>In this edge container we use array lists to minimize memory toll.
 * However, for high-degree vertices we replace the entire edge container
 * with a direct access subclass (to be implemented).</p>
 *
 * @author Barak Naveh
 */
class VertexContainer<C, EE> implements Serializable
{
    private static final long serialVersionUID = 7494242245729767106L;
    final Set<EE> incoming;
    final Set<EE> outgoing;
    private C value;


    public VertexContainer(C value, Set<EE> incoming, Set<EE> outgoing) {
        this.incoming = incoming;
        this.outgoing = outgoing;
        this.value = value;
    }

    public C getValue() {
        return value;
    }

    public <V> void setValue(C value) {
        this.value = value;
    }

    /**
     * .
     *
     * @param e
     */
    public void addIncomingEdge(EE e)
    {
        incoming.add(e);
    }

    /**
     * .
     *
     * @param e
     */
    public void addOutgoingEdge(EE e)
    {
        outgoing.add(e);
    }

    /**
     * .
     *
     * @param e
     */
    public void removeIncomingEdge(EE e)
    {
        incoming.remove(e);
    }

    /**
     * .
     *
     * @param e
     */
    public void removeOutgoingEdge(EE e)
    {
        outgoing.remove(e);
    }


}
