package com.github.fluidsonic.fluid.json

import kotlin.reflect.KClass


internal class StandardCodecResolver<in Context : JSONCoderContext> private constructor(
	private val decoderCodecByClass: MutableMap<KClass<*>, JSONDecoderCodec<*, Context>>,
	private val encoderCodecByClass: MutableMap<KClass<*>, JSONEncoderCodec<*, Context>>
) : JSONCodecResolver<Context> {

	private val resolvedDecoderCodecByClass = mutableMapOf<KClass<*>, JSONDecoderCodec<*, Context>>()
	private val resolvedEncoderCodecByClass = mutableMapOf<KClass<*>, JSONEncoderCodec<*, Context>>()

	override val decoderCodecs = decoderCodecByClass.values.toSet().toList()
	override val encoderCodecs = encoderCodecByClass.values.toSet().toList()


	@Suppress("UNCHECKED_CAST")
	override fun <Value : Any> decoderCodecForClass(`class`: KClass<out Value>): JSONDecoderCodec<Value, Context>? {
		val codecs = resolvedDecoderCodecByClass

		var decoder = synchronized(codecs) { codecs[`class`] }
		if (decoder == null) {
			if (`class` == Any::class) {
				decoder = decoderCodecByClass[Any::class]
			}
			if (decoder == null) {
				decoder = decoderCodecByClass.entries.firstOrNull { it.key.isAssignableOrBoxableTo(`class`) }?.value
			}

			decoder?.let { resolvedDecoder ->
				synchronized(codecs) {
					codecs[`class`] = resolvedDecoder
				}
			}
		}

		return decoder as JSONDecoderCodec<Value, Context>?
	}


	@Suppress("UNCHECKED_CAST")
	override fun <Value : Any> encoderCodecForClass(`class`: KClass<out Value>): JSONEncoderCodec<in Value, Context>? {
		val codecs = resolvedEncoderCodecByClass

		var encoder = synchronized(codecs) { codecs[`class`] }
		if (encoder == null) {
			val resolvedEncoder = encoderCodecByClass.entries.firstOrNull { it.key.isAssignableOrBoxableFrom(`class`) }?.value
			if (resolvedEncoder != null) {
				synchronized(codecs) {
					codecs[`class`] = resolvedEncoder
				}

				encoder = resolvedEncoder
			}
		}

		return encoder as JSONEncoderCodec<Value, Context>?
	}


	companion object {

		private fun <Context : JSONCoderContext> collectDecoderCodecs(
			codecs: List<JSONDecoderCodec<*, Context>>,
			into: MutableMap<KClass<*>, JSONDecoderCodec<*, Context>>
		) {
			codecs
				.filter { into.putIfAbsent(it.decodableClass, it) == null }
				.forEach { collectDecoderCodecs(it.decoderCodecs, into) }
		}


		private fun <Context : JSONCoderContext> collectEncoderCodecs(
			codecs: List<JSONEncoderCodec<*, Context>>,
			into: MutableMap<KClass<*>, JSONEncoderCodec<*, Context>>
		) {
			for (codec in codecs) {
				codec.encodableClasses
					.filter { into.putIfAbsent(it, codec) == null }
					.forEach { collectEncoderCodecs(codec.encoderCodecs, into) }
			}
		}


		fun <Context : JSONCoderContext> of(
			providers: Iterable<JSONCodecProvider<Context>>
		): StandardCodecResolver<Context> =
			StandardCodecResolver(
				decoderCodecByClass = mutableMapOf<KClass<*>, JSONDecoderCodec<*, Context>>()
					.apply {
						collectDecoderCodecs(providers.flatMap { it.decoderCodecs }, into = this)
					},
				encoderCodecByClass = mutableMapOf<KClass<*>, JSONEncoderCodec<*, Context>>()
					.apply {
						collectEncoderCodecs(providers.flatMap { it.encoderCodecs }, into = this)
					}
			)
	}
}
