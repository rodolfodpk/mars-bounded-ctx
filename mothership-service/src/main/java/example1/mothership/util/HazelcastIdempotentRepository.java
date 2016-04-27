package example1.mothership.util;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.ServiceSupport;

public class HazelcastIdempotentRepository extends ServiceSupport implements IdempotentRepository<String> {

    private String repositoryName;
    private IMap<String, Boolean> repo;
    private HazelcastInstance hazelcastInstance;

    public HazelcastIdempotentRepository(HazelcastInstance hazelcastInstance) {
        this(hazelcastInstance, HazelcastIdempotentRepository.class.getSimpleName());
    }

    public HazelcastIdempotentRepository(HazelcastInstance hazelcastInstance, String repositoryName) {
        this.repositoryName = repositoryName;
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    protected void doStart() throws Exception {
        repo = hazelcastInstance.getMap(repositoryName);
    }

    @Override
    protected void doStop() throws Exception {
        // noop
    }

    @Override
    public boolean add(String key) {
        repo.lock(key);
        try {
            return repo.putIfAbsent(key, false) == null;
        } finally {
            repo.unlock(key);
        }
    }

    @Override
    public boolean confirm(String key) {
        repo.lock(key);
        try {
            return repo.replace(key, false, true);
        } finally {
            repo.unlock(key);
        }
    }

    @Override
    public boolean contains(String key) {
        repo.lock(key);
        try {
            return this.repo.containsKey(key);
        } finally {
            repo.unlock(key);
        }
    }

    @Override
    public boolean remove(String key) {
        repo.lock(key);
        try {
            return repo.remove(key) != null;
        } finally {
            repo.unlock(key);
        }
    }

    @Override
    public void clear() {
        repo.clear();
    }

    public String getRepositoryName() {
        return repositoryName;
    }
}