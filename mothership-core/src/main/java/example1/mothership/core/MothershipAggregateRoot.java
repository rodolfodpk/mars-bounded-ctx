package example1.mothership.core;

import example1.mothership.core.entities.Mission;
import example1.mothership.core.entities.Plateau;
import example1.mothership.core.entities.Rover;
import example1.mothership.core.services.LocalizationService;
import example1.mothership.core.services.TemperatureService;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Set;
import javaslang.control.Option;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import lombok.val;

import java.util.Objects;

import static example1.mothership.core.MothershipDataSchema.*;
import static example1.mothership.core.MothershipDataSchema.MothershipStatus.AVALIABLE;
import static example1.mothership.core.MothershipDataSchema.MothershipStatus.ON_MISSION;
import static example1.mothership.core.MothershipExceptions.*;

@Value @AllArgsConstructor @Wither @Builder public class MothershipAggregateRoot {

    // these props never change
    MothershipId id;
    Map<String, Rover> rovers;

    // these props can change
    MothershipStatus status;
    Option<Mission> mission;

    // services
    transient TemperatureService temperatureService;
    transient LocalizationService localizationService;

    // events emitters

    public List<? super MothershipEvent> create(MothershipId id, Set<Rover> avaliableRovers) {
        isNew();
        hasAtLeastOneRover(avaliableRovers);
        return List.of(new MothershipCreated(id, avaliableRovers));
    }

    public List<? super MothershipEvent> startMission(MissionId missionId, Plateau plateau) {
        isNotNew();
        isFirstMission();
        statusIs(AVALIABLE);
        return List.of(new MissionStarted(new Mission(missionId, plateau)));
    }

    public List<? super MothershipEvent> landRover(RoverId roverId, PlateauLocation plateauLocation) {
        isNotNew();
        statusIs(ON_MISSION);
        hasRover(roverId);
        mission.get().canLaunchRover(roverId, plateauLocation, temperatureService);
        return List.of(new RoverLaunched(roverId, plateauLocation));
    }

    public List<? super MothershipEvent> changeRoverDirection(RoverId roverId, RoverDirection newDirection) {
        isNotNew();
        statusIs(ON_MISSION);
        hasRover(roverId);
        // TODO could also check if rover is already landed
        return List.of(new RoverDirectionChanged(roverId, newDirection));
    }

    public List<? super MothershipEvent> moveRoverForward(RoverId roverId) {
        isNotNew();
        statusIs(ON_MISSION);
        hasRover(roverId);
        val currentLocation = mission.get().roverLocation(roverId);
        val currentDirection = rovers.get(roverId.getId()).get().getRoverDirection();
        val newLocation = localizationService.calculateTargetMovingFrom(currentLocation, currentDirection);
        mission.get().canMoveRoverTo(roverId, newLocation);
        // TODO could also check if rover is already landed
        return List.of(new RoverMoved(roverId, newLocation));
    }

    public List<? super MothershipEvent> getBackRover(RoverId roverId) {
        isNotNew();
        statusIs(ON_MISSION);
        hasRover(roverId);
        // TODO could also check if rover is already landed
        return List.of(new RoverIsBack(roverId));
    }

    public List<? super MothershipEvent> finishMission() {
        isNotNew();
        statusIs(ON_MISSION);
        return List.of(new MissionFinished());
    }

    // guards

    private void hasRover(RoverId roverId) {
        if (!rovers.containsKey(roverId.getId())){
            throw new CantLandUnknownRover();
        }
    }

    void statusIs(MothershipStatus requiredStatus) {
        if (!requiredStatus.equals(status)) {
            throw new MothershipStatusConflict();
        }
    }

    void isNew() {
        Objects.isNull(id);
    }

    void hasAtLeastOneRover(Set<Rover> avaliableRovers) {
         if (avaliableRovers.size() == 0) {
             throw new MothershipMustHaveAtLeastOneRover();
         }
    }

    void isNotNew() {
        Objects.requireNonNull(id);
    }

    void isFirstMission() {
        if (mission.isDefined()) {
            throw new MothershipCanHaveJustOneMission();
        }
    }

}
