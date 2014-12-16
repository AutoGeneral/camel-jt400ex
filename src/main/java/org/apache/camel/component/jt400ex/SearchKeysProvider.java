package org.apache.camel.component.jt400ex;

import java.util.List;

/**
 * Defines the interface that accepts new keys and returns all keys to be
 * consumed off a JT400 keyed data queue.
 *
 *
 */
public interface SearchKeysProvider {
    /**
     *
     * @param key A key to be consumed off a keyed data queue
     */
    void addKey(final String key);

    /**
     *
     * @param key A key to be consumed off a keyed data queue
     */
    void removeKey(final String key);

    /**
     *
     * @return A list of the current keys
     */
    List<String> getKeys();
}
