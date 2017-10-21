package tests

import com.github.fluidsonic.fluid.json.JSONCodecResolver
import com.github.fluidsonic.fluid.json.JSONCoderContext
import com.github.fluidsonic.fluid.json.JSONDecoder
import com.github.fluidsonic.fluid.json.JSONToken
import com.github.fluidsonic.fluid.json.readDecodable
import com.github.fluidsonic.fluid.json.readDecodableOfClassOrNull
import com.github.fluidsonic.fluid.json.readDecodableOrNull
import com.github.fluidsonic.fluid.json.readListOfDecodableElements
import com.github.fluidsonic.fluid.json.readListOrNullOfDecodableElements
import com.github.fluidsonic.fluid.json.readMapOfDecodableElements
import com.github.fluidsonic.fluid.json.readMapOfDecodableKeys
import com.github.fluidsonic.fluid.json.readMapOfDecodableValues
import com.github.fluidsonic.fluid.json.readMapOrNullOfDecodableElements
import com.github.fluidsonic.fluid.json.readMapOrNullOfDecodableKeys
import com.github.fluidsonic.fluid.json.readMapOrNullOfDecodableValues
import com.winterbe.expekt.should
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.StringReader


internal object JSONDecoderSpec : Spek({

	describe("JSONDecoder") {

		it(".with() shortcuts pass correct values") {
			JSONDecoder.with(source = "", codecResolver = JSONCodecResolver.default)
				.context.should.equal(JSONCoderContext.empty)

			JSONDecoder.with(source = StringReader(""), codecResolver = JSONCodecResolver.default)
				.context.should.equal(JSONCoderContext.empty)

			JSONDecoder.with(source = DummyJSONReader(), codecResolver = JSONCodecResolver.default)
				.context.should.equal(JSONCoderContext.empty)

			val testContext = TestCoderContext()

			JSONDecoder.with(source = "", codecResolver = JSONCodecResolver.default, context = testContext)
				.context.should.equal(testContext)

			JSONDecoder.with(source = StringReader(""), codecResolver = JSONCodecResolver.default, context = testContext)
				.context.should.equal(testContext)

			JSONDecoder.with(source = DummyJSONReader(), codecResolver = JSONCodecResolver.default, context = testContext)
				.context.should.equal(testContext)
		}

		it(".read() shortcuts pass correct values") {
			// TODO rework - make decoder which is backed by a stream of tokens & values

			var inputKey: String? = null
			var inputValue: String? = null

			@Suppress("UNCHECKED_CAST")
			val decoder = object : DummyJSONDecoder() {

				private var currentToken: JSONToken? = null
				private var tokenIterator = emptyList<JSONToken>().iterator()

				override val nextToken: JSONToken?
					get() = currentToken

				private fun consumeNextToken(expected: JSONToken) {
					currentToken.should.equal(expected)
					updateCurrentToken()
				}

				override fun <Value : Any> readDecodableOfClass(valueClass: Class<out Value>): Value {
					if (currentToken == JSONToken.mapKey) {
						consumeNextToken(JSONToken.mapKey)
						return inputKey as Value
					}
					else {
						consumeNextToken(JSONToken.stringValue)
						return inputValue as Value
					}
				}

				override fun readListEnd() {
					consumeNextToken(JSONToken.listEnd)
				}

				override fun readListStart() {
					consumeNextToken(JSONToken.listStart)
				}

				override fun readMapEnd() {
					consumeNextToken(JSONToken.mapEnd)
				}

				override fun readMapStart() {
					consumeNextToken(JSONToken.mapStart)
				}

				override fun readNull(): Nothing? {
					consumeNextToken(JSONToken.nullValue)
					return inputValue as Nothing?
				}

				override fun readString(): String {
					if (currentToken == JSONToken.mapKey) {
						consumeNextToken(JSONToken.mapKey)
						return inputKey!!
					}
					else {
						consumeNextToken(JSONToken.stringValue)
						return inputValue!!
					}
				}

				private fun updateCurrentToken() {
					currentToken = if (tokenIterator.hasNext()) tokenIterator.next() else null
				}

				inline fun with(vararg tokens: JSONToken, body: JSONDecoder<JSONCoderContext>.() -> Unit) {
					tokenIterator = tokens.iterator()
					updateCurrentToken()

					body()

					currentToken.should.equal(null)
					currentToken = null
					tokenIterator = emptyList<JSONToken>().iterator()
				}
			}

			inputKey = "key"

			decoder.with(JSONToken.stringValue) {
				inputValue = "okay"
				decoder.readDecodable<String>().should.equal(inputValue)
			}

			decoder.with(JSONToken.stringValue) {
				inputValue = "okay"
				decoder.readDecodableOfClassOrNull(String::class.java).should.equal(inputValue)
			}

			decoder.with(JSONToken.stringValue) {
				inputValue = "okay"
				decoder.readDecodableOrNull<String>().should.equal(inputValue)
			}

			decoder.with(JSONToken.nullValue, JSONToken.nullValue) {
				inputValue = null
				decoder.readDecodableOfClassOrNull(String::class.java).should.equal(null)
				decoder.readDecodableOrNull<String>().should.equal(null)
			}

			decoder.with(JSONToken.listStart, JSONToken.stringValue, JSONToken.listEnd) {
				inputValue = "okay"
				decoder.readListOfDecodableElements<String>().should.equal(listOf("okay"))
			}

			decoder.with(
				JSONToken.listStart, JSONToken.stringValue, JSONToken.listEnd,
				JSONToken.nullValue
			) {
				inputValue = "okay"
				decoder.readListOrNullOfDecodableElements<String>().should.equal(listOf("okay"))

				inputValue = null
				decoder.readListOrNullOfDecodableElements<String>().should.equal(null)
			}

			decoder.with(JSONToken.mapStart, JSONToken.mapKey, JSONToken.stringValue, JSONToken.mapEnd) {
				inputValue = "okay"
				decoder.readMapOfDecodableElements<String, String>().should.equal(mapOf("key" to "okay"))
			}

			decoder.with(JSONToken.mapStart, JSONToken.mapKey, JSONToken.stringValue, JSONToken.mapEnd) {
				inputValue = "okay"
				decoder.readMapOfDecodableKeys<String>().should.equal(mapOf("key" to "okay"))
			}

			decoder.with(JSONToken.mapStart, JSONToken.mapKey, JSONToken.stringValue, JSONToken.mapEnd) {
				inputValue = "okay"
				decoder.readMapOfDecodableValues<String>().should.equal(mapOf("key" to "okay"))
			}

			decoder.with(
				JSONToken.mapStart, JSONToken.mapKey, JSONToken.stringValue, JSONToken.mapEnd,
				JSONToken.nullValue
			) {
				inputValue = "okay"
				decoder.readMapOrNullOfDecodableElements<String, String>().should.equal(mapOf("key" to "okay"))

				inputValue = null
				decoder.readMapOrNullOfDecodableElements<String, String>().should.equal(null)
			}

			decoder.with(
				JSONToken.mapStart, JSONToken.mapKey, JSONToken.stringValue, JSONToken.mapEnd,
				JSONToken.nullValue
			) {
				inputValue = "okay"
				decoder.readMapOrNullOfDecodableKeys<String>().should.equal(mapOf("key" to "okay"))

				inputValue = null
				decoder.readMapOrNullOfDecodableKeys<String>().should.equal(null)
			}

			decoder.with(
				JSONToken.mapStart, JSONToken.mapKey, JSONToken.stringValue, JSONToken.mapEnd,
				JSONToken.nullValue
			) {
				inputValue = "okay"
				decoder.readMapOrNullOfDecodableValues<String>().should.equal(mapOf("key" to "okay"))

				inputValue = null
				decoder.readMapOrNullOfDecodableValues<String>().should.equal(null)
			}
		}
	}
})
