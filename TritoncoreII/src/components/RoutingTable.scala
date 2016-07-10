import java.math.BigInteger

import Chisel._

class RoutingTable extends Module {
  val io = new Bundle {
    val input      = Bits(INPUT,   128)
    val routing    = Bits(INPUT,  1100)
    val outputs    = Bits(OUTPUT,  220)
  }

  io.outputs := UInt(0) // weird CHISEL req

  for (i <- 0 to 3) {    // low, med-low, med-high, high order bits
    for (j <- 0 to 54) { // one for each LogicBlock on all orders
    val lut = Module(new LUT(inCount = 5,outCount = 1))
      lut.io.lut               := io.input((i + 1) * 32 - 1, i * 32) // take the high, med, or etc order bits
      lut.io.sel               := io.routing((i * 55 + j + 1) * 5 - 1, (i * 55 + j) * 5)
      io.outputs((i*55) + j)   := lut.io.res(0) // take logic block # j, and attach input i to it.
    }
  }
}


class RoutingTableTests(c: RoutingTable) extends Tester(c) {
  val routing = new ModifyableBigInt()
  // we only need to route each of the 32 inputs we care about to their respective LogicBlocks.
  // an easy way to do this is to simply take ech of the first two routing domains and route input 1 to output 1,
  // input 2 to output 2, etc.

  for (i <- 0 to 31) {
    // 5 bits per input.
    routing.setBits(5 * i + 4, 5 * i, i)
  }

  for (i <-0 to 31) {
    routing.setBits(5 * i + 4 + (5 * 55), 5 * i + (5 * 55), i)
  }

  println("routing value is: " + routing)

  poke(c.io.routing, routing.value)
  poke(c.io.input, new BigInteger("FF008000F0C02", 16))
  expect(c.io.outputs, new BigInteger("7F804000000000F0C02", 16))

}

object RoutingTableTestRunner {
  def main(args: Array[String]): Unit = {
    chiselMainTest(Array[String]("--backend", "c", "--compile", "--test", "--genHarness"),
      () => Module(new RoutingTable()))
    {
      c => new RoutingTableTests(c)
    }
  }
}