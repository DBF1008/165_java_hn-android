package com.manuelmaly.hn.testutil;

import com.manuelmaly.hn.model.HNFeed;
import com.manuelmaly.hn.model.HNPost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builders for test models. Posts are keyed by a single id used for url/title/
 * author/postID, so two posts built with the same id are {@link HNPost#equals}
 * (which compares author+postID+url) — useful for exercising load-more dedup.
 */
public final class Models {

    private Models() {
    }

    public static HNPost post(String id) {
        return new HNPost("http://" + id, id, id + ".com", id, id, 0, 0, null);
    }

    public static HNFeed feed(List<HNPost> posts, String nextPageUrl) {
        return new HNFeed(new ArrayList<HNPost>(posts), nextPageUrl, "");
    }

    public static HNFeed feed(List<HNPost> posts, String nextPageUrl, String userAcquiredFor) {
        return new HNFeed(new ArrayList<HNPost>(posts), nextPageUrl, userAcquiredFor);
    }

    public static HNFeed feedWith(HNPost... posts) {
        return new HNFeed(new ArrayList<HNPost>(Arrays.asList(posts)), null, "");
    }

    public static List<HNPost> posts(String... ids) {
        List<HNPost> list = new ArrayList<HNPost>();
        for (String id : ids) {
            list.add(post(id));
        }
        return list;
    }
}
