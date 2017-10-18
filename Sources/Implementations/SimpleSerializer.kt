package com.github.fluidsonic.fluid.json


// FIXME find extendable solution for options
internal class SimpleSerializer(
	val convertsInvalidKeysToString: Boolean = false,
	val convertsInvalidValuesToString: Boolean = false
) : JSONSerializer {

	override fun serialize(value: Any?, destination: JSONWriter) {
		var currentIterator: Iterator<*>? = null
		var currentValue = value
		var isInMap = false
		val iteratorStack = mutableListOf<Iterator<*>>()
		val isInMapStack = mutableListOf<Boolean>()
		var isInMapElementValue = false

		loop@ do {
			if (!isInMapElementValue && currentIterator != null) {
				if (currentIterator.hasNext()) {
					currentValue = currentIterator.next()
				}
				else {
					if (isInMap) {
						destination.writeMapEnd()
					}
					else {
						destination.writeListEnd()
					}

					val iteratorStackSize = iteratorStack.size
					if (iteratorStackSize > 0) {
						currentIterator = iteratorStack.removeAt(iteratorStackSize - 1)
						isInMap = isInMapStack.removeAt(isInMapStack.size - 1)

						continue
					}
					else {
						break
					}
				}
			}

			when (currentValue) {
				null ->
					destination.writeNull()

				is Boolean ->
					destination.writeBoolean(currentValue)

				is Double ->
					if (currentValue.isFinite())
						destination.writeDouble(currentValue)
					else if (convertsInvalidValuesToString)
						destination.writeString(currentValue.toString())
					else
						throw JSONException("Cannot serialize double value '$currentValue'")

				is Float ->
					if (currentValue.isFinite())
						destination.writeFloat(currentValue)
					else if (convertsInvalidValuesToString)
						destination.writeString(currentValue.toString())
					else
						throw JSONException("Cannot serialize float value '$currentValue'")

				is Number ->
					destination.writeNumber(currentValue)

				is String ->
					destination.writeString(currentValue)

				is Map<*, *> -> {
					if (currentIterator != null) {
						iteratorStack += currentIterator
					}
					isInMapStack += isInMap

					currentIterator = currentValue.iterator()
					isInMap = true

					destination.writeMapStart()
				}

				is Iterable<*> -> {
					if (currentIterator != null) {
						iteratorStack += currentIterator
					}
					isInMapStack += isInMap

					currentIterator = currentValue.iterator()
					isInMap = false

					destination.writeListStart()
				}

				is Map.Entry<*, *> ->
					if (isInMap && !isInMapElementValue) {
						val (elementKey, elementValue) = currentValue
						when {
							elementKey is String -> destination.writeKey(elementKey)
							convertsInvalidKeysToString -> destination.writeKey(elementKey.toString())
							else -> throw JSONException("Cannot serialize non-String key of ${currentValue::class.java}: $currentValue")
						}

						currentValue = elementValue
						isInMapElementValue = true
						continue@loop
					}
					else if (convertsInvalidValuesToString)
						destination.writeString(currentValue.toString())
					else
						throw JSONException("Cannot serialize value of ${currentValue::class.java}: $currentValue")

				else ->
					if (convertsInvalidValuesToString)
						destination.writeString(currentValue.toString())
					else
						throw JSONException("Cannot serialize value of ${currentValue::class.java}: $currentValue")
			}

			isInMapElementValue = false
		}
		while (currentIterator != null)
	}


	companion object {

		val strict = SimpleSerializer()
	}
}
