package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.graph.AdjacencyListsGraph;
import org.onlab.graph.DijkstraGraphSearch;
import org.onlab.graph.Graph;
import org.onlab.graph.GraphPathSearch;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.topology.ClusterId;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.TopoEdge;
import org.onlab.onos.net.topology.TopoVertex;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyDescription;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.graph.GraphPathSearch.Result;
import static org.onlab.onos.net.Link.Type.INDIRECT;

/**
 * Default implementation of an immutable topology data carrier.
 */
class DefaultTopologyDescription implements TopologyDescription {

    private static final GraphPathSearch<TopoVertex, TopoEdge> DIJKSTRA =
            new DijkstraGraphSearch<>();

    private final long nanos;
    private final Map<DeviceId, TopoVertex> vertexesById = Maps.newHashMap();
    private final Graph<TopoVertex, TopoEdge> graph;
    private final Map<DeviceId, Result<TopoVertex, TopoEdge>> results;
    private final Map<ClusterId, TopologyCluster> clusters;
//    private final Multimap<ClusterId, DeviceId> clusterDevices;
//    private final Multimap<ClusterId, Link> clusterLinks;
//    private final Map<DeviceId, TopologyCluster> deviceClusters;


    DefaultTopologyDescription(long nanos, Iterable<Device> devices, Iterable<Link> links) {
        this.nanos = nanos;
        this.graph = buildGraph(devices, links);
        this.results = computeDefaultPaths();
        this.clusters = computeClusters();
//        this.clusterDevices = clusterDevices;
//        this.clusterLinks = clusterLinks;
//        this.deviceClusters = deviceClusters;
    }

    // Constructs the topology graph using the supplied devices and links.
    private Graph<TopoVertex, TopoEdge> buildGraph(Iterable<Device> devices,
                                                   Iterable<Link> links) {
        Graph<TopoVertex, TopoEdge> graph =
                new AdjacencyListsGraph<>(buildVertexes(devices),
                                          buildEdges(links));
        return graph;
    }

    // Builds a set of topology vertexes from the specified list of devices
    private Set<TopoVertex> buildVertexes(Iterable<Device> devices) {
        Set<TopoVertex> vertexes = Sets.newHashSet();
        for (Device device : devices) {
            TopoVertex vertex = new TVertex(device.id());
            vertexesById.put(vertex.deviceId(), vertex);
            vertexes.add(vertex);
        }
        return vertexes;
    }

    // Builds a set of topology vertexes from the specified list of links
    private Set<TopoEdge> buildEdges(Iterable<Link> links) {
        Set<TopoEdge> edges = Sets.newHashSet();
        for (Link link : links) {
            edges.add(new TEdge(vertexOf(link.src()), vertexOf(link.dst()), link));
        }
        return edges;
    }

    // Computes the default shortest paths for all source/dest pairs using
    // the multi-path Dijkstra and hop-count as path cost.
    private Map<DeviceId, Result<TopoVertex, TopoEdge>> computeDefaultPaths() {
        LinkWeight weight = new HopCountLinkWeight(graph.getVertexes().size());
        Map<DeviceId, Result<TopoVertex, TopoEdge>> results = Maps.newHashMap();

        // Search graph paths for each source to all destinations.
        for (TopoVertex src : vertexesById.values()) {
            results.put(src.deviceId(), DIJKSTRA.search(graph, src, null, weight));
        }
        return results;
    }

    // Computes topology SCC clusters using Tarjan algorithm.
    private Map<ClusterId, TopologyCluster> computeClusters() {
        Map<ClusterId, TopologyCluster> clusters = Maps.newHashMap();
        return clusters;
    }

    // Fetches a vertex corresponding to the given connection point device.
    private TopoVertex vertexOf(ConnectPoint connectPoint) {
        DeviceId id = connectPoint.deviceId();
        TopoVertex vertex = vertexesById.get(id);
        if (vertex == null) {
            // If vertex does not exist, create one and register it.
            vertex = new TVertex(id);
            vertexesById.put(id, vertex);
        }
        return vertex;
    }

    @Override
    public long timestamp() {
        return nanos;
    }

    @Override
    public Graph<TopoVertex, TopoEdge> graph() {
        return graph;
    }

    @Override
    public Result<TopoVertex, TopoEdge> pathResults(DeviceId srcDeviceId) {
        return results.get(srcDeviceId);
    }

    @Override
    public Set<TopologyCluster> clusters() {
        return ImmutableSet.copyOf(clusters.values());
    }

    @Override
    public Set<DeviceId> clusterDevices(TopologyCluster cluster) {
        return null; // clusterDevices.get(cluster.id());
    }

    @Override
    public Set<Link> clusterLinks(TopologyCluster cluster) {
        return null; // clusterLinks.get(cluster.id());
    }

    @Override
    public TopologyCluster clusterFor(DeviceId deviceId) {
        return null; // deviceClusters.get(deviceId);
    }

    // Implementation of the topology vertex backed by a device id
    private static class TVertex implements TopoVertex {

        private final DeviceId deviceId;

        public TVertex(DeviceId deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public DeviceId deviceId() {
            return deviceId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TVertex) {
                final TVertex other = (TVertex) obj;
                return Objects.equals(this.deviceId, other.deviceId);
            }
            return false;
        }

        @Override
        public String toString() {
            return deviceId.toString();
        }
    }

    // Implementation of the topology edge backed by a link
    private class TEdge implements TopoEdge {
        private final Link link;
        private final TopoVertex src;
        private final TopoVertex dst;

        public TEdge(TopoVertex src, TopoVertex dst, Link link) {
            this.src = src;
            this.dst = dst;
            this.link = link;
        }

        @Override
        public Link link() {
            return link;
        }

        @Override
        public TopoVertex src() {
            return src;
        }

        @Override
        public TopoVertex dst() {
            return dst;
        }

        @Override
        public int hashCode() {
            return Objects.hash(link);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TEdge) {
                final TEdge other = (TEdge) obj;
                return Objects.equals(this.link, other.link);
            }
            return false;
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("src", src).add("dst", dst).toString();
        }
    }

    // Link weight for measuring link cost as hop count with indirect links
    // being as expensive as traversing the entire graph to assume the worst.
    private class HopCountLinkWeight implements LinkWeight {
        private final int indirectLinkCost;

        public HopCountLinkWeight(int indirectLinkCost) {
            this.indirectLinkCost = indirectLinkCost;
        }

        @Override
        public double weight(TopoEdge edge) {
            // To force preference to use direct paths first, make indirect
            // links as expensive as the linear vertex traversal.
            return edge.link().type() == INDIRECT ? indirectLinkCost : 1;
        }
    }

}