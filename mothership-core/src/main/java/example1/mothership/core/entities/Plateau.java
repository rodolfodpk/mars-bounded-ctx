package example1.mothership.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import example1.mothership.core.services.TemperatureService;
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

import javax.validation.Valid;

import static example1.mothership.core.MothershipDataSchema.*;
import static example1.mothership.core.MothershipExceptions.*;

@Value @Wither @AllArgsConstructor public class Plateau {

    PlateauId id;
    @Valid PlateauDimension dimension;
    @JsonIgnore Map<String, PlateauLocation> landedRovers;

    public Plateau(PlateauId id, PlateauDimension dimension) {
        this.id = id;
        this.dimension = dimension;
        this.landedRovers = HashMap.empty();
    }

    public void canLaunchRover(RoverId roverId, PlateauLocation plateauLocation, TemperatureService temperatureService) {

        if (temperatureService.currentTemperatureInCelsius() > 100 /*celsius*/) {
            throw new CantLandOverATooHotPlateau();
        }
        if (landedRovers.containsValue(plateauLocation)) {
            throw new CantLandToAnAlreadyOccupiedPosition();
        }
        if (landedRovers.containsKey(roverId.getId())) {
            throw new CantLandAlreadyLandedRover();
        }
        if (plateauLocation.getX() >= dimension.getWidth() ||
            plateauLocation.getY() >= dimension.getHeight()) {
            throw new CantLandOutsidePlateau();
        }
    }

    @JsonIgnore public Map<String, PlateauLocation> landedRovers() { return landedRovers; }

    public PlateauLocation currentRoverLocation(RoverId roverId) {
        return landedRovers.get(roverId.getId()).get();
    }

    public void canMoveRoverTo(RoverId roverId, PlateauLocation newLocation) {

        if (!landedRovers.containsKey(roverId.getId())) {
            throw new IllegalStateException(); // TODO
        }

        if (newLocation.getX() >= dimension.getWidth() ||
                newLocation.getY() >= dimension.getHeight()) {
            throw new IllegalStateException(); // TODO
        }

        if (landedRovers.containsValue(newLocation)) {
            throw new IllegalStateException(); // TODO
        }
    }


}
