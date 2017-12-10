package examples

import com.github.fluidsonic.fluid.json.*
import java.io.StringReader


fun main(args: Array<String>) {
	val parser = JSONCodingParser.default

	// You can also let the parser read from a Reader
	val value = parser.parseValue(StringReader(""" { "hello": "world", "test": 123 } """))
	println(value)
}
