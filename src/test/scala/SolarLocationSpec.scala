package com.locusenergy

import org.scalatest._
import com.locusenergy.solarlocation.SolarLocation
import java.util.{Calendar, TimeZone}
import java.util.Calendar.HOUR_OF_DAY

class SolarLocationSpec extends FlatSpec with Matchers {

  def fixture = new {
    // Using San Francisco's coordinates
    val lat = 37.775
    val lon = -122.4183333
    val solarlocation = new SolarLocation(lat, lon)
    val date = Calendar.getInstance()
    date.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
  }

  "SolarLocation" should "return the same lat and lon it is initialized with" in {
    assert(fixture.solarlocation.getLatitude() === fixture.lat)
    assert(fixture.solarlocation.getLongitude() === fixture.lon)
  }

  "isDay()" should "return true when it's noon" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 12)
    assert(f.solarlocation.isDay(f.date) === true)
  }

  "isDay()" should "return true when it's 9am" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 9)
    assert(f.solarlocation.isDay(f.date) === true)
  }

  "isDay()" should "return true when it's 3pm" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 15)
    assert(f.solarlocation.isDay(f.date) === true)
  }

  it should "return false when it's midnight" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 24)
    assert(f.solarlocation.isDay(f.date) === false)
  }

  it should "return false when it's 9pm" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 21)
    assert(f.solarlocation.isDay(f.date) === false)
  }

  it should "return false when it's 3am" in {
    val f = fixture
    f.date.set(HOUR_OF_DAY, 3)
    assert(f.solarlocation.isDay(f.date) === false)
  }

}