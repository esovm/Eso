package common

import java.util.Calendar

import scala.collection.{immutable, mutable}
import scala.util.{Success, Try}

case class Config(bool: immutable.HashMap[String, Boolean], num: immutable.HashMap[String, Int], cal: Calendar) extends EsoObj{
  def apply(tag: String): Either[Int, Boolean] = Either.cond(bool.isDefinedAt(tag), bool(tag), num(tag))
  def get(tag: String): Option[Either[Int, Boolean]] = Try{apply(tag)} match{
    case Success(res) => Some(res)
    case _ => None
  }
  
  def bools: Vector[(String, Boolean)] = bool.toVector.sortBy(_._1)
  def nums: Vector[(String, Int)] = num.toVector.sortBy(_._1)
  
  def set(nam: String, b: Boolean): Config = Config(bool + ((nam, b)), num, cal)
  def set(nam: String, n: Int): Config = Config(bool, num + ((nam, n)), cal)
}
object Config extends EsoObj{
  def apply(bool: mutable.HashMap[String, Boolean], num: mutable.HashMap[String, Int]): Config = new Config(mkImmut(bool), mkImmut(num), Calendar.getInstance)
  def apply(bool: immutable.HashMap[String, Boolean], num: immutable.HashMap[String, Int]): Config = new Config(bool, num, Calendar.getInstance)
}