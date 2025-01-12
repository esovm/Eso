package brainfuck

import common.Config

import scala.annotation.tailrec
import scala.collection.immutable
import scala.util.{Success, Try}

class GenBFT(val name: String, val baseLang: String, val kvPairs: Vector[(String, String)]) extends BFTranslator{
  def apply(config: Config)(progRaw: String): Try[String] = Success(translate(progRaw, revSyntax))
  def unapply(config: Config)(progRaw: String): Try[String] = Success(translate(progRaw, syntax))
  
  private def translate(prog: String, syn: immutable.HashMap[String, String]): String = {
    val keysOrder = syn.keys.toVector.sortWith(_.length > _.length)
    
    @tailrec
    def tHelper(log: String, src: String): String = keysOrder.find(src.startsWith) match{
      case Some(k) => tHelper(log ++ syn(k), src.drop(k.length))
      case None if src.nonEmpty => tHelper(log :+ src.head, src.tail)
      case None if src.isEmpty => log
    }
    
    tHelper("", prog)
  }
}
object GenBFT{
  def apply(name: String, syntax: immutable.HashMap[String, String]): GenBFT = {
    new GenBFT(name, "BrainFuck", syntax.toVector)
  }
  
  def apply(name: String, pairs: IndexedSeq[(String, String)]): GenBFT = {
    new GenBFT(name, "BrainFuck", pairs.toVector)
  }
}
