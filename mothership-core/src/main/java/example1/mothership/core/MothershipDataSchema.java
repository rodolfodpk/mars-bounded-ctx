package example1.mothership.core;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import example1.mothership.core.entities.Mission;
import example1.mothership.core.entities.Plateau;
import example1.mothership.core.entities.Rover;
import javaslang.collection.List;
import javaslang.collection.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public class MothershipDataSchema {

    // value objects

    @Value public static class MothershipId { String id; }

    public enum MothershipStatus { AVALIABLE, ON_MISSION, FINISHED_MISSION}

    @Value public static class MissionId { String id; }

    @Value public static class PlateauId { String id; }

    @Value public static class RoverId { String id; }

    @Value public static class PlateauDimension { @Min(value = 2) int height; @Min(2) int width; }

    @Value public static class PlateauLocation { @Min(0) int x; @Min(0) int y; }

    public enum RoverDirection { NORTH, SOUTH, EAST, WEST;}

    @Value public static class CommandId { UUID uuid; public CommandId() { this.uuid = UUID.randomUUID(); } }

    @Value public static class UnitOfWorkId { UUID uuid; public UnitOfWorkId() { this.uuid = UUID.randomUUID(); }}

    // commands

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@cmdType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CreateMothership.class, name = "CreateMothership"),
            @JsonSubTypes.Type(value = StartsMissionTo.class, name = "StartsMissionTo"),
            @JsonSubTypes.Type(value = LaunchRoverTo.class, name = "LaunchRoverTo"),
            @JsonSubTypes.Type(value = ChangeRoverDirection.class, name = "ChangeRoverDirection"),
            @JsonSubTypes.Type(value = MoveRover.class, name = "MoveRover"),
            @JsonSubTypes.Type(value = ComeBackRover.class, name = "ComeBackRover"),
            @JsonSubTypes.Type(value = FinishCurrentMission.class, name = "FinishCurrentMission")

    })
    public interface MothershipCommand {
        CommandId getCommandId();
        MothershipId getMothershipId();
    }

    @Value @Builder @AllArgsConstructor @JsonTypeName("CreateMothership")
    public static class CreateMothership implements MothershipCommand { CommandId commandId; MothershipId mothershipId;
        Set<Rover> rovers; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("StartsMissionTo")
    public static class StartsMissionTo implements MothershipCommand {CommandId commandId; MothershipId mothershipId;
        MissionId missionId; @NotNull @Valid
        Plateau plateau; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("LaunchRoverTo")
    public static class LaunchRoverTo implements MothershipCommand { CommandId commandId; MothershipId mothershipId;
        RoverId roverId; @Valid PlateauLocation plateauLocation; } // TODO add rover direction

    @Value @Builder @AllArgsConstructor @JsonTypeName("ChangeRoverDirection")
    public static class ChangeRoverDirection implements MothershipCommand { CommandId commandId; MothershipId
            mothershipId; RoverId roverId; RoverDirection direction; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("MoveRover")
    public static class MoveRover implements MothershipCommand { CommandId commandId; MothershipId mothershipId;
        RoverId roverId; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("ComeBackRover")
    public static class ComeBackRover implements MothershipCommand { CommandId commandId; MothershipId mothershipId;
        RoverId roverId; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("FinishCurrentMission")
    public static class FinishCurrentMission implements MothershipCommand { CommandId commandId;
        MothershipId mothershipId; }


    // events

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@evtType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MothershipCreated.class, name = "MothershipCreated"),
            @JsonSubTypes.Type(value = MissionStarted.class, name = "MissionStarted"),
            @JsonSubTypes.Type(value = RoverLaunched.class, name = "RoverLaunched"),
            @JsonSubTypes.Type(value = RoverDirectionChanged.class, name = "RoverDirectionChanged"),
            @JsonSubTypes.Type(value = RoverMoved.class, name = "RoverMoved"),
            @JsonSubTypes.Type(value = RoverIsBack.class, name = "RoverIsBack"),
            @JsonSubTypes.Type(value = MissionFinished.class, name = "MissionFinished")

    })
    public interface MothershipEvent { }

    @Value @Builder @AllArgsConstructor @JsonTypeName("MothershipCreated")
    public static class MothershipCreated implements MothershipEvent {MothershipId mothershipId; Set<Rover> rovers; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("MissionStarted")
    public static class MissionStarted implements MothershipEvent {Mission mission; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("RoverLaunched")
    public static class RoverLaunched implements MothershipEvent {RoverId roverId; PlateauLocation plateauLocation; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("RoverDirectionChanged")
    public static class RoverDirectionChanged implements MothershipEvent {RoverId roverId;
        RoverDirection newDirection; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("RoverMoved")
    public static class RoverMoved implements MothershipEvent {RoverId roverId; PlateauLocation plateauLocation; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("RoverIsBack")
    public static class RoverIsBack implements MothershipEvent {RoverId roverId; }

    @Value @Builder @AllArgsConstructor @JsonTypeName("MissionFinished")
    public static class MissionFinished implements MothershipEvent {}

    // unitofwork

    @Value
    @Builder
    @AllArgsConstructor
    public static class MothershipUnitOfWork {
        UnitOfWorkId id;
        MothershipCommand command;
        Long originalVersion;
        List<MothershipEvent> events;
        LocalDateTime localDateTime;
        Long resultingVersion;
    }

}
