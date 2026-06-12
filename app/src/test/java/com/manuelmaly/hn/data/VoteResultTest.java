package com.manuelmaly.hn.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuelmaly.hn.model.HNPost;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link VoteResult}.
 */
public class VoteResultTest {

    @Test
    public void success_result() {
        Object item = new Object();
        VoteResult result = new VoteResult(true, item);
        assertTrue(result.success);
        assertEquals(item, result.votedItem);
    }

    @Test
    public void failure_result() {
        VoteResult result = new VoteResult(false, null);
        assertFalse(result.success);
        assertEquals(null, result.votedItem);
    }
}
