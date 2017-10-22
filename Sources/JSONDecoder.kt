package com.github.fluidsonic.fluid.json

import java.io.Reader
import java.io.StringReader


@Suppress("AddVarianceModifier")
interface JSONDecoder<Context : JSONCoderContext> : JSONReader {

	val context: Context


	fun <Value : Any> readDecodableOfClass(valueClass: Class<out Value>): Value


	companion object {

		fun builder(): BuilderForCodecs<JSONCoderContext> =
			BuilderForCodecsImpl(context = JSONCoderContext.empty)


		fun <Context : JSONCoderContext> builder(context: Context): BuilderForCodecs<Context> =
			BuilderForCodecsImpl(context = context)


		interface BuilderForCodecs<Context : JSONCoderContext> {

			fun codecs(resolver: JSONCodecResolver<Context>): BuilderForSource<Context>


			fun codecs(
				vararg providers: JSONCodecProvider<Context>,
				appendDefaultCodecs: Boolean = true
			) =
				codecs(JSONCodecResolver.of(providers = *providers, appendDefaultCodecs = appendDefaultCodecs))


			fun codecs(
				providers: Iterable<JSONCodecProvider<Context>>,
				appendDefaultCodecs: Boolean = true
			) =
				codecs(JSONCodecResolver.of(providers = providers, appendDefaultCodecs = appendDefaultCodecs))
		}


		private class BuilderForCodecsImpl<Context : JSONCoderContext>(
			private val context: Context
		) : BuilderForCodecs<Context> {

			override fun codecs(resolver: JSONCodecResolver<Context>) =
				BuilderForSourceImpl(
					context = context,
					codecResolver = resolver
				)
		}


		interface BuilderForSource<Context : JSONCoderContext> {

			fun source(source: JSONReader): Builder<Context>


			fun source(source: Reader) =
				source(JSONReader.build(source))


			fun source(source: String) =
				source(StringReader(source))
		}


		private class BuilderForSourceImpl<Context : JSONCoderContext>(
			private val context: Context,
			private val codecResolver: JSONCodecResolver<Context>
		) : BuilderForSource<Context> {

			override fun source(source: JSONReader) =
				BuilderImpl(
					context = context,
					codecResolver = codecResolver,
					source = source
				)
		}


		interface Builder<Context : JSONCoderContext> {

			fun build(): JSONDecoder<Context>
		}


		private class BuilderImpl<Context : JSONCoderContext>(
			private val context: Context,
			private val codecResolver: JSONCodecResolver<Context>,
			private val source: JSONReader
		) : Builder<Context> {

			override fun build() =
				StandardDecoder(
					context = context,
					codecResolver = codecResolver,
					source = source
				)
		}
	}
}


inline fun <reified Value : Any> JSONDecoder<*>.readDecodable() =
	readDecodableOfClass(Value::class.java)


inline fun <reified Value : Any> JSONDecoder<*>.readDecodableOrNull() =
	if (nextToken != JSONToken.nullValue) readDecodable<Value>() else readNull()


fun <Value : Any> JSONDecoder<*>.readDecodableOrNullOfClass(valueClass: Class<out Value>) =
	if (nextToken != JSONToken.nullValue) readDecodableOfClass(valueClass) else readNull()


inline fun <reified Element : Any> JSONDecoder<*>.readListOfDecodableElements() =
	readListByElement { readDecodableOfClass(Element::class.java) }


inline fun <reified Element : Any> JSONDecoder<*>.readListOrNullOfDecodableElements() =
	if (nextToken != JSONToken.nullValue) readListOfDecodableElements<Element>() else readNull()


inline fun <reified ElementKey : Any, reified ElementValue : Any> JSONDecoder<*>.readMapOfDecodableElements() =
	readMapByElement { readDecodableOfClass(ElementKey::class.java) to readDecodableOfClass(ElementValue::class.java) }


inline fun <reified ElementKey : Any> JSONDecoder<*>.readMapOfDecodableKeys() =
	readMapByElement { readDecodableOfClass(ElementKey::class.java) to readValue() }


inline fun <reified ElementValue : Any> JSONDecoder<*>.readMapOfDecodableValues() =
	readMapByElement { readMapKey() to readDecodableOfClass(ElementValue::class.java) }


inline fun <reified ElementKey : Any, reified ElementValue : Any> JSONDecoder<*>.readMapOrNullOfDecodableElements() =
	if (nextToken != JSONToken.nullValue) readMapOfDecodableElements<ElementKey, ElementValue>() else readNull()


inline fun <reified ElementKey : Any> JSONDecoder<*>.readMapOrNullOfDecodableKeys() =
	if (nextToken != JSONToken.nullValue) readMapOfDecodableKeys<ElementKey>() else readNull()


inline fun <reified ElementValue : Any> JSONDecoder<*>.readMapOrNullOfDecodableValues() =
	if (nextToken != JSONToken.nullValue) readMapOfDecodableValues<ElementValue>() else readNull()
