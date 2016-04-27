package example1.mothership.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import example1.mothership.core.entities.Plateau;
import example1.mothership.core.entities.Rover;
import javaslang.collection.HashMap;
import javaslang.collection.HashSet;
import javaslang.collection.Map;
import javaslang.jackson.datatype.JavaslangModule;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static example1.mothership.core.MothershipDataSchema.*;
import static example1.mothership.core.MothershipDataSchema.RoverDirection.NORTH;
import static org.junit.Assert.assertEquals;

public class SerializationTest {

    static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaslangModule());
        mapper.registerModule(new Jdk8Module());
    }

    @Test
    public void createCmd() throws IOException {

        val mothershipId = new MothershipId("voyager");

        val createCmd = CreateMothership.builder()
                .commandId(new CommandId())
                .mothershipId(mothershipId)
                .rovers(HashSet.of(new Rover(new RoverId("enio")), new Rover(new RoverId("beto"))))
                .build();

        val asJson = mapper.writeValueAsString(createCmd);

        assertEquals(mapper.readerFor(MothershipCommand.class).readValue(asJson), createCmd);

        assertEquals(mapper.readerFor(CreateMothership.class).readValue(asJson), createCmd);

    }

    @Test
    public void startMissionCmd() throws IOException {

        val mothershipId = new MothershipId("voyager");

        val startMissionCmd = StartsMissionTo.builder()
                .mothershipId(mothershipId)
                .commandId(new CommandId())
                .missionId(new MissionId("mars"))
                .plateau(new Plateau(new PlateauId("deadSea"), new PlateauDimension(4, 4)))
                .build();

        val asJson = mapper.writeValueAsString(startMissionCmd);

        assertEquals(mapper.readerFor(MothershipCommand.class).readValue(asJson), startMissionCmd);

        assertEquals(mapper.readerFor(StartsMissionTo.class).readValue(asJson), startMissionCmd);

    }

    @Test
    public void string_map_key_should_pass() throws IOException {

        PlateauLocation roverPosition1 = new PlateauLocation(2, 3);
        PlateauLocation roverPosition2 = new PlateauLocation(3, 4);

        Map<String, PlateauLocation> map = HashMap.of(new RoverId("r1").toString(), roverPosition1,
                                                   new RoverId("r2").toString(), roverPosition2);

        val asJson1 = mapper.writeValueAsString(map);

        val typeRefT2 = new TypeReference<Map<String, PlateauLocation>>() {};

        assertEquals(mapper.readerFor(typeRefT2).readValue(asJson1), map);

    }



    @Test @Ignore // failing TODO investigate https://github.com/msgpack/msgpack-java/issues/244
    public void non_string_map_key_should_pass() throws IOException {

        PlateauLocation roverPosition1 = new PlateauLocation(2, 3);
        PlateauLocation roverPosition2 = new PlateauLocation(3, 4);

        Map<String, PlateauLocation> map = HashMap.of(new RoverId("r1").toString(), roverPosition1,
                new RoverId("r2").toString(), roverPosition2);

        val asJson1 = mapper.writeValueAsString(map);

        val typeRefT2 = new TypeReference<Map<PlateauLocation, RoverId>>() {};

        assertEquals(mapper.readerFor(typeRefT2).readValue(asJson1), map);

    }

    @Test // notice: if using javaslang List instead, it does not work // TODO investigate
    public void listOfCommands() throws IOException {

        val typeRef = new TypeReference<java.util.List<MothershipCommand>>() {};

        val mothershipId = new MothershipId("voyager");

        val createCmd = CreateMothership.builder()
                .commandId(new CommandId())
                .mothershipId(mothershipId)
                .rovers(HashSet.of(new Rover(new RoverId("enio"), NORTH), new Rover(new RoverId("beto"), NORTH)))
                .build();

        val startMissionCmd = StartsMissionTo.builder()
                .mothershipId(mothershipId)
                .commandId(new CommandId())
                .missionId(new MissionId("mars"))
                .plateau(new Plateau(new PlateauId("deadSea"), new PlateauDimension(4, 4)))
                .build();

        java.util.List<MothershipCommand> listOfCommands = Arrays.asList(createCmd, startMissionCmd);

        val listJson = mapper.writerFor(typeRef).writeValueAsString(listOfCommands);

        java.util.List<MothershipCommand> backToList = mapper.readValue(listJson, typeRef);

        assertEquals(createCmd, backToList.get(0));

        assertEquals(startMissionCmd, backToList.get(1));
    }

    // TODO test events

}
