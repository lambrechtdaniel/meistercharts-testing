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
package com.meistercharts.algorithms.tile

import assertk.*
import assertk.assertions.*
import com.meistercharts.charts.ChartId
import com.meistercharts.model.Zoom
import com.meistercharts.tile.TileIndex
import org.junit.jupiter.api.Test

class HistoryTileInvalidatorTest {
  @Test
  fun testXHashCode() {
    val hashCodes = listOf(
      TileIdentifier(
        chartId = ChartId(17),
        tileIndex = TileIndex(1, 2, 3, 4),
        zoom = Zoom(1.0, 1.0)
      ).xDataHashCode(),


      TileIdentifier(
        chartId = ChartId(17),
        tileIndex = TileIndex(1, 3, 3, 4),
        zoom = Zoom(1.0, 1.0)
      ).xDataHashCode(),


      TileIdentifier(
        chartId = ChartId(17),
        tileIndex = TileIndex(2, 3, 3, 4),
        zoom = Zoom(1.0, 1.0)
      ).xDataHashCode(),


      TileIdentifier(
        chartId = ChartId(17),
        tileIndex = TileIndex(2, 2, 3, 4),
        zoom = Zoom(1.0, 1.0)
      ).xDataHashCode(),
    )

    assertThat(hashCodes.toSet()).hasSize(hashCodes.size)
  }
}
