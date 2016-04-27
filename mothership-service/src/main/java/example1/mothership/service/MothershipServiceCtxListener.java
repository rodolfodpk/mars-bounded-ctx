package example1.mothership.service;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.hazelcast.config.Config;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import example1.mothership.service.processors.FromJsonProcessor;
import example1.mothership.service.processors.ValidateProcessor;
import example1.mothership.service.routes.CommandsPullerRoute;
import example1.mothership.service.routes.CommandsRestRoute;
import example1.mothership.util.HazelcastIdempotentRepository;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static example1.mothership.core.MothershipDataSchema.MothershipCommand;

public class MothershipServiceCtxListener implements ServletContextListener {

    CamelContext context;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        RingbufferConfig rbConfig = new RingbufferConfig("default")
                .setCapacity(50 * 1000);
        Config config = new Config();
        config.addRingBufferConfig(rbConfig);
        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
        Ringbuffer<String> commandsRb = hz.getRingbuffer("commands");

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        mapper.registerModule(new Jdk8Module());

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        ValidateProcessor<MothershipCommand> validateProcessor =
                new ValidateProcessor<>(validator, MothershipCommand.class);

        SimpleRegistry registry = new SimpleRegistry();

        HazelcastIdempotentRepository repo = new HazelcastIdempotentRepository(hz, "commands");

        context = new DefaultCamelContext(registry);

        try {
            context.addRoutes(new CommandsRestRoute(new FromJsonProcessor<>(mapper, MothershipCommand.class),
                                                    validateProcessor, commandsRb));
            context.addRoutes(new CommandsPullerRoute(commandsRb, repo));
            context.start();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

        try {
            context.stop();
        } catch (Exception e) {
            // e.printStackTrace();
        }

    }
}
