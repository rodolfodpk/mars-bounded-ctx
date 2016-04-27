package example1.mothership.core.services;

import static example1.mothership.core.MothershipDataSchema.PlateauLocation;
import static example1.mothership.core.MothershipDataSchema.RoverDirection;

public interface LocalizationService {

    PlateauLocation calculateTargetMovingFrom(PlateauLocation plateauLocation, RoverDirection roverDirection) ;

}
