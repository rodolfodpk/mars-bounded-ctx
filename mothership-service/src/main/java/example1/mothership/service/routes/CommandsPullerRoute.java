package example1.mothership.service.routes;

import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.ringbuffer.ReadResultSet;
import com.hazelcast.ringbuffer.Ringbuffer;
import example1.mothership.core.MothershipDataSchema;
import example1.mothership.util.HazelcastIdempotentRepository;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CommandsPullerRoute extends RouteBuilder {

    final Ringbuffer<String> commandsRb;
    final HazelcastIdempotentRepository repo;
    final AtomicLong sequence;

    public CommandsPullerRoute(Ringbuffer<String> commandsRb, HazelcastIdempotentRepository repo) {
        this.commandsRb = commandsRb;
        this.repo = repo;
        this.sequence = new AtomicLong(commandsRb.headSequence());
    }

    @Override
    public void configure() throws Exception {

        from("scheduler://commands?initialDelay=1000&delay=5000&useFixedDelay=true$timeUnit=MILLISECONDS")
                .routeId("commands-processing")
                .log("--> will now consume commands after " + sequence.get())
                .process(e -> {
                    try {
                        ICompletableFuture<ReadResultSet<String>> f =
                                commandsRb.readManyAsync(sequence.get(), 1, 10, null);
                        ReadResultSet<String> rs = f.get();
                        List<String> orders = new ArrayList<>();
                        rs.forEach(json -> {
                            orders.add(json);
                            sequence.incrementAndGet();
                        });
                        e.getOut().setBody(orders);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                })
                .log("--> got ${body.size()} commands from queue")
                .split(body())
                .unmarshal().json(JsonLibrary.Jackson, MothershipDataSchema.MothershipCommand.class)
                .idempotentConsumer(simple("${body.orderNo}"), repo)
                .log("--> command : ${body}") ; // TODO send to cmd-processor

    }

}
