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
package com.meistercharts.demo.descriptors

import com.meistercharts.algorithms.ResetToDefaultsOnWindowResize
import com.meistercharts.algorithms.impl.FittingWithMargin
import com.meistercharts.algorithms.layers.AbstractLayer
import com.meistercharts.algorithms.layers.ContentAreaLayer
import com.meistercharts.algorithms.layers.LayerPaintingContext
import com.meistercharts.algorithms.layers.LayerType
import com.meistercharts.algorithms.layers.addClearBackground
import com.meistercharts.algorithms.painter.stripe.enums.EnumAggregationMode
import com.meistercharts.algorithms.painter.stripe.enums.RectangleEnumStripePainter
import com.meistercharts.canvas.BindContentAreaSize2ContentViewport
import com.meistercharts.canvas.paintMark
import com.meistercharts.canvas.translateToContentAreaOrigin
import com.meistercharts.demo.ChartingDemo
import com.meistercharts.demo.ChartingDemoDescriptor
import com.meistercharts.demo.DemoCategory
import com.meistercharts.demo.PredefinedConfiguration
import com.meistercharts.demo.configurableDouble
import com.meistercharts.demo.configurableEnum
import com.meistercharts.history.DataSeriesId
import com.meistercharts.history.EnumDataSeriesIndex
import com.meistercharts.history.HistoryEnum
import com.meistercharts.history.HistoryEnumOrdinal
import com.meistercharts.history.HistoryEnumSet
import com.meistercharts.history.historyConfiguration
import com.meistercharts.model.Direction
import com.meistercharts.model.Insets
import it.neckar.open.observable.ObservableDouble

/**
 * A simple hello world demo
 */
class RectangleEnumStripePainterDemoDescriptor : ChartingDemoDescriptor<Nothing> {
  override val name: String = "Rectangle Enum Bar Stripe Painter"

  //language=HTML
  override val category: DemoCategory = DemoCategory.Painters

  override fun createDemo(configuration: PredefinedConfiguration<Nothing>?): ChartingDemo {
    return ChartingDemo {
      meistercharts {
        val margin = Insets(20.0, 20.0, 20.0, 70.0)

        zoomAndTranslationDefaults {
          FittingWithMargin(margin)
        }
        contentAreaSizingStrategy = BindContentAreaSize2ContentViewport()

        val historyConfiguration = historyConfiguration {
          enumDataSeries(DataSeriesId(0), "Enum with 5 options", HistoryEnum.createSimple("Simple", listOf("zero", "one", "two", "three", "four", "five", "six")))
          enumDataSeries(DataSeriesId(1), "Enum with 2 options", HistoryEnum.Boolean)
        }

        configure {
          chartSupport.windowResizeBehavior = ResetToDefaultsOnWindowResize

          layers.addClearBackground()
          layers.addLayer(ContentAreaLayer())

          val enumStripePainter = RectangleEnumStripePainter().also {
            it.configuration.aggregationMode = EnumAggregationMode.ByOrdinal
          }

          val heightProperty = ObservableDouble(20.0)

          configurableDouble("Enum Bar Height", heightProperty) {
            max = 150.0
          }

          configurableEnum("Aggregation Type", enumStripePainter.configuration::aggregationMode)

          val painterLayer = object : AbstractLayer() {
            override val type: LayerType = LayerType.Content

            override fun paint(paintingContext: LayerPaintingContext) {
              val gc = paintingContext.gc
              val chartCalculator = paintingContext.chartCalculator

              gc.fillText("Rect Enum Bar Stripe Painter below...", 0.0, 0.0, Direction.TopLeft)
              gc.translateToContentAreaOrigin(chartCalculator)

              gc.paintMark()
              paintEnumValues(paintingContext)

              gc.translate(0.0, heightProperty.value + 10.0)

              paintBooleanValues(paintingContext)
            }

            private fun paintEnumValues(paintingContext: LayerPaintingContext) {
              val enumOrdinalMostTime = HistoryEnumOrdinal(4)

              enumStripePainter.begin(paintingContext, heightProperty.value, EnumDataSeriesIndex.zero, historyConfiguration)
              enumStripePainter.valueChange(paintingContext, 0.0, 100.0, HistoryEnumSet(0b101), enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 100.0, 180.0, HistoryEnumSet(0b111111), enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 180.0, 250.0, HistoryEnumSet.second, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 250.0, 320.0, HistoryEnumSet.third, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 320.0, 350.0, HistoryEnumSet.NoValue, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 350.0, 450.0, HistoryEnumSet.first, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 450.0, 460.0, HistoryEnumSet(0b11), enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 460.0, 465.0, HistoryEnumSet(0b101), enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 465.0, 500.0, HistoryEnumSet.third, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 500.0, 550.0, HistoryEnumSet.Pending, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 550.0, 620.0, HistoryEnumSet.third, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.finish(paintingContext)
            }

            private fun paintBooleanValues(paintingContext: LayerPaintingContext) {
              val enumOrdinalMostTime = HistoryEnumOrdinal(1)

              enumStripePainter.begin(paintingContext, heightProperty.value, EnumDataSeriesIndex.zero, historyConfiguration)
              enumStripePainter.valueChange(paintingContext, 0.0, 100.0, HistoryEnumSet.first, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 100.0, 150.0, HistoryEnumSet.first, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 150.0, 250.0, HistoryEnumSet.second, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 250.0, 300.0, HistoryEnumSet.first, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 300.0, 350.0, HistoryEnumSet.NoValue, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 350.0, 450.0, HistoryEnumSet.second, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 450.0, 500.0, HistoryEnumSet.second, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 500.0, 510.0, HistoryEnumSet.NoValue, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 510.0, 515.0, HistoryEnumSet.first, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.valueChange(paintingContext, 515.0, 600.0, HistoryEnumSet.second, enumOrdinalMostTime, Unit, Unit)
              enumStripePainter.finish(paintingContext)
            }
          }
          layers.addLayer(painterLayer)
        }
      }
    }
  }
}
