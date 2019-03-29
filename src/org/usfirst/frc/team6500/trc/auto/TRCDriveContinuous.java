package org.usfirst.frc.team6500.trc.auto;


import org.usfirst.frc.team6500.trc.util.TRCTypes.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.usfirst.frc.team6500.trc.util.TRCNetworkData;
import org.usfirst.frc.team6500.trc.wrappers.systems.drives.TRCMecanumDrive;


public class TRCDriveContinuous
{
    private static Object drive;
    private static DriveType driveType;
    private static double speed;

    private static AtomicBoolean driving;
    private static AtomicBoolean shouldQuit;
    private static AtomicInteger actionType;
    private static Thread runner;


    public static void initializeTRCDriveContinuous(Object driveBase, DriveType driveBaseType, double driveSpeed)
    {
        drive = driveBase;
        driveType = driveBaseType;
        speed = driveSpeed;
        driving = new AtomicBoolean(false);
        shouldQuit = new AtomicBoolean(false);
        runner = new Thread(TRCDriveContinuous::driveContinuous);
        runner.setName("Continuous Drive Thread");

        TRCNetworkData.logString(VerbosityType.Log_Info, "DriveContinuous is online.");
    }


    public static void startDriveContinuous(DriveContinuousActionType driveActionType)
    {
        if (runner != null) { return; }

        actionType.set(driveActionType.ordinal());
        driving.set(true);
        shouldQuit.set(false);
        TRCDriveSync.requestChangeState(DriveSyncState.DriveContinuous);

        runner.start();
    }

    public static void driveContinuous()
    {
        driving.set(true);

        while (shouldQuit.get())
        {
            if (!driving.get()) { continue; }
            TRCDriveSync.assertDriveContinuous();

            if (driveType == DriveType.Mecanum)
            {
                if (actionType.get() == DriveContinuousActionType.Stop.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(0.0, 0.0, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.Forward.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(speed, 0.0, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.Reverse.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(-speed, 0.0, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.Left.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(0.0, -speed, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.Right.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(0.0, speed, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.RotateLeft.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(0.0, 0.0, -speed);
                }
                else if (actionType.get() == DriveContinuousActionType.RotateRight.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(0.0, 0.0, speed);
                }
                else if (actionType.get() == DriveContinuousActionType.ForwardRight.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(speed, speed, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.ReverseRight.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(-speed, speed, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.ForwardLeft.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(speed, -speed, 0.0);
                }
                else if (actionType.get() == DriveContinuousActionType.ReverseLeft.ordinal())
                {
                    ((TRCMecanumDrive) drive).driveCartesian(-speed, -speed, 0.0);
                }
            }
            else if (driveType == DriveType.Differential)
            {
                // Maybe another day...
            }
        }
    }

    public static void setDriveContinuousSpeed(double newSpeed) { speed = newSpeed; }
    public static void setDriveContinuousActionType(DriveContinuousActionType newActionType) { actionType.set(newActionType.ordinal()); }
    
    public static void pauseDriveContinuous()
    {
        TRCDriveSync.requestChangeState(DriveSyncState.Teleop);
        driving.set(false);
    }
    
    public static void resumeDriveContinuous()
    {
        TRCDriveSync.requestChangeState(DriveSyncState.DriveContinuous);
        driving.set(true);
    }

    public static void stopDriveContinuous()
    {
        driving.set(false);
        shouldQuit.set(true);
    }
}