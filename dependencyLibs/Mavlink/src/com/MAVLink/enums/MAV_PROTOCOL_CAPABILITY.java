
/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

package com.MAVLink.enums;

/**
* Bitmask of (optional) autopilot capabilities (64 bit). If a bit is set, the autopilot supports this capability.
*/
public class MAV_PROTOCOL_CAPABILITY {
   public static final int MAV_PROTOCOL_CAPABILITY_MISSION_FLOAT = 1; /* Autopilot supports MISSION float message type. | */
   public static final int MAV_PROTOCOL_CAPABILITY_PARAM_FLOAT = 2; /* Autopilot supports the new param float message type. | */
   public static final int MAV_PROTOCOL_CAPABILITY_MISSION_INT = 4; /* Autopilot supports MISSION_INT scaled integer message type. | */
   public static final int MAV_PROTOCOL_CAPABILITY_COMMAND_INT = 8; /* Autopilot supports COMMAND_INT scaled integer message type. | */
   public static final int MAV_PROTOCOL_CAPABILITY_PARAM_UNION = 16; /* Autopilot supports the new param union message type. | */
   public static final int MAV_PROTOCOL_CAPABILITY_FTP = 32; /* Autopilot supports the new param union message type. | */
   public static final int MAV_PROTOCOL_CAPABILITY_SET_ATTITUDE_TARGET = 64; /* Autopilot supports commanding attitude offboard. | */
   public static final int MAV_PROTOCOL_CAPABILITY_SET_POSITION_TARGET_LOCAL_NED = 128; /* Autopilot supports commanding position and velocity targets in local NED frame. | */
   public static final int MAV_PROTOCOL_CAPABILITY_SET_POSITION_TARGET_GLOBAL_INT = 256; /* Autopilot supports commanding position and velocity targets in global scaled integers. | */
   public static final int MAV_PROTOCOL_CAPABILITY_TERRAIN = 512; /* Autopilot supports terrain protocol / data handling. | */
   public static final int MAV_PROTOCOL_CAPABILITY_SET_ACTUATOR_TARGET = 1024; /* Autopilot supports direct actuator control. | */
   public static final int MAV_PROTOCOL_CAPABILITY_FLIGHT_TERMINATION = 2048; /* Autopilot supports the flight termination command. | */
   public static final int MAV_PROTOCOL_CAPABILITY_COMPASS_CALIBRATION = 4096; /* Autopilot supports onboard compass calibration. | */
   public static final int MAV_PROTOCOL_CAPABILITY_ENUM_END = 4097; /*  | */
}
            