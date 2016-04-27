package example1.mothership.service.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@AllArgsConstructor
public class ToJsonProcessor implements Processor {
    final ObjectMapper mapper ;
    @Override
    public void process(Exchange e) throws Exception {
        Object object = e.getIn().getBody();
        String json = mapper.writeValueAsString(object);
        e.getOut().setBody(json);
        e.getOut().setHeaders(e.getIn().getHeaders());
    }
}
