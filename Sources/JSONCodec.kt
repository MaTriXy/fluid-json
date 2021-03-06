package com.github.fluidsonic.fluid.json

import kotlin.reflect.KClass


interface JSONCodec<Value : Any, in Context : JSONCoderContext>
	: JSONDecoderCodec<Value, Context>, JSONEncoderCodec<Value, Context> {

	override val encodableClass
		get() = decodableType.rawClass


	override fun <Value : Any> decoderCodecForType(decodableType: JSONCodableType<in Value>) =
		super<JSONDecoderCodec>.decoderCodecForType(decodableType)


	override fun <Value : Any> encoderCodecForClass(encodableClass: KClass<out Value>) =
		super<JSONEncoderCodec>.encoderCodecForClass(encodableClass)


	companion object
}
