/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package deliverif.app.model.graph;

/**
 * Represents an edge between two vertex on a graph
 * @author zakaria
 */
public class Edge {

    // Source vertex of edge is implicit
    /**
     * Destination vertex
     */
    public Vertex dest;  // Index of destination vertex of this edge
    /**
     * Edge's cost
     */
    public float cost;  // Cost (weight) of this edge

    /**
     * Create an edge from one vertex to another
     *
     * @param d the destination vertex
     * @param c edge cost
     */
    public Edge(Vertex d, float c) {
        dest = d;
        cost = c;
    }

}
