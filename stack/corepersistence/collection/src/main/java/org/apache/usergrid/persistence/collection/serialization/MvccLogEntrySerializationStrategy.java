package org.apache.usergrid.persistence.collection.serialization;


import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/** The interface that allows us to serialize a log entry to disk */
public interface MvccLogEntrySerializationStrategy {

    /**
     * Serialize the entity to the data store with the given collection context
     *
     * @param entry the entry to write
     *
     * @return The mutation batch with the mutation operations for this write.
     */
    public MutationBatch write( final CollectionScope context, MvccLogEntry entry );

    /**
     * Load and return the stage with the given id and a version that is <= the version provided
     *
     * @param context The context to persist the entity into
     * @param entityId The entity id to load
     * @param version The version to load.  This will return the version == the given version
     *
     * @return The deserialized version of the log entry.  Null if no version == the current version exists
     */
    public MvccLogEntry load( final CollectionScope context, final Id entityId, final UUID version )
            throws ConnectionException;

    /**
     * Load a list, from highest to lowest of the stage with versions <= version up to maxSize elements
     *
     * @param context The context to persist the entity into
     * @param entityId The entity id to load
     * @param version The max version to seek from
     * @param maxSize The maximum size to return.  If you receive this size, there may be more versions to load.
     *
     * @return A list of entities up to max size ordered from max(UUID)=> min(UUID)
     */
    public List<MvccLogEntry> load( CollectionScope context, Id entityId, UUID version, int maxSize )
            throws ConnectionException;

    /**
     * DeleteCommit the stage from the context with the given entityId and version
     *
     * @param context The context that contains the entity
     * @param entityId The entity id to delete
     * @param version The version to delete
     */
    public MutationBatch delete( CollectionScope context, Id entityId, UUID version );
}