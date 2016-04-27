package example1.mothership.service.processors;

import example1.mothership.service.routes.CommandsRestRoute;
import lombok.AllArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ValidateProcessor<T> implements Processor {
    final Validator validator;
    final Class<T> clazz;
    @Override
    public void process(Exchange e) throws Exception {
        T request = e.getIn().getBody(clazz);
        e.getOut().setBody(request);
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        List<CommandsRestRoute.ErrorResponse.PropertyError> messages = violations.stream()
                .map(cv -> new CommandsRestRoute.ErrorResponse.PropertyError(cv.getPropertyPath().toString(),
                        cv.getMessage()))
                .collect(Collectors.toList());
        e.getOut().setHeader("violations", violations.size() == 0 ? null :
                                                                    new CommandsRestRoute.ErrorResponse(messages));
    }
}
