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
package com.meistercharts.algorithms.layers

import com.meistercharts.algorithms.painter.ArrowHead
import com.meistercharts.annotations.Domain
import com.meistercharts.annotations.DomainRelative
import com.meistercharts.annotations.Window
import com.meistercharts.annotations.ZIndex
import com.meistercharts.calc.ChartCalculator
import com.meistercharts.canvas.ConfigurationDsl
import com.meistercharts.canvas.DebugFeature
import com.meistercharts.font.FontDescriptorFragment
import com.meistercharts.canvas.text.LineSpacing
import com.meistercharts.canvas.layout.cache.BoundsLayoutCache
import com.meistercharts.canvas.layout.cache.CoordinatesCache
import com.meistercharts.canvas.layout.cache.ObjectsCache
import com.meistercharts.canvas.layout.cache.ZIndexSortingCache
import com.meistercharts.canvas.paintLocation
import com.meistercharts.canvas.paintTextBox
import com.meistercharts.canvas.saved
import com.meistercharts.color.Color
import com.meistercharts.design.Theme
import it.neckar.geometry.Direction
import it.neckar.geometry.HorizontalAlignment
import it.neckar.geometry.Orientation
import it.neckar.geometry.Side
import com.meistercharts.model.Vicinity
import com.meistercharts.style.BoxStyle
import com.meistercharts.style.Shadow
import it.neckar.open.provider.CoordinatesProvider1
import it.neckar.open.provider.DoublesProvider
import it.neckar.open.provider.MultiDoublesProvider
import it.neckar.open.provider.MultiProvider
import it.neckar.open.provider.MultiProvider1
import it.neckar.open.provider.MultiProviderIndexContextAnnotation
import it.neckar.open.provider.fastForEachIndexed
import it.neckar.open.unit.number.MayBeNaN
import it.neckar.open.unit.number.MayBeNegative
import it.neckar.open.unit.other.px

/**
 * Provides the labels for each HUD element
 */
typealias HudLabelsProvider = MultiProvider1<HudElementIndex, List<String>, LayerPaintingContext>

/**
 * Paints the HUD (usually on the value axis).
 *
 */
class ValueAxisHudLayer(
  val configuration: Configuration,
  additionalConfiguration: Configuration.() -> Unit = {},
  ) : AbstractLayer() {

  constructor(
    /**
     * Provides the anchor location of the hud element.
     * If NaN is returned, the element is not painted
     */
    locations: @MayBeNaN @Window CoordinatesProvider1<LayerPaintingContext>,

    /**
     * Provides the text that is painted within the HUD element
     */
    labels: HudLabelsProvider,

    additionalConfiguration: Configuration.() -> Unit = {},
  ): this(Configuration(locations, labels), additionalConfiguration)

  init {
    configuration.additionalConfiguration()
  }

  override val type: LayerType = LayerType.Content

  override fun paintingVariables(): ValueAxisHudLayerPaintingVariables {
    return paintingVariables
  }

  private val paintingVariables = object : ValueAxisHudLayerPaintingVariables {
    /**
     * Contains the coordinates for the HUD element
     */
    override val coordinatesCache = @Window @MayBeNaN CoordinatesCache()

    override val anchorDirectionsCache = ObjectsCache(Direction.TopLeft)

    /**
     * Contains the labels
     */
    override val labelsCache = ObjectsCache<List<String>>(emptyList())

    /**
     * ATTENTION: Filled in the *paint* method!
     * Must only be used after paint has been called (e.g. for mouse events)
     */
    override val boundingBoxes: BoundsLayoutCache = BoundsLayoutCache()

    override val zOrder = ZIndexSortingCache()

    override fun calculate(paintingContext: LayerPaintingContext) {
      val chartSupport = paintingContext.chartSupport

      @HudElementIndex val size = configuration.locations.size(paintingContext)

      //Prepare the caches
      coordinatesCache.prepare(size)
      anchorDirectionsCache.prepare(size)
      labelsCache.prepare(size)
      zOrder.prepare(size)
      boundingBoxes.prepare(size)

      configuration.locations.fastForEachIndexed(paintingContext) { index: @HudElementIndex Int, x: @MayBeNaN @Window Double, y: @MayBeNaN @Window Double ->
        coordinatesCache.x(index, x)
        coordinatesCache.y(index, y)

        anchorDirectionsCache[index] = configuration.anchorDirections.valueAt(index)
        labelsCache[index] = configuration.labels.valueAt(index, paintingContext)

        zOrder[index] = com.meistercharts.model.ZIndex(configuration.zOrder.valueAt(index))
      }

      zOrder.sortByZIndex()
    }
  }

  override fun paint(paintingContext: LayerPaintingContext) {
    val gc = paintingContext.gc
    val chartCalculator = paintingContext.chartCalculator

    paintingVariables.zOrder.fastForEach { value ->
      @HudElementIndex val index = value.index

      @Window @MayBeNaN val x = paintingVariables.coordinatesCache.x(index)
      @Window @MayBeNaN val y = paintingVariables.coordinatesCache.y(index)

      if (x.isFinite().not() || y.isFinite().not()) {
        //Skip if x or y are not finite
        return@fastForEach
      }

      val active = index == configuration.activeHudElementIndex

      gc.saved {
        gc.translate(x, y)

        paintingContext.ifDebug(DebugFeature.ShowAnchors) {
          gc.paintLocation()
        }

        val anchorDirection = paintingVariables.anchorDirectionsCache[index]

        val arrowHeadLength = configuration.arrowHeadLength.valueAt(index)
        val arrowHeadWidth = configuration.arrowHeadWidth.valueAt(index)

        gc.font(configuration.textFonts.valueAt(index))


        val textColor: Color
        val boxStyle: BoxStyle

        if (active) {
          textColor = configuration.textColorsActive.valueAt(index)
          boxStyle = configuration.boxStylesActive.valueAt(index)
        } else {
          boxStyle = configuration.boxStyles.valueAt(index)
          textColor = configuration.textColors.valueAt(index)
        }

        val boundingBox = gc.paintTextBox(
          lines = paintingVariables.labelsCache[index],
          lineSpacing = LineSpacing.Single,
          horizontalAlignment = configuration.textAlignments.valueAt(index),
          anchorDirection = anchorDirection,
          anchorGapHorizontal = arrowHeadLength, //always use length
          anchorGapVertical = arrowHeadLength, //always use length
          boxStyle = boxStyle,
          textColor = textColor,
          maxStringWidth = configuration.maxWidth.valueAt(index)
        )

        //Store the bounding box
        paintingVariables.boundingBoxes[index] = boundingBox.plus(x, y)

        gc.beginPath()
        ArrowHead.forOrientation(gc, anchorDirection, arrowHeadLength, arrowHeadWidth)

        if (active) {
          gc.fill(configuration.arrowFillsActive.valueAt(index))
        } else {
          gc.fill(configuration.arrowFills.valueAt(index))
        }

        gc.fill()
      }
    }
  }

  @ConfigurationDsl
  class Configuration(
    /**
     * Provides the anchor location of the hud element.
     * If NaN is returned, the element is not painted
     */
    var locations: @MayBeNaN @Window CoordinatesProvider1<LayerPaintingContext>,

    /**
     * Provides the text that is painted within the HUD element
     */
    var labels: HudLabelsProvider,
  ) {

    /**
     * Provides the z order for the elements
     */
    var zOrder: @ZIndex @MayBeNaN MultiDoublesProvider<HudElementIndex> = MultiDoublesProvider.always(com.meistercharts.model.ZIndex.auto.value)

    fun resetZOrder() {
      this.zOrder = MultiDoublesProvider.always(com.meistercharts.model.ZIndex.auto.value)
    }

    /**
     * Updates the z order to show the provided element index on top
     */
    fun zOrderShowIndexOnTop(hudElementIndexTop: @MayBeNegative @HudElementIndex Int) {
      if (hudElementIndexTop < 0) {
        resetZOrder()
        return
      }

      zOrder = MultiDoublesProvider {
        if (it == hudElementIndexTop) {
          1.0
        } else {
          com.meistercharts.model.ZIndex.auto.value
        }
      }
    }

    /**
     * The direction of the anchor (in which direction the anchor can be found)
     */
    var anchorDirections: MultiProvider<HudElementIndex, Direction> = MultiProvider.always(Direction.CenterLeft)

    /**
     * The length of the arrow-head
     */
    var arrowHeadLength: @px MultiDoublesProvider<HudElementIndex> = MultiDoublesProvider.always(10.0)

    /**
     * The width of the arrow-head
     */
    var arrowHeadWidth: @px MultiDoublesProvider<HudElementIndex> = MultiDoublesProvider.always(10.0)

    /**
     * The box style for the HUD element
     */
    var boxStyles: MultiProvider<HudElementIndex, BoxStyle> = MultiProvider.always(BoxStyle.modernBlue)

    /**
     * The box style for the active HUD element
     */
    var boxStylesActive: MultiProvider<HudElementIndex, BoxStyle> = MultiProvider.always(
      BoxStyle.modernBlue.copy(
        borderColor = Color.blue3,
        shadow = Shadow.Drop,
      )
    )

    /**
     * Returns the fill color for the arrow.
     * The default implementation returns the border color of the [boxStyles]
     */
    var arrowFills: MultiProvider<HudElementIndex, Color> = MultiProvider { index: @HudElementIndex Int ->
      val boxStyle = boxStyles.valueAt(index)
      boxStyle.borderColor ?: boxStyle.fill ?: textColors.valueAt(index)
    }

    /**
     * The fill for the active arrow
     */
    var arrowFillsActive: MultiProvider<HudElementIndex, Color> = MultiProvider { index: @HudElementIndex Int ->
      val boxStyle = boxStylesActive.valueAt(index)
      boxStyle.borderColor ?: boxStyle.fill ?: textColors.valueAt(index)
    }

    /**
     * The text color for the label
     */
    var textColors: MultiProvider<HudElementIndex, Color> = MultiProvider.always(Color.black)

    var textColorsActive: MultiProvider<HudElementIndex, Color> = MultiProvider.always(Color.black)

    /**
     * The text font fragments
     */
    var textFonts: MultiProvider<HudElementIndex, FontDescriptorFragment> = MultiProvider.always(Theme.thresholdLabelFont())

    /**
     * The text alignment
     */
    var textAlignments: MultiProvider<HudElementIndex, HorizontalAlignment> = MultiProvider.always(HorizontalAlignment.Left)

    /**
     * The max width for the text box
     */
    var maxWidth: @px MultiDoublesProvider<HudElementIndex> = MultiDoublesProvider.always(Double.MAX_VALUE)

    /**
     * THe index of the active hud element
     */
    var activeHudElementIndex: @HudElementIndex Int = HudElementIndex.None

    /**
     * Sets the active hud element index if necessary. calls [callbackOnChange] only if the value has changed
     */
    inline fun setActiveHudElementIndex(index: @HudElementIndex Int, callbackOnChange: () -> Unit = {}) {
      if (activeHudElementIndex == index) {
        return
      }

      activeHudElementIndex = index
      callbackOnChange()
    }
  }
}

/**
 * Creates a value axis hud layer - based upon the value axis layer
 */
fun ValueAxisLayer.hudLayer(
  /**
   * Provides the domain values that are painted as HUD elements
   */
  domainValues: @Domain DoublesProvider,
): ValueAxisHudLayer {
  return ValueAxisHudLayer(

    locations = object : @Window CoordinatesProvider1<LayerPaintingContext> {
      override fun size(param1: LayerPaintingContext): Int {
        return domainValues.size()
      }

      override fun xAt(index: Int, param1: LayerPaintingContext): @Window Double {
        val chartCalculator = param1.chartCalculator

        return when (axisConfiguration.orientation) {
          Orientation.Vertical -> paintingVariables().axisLineLocation
          Orientation.Horizontal -> {
            @DomainRelative val domainRelative = data.valueRangeProvider().toDomainRelative(domainValues.valueAt(index))

            if (axisConfiguration.paintRange == AxisConfiguration.PaintRange.ContentArea) {
              if (ChartCalculator.inContentArea(domainRelative).not()) {
                //Not within content area
                return Double.NaN
              }
            }

            @Window val xWindow = param1.chartCalculator.domainRelative2windowX(domainRelative)
            chartCalculator.inViewportOrX(xWindow, Double.NaN)
          }
        }
      }

      override fun yAt(index: Int, param1: LayerPaintingContext): @Window Double {
        val chartCalculator = param1.chartCalculator

        return when (axisConfiguration.orientation) {
          Orientation.Vertical -> {
            @DomainRelative val domainRelative = data.valueRangeProvider().toDomainRelative(domainValues.valueAt(index))

            if (axisConfiguration.paintRange == AxisConfiguration.PaintRange.ContentArea) {
              if (ChartCalculator.inContentArea(domainRelative).not()) {
                //Not within content area
                return Double.NaN
              }
            }

            val windowY = param1.chartCalculator.domainRelative2windowY(domainRelative)
            chartCalculator.inViewportOrY(windowY, Double.NaN)
          }

          Orientation.Horizontal -> paintingVariables().axisLineLocation
        }
      }
    }, labels = { index, _ ->
      val value = domainValues.valueAt(index)
      listOf(axisConfiguration.ticksFormat.format(value))
    }
  ) {
    anchorDirections = MultiProvider {
      val tickOrientation = axisConfiguration.tickOrientation

      when (axisConfiguration.side) {
        Side.Left -> when (tickOrientation) {
          Vicinity.Inside -> Direction.CenterLeft
          Vicinity.Outside -> Direction.CenterRight
        }

        Side.Right -> when (tickOrientation) {
          Vicinity.Inside -> Direction.CenterRight
          Vicinity.Outside -> Direction.CenterLeft
        }

        Side.Top -> when (tickOrientation) {
          Vicinity.Inside -> Direction.TopCenter
          Vicinity.Outside -> Direction.BottomCenter
        }

        Side.Bottom -> when (tickOrientation) {
          Vicinity.Inside -> Direction.BottomCenter
          Vicinity.Outside -> Direction.TopCenter
        }
      }
    }

    maxWidth = MultiDoublesProvider {
      when (axisConfiguration.side) {
        Side.Left,
        Side.Right,
        -> {
          //size (width) of axis
          // - width of axis lines
          // - arrow size
          // - box insets
          axisConfiguration.size - axisConfiguration.axisLineWidth - arrowHeadLength.valueAt(it) - boxStyles.valueAt(it).padding.offsetWidth
        }

        Side.Top,
        Side.Bottom,
        -> {
          //At the moment do not limit the width
          Double.NaN
        }
      }
    }
  }
}

/**
 * Index annotation for the HUD element index
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.LOCAL_VARIABLE)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@MultiProviderIndexContextAnnotation
annotation class HudElementIndex {

  companion object {
    const val None: @HudElementIndex Int = -1
  }
}


interface ValueAxisHudLayerPaintingVariables : PaintingVariables {
  /**
   * Contains the indices by z index
   */
  val zOrder: ZIndexSortingCache

  /**
   * Contains the coordinates for the HUD element
   */
  val coordinatesCache: CoordinatesCache

  /**
   * Cache for anchor directions
   */
  val anchorDirectionsCache: ObjectsCache<Direction>

  /**
   * Contains the labels
   */
  val labelsCache: ObjectsCache<List<String>>

  /**
   * ATTENTION: Filled in the *paint* method!
   * Must only be used after paint has been called (e.g. for mouse events)
   */
  val boundingBoxes: BoundsLayoutCache
}
