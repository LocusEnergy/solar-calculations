package com.locusenergy.solarcalculations;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * computes relevant solar calculations
 * modified from NOAA's calculators:
 * http://www.srrb.noaa.gov/highlights/sunrise/azel.html
 * http://www.esrl.noaa.gov/gmd/grad/solcalc/sunrise.html
 * javascript: https://www.esrl.noaa.gov/gmd/grad/solcalc/main.js
 */
public class SolarCalculations {

  private static final double I_SC = 1367; // accepted solar constant [W/m^2]
  public static final int SUNRISE = 1; // constant for use in sunrise
  public static final int SUNSET = -1; // constant for use in sunset

  private double latitude; // latitude, north positive [degrees]
  private double longitude; // longitude, east positive [degrees]
  private boolean dst; // use daylight savings? default: true

  public SolarCalculations(double latitude, double longitude, boolean dst) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.dst = dst;
  }

  public SolarCalculations(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.dst = true;
  }

  public SolarCalculations() {}

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getLongitude() {
    return longitude;
  }

  private double calcJulianDate(Calendar cal) {
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1;
    int day = cal.get(Calendar.DAY_OF_MONTH);

    if (month <= 2) {
      year--;
      month += 12;
    }

    double a = Math.floor(year/100);
    double b = 2 - a + Math.floor(a/4);
    double output = Math.floor(365.25*(year+4716)) + Math.floor(30.6001*(month+1)) + day + b - 1524.5;
    return output;
  }

  public double calcTimeDecimal(Calendar cal) {
    double hour = (double) cal.get(Calendar.HOUR_OF_DAY);
    double minute = (double) cal.get(Calendar.MINUTE);
    double second = (double) cal.get(Calendar.SECOND);
    return hour + minute/60 + second/3600;
  }

  public int[] calcHourMinSec(double timeDecimal) {
    int[] output = new int[3];
    output[0] = (int) timeDecimal; // hour
    double minutesDecimal = (timeDecimal - output[0])*60;
    output[1] = (int) minutesDecimal;
    output[2] = (int) ((minutesDecimal - output[1])*60);
    return output;
  }

  public Calendar setCalHourMinSec(Calendar cal, double timeDecimal) {
    Calendar calCopy = (Calendar) cal.clone();

    int[] hourMinSec = calcHourMinSec(timeDecimal);

    int day = calCopy.get(Calendar.DAY_OF_MONTH);

    calCopy.set(Calendar.HOUR_OF_DAY, hourMinSec[0]);
    calCopy.set(Calendar.MINUTE, hourMinSec[1]);
    calCopy.set(Calendar.SECOND, hourMinSec[2]);

    if (timeDecimal < 0) {
      calCopy.set(Calendar.DAY_OF_MONTH, day+1);
    }

    return calCopy;
  }

  private double getOffset(Calendar cal) {
    if (dst) // use DST
      return (cal.get(Calendar.DST_OFFSET) + cal.get(Calendar.ZONE_OFFSET))/(3600000);
    else // don't use DST
      return (cal.get(Calendar.ZONE_OFFSET))/(3600000);
  }

  // assumes datetime given in local time
  public double calcTimeJulian(Calendar cal) {
    double jd = calcJulianDate(cal);
    double time = calcTimeDecimal(cal);
    double offset = getOffset(cal);
    double output = (jd + (time - offset)/24 - 2451545)/36525;
    return output;
  }

  private double calcGeomMeanLongSun(double timeJulian) {
    double output = 280.46646 + timeJulian*(36000.76983 + 0.0003032*timeJulian);
    while (output > 360) output -= 360;
    while (output < 0) output += 360;
    return output;
  }

  private double calcGeomMeanAnomalySun(double timeJulian) {
    double output = 357.52911 + timeJulian*(35999.05029 - 0.0001537*timeJulian);
    return output;
  }

  private double calcEccentricityEarthOrbit(double timeJulian) {
    double output = 0.016708634 - timeJulian*(0.000042037 + 0.0000001267*timeJulian);
    return output;
  }

  private double calcSunEqOfCenter(double timeJulian) {
    double m = calcGeomMeanAnomalySun(timeJulian);
    double output = Trig.sinD(m)*(1.914602 - timeJulian*(0.004817 + 0.000014*timeJulian)) + Trig.sinD(m*2)*(0.019993 - 0.000101*timeJulian) + Trig.sinD(m*3)*0.000289;
    return output;
  }

  private double calcSunTrueLong(double timeJulian) {
    double output = calcGeomMeanLongSun(timeJulian) + calcSunEqOfCenter(timeJulian);
    return output;
  }

  private double calcSunApparentLong(double timeJulian) {
    double o = calcSunTrueLong(timeJulian);
    double omega = 125.04 - 1934.136*timeJulian;
    double output = o - 0.00569 - 0.00478 * Trig.sinD(omega);
    return output;
  }

  private double calcMeanObliquityOfEcliptic(double timeJulian) {
    double seconds = 21.448 - timeJulian*(46.8150 + timeJulian*(0.00059 - timeJulian*(0.001813)));
    double output = 23 + (26 + seconds/60)/60;
    return output;
  }

  private double calcObliquityCorrection(double timeJulian) {
    double e0 = calcMeanObliquityOfEcliptic(timeJulian);
    double omega = 125.04 - 1934.136*timeJulian;
    double output = e0 + 0.00256*Trig.cosD(omega);
    return output;
  }

  // returns: solar declination [degrees]
  public double calcSolarDeclination(Calendar cal) {
    double timeJulian = calcTimeJulian(cal);
    double e = calcObliquityCorrection(timeJulian);
    double lambda = calcSunApparentLong(timeJulian);
    double sint = Trig.sinD(e)*Trig.sinD(lambda);
    double output = Math.toDegrees(Math.asin(sint));
    return output;
  }

  // returns: equation of time in minutes
  public double calcEquationOfTime(Calendar cal) {
    double timeJulian = calcTimeJulian(cal);
    double epsilon = calcObliquityCorrection(timeJulian);
    double l0 = calcGeomMeanLongSun(timeJulian);
    double e = calcEccentricityEarthOrbit(timeJulian);
    double m = calcGeomMeanAnomalySun(timeJulian);

    double y = Trig.tanD(epsilon/2);
    y *= y;
    double sin2l0 = Trig.sinD(2*l0);
    double sinm = Trig.sinD(m);
    double cos2l0 = Trig.cosD(2*l0);
    double sin4l0 = Trig.sinD(4*l0);
    double sin2m = Trig.sinD(2*m);
    double eqTime = y*sin2l0 - 2*e*sinm + 4*e*y*sinm*cos2l0 - 0.5*y*y*sin4l0 - 1.25*e*e*sin2m;
    double output = Math.toDegrees(eqTime)*4;
    return output;
  }

  // returns: true solar time in minutes
  private double calcTrueSolarTime(Calendar cal) {
    double eqTime = calcEquationOfTime(cal);
    double time = calcTimeDecimal(cal);
    double offset = getOffset(cal);
    double solarTimeFix = eqTime + 4*this.longitude - 60*offset;
    double trueSolarTime = time*60 + solarTimeFix;
    while (trueSolarTime > 1440) trueSolarTime -= 1440;
    double output = trueSolarTime;
    return output;
  }

  // returns: hour angle [degrees]
  private double calcHourAngle(Calendar cal) {
    double trueSolarTime = calcTrueSolarTime(cal);
    double hourAngle = trueSolarTime/4 - 180;
    if (hourAngle < -180) hourAngle += 360;
    double output = hourAngle;
    return output;
  }

  // returns: solar zenith [degrees]
  // refraction: boolean to indicate whether to calculate the refraction correction. true calculates refraction correction, false does not
  public double calcSolarZenith(Calendar cal, boolean refraction) {
    double solarDeclination = calcSolarDeclination(cal);
    double hourAngle = calcHourAngle(cal);
    double csz = Trig.sinD(this.latitude)*Trig.sinD(solarDeclination) +
            Trig.cosD(this.latitude)*Trig.cosD(solarDeclination)*Trig.cosD(hourAngle);
    double solarZenith = Math.toDegrees(Math.acos(csz));

    if (refraction) {
      double solarElevation = 90 - solarZenith;
      double refractionCorrection = 0;
      double te = Trig.tanD(solarElevation);
      if (solarElevation <= -0.575) refractionCorrection = -20.774/te;
      else if (solarElevation <= 5) refractionCorrection = 1735 + solarElevation*(-518.2 + solarElevation*(103.4 + solarElevation*(-12.79 + solarElevation*0.711)));
      else if (solarElevation <= 85) refractionCorrection = 58.1/te - 0.07/Math.pow(te, 3) + 0.000086/Math.pow(te, 5);
      else refractionCorrection = 0;
      solarZenith -= refractionCorrection/3600;
    }

    double output = solarZenith;
    return output;
  }

  // returns: solar zenith [degrees]
  // overloads calcSolarZenith(int year, int month, int day, double time, boolean refraction)
  // defaults "boolean refraction" to true
  public double calcSolarZenith(Calendar cal) {
    double output = calcSolarZenith(cal, true);
    return output;
  }

  // returns: solar azimuth, due south is 0 [degrees]
  public double calcSolarAzimuth(Calendar cal) {
    double solarDeclination = calcSolarDeclination(cal);
    double hourAngle = calcHourAngle(cal);
    double solarZenith = calcSolarZenith(cal, false);

    double output = Math.toDegrees( Math.signum(hourAngle) * Math.acos( (Trig.cosD(solarZenith)*Trig.sinD(this.latitude) - Trig.sinD(solarDeclination)) / (Trig.sinD(solarZenith)*Trig.cosD(this.latitude)) ) );
    return output;
  }

  // returns: air mass [units?]
  public double calcAirMass(Calendar cal) {
    double solarZenith = calcSolarZenith(cal);
    double output = 0;
    if (solarZenith < 90) output = (1.002432*Math.pow(Trig.cosD(solarZenith), 2) + 0.148386*Trig.cosD(solarZenith) + 0.0096467) / (Math.pow(Trig.cosD(solarZenith), 3) + 0.149864*Math.pow(Trig.cosD(solarZenith), 2) + 0.0102963 * Trig.cosD(solarZenith) + 0.000303978);
    return output;
  }

  // returns: extraterrestrial irradiance [W/m^2]
  public double calcExtraIrradiance(Calendar cal) {
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    double doy = cal.get(Calendar.DAY_OF_YEAR);
    double output = I_SC*(1.00011 + 0.034221*Trig.cosD(360*doy/365) + 0.00128*Trig.sinD(360*doy/365) + 0.000719*Trig.cosD(2*360*doy/365) + 0.000077*Trig.sinD(2*360*doy/365));
    return output;
  }

  // returns: sunrise or sunset in local time [hours]
  // inputs:
  // srss: +1 or -1, +1 for sunrise, -1 for sunset. Constants SUNRISE and SUNSET can be used for the appropriate values.
  public double calcSunriseSet(Calendar cal, int srss) {
    double noonmin = calcSolNoon(cal);
    cal = setCalHourMinSec(cal, noonmin/60);
    double eqTime = calcEquationOfTime(cal);
    double hourAngle = calcHourAngleSunrise(cal)*srss;
    double delta = -this.longitude - hourAngle;
    double timeDiff = 4*delta;
    double timeUTC = 720 + timeDiff - eqTime;
    cal = setCalHourMinSec(cal, timeUTC/60);
    eqTime = calcEquationOfTime(cal);
    hourAngle = calcHourAngleSunrise(cal)*srss;
    delta = -this.longitude - hourAngle;
    timeDiff = 4*delta;
    timeUTC = (720 + timeDiff - eqTime)/60;
    double output = timeUTC + getOffset(cal);
    return output;
  }

  // returns: solar noon in UTC [hours]
  private double calcSolNoon(Calendar cal) {
    cal = setCalHourMinSec(cal, this.longitude*24/360);
    double eqTime = calcEquationOfTime(cal);
    double solNoonUTC = 720 - (this.longitude*4) - eqTime;
    cal = setCalHourMinSec(cal, solNoonUTC/60 - 12);
    eqTime = calcEquationOfTime(cal);
    solNoonUTC = 720 - (this.longitude*4) - eqTime;
    return solNoonUTC;
  }

  // returns: hour angle of sunrise [degrees]
  // the inverse of the returned value is the hour angle of sunset
  private double calcHourAngleSunrise(Calendar cal) {
    double solarDeclination = calcSolarDeclination(cal);
    double hourAngle = Math.toDegrees(Math.acos(Trig.cosD(90.833)/(Trig.cosD(this.latitude)*Trig.cosD(solarDeclination))-Trig.tanD(this.latitude) * Trig.tanD(solarDeclination)));
    return hourAngle;
  }

  public boolean isDay(Calendar cal) {
    double time = calcTimeDecimal(cal);
    double sunrise = calcSunriseSet(cal, SUNRISE);// + offset;
    double sunset = calcSunriseSet(cal, SUNSET);// + offset;

    return ((time > sunrise) && (time < sunset));
  }
}
