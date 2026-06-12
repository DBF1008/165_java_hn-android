package com.manuelmaly.hn.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link Resource} status wrapper.
 */
public class ResourceTest {

    @Test
    public void success_createsCorrectResource() {
        Resource<String> resource = Resource.success("data");
        assertEquals(Resource.Status.SUCCESS, resource.status);
        assertEquals("data", resource.data);
        assertEquals(null, resource.message);
    }

    @Test
    public void error_createsCorrectResource() {
        Resource<String> resource = Resource.error("error message", "partial");
        assertEquals(Resource.Status.ERROR, resource.status);
        assertEquals("partial", resource.data);
        assertEquals("error message", resource.message);
    }

    @Test
    public void loading_createsCorrectResource() {
        Resource<String> resource = Resource.loading(null);
        assertEquals(Resource.Status.LOADING, resource.status);
        assertEquals(null, resource.data);
        assertEquals(null, resource.message);
    }

    @Test
    public void loading_withExistingData() {
        Resource<String> resource = Resource.loading("stale");
        assertEquals(Resource.Status.LOADING, resource.status);
        assertEquals("stale", resource.data);
    }

    @Test
    public void error_withNullData() {
        Resource<String> resource = Resource.error("fail", null);
        assertEquals(Resource.Status.ERROR, resource.status);
        assertEquals(null, resource.data);
        assertEquals("fail", resource.message);
    }
}
