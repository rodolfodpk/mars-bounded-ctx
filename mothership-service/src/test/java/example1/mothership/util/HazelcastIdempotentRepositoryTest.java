package example1.mothership.util;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class HazelcastIdempotentRepositoryTest extends CamelTestSupport {

    private IMap<String, Boolean> cache;
    private HazelcastIdempotentRepository repo;
    private HazelcastInstance hazelcastInstance;

    private String key01 = "123";
    private String key02 = "456";

    public void setUp() throws Exception {
        hazelcastInstance = Hazelcast.newHazelcastInstance(null);
        cache = hazelcastInstance.getMap("myRepo");
        repo = new HazelcastIdempotentRepository(hazelcastInstance, "myRepo");
        super.setUp();
        cache.clear();
        repo.start();
    }

    public void tearDown() throws Exception {
        repo.stop();
        super.tearDown();
        cache.clear();
        hazelcastInstance.getLifecycleService().shutdown();
    }

    @Test
    public void testAdd() throws Exception {
        // add first key
        assertTrue(repo.add(key01));
        assertTrue(cache.containsKey(key01));

        // try to add the same key again
        assertFalse(repo.add(key01));
        assertEquals(1, cache.size());

        // try to add an other one
        assertTrue(repo.add(key02));
        assertEquals(2, cache.size());
    }

    @Test
    public void testConfirm() throws Exception {
        // add first key and confirm
        assertTrue(repo.add(key01));
        assertTrue(repo.confirm(key01));

        // try to confirm a key that isn't there
        assertFalse(repo.confirm(key02));
    }

    @Test
    public void testContains() throws Exception {
        assertFalse(repo.contains(key01));

        // add key and check again
        assertTrue(repo.add(key01));
        assertTrue(repo.contains(key01));

    }

    @Test
    public void testRemove() throws Exception {
        // add key to remove
        assertTrue(repo.add(key01));
        assertTrue(repo.add(key02));
        assertEquals(2, cache.size());

        // clear repo
        repo.clear();
        assertEquals(0, cache.size());
    }

    @Test
    public void testClear() throws Exception {
        // add key to remove
        assertTrue(repo.add(key01));
        assertEquals(1, cache.size());

        // remove key
        assertTrue(repo.remove(key01));
        assertEquals(0, cache.size());

        // try to remove a key that isn't there
        assertFalse(repo.remove(key02));
    }

    @Test
    public void testRepositoryInRoute() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:out");
        mock.expectedBodiesReceived("a", "b");
        // c is a duplicate

        // should be started
        assertEquals("Should be started", true, repo.getStatus().isStarted());

        // send 3 message with one duplicated key (key01)
        template.sendBodyAndHeader("direct://in", "a", "messageId", key01);
        template.sendBodyAndHeader("direct://in", "b", "messageId", key02);
        template.sendBodyAndHeader("direct://in", "c", "messageId", key01);

        assertMockEndpointsSatisfied();
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct://in")
                        .idempotentConsumer(header("messageId"), repo)
                        .to("mock://out");
            }
        };
    }

}