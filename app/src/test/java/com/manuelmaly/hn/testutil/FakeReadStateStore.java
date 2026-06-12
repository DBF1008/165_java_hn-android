package com.manuelmaly.hn.testutil;

import com.manuelmaly.hn.data.IReadStateStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** In-memory {@link IReadStateStore} for tests. */
public class FakeReadStateStore implements IReadStateStore {

    public final Set<Integer> hashes = new HashSet<Integer>();
    public final List<String> marked = new ArrayList<String>();

    @Override
    public Set<Integer> loadReadTitleHashes() {
        return new HashSet<Integer>(hashes);
    }

    @Override
    public void markRead(String title) {
        marked.add(title);
    }
}
