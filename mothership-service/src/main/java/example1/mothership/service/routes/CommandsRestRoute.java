package example1.mothership.service.routes;

import com.hazelcast.ringbuffer.Ringbuffer;
import example1.mothership.service.processors.FromJsonProcessor;
import example1.mothership.service.processors.ValidateProcessor;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static example1.mothership.core.MothershipDataSchema.MothershipCommand;

@AllArgsConstructor
public class CommandsRestRoute extends RouteBuilder {

    public static final String JSON_CONTENT = "application/json";
    public static final String VIOLATIONS_HEADER = "violations";
    public static final String ORIGINAL_REQUEST_HEADER = "originalRequest";
    public static final String ORIGINAL_BODY_HEADER = "originalBody";

    final FromJsonProcessor<MothershipCommand> fromJsonProcessor;
    final ValidateProcessor<MothershipCommand> validator;
    final Ringbuffer<String> commandsRb;

    @Override
    public void configure() throws Exception {

        restConfiguration().component("servlet")
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "Mothership API").apiProperty("api.version", "1.2.3")
                .dataFormatProperty("prettyPrint", "true")
                // and enable CORS
                .apiProperty("cors", "true")
                .host("localhost").port(8080)
                .bindingMode(RestBindingMode.off);

        rest("/commands")
                .consumes(JSON_CONTENT).produces(JSON_CONTENT)
                .post("/").description("Submit a command")
                .param().name("body").description("The command").endParam()
                .to("direct:process-cmd-request");

        from("direct:process-cmd-request")
                .routeId("process-cmd-request")
                .streamCaching()
                .process(fromJsonProcessor)
                .process(validator)
                .process(e -> {
                    CommandResponse resp = new CommandResponse(UUID.randomUUID());
                    e.getOut().setBody(resp);
                    e.getOut().setHeader(ORIGINAL_REQUEST_HEADER, e.getIn().getBody(MothershipCommand.class));
                    e.getOut().setHeader(VIOLATIONS_HEADER, e.getIn().getHeader(VIOLATIONS_HEADER));
                })
                .choice()
                    .when(header(VIOLATIONS_HEADER).isNull())
                        .to("direct:enqueue-cmd")
                    .otherwise()
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                        .setHeader(Exchange.CONTENT_TYPE, constant(JSON_CONTENT))
                        .setBody(header(VIOLATIONS_HEADER))
                        .marshal().json(JsonLibrary.Jackson)
                .end()
//                .log("final headers --> ${headers}")
//                .log("final body --> ${body}");
                ;

        from("direct:enqueue-cmd")
                .routeId("enqueue-cmd-request")
                .setHeader(ORIGINAL_BODY_HEADER, body())
                .setBody(header(ORIGINAL_REQUEST_HEADER))
                .marshal().json(JsonLibrary.Jackson)
                .process(e -> {
                    commandsRb.add(e.getIn().getBody(String.class));
                })
                .setBody(header(ORIGINAL_BODY_HEADER));

    }

    @Value @AllArgsConstructor
    public static class CommandResponse {
        UUID ticket;
    }

    @Value
    @AllArgsConstructor
    public static class ErrorResponse implements Serializable {
        List<PropertyError> errorMessages;
        @Value
        @AllArgsConstructor
        public static class PropertyError {
            String property;
            String errorMessage;
        }
    }

}
