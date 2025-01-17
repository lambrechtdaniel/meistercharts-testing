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
package com.meistercharts.charts.support.threshold

import com.meistercharts.algorithms.layers.DirectionalLinesLayer
import com.meistercharts.algorithms.layers.HudElementIndex
import com.meistercharts.algorithms.layers.HudLabelsProvider
import com.meistercharts.algorithms.layers.LayerPaintingContext
import com.meistercharts.algorithms.layers.MultipleLayersDelegatingLayer
import com.meistercharts.algorithms.layers.ValueAxisHudLayer
import com.meistercharts.algorithms.layers.ValueAxisLayer
import com.meistercharts.algorithms.layers.hudLayer
import com.meistercharts.algorithms.layers.mouseOverInteractions
import com.meistercharts.algorithms.layers.visibleIf
import com.meistercharts.annotations.Domain
import com.meistercharts.canvas.layer.LayerSupport
import com.meistercharts.charts.support.ValueAxisForKeyProvider
import com.meistercharts.charts.support.ValueAxisSupport
import it.neckar.open.collections.Cache
import it.neckar.open.collections.cache
import it.neckar.open.provider.DoublesProvider
import it.neckar.open.provider.DoublesProvider1
import it.neckar.open.provider.MultiProvider2
import it.neckar.open.provider.SizedProvider
import it.neckar.open.provider.asDoublesProvider
import it.neckar.open.provider.asDoublesProvider1
import it.neckar.open.provider.asMultiProvider2withParam2

/**
 * Visualizes thresholds using
 * * [com.meistercharts.algorithms.layers.ValueAxisHudLayer]
 * * [com.meistercharts.algorithms.layers.DirectionalLinesLayer].
 *
 * This gestalt does *not* add layers on itself.
 * It is a helper class that *provides* layers for existing [ValueAxisLayer]s
 *
 * It supports multiple keys that can be used to identify multiple value axis.
 * If only a single value axis is supported [Unit] should be used as key.
 */
class ThresholdsSupport<Key>(
  /**
   * Provides the value axis for the given key
   */
  valueAxisProvider: ValueAxisForKeyProvider<Key>,

  /**
   * Provides the threshold values for *each* key
   * The size will be interpreted as [HudElementIndex]
   */
  thresholdValueProvider: @Domain DoublesProvider1<Key>,

  /**
   * Provides the labels for each threshold.
   * The parameter represents the key. The index corresponds to [HudElementIndex] - depending on the size of [thresholdValueProvider]
   */
  thresholdLabelProvider: @Domain MultiProvider2<HudElementIndex, List<String>, Key, LayerPaintingContext>,

  /**
   * Additional configuration
   */
  additionalConfiguration: ThresholdsSupport<Key>.Configuration.() -> Unit = {},
) {

  /**
   * Contains the HUD layers (for threshold)
   */
  private val hudLayersCache: Cache<Key, ValueAxisHudLayer> = cache("hudLayersCache", 100)

  /**
   * Returns the hud layer for the given data series index
   */
  fun getHudLayer(key: Key): ValueAxisHudLayer {
    return hudLayersCache.getOrStore(key) {
      //Create the values provider
      val values: @Domain DoublesProvider = configuration::thresholdValueProvider.asDoublesProvider(key)

      //Get the value axis layer that represents the "base" for the hud layer
      val valueAxisLayer = configuration.valueAxisProvider.getAxisLayer(key)

      valueAxisLayer.hudLayer(values).also { valueAxisHudLayer ->
        valueAxisHudLayer.configuration.labels = HudLabelsProvider { index, param1 -> configuration.thresholdLabelProvider.valueAt(index, key, param1) }

        configuration.hudLayerConfiguration(valueAxisHudLayer.configuration, key, valueAxisHudLayer)
      }
    }
  }

  /**
   * Contains the lines layers
   */
  private val thresholdLinesLayersCache = cache<Key, DirectionalLinesLayer>("directionalLinesLayersCache", 100)

  /**
   * Returns the threshold layer for the given data series index
   */
  fun getThresholdLinesLayer(key: Key): DirectionalLinesLayer {
    return thresholdLinesLayersCache.getOrStore(key) {

      //Get the "base" layers
      val valueAxisLayer = configuration.valueAxisProvider.getAxisLayer(key)
      val hudLayer = getHudLayer(key)

      DirectionalLinesLayer.createForValueAxisAndHud(valueAxisLayer, hudLayer).also { layer ->
        configuration.thresholdLinesLayerConfiguration(layer.configuration, key, layer)
      }
    }
  }

  /**
   * Adds all layers for the provided key
   */
  fun addLayers(layerSupport: LayerSupport, key: Key, visibleCondition: (() -> Boolean)? = null): LayerAddResult<Key> {
    val hudLayer = getHudLayer(key)
    val hudLayerVisibilityLayer = hudLayer.visibleIf(false, visibleCondition)
    layerSupport.layers.addLayer(hudLayerVisibilityLayer)

    val thresholdLinesLayer = getThresholdLinesLayer(key)
    val thresholdLinesVisibilityLayer = thresholdLinesLayer.visibleIf(false, visibleCondition)
    layerSupport.layers.addLayer(thresholdLinesVisibilityLayer)

    return LayerAddResult(key, hudLayer, thresholdLinesLayer)
  }

  /**
   * Represents the layers that have been added
   */
  data class LayerAddResult<Key>(val key: Key, val hudLayer: ValueAxisHudLayer, val thresholdLinesLayer: DirectionalLinesLayer)

  /**
   * The configuration for the thresholds support
   */
  val configuration: Configuration = Configuration(
    valueAxisProvider, thresholdValueProvider, thresholdLabelProvider
  ).also(additionalConfiguration) //initialize *after* the cache field declarations

  inner class Configuration(
    /**
     * Provides the value axis for the given key
     */
    val valueAxisProvider: ValueAxisForKeyProvider<Key>,

    /**
     * Provides the threshold values for *each* key
     * The size will be interpreted as [HudElementIndex]
     */
    var thresholdValueProvider: @Domain DoublesProvider1<Key>,

    /**
     * Provides the labels for each threshold.
     * The parameter represents the key. The index corresponds to [HudElementIndex] - depending on the size of [thresholdValueProvider]
     */
    var thresholdLabelProvider: @Domain MultiProvider2<HudElementIndex, List<String>, Key, LayerPaintingContext>,
  ) {
    /**
     * Is called for each hud layer - only when instantiated(!)
     */
    var hudLayerConfiguration: ValueAxisHudLayerConfiguration<Key> = { _, _ -> }
      set(value) {
        field = value
        //Apply the new configuration to existing
        hudLayersCache.forEach { key, layer ->
          value.invoke(layer.configuration, key, layer)
        }
      }

    var thresholdLinesLayerConfiguration: ThresholdLinesLayerConfiguration<Key> = { _, _ -> }
      set(value) {
        field = value
        //Apply the new configuration to existing
        thresholdLinesLayersCache.forEach { key, layer ->
          value.invoke(layer.configuration, key, layer)
        }
      }
  }

  companion object {
    /**
     * Creates a threshold support for a single value axis
     */
    fun singleValueAxis(
      valueAxisProvider: () -> ValueAxisLayer,
      thresholdValues: @Domain DoublesProvider,
      thresholdLabels: HudLabelsProvider,
      additionalConfiguration: ThresholdsSupport<Unit>.Configuration.() -> Unit = {},
    ): ThresholdsSupport<Unit> {
      return ThresholdsSupport(
        valueAxisProvider = { valueAxisProvider() },
        thresholdValueProvider = thresholdValues.asDoublesProvider1(),
        thresholdLabelProvider = thresholdLabels.asMultiProvider2withParam2(),
        additionalConfiguration = additionalConfiguration,
      )
    }

    /**
     * Creates the interaction layers for multiple [DirectionalLinesLayer]s and [ValueAxisHudLayer]
     */
    fun createInteractionLayers(
      linesDelegateLayer: MultipleLayersDelegatingLayer<DirectionalLinesLayer>,
      hudLayersDelegate: MultipleLayersDelegatingLayer<ValueAxisHudLayer>,
    ): HudAndDirectionLayerActiveConnector {
      return createInteractionLayers(linesDelegateLayer.delegates, hudLayersDelegate.delegates)
    }

    /**
     * Creates the interaction layers for multiple [DirectionalLinesLayer]s and [ValueAxisHudLayer]
     */
    fun createInteractionLayers(
      directionalLayers: SizedProvider<DirectionalLinesLayer>,
      hudLayers: SizedProvider<ValueAxisHudLayer>,
    ): HudAndDirectionLayerActiveConnector {
      val directionalLinesInteractionLayer = directionalLayers.mouseOverInteractions()
      val hudLayersInteractionLayer = hudLayers.mouseOverInteractions()

      return HudAndDirectionLayerActiveConnector(directionalLinesInteractionLayer, hudLayersInteractionLayer).connectTo(directionalLayers, hudLayers)
    }
  }
}

typealias ValueAxisHudLayerConfiguration<Key> = ValueAxisHudLayer.Configuration.(Key, axis: ValueAxisHudLayer) -> Unit
typealias ThresholdLinesLayerConfiguration<Key> = DirectionalLinesLayer.Configuration.(Key, axis: DirectionalLinesLayer) -> Unit

/**
 * Creates a thresholds support for this value axis support
 */
fun <Key> ValueAxisSupport<Key>.thresholdsSupport(
  /**
   * Provides the threshold values for *each* key
   * The size will be interpreted as [HudElementIndex]
   */
  thresholdValueProvider: @Domain DoublesProvider1<Key>,

  /**
   * Provides the labels for each threshold.
   * The parameter represents the key. The index corresponds to [HudElementIndex] - depending on the size of [thresholdValueProvider]
   */
  thresholdLabelProvider: @Domain MultiProvider2<HudElementIndex, List<String>, Key, LayerPaintingContext>,

  additionalConfiguration: ThresholdsSupport<Key>.Configuration.() -> Unit = {},
): ThresholdsSupport<Key> {
  return ThresholdsSupport(
    valueAxisProvider = { key ->
      getAxisLayer(key)
    },
    thresholdValueProvider = thresholdValueProvider,
    thresholdLabelProvider = thresholdLabelProvider,
    additionalConfiguration = additionalConfiguration,
  )
}

/**
 * Creates a threshold support for a *single* value axis support
 */
fun ValueAxisSupport<Unit>.thresholdsSupportSingle(
  /**
   * Provides the threshold values for *each* key
   * The size will be interpreted as [HudElementIndex]
   */
  thresholdValues: @Domain DoublesProvider,

  /**
   * Provides the labels for each threshold.
   * The parameter represents the key. The index corresponds to [HudElementIndex] - depending on the size of [thresholdValues]
   */
  thresholdLabels: @Domain HudLabelsProvider,

  /**
   * Additional configuration
   */
  additionalConfiguration: ThresholdsSupport<Unit>.Configuration.() -> Unit = {},
): ThresholdsSupport<Unit> {
  return ThresholdsSupport.singleValueAxis(
    valueAxisProvider = { getAxisLayer(Unit) },
    thresholdValues = thresholdValues,
    thresholdLabels = thresholdLabels,
    additionalConfiguration = additionalConfiguration
  )
}

/**
 * Returns the HUD layer for the thresholds support
 */
inline fun ThresholdsSupport<Unit>.getHudLayer(): ValueAxisHudLayer {
  return this.getHudLayer(Unit)
}

/**
 * Returns the thresholds lines layer
 */
inline fun ThresholdsSupport<Unit>.getThresholdLinesLayer(): DirectionalLinesLayer {
  return this.getThresholdLinesLayer(Unit)
}

/**
 * Adds the hud and thresholds lines layer.
 * For a single value axis! Uses [Unit] as key.
 */
inline fun ThresholdsSupport<Unit>.addLayers(layerSupport: LayerSupport, noinline visibleCondition: (() -> Boolean)? = null) {
  val result = this.addLayers(layerSupport, Unit, visibleCondition)

  //Create the interaction layers
  val directionalLinesInteractionLayer = result.thresholdLinesLayer.mouseOverInteractions()
  val hudLayerInteractionLayer = result.hudLayer.mouseOverInteractions()

  //Connect both interaction layers
  HudAndDirectionLayerActiveConnector(directionalLinesInteractionLayer, hudLayerInteractionLayer).connectTo(result.thresholdLinesLayer, result.hudLayer)

  //Add the layers
  layerSupport.layers.addLayer(directionalLinesInteractionLayer)
  layerSupport.layers.addLayer(hudLayerInteractionLayer)
}

