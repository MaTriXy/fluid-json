package com.github.fluidsonic.fluid.json

import java.time.DateTimeException
import java.time.Year


object YearJSONCodec : AbstractJSONCodec<Year, JSONCodingContext>() {

	override fun JSONDecoder<JSONCodingContext>.decode(valueType: JSONCodingType<in Year>) =
		readInt().let { raw ->
			try {
				Year.of(raw)!!
			}
			catch (e: DateTimeException) {
				invalidValueError("year in ISO-8601 format expected, got '$raw'")
			}
		}


	override fun JSONEncoder<JSONCodingContext>.encode(value: Year) =
		writeInt(value.value)
}
