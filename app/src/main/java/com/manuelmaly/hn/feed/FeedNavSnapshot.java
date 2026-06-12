package com.manuelmaly.hn.feed;

import com.manuelmaly.hn.model.HNFeed;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A serializable snapshot of {@link FeedNavigationController} state, used to survive
 * configuration changes / process death.
 *
 * <p>Keyed by {@link FeedType#getId()} so it is robust to enum reordering. The scroll values are
 * typed as {@link Object} because the real runtime type is a {@code Parcelable}; the Activity does
 * NOT serialize this snapshot (it puts {@code Parcelable} scroll state and the current id straight
 * into its {@code Bundle}, and reloads post data from the per-feed file cache). Serializability here
 * exists so the controller's {@code save()/restore()} round-trip can be exercised by unit tests with
 * plain serializable tokens.
 */
public class FeedNavSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    public String currentId;
    public Map<String, Object> scrollByFeed = new LinkedHashMap<String, Object>();
    public Map<String, HNFeed> feedByFeed = new LinkedHashMap<String, HNFeed>();
}
