import sensors from "./src/sensors";
export { setUpdateInterval as setUpdateIntervalForType } from "./src/rnsensors";

export const SensorTypes = {
  accelerometer: "accelerometer",
  gyroscope: "gyroscope",
  magnetometer: "magnetometer",
  barometer: "barometer",
  rotation: "rotation"
};

export const { accelerometer, gyroscope, magnetometer, barometer, rotation } = sensors;
export default sensors;
