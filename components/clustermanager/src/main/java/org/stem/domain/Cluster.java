/*
 * Copyright 2014 Alexey Plotnik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stem.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stem.ClusterManagerDaemon;
import org.stem.RestUtils;
import org.stem.api.REST;
import org.stem.coordination.*;
import org.stem.domain.topology.Partitioner;
import org.stem.domain.topology.TopologyChangesListener;
import org.stem.domain.topology.TopologyEventListener;
import org.stem.exceptions.StemException;
import org.stem.exceptions.TopologyException;
import org.stem.streaming.StreamSession;
import org.stem.utils.TopologyUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// TODO: Init zookeeper client when cluster has been started?
public class Cluster {

    private static final Logger logger = LoggerFactory.getLogger(Cluster.class);

    public static enum State {
        UNINITIALIZED, INITIALIZING, INITIALIZED
    }

    public static final Cluster instance = new Cluster();

    final Manager manager;
    Descriptor descriptor;

    @Deprecated
    Topology topology; // TODO: get rid of entirely

    org.stem.domain.topology.Topology topology2;    // TODO: load topology from Zookeeper
    Partitioner partitioner;
    private Unauthorized freshNodes = new Unauthorized(this);

    private Cluster() {
        try {
            String zookeeperEndpoint = ClusterManagerDaemon.zookeeperEndpoint();
            this.manager = new Manager(zookeeperEndpoint);
            this.topology2 = org.stem.domain.topology.Topology.Factory.create(this);
        } catch (ZooException e) {
            throw new StemException("Can't initialize Cluster.Manager instance", e);
        }
    }

    void addStorageNode(org.stem.domain.topology.Topology.StorageNode node, String dcName, String rackName) {
        if (null != topology2.findStorageNode(node.id))
            throw new TopologyException(String.format("Node with id=%s already exist in cluster", node.id));

        org.stem.domain.topology.Topology.Datacenter datacenter = topology2.findDatacenter(dcName);
        if (null == datacenter) {
            if (topology2.dataCenters().isEmpty()) {
                datacenter = new org.stem.domain.topology.Topology.Datacenter(dcName);
                topology2.addDatacenter(datacenter);
            } else
                throw new TopologyException(String.format("Datacenter '%s' can not be found", dcName));
        }

        org.stem.domain.topology.Topology.Rack rack = topology2.findRack(datacenter, rackName);
        if (null == rack) {
            org.stem.domain.topology.Topology.Rack newRack = new org.stem.domain.topology.Topology.Rack(rackName);
            datacenter.addRack(newRack);
            rack = newRack;
        }

        rack.addStorageNode(node);
        // TODO: I'm 100% sure there should be much more arbitrary checks and validations

    }

    public Unauthorized unauthorized() {
        return freshNodes;
    }

    public static Cluster instance() {
        return instance;
    }

    private AtomicReference<State> state = new AtomicReference<>(State.UNINITIALIZED);

    /**
     * Initialize cluster by loading it's configuration from Zookeeper database
     *
     * @return boolean whether the cluster initialized
     */
    public boolean load() {
        try {
            if (!manager.loadCluster()) {
                return false; // Already initialized
            }
            // TODO: manager.loadUnauthorized();
        } catch (Exception e) {
            state.set(State.UNINITIALIZED);
            throw new StemException("Error while loading cluster configuration", e);
        }
        return true;
    }

    public void initialize(String name, int vBuckets, int rf, String partitioner) {
        try {
            manager.ensureUninitialized();
            Descriptor desc = new Descriptor(name, vBuckets, rf, manager.endpoint, Partitioner.Type.byName(partitioner));
            manager.newCluster(desc);
        } catch (Exception e) {
            state.compareAndSet(State.INITIALIZING, State.UNINITIALIZED);
            throw new StemException(String.format("Error while initializing a new cluster: %s", e.getMessage()), e);
        }
    }

    public void save() {
        try {
            manager.save();
        } catch (Exception e) {
            throw new StemException("Can not save cluster configuration", e);
        }
    }

    public void destroy() {
        // TODO: implement
        /**
         if (null != cluster.get()) {
         client.close();
         cluster.set(null);
         }
         */
    }

    public boolean initialized() {
        return state.get() == State.INITIALIZED;
    }

    public Cluster ensureInitialized() {
        manager.ensureInitialized();
        return this;
    }

    public State state() {
        return state.get();
    }

    public Descriptor descriptor() {
        manager.ensureInitialized();
        return descriptor;
    }

    public org.stem.domain.topology.Topology topology() {
        return topology2;
    }

    private static StemException produceInitError(State state) {
        switch (state) {
            case INITIALIZING:
                return new StemException("Cluster is initializing");
            case INITIALIZED:
                return new StemException("Cluster has already been initialized");
            default:
                return new StemException("Unknown race error while initializing cluster"); // We should not get here
        }
    }

    public synchronized void addStorageIfNotExist(StorageNode storage)  // replace synchronized with Lock
    {
        try {
            manager.addStorageIfNotExist(storage);
        } catch (Exception e) {
            throw new StemException("Can nod add storage node", e);
        }
    }

    public Collection<StorageNode> getStorageNodes() {
        return topology.getStorages();
    }

    public long getUsedBytes() {
        long sum = 0;
        for (StorageNode node : getStorageNodes()) {
            sum += node.getUsedBytes();
        }
        return sum;
    }

    public long getTotalBytes() {
        long sum = 0;
        for (StorageNode node : getStorageNodes()) {
            sum += node.getTotalBytes();
        }
        return sum;
    }

    public synchronized void computeMapping() // TODO: synchronized is BAD
    {
        try {
            manager.computeMapping();
        } catch (Exception e) {
            throw new StemException("Compute mapping failed", e);
        }
    }

    public void updateStat(StorageStat stat) {
        if (topology.storageExists(stat.getEndpoint())) {
            StorageNode node = topology.getStorage(stat.getEndpoint());
            node.setDisks(stat.getDisks()); // TODO: Check disks existence
        }
    }

    public TopologyEventListener topologyAutoSaver() {
        return manager.topologyPersistingListener;
    }

    /**
     *
     */
    class Manager {

        final String endpoint;
        private ZookeeperClient client;

        final TopologyEventListener topologyPersistingListener;

        public Manager(String endpoint) throws ZooException {
            this.endpoint = endpoint;
            client = ZookeeperClientFactory.newClient(endpoint);

            topologyPersistingListener = new TopologyChangesListener() {
                @Override
                public void onTopologyUpdated(org.stem.domain.topology.Topology.Node node) {
                    try {
                        saveTopology();
                    } catch (Exception e) {
                        logger.error("Failed to save topology to Zookeeper");
                    }
                }
            };
        }

        private void saveTopology() throws Exception {
            client.updateNode(ZookeeperPaths.CLUSTER, RestUtils.packTopology(topology2));

        }

        synchronized boolean loadCluster() throws Exception {
            if (!state.compareAndSet(State.UNINITIALIZED, State.INITIALIZING)) {
                // We race with another initialization, it's ok
                throw produceInitError(state.get());
            }
            ensureInitializing();

            tryStartZookeeperClient();

            Descriptor persisted = readDescriptor();
            if (null == persisted) {
                state.set(State.UNINITIALIZED);
                return false;
            }

            validate(persisted);
            descriptor = persisted;
            topology = new Topology(descriptor.name, descriptor.rf);  // TODO: Load topology from zookeeper !!!!!!
            org.stem.domain.topology.Topology persistedTopo = readTopology();
            if (null != persistedTopo) {
                register(persistedTopo);
            }

            initZookeeperPaths();
            state.set(State.INITIALIZED);

            startListenForStats();
            return true;
        }

        private void register(org.stem.domain.topology.Topology persistedTopo) {
            topology2 = persistedTopo;
            topology2.setOwner(Cluster.this);
        }

        // TODO: persist and restore nodes and disks states (Topology.NodeState, Topology.DiskState enums)
        // TODO: turn off listeners until we load topology ???
        private org.stem.domain.topology.Topology readTopology() throws Exception {

            REST.Topology topologyTransient = client.readZNodeData(ZookeeperPaths.CLUSTER_TOPOLOGY_PATH, REST.Topology.class);
            if (null == topologyTransient)
                return null;

            org.stem.domain.topology.Topology result = org.stem.domain.topology.Topology.Factory.create();
            for (org.stem.domain.topology.Topology.Datacenter datacenter : RestUtils.extractDataCenters(topologyTransient)) {
                result.addDatacenter(datacenter);
            }

            return result; // return standalone topology, need to register it on cluster
        }

        synchronized void newCluster(Descriptor newDescriptor) throws Exception {
            if (!state.compareAndSet(State.UNINITIALIZED, State.INITIALIZING)) {
                // We race with another initialization, it's ok
                throw produceInitError(state.get());
            }

            ensureInitializing();

            tryStartZookeeperClient();

            validate(newDescriptor);
            descriptor = newDescriptor;
            topology = new Topology(descriptor.name, descriptor.rf);
            partitioner = descriptor.partitioner.builder.build();

            initZookeeperPaths();
            state.set(State.INITIALIZED);

            startListenForStats();
        }

        private void startListenForStats() throws Exception {
            client.listenForChildren(ZookeeperPaths.CLUSTER, new StorageStatListener());
        }


        synchronized public void save() throws Exception {
            ensureInitialized();
            tryStartZookeeperClient();
            client.createNode(ZookeeperPaths.CLUSTER, descriptor);
        }

        private void addStorageIfNotExist(StorageNode storage) throws Exception {
            ensureInitialized();

            if (!topology.storageExists(storage)) {
                topology.addStorage(storage);

                // TODO: StorageNode vs. StorageStat vs. JoinRequest = combine ?

                StorageStat nodeStat = new StorageStat(storage.getIpAddress(), storage.getPort());
                for (Disk disk : storage.getDisks()) {
                    DiskStat diskStat = new DiskStat(disk.getId());
                    diskStat.setPath(disk.getPath());
                    diskStat.setTotalBytes(disk.getTotalBytes());
                    diskStat.setUsedBytes(disk.getUsedBytes());
                    nodeStat.getDisks().add(diskStat);
                }

                client.createNodeIfNotExists(ZookeeperPaths.CLUSTER, nodeStat);
            }
            // TODO: 1. Handle the situation when storage already exists but new disk were added
            // TODO: 2. Handle the situation when storage is new but its disks are already attached to another storage
            // TODO:    (maybe disk was moved)
        }

        private void computeMapping() throws Exception {
            ensureInitialized();

            topology.computeMappings(descriptor.vBuckets);
            TopoMapping topoMap = TopologyUtils.buildTopoMap(topology);
            client.updateNode(ZookeeperPaths.TOPOLOGY, topoMap);

            List<StreamSession> sessions = topology.computeStreamingSessions();

            // TODO: Anything below is not a part og this method, it should be passed to somewhere like SessionManager
            for (StreamSession s : sessions) {
                client.createNodeIfNotExists(ZookeeperPaths.OUT_SESSIONS, s);
            }
        }

        private void ensureUninitialized() {
            if (Cluster.this.state.get() != State.UNINITIALIZED)
                throw new StemException("Cluster has already been initialized");
        }

        private void ensureInitializing() {
            if (Cluster.this.state.get() != State.INITIALIZING)
                throw new StemException("Cluster has already been initialized");
        }


        private void ensureInitialized() {
            if (Cluster.this.state.get() != State.INITIALIZED)
                throw new StemException("Cluster has not been initialized yet");
        }

        private Descriptor readDescriptor() throws Exception {
            Descriptor descriptor = client.readZNodeData(ZookeeperPaths.CLUSTER_DESCRIPTOR_PATH, Descriptor.class);
            return descriptor;
        }

        private void tryStartZookeeperClient() throws ZooException {
            if (null == client) {
                client = ZookeeperClientFactory.newClient(endpoint);
            }

            if (!client.isUninitialized()) {
                client.close();
                client = ZookeeperClientFactory.newClient(endpoint);
            }
        }

        private void initZookeeperPaths() throws Exception {
            //client.createIfNotExists(ZooConstants.TOPOLOGY + "/" + ZooConstants.TOPO_MAP);
            client.createNodeIfNotExists(ZookeeperPaths.TOPOLOGY, new TopoMapping());
            client.createIfNotExists(ZookeeperPaths.OUT_SESSIONS);
        }

        private void validate(Descriptor desc) {
            validate(desc.name, desc.vBuckets, desc.rf, desc.zookeeperEndpoint);
        }

        private void validate(String name, int vBuckets, int rf, String zookeeperEndpoint) {
            if (null == name)
                throw new StemException("Cluster name is null"); // TODO: use ValidationException

            if (name.length() > 100)
                throw new StemException("Cluster name must be less than 100 symbols");

            if (vBuckets <= 0)
                throw new StemException("Number of virtual buckets must be greater than zero");

            if (rf <= 0)
                throw new StemException("Replication factor must be greater than zero");

            if (null == zookeeperEndpoint || zookeeperEndpoint.isEmpty())
                throw new StemException("Replication factor must be greater than zero");
        }

        public ZookeeperClient getZookeeperClient() {
            return client;
        }
    }

    /**
     *
     */
    public static class Descriptor extends ZNodeAbstract {

        String name;
        int vBuckets; // TODO: rename to partitions
        int rf;
        Partitioner.Type partitioner = Partitioner.Type.CRUSH;
        String zookeeperEndpoint;

        public Descriptor() {
        }

        public String getName() {
            return name;
        }

        public int getvBuckets() {
            return vBuckets;
        }

        public int getRf() {
            return rf;
        }

        public Partitioner.Type getPartitioner() {
            return partitioner;
        }

        public String getZookeeperEndpoint() {
            return zookeeperEndpoint;
        }

        public Descriptor(String name, int vBuckets, int rf, String zookeeperEndpoint, Partitioner.Type partitioner) {
            this.name = name;
            this.vBuckets = vBuckets;
            this.rf = rf;
            this.zookeeperEndpoint = zookeeperEndpoint;
            this.partitioner = partitioner;
        }

        @Override
        public String name() {
            return ZookeeperPaths.CLUSTER_DESCRIPTOR_NAME;
        }
    }

    @Override
    public String toString() {
        return "Cluster \"" + descriptor.name + "\"";
    }
}
