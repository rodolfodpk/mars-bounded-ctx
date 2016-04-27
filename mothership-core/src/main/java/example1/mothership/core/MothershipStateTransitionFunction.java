package example1.mothership.core;


import example1.mothership.core.entities.Mission;
import example1.mothership.core.entities.Plateau;
import example1.mothership.core.entities.Rover;
import javaslang.Function2;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.control.Option;
import lombok.val;

import static example1.mothership.core.MothershipDataSchema.*;
import static example1.mothership.core.MothershipDataSchema.MothershipStatus.*;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

class MothershipStateTransitionFunction
        implements Function2<MothershipEvent, MothershipAggregateRoot, MothershipAggregateRoot> {

    @Override
    public MothershipAggregateRoot apply(MothershipEvent mothershipEvent, MothershipAggregateRoot mothership) {
        return Match(mothershipEvent).of(
                Case(instanceOf(MothershipCreated.class),
                        event -> mothership
                                    .withId(event.getMothershipId())
                                    .withRovers(HashMap.ofEntries(event.getRovers()
                                            .map(rover -> Tuple.of(rover.getId().getId(), rover))))
                                    .withStatus(AVALIABLE).withMission(Option.none())
                        ),
                Case(instanceOf(MissionStarted.class),
                        event -> mothership.withStatus(ON_MISSION).withMission(Option.of(event.getMission()))
                ),
                Case(instanceOf(RoverDirectionChanged.class),
                        event -> mothership.withRovers(mothership.getRovers().
                                put(event.getRoverId().getId(), new Rover(event.getRoverId(), event.getNewDirection())))
                ),
                Case(instanceOf(RoverMoved.class),
                        event -> {
                            Plateau currentPlateau = mothership.getMission().get().getPlateau();
                            Plateau newPLateau = currentPlateau.withLandedRovers(currentPlateau.getLandedRovers()
                                    .put(event.getRoverId().getId(), event.getPlateauLocation()));
                            return mothership.withMission(
                                    Option.of(mothership.getMission().get().withPlateau(newPLateau)));
                        }
                ),
                Case(instanceOf(RoverIsBack.class),
                        event -> {
                            Plateau currentPlateau = mothership.getMission().get().getPlateau();
                            Plateau newPlateau = currentPlateau.withLandedRovers(currentPlateau
                                    .getLandedRovers().remove(event.getRoverId().getId()));
                            return mothership.withMission(Option.of(mothership.getMission().get()
                                    .withPlateau(newPlateau)));
                        }
                ),
                Case(instanceOf(MissionFinished.class),
                        event -> {
                            Plateau currentPlateau = mothership.getMission().get().getPlateau();
                            Plateau newPlateau = currentPlateau.withLandedRovers(HashMap.empty());
                            return mothership.withMission(Option.of(mothership.getMission().get()
                                    .withPlateau(newPlateau)))
                                    .withStatus(FINISHED_MISSION);
                        }
                ),
                Case(instanceOf(RoverLaunched.class),
                        event -> {
                            Mission currentMission = mothership.getMission().get();
                            Plateau currentPlateau = currentMission.getPlateau();
                            Plateau newPlateau = currentPlateau
                                    .withLandedRovers(currentPlateau.landedRovers()
                                    .put(event.getRoverId().getId(), event.getPlateauLocation()));
                            return mothership.withMission(Option.of(currentMission.withPlateau(newPlateau)));
                        })
                )
        ;
    }
}
