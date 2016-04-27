package example1.mothership.core;

import example1.mothership.core.MothershipExceptions.CantLandAlreadyLandedRover;
import example1.mothership.core.MothershipExceptions.CantLandOutsidePlateau;
import example1.mothership.core.MothershipExceptions.CantLandUnknownRover;
import example1.mothership.core.MothershipExceptions.MothershipCanHaveJustOneMission;
import example1.mothership.core.entities.Mission;
import example1.mothership.core.entities.Plateau;
import example1.mothership.core.entities.Rover;
import example1.mothership.core.services.LocalizationService;
import example1.mothership.core.services.TemperatureService;
import javaslang.collection.HashMap;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.control.Option;
import lombok.val;
import org.junit.Test;

import static example1.mothership.core.MothershipDataSchema.*;
import static example1.mothership.core.MothershipDataSchema.MothershipStatus.AVALIABLE;
import static example1.mothership.core.MothershipDataSchema.MothershipStatus.ON_MISSION;
import static example1.mothership.core.MothershipDataSchema.RoverDirection.NORTH;
import static example1.mothership.core.MothershipDataSchema.RoverDirection.SOUTH;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MothershipAggregateRootTest {

    // happy path scenarios

    @Test
    public void create_mothership_on_fresh_should_fire_event() {

        // given
        val mId = new MothershipId("startreck");
        val freshMothership = MothershipAggregateRoot.builder().build();
        val rovers = HashSet.of(new Rover(new RoverId("enio")), new Rover(new RoverId("beto")));

        // when
        val fired_events = freshMothership.create(mId, rovers);

        // then
        assertEquals(List.of(new MothershipCreated(mId, rovers)), fired_events);
    }

    @Test
    public void start_mission_on_fine_mothership_should_fire_event() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> rovers = HashMap.of("enio", new Rover(new RoverId("enio")), "beto",
                new Rover(new RoverId("beto")));
        val avaliableMothership = MothershipAggregateRoot.builder().id(mId).rovers(rovers).status(AVALIABLE)
                .mission(Option.none()).build();

        val initialPlateau = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2));
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(initialPlateau).build();

        // when
        val fired_events = avaliableMothership.startMission(mission.getMissionId(), initialPlateau);

        // then
        assertEquals(List.of(new MissionStarted(mission)), fired_events);
    }

    @Test
    public void land_rover_should_fire_event() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> rovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val initialPlateau = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2));
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(initialPlateau).build();
        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(99f);
        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(rovers).status(ON_MISSION)
                .mission(Option.of(mission)).temperatureService(mockService).build();

        // when
        val fired_events = onMissionMothership.landRover(new RoverId("enio"), new PlateauLocation(0,0));

        // then
        assertEquals(List.of(new RoverLaunched(new RoverId("enio"), new PlateauLocation(0,0))), fired_events);
    }


    @Test
    public void change_rover_direction_should_fire_event() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> rovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val initialPlateau = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2));
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(initialPlateau).build();
        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(99f);
        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(rovers).status(ON_MISSION)
                .mission(Option.of(mission))
                .temperatureService(mockService).build();

        // when
        val fired_events = onMissionMothership.changeRoverDirection(new RoverId("enio"), SOUTH);

        // then
        assertEquals(List.of(new RoverDirectionChanged(new RoverId("enio"), SOUTH)), fired_events);
    }

    @Test
    public void move_rover_forward_should_fire_event() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> rovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val plateauWithRover = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2))
                .withLandedRovers(HashMap.of("enio", new PlateauLocation(0, 0)));
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(plateauWithRover).build();

        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(99f);

        val localizationService = mock(LocalizationService.class);
        when(localizationService.calculateTargetMovingFrom(any(PlateauLocation.class), any(RoverDirection.class)))
                .thenReturn(new PlateauLocation(0, 1));

        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(rovers).status(ON_MISSION)
                .mission(Option.of(mission))
                .temperatureService(mockService)
                .localizationService(localizationService)
                .build();

        // when
        val fired_events = onMissionMothership.moveRoverForward(new RoverId("enio"));

        // then
        assertEquals(List.of(new RoverMoved(new RoverId("enio"), new PlateauLocation(0, 1))), fired_events);
    }

    @Test
    public void finish_mission_should_fire_event() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> rovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val plateauWithRover = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2))
                .withLandedRovers(HashMap.of("enio", new PlateauLocation(0, 0)));
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(plateauWithRover).build();

        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(99f);

        val localizationService = mock(LocalizationService.class);
        when(localizationService.calculateTargetMovingFrom(any(PlateauLocation.class), any(RoverDirection.class)))
                .thenReturn(new PlateauLocation(0, 1));

        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(rovers).status(ON_MISSION)
                .mission(Option.of(mission))
                .temperatureService(mockService)
                .localizationService(localizationService)
                .build();

        // when
        val fired_events = onMissionMothership.finishMission();

        // then
        assertEquals(List.of(new MissionFinished()), fired_events);
    }

    // failing scenarios

    @Test(expected = MothershipCanHaveJustOneMission.class)
    public void start_mission_conflict_should_fail() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> rovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val initialPlateau = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2));
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(initialPlateau).build();
        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(rovers).status(ON_MISSION)
                .mission(Option.of(mission)).build();

        // when
        val fired_events = onMissionMothership.startMission(mission.getMissionId(), initialPlateau);
    }

    @Test(expected = CantLandUnknownRover.class)
    public void land_a_unknown_rover_should_fail() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> rovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val initialPlateau = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2));
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(initialPlateau).build();
        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(rovers).status(ON_MISSION)
                .mission(Option.of(mission)).build();

        // when
        val fired_events = onMissionMothership.landRover(new RoverId("alien"), new PlateauLocation(0, 0));
    }

    @Test(expected = CantLandAlreadyLandedRover.class)
    public void land_an_already_landed_rover_should_fail() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> avaliableRovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val landedRovers = HashMap.of(new RoverId("enio").getId(), new PlateauLocation(1,1));
        val onMissionPlateau = new Plateau(new PlateauId("death's cave"),
                new PlateauDimension(2, 2)).withLandedRovers(landedRovers);
        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(99f);
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(onMissionPlateau).build();
        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(avaliableRovers).status(ON_MISSION)
                .mission(Option.of(mission)).temperatureService(mockService).build();

        // when
        val fired_events = onMissionMothership.landRover(new RoverId("enio"), new PlateauLocation(0, 0));
    }


    @Test(expected = CantLandOutsidePlateau.class)
    public void land_outside_plateau_should_fail() {

        // given
        val mId = new MothershipId("startreck");
        Map<String, Rover> avaliableRovers = HashMap.of("enio", new Rover(new RoverId("enio"), NORTH), "beto",
                new Rover(new RoverId("beto"), NORTH));
        val onMissionPlateau = new Plateau(new PlateauId("death's cave"), new PlateauDimension(2, 2));
        val mockService = mock(TemperatureService.class);
        when(mockService.currentTemperatureInCelsius()).thenReturn(99f);
        val mission = Mission.builder().missionId(new MissionId("kamikaze")).plateau(onMissionPlateau).build();
        val onMissionMothership = MothershipAggregateRoot.builder().id(mId).rovers(avaliableRovers).status(ON_MISSION)
                .mission(Option.of(mission)).temperatureService(mockService).build();

        // when
        val fired_events = onMissionMothership.landRover(new RoverId("enio"), new PlateauLocation(5, 5));
    }

}