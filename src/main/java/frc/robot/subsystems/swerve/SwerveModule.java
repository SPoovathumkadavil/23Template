package frc.robot.subsystems.swerve;

import com.pathplanner.lib.auto.PIDConstants;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.hardware.EncodedMotorController;

public class SwerveModule {
	private EncodedMotorController driveMotor;
	private EncodedMotorController angleMotor;
	private Translation2d translationFromCenter;

	public SwerveModule(
		SwerveMotorConfig driveConfig,
		SwerveMotorConfig angleConfig,
		Translation2d translationToCenter
	) {
		driveMotor = driveConfig.motor;
		angleMotor = angleConfig.motor;

		driveMotor
			.setInversion(driveConfig.invert)
			.configureCurrentLimit(driveConfig.currentLimit)
			.setPID(driveConfig.pid);
		angleMotor
			.setInversion(angleConfig.invert)
			.configureCurrentLimit(angleConfig.currentLimit)
			.setPID(angleConfig.pid);
		
		this.translationFromCenter = translationToCenter;
	}

	public void drive(SwerveModuleState initialTargetState) {
		SwerveModuleState targetState = optimizeTalon(
			initialTargetState,
			getModuleState().angle
		);
		setModuleVelocity(
			targetState.speedMetersPerSecond  *// This is scales the velocity by how off the wheel is from the target angle.
			Math.abs(targetState.angle.minus(getModuleState().angle).getCos())
		);
		setModuleAngle(targetState.angle.getRadians());
	}

	public SwerveModuleState getModuleState() {
		return new SwerveModuleState(
			driveMotor.getAngularVelocity() *
			SwerveConstants.DRIVE_RATIO *
			SwerveConstants.WHEEL_DIAMETER_METERS /
			2,
			new Rotation2d(angleMotor.getAngle() * SwerveConstants.ANGLE_RATIO)
		);
	}

	public double getAngularVelocity() {
		return angleMotor.getAngularVelocity() * SwerveConstants.ANGLE_RATIO;
	}

	public SwerveModulePosition getModulePosition() {
		return new SwerveModulePosition(
			driveMotor.getAngle() /
			(2 * Math.PI) * 
			SwerveConstants.DRIVE_RATIO *
			SwerveConstants.WHEEL_DIAMETER_METERS *
			Math.PI,
			getModuleState().angle
		);
	}

	public Translation2d getTranslationFromCenter() {
		return translationFromCenter;
	}

	public void setModuleAngle(double targetAngleRadians) {
		angleMotor.setAngle(targetAngleRadians / SwerveConstants.ANGLE_RATIO);
	}

	public void setModuleVelocity(double targetVelocityMetersPerSecond) {
		driveMotor.setAngularVelocity(
			targetVelocityMetersPerSecond *
			2 /
			(SwerveConstants.DRIVE_RATIO * SwerveConstants.WHEEL_DIAMETER_METERS)
		);
	}

	/**
	 * Minimize the change in heading the desired swerve module state would require
	 * by potentially reversing the direction the wheel spins. Customized from
	 * WPILib's version to include placing in appropriate scope for CTRE onboard
	 * control.
	 * 
	 * @see <a
	 *      href=https://www.chiefdelphi.com/t/swerve-modules-flip-180-degrees-periodically-conditionally/393059/3
	 *      >Chief Delphi Post Concerning The Issue</a>
	 * 
	 * @param desiredState The desired state.
	 * @param currentAngle The current module angle.
	 */
	private SwerveModuleState optimizeTalon(SwerveModuleState desiredState, Rotation2d currentAngle) {
		double targetAngle = placeInAppropriate0To360Scope(currentAngle.getDegrees(), desiredState.angle.getDegrees());
		double targetSpeed = desiredState.speedMetersPerSecond;
		double delta = targetAngle - currentAngle.getDegrees();
		if (Math.abs(delta) > 90) {
			targetSpeed = -targetSpeed;
			targetAngle = delta > 90 ? (targetAngle -= 180) : (targetAngle += 180);
		}
		return new SwerveModuleState(
				targetSpeed,
				Rotation2d.fromDegrees(targetAngle));
	}

	/**
	 * Places the given angle in the appropriate 0 to 360 degree scope based on the
	 * reference angle.
	 * 
	 * @param scopeReference the reference angle to base the scope on
	 * @param newAngle       the angle to place in the scope
	 * @return the new angle within the appropriate 0 to 360 degree scope
	 */
	private double placeInAppropriate0To360Scope(double scopeReference, double newAngle) {
		double lowerBound;
		double upperBound;
		double lowerOffset = scopeReference % 360;
		if (lowerOffset >= 0) {
			lowerBound = scopeReference - lowerOffset;
			upperBound = scopeReference + (360 - lowerOffset);
		} else {
			upperBound = scopeReference - lowerOffset;
			lowerBound = scopeReference - (360 + lowerOffset);
		}
		while (newAngle < lowerBound) {
			newAngle += 360;
		}
		while (newAngle > upperBound) {
			newAngle -= 360;
		}
		if (newAngle - scopeReference > 180) {
			newAngle -= 360;
		} else if (newAngle - scopeReference < -180) {
			newAngle += 360;
		}
		return newAngle;
	}

	public static class SwerveMotorConfig {
		public EncodedMotorController motor;
		public boolean invert;
		public int currentLimit;
		public PIDConstants pid;
		public SwerveMotorConfig(
			EncodedMotorController motor,
			boolean invert, 
			int currentLimit, 
			PIDConstants pid
		) {
			this.motor = motor;
			this.invert = invert;
			this.currentLimit = currentLimit;
			this.pid = pid;
		}
	}
}
