/*
 * This file is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tresamigos.smv

import scala.collection.SortedMap

//import org.tresamigos.smv.StringSchemaEntry

// TODO: test writing of schema to file
// TODO: test reading/writing of data with different schema format (string quote, timestamp, etc).

class SchemaTest extends SparkTestUtil {
  test("Test schema string parsing") {
    val s = Schema.fromString("a:string; b:double")
    val entries = s.entries
    assert(entries.size === 2)
    assert(entries(0) === StringSchemaEntry("a"))
    assert(entries(1) === DoubleSchemaEntry("b"))
  }

  sparkTest("Test schema file parsing") {
    val s = Schema.fromFile(sc, testDataDir +  "SchemaTest/test1.schema")
    val entries = s.entries
    assert(entries.size === 9)
    assert(entries(0) === StringSchemaEntry("id"))
    assert(entries(1) === DoubleSchemaEntry("val"))
    assert(entries(2) === TimestampSchemaEntry("val2"))
    assert(entries(3) === TimestampSchemaEntry("val3", "ddMMyyyy"))
    assert(entries(4) === LongSchemaEntry("val4"))
    assert(entries(5) === IntegerSchemaEntry("val5"))
    assert(entries(6) === BooleanSchemaEntry("val6"))
    assert(entries(7) === FloatSchemaEntry("val7"))
    assert(entries(8) === MapSchemaEntry("val8", StringSchemaEntry("keyType"), IntegerSchemaEntry("valType")))
  }

  test("Schema entry equality") {
    val e1: SchemaEntry = StringSchemaEntry("a")
    val e2: SchemaEntry = StringSchemaEntry("a")
    val e3: SchemaEntry = StringSchemaEntry("b")
    val e4: SchemaEntry = DoubleSchemaEntry("a")

    assert(e1 == e2)
    assert(e1 != e3) // different name
    assert(e1 != e4) // different type
  }

  test("Test Timestamp Format") {
    val s = Schema.fromString("a:timestamp[yyyy]; b:Timestamp; c:Timestamp[yyyyMMdd]")
    val a = s.entries(0)
    val b = s.entries(1)
    val c = s.entries(2)

    assert(a === TimestampSchemaEntry("a", "yyyy"))
    assert(b === TimestampSchemaEntry("b", "yyyyMMdd"))
    assert(c === TimestampSchemaEntry("c", "yyyyMMdd"))

    val date_a = a.valToStr(a.strToVal("2014"))
    val date_b = b.valToStr(b.strToVal("20140203"))
    assert(date_a === "2014-01-01 00:00:00.0") // 2014
    assert(date_b === "2014-02-03 00:00:00.0") // 20140203
  }

  test("Test Map Values") {
    val s = Schema.fromString("a:map[integer, string]")
    val a = s.entries(0)

    assert(a === MapSchemaEntry("a", IntegerSchemaEntry("keyType"), StringSchemaEntry("valType")))

    val map_a = a.strToVal("1|2|3|4")
    assert(map_a === Map(1->"2", 3->"4"))

    // use a sorted map to ensure traversal order during serialization.
    val map_a_sorted = SortedMap(1->"2", 3->"4")
    val str_a = a.valToStr(map_a_sorted)
    assert(str_a === "1|2|3|4")
  }

  sparkTest("Test Timestamp in file") {
    val srdd = sqlContext.csvFileWithSchema(testDataDir +  "SchemaTest/test2")
    assert(srdd.count === 3)
  }
}