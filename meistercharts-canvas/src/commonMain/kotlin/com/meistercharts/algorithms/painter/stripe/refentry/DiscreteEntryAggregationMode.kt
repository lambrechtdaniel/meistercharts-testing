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
package com.meistercharts.algorithms.painter.stripe.refentry

/**
 * How to visualize aggregated reference entries
 */
enum class DiscreteEntryAggregationMode {
  /**
   * Visualizes the most of the time reference entry
   */
  MostOfTheTimeLabel,

  /**
   * Visualizes the most important status.
   *
   * Does *not* show the most-of-the-time-label to avoid confusion.
   * It is possible that the most-of-the-time-label does not have the status enum with the lowest ordinal.
   */
  MostImportantStatus,
}
