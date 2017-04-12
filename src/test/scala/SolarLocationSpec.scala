package com.locusenergy

import org.scalatest._
import com.locusenergy.solarcalculations.SolarCalculations
import java.util.{Calendar, TimeZone}
import java.text.SimpleDateFormat;
import java.util.Calendar.HOUR_OF_DAY
import scala.math._

class SolarCalculationsSpec extends FlatSpec with Matchers {

  def fixture = new {
    // Using San Francisco's coordinates
    val lat = 37.775
    val lon = -122.4183333
    val solarcalculations = new SolarCalculations(lat, lon)
    val date = Calendar.getInstance()
    date.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
    val datefixed = Calendar.getInstance()
    datefixed.setTime(new SimpleDateFormat("yyyy-MM-dd hh:mm").parse("2017-03-14 17:00"))
    datefixed.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
  }

  "SolarCalculations" should "return the same lat and lon it is initialized with" in {
    assert(fixture.solarcalculations.getLatitude() === fixture.lat)
    assert(fixture.solarcalculations.getLongitude() === fixture.lon)
  }

  "isDay()" should "return true when it's noon" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 12)
    assert(f.solarcalculations.isDay(f.date) === true)
  }

  "isDay()" should "return true when it's 9am" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 9)
    assert(f.solarcalculations.isDay(f.date) === true)
  }

  "isDay()" should "return true when it's 3pm" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 15)
    assert(f.solarcalculations.isDay(f.date) === true)
  }

  it should "return false when it's midnight" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 24)
    assert(f.solarcalculations.isDay(f.date) === false)
  }

  it should "return false when it's 9pm" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 21)
    assert(f.solarcalculations.isDay(f.date) === false)
  }

  it should "return false when it's 3am" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 3)
    assert(f.solarcalculations.isDay(f.date) === false)
  }

  "SolarCalculations" should "match NOAA calculator" in {
    // values were pulled from NOAA calculator 03/27/2017
    // https://www.esrl.noaa.gov/gmd/grad/solcalc/
    assert(abs(fixture.solarcalculations.calcEquationOfTime(fixture.datefixed) - -8.98) < 0.01)
    assert(abs(180 + fixture.solarcalculations.calcSolarAzimuth(fixture.datefixed) - 245.32) < 0.01)
    assert(abs(fixture.solarcalculations.calcSolarDeclination(fixture.datefixed) - -2.15) < 0.01)
  }

  "SolarCalculations" should "match R locuscore" in {
    // values were pulled from R locuscore 03/27/2017
    assert(abs(fixture.solarcalculations.calcTimeJulian(fixture.datefixed) - 0.172005) < 0.000001)
    assert(abs(fixture.solarcalculations.calcExtraIrradiance(fixture.datefixed) - 1381.756) < 0.001)
    assert(abs(fixture.solarcalculations.calcSolarZenith(fixture.datefixed) - 64.734) < 0.001)
    assert(abs(fixture.solarcalculations.calcAirMass(fixture.datefixed) - 2.328769) < 0.000001)
  }

}