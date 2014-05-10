package com.devdaily.sarah.plugin.weather

import com.devdaily.sarah.plugins._
import java.io._
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import scala.collection.mutable.StringBuilder
import scala.xml.XML
import java.util.Properties
import akka.actor.ActorSystem
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Get (a) the current weather or (b) the weather forecast.
 */
class WeatherPlugin extends SarahPlugin {

  val phrasesICanHandle = List("current weather", "weather forecast")
  val CURRENT_WEATHER = "current weather"
  val FORECAST = "weather forecast"
  val WEATHER_URL = "http://weather.yahooapis.com/forecastrss?p=%s&u=f" 

  var canonPluginDirectory = ""
  val REL_PROPERTIES_FILENAME = "Weather.properties"
  val zipCodeKey = "zip_code"
  var zipCode = "99676"
  
  implicit val system = ActorSystem("DDWeatherActorSystem")

  // sarah callback
  def textPhrasesICanHandle: List[String] = {
    return phrasesICanHandle
  }

  // sarah callback
  override def setPluginDirectory(dir: String) {
    canonPluginDirectory = dir
  }

  // sarah callback
  def startPlugin = {
    zipCode = getZipCodeFromConfigFile(getCanonPropertiesFilename)
    println("WEATHER: zip code = " + zipCode)
  }

  // sarah callback. handle our phrases when we get them.
  def handlePhrase(phrase: String): Boolean = {
    if (phrase.trim.equals(CURRENT_WEATHER)) {
      val f1 = Future { brain ! PleaseSay("Stand by.") }
      val f2 = Future { brain ! PleaseSay(getCurrentWeather) }
      return true
    } else if (phrase.trim.equals(FORECAST)) {
      val f1 = Future { brain ! PleaseSay("Stand by.") }
      val f2 = Future { brain ! PleaseSay(getWeatherForecast) }
      return true
    } else {
      return false
    }
  }
  
  val exceptionHappenedText = "Sorry, got an exception trying to check the weather."
  val unknownHappenedText = "Sorry, ran into an unknown problem checking the weather."

  def getWeatherForecast:String = {
    try {
      // 1) get the content from the yahoo weather api url
      val content = getRestContent(buildWeatherUrl(zipCode))
    
      // 2) convert it to xml
      val xml = XML.loadString(content)
      
      // 3) get the elements i want
      val days = Map("Mon" -> "Monday", 
                     "Tue" -> "Tuesday", 
                     "Wed" -> "Wednesday", 
                     "Thu" -> "Thursday", 
                     "Fri" -> "Friday", 
                     "Sat" -> "Saturday", 
                     "Sun" -> "Sunday")
      val sb = new StringBuilder
      for (i <- 0 until 2) {
        val day = (xml \\ "channel" \\ "item" \ "forecast")(i) \ "@day"
        val date = (xml \\ "channel" \\ "item" \ "forecast")(i) \ "@date"
        val low = (xml \\ "channel" \\ "item" \ "forecast")(i) \ "@low"
        val high = (xml \\ "channel" \\ "item" \ "forecast")(i) \ "@high"
        val text = (xml \\ "channel" \\ "item" \ "forecast")(i) \ "@text"
        if (i == 0) {
          sb.append(format("Here's the forecast. For %s, a low of %s, a high of %s, and %s skies. ", days(day.toString.trim), low, high, text ))
        } else {
          sb.append(format("For %s, a low of %s, a high of %s, and %s skies. ", days(day.toString.trim), low, high, text ))
        }
      }
      return sb.toString

    } catch {
      case e: Exception => println("(WeatherPlugin) caught a plain Exception: " + e.getMessage)
                           return exceptionHappenedText
      case unknown: Throwable =>      return unknownHappenedText
    }
  }
  
  def getCurrentWeather:String = {
    try {
      // (1) get the content from the yahoo weather api url
      val content = getRestContent(buildWeatherUrl(zipCode))
    
      // (2) convert it to xml
      val xml = XML.loadString(content)
      assert(xml.isInstanceOf[scala.xml.Elem])  // needed?

      // (3) search the xml for the nodes i want
      val temp = (xml \\ "channel" \\ "item" \ "condition" \ "@temp") text
      val text = (xml \\ "channel" \\ "item" \ "condition" \ "@text") text

      // (4) print the results
      return format("The current temperature is %s degrees, and the sky is %s.", temp, text.toLowerCase)
    } catch {
      case e: Exception => println("(WeatherPlugin) caught a plain Exception: " + e.getMessage)
                           return exceptionHappenedText
      case unknown: Throwable =>      return unknownHappenedText
    }
  }
  
  def buildWeatherUrl(zipCode: String): String = {
    return format(WEATHER_URL, zipCode)
  }
  
  def getCanonPropertiesFilename: String = {
    return canonPluginDirectory + PluginUtils.getFilepathSeparator + REL_PROPERTIES_FILENAME
  }

  
  /**
   * Returns the text content from a REST URL. Returns a blank String if there
   * is a problem.
   */
  def getRestContent(url:String): String = {
    val httpClient = new DefaultHttpClient()
    val httpResponse = httpClient.execute(new HttpGet(url))
    val entity = httpResponse.getEntity()
    var content = ""
    if (entity != null) {
      val inputStream = entity.getContent()
      content = io.Source.fromInputStream(inputStream).getLines.mkString
      inputStream.close
    }
    httpClient.getConnectionManager().shutdown()
    return content
  }
  
  // TODO handle exceptions
  def getZipCodeFromConfigFile(canonConfigFilename: String): String = {
	val properties = new Properties()
	val in = new FileInputStream(canonConfigFilename)
	properties.load(in)
	in.close()
	return properties.getProperty(zipCodeKey);
  }

  
  
  
}



