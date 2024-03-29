package d13n.repository;

import java.util.NoSuchElementException;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.AbstractPipe;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.gremlin.pipes.transform.BothEdgesPipe;
import com.tinkerpop.gremlin.pipes.transform.BothVerticesPipe;
import com.tinkerpop.gremlin.pipes.transform.InEdgesPipe;
import com.tinkerpop.gremlin.pipes.transform.InVertexPipe;
import com.tinkerpop.gremlin.pipes.transform.OutEdgesPipe;
import com.tinkerpop.gremlin.pipes.transform.OutVertexPipe;
import com.tinkerpop.pipes.util.Pipeline;

public class LabeledEdgePipe extends AbstractPipe<Vertex, Vertex> implements Pipe<Vertex, Vertex> {

    public enum Step {
        OUT_IN, IN_OUT, BOTH_BOTH
    }

    Pipe<Vertex, Vertex> pipe;

    public LabeledEdgePipe(final String label, Step step) {
        super();
        Pipe<Vertex, Edge> edges = null;
        Pipe<Edge, Vertex> vertices = null;
        switch (step) {
        case OUT_IN:
//            edges = new VertexEdgePipe(VertexEdgePipe.Step.OUT_EDGES);
            edges = new OutEdgesPipe(label);
//            vertices = new EdgeVertexPipe(EdgeVertexPipe.Step.IN_VERTEX);
            vertices = new InVertexPipe();
            break;
        case IN_OUT:
//            edges = new VertexEdgePipe(VertexEdgePipe.Step.IN_EDGES);
            edges = new InEdgesPipe(label);
//            vertices = new EdgeVertexPipe(EdgeVertexPipe.Step.OUT_VERTEX);
            vertices = new OutVertexPipe();
            break;
        case BOTH_BOTH:
//            edges = new VertexEdgePipe(VertexEdgePipe.Step.BOTH_EDGES);
            edges = new BothEdgesPipe(label);
//            vertices = new EdgeVertexPipe(EdgeVertexPipe.Step.BOTH_VERTICES);
            vertices = new BothVerticesPipe();
            break;
        default:
            break;
        }
        pipe = new Pipeline<Vertex, Vertex>(edges, vertices);
    }

    @Override
    protected Vertex processNextStart() throws NoSuchElementException {
        pipe.setStarts(this.starts);
        return pipe.next();
    }

}
