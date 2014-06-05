/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.graph.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.core.astyanax.CassandraConfig;
import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.consistency.TimeServiceImpl;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.GraphManagerImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteListener;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteListenerImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepairImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepairImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeWriteCompact;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeWriteCompactImpl;
import org.apache.usergrid.persistence.graph.impl.stage.NodeDeleteListener;
import org.apache.usergrid.persistence.graph.impl.stage.NodeDeleteListenerImpl;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.MergedEdgeReader;
import org.apache.usergrid.persistence.graph.serialization.impl.MergedEdgeReaderImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.NodeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardCache;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.EdgeShardCounterSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.EdgeShardSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardAllocationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardApproximationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardCacheImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.SizebasedEdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.TimebasedEdgeShardStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.netflix.astyanax.Keyspace;


public class GraphModule extends AbstractModule {

    @Override
    protected void configure() {

        //install our configuration
        install( new GuicyFigModule( GraphFig.class ) );


        bind( EdgeMetadataSerialization.class ).to( EdgeMetadataSerializationImpl.class );
        bind( NodeSerialization.class ).to( NodeSerializationImpl.class );

        bind( TimeService.class ).to( TimeServiceImpl.class );

        // create a guice factory for getting our collection manager
        install( new FactoryModuleBuilder().implement( GraphManager.class, GraphManagerImpl.class )
                                           .build( GraphManagerFactory.class ) );


        /**
         * bindings for shard allocations
         */

        bind( NodeShardAllocation.class ).to( NodeShardAllocationImpl.class );
        bind( NodeShardApproximation.class ).to( NodeShardApproximationImpl.class );
        bind( NodeShardCache.class ).to( NodeShardCacheImpl.class );

        /**
         * Bind our strategies based on their internal annotations.
         */

        bind( EdgeShardSerialization.class ).to( EdgeShardSerializationImpl.class );
        bind( MergedEdgeReader.class ).to( MergedEdgeReaderImpl.class );
        bind( EdgeShardCounterSerialization.class ).to( EdgeShardCounterSerializationImpl.class );


        //Repair/cleanup classes.
        bind( EdgeMetaRepair.class ).to( EdgeMetaRepairImpl.class );
        bind( EdgeDeleteRepair.class ).to( EdgeDeleteRepairImpl.class );
        bind( EdgeWriteCompact.class ).to( EdgeWriteCompactImpl.class );


        /**
         * Bindings to fire up all our message listener implementations.  These will bind their dependencies
         */
//        Multibinder<MessageListener> messageListenerMultibinder =
//                Multibinder.newSetBinder( binder(), MessageListener.class );
//
//        messageListenerMultibinder.addBinding().toProvider( EdgeWriteListenerProvider.class ).asEagerSingleton();
//        messageListenerMultibinder.addBinding().toProvider( EdgeDeleteListenerProvider.class ).asEagerSingleton();
//        messageListenerMultibinder.addBinding().toProvider( NodeDeleteListenerProvider.class ).asEagerSingleton();

        /**
         * Add our listeners
         */
        bind( NodeDeleteListener.class ).to( NodeDeleteListenerImpl.class );
        bind( EdgeDeleteListener.class).to( EdgeDeleteListenerImpl.class );



        /********
         * Migration bindings
         ********/


        //do multibindings for migrations
        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().to( Key.get( NodeSerialization.class ) );
        migrationBinding.addBinding().to( Key.get( EdgeMetadataSerialization.class ) );

        //bind each singleton to the multi set.  Otherwise we won't migrate properly
        migrationBinding.addBinding().to( Key.get( EdgeSerialization.class, StorageEdgeSerialization.class ) );
        migrationBinding.addBinding().to( Key.get( EdgeSerialization.class, CommitLogEdgeSerialization.class ) );

        migrationBinding.addBinding().to( Key.get( EdgeShardSerialization.class ) );
        migrationBinding.addBinding().to( Key.get( EdgeShardCounterSerialization.class ) );
    }


    /**
     * Our permanent serialization strategy
     */
    @Provides
    @Singleton
    @Inject
    @StorageEdgeSerialization
    public EdgeSerialization permanentStorageSerialization( final NodeShardCache cache, final Keyspace keyspace,
                                                            final CassandraConfig cassandraConfig,
                                                            final GraphFig graphFig ) {

        final EdgeShardStrategy sizeBasedStrategy = new SizebasedEdgeShardStrategy( cache );

        final EdgeSerializationImpl edgeSerialization =
                new EdgeSerializationImpl( keyspace, cassandraConfig, graphFig, sizeBasedStrategy );


        return edgeSerialization;
    }


    /**
     * The commit log strategy for fast writes
     */
    @Provides
    @Singleton
    @Inject
    @CommitLogEdgeSerialization
    public EdgeSerialization commitlogStorageSerialization( final NodeShardCache cache, final Keyspace keyspace,
                                                            final CassandraConfig cassandraConfig,
                                                            final GraphFig graphFig ) {

        final EdgeShardStrategy sizeBasedStrategy = new TimebasedEdgeShardStrategy( cache );

        final EdgeSerializationImpl edgeSerialization =
                new EdgeSerializationImpl( keyspace, cassandraConfig, graphFig, sizeBasedStrategy );


        return edgeSerialization;
    }


//
//    /**
//     * Create the provider for the node delete listener
//     */
//    public static class EdgeDeleteListenerProvider
//            implements Provider< MessageListener<EdgeDeleteEvent, EdgeDeleteEvent>> {
//
//
//        private final EdgeDeleteRepair edgeDeleteRepair;
//        private final EdgeMetaRepair edgeMetaRepair;
//        private final AsyncProcessorFactory asyncProcessorFactory;
//
//
//        @Inject
//        public EdgeDeleteListenerProvider(
//                final AsyncProcessorFactory asyncProcessorFactory,
//                                           final EdgeDeleteRepair edgeDeleteRepair, final EdgeMetaRepair edgeMetaRepair) {
//
//
//            this.asyncProcessorFactory = asyncProcessorFactory;
//            this.edgeDeleteRepair = edgeDeleteRepair;
//            this.edgeMetaRepair = edgeMetaRepair;
//        }
//
//
//        @Override
//        public MessageListener<EdgeDeleteEvent, EdgeDeleteEvent> get() {
//            return new EdgeDeleteListener(asyncProcessorFactory, edgeDeleteRepair, edgeMetaRepair );
//        }
//    }
//
//
//
//
//
//    /**
//     * Create the provider for the node delete listener
//     */
//    public static class NodeDeleteListenerProvider implements Provider<MessageListener<EdgeEvent<Id>, Integer>> {
//
//        private final NodeSerialization nodeSerialization;
//        private final EdgeMetadataSerialization edgeMetadataSerialization;
//        private final EdgeMetaRepair edgeMetaRepair;
//        private final GraphFig graphFig;
//        private final AsyncProcessorFactory asyncProcessorFactory;
//        private final EdgeSerialization commitLogSerialization;
//        private final EdgeSerialization storageSerialization;
//        private final MergedEdgeReader mergedEdgeReader;
//        private final Keyspace keyspace;
//
//
//        @Inject
//        public NodeDeleteListenerProvider( final NodeSerialization nodeSerialization,
//                                           final EdgeMetadataSerialization edgeMetadataSerialization,
//                                           final EdgeMetaRepair edgeMetaRepair,
//                                           final GraphFig graphFig,
//                                           final AsyncProcessorFactory asyncProcessorFactory,
//                                           @CommitLogEdgeSerialization final EdgeSerialization commitLogSerialization,
//                                           @StorageEdgeSerialization final EdgeSerialization storageSerialization, final MergedEdgeReader mergedEdgeReader,
//                                           final Keyspace keyspace ) {
//
//            this.nodeSerialization = nodeSerialization;
//            this.edgeMetadataSerialization = edgeMetadataSerialization;
//            this.edgeMetaRepair = edgeMetaRepair;
//            this.graphFig = graphFig;
//            this.asyncProcessorFactory = asyncProcessorFactory;
//            this.commitLogSerialization = commitLogSerialization;
//            this.storageSerialization = storageSerialization;
//            this.mergedEdgeReader = mergedEdgeReader;
//            this.keyspace = keyspace;
//        }
//
//
//        @Override
//        public MessageListener<EdgeEvent<Id>, Integer> get() {
//            return new NodeDeleteListener( nodeSerialization, edgeMetadataSerialization, edgeMetaRepair, graphFig,
//                    asyncProcessorFactory, commitLogSerialization, storageSerialization, mergedEdgeReader, keyspace );
//        }
//    }
//
//
//
//    /**
//     * This is a bit of a pain, see the reference to the provider in the configuration.  This is related to this issue
//     * in Guice
//     *
//     * https://code.google.com/p/google-guice/issues/detail?id=216
//     */
//    public static class EdgeWriteListenerProvider implements Provider<MessageListener<EdgeEvent<MarkedEdge>, Integer>> {
//
//        private final EdgeWriteCompact edgeWriteCompact;
//        private final AsyncProcessor<EdgeEvent<MarkedEdge>> edgeWrite;
//
//
//        @Inject
//        public EdgeWriteListenerProvider( final EdgeWriteCompact edgeWriteCompact,
//                                       @EdgeWrite final AsyncProcessor<EdgeEvent<MarkedEdge>> edgeWrite ) {
//            this.edgeWriteCompact = edgeWriteCompact;
//            this.edgeWrite = edgeWrite;
//        }
//
//
//        @Override
//        public MessageListener<EdgeEvent<MarkedEdge>, Integer> get() {
//            return new EdgeWriteListener( edgeWriteCompact, edgeWrite);
//        }
//    }
}



