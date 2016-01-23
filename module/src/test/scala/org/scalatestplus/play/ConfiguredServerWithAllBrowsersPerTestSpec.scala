/*
 * Copyright 2001-2014 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatestplus.play

import play.api.test._
import org.scalatest._
import play.api.{Play, Application}
import play.api.mvc.{Action, Results}
import play.api.inject.guice._
import play.api.routing._
import play.api.routing.sird._

class ConfiguredServerWithAllBrowsersPerTestSpec extends Suites(
  new ConfiguredServerWithAllBrowsersPerTestNestedSpec 
)
with OneServerPerSuite {
  override lazy val app: Application =
    new GuiceApplicationBuilder().configure("foo" -> "bar", "ehcacheplugin" -> "disabled").additionalRouter(Router.from {
      case GET(p"/testing") =>
        Action(
          Results.Ok(
            "<html>" +
              "<head><title>Test Page</title></head>" +
              "<body>" +
              "<input type='button' name='b' value='Click Me' onclick='document.title=\"scalatest\"' />" +
              "</body>" +
              "</html>"
          ).as("text/html")
        )
    }).build()
}

@DoNotDiscover
class ConfiguredServerWithAllBrowsersPerTestNestedSpec extends UnitSpec with ConfiguredServer with AllBrowsersPerTest {

  def getConfig(key: String)(implicit app: Application) = app.configuration.getString(key)

  def sharedTests(browser: BrowserInfo) = {

    "The AllBrowsersPerTest trait" must {
      "provide a web driver " + browser.name in {
        go to ("http://localhost:" + port + "/testing")
        pageTitle mustBe "Test Page"
        click on find(name("b")).value
        eventually { pageTitle mustBe "scalatest" }
      }
    }
  }

  "The AllBrowsersPerTest trait" must {
    "provide an Application" in {
      app.configuration.getString("foo") mustBe Some("bar")
    }
    "make the Application available implicitly" in {
      getConfig("foo") mustBe Some("bar")
    }
    "start the Application" in {
      Play.maybeApplication mustBe Some(app)
    }
    "provide the port" in {
      port mustBe Helpers.testServerPort
    }
    "send 404 on a bad request" in {
      import java.net._
      val url = new URL("http://localhost:" + port + "/boum")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      try con.getResponseCode mustBe 404
      finally con.disconnect()
    }
    "provide an UnneededDriver to non-shared test whose methods throw UnsupportedOperationException with an error message that gives a hint to put the test into the sharedTests method" in {
      the [UnsupportedOperationException] thrownBy webDriver.get("funky") must have message Resources("webDriverUsedFromUnsharedTest")
    }
  }
}
