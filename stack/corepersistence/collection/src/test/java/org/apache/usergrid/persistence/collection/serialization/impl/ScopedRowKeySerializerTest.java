package org.apache.usergrid.persistence.collection.serialization.impl;


import java.nio.ByteBuffer;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.astynax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.collection.astynax.ScopedRowKey;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class ScopedRowKeySerializerTest {

    @Test
    public void testSerialization() {

        final Id testId = new SimpleId( "scopeType" );
        final String name = "scopeName";
        final Id testKey = new SimpleId( "testKey" );

        final CollectionScope collectionScope = new CollectionScopeImpl( testId, name );
        final ScopedRowKey<CollectionScope, Id> rowKey = new ScopedRowKey<CollectionScope, Id>( collectionScope, testKey );


        ScopedRowKeySerializer<Id> scopedRowKeySerializer = new ScopedRowKeySerializer<Id>( IdRowCompositeSerializer
                .get() );


        ByteBuffer buff = scopedRowKeySerializer.toByteBuffer( rowKey );


        ScopedRowKey<CollectionScope, Id> parsedRowKey = scopedRowKeySerializer.fromByteBuffer( buff );

        assertEquals("Row key serialized correctly", rowKey, parsedRowKey);

    }
}