/**
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meistercharts.canvas

import com.meistercharts.events.ImageLoadedEventBroker
import com.meistercharts.js.MeisterChartClasses
import it.neckar.geometry.Size
import it.neckar.logging.LoggerFactory
import it.neckar.logging.debug
import it.neckar.open.http.Url
import kotlinx.browser.document
import org.w3c.dom.HTMLImageElement

/**
 * Loads an image from a URL (should not use caches)
 *
 */
actual fun loadImageUncached(url: Url, callback: (Image) -> Unit) {
  logger.debug { "Loading image: $url" }

  (document.createElement("IMG") as HTMLImageElement).apply {
    this.classList.add(MeisterChartClasses.imageLoader)

    style.width = "${this.width} px"
    style.height = "${this.height} px"

    // only after the load-event IE11 is able to draw the image on a canvas
    addEventListener("load", {
      logger.debug {
        "Image loaded: $url"
      }

      // update image property or UrlPaintable
      callback(createImage())
      // notify the world about the event
      ImageLoadedEventBroker.notifyLoaded()
    })
    addEventListener("error", {
      logger.warn("Could not load image <$url>")
    })

    // set src after(!) adding the event-listener for the load-event
    src = UrlConversion.convert(url).value

    // Check if the image has been loaded already to avoid flickering.
    // Beware that 'complete' evaluates to true in IE11 for data-images.
    // However, IE11 cannot render data-images until the load-event has fired.
    if (complete && !src.startsWith("data:image", true)) {
      //Already loaded (from cache?) Therefore, we instantiate the image immediately
      callback(createImage())
    }
  }
}

/**
 * Creates a new Image object from an HTMLImageElement.
 *
 * @receiver the HTMLImageElement from which to create the Image object
 * @return the newly created Image object
 */
private fun HTMLImageElement.createImage(): Image {
  return Image(this, Size(this.naturalWidth.toDouble(), this.naturalHeight.toDouble()))
}

private val logger = LoggerFactory.getLogger("com.meistercharts.canvas.LoadImageJs")
