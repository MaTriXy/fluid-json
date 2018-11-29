package tests.coding

import com.github.fluidsonic.fluid.json.*
import com.winterbe.expekt.should
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.StringReader


internal object JSONDecoderSpec : Spek({

	describe("JSONDecoder") {

		it(".builder()") {
			JSONDecoder.builder()
				.codecs(BooleanJSONCodec)
				.source("true")
				.build()
				.apply {
					context.should.equal(JSONCodingContext.empty)
					readBoolean().should.be.`true`
				}

			JSONDecoder.builder()
				.codecs(listOf(BooleanJSONCodec))
				.source(StringReader("true"))
				.build()
				.apply {
					context.should.equal(JSONCodingContext.empty)
					readBoolean().should.be.`true`
				}

			val testContext = TestCoderContext()

			JSONDecoder.builder(testContext)
				.codecs()
				.source(JSONReader.build(StringReader("true")))
				.build()
				.apply {
					context.should.equal(testContext)
					readBoolean().should.be.`true`
				}
		}

		it(".read() shortcuts pass correct values") {
			testReadMethod<Any>("1") { readValueOrNull() }
			testReadMethod<Any>(null) { readValueOrNull() }

			testReadMethod("1") { readValueOfType() }
			testReadMethod("1") { readValueOfTypeOrNull() }
			testReadMethod("1") { readValueOfTypeOrNull(it) }
			testReadMethod<String>(null) { readValueOfTypeOrNull() }
			testReadMethod<String>(null) { readValueOfTypeOrNull(it) }

			testReadMethod(listOf("1")) { readValueOfType() }
			testReadMethod(listOf("1")) { readValueOfTypeOrNull() }
			testReadMethod(listOf("1")) { readValueOfTypeOrNull(it) }
			testReadMethod<List<String>>(null) { readValueOfTypeOrNull() }
			testReadMethod<List<String>>(null) { readValueOfTypeOrNull(it) }

			testReadMethod(listOf("1", null)) { readValueOfType() }
			testReadMethod(listOf("1", null)) { readValueOfTypeOrNull() }
			testReadMethod(listOf("1", null)) { readValueOfTypeOrNull(it) }
			testReadMethod<List<String?>>(null) { readValueOfTypeOrNull() }
			testReadMethod<List<String?>>(null) { readValueOfTypeOrNull(it) }

			testReadMethod(mapOf("1" to true)) { readValueOfType() }
			testReadMethod(mapOf("1" to true)) { readValueOfTypeOrNull() }
			testReadMethod(mapOf("1" to true)) { readValueOfTypeOrNull(it) }
			testReadMethod<Map<String, Boolean>>(null) { readValueOfTypeOrNull() }
			testReadMethod<Map<String, Boolean>>(null) { readValueOfTypeOrNull(it) }

			testReadMethod(mapOf("1" to true, "2" to null)) { readValueOfType() }
			testReadMethod(mapOf("1" to true, "2" to null)) { readValueOfTypeOrNull() }
			testReadMethod(mapOf("1" to true, "2" to null)) { readValueOfTypeOrNull(it) }
			testReadMethod<Map<String, Boolean?>>(null) { readValueOfTypeOrNull() }
			testReadMethod<Map<String, Boolean?>>(null) { readValueOfTypeOrNull(it) }

			testReadMethod(mapOf("1" to true, null to true)) { readValueOfType() }
			testReadMethod(mapOf("1" to true, null to true)) { readValueOfTypeOrNull() }
			testReadMethod(mapOf("1" to true, null to true)) { readValueOfTypeOrNull(it) }
			testReadMethod<Map<String, Boolean?>>(null) { readValueOfTypeOrNull() }
			testReadMethod<Map<String, Boolean?>>(null) { readValueOfTypeOrNull(it) }

			testReadMethod(mapOf("1" to true, null to null)) { readValueOfType() }
			testReadMethod(mapOf("1" to true, null to null)) { readValueOfTypeOrNull() }
			testReadMethod(mapOf("1" to true, null to null)) { readValueOfTypeOrNull(it) }
			testReadMethod<Map<String?, Boolean?>>(null) { readValueOfTypeOrNull() }
			testReadMethod<Map<String?, Boolean?>>(null) { readValueOfTypeOrNull(it) }
		}
	}
})


// TODO move the following methods inside the object above once KT-19796 is fixed
// https://youtrack.jetbrains.com/issue/KT-19796

private inline fun <reified Value : Any> testReadMethod(
	expectedValue: Value?,
	testBody: JSONDecoder<JSONCodingContext>.(type: JSONCodingType<Value>) -> Value?
) {
	val expectedType = jsonCodingType<Value>()

	val decoder = object : JSONDecoder<JSONCodingContext>, JSONReader by DummyJSONReader() {

		override val context: JSONCodingContext
			get() = error("")


		override val nextToken: JSONToken?
			get() = if (expectedValue == null) JSONToken.nullValue else JSONToken.stringValue


		override fun readNull(): Nothing? =
			null


		override fun readValue() =
			super<JSONDecoder>.readValue()


		override fun <Value : Any> readValueOfType(valueType: JSONCodingType<Value>): Value {
			(valueType as JSONCodingType<*>).should.equal(expectedType)

			@Suppress("UNCHECKED_CAST")
			return expectedValue as Value
		}
	}

	decoder.testBody(expectedType).should.equal(expectedValue)
}
