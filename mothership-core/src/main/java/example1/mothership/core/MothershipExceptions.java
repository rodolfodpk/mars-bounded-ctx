package example1.mothership.core;

public class MothershipExceptions {

    public static class MothershipStatusConflict extends RuntimeException {}

    public static class CantLandUnknownRover extends RuntimeException {}

    public static class MothershipMustHaveAtLeastOneRover extends RuntimeException {}

    public static class MothershipCanHaveJustOneMission extends RuntimeException {}

    public static class CantLandAlreadyLandedRover extends RuntimeException {}

    public static class CantLandToAnAlreadyOccupiedPosition extends RuntimeException {}

    public static class CantLandOverATooHotPlateau extends RuntimeException {}

    public static class CantLandOutsidePlateau extends RuntimeException {}

}
