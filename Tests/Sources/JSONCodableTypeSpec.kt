package tests

import com.github.fluidsonic.fluid.json.*
import com.winterbe.expekt.should
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it


internal object JSONCodableTypeSpec : Spek({

	describe("JSONCodableType") {

		it("supports generic type parameters with multiple upper bounds") {
			jsonCodableType<MultipleUpperBounds<*>>()
		}

		it("supports KClass-based creation") {
			jsonCodableType(List::class).should.equal(jsonCodableType())
			jsonCodableType(Array<Any>::class).should.equal(jsonCodableType())
			jsonCodableType(Map::class).should.equal(jsonCodableType())
			jsonCodableType(String::class).should.equal(jsonCodableType())
		}
	}
}) {

	private interface Interface1
	private interface Interface2
	private interface MultipleUpperBounds<T> where T : Interface1, T : Interface2
}
