package frc.robot.subsystems.placer;

import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.Logger;

import com.pathplanner.lib.auto.PIDConstants;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.RobotState;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.PlacerConstants;
import frc.robot.Constants.EnumConstants.GamePiece;
import frc.robot.Constants.EnumConstants.IntakeState;
import frc.robot.Constants.EnumConstants.PlacerState;
import frc.robot.Constants.EnumConstants.TalonModel;
import frc.robot.hardware.SparkMaxMotorController;
import frc.robot.hardware.TalonMotorController;
import frc.robot.hardware.interfaces.EncodedMotorController;
import frc.robot.utilities.Loggable;

public class Placer extends SubsystemBase implements Loggable{
    private static Placer instance;
    private EncodedMotorController outputMotor;
    private EncodedMotorController intakeAngleMotor;
    private EncodedMotorController armAngleMotor;
    private EncodedMotorController extensionMotor;
    private GamePiece currentGamePiece;
    private PlacerState targetPlacerState;

    private Placer() {
        extensionMotor = new TalonMotorController(PlacerConstants.ARM_EXTENSION_MOTOR_ID, TalonModel.TalonSRX);
        extensionMotor.setPID(new PIDConstants(0.4, 0, 0));
        extensionMotor.setMinOutput(-0.3);
        extensionMotor.setMaxOutput(0.6);
        extensionMotor.setAngleTolerance(400);
        extensionMotor.setMaxAngle(15.3398);

        armAngleMotor = new SparkMaxMotorController(PlacerConstants.ARM_ANGLE_MOTOR_ID, MotorType.kBrushless);
        armAngleMotor.setInverted(true);
        armAngleMotor.setPID(new PIDConstants(0.04, 0, 0));
        armAngleMotor.setMinOutput(-0.5);
        armAngleMotor.setMaxOutput(0.75);

        intakeAngleMotor = new SparkMaxMotorController(PlacerConstants.INTAKE_ANGLE_MOTOR_ID, MotorType.kBrushless);
        intakeAngleMotor.setBrakeOnIdle(true);
        intakeAngleMotor.setMinAngle(-80 * Math.PI);
        intakeAngleMotor.setPID(new PIDConstants(1, 0, 0));
        intakeAngleMotor.setMinOutput(-0.3);
        intakeAngleMotor.setMaxOutput(0.3);

        outputMotor = new SparkMaxMotorController(PlacerConstants.INTAKE_RUN_MOTOR_ID, MotorType.kBrushless);
        outputMotor.setBrakeOnIdle(false);
    }

    public static synchronized Placer getInstance() {
        return instance == null ? new Placer() : instance;
    }

    @Override
    public String getTableName() {
        return "Placer";
    }

    @Override
    public void logData(Logger logger, LogTable table) {
        table.put("Output (Percent)", getOutput());
        table.put("Intake Angle (Radians", getIntakeAngle());
        table.put("Arm Angle (Radians", getArmAngle());
        table.put("Extension (Radians)", getArmExtension());
        table.put("Game Piece", getGamePiece().name());
        table.put("Placer State", getPlacerState().name());
    }

    public void setOutput(double output) {
        outputMotor.setOutput(output);
    }

    public double getOutput() {
        return outputMotor.getOutput();
    }

    public void setIntakeAngle(double angleRadians) {
        intakeAngleMotor.setAngle(angleRadians);
    }

    public double getIntakeAngle() {
        return intakeAngleMotor.getAngle();
    }

    public void setArmAngle(double angleRadians) {
        armAngleMotor.setAngle(angleRadians);
    }

    public double getArmAngle() {
        return armAngleMotor.getAngle();
    }

    public void setArmExtension(double extensionAsRadians) {
        extensionMotor.setAngle(extensionAsRadians);
    }

    public double getArmExtension() {
        return extensionMotor.getAngle();
    }

    public void setGamePiece(GamePiece newGamePiece) {
        currentGamePiece = newGamePiece;
    }

    public GamePiece getGamePiece() {
        return currentGamePiece;
    }

    public void setPlacerState(PlacerState newPlacerState) {
        targetPlacerState = newPlacerState;
    }

    public PlacerState getPlacerState() {
        return targetPlacerState;
    }

    public Command setPlacerCommand(PlacerState placerState, IntakeState intakeState) {
        return setPlacerCommand(placerState)
            .alongWith(runIntakeCommand(intakeState));
    }

    public Command setPlacerCommand(PlacerState placerState) {
        return Commands.runOnce(
            () -> {
                setPlacerState(placerState);
                setArmExtension(placerState.extension);
                setArmAngle(placerState.armAngle);
                setIntakeAngle(placerState.intakeAngle);
            }
        );
    }

    public Command runIntakeCommand(IntakeState intakeState) {
        return Commands.runOnce(
            () -> {
                setOutput(intakeState == IntakeState.Place ? getGamePiece().outputToPlace : intakeState.output);
                if (intakeState.newGamePiece != null) setGamePiece(intakeState.newGamePiece);
            }
        );
    }

    public Command zeroPlacerCommand() {
        return setPlacerCommand(
            RobotState.isAutonomous() ? PlacerState.Zero : PlacerState.Travel, 
            IntakeState.Off
        );
    }

    public Command placeCommand() {
        return runIntakeCommand(IntakeState.Place)
            .andThen(Commands.waitSeconds(1))
            .andThen(zeroPlacerCommand());
    }

    public Command placeCommand(PlacerState placerState) {
        return setPlacerCommand(placerState)
            .andThen(Commands.waitSeconds(1.5))
            .andThen(placeCommand());
    }
}