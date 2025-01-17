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

import assertk.*
import assertk.assertions.*
import com.meistercharts.Meistercharts
import com.meistercharts.axis.DistanceDays
import it.neckar.open.i18n.I18nConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class RelativeToNowTickFormatTest {
  @BeforeEach
  fun setUp() {
    Meistercharts.renderLoop.setCurrentFrameTimestampForTestsOnly(123.0)
  }

  @Test
  fun testRelativeFormat() {
    assertThat(RelativeToNowTickFormat.format(10.0, DistanceDays(7), I18nConfiguration.Germany)).isEqualTo("-0 y 0 M 0 d 0 h 0 min 0 s 113 ms")
  }
}
