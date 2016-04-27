package example1.mothership.service.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@AllArgsConstructor
public class FromJsonProcessor<T> implements Processor {
    final ObjectMapper mapper ;
    final Class<T> clazz;
    @Override
    public void process(Exchange e) throws Exception {
        String json = e.getIn().getBody(String.class);
        T object = mapper.readValue(json, clazz);
        e.getOut().setBody(object, clazz);
        e.getOut().setHeaders(e.getIn().getHeaders());
    }
}
