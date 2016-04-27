import javaslang.Tuple3;
import javaslang.control.Try;
import keyvent.flows.StateTransitionsTracker;
import keyvent.flows.commands.CommandHandler;

public class CreateMothershipCommandHandler<ID, AR, CMD, EV>  implements CommandHandler<ID, AR, CMD, EV>  {

    @Override
    public Try<Tuple3<ID, CMD, StateTransitionsTracker<EV, AR>>> handle(Tuple3<ID, CMD,
                                                                    StateTransitionsTracker<EV, AR>> commandContext) {
        return null;
    }

}
